import javafx.scene.control.ListCell;
import javafx.scene.paint.Color;

public class AnnotationFormatCell extends ListCell<String> {

    //No constructor.

    public static Color get_color(String item) {
        item = " " + item + " ";

        if(item.contains(" !! "))
            return(Color.GREEN);
        else if(item.contains(" ! "))
            return(Color.DARKGREEN);
        else if(item.contains(" ?? "))
            return(Color.RED);
        else if(item.contains(" ? "))
            return(Color.DARKRED);
        else if(item.contains(" !? "))
            return(Color.DARKBLUE);
        else if(item.contains(" ?! "))
            return(Color.LIGHTBLUE);
        else if(item.contains(" - "))
            return(Color.BLACK);
        //Default
        return Color.GRAY;

    }

    @Override protected void updateItem(String item, boolean empty) {
        // calling super here is very important - don't skip this!
        super.updateItem(item, empty);
        setText(item);
        if(item == null)
            return;
        Color c = get_color(item);

        if(!c.equals(Color.GRAY))
            setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        else
            setStyle("-fx-font-size: 20px;");

        setTextFill(c);
    }
}