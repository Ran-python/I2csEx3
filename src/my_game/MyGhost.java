package my_game;

import java.util.Random;

/**
 * MyGhost - server-side implementation of GhostCL.
 *
 * - Holds current position (x,y)
 * - Holds eatable timer (double)
 * - Moves each tick according to type:
 *   RANDOM_WALK0 / RANDOM_WALK1 / GREEDY_SP
 */
public class MyGhost implements GhostCL {

    private final int type;
    private int status = INIT;

    private int x, y;
    private double eatableTime = 0.0;

    private final Random rnd;

    public MyGhost(int type, int x, int y, Random rnd) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.rnd = rnd;
    }

    // ===== Engine hooks (server internal) =====

    public void setStatus(int s) {
        this.status = s;
    }

    public void setEatableTime(double t) {
        // keep the maximum (if you eat another power while already eatable)
        this.eatableTime = Math.max(this.eatableTime, t);
    }

    public void tickEatable(double dt) {
        if (eatableTime > 0) {
            eatableTime -= dt;
            if (eatableTime < 0) eatableTime = 0;
        }
    }

    public int getX() { return x; }
    public int getY() { return y; }

    public void respawn(int nx, int ny) {
        this.x = nx;
        this.y = ny;
        this.eatableTime = 0;
    }

    /**
     * One ghost step (called once per tick by MyPacmanGame).
     */
    public void step(MyPacmanGame game, int pacX, int pacY) {
        if (status != PLAY) return;

        int dir;

        // If eatable -> usually random (run away logic could be added later)
        if (eatableTime > 0) {
            dir = randomMove(game);
        } else {
            if (type == GREEDY_SP) dir = greedyMove(game, pacX, pacY);
            else dir = randomMove(game);
        }

        int nx = game.stepX(x, dir);
        int ny = game.stepY(y, dir);

        if (game.isPassable(nx, ny)) {
            x = nx;
            y = ny;
        }
    }

    private int randomMove(MyPacmanGame game) {
        int[] dirs = {PacManGame.UP, PacManGame.LEFT, PacManGame.DOWN, PacManGame.RIGHT};

        // try a few random attempts
        for (int i = 0; i < 8; i++) {
            int d = dirs[rnd.nextInt(dirs.length)];
            int nx = game.stepX(x, d);
            int ny = game.stepY(y, d);
            if (game.isPassable(nx, ny)) return d;
        }

        // fallback deterministic
        for (int d : dirs) {
            int nx = game.stepX(x, d);
            int ny = game.stepY(y, d);
            if (game.isPassable(nx, ny)) return d;
        }

        return PacManGame.STAY;
    }

    /**
     * Simple greedy: choose a legal move that minimizes Manhattan distance to Pac-Man.
     * (This is not BFS shortest path, but it's stable and fast.)
     */
    private int greedyMove(MyPacmanGame game, int pacX, int pacY) {
        int[] dirs = {PacManGame.UP, PacManGame.LEFT, PacManGame.DOWN, PacManGame.RIGHT};

        int bestDir = PacManGame.STAY;
        int bestDist = Integer.MAX_VALUE;

        for (int d : dirs) {
            int nx = game.stepX(x, d);
            int ny = game.stepY(y, d);
            if (!game.isPassable(nx, ny)) continue;

            int dist = Math.abs(nx - pacX) + Math.abs(ny - pacY);
            if (dist < bestDist) {
                bestDist = dist;
                bestDir = d;
            }
        }

        if (bestDir != PacManGame.STAY) return bestDir;
        return randomMove(game);
    }

    // ===== GhostCL interface =====

    @Override
    public int getType() {
        return type;
    }

    @Override
    public String getPos(int code) {
        return x + "," + y;
    }

    @Override
    public String getInfo() {
        return "MyGhost(type=" + type + ", x=" + x + ", y=" + y + ", eatable=" + eatableTime + ")";
    }

    @Override
    public double remainTimeAsEatable(int code) {
        return eatableTime;
    }

    @Override
    public int getStatus() {
        return status;
    }
}
