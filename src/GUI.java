import javafx.stage.*;

import javafx.scene.layout.HBox;

public class GUI {
    private Board board; //Board for display
    private Game g; //Game class.
    private Stage stage; //Top-Level Container (Window): Like JFrame in Swing

    public HBox AppBox = new HBox(2); //Box contains board + book move + movelist + other features.

    public void shutdown() {
        //board.stop_engine_process();
        //Do nothing, for now.
    }

    /**Initialize stage, board, and create a game instance from the stage + board.**/
    public GUI(Stage set_s) {
        stage = set_s;
        board = new Board(true);
        board.s = stage;
        g = new Game(stage, board);

        g.reset(board.getFEN());
        board.g = g;

        AppBox.getChildren().add(board.main_box);
        AppBox.getChildren().add(g.vertical_box);
    }

}
