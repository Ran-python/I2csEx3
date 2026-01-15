package my_game;

import java.util.Random;

public class MyGhost implements GhostCL {

    private final int type;
    private int status = INIT;

    private int x, y;
    private double eatableTime = 0.0;

    private boolean dead = false;
    private double respawnTimer = 0.0;
    private int spawnX, spawnY;

    private final Random rnd;

    public MyGhost(int type, int x, int y, Random rnd) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.spawnX = x;
        this.spawnY = y;
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

    // ✅ חדש: טיק לריספאון
    public void tickRespawn(double dt) {
        if (!dead) return;
        respawnTimer -= dt;
        if (respawnTimer <= 0) {
            dead = false;
            respawnTimer = 0;
            respawn(spawnX, spawnY);
        }
    }

    // ✅ חדש: להרוג רוח לזמן מסוים
    public void killFor(double seconds, int sx, int sy) {
        dead = true;
        respawnTimer = seconds;
        spawnX = sx;
        spawnY = sy;
        // שמים מחוץ למפה כדי שלא "יתנגש"
        x = -9999;
        y = -9999;
        eatableTime = 0;
    }

    // ✅ חדש: לבדיקה ב-GUI
    public boolean isDead() {
        return dead;
    }

    public int getX() { return x; }
    public int getY() { return y; }

    public void respawn(int nx, int ny) {
        this.x = nx;
        this.y = ny;
        this.eatableTime = 0;
    }

    public void step(MyPacmanGame game, int pacX, int pacY) {
        if (status != PLAY) return;
        if (dead) return; // ✅ אם מתה — לא זזה

        int dir;

        if (eatableTime > 0) dir = randomMove(game);
        else {
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

        for (int i = 0; i < 8; i++) {
            int d = dirs[rnd.nextInt(dirs.length)];
            int nx = game.stepX(x, d);
            int ny = game.stepY(y, d);
            if (game.isPassable(nx, ny)) return d;
        }

        for (int d : dirs) {
            int nx = game.stepX(x, d);
            int ny = game.stepY(y, d);
            if (game.isPassable(nx, ny)) return d;
        }

        return PacManGame.STAY;
    }

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

    @Override
    public int getType() { return type; }

    @Override
    public String getPos(int code) { return x + "," + y; }

    @Override
    public String getInfo() {
        return "MyGhost(type=" + type + ", x=" + x + ", y=" + y +
                ", eatable=" + eatableTime + ", dead=" + dead + ")";
    }

    @Override
    public double remainTimeAsEatable(int code) { return eatableTime; }

    @Override
    public int getStatus() { return status; }
}