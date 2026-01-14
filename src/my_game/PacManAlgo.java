package my_game;

/**
 * PacManAlgo - the AI algorithm interface.
 *
 * Your algorithm should implement this interface.
 * The server (MyPacmanGame) can call algo.move(game) every tick
 * to get the next direction.
 */
public interface PacManAlgo {

    /**
     * @return short text describing the algorithm (optional).
     */
    default String getInfo() {
        return "PacManAlgo";
    }

    /**
     * Called once per tick to decide the next move.
     *
     * @param game the game API (server)
     * @return one of PacManGame.UP/DOWN/LEFT/RIGHT/STAY
     */
    int move(PacManGame game);
}
