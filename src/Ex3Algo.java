import exe.ex3.game.Game;
import exe.ex3.game.GhostCL;
import exe.ex3.game.PacManAlgo;
import exe.ex3.game.PacmanGame;

import java.awt.*;
import java.util.ArrayDeque;
import java.util.ArrayList;

/**
 * Ex3 Pac-Man algorithm (v11 cleaned).
 *
 * <p>Priorities:
 * <ol>
 *   <li><b>Escape</b> when danger ghosts are near (maximize distance).</li>
 *   <li><b>Eat fast</b>: move toward the nearest target using BFS shortest path.</li>
 *   <li><b>Tie-break smartly</b>: when multiple targets have equal shortest distance,
 *       choose the safer / more open / less loopy option.</li>
 * </ol>
 *
 * <p>Power policy:
 * <ul>
 *   <li><b>POWER LOCK</b>: while ghosts are eatable (power mode), do not step on GREEN.</li>
 *   <li><b>NO POWER FIRST</b>: do not step on GREEN during the first ~5 seconds.</li>
 * </ul>
 */
public class Ex3Algo implements PacManAlgo {
    private int _count;

    private boolean _inited = false;
    private int DOT, POWER;

    /**
     * Detected wall value (stable).
     */
    private int baseWallValue = Integer.MIN_VALUE;

    /**
     * Last position and direction (used for reverse-avoid and "keep direction" bias).
     */
    private int lastX = Integer.MIN_VALUE, lastY = Integer.MIN_VALUE;
    private int lastDir = Game.STAY;

    /**
     * Counts consecutive ticks where Pac-Man didn't move.
     */
    private int stuckCount = 0;

    // ======= BEHAVIOR KNOBS =======

    /**
     * If nearest danger ghost distance <= this -> go into escape mode.
     */
    private static final int DANGER_TRIGGER = 7;

    /**
     * Avoid stepping into cells that are within this distance to a danger ghost (if possible).
     */
    private static final int HARD_AVOID = 2;

    /**
     * If POWER is this close, allow taking it (when not blocked by policy).
     */
    private static final int POWER_TAKE_IF_DIST_LE = 2;

    /**
     * If danger is this close, POWER becomes valuable.
     */
    private static final int POWER_PREFER_IF_DANGER_LE = 5;

    /**
     * Small opening phase: start moving to avoid idling.
     */
    private static final int OPENING_STEPS = 25 ;


    /** In power mode: if ghost is within this distance, strongly prioritize eating it. */
    private static final int GHOST_CHASE_DIST = 25;

    /** Safety margin (ticks) so we don't arrive when power just ends. */
    private static final int EATABLE_TIME_MARGIN = 2;


    /**
     * Block GREEN for first ~10 seconds.
     * Assumption: ~10 ticks/sec => 10 sec ≈ 100 ticks.
     */
    private static final int NO_POWER_FIRST_TICKS = 50;

    /**
     * Memory of recent positions to break loops.
     */
    private static final int LOOP_MEM = 12;

    private int prevW = -1, prevH = -1;

    private final ArrayDeque<Long> lastPositions = new ArrayDeque<>();

    public Ex3Algo() {
        _count = 0;
    }

    @Override
    public String getInfo() {
        return "PacMan v11 (clean): escape-first, eat nearest fast (smart tie-break), smart power + POWER LOCK + NO GREEN first 5s.";
    }

    private void resetMemory() {
        lastPositions.clear();
        stuckCount = 0;
        lastX = Integer.MIN_VALUE;
        lastY = Integer.MIN_VALUE;
        lastDir = Game.STAY;
        baseWallValue = Integer.MIN_VALUE;
        _count = 0;
    }

