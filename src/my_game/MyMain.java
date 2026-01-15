package my_game;

/**
 * Runs the game using Ex3Algo (no keyboard control).//
 */
public class MyMain {

    public static void main(String[] args) {

        int code = 0;

        MyPacmanGame game = new MyPacmanGame();

        String level = "level4";
        boolean cyclic = true;
        long seed = 1L;
        double dt = 0.1;

        game.init(code, level, cyclic, seed, dt, 0, 0);

        // connect algo
        game.setAlgo(code, new Ex3Algo());

        game.play();

        MyGui gui = new MyGui(game, code);

        while (game.getStatus() != PacManGame.DONE && game.getStatus() != PacManGame.ERR) {
            game.move(code);   // engine calls algo internally now ✅
            gui.draw();

            StdDraw.pause(30);
        }

        System.out.println(game.getData(code));
        System.out.println(game.end(code));
    }
}
