package my_game;

import java.util.Random;

public class MyGhost implements GhostCL {

    private final int type;
    private int status = INIT;

    private int x, y;
    private double eatableTime = 0.0;

    private final Random rnd;


    private int lastDir = PacManGame.STAY;

    public MyGhost(int type, int x, int y, Random rnd) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.rnd = rnd;
    }

    public void setStatus(int s) { this.status = s; }

    public void setEatableTime(double t) {
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

    @Override
    public int getType() { return type; }

    @Override
    public String getPos(int code) { return x + "," + y; }

    @Override
    public String getInfo() {
        return "MyGhost(type=" + type + ", x=" + x + ", y=" + y +
                ", eatable=" + eatableTime + ", lastDir=" + lastDir + ")";
    }

    @Override
    public double remainTimeAsEatable(int code) { return eatableTime; }

    @Override
    public int getStatus() { return status; }

    // ================= Movement =================

    public void step(MyPacmanGame game, int pacX, int pacY) {
        if (status != PLAY) return;

        int dir;

        if (eatableTime > 0) {
            dir = smartRandomMove(game);
        } else {
            if (type == GREEDY_SP) dir = greedySmartMove(game, pacX, pacY);
            else dir = smartRandomMove(game);
        }

        int nx = game.stepX(x, dir);
        int ny = game.stepY(y, dir);

        if (game.isPassable(nx, ny)) {
            x = nx;
            y = ny;
            lastDir = dir;
        } else {

            int fallback = smartRandomMove(game);
            int fx = game.stepX(x, fallback);
            int fy = game.stepY(y, fallback);
            if (game.isPassable(fx, fy)) {
                x = fx; y = fy; lastDir = fallback;
            }
        }
    }


    private int smartRandomMove(MyPacmanGame game) {
        int[] dirs = {PacManGame.UP, PacManGame.LEFT, PacManGame.DOWN, PacManGame.RIGHT};

        int back = opposite(lastDir);
        int[] candidates = new int[4];
        int c = 0;

        for (int d : dirs) {
            int nx = game.stepX(x, d);
            int ny = game.stepY(y, d);
            if (!game.isPassable(nx, ny)) continue;
            if (d == back) continue;
            candidates[c++] = d;
        }

        if (c == 0) {
            for (int d : dirs) {
                int nx = game.stepX(x, d);
                int ny = game.stepY(y, d);
                if (game.isPassable(nx, ny)) candidates[c++] = d;
            }
        }

        if (c == 0) return PacManGame.STAY;
        return candidates[rnd.nextInt(c)];
    }

        private int greedySmartMove(MyPacmanGame game, int pacX, int pacY) {
        int[] dirs = {PacManGame.UP, PacManGame.LEFT, PacManGame.DOWN, PacManGame.RIGHT};
        int back = opposite(lastDir);

        int bestDir = PacManGame.STAY;
        int bestDist = Integer.MAX_VALUE;

        for (int pass = 0; pass < 2; pass++) {
            for (int d : dirs) {
                if (pass == 0 && d == back) continue; // pass 0: בלי פרסה
                int nx = game.stepX(x, d);
                int ny = game.stepY(y, d);
                if (!game.isPassable(nx, ny)) continue;

                int dist = manhattanCyclic(game, nx, ny, pacX, pacY);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestDir = d;
                }
            }
            if (bestDir != PacManGame.STAY) break;
        }


        if (bestDir == PacManGame.STAY) return smartRandomMove(game);
        return bestDir;
    }

    private int manhattanCyclic(MyPacmanGame game, int x1, int y1, int x2, int y2) {

        int[][] b = game.getGame(0);
        int w = b.length;
        int h = b[0].length;

        int dx = Math.abs(x1 - x2);
        int dy = Math.abs(y1 - y2);

        if (game.isCyclic()) {
            dx = Math.min(dx, w - dx);
            dy = Math.min(dy, h - dy);
        }
        return dx + dy;
    }

    private static int opposite(int dir) {
        if (dir == PacManGame.UP) return PacManGame.DOWN;
        if (dir == PacManGame.DOWN) return PacManGame.UP;
        if (dir == PacManGame.LEFT) return PacManGame.RIGHT;
        if (dir == PacManGame.RIGHT) return PacManGame.LEFT;
        return PacManGame.STAY;
    }
}
