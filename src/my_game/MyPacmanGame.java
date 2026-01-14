package my_game;

import java.util.Random;

/**
 * MyPacmanGame - full server-side game engine implementing PacManGame.
 *
 * Works without any exe.* dependency.
 * Provides:
 * - getGame(), getPos(), getGhosts()
 * - init(), play(), move(), end()
 * - cyclic wrap-around if enabled
 * - dots + power pellets
 * - power mode: ghosts become eatable for POWER_DURATION seconds
 */
public class MyPacmanGame implements PacManGame {

    // ===== Board cell values (you control them) =====
    public static final int EMPTY = 0;
    public static final int DOT   = 10;      // "pink"
    public static final int POWER = 11;      // "green"
    public static final int WALL  = -999;    // wall

    // ===== Tuning =====
    private static final double POWER_DURATION = 6.0; // seconds (or "time units")
    private static final int GHOST_EAT_SCORE = 50;
    private static final int DOT_SCORE = 1;
    private static final int POWER_SCORE = 5;

    // ===== Game state =====
    private int status = INIT;
    private boolean cyclic = true;

    private int[][] board;
    private int w, h;

    private int pacX, pacY;
    private int score = 0;

    private long seed = 1L;
    private Random rnd;

    private double dt = 0.1; // tick duration (time units per move)
    private int ghostType = GhostCL.RANDOM_WALK0;

    private MyGhost[] ghosts = new MyGhost[0];

    // manual key (optional)
    private Character lastKey = null;

    // ===== Pac-Man direction input =====
    // Since PacManGame.move(int) has no "dir" param, we store it here.
    private int pendingPacDir = STAY;

    // ===== Spawns =====
    private int pacSpawnX = 1, pacSpawnY = 1;
    private int[] ghostSpawnX = {5, 7};
    private int[] ghostSpawnY = {5, 7};

    // ----------------- Public helper for your Main/Algo wrapper -----------------

    /**
     * The algo (or wrapper) should call this before move(code).
     */
    public void setPacmanDirection(int dir) {
        this.pendingPacDir = dir;
    }

    // ----------------- Init / level loading -----------------

    @Override
    public String init(int code, String level, boolean cyclic, long seed, double dt, int wParam, int hParam) {
        this.cyclic = cyclic;
        this.seed = seed;
        this.rnd = new Random(seed);
        this.dt = dt;

        // You can interpret "level" however you want.
        // For now: if level contains "4" -> load level4-like map else load small default
        if (level != null && level.contains("4")) loadLevel4();
        else loadDefault();

        spawnEntities();

        this.status = INIT;
        this.score = 0;
        this.pendingPacDir = STAY;

        return "OK init(level=" + level + ", cyclic=" + cyclic + ", seed=" + seed + ")";
    }

    private void loadDefault() {
        // ASCII map:
        // # = wall
        // . = dot
        // o = power
        String[] rows = {
                "###########",
                "#....o....#",
                "#.###.###.#",
                "#.........#",
                "###.#.#.###",
                "#....#....#",
                "###########"
        };
        fromAscii(rows);

        pacSpawnX = 1; pacSpawnY = 1;
        ghostSpawnX = new int[]{9, 9};
        ghostSpawnY = new int[]{1, 5};

        ghostType = GhostCL.GREEDY_SP;
    }

    private void loadLevel4() {
        String[] rows = {
                "###############",
                "#....#....#...#",
                "#.##.#.##.#.#.#",
                "#o...#....#...#",
                "#.#####.#####.#",
                "#.....#.....#.#",
                "###.#.###.#.#.#",
                "#...#.....#...#",
                "#.#.#####.#.###",
                "#...#....#...o#",
                "###############"
        };
        fromAscii(rows);

        pacSpawnX = 1; pacSpawnY = 1;
        ghostSpawnX = new int[]{13, 13};
        ghostSpawnY = new int[]{1, 9};

        ghostType = GhostCL.GREEDY_SP;
    }

    /**
     * Builds board[x][y] from ASCII.
     * IMPORTANT: we store y so that UP means y+1 (like your algo).
     */
    private void fromAscii(String[] rows) {
        h = rows.length;
        w = rows[0].length();

        board = new int[w][h];

        for (int y = 0; y < h; y++) {
            String row = rows[h - 1 - y]; // reverse to make y go "up"
            for (int x = 0; x < w; x++) {
                char c = row.charAt(x);
                board[x][y] = switch (c) {
                    case '#' -> WALL;
                    case '.' -> DOT;
                    case 'o' -> POWER;
                    default  -> EMPTY;
                };
            }
        }
    }

