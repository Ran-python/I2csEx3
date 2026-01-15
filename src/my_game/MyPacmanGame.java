package my_game;

import java.util.Random;

public class MyPacmanGame implements PacManGame {

    // ===== Board cell values =====
    public static final int EMPTY = 0;
    public static final int DOT   = 10;   // pink
    public static final int POWER = 11;   // green
    public static final int WALL  = -999; // wall

    // ===== Tuning =====
    private static final double POWER_DURATION = 6.0;   // seconds (time units)
    private static final double RESPAWN_DELAY  = 1.2;   // how long ghost disappears after eaten
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

    private double dt = 0.1;
    private int ghostType = GhostCL.RANDOM_WALK0;

    private MyGhost[] ghosts = new MyGhost[0];
    private double[] respawnTimers = new double[0]; // ✅ if >0 -> ghost is currently "gone"

    // manual key (optional)
    private Character lastKey = null;

    // ===== Pac-Man direction input =====
    private int pendingPacDir = STAY;

    // ===== Spawns =====
    private int pacSpawnX = 1, pacSpawnY = 1;
    private int[] ghostSpawnX = {5, 7, 9, 10};
    private int[] ghostSpawnY = {5, 7, 5, 7};

    // ===== Algo hook =====
    private PacManAlgo algo;

    public void setAlgo(PacManAlgo algo) { this.algo = algo; }
    public void setAlgo(int code, PacManAlgo algo) { this.algo = algo; }
    public PacManAlgo getAlgo() { return this.algo; }

    public void setPacmanDirection(int dir) { this.pendingPacDir = dir; }

    @Override
    public String init(int code, String level, boolean cyclic, long seed, double dt, int wParam, int hParam) {
        this.cyclic = cyclic;
        this.seed = seed;
        this.rnd = new Random(seed);
        this.dt = dt;

        if (level != null && level.contains("4")) loadLevel4();
        else loadDefault();

        spawnEntities();

        this.status = INIT;
        this.score = 0;
        this.pendingPacDir = STAY;

        return "OK init(level=" + level + ", cyclic=" + cyclic + ", seed=" + seed + ")";
    }

    private void loadDefault() {
        String[] rows = {
                "###########",
                "#....o....#",
                "#.###.###.#",
                "#.........#",
                "###.#.#.###",
                "#....#....#",
                "###########"
        };
        fromAsciiSafe(rows);

        pacSpawnX = 1;
        pacSpawnY = 1;

        ghostSpawnX = new int[]{9, 9, 8, 8};
        ghostSpawnY = new int[]{1, 5, 1, 5};

        ghostType = GhostCL.GREEDY_SP;
    }

    private void loadLevel4() {
        // ⚠️ חובה: כל השורות ייעשו אותו אורך אוטומטית ע״י fromAsciiSafe
        String[] rows = {
                "#############################",
                "#...........##.............##",
                "#.#####.###.##.###.#####.####",
                "#.#...#............###..o####",
                "#.#.#.#####.####.#####.#.####",
                "#...#.......####.......#.####",
                "#.#.#####.#.####.#.#####.####",
                "#..o#####.#......#.#.......###",
                "#.#######.########.#######.###",
                "#.....###....##....###.....###",
                "#####.#.###.####.###.#.#######",
                "#...........#..#.....#.....###",
                "#.#########.#..#.#########.###",
                "#...........#.............o##",
                "#.#######.#.#.##.#.#######.##",
                "#.#.......#......#.........##",
                "#.#.#####.#.####.#########.##",
                "#o..#####..................##",
                "#############################"
        };

        fromAsciiSafe(rows);

        pacSpawnX = 23;
        pacSpawnY = 3;

        // 4 רוחות
        ghostSpawnX = new int[]{14, 15, 13, 16};
        ghostSpawnY = new int[]{8,  8,  7,  7};

        ghostType = GhostCL.GREEDY_SP;
    }

