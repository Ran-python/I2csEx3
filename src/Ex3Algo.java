import exe.ex3.game.Game;
import exe.ex3.game.GhostCL;
import exe.ex3.game.PacManAlgo;
import exe.ex3.game.PacmanGame;

import java.awt.*;
import java.util.ArrayDeque;

public class Ex3Algo implements PacManAlgo {

    private boolean inited = false;
    private int DOT, POWER, WALL;

    // ====== TUNING (for your map) ======
    private static final int ESCAPE_TRIGGER = 6;   // escape only when really close (more win-focused)
    private static final int SOFT_DANGER = 7;      // start caring a bit
    private static final int KILL_ZONE = 1;        // avoid stepping into dangerDist<=1 unless forced

    private static final int POWER_FREE_DIST = 2;  // take power if basically free
    private static final int POWER_WHEN_DANGER = 3;

    // anti-loop
    private static final int LOOP_MEM = 10;
    private static final int LOOP_PENALTY = 120;
    private static final int STUCK_LIMIT = 6;

    private int lastX = Integer.MIN_VALUE, lastY = Integer.MIN_VALUE;
    private int lastDir = Game.STAY;
    private int stuckCount = 0;
    private final ArrayDeque<Long> lastPos = new ArrayDeque<>();

    @Override
    public String getInfo() {
        return "Win-first: DOT BFS + POWER when needed + urgent escape. Walls detected by BLUE.";
    }

    @Override
    public int move(PacmanGame game) {
        final int code = 0;
        if (!inited) initColors(code);

        int[][] b = game.getGame(code);
        int w = b.length, h = b[0].length;

        int[] pac = parseXY(game.getPos(code));
        int px = wrap(pac[0], w), py = wrap(pac[1], h);

        GhostCL[] ghosts = game.getGhosts(code);

        // stuck/loop memory
        if (px == lastX && py == lastY) stuckCount++;
        else stuckCount = 0;
        pushPos(px, py);

        // Build obstacle grid using BLUE wall
        boolean[][] blocked = new boolean[w][h];
        for (int x = 0; x < w; x++) for (int y = 0; y < h; y++) {
            blocked[x][y] = (b[x][y] == WALL);
        }

        // Distance maps
        int[][] dotDist = multiSourceDistToValue(b, blocked, w, h, DOT);
        int[][] powDist = multiSourceDistToValue(b, blocked, w, h, POWER);
        int[][] dangerDist = multiSourceDangerDist(blocked, w, h, ghosts, code);

        int dangerHere = dangerVal(dangerDist[px][py]);
        boolean powerMode = anyEatable(ghosts, code);

        // If in power mode: still DOT-first, but don't fear ghosts; optionally chase if adjacent
        // (Keeping it simple & fast: just ignore danger in power mode)
        if (powerMode) {
            int dir = chooseDotFirst(px, py, b, blocked, w, h, ghosts, code, dotDist, powDist, null, true);
            if (dir != Game.STAY) return rememberAndReturn(px, py, dir);
        }

        // Decide target (DOT first unless power is free or danger is rising)
        int dDot = dotDist[px][py];
        int dPow = powDist[px][py];

        boolean powerFree = (dPow >= 0 && dPow <= POWER_FREE_DIST);
        boolean dangerNear = (dangerHere <= POWER_WHEN_DANGER);

        boolean goPower = false;
        if (dDot < 0 && dPow >= 0) goPower = true;
        if (powerFree) goPower = true;
        if (dangerNear && dPow >= 0 && (dDot < 0 || dPow <= dDot + 2)) goPower = true;

        int chosen;

        // Urgent escape when really close
        if (dangerHere <= ESCAPE_TRIGGER) {
            chosen = escapeMove(px, py, b, blocked, w, h, ghosts, code, dangerDist, dotDist);
        } else {
            chosen = chooseDotFirst(px, py, b, blocked, w, h, ghosts, code,
                    dotDist, powDist, dangerDist, false);

            // If we chose DOT but power is clearly better under danger, allow switching
            if (goPower) {
                int toPow = choosePower(px, py, b, blocked, w, h, ghosts, code, powDist, dangerDist);
                if (toPow != Game.STAY) chosen = toPow;
            }
        }

        // Unstuck: if looping while safe -> force dot progress (even through corridors)
        if (stuckCount >= STUCK_LIMIT && dangerHere > ESCAPE_TRIGGER + 1) {
            int forced = forceDotProgress(px, py, b, blocked, w, h, ghosts, code, dotDist, dangerDist);
            if (forced != Game.STAY) chosen = forced;
        }

        if (chosen == Game.STAY) chosen = anyLegal(px, py, b, blocked, w, h, ghosts, code, dangerDist);

        return rememberAndReturn(px, py, chosen);
    }

