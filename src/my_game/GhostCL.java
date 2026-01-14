package my_game;

/**
        * GhostCL (Ghost Client / Logic).
        *
        * This interface represents a single ghost on the server side.
        * Your server implementation (e.g. MyGhost) must implement this interface.
        *
        * The Pac-Man algorithm (client) treats ghosts as read-only objects:
        * - It queries their position
 * - It checks whether they are currently eatable
 * - It may use their type/status for logic
 *
         * IMPORTANT:
        * - remainTimeAsEatable(...) MUST return double
 * - getPos(...) MUST return a string formatted exactly as "x,y"
        */

public interface GhostCL {
    // ===== Ghost status values =====
    /** Ghost created but not active yet. */
    int INIT  = 0;

    /** Ghost is active and moving. */
    int PLAY  = 1;

    /** Ghost is paused (if your server supports pause). */
    int PAUSE = 2;

    // ===== Ghost movement / AI types =====
    /** Completely random movement (simple). */
    int RANDOM_WALK0 = 10;

    /** Slightly smarter random movement. */
    int RANDOM_WALK1 = 11;

    /** Greedy shortest-path movement (e.g., BFS toward Pac-Man). */
    int GREEDY_SP    = 12;

    /**
     * Returns the ghost movement/AI type.
     * This value is optional for the algo, but useful for debugging and extensions.
     *
     * @return one of RANDOM_WALK0, RANDOM_WALK1, GREEDY_SP
     */
    int getType();

    /**
     * Returns the ghost position as a string formatted exactly as: "x,y".
     *
     * Your Pac-Man algorithm parses this string directly,
     * so the format MUST NOT change.
     *
     * @param code game code (usually 0)
     * @return position string: "x,y"
     */
    String getPos(int code);

    /**
     * Returns human-readable information about this ghost.
     * Useful for debugging, logs, or GUI display.
     *
     * Example:
     * "Ghost(type=GREEDY_SP, x=5, y=7, eatable=3.2)"
     *
     * @return info string
     */
    String getInfo();

    /**
     * Returns how much time (in ticks or seconds – your choice, but consistent)
     * this ghost is still eatable by Pac-Man.
     *
     * IMPORTANT:
     * - If return value > 0 → ghost is eatable
     * - If return value <= 0 → ghost is dangerous
     *
     * Your algorithm heavily relies on this method.
     *
     * @param code game code (usually 0)
     * @return remaining eatable time (double)
     */
    double remainTimeAsEatable(int code);

    /**
     * Returns the current ghost status.
     *
     * @return INIT / PLAY / PAUSE
     */
    int getStatus();
}
