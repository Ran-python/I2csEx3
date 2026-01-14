package my_game;

import java.awt.Color;

/**
 * Simple Pac-Man GUI using StdDraw (your local StdDraw class).
 * Draws:
 * - Walls in blue outline
 * - Dots (DOT) as small pink/white points
 * - Power (POWER) as green circles
 * - Pac-Man (p1.png)
 * - Ghosts (g1..g4.png)
 */
public class MyGui {

    // cell value conventions (must match MyPacmanGame)
    private static final int EMPTY = MyPacmanGame.EMPTY;
    private static final int DOT   = MyPacmanGame.DOT;
    private static final int POWER = MyPacmanGame.POWER;
    private static final int WALL  = MyPacmanGame.WALL;

    // images (paths)
    private final String pacImg = "src/images/p1.png";
    private final String[] ghostImgs = {
            "src/images/g1.png",
            "src/images/g2.png",
            "src/images/g3.png",
            "src/images/g4.png"
    };

    // colors similar to the screenshot
    private final Color wallBlue = new Color(0, 80, 220);
    private final Color bgBlack  = Color.BLACK;
    private final Color dotPink  = new Color(255, 170, 210);
    private final Color powerGreen = new Color(0, 220, 0);

    // HUD text color
    private final Color hudColor = Color.WHITE;

    public MyGui() {
        // nice large window
        StdDraw.setCanvasSize(900, 900);
        StdDraw.enableDoubleBuffering();
    }

    public void render(PacManGame game, int code) {
        int[][] b = game.getGame(code);
        int w = b.length;
        int h = b[0].length;

        // coordinate system: cells are [0..w]x[0..h]
        StdDraw.setXscale(0, w);
        StdDraw.setYscale(0, h);

        // background
        StdDraw.clear(bgBlack);

        // draw board
        drawWallsDotsPower(b, w, h);

        // draw entities
        drawPacman(game, code);
        drawGhosts(game, code);

        // draw HUD (top text)
        drawHud(game, code, w, h);

        StdDraw.show();
    }

    private void drawWallsDotsPower(int[][] b, int w, int h) {
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int v = b[x][y];

                if (v == WALL) {
                    // blue outline square like the screenshot
                    StdDraw.setPenColor(wallBlue);
                    StdDraw.setPenRadius(0.004);
                    StdDraw.square(x + 0.5, y + 0.5, 0.48);
                }
                else if (v == DOT) {
                    // small dot (pink-ish)
                    StdDraw.setPenColor(dotPink);
                    StdDraw.setPenRadius(0.012);
                    StdDraw.point(x + 0.5, y + 0.5);
                }
                else if (v == POWER) {
                    // green circle
                    StdDraw.setPenColor(powerGreen);
                    StdDraw.setPenRadius(0.004);
                    StdDraw.filledCircle(x + 0.5, y + 0.5, 0.22);
                }
            }
        }
        // reset pen radius
        StdDraw.setPenRadius();
    }

    private void drawPacman(PacManGame game, int code) {
        String pos = game.getPos(code); // "x,y"
        int[] p = parsePos(pos);
        int x = p[0], y = p[1];

        // draw image centered in cell
        // scaled a bit smaller than cell so it looks clean
        StdDraw.picture(x + 0.5, y + 0.5, pacImg, 0.85, 0.85);
    }

    private void drawGhosts(PacManGame game, int code) {
        GhostCL[] gs = game.getGhosts(code);
        if (gs == null) return;

        for (int i = 0; i < gs.length; i++) {
            GhostCL g = gs[i];
            int[] p = parsePos(g.getPos(code));
            int gx = p[0], gy = p[1];

            String img = ghostImgs[i % ghostImgs.length];

            // if eatable -> draw a small blue aura behind (optional nice effect)
            if (g.remainTimeAsEatable(code) > 0) {
                StdDraw.setPenColor(new Color(50, 120, 255));
                StdDraw.filledCircle(gx + 0.5, gy + 0.5, 0.42);
            }

            StdDraw.picture(gx + 0.5, gy + 0.5, img, 0.85, 0.85);
        }
    }

    private void drawHud(PacManGame game, int code, int w, int h) {
        StdDraw.setPenColor(hudColor);

        // Put text slightly above the board top line
        // (since we scaled to 0..h, top is h; we place near h - 0.3)
        String data = game.getData(code);
        StdDraw.textLeft(0.2, h - 0.3, "Ex3: PacMan Game!   " + data);
    }

    private static int[] parsePos(String s) {
        // expects "x,y"
        if (s == null || s.isEmpty()) return new int[]{0, 0};
        String[] parts = s.split(",");
        int x = Integer.parseInt(parts[0].trim());
        int y = Integer.parseInt(parts[1].trim());
        return new int[]{x, y};
    }
}