    // ===================== DOT-FIRST MOVE =====================

    private int chooseDotFirst(int px, int py, int[][] b, boolean[][] blocked, int w, int h,
                               GhostCL[] ghosts, int code,
                               int[][] dotDist, int[][] powDist, int[][] dangerDist,
                               boolean ignoreDanger) {

        int[] dirs = {Game.UP, Game.LEFT, Game.DOWN, Game.RIGHT};
        int bestDir = Game.STAY;
        int bestScore = Integer.MIN_VALUE;

        for (int d : dirs) {
            int nx = wrap(px + dx(d), w), ny = wrap(py + dy(d), h);
            if (blocked[nx][ny]) continue;
            if (isDangerGhostAt(nx, ny, ghosts, code, w, h)) continue;

            int dd = dotDist[nx][ny];
            int pd = powDist[nx][ny];

            // If no dots reachable, fallback will handle power
            int dotTerm = (dd < 0) ? -9999 : -dd;

            int safety = 50;
            if (!ignoreDanger && dangerDist != null) safety = dangerVal(dangerDist[nx][ny]);

            // avoid stepping into immediate kill unless truly forced
            if (!ignoreDanger && dangerDist != null && safety <= KILL_ZONE) continue;

            int reward = 0;
            if (b[nx][ny] == DOT) reward += 1200;
            if (b[nx][ny] == POWER) reward += 400; // small bonus; DOT is main win path

            int exits = exitsCount(nx, ny, blocked, w, h);
            int loopPenalty = isRecentPos(nx, ny) ? LOOP_PENALTY : 0;

            // DOT dominates when safe; safety only lightly affects unless close
            int score =
                    dotTerm * 320 +            // <<< main engine (very strong)
                            reward +
                            exits * 55 -               // prefer junctions => less corner-sticking
                            loopPenalty;

            if (!ignoreDanger && dangerDist != null) {
                // light safety shaping (doesn't over-camp)
                if (safety <= SOFT_DANGER) score += safety * 120;
                else score += 600; // when very safe, don't care much
            }

            // tiny keep-direction bias (reduces jitter)
            if (d == lastDir) score += 20;

            // tiny reverse penalty
            if (opposite(lastDir) == d) score -= 15;

            // if no dot path from that neighbor, discourage
            if (dd < 0) score -= 2000;

            // if power is free, allow taking it sometimes
            if (pd >= 0 && pd <= POWER_FREE_DIST) score += 200;

            if (score > bestScore) {
                bestScore = score;
                bestDir = d;
            }
        }

        return bestDir;
    }

    // ===================== POWER MOVE =====================

    private int choosePower(int px, int py, int[][] b, boolean[][] blocked, int w, int h,
                            GhostCL[] ghosts, int code,
                            int[][] powDist, int[][] dangerDist) {
        int[] dirs = {Game.UP, Game.LEFT, Game.DOWN, Game.RIGHT};
        int bestDir = Game.STAY;
        int bestScore = Integer.MIN_VALUE;

        for (int d : dirs) {
            int nx = wrap(px + dx(d), w), ny = wrap(py + dy(d), h);
            if (blocked[nx][ny]) continue;
            if (isDangerGhostAt(nx, ny, ghosts, code, w, h)) continue;

            int pd = powDist[nx][ny];
            if (pd < 0) continue;

            int safety = (dangerDist == null) ? 50 : dangerVal(dangerDist[nx][ny]);
            if (dangerDist != null && safety <= KILL_ZONE) continue;

            int exits = exitsCount(nx, ny, blocked, w, h);
            int reward = (b[nx][ny] == POWER) ? 900 : 0;

            int score = (-pd) * 260 + reward + exits * 35;

            if (dangerDist != null) score += safety * 160;

            if (score > bestScore) {
                bestScore = score;
                bestDir = d;
            }
        }
        return bestDir;
    }

