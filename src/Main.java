import javafx.application.Application;
import javafx.scene.*;
import javafx.stage.*;

import java.io.*;

/** Main class to start the ChessDB-Remastered Application
 * @author Kevin Moy **/
public class Main extends Application {
    public GUI gui;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    /** JavaFX application entry point. Runs after init(), which we do NOT override.
     * @param primaryStage stage where application SCENE can be set.
     *                     If applet, primaryStage embedded into browser.
     */
    public void start(Stage primaryStage) {
        try {
            new File("book").mkdir();
        }
        catch(Exception e) {}

        Board.init_class();
        System.out.println("ChessDB-Remastered Initialized");

        primaryStage.setTitle("ChessDB-Remastered");
        Group root = new Group();
        gui = new GUI(primaryStage);
        root.getChildren().add(gui.AppBox);
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
        System.out.println("ChessDB-Remastered Start");
    }

    @Override
    public void stop() {
        gui.shutdown(); //Does nothing atm
        System.out.println("ChessDB-Remastered Finish");
    }
}