    private void spawnEntities() {
        pacX = pacSpawnX;
        pacY = pacSpawnY;

        ghosts = new MyGhost[ghostSpawnX.length];
        for (int i = 0; i < ghosts.length; i++) {
            ghosts[i] = new MyGhost(ghostType, ghostSpawnX[i], ghostSpawnY[i], rnd);
            ghosts[i].setStatus(PLAY);
        }
    }

    // ----------------- Game loop -----------------

    @Override
    public void play() {
        status = PLAY;
    }

    @Override
    public String move(int code) {
        if (status == DONE || status == ERR) return "DONE";
        if (status == PAUSE) return "PAUSE";
        if (status == INIT) status = PLAY;

        // 1) move pacman
        stepPacman();

        // 2) eat dot/power
        eatAtPacman();

        // 3) tick eatable timers
        for (MyGhost g : ghosts) g.tickEatable(dt);

        // 4) move ghosts
        for (MyGhost g : ghosts) g.step(this, pacX, pacY);

        // 5) collisions
        if (handleCollisions()) {
            status = DONE;
            return "LOSE score=" + score;
        }

        // 6) win condition
        if (!hasDotsOrPower()) {
            status = DONE;
            return "WIN score=" + score;
        }

        return "OK score=" + score;
    }

    private void stepPacman() {
        int dir = pendingPacDir;
        if (dir == STAY) return;

        int nx = stepX(pacX, dir);
        int ny = stepY(pacY, dir);

        if (isPassable(nx, ny)) {
            pacX = nx;
            pacY = ny;
        }
    }

    private void eatAtPacman() {
        int v = board[pacX][pacY];

        if (v == DOT) {
            board[pacX][pacY] = EMPTY;
            score += DOT_SCORE;
        } else if (v == POWER) {
            board[pacX][pacY] = EMPTY;
            score += POWER_SCORE;

            // make all ghosts eatable
            for (MyGhost g : ghosts) {
                g.setEatableTime(POWER_DURATION);
            }
        }
    }

    /**
     * @return true if Pac-Man died this tick.
     */
    private boolean handleCollisions() {
        for (int i = 0; i < ghosts.length; i++) {
            MyGhost g = ghosts[i];

            if (g.getX() == pacX && g.getY() == pacY) {
                if (g.remainTimeAsEatable(0) > 0) {
                    // eat ghost
                    score += GHOST_EAT_SCORE;

                    // respawn ghost at its spawn
                    g.respawn(ghostSpawnX[i], ghostSpawnY[i]);
                } else {
                    // pacman dies
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasDotsOrPower() {
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int v = board[x][y];
                if (v == DOT || v == POWER) return true;
            }
        }
        return false;
    }

    // ----------------- Board movement helpers (CYCLIC) -----------------

    public boolean isPassable(int x, int y) {
        int wx = wrapX(x);
        int wy = wrapY(y);
        return board[wx][wy] != WALL;
    }

    public int stepX(int x, int dir) {
        return wrapX(x + dx(dir));
    }

    public int stepY(int y, int dir) {
        return wrapY(y + dy(dir));
    }

    private int wrapX(int x) {
        if (!cyclic) return clamp(x, 0, w - 1);
        x %= w;
        if (x < 0) x += w;
        return x;
    }

    private int wrapY(int y) {
        if (!cyclic) return clamp(y, 0, h - 1);
        y %= h;
        if (y < 0) y += h;
        return y;
    }

    private static int clamp(int v, int lo, int hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }

    private static int dx(int dir) {
        if (dir == LEFT) return -1;
        if (dir == RIGHT) return 1;
        return 0;
    }

    private static int dy(int dir) {
        // IMPORTANT: UP increases y (matches your algo code)
        if (dir == UP) return 1;
        if (dir == DOWN) return -1;
        return 0;
    }

    // ----------------- PacManGame interface read-only methods -----------------

    @Override
    public Character getKeyChar() {
        return lastKey;
    }

    @Override
    public String getPos(int code) {
        return pacX + "," + pacY;
    }

    @Override
    public GhostCL[] getGhosts(int code) {
        return ghosts;
    }

    @Override
    public int[][] getGame(int code) {
        return board;
    }

    @Override
    public String end(int code) {
        status = DONE;
        return "END score=" + score;
    }

    @Override
    public String getData(int code) {
        return "score=" + score + ", pos=" + getPos(code) + ", ghosts=" + ghosts.length;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public boolean isCyclic() {
        return cyclic;
    }
}