    // ===================== ESCAPE =====================

    private int escapeMove(int px, int py, int[][] b, boolean[][] blocked, int w, int h,
                           GhostCL[] ghosts, int code,
                           int[][] dangerDist, int[][] dotDist) {
        int[] dirs = {Game.UP, Game.LEFT, Game.DOWN, Game.RIGHT};
        int bestDir = Game.STAY;
        int bestScore = Integer.MIN_VALUE;

        for (int d : dirs) {
            int nx = wrap(px + dx(d), w), ny = wrap(py + dy(d), h);
            if (blocked[nx][ny]) continue;
            if (isDangerGhostAt(nx, ny, ghosts, code, w, h)) continue;

            int safety = dangerVal(dangerDist[nx][ny]);
            int exits = exitsCount(nx, ny, blocked, w, h);

            // while escaping: safety dominates, but still prefer moves that don't ruin dot progress forever
            int dd = dotDist[nx][ny];
            int dotTerm = (dd < 0) ? -9999 : -dd;

            int score =
                    safety * 3000 +
                            exits * 180 +
                            dotTerm * 40 -
                            (isRecentPos(nx, ny) ? 200 : 0);

            if (score > bestScore) {
                bestScore = score;
                bestDir = d;
            }
        }

        return bestDir;
    }

    // ===================== UNSTICK =====================

    private int forceDotProgress(int px, int py, int[][] b, boolean[][] blocked, int w, int h,
                                 GhostCL[] ghosts, int code,
                                 int[][] dotDist, int[][] dangerDist) {
        int[] dirs = {Game.UP, Game.LEFT, Game.DOWN, Game.RIGHT};
        int bestDir = Game.STAY;
        int best = Integer.MIN_VALUE;

        for (int d : dirs) {
            int nx = wrap(px + dx(d), w), ny = wrap(py + dy(d), h);
            if (blocked[nx][ny]) continue;
            if (isDangerGhostAt(nx, ny, ghosts, code, w, h)) continue;

            int dd = dotDist[nx][ny];
            if (dd < 0) continue;

            int safety = dangerVal(dangerDist[nx][ny]);
            if (safety <= KILL_ZONE) continue;

            int val = (-dd) * 1000 + safety * 40 - (isRecentPos(nx, ny) ? 2000 : 0);
            if (val > best) {
                best = val;
                bestDir = d;
            }
        }
        return bestDir;
    }

    // ===================== BFS MAPS =====================

    // Multi-source BFS from all cells with value target (DOT/POWER). -1 unreachable.
    private int[][] multiSourceDistToValue(int[][] b, boolean[][] blocked, int w, int h, int target) {
        int[][] dist = new int[w][h];
        for (int x = 0; x < w; x++) for (int y = 0; y < h; y++) dist[x][y] = -1;

        int max = w * h;
        int[] qx = new int[max];
        int[] qy = new int[max];
        int head = 0, tail = 0;

        for (int x = 0; x < w; x++) for (int y = 0; y < h; y++) {
            if (blocked[x][y]) continue;
            if (b[x][y] == target) {
                dist[x][y] = 0;
                qx[tail] = x; qy[tail] = y; tail++;
            }
        }

        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};