    /**
     * ✅ SAFE builder:
     * - makes map rectangular by padding short rows with '#'
     * - reverses y so UP means y+1
     */
    private void fromAsciiSafe(String[] rows) {
        h = rows.length;

        int maxW = 0;
        for (String r : rows) maxW = Math.max(maxW, r.length());
        w = maxW;

        board = new int[w][h];

        for (int y = 0; y < h; y++) {
            String row = rows[h - 1 - y]; // reverse so y goes up

            for (int x = 0; x < w; x++) {
                char c = (x < row.length()) ? row.charAt(x) : '#'; // pad with wall

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
        respawnTimers = new double[ghosts.length];

        for (int i = 0; i < ghosts.length; i++) {
            ghosts[i] = new MyGhost(ghostType, ghostSpawnX[i], ghostSpawnY[i], rnd);
            ghosts[i].setStatus(PLAY);
            respawnTimers[i] = 0.0;
        }
    }

    @Override
    public void play() { status = PLAY; }

    @Override
    public String move(int code) {
        if (status == DONE || status == ERR) return "DONE";
        if (status == PAUSE) return "PAUSE";
        if (status == INIT) status = PLAY;

        // algo decides pacman direction
        if (algo != null) {
            int dir = algo.move(this);
            setPacmanDirection(dir);
        }

        // move pacman
        stepPacman();

        // eat dot/power
        eatAtPacman();

        // tick ghost eatable + respawn timers
        for (int i = 0; i < ghosts.length; i++) {
            if (ghosts[i] != null) ghosts[i].tickEatable(dt);

            if (respawnTimers[i] > 0) {
                respawnTimers[i] -= dt;
                if (respawnTimers[i] <= 0) {
                    ghosts[i] = new MyGhost(ghostType, ghostSpawnX[i], ghostSpawnY[i], rnd);
                    ghosts[i].setStatus(PLAY);
                    respawnTimers[i] = 0.0;
                }
            }
        }

        // move ghosts
        for (int i = 0; i < ghosts.length; i++) {
            if (ghosts[i] == null) continue;
            ghosts[i].step(this, pacX, pacY);
        }

        // collisions
        if (handleCollisions()) {
            status = DONE;
            return "LOSE score=" + score;
        }

        // win
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

            // all ghosts become eatable
            for (int i = 0; i < ghosts.length; i++) {
                if (ghosts[i] != null) ghosts[i].setEatableTime(POWER_DURATION);
            }
        }
    }

    // ✅ THIS IS THE IMPORTANT FIX: ghost disappears (null) when eaten
    private boolean handleCollisions() {
        for (int i = 0; i < ghosts.length; i++) {
            MyGhost g = ghosts[i];
            if (g == null) continue;               // eaten -> not in game now
            if (respawnTimers[i] > 0) continue;    // just in case

            if (g.getX() == pacX && g.getY() == pacY) {
                if (g.remainTimeAsEatable(0) > 0) {
                    score += GHOST_EAT_SCORE;

                    // remove ghost and schedule respawn
                    ghosts[i] = null;
                    respawnTimers[i] = RESPAWN_DELAY;

                } else {
                    return true; // pacman dies
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

    // movement helpers
    public boolean isPassable(int x, int y) {
        int wx = wrapX(x);
        int wy = wrapY(y);
        return board[wx][wy] != WALL;
    }

    public int stepX(int x, int dir) { return wrapX(x + dx(dir)); }
    public int stepY(int y, int dir) { return wrapY(y + dy(dir)); }

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
        if (dir == UP) return 1;
        if (dir == DOWN) return -1;
        return 0;
    }

    // interface methods
    @Override public Character getKeyChar() { return lastKey; }
    @Override public String getPos(int code) { return pacX + "," + pacY; }
    @Override public GhostCL[] getGhosts(int code) { return ghosts; }
    @Override public int[][] getGame(int code) { return board; }

    @Override
    public String end(int code) {
        status = DONE;
        return "END score=" + score;
    }

    @Override
    public String getData(int code) {
        return "score=" + score + ", pos=" + getPos(code) + ", ghosts=" + ghosts.length;
    }

    @Override public int getStatus() { return status; }
    @Override public boolean isCyclic() { return cyclic; }
}