    /**
     * Main decision function called each tick by the game.
     *
     * @param game game API
     * @return movement direction (Game.UP/DOWN/LEFT/RIGHT/STAY)
     */
    @Override
    public int move(PacmanGame game) {
        final int code = 0;

        int[][] b = game.getGame(code);

        if (b.length != prevW || b[0].length != prevH) {
            prevW = b.length;
            prevH = b[0].length;
            resetMemory();
        }
        _count++;

        if (!_inited) initColors(code);

        if (baseWallValue == Integer.MIN_VALUE) {
            baseWallValue = detectWallValueStable(b);
        }

        int[] pac = parseXY(game.getPos(code));
        int px = wrapX(pac[0], b), py = wrapY(pac[1], b);

        if (px == lastX && py == lastY) stuckCount++;
        else stuckCount = 0;

        GhostCL[] ghosts = game.getGhosts(code);
        pushPos(px, py);

        boolean powerMode = isPowerMode(ghosts, code);

        // Block GREEN if:
        // 1) power mode active (POWER LOCK), OR
        // 2) first ~5 seconds
        boolean blockPowerTiles = powerMode || (_count <= NO_POWER_FIRST_TICKS);

        // Opening: just start moving (still obeys passable rules)
        if (_count <= OPENING_STEPS) {
            int op = openingMove(px, py, b, blockPowerTiles, ghosts, code);
            if (op != Game.STAY) {
                remember(px, py, op);
                return op;
            }
        }

        int chosen;

        if (powerMode) {
            // NEW: hunt eatable ghosts first (if reachable in time)
            chosen = chaseEatableGhostMove(px, py, b, blockPowerTiles, ghosts, code);

            // fallback: eat dots
            if (chosen == Game.STAY) {
                chosen = bfsToNearestValueSmart(px, py, b, DOT, blockPowerTiles, ghosts, code);
            }
            if (chosen == Game.STAY) chosen = anyLegalMove(px, py, b, blockPowerTiles, ghosts, code);
        } else {

            int curThreat = minBfsDistToDangerGhost(px, py, b, ghosts, code, blockPowerTiles);

            if (curThreat != Integer.MAX_VALUE && curThreat <= DANGER_TRIGGER) {
                chosen = escapeMove(px, py, b, blockPowerTiles, ghosts, code, curThreat);
            } else {
                chosen = eatFastMove(px, py, b, blockPowerTiles, ghosts, code, curThreat);
            }
        }

        if (chosen == Game.STAY) chosen = anyLegalMove(px, py, b, blockPowerTiles, ghosts, code);

        // loop-breaking + stuck handling
        chosen = breakLoopIfNeeded(px, py, b, chosen, blockPowerTiles, ghosts, code);
        if (stuckCount >= 3) chosen = forceDifferentLegal(px, py, b, chosen, blockPowerTiles, ghosts, code);

        // avoid reversing direction if possible
        chosen = applyNoReverse(px, py, b, chosen, blockPowerTiles, ghosts, code);

        remember(px, py, chosen);
        return chosen;
    }

    // ===================== PRIORITY 1: ESCAPE =====================

    /**
     * Choose the move that maximizes distance from the nearest danger (non-eatable) ghost.
     * Uses a hard-avoid threshold when possible; relaxes if trapped.
     */
    private int escapeMove(int px, int py, int[][] b, boolean blockPowerTiles,
                           GhostCL[] ghosts, int code, int curThreat) {
        int[] dirs = {Game.UP, Game.LEFT, Game.DOWN, Game.RIGHT};

        int bestDir = Game.STAY;
        int bestScore = Integer.MIN_VALUE;

        // Pass 1: enforce hard avoid if possible
        for (int d : dirs) {
            int nx = stepX(px, d, b);
            int ny = stepY(py, d, b);
            if (!passable(nx, ny, b, blockPowerTiles, ghosts, code)) continue;

            int nt = minBfsDistToDangerGhost(nx, ny, b, ghosts, code, blockPowerTiles);
            if (nt <= HARD_AVOID && curThreat > HARD_AVOID) continue;

            int score =
                    safeVal(nt) * 2000 +
                            countExits(nx, ny, b, blockPowerTiles, ghosts, code) * 120 +
                            (d == lastDir ? 40 : 0) +
                            (isRecentPos(nx, ny) ? -300 : 0);

            if (score > bestScore) {
                bestScore = score;
                bestDir = d;
            }
        }
        if (bestDir != Game.STAY) return bestDir;

        // Pass 2: relax hard avoid (if trapped)
        for (int d : dirs) {
            int nx = stepX(px, d, b);
            int ny = stepY(py, d, b);
            if (!passable(nx, ny, b, blockPowerTiles, ghosts, code)) continue;

            int nt = minBfsDistToDangerGhost(nx, ny, b, ghosts, code, blockPowerTiles);

            int score =
                    safeVal(nt) * 2000 +
                            countExits(nx, ny, b, blockPowerTiles, ghosts, code) * 120 +
                            (d == lastDir ? 40 : 0) +
                            (isRecentPos(nx, ny) ? -300 : 0);

            if (score > bestScore) {
                bestScore = score;
                bestDir = d;
            }
        }
        return bestDir;
    }