        while (head < tail) {
            int x = qx[head], y = qy[head];
            int d = dist[x][y];
            head++;

            for (int k = 0; k < 4; k++) {
                int nx = wrap(x + dx[k], w);
                int ny = wrap(y + dy[k], h);
                if (blocked[nx][ny]) continue;
                if (dist[nx][ny] != -1) continue;
                dist[nx][ny] = d + 1;
                qx[tail] = nx; qy[tail] = ny; tail++;
            }
        }
        return dist;
    }

    // Multi-source BFS from all danger ghosts. -1 means no danger reachable (treat as very safe).
    private int[][] multiSourceDangerDist(boolean[][] blocked, int w, int h, GhostCL[] ghosts, int code) {
        int[][] dist = new int[w][h];
        for (int x = 0; x < w; x++) for (int y = 0; y < h; y++) dist[x][y] = -1;
        if (ghosts == null) return dist;

        int max = w * h;
        int[] qx = new int[max];
        int[] qy = new int[max];
        int head = 0, tail = 0;

        for (GhostCL g : ghosts) {
            if (g == null) continue;
            if (g.remainTimeAsEatable(code) > 0) continue; // danger only
            int[] gp = parseXY(g.getPos(code));
            int gx = wrap(gp[0], w), gy = wrap(gp[1], h);
            if (blocked[gx][gy]) continue;
            if (dist[gx][gy] == -1) {
                dist[gx][gy] = 0;
                qx[tail] = gx; qy[tail] = gy; tail++;
            }
        }

        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};

        while (head < tail) {
            int x = qx[head], y = qy[head];
            int d = dist[x][y];
            head++;

            for (int k = 0; k < 4; k++) {
                int nx = wrap(x + dx[k], w);
                int ny = wrap(y + dy[k], h);
                if (blocked[nx][ny]) continue;
                if (dist[nx][ny] != -1) continue;
                dist[nx][ny] = d + 1;
                qx[tail] = nx; qy[tail] = ny; tail++;
            }
        }
        return dist;
    }

    // ===================== SMALL HELPERS =====================

    private int exitsCount(int x, int y, boolean[][] blocked, int w, int h) {
        int exits = 0;
        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};
        for (int k = 0; k < 4; k++) {
            int nx = wrap(x + dx[k], w);
            int ny = wrap(y + dy[k], h);
            if (!blocked[nx][ny]) exits++;
        }
        return exits;
    }

    private boolean anyEatable(GhostCL[] ghosts, int code) {
        if (ghosts == null) return false;
        for (GhostCL g : ghosts) if (g != null && g.remainTimeAsEatable(code) > 0) return true;
        return false;
    }

    private boolean isDangerGhostAt(int x, int y, GhostCL[] ghosts, int code, int w, int h) {
        if (ghosts == null) return false;
        for (GhostCL g : ghosts) {
            if (g == null) continue;
            if (g.remainTimeAsEatable(code) > 0) continue;
            int[] gp = parseXY(g.getPos(code));
            int gx = wrap(gp[0], w), gy = wrap(gp[1], h);
            if (gx == x && gy == y) return true;
        }
        return false;
    }

    private int anyLegal(int px, int py, int[][] b, boolean[][] blocked, int w, int h,
                         GhostCL[] ghosts, int code, int[][] dangerDist) {
        int[] dirs = {Game.UP, Game.LEFT, Game.DOWN, Game.RIGHT};
        int bestDir = Game.STAY;
        int best = Integer.MIN_VALUE;

        for (int d : dirs) {
            int nx = wrap(px + dx(d), w), ny = wrap(py + dy(d), h);
            if (blocked[nx][ny]) continue;
            if (isDangerGhostAt(nx, ny, ghosts, code, w, h)) continue;

            int safety = dangerVal(dangerDist[nx][ny]);
            int score = safety * 1000 - (isRecentPos(nx, ny) ? 200 : 0);

            if (score > best) { best = score; bestDir = d; }
        }
        return bestDir;
    }

    // ===================== LOOP MEMORY =====================

    private void pushPos(int x, int y) {
        long key = (((long) x) << 32) ^ (y & 0xffffffffL);
        lastPos.addLast(key);
        while (lastPos.size() > LOOP_MEM) lastPos.removeFirst();
    }

    private boolean isRecentPos(int x, int y) {
        long key = (((long) x) << 32) ^ (y & 0xffffffffL);
        for (long k : lastPos) if (k == key) return true;
        return false;
    }

    private int rememberAndReturn(int px, int py, int dir) {
        lastX = px;
        lastY = py;
        lastDir = dir;
        return dir;
    }

    // ===================== INIT + UTILS =====================

    private void initColors(int code) {
        DOT = Game.getIntColor(Color.PINK, code);
        POWER = Game.getIntColor(Color.GREEN, code);
        WALL = Game.getIntColor(Color.BLUE, code);   // <<< IMPORTANT for your map
        inited = true;
    }

    private static int dangerVal(int d) {
        return (d < 0) ? 50 : Math.min(d, 50);
    }

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

    private static int wrap(int v, int mod) {
        int r = v % mod;
        if (r < 0) r += mod;
        return r;
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
}
