package my_game;

/**
 * MyGameMap - holds the board and helps build it from ASCII.
 *
 * This class is optional, but recommended to keep MyPacmanGame clean.
 */
public class MyGameMap {

    private final int[][] board;
    private final int w, h;

    public MyGameMap(String[] rows) {
        this.h = rows.length;
        this.w = rows[0].length();
        this.board = new int[w][h];

        // store with y increasing upwards (UP means y+1)
        for (int y = 0; y < h; y++) {
            String row = rows[h - 1 - y]; // reverse so y goes "up"
            for (int x = 0; x < w; x++) {
                char c = row.charAt(x);
                board[x][y] = switch (c) {
                    case '#' -> MyPacmanGame.WALL;
                    case '.' -> MyPacmanGame.DOT;
                    case 'o' -> MyPacmanGame.POWER;
                    default  -> MyPacmanGame.EMPTY;
                };
            }
        }
    }

    public int[][] getBoard() { return board; }
    public int getW() { return w; }
    public int getH() { return h; }

    public boolean hasDotsOrPower() {
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int v = board[x][y];
                if (v == MyPacmanGame.DOT || v == MyPacmanGame.POWER) return true;
            }
        }
        return false;
    }
}