    /**
     * Clamp distance to a reasonable range for scoring.
     */
    private int safeVal(int dist) {
        if (dist == Integer.MAX_VALUE) return 50;
        return Math.min(dist, 50);
    }

    // ===================== PRIORITY 2+3: EAT FAST (NEAREST) =====================

    /**
     * Choose the nearest target type (DOT vs POWER) using BFS distance.
     * Default prefers DOT; allows POWER only when strategically valuable.
     */
    private int eatFastMove(int px, int py, int[][] b, boolean blockPowerTiles,
                            GhostCL[] ghosts, int code, int curThreat) {
        int dotDist = nearestTargetDist(px, py, b, DOT, blockPowerTiles, ghosts, code);
        int powDist = nearestTargetDist(px, py, b, POWER, blockPowerTiles, ghosts, code);

        boolean dangerNear = (curThreat != Integer.MAX_VALUE && curThreat <= POWER_PREFER_IF_DANGER_LE);
        boolean powerVeryClose = (powDist != Integer.MAX_VALUE && powDist <= POWER_TAKE_IF_DIST_LE);

        if (dotDist == Integer.MAX_VALUE && powDist == Integer.MAX_VALUE) return Game.STAY;
        if (dotDist == Integer.MAX_VALUE)
            return bfsToNearestValueSmart(px, py, b, POWER, blockPowerTiles, ghosts, code);
        if (powDist == Integer.MAX_VALUE) return bfsToNearestValueSmart(px, py, b, DOT, blockPowerTiles, ghosts, code);

        boolean shouldTakePower = dangerNear || powerVeryClose || (powDist + 2 < dotDist);
        int target = shouldTakePower ? POWER : DOT;

        int dir = bfsToNearestValueSmart(px, py, b, target, blockPowerTiles, ghosts, code);
        if (dir != Game.STAY) return dir;

        int other = (target == DOT) ? POWER : DOT;
        return bfsToNearestValueSmart(px, py, b, other, blockPowerTiles, ghosts, code);
    }

    // ===================== BETTER NEAREST (BFS + TIE-BREAK) =====================

