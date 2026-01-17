package my_game;

import java.awt.Color;

public class MyGui {

    private final PacManGame game;
    private final int code;

    private int w = -1;
    private int h = -1;

    private int lastPacX = Integer.MIN_VALUE;
    private int lastPacY = Integer.MIN_VALUE;
    private double pacAngle = 0.0; // rotation in degrees


    private final String pacImg = "p1.png";
    private final String[] ghostImgs = {"g0.png", "g1.png", "g2.png", "g3.png"};

    public MyGui(PacManGame game, int code) {
        this.game = game;
        this.code = code;
        setupCanvas();
    }

    private void setupCanvas() {
        int[][] b = game.getGame(code);
        w = b.length;
        h = b[0].length;

        StdDraw.setCanvasSize(w * 32, h * 32);
        StdDraw.setXscale(0, w);
        StdDraw.setYscale(0, h);
        StdDraw.enableDoubleBuffering();
    }

    public void draw() {
        int[][] b = game.getGame(code);
        if (b.length != w || b[0].length != h) setupCanvas();

        StdDraw.clear(Color.BLACK);

        drawBoard(b);
        drawPacman();
        drawGhosts();

        StdDraw.show();
    }

    private void drawBoard(int[][] b) {
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                double cx = x + 0.5;
                double cy = y + 0.5;

                int v = b[x][y];

                if (v == MyPacmanGame.WALL) {
                    StdDraw.setPenColor(new Color(0, 80, 255));
                    StdDraw.square(cx, cy, 0.5);

                } else if (v == MyPacmanGame.DOT) {
                    StdDraw.setPenColor(Color.PINK);
                    StdDraw.filledCircle(cx, cy, 0.06);

                } else if (v == MyPacmanGame.POWER) {
                    StdDraw.setPenColor(Color.GREEN);
                    StdDraw.filledCircle(cx, cy, 0.18);
                }
            }
        }
    }

    private void drawPacman() {
        int[] p = parseXY(game.getPos(code));
        int x = p[0];
        int y = p[1];

        // קביעת כיוון לפי תזוזה
        if (lastPacX != Integer.MIN_VALUE) {
            int dx = x - lastPacX;
            int dy = y - lastPacY;

            if (dx > 0) pacAngle = 0;        // RIGHT
            else if (dx < 0) pacAngle = 180; // LEFT
            else if (dy > 0) pacAngle = 90;  // UP
            else if (dy < 0) pacAngle = -90; // DOWN
        }

        lastPacX = x;
        lastPacY = y;

        double px = x + 0.5;
        double py = y + 0.5;

        StdDraw.picture(px, py, pacImg, 1.0, 1.0, pacAngle);
    }

    private void drawGhosts() {
        GhostCL[] ghosts = game.getGhosts(code);
        if (ghosts == null) return;

        for (int i = 0; i < ghosts.length; i++) {
            GhostCL g = ghosts[i];
            if (g == null) continue; // ✅ eaten -> disappears

            int[] xy = parseXY(g.getPos(code));
            double gx = xy[0] + 0.5;
            double gy = xy[1] + 0.5;

            if (g.remainTimeAsEatable(code) > 0) {
                StdDraw.setPenColor(Color.RED);
                StdDraw.filledCircle(gx, gy, 0.20);
            } else {
                String img = ghostImgs[i % ghostImgs.length];
                StdDraw.picture(gx, gy, img, 1.0, 1.0);
            }
        }
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
}
