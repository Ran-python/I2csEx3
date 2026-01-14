package my_game;

/**
 * PacmanGame (server-side API).
 *
 * This interface is what your Pac-Man algorithm (client) talks to.
 * You will implement this interface in your own server class (e.g. MyPacmanGame)
 * and then your algo (Ex3Algo) will call these methods every tick.
 *
 * Notes:
 * - Most methods receive an int "code" (usually 0 in the course).
 * - getPos/getGhosts/getGame are the main read-only methods the algo uses.
 * - move/play/end/init are used to control the game loop / state.
 */

public interface PacManGame {
    // ===== Game status values =====
    /** Game created but not started yet. */
    int INIT  = 0;
    /** Game is running. */
    int PLAY  = 1;
    /** Game is paused. */
    int PAUSE = 2;
    /** Game finished (win/lose/time over). */
    int DONE  = 3;
    /** Error status (something invalid happened). */
    int ERR   = -1;

    // ===== Movement direction codes =====
    /** Do not move this tick. */
    int STAY  = 0;
    /** Move up (y + 1). */
    int UP    = 1;
    /** Move left (x - 1). */
    int LEFT  = 2;
    /** Move down (y - 1). */
    int DOWN  = 3;
    /** Move right (x + 1). */
    int RIGHT = 4;

    /**
     * Returns the last pressed key in manual mode (if you implement manual play),
     * or null if no key was pressed / not supported.
     *
     * @return key char, or null
     */
    Character getKeyChar();

    /**
     * Returns Pac-Man position as a string formatted exactly like: "x,y".
     * Your algo parses this string.
     *
     * @param code game code (usually 0)
     * @return position string: "x,y"
     */
    String getPos(int code);

    /**
     * Returns an array of ghosts (server objects that implement GhostCL).
     * Your algo uses:
     * - g.getPos(code) -> "x,y"
     * - g.remainTimeAsEatable(code) -> double
     *
     * @param code game code (usually 0)
     * @return array of ghosts (may be empty, should not be null if possible)
     */
    GhostCL[] getGhosts(int code);

    /**
     * Returns the board matrix as int[][].
     *
     * Conventions used by the course jar:
     * - Walls are some constant int value (you choose, but keep it stable).
     * - DOT (pink) should be: Game.getIntColor(Color.PINK, code)
     * - POWER (green) should be: Game.getIntColor(Color.GREEN, code)
     * - Empty is usually 0
     *
     * IMPORTANT:
     * If the game is cyclic, moving off an edge should wrap around.
     *
     * @param code game code (usually 0)
     * @return board matrix [width][height]
     */
    int[][] getGame(int code);

    /**
     * Advances the game by one "tick".
     *
     * Typical design:
     * - If manual mode: use getKeyChar() to decide Pac-Man move.
     * - If algo mode: you may store last algo move or pass it in some other way.
     *
     * In the original jar this returns a String (often debug/info).
     * You can return "" or any useful info.
     *
     * @param code game code (usually 0)
     * @return optional message / debug text
     */
    String move(int code);

    /**
     * Starts or resumes the game loop (sets status to PLAY).
     * In your server you can implement this as: status = PLAY.
     */
    void play();

    /**
     * Ends the game and returns a summary string.
     * You can also update status to DONE.
     *
     * @param code game code (usually 0)
     * @return summary (score/time/win/lose)
     */
    String end(int code);

    /**
     * Returns game data / stats as a String.
     * In the jar this is often used for printing score/time/etc.
     *
     * @param code game code (usually 0)
     * @return data string (JSON or human-readable)
     */
    String getData(int code);

    /**
     * Returns current status: INIT / PLAY / PAUSE / DONE / ERR.
     *
     * @return status code
     */
    int getStatus();

    /**
     * @return true if this board is cyclic (wrap-around at edges).
     */
    boolean isCyclic();

    /**
     * Initializes a new game.
     *
     * The course jar uses a String parameter (var2) for level/map name or path.
     * You can interpret it however you want (filename, level id, embedded map key).
     *
     * Suggested meanings (you decide and document them):
     * @param code   game code (usually 0)
     * @param level  level/map identifier (name/path)
     * @param cyclic true if wrap-around edges
     * @param seed   random seed (for ghosts movement / randomness)
     * @param dt     tick duration (seconds per step) OR speed factor
     * @param w      width or some config (depends on your implementation)
     * @param h      height or some config (depends on your implementation)
     *
     * @return status/info string ("" is fine, or "OK", or JSON)
     */
    String init(int code, String level, boolean cyclic, long seed, double dt, int w, int h);
}