    /**
     * BFS to nearest target value (shortest path in steps).
     * If there are multiple targets at the same minimal distance, choose the best by:
     * <ol>
     *   <li>Safer from danger ghosts (larger min BFS distance).</li>
     *   <li>More exits (less likely to get trapped).</li>
     *   <li>Avoid recent positions (reduce looping).</li>
     * </ol>
     *
     * @return the first move direction toward the selected best target, or {@link Game#STAY}.
     */
    private int bfsToNearestValueSmart(int px, int py, int[][] b, int targetValue,
                                       boolean blockPowerTiles, GhostCL[] ghosts, int code) {
        if (blockPowerTiles && targetValue == POWER) return Game.STAY;

        int w = b.length, h = b[0].length;
        if (!boardHasValue(b, targetValue)) return Game.STAY;

        boolean[][] vis = new boolean[w][h];
        int[][] firstDir = new int[w][h];
        int[][] dist = new int[w][h];

        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                dist[i][j] = Integer.MAX_VALUE;
                firstDir[i][j] = Game.STAY;
            }
        }

        ArrayDeque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{px, py});
        vis[px][py] = true;
        dist[px][py] = 0;

        int[] dirs = {Game.UP, Game.LEFT, Game.DOWN, Game.RIGHT};

        int foundDist = Integer.MAX_VALUE;
        ArrayList<int[]> candidates = new ArrayList<>();

        while (!q.isEmpty()) {
            int[] cur = q.poll();
            int cx = cur[0], cy = cur[1];
            int cd = dist[cx][cy];

            if (cd > foundDist) break; // stop after minimal layer

            for (int d : dirs) {
                int nx = stepX(cx, d, b);
                int ny = stepY(cy, d, b);

                if (vis[nx][ny]) continue;
                if (!passable(nx, ny, b, blockPowerTiles, ghosts, code)) continue;

                vis[nx][ny] = true;
                dist[nx][ny] = cd + 1;
                firstDir[nx][ny] = (cx == px && cy == py) ? d : firstDir[cx][cy];

                if (b[nx][ny] == targetValue) {
                    if (dist[nx][ny] < foundDist) {
                        foundDist = dist[nx][ny];
                        candidates.clear();
                        candidates.add(new int[]{nx, ny});
                    } else if (dist[nx][ny] == foundDist) {
                        candidates.add(new int[]{nx, ny});
                    }
                }

                q.add(new int[]{nx, ny});
            }
        }

        if (candidates.isEmpty()) return Game.STAY;

        int bestDir = Game.STAY;
        int bestScore = Integer.MIN_VALUE;

        for (int[] c : candidates) {
            int x = c[0], y = c[1];
            int dir = firstDir[x][y];
            if (dir == Game.STAY) continue;

            int threat = minBfsDistToDangerGhost(x, y, b, ghosts, code, blockPowerTiles);
            int exits = countExits(x, y, b, blockPowerTiles, ghosts, code);

            int score =
                    safeVal(threat) * 1000 +
                            exits * 120 +
                            (dir == lastDir ? 40 : 0) +
                            (isRecentPos(x, y) ? -400 : 0);

            if (score > bestScore) {
                bestScore = score;
                bestDir = dir;
            }
        }

        return bestDir;
    }

    // ===================== DIST / GHOST HELPERS =====================

    /**
     * BFS distance from (sx,sy) to nearest cell containing targetValue.
     *
     * @return shortest distance in steps, or {@link Integer#MAX_VALUE} if unreachable / not allowed.
     */
    private int nearestTargetDist(int sx, int sy, int[][] b, int targetValue,
                                  boolean blockPowerTiles, GhostCL[] ghosts, int code) {
        if (blockPowerTiles && targetValue == POWER) return Integer.MAX_VALUE;

        int w = b.length, h = b[0].length;
        boolean[][] vis = new boolean[w][h];
        ArrayDeque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{sx, sy, 0});
        vis[sx][sy] = true;

        int[] dirs = {Game.UP, Game.LEFT, Game.DOWN, Game.RIGHT};

        while (!q.isEmpty()) {
            int[] cur = q.poll();
            int x = cur[0], y = cur[1], dist = cur[2];

            if (b[x][y] == targetValue && !(x == sx && y == sy)) return dist;

            for (int d : dirs) {
                int nx = stepX(x, d, b);
                int ny = stepY(y, d, b);

                if (vis[nx][ny]) continue;
                if (!passable(nx, ny, b, blockPowerTiles, ghosts, code)) continue;

                vis[nx][ny] = true;
                q.add(new int[]{nx, ny, dist + 1});
            }
        }
        return Integer.MAX_VALUE;
    }

    /**
     * @return minimal BFS distance from (px,py) to any non-eatable ghost.
     */
    private int minBfsDistToDangerGhost(int px, int py, int[][] b, GhostCL[] ghosts,
                                        int code, boolean blockPowerTiles) {
        int best = Integer.MAX_VALUE;
        if (ghosts == null) return best;

        for (GhostCL g : ghosts) {
            if (g == null) continue;
            if (g.remainTimeAsEatable(code) > 0) continue;
            int[] gp = parseXY(g.getPos(code));
            int gx = wrapX(gp[0], b), gy = wrapY(gp[1], b);
            int d = bfsDist(px, py, gx, gy, b, blockPowerTiles, ghosts, code);
            best = Math.min(best, d);
        }
        return best;
    }

    /**
     * BFS distance between two coordinates (walls/ghost blocks apply).
     */
    private int bfsDist(int sx, int sy, int tx, int ty, int[][] b,
                        boolean blockPowerTiles, GhostCL[] ghosts, int code) {
        if (sx == tx && sy == ty) return 0;

        int w = b.length, h = b[0].length;
        boolean[][] vis = new boolean[w][h];
        ArrayDeque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{sx, sy, 0});
        vis[sx][sy] = true;

        int[] dirs = {Game.UP, Game.LEFT, Game.DOWN, Game.RIGHT};

        while (!q.isEmpty()) {
            int[] cur = q.poll();
            int x = cur[0], y = cur[1], dist = cur[2];

            for (int d : dirs) {
                int nx = stepX(x, d, b);
                int ny = stepY(y, d, b);

                if (vis[nx][ny]) continue;
                if (b[nx][ny] == baseWallValue) continue;
                if (blockPowerTiles && b[nx][ny] == POWER) continue;
                if (isNonEatableGhostAt(nx, ny, ghosts, code)) continue;

                if (nx == tx && ny == ty) return dist + 1;

                vis[nx][ny] = true;
                q.add(new int[]{nx, ny, dist + 1});
            }
        }
        return Integer.MAX_VALUE;
    }

    // ===================== LOOP =====================

    /**
     * Push current position into loop memory.
     */
    private void pushPos(int x, int y) {
        long key = (((long) x) << 32) ^ (y & 0xffffffffL);
        lastPositions.addLast(key);
        while (lastPositions.size() > LOOP_MEM) lastPositions.removeFirst();
    }

    /**
     * @return true if (x,y) was visited recently.
     */
    private boolean isRecentPos(int x, int y) {
        long key = (((long) x) << 32) ^ (y & 0xffffffffL);
        for (long k : lastPositions) if (k == key) return true;
        return false;
    }

    /**
     * If chosen move leads to a recently visited position, try alternative legal move to break loops.
     */
    private int breakLoopIfNeeded(int px, int py, int[][] b, int chosen,
                                  boolean blockPowerTiles, GhostCL[] ghosts, int code) {
        int nx = stepX(px, chosen, b);
        int ny = stepY(py, chosen, b);
        if (!isRecentPos(nx, ny)) return chosen;

        int[] dirs = {Game.UP, Game.LEFT, Game.DOWN, Game.RIGHT};
        for (int d : dirs) {
            if (d == chosen) continue;
            int tx = stepX(px, d, b), ty = stepY(py, d, b);
            if (!passable(tx, ty, b, blockPowerTiles, ghosts, code)) continue;
            if (!isRecentPos(tx, ty)) return d;
        }
        return chosen;
    }

    // ===================== MOVEMENT HELPERS =====================

    /**
     * Count legal neighboring cells (bigger = more open, less trap-prone).
     */
    private int countExits(int x, int y, int[][] b, boolean blockPowerTiles, GhostCL[] ghosts, int code) {
        int exits = 0;
        int[] dirs = {Game.UP, Game.LEFT, Game.DOWN, Game.RIGHT};
        for (int d : dirs) {
            int nx = stepX(x, d, b);
            int ny = stepY(y, d, b);
            if (passable(nx, ny, b, blockPowerTiles, ghosts, code)) exits++;
        }
        return exits;
    }

    /**
     * Fallback when no smart choice exists.
     * Tries to keep direction, avoids immediate reverse if possible.
     */
    private int anyLegalMove(int px, int py, int[][] b, boolean blockPowerTiles, GhostCL[] ghosts, int code) {
        int[] dirs = {Game.UP, Game.LEFT, Game.DOWN, Game.RIGHT};

        if (lastDir != Game.STAY) {
            int nx = stepX(px, lastDir, b), ny = stepY(py, lastDir, b);
            if (passable(nx, ny, b, blockPowerTiles, ghosts, code)) return lastDir;
        }

        int rev = opposite(lastDir);
        for (int d : dirs) {
            if (d == rev) continue;
            int nx = stepX(px, d, b), ny = stepY(py, d, b);
            if (passable(nx, ny, b, blockPowerTiles, ghosts, code)) return d;
        }

        for (int d : dirs) {
            int nx = stepX(px, d, b), ny = stepY(py, d, b);
            if (passable(nx, ny, b, blockPowerTiles, ghosts, code)) return d;
        }
        return Game.STAY;
    }

    /**
     * If stuck for several ticks, force a different legal direction (avoid "chosen" and avoid reverse if possible).
     */
    private int forceDifferentLegal(int px, int py, int[][] b, int avoid,
                                    boolean blockPowerTiles, GhostCL[] ghosts, int code) {
        int[] dirs = {Game.UP, Game.LEFT, Game.DOWN, Game.RIGHT};
        int rev = opposite(lastDir);

        for (int d : dirs) {
            if (d == avoid) continue;
            if (d == rev) continue;
            int nx = stepX(px, d, b), ny = stepY(py, d, b);
            if (passable(nx, ny, b, blockPowerTiles, ghosts, code)) return d;
        }
        for (int d : dirs) {
            if (d == avoid) continue;
            int nx = stepX(px, d, b), ny = stepY(py, d, b);
            if (passable(nx, ny, b, blockPowerTiles, ghosts, code)) return d;
        }
        return avoid;
    }

    /**
     * Avoid immediate reverse direction if there is an alternative legal move.
     */
    private int applyNoReverse(int px, int py, int[][] b, int chosen,
                               boolean blockPowerTiles, GhostCL[] ghosts, int code) {
        if (lastDir == Game.STAY) return chosen;

        int rev = opposite(lastDir);
        if (chosen != rev) return chosen;

        int[] dirs = {Game.UP, Game.LEFT, Game.DOWN, Game.RIGHT};
        for (int d : dirs) {
            if (d == rev) continue;
            int nx = stepX(px, d, b), ny = stepY(py, d, b);
            if (passable(nx, ny, b, blockPowerTiles, ghosts, code)) return d;
        }
        return chosen;
    }

    private void remember(int px, int py, int dir) {
        lastX = px;
        lastY = py;
        lastDir = dir;
    }

    // ===================== PASSABLE / WALL =====================

    /**
     * A cell is passable if it is not a wall, not a blocked POWER tile, and not occupied by a danger ghost.
     */
    private boolean passable(int x, int y, int[][] b, boolean blockPowerTiles, GhostCL[] ghosts, int code) {
        int wx = wrapX(x, b);
        int wy = wrapY(y, b);

        int v = b[wx][wy];
        if (v == baseWallValue) return false;
        if (blockPowerTiles && v == POWER) return false;
        return !isNonEatableGhostAt(wx, wy, ghosts, code);
    }

    /**
     * @return true if a non-eatable (danger) ghost is exactly on (x,y).
     */
    private boolean isNonEatableGhostAt(int x, int y, GhostCL[] ghosts, int code) {
        if (ghosts == null) return false;
        for (GhostCL g : ghosts) {
            if (g == null) continue;
            if (g.remainTimeAsEatable(code) > 0) continue;
            int[] gp = parseXY(g.getPos(code));
            if (gp[0] == x && gp[1] == y) return true;
        }
        return false;
    }

    // ===================== WALL DETECTION (NO HASHMAP) =====================

    // Simple int->count structure without HashMap (arrays + linear search)
    private static class IntCounter {
        private int[] vals;
        private int[] cnts;
        private int size;

        IntCounter(int capacity) {
            if (capacity < 4) capacity = 4;
            vals = new int[capacity];
            cnts = new int[capacity];
            size = 0;
        }

        void add(int v) {
            for (int i = 0; i < size; i++) {
                if (vals[i] == v) {
                    cnts[i]++;
                    return;
                }
            }
            ensureCapacity(size + 1);
            vals[size] = v;
            cnts[size] = 1;
            size++;
        }

        int argMaxValueOr(int fallback) {
            if (size == 0) return fallback;
            int bestIdx = 0;
            for (int i = 1; i < size; i++) {
                if (cnts[i] > cnts[bestIdx]) bestIdx = i;
            }
            return vals[bestIdx];
        }

        private void ensureCapacity(int needed) {
            if (needed <= vals.length) return;
            int newCap = Math.max(needed, vals.length * 2);
            int[] nv = new int[newCap];
            int[] nc = new int[newCap];
            for (int i = 0; i < size; i++) {
                nv[i] = vals[i];
                nc[i] = cnts[i];
            }
            vals = nv;
            cnts = nc;
        }
    }

    /**
     * Detect the wall value by taking the most frequent "non-empty non-dot non-power" value on the borders.
     * If not found, falls back to most frequent excluding DOT/POWER.
     */
    private int detectWallValueStable(int[][] b) {
        int w = b.length, h = b[0].length;
        IntCounter borderFreq = new IntCounter(2 * w + 2 * h);

        for (int x = 0; x < w; x++) {
            addIfWallCandidate(borderFreq, b[x][0]);
            addIfWallCandidate(borderFreq, b[x][h - 1]);
        }
        for (int y = 0; y < h; y++) {
            addIfWallCandidate(borderFreq, b[0][y]);
            addIfWallCandidate(borderFreq, b[w - 1][y]);
        }

        int bestVal = borderFreq.argMaxValueOr(Integer.MIN_VALUE);
        if (bestVal != Integer.MIN_VALUE) return bestVal;

        return mostFrequentExcluding(b, DOT, POWER);
    }

    private void addIfWallCandidate(IntCounter freq, int v) {
        if (v == DOT || v == POWER || v == 0) return;
        freq.add(v);
    }

    private static int mostFrequentExcluding(int[][] b, int a, int c) {
        int w = b.length, h = b[0].length;
        IntCounter freq = new IntCounter(w * h);

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int v = b[x][y];
                if (v == a || v == c || v == 0) continue;
                freq.add(v);
            }
        }

        int bestVal = freq.argMaxValueOr(Integer.MIN_VALUE);
        return (bestVal == Integer.MIN_VALUE) ? mostFrequent(b) : bestVal;
    }

    private static int mostFrequent(int[][] b) {
        int w = b.length, h = b[0].length;
        IntCounter freq = new IntCounter(w * h);

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                freq.add(b[x][y]);
            }
        }
        return freq.argMaxValueOr(b[0][0]);
    }

    // ===================== BASIC =====================

    /**
     * Initialize board color codes (DOT/PINK and POWER/GREEN).
     */
    private void initColors(int code) {
        DOT = Game.getIntColor(Color.PINK, code);
        POWER = Game.getIntColor(Color.GREEN, code);
        _inited = true;
    }

    /**
     * Parse "x,y" string-like position object into int array {x,y}.
     */
    private static int[] parseXY(Object posObj) {
        String s = String.valueOf(posObj).trim();
        int comma = s.indexOf(',');
        if (comma < 0) return new int[]{0, 0};
        try {
            int x = Integer.parseInt(s.substring(0, comma).trim());
            int y = Integer.parseInt(s.substring(comma + 1).trim());
            return new int[]{x, y};
        } catch (Exception e) {
            return new int[]{0, 0};
        }
    }

    private boolean isPowerMode(GhostCL[] ghosts, int code) {
        if (ghosts == null) return false;
        for (GhostCL g : ghosts) {
            if (g == null) continue;
            if (g.remainTimeAsEatable(code) > 0) return true;
        }
        return false;
    }

    private boolean boardHasValue(int[][] b, int targetValue) {
        for (int x = 0; x < b.length; x++) {
            for (int y = 0; y < b[0].length; y++) {
                if (b[x][y] == targetValue) return true;
            }
        }
        return false;
    }

    private int wrapX(int x, int[][] b) {
        int w = b.length;
        x %= w;
        if (x < 0) x += w;
        return x;
    }

    private int wrapY(int y, int[][] b) {
        int h = b[0].length;
        y %= h;
        if (y < 0) y += h;
        return y;
    }

    private int stepX(int x, int dir, int[][] b) {
        return wrapX(x + dx(dir), b);
    }

    private int stepY(int y, int dir, int[][] b) {
        return wrapY(y + dy(dir), b);
    }

    private static int dx(int dir) {
        if (dir == Game.LEFT) return -1;
        if (dir == Game.RIGHT) return 1;
        return 0;
    }

    private static int dy(int dir) {
        if (dir == Game.UP) return 1;
        if (dir == Game.DOWN) return -1;
        return 0;
    }

    private static int opposite(int dir) {
        if (dir == Game.UP) return Game.DOWN;
        if (dir == Game.DOWN) return Game.UP;
        if (dir == Game.LEFT) return Game.RIGHT;
        if (dir == Game.RIGHT) return Game.LEFT;
        return Game.STAY;
    }

    /**
     * Simple "start moving" heuristic for the first few ticks, still obeying passable rules.
     */
    private int openingMove(int px, int py, int[][] b, boolean blockPowerTiles, GhostCL[] ghosts, int code) {
        int rx = stepX(px, Game.RIGHT, b), ry = stepY(py, Game.RIGHT, b);
        if (passable(rx, ry, b, blockPowerTiles, ghosts, code)) return Game.RIGHT;

        int lx = stepX(px, Game.LEFT, b), ly = stepY(py, Game.LEFT, b);
        if (passable(lx, ly, b, blockPowerTiles, ghosts, code)) return Game.LEFT;

        return Game.STAY;
    }


    /**
     * In power mode: chase the best eatable ghost if reachable before power ends.
     * Returns first direction toward that ghost, or STAY if no good chase exists.
     */
    private int chaseEatableGhostMove(int px, int py, int[][] b, boolean blockPowerTiles,
                                      GhostCL[] ghosts, int code) {
        if (ghosts == null) return Game.STAY;

        int bestDir = Game.STAY;
        int bestScore = Integer.MIN_VALUE;

        for (GhostCL g : ghosts) {
            if (g == null) continue;

            int t = (int) g.remainTimeAsEatable(code);
            if (t <= 0) continue; // not eatable now

            int[] gp = parseXY(g.getPos(code));
            int gx = wrapX(gp[0], b), gy = wrapY(gp[1], b);

            int d = bfsDist(px, py, gx, gy, b, blockPowerTiles, ghosts, code);
            if (d == Integer.MAX_VALUE) continue;

            // Must be reachable before power ends (with margin)
            if (d > t - EATABLE_TIME_MARGIN) continue;

            // Only chase if not super far (optional but helps)
            if (d > GHOST_CHASE_DIST && t < d + 2) continue;

            // Get first step toward this ghost
            int dir = firstStepToward(px, py, gx, gy, b, blockPowerTiles, ghosts, code);
            if (dir == Game.STAY) continue;

            // Score: closer is better, more time left is better, more exits is better
            int score =
                    (100 - Math.min(d, 100)) * 6000      // מרחק = הכי הכי חשוב
                            + Math.min(t, 100) * 50      // זמן שנשאר = בונוס קטן
                            + (dir == lastDir ? 20 : 0);

            if (score > bestScore) {
                bestScore = score;
                bestDir = dir;
            }
        }

        return bestDir;
    }

    /**
     * Returns the first direction from (sx,sy) on a shortest path to (tx,ty), or STAY if unreachable.
     */
    private int firstStepToward(int sx, int sy, int tx, int ty, int[][] b,
                                boolean blockPowerTiles, GhostCL[] ghosts, int code) {
        if (sx == tx && sy == ty) return Game.STAY;

        int w = b.length, h = b[0].length;
        boolean[][] vis = new boolean[w][h];
        int[][] firstDir = new int[w][h];

        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) firstDir[i][j] = Game.STAY;
        }

        ArrayDeque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{sx, sy});
        vis[sx][sy] = true;

        int[] dirs = {Game.UP, Game.LEFT, Game.DOWN, Game.RIGHT};

        while (!q.isEmpty()) {
            int[] cur = q.poll();
            int x = cur[0], y = cur[1];

            for (int d : dirs) {
                int nx = stepX(x, d, b);
                int ny = stepY(y, d, b);

                if (vis[nx][ny]) continue;
                if (!passable(nx, ny, b, blockPowerTiles, ghosts, code)) continue;

                vis[nx][ny] = true;
                firstDir[nx][ny] = (x == sx && y == sy) ? d : firstDir[x][y];

                if (nx == tx && ny == ty) return firstDir[nx][ny];

                q.add(new int[]{nx, ny});
            }
        }

        return Game.STAY;
    }
}