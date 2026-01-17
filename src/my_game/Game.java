package my_game;

import java.awt.Color;

/**
 * Game utilities and constants (replacement for exe.ex3.game.Game).
 *
 * This class provides:
 * - Direction constants (UP/DOWN/LEFT/RIGHT/STAY)
 * - A stable int encoding for colors used by the board:
 *   PINK  -> DOT
 *   GREEN -> POWER
 *
 */
public final class Game {

    private Game() {}

    // Directions (match PacManGame)
    public static final int STAY  = PacManGame.STAY;
    public static final int UP    = PacManGame.UP;
    public static final int LEFT  = PacManGame.LEFT;
    public static final int DOWN  = PacManGame.DOWN;
    public static final int RIGHT = PacManGame.RIGHT;

    /**
     * Converts a Color into the board int value.
     * This mimics the course jar behavior but with your own encoding.
     *
     * @param c    color
     * @param code game code (unused, kept for signature compatibility)
     * @return int value representing that color on the board
     */
    public static int getIntColor(Color c, int code) {
        if (c == null) return 0;

        // map to your board constants
        if (Color.PINK.equals(c))  return MyPacmanGame.DOT;    // 10
        if (Color.GREEN.equals(c)) return MyPacmanGame.POWER;  // 11

        // fallback: encode RGB (not really needed now, but safe)
        return (c.getRed() << 16) | (c.getGreen() << 8) | (c.getBlue());
    }
}
