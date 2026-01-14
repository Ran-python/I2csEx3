package my_game;

public class MyMain {
    public static void main(String[] args) {
        int code = 0;

        MyPacmanGame g = new MyPacmanGame();
        g.init(code, "level4", true, 1234L, 0.1, 0, 0);
        g.play();

        MyGui gui = new MyGui();

        while (g.getStatus() == PacManGame.PLAY || g.getStatus() == PacManGame.INIT) {
            // פה בעתיד תכניס את האלגו שלך:
            // int dir = algo.move(g);
            // g.setPacmanDirection(dir);

            // כרגע: נעמוד במקום
            g.setPacmanDirection(PacManGame.STAY);

            g.move(code);
            gui.render(g, code);

            StdDraw.pause(40); // ~25 FPS
        }
        System.out.println("Game ended: " + g.end(code));
    }
}
