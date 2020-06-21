import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

import javafx.stage.*;

import javafx.stage.FileChooser;

import java.io.File;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.util.Set;

import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.scene.Group;

import javafx.scene.control.ListCell;
import javafx.util.Callback;

import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

import javax.swing.JOptionPane;


public class Game {
    List<BookMove> book_list;
    private Hashtable pgn_header_hash = new Hashtable();
    public HBox clip_box = new HBox(2);
    public HBox book_box = new HBox(2);
    public HBox save_pgn_box = new HBox(2);
    private TextField pgn_name_text = new TextField();
    private String pgn;
    String initial_dir = "";
    ListView<String> list = new ListView<String>();
    ListView blist = new ListView<String>();
    TextArea pgn_text = new TextArea();
    public VBox vertical_box = new VBox(2);
    FileChooser f = new FileChooser();
    private Stage s;
    private Board b;
    private TextArea game_text = new TextArea();
    final private int MAX_MOVES = 250;
    String[] pgn_lines = new String[MAX_MOVES];
    private String[] moves = new String[MAX_MOVES];
    public String initial_position;
    private String[] positions = new String[MAX_MOVES];
    int move_indices[] = new int[MAX_MOVES];
    int start_fen_end_index = 0;
    private int move_ptr = 0;
    private int game_ptr = 0;

    public Game(Stage s, Board b) {
        this.s = s;
        this.b = b;

        book_file = new MyFile("book.txt");
        book = new Hashtable();

        //Create buttons for clip_box (HBOX).
        Button openPGNButton = createOpenPGNButton();
        Button save_as_pgn_button = createSavePGNButton();
        Button save_to_pgn_button = createSaveToPGNButton();
        Button clip_to_fen_button = createClipFENButton();
        Button fen_to_clip_button = createFENClipButton();
        Button clip_to_pgn_button = createClipPGNButton();
        Button pgn_to_clip_button = createPGNClipButton();
        Button load_book_button = createLoadBookButton();
        Button save_book_button = createSaveBookButton();
        //Add buttons to clip box.
        clip_box.getChildren().add(openPGNButton);
        clip_box.getChildren().add(clip_to_fen_button);
        clip_box.getChildren().add(fen_to_clip_button);
        clip_box.getChildren().add(clip_to_pgn_button);
        clip_box.getChildren().add(pgn_to_clip_button);
        clip_box.getChildren().add(load_book_button);
        clip_box.getChildren().add(save_book_button);
        //Set book_box to contain display of book moves given current position.
        //
        list.setMaxWidth(115);
        book_box.getChildren().add(list);
        blist.setMinWidth(400);
        blist.setStyle("-fx-font-family: monospace;");
        blist.setCellFactory((Callback<ListView<String>, ListCell<String>>) list -> new AnnotationFormatCell());
        book_box.getChildren().add(blist);

        vertical_box.getChildren().add(clip_box);
        vertical_box.getChildren().add(book_box);

        Button start_deep_button = createStartDeepButton();

        int deep_height = 38;
        save_as_pgn_button.setMinHeight(deep_height);
        save_pgn_box.getChildren().add(save_as_pgn_button);
        pgn_name_text.setMinWidth(290);
        pgn_name_text.setStyle("-fx-font-size: " + (deep_height-18)
                + "px;-fx-font-family: monospace;-fx-font-weight: bold;");
        pgn_name_text.setOnMouseClicked(pgn_name_text_clicked);
        MyFile config = new MyFile("config.txt");
        String result = config.get_field("initial_dir");
        if(result != null) {
            pgn_name_text.setText(result+File.separator + "default.pgn");
        }
        save_pgn_box.getChildren().add(pgn_name_text);
        save_to_pgn_button.setMinHeight(deep_height);
        save_pgn_box.getChildren().add(save_to_pgn_button);
        start_deep_button.setMinHeight(deep_height);
        save_pgn_box.getChildren().add(start_deep_button);
        vertical_box.getChildren().add(save_pgn_box);
        pgn_text.setWrapText(true);
        pgn_text.setStyle("-fx-display-caret: false;");
        pgn_text.setMinHeight(224);
        pgn_text.setOnMouseClicked(mouseHandler);
        blist.setOnMouseClicked(mouseHandlerBook);
        vertical_box.getChildren().add(pgn_text);
        list.setOnMouseClicked((EventHandler<Event>) event -> {
            int selected = list.getSelectionModel().getSelectedIndex();
            String pos = initial_position;
            if(selected > 0) pos = positions[selected - 1];
            game_ptr = selected;
            this.b.set_from_fen_inner(pos, false);
            update_game();
        });
    }

    private Button createStartDeepButton() {
        Button start_deep_button = new Button();
        start_deep_button.setText("Start Deep");
        start_deep_button.setOnAction(e -> {
            if(!this.b.is_engine_installed()) {
                JOptionPane.showMessageDialog(null, "Please load a UCI engine!");
                return;
            }
            create_start_deep_group();
            start_deep_modal = new MyModal(start_deep_group,"Deep Analysis");
            start_deep_modal.setxy(15, 515);
            deep_stop_button.setOnAction(e1 -> interrupt_deep = true);
            this.b.stop_engine();
            this.b.list_legal_moves();
            deep_legal_move_list_buffer = new String[500];
            deep_legal_move_list_buffer_cnt = this.b.legal_move_list_buffer_cnt;
            deep_legal_move_list_buffer = Arrays.copyOfRange(this.b.legal_move_list_buffer, 0, deep_legal_move_list_buffer_cnt);
            Arrays.sort(deep_legal_move_list_buffer);
            System.out.println("no legal moves: " + this.b.legal_move_list_buffer_cnt);
            runnable_do_deep_thread = new MyRunnable();
            runnable_do_deep_thread.kind = "do_deep";
            runnable_do_deep_thread.b = this.b;
            do_deep_thread = new Thread(runnable_do_deep_thread);
            runnable_update_deep_thread = new MyRunnable();
            runnable_update_deep_thread.kind = "update_deep";
            runnable_update_deep_thread.b = this.b;
            update_deep_thread = new Thread(runnable_update_deep_thread);
            interrupt_deep = false;
            this.b.deep_going = true;
            deep_going = true;
            do_deep_thread.start();
            update_deep_thread.start();
            start_deep_modal.show_and_wait();
            this.b.deep_going = false;
        });
        return start_deep_button;
    }

    private Button createSaveBookButton() {
        Button save_book_button = new Button();
        save_book_button.setText("Save Book");
        save_book_button.setOnAction(e -> book_file.from_hash(book));
        return save_book_button;
    }

    private Button createLoadBookButton() {
        Button load_book_button = new Button();
        load_book_button.setText("Load Book");
        load_book_button.setOnAction(e -> {
            book = book_file.to_hash();
            update_game();
        });
        return load_book_button;
    }

    private Button createPGNClipButton() {
        Button pgn_to_clip_button = new Button();
        pgn_to_clip_button.setText("PGN->Clip");
        pgn_to_clip_button.setOnAction(e -> copy_content(getPGN()));
        return pgn_to_clip_button;
    }

    private Button createClipPGNButton() {
        Button clip_to_pgn_button = new Button();
        clip_to_pgn_button.setText("Clip->PGN");
        clip_to_pgn_button.setOnAction(e -> {
            String pgn = get_content();
            if(pgn != null) {
                pgn_lines = pgn.split("\\r?\\n");
                set_from_pgn_lines();
            }
        });
        return clip_to_pgn_button;
    }

    private Button createFENClipButton() {
        Button fen_to_clip_button = new Button();
        fen_to_clip_button.setText("Fen->Clip");
        fen_to_clip_button.setOnAction(e -> copy_content(this.b.getFEN()));
        return fen_to_clip_button;
    }

    private Button createClipFENButton() {
        Button clip_to_fen_button = new Button();
        clip_to_fen_button.setText("Clip->Fen");
        clip_to_fen_button.setOnAction(e -> {
            String fen = get_content();
            if(fen != null) {
                this.b.set_from_fen(fen);
                this.b.drawBoard();
            }
        });
        return clip_to_fen_button;
    }

    private Button createSaveToPGNButton() {
        Button save_to_pgn_button = new Button();
        save_to_pgn_button.setText("Save to PGN");
        save_to_pgn_button.setOnAction(e -> {
            look_for_initial_dir();
            if(initial_dir != "") {
                File dir = new File(initial_dir);
                f.setInitialDirectory(dir);
            }
            File file = f.showOpenDialog(this.s);

            if(file == null) return;
            String path = file.getPath();
            initial_dir = path.substring(0, path.lastIndexOf(File.separator));
            MyFile my_file = new MyFile(path);
            getPGN();
            my_file.content = pgn;
            my_file.write_content();
        });
        return save_to_pgn_button;
    }

    private Button createSavePGNButton() {
        Button save_as_pgn_button = new Button();
        save_as_pgn_button.setText("Save as: ");
        save_as_pgn_button.setOnAction(e -> {
            String path = pgn_name_text.getText();
            if(path.length() > 0) {
                MyFile my_file = new MyFile(path);
                getPGN();
                my_file.content = pgn;
                my_file.write_content();
                System.out.println("Saved to file: " + path + "\n\nContent:\n\n" + my_file.content);
            }
        });
        return save_as_pgn_button;
    }

    private Button createOpenPGNButton() {
        Button open_pgn_button = new Button();
        open_pgn_button.setText("Open PGN");
        open_pgn_button.setOnAction(e -> {
            look_for_initial_dir();
            if(initial_dir != "") {
                File dir = new File(initial_dir);
                f.setInitialDirectory(dir);
            }
            File file = f.showOpenDialog(this.s);
            if(file == null) return;
            String path = file.getPath();
            pgn_name_text.setText(path);
            initial_dir = path.substring(0, path.lastIndexOf(File.separator));
            MyFile config = new MyFile("config.txt");
            config.set_field("initial_dir", initial_dir);
            MyFile my_file = new MyFile(path);
            pgn_lines = my_file.read_lines();
            set_from_pgn_lines();
            highlight_name_in_path();
        });
        return open_pgn_button;
    }

    public void reset(String initial_fen) {
        sel_book_move = -1;
        move_ptr = 0;
        game_ptr = 0;
        initial_position = initial_fen;
        update_game();
    }

    private String fen_to_name(String fen) {
        return "book" + File.separator + Encode32.encode(fen, true) + ".txt";
    }

    private Hashtable get_pos(String fen) {
        fen = Board.fen_to_raw(fen);
        Object pos_obj = book.get(fen);

        if(pos_obj == null) {
            String name = fen_to_name(fen);
            File f = new File(name);
            if(f.exists()) {
                MyFile look_up = new MyFile(name);
                Hashtable pos = look_up.to_hash();
                book.put(fen,pos);
                return pos;
            }
            else {
                return new Hashtable();
            }
        }
        else {
            return (Hashtable) pos_obj;
        }
    }

    private void store_pos(String fen, Hashtable hash) {
        fen = Board.fen_to_raw(fen);
        String name = fen_to_name(fen);
        MyFile pos_file = new MyFile(name);
        pos_file.from_hash(hash);
    }

    public void add_move(String san,String fen_after) {
        sel_book_move = -1;
        if(game_ptr < move_ptr) {
            move_ptr = game_ptr;
        }
        if(move_ptr >= MAX_MOVES) {}
        else {
            String fen_before = initial_position;
            if(move_ptr > 0) {
                fen_before = positions[move_ptr - 1];
            }
            positions[move_ptr] = fen_after;
            moves[move_ptr++] = san;
            game_ptr++;

            Hashtable pos = get_pos(fen_before);

            if(pos.get(san) == null) {
                BookMove new_book_move = new BookMove(san);
                new_book_move.count = 1;
                pos.put(san, new_book_move.report_hash());
            }
            else {
                BookMove old_book_move = new BookMove(san);
                old_book_move.set_from_hash((Hashtable) pos.get(san));
                old_book_move.count++;
                pos.put(san, old_book_move.report_hash());
            }
            store_pos(fen_before, pos);
        }
        update_game();
    }

    /** Jump to beginning of game, i.e. starting position.
     * Retain moves list.
     * @return starting position string. **/
    public String to_begin() {
        sel_book_move = -1;
        game_ptr = 0;
        update_game();
        return initial_position;
    }

    /** Roll back a move on the board.
     * @return position string for one move before current position.**/
    public String back() {
        sel_book_move = -1;
        if(game_ptr==0) return initial_position;
        else {
            game_ptr--;
            update_game();
            if(game_ptr == 0) return initial_position;
            return positions[game_ptr - 1];
        }
    }

    /** Move forward one move on the board.**/
    public String forward() {
        sel_book_move = -1;
        if(game_ptr == move_ptr) {
            if(game_ptr == 0) {
                return initial_position;
            }
            return positions[game_ptr-1];
        }
        else {
            game_ptr++;
            update_game();
            return positions[game_ptr-1];
        }
    }

    /** Fast-forward to latest position based on move list **/
    public String to_end() {
        sel_book_move = -1;
        game_ptr = move_ptr;
        update_game();
        if(move_ptr == 0) {
            return initial_position;
        }
        return positions[move_ptr-1];
    }

    /** Delete latest move off move list. **/
    public String delete_move() {
        sel_book_move = -1;
        if(game_ptr < move_ptr) {
            move_ptr = game_ptr;
        }
        if(move_ptr==0) {
            update_game();
            return initial_position;
        }
        else {
            if(move_ptr > 0) {
                move_ptr--;
                game_ptr--;
            }
            update_game();
            if(move_ptr == 0) {
                return initial_position;
            }
            return positions[move_ptr-1];
        }
    }

    private int no_book_moves;

    public void update_book() {
        String fen = initial_position;
        if(game_ptr > 0) {
            fen = positions[game_ptr-1];
        }
        Hashtable book_moves=get_pos(fen);
        no_book_moves = 0;
        book_list = new ArrayList<BookMove>();
        Set<String> keys = book_moves.keySet();
        for(String key: keys) {
            Hashtable value = (Hashtable) (book_moves.get(key));
            BookMove book_move = new BookMove(key);
            book_move.set_from_hash(value);
            book_list.add(book_move);
        }
        book_list.sort(new BookMoveComparator());
        no_book_moves = book_list.size();
        String[] temp_list = new String[200];
        int temp_ptr = 0;
        for (BookMove temp : book_list) {
            String notation_as_string = "N/A";
            if(temp.notation >= 0) {
                notation_as_string = notation_list[temp.notation];
            }
            String eval = "_";
            if(temp.is_analyzed) {
                eval = "" + temp.eval;
            }
            String book_line = String.format("%-10s %-4s %5d %8s", temp.san, notation_as_string, temp.count, eval);
            temp_list[temp_ptr++] = book_line;
        }

        ObservableList<String> items = FXCollections.observableArrayList(
                Arrays.copyOfRange(temp_list, 0, no_book_moves));

        blist.setItems(items);

        if(sel_book_move >= 0) {
            blist.getSelectionModel().select(null);
        }
        else {
            blist.getSelectionModel().select(null);
        }
    }

    private String curr_pos(int ptr)
    {
        if(ptr>0)
        {
            return positions[ptr-1];
        }
        else
        {
            return initial_position;
        }
    }

    public void highlight_last_move()
    {
        if(game_ptr>0)
        {
            String fen_before=curr_pos(game_ptr-1);
            Board dummy=new Board(false);
            dummy.set_from_fen(fen_before);
            String san=moves[game_ptr-1];
            Move m=dummy.san_to_move(san);
            b.highlight_move(m);
        }
        else
        {
            b.highlight_move(null);
        }

    }

    public void update_game() {
        update_book();
        String[] game_buffer = new String[MAX_MOVES+1];
        game_buffer[0] = "*";
        Board dummy = new Board(false);
        dummy.set_from_fen_inner(initial_position,false);
        int fullmove_number = dummy.fullmove_number;
        int turn = dummy.turnToMove;
        for(int i = 0; i < move_ptr; i++) {
            if(turn == 1) {
                game_buffer[i+1] = fullmove_number + ". " + moves[i];
            }
            else {
                game_buffer[i + 1] = "          ... " + moves[i];
                fullmove_number++;
            }
            turn = -turn;
        }
        ObservableList<String> items = FXCollections.observableArrayList(
                Arrays.copyOfRange(game_buffer, 0, move_ptr+1)
        );

        list.setItems(items);
        list.getSelectionModel().select(game_ptr);
        list.scrollTo(game_ptr);

        pgn_text.setText(getPGN());

        if(game_ptr > 0) {
            pgn_text.positionCaret(move_indices[game_ptr - 1]);
            pgn_text.selectNextWord();
        }
        else {
            pgn_text.selectRange(0, start_fen_end_index);
        }
        String fen = curr_pos(game_ptr);
        b.set_from_fen_inner(fen, false);
        b.make_move_show(null);
        highlight_last_move();
    }

    /** Return PGN from the move list so far. **/
    public String getPGN() {
        Board dummy = new Board(false);
        dummy.set_from_fen(initial_position);
        int fullmove_number = dummy.fullmove_number;
        int turn = dummy.turnToMove;
        pgn="[FEN \"" + initial_position + "\"]\n";
        start_fen_end_index = pgn.length()-1;
        pgn += "[Flip \"" + b.flip + "\"]\n";
        // Hash headers.
        Set<String> keys = pgn_header_hash.keySet();
        for(String key: keys) {
            String value = pgn_header_hash.get(key).toString();
            if((!key.equals("FEN")) && (!key.equals("Flip"))) {
                pgn += "["+key+" \"" + value + "\"]\n";
            }
        }
        pgn += "\n";
        if(move_ptr > 0) {
            pgn += fullmove_number+". ";
            if(turn == Board.BTURN) {
                pgn += "... ";
            }
            move_indices[0] = pgn.length();
            pgn += moves[0] + " ";
        }

        for(int i = 1; i < move_ptr; i++) {
            dummy.set_from_fen(positions[i - 1]);
            turn = dummy.turnToMove;
            if(dummy.turnToMove == Board.WTURN) {
                fullmove_number++;
                pgn += fullmove_number+". ";
            }
            move_indices[i] = pgn.length();
            pgn += moves[i] + " ";
        }
        return pgn;
    }

    private void set_from_pgn_lines() {
        move_ptr = 0;
        pgn_header_hash.clear();
        initial_position = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        int line_cnt = 0;
        // read headers
        int empty_cnt = 0;
        Boolean finished=false;
        do {
            String line = pgn_lines[line_cnt++];
            if(line_cnt < pgn_lines.length) {
                if(line.length() < 2) {
                    finished = true;
                }
                else {
                    if(line.charAt(0) != '[') {
                        finished = true;
                    }
                    else {
                        // parse header fields
                        Pattern get_header = Pattern.compile("\\[([^ ]+) \"([^\\\"]+)\\\"");
                        Matcher header_matcher = get_header.matcher(line);

                        if(header_matcher.find()) {
                            String key = header_matcher.group(1);
                            String value = header_matcher.group(2);
                            pgn_header_hash.put(key,value);
                            if(key.equals("FEN")) {
                                initial_position = value;
                            }
                        }
                    }
                }
            }
            else {
                finished = true;
            }

        }while(!finished);

        String body = "";
        while(line_cnt < pgn_lines.length) {
            String line = pgn_lines[line_cnt++];
            if(line.length()<2) break;
            body += line + " ";
        }
        // remove all comments, carriage return, line feed
        body = body.replaceAll("\r|\n|\\{[^\\}]*\\}","");

        MyTokenizer t = new MyTokenizer(body);
        String token;
        b.set_from_fen(initial_position);
        Object flip = pgn_header_hash.get("Flip");
        if(flip != null) {
            b.flip = flip.toString().equals("true")? true : false;
        }

        while((token = t.get_token()) != null) {
            if(b.is_san_move_legal(token)) {
                b.make_san_move(token,  false);
                String fen_after = b.getFEN();
                add_move(token, fen_after);
            }
        }
        game_ptr = move_ptr;
        update_game();
    }

    private EventHandler<MouseEvent> mouseHandler = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent mouseEvent) {
            int x = (int)mouseEvent.getX();
            int y = (int)mouseEvent.getY();
            String type = mouseEvent.getEventType().toString();
            if(type.equals("MOUSE_CLICKED")) {
                int click_index = pgn_text.getCaretPosition();
                for(int i = 0; i < move_ptr; i++) {
                    int move_index = move_indices[i];
                    if(click_index < move_index) {
                        game_ptr = i;
                        String pos = initial_position;
                        if(game_ptr > 0) pos = positions[game_ptr - 1];
                        update_game();
                        return;
                    }
                }
                if(move_ptr > 0) {
                    game_ptr = move_ptr;
                    String pos = positions[game_ptr - 1];
                    update_game();
                }
            }
        }
    };

    private int sel_book_move = -1;
    int selected_notation;
    Group select_notation_group;
    ListView<String> select_notation_list;
    MyModal modal;

    private void create_select_notation_group() {
        select_notation_group = new Group();
        select_notation_list = new ListView<String>();
        select_notation_list.setStyle("-fx-font-family: monospace;");
        select_notation_list.setMinWidth(280);
        select_notation_list.setMaxWidth(280);
        select_notation_list.setMinHeight(260);
        select_notation_list.setMaxHeight(260);

        String[] notation_list = {"!!  winning","!   strong","!?  promising","-   stable","?!  interesting","?   bad","??  losing"};

        ObservableList<String> select_notation_items = FXCollections.observableArrayList(notation_list);

        select_notation_list.setItems(select_notation_items);
        select_notation_list.setCellFactory(list -> new AnnotationFormatCell());

        select_notation_group.getChildren().add(select_notation_list);
    }

    private void select_notation_for(String san)
    {
        String fen_before=initial_position;
        if(move_ptr>0)
        {
            fen_before=positions[game_ptr-1];
        }

        Hashtable pos=get_pos(fen_before);

        if(pos.get(san)==null)
        {
            BookMove new_book_move=new BookMove(san);
            new_book_move.count=1;
            pos.put(san,new_book_move.report_hash());
        }
        else
        {
            BookMove old_book_move=new BookMove(san);
            old_book_move.set_from_hash((Hashtable)pos.get(san));

            // obtain new notation

            create_select_notation_group();

            modal=new MyModal(select_notation_group,"Select");

            select_notation_list.setOnMouseClicked(new EventHandler<Event>() {

                @Override
                public void handle(Event event) {

                    selected_notation=
                            notation_list.length-1-
                                    select_notation_list.getSelectionModel().getSelectedIndex()
                    ;

                    modal.close();
                }

            });

            selected_notation=old_book_move.notation;

            modal.show_and_wait();

            // end obtain new notation

            old_book_move.notation=selected_notation;

            pos.put(san,old_book_move.report_hash());

            store_pos(fen_before,pos);

        }

        //book_file.from_hash(book);

        update_book();
    }

    private EventHandler<MouseEvent> mouseHandlerBook = new EventHandler<MouseEvent>() {

        @Override
        public void handle(MouseEvent mouseEvent) {

            int x=(int)mouseEvent.getX();
            int y=(int)mouseEvent.getY();

            String type=mouseEvent.getEventType().toString();

            if(type.equals("MOUSE_CLICKED"))
            {

                int j=blist.getSelectionModel().getSelectedIndex();

                sel_book_move=j;

                int size=book_list.size();

                if((j>=0)&&(j<size))
                {

                    String san=book_list.get(j).san;

                    if(x<120)
                    {

                        b.make_san_move(san,true);

                    }
                    else
                    {

                        select_notation_for(san);

                    }

                }

            }

        }

    };

    private void look_for_initial_dir()
    {
        if(initial_dir.equals(""))
        {
            MyFile config=new MyFile("config.txt");
            String result=config.get_field("initial_dir");
            if(result!=null)
            {
                initial_dir=result;
            }
        }
    }

    private Clipboard clip=Clipboard.getSystemClipboard();

    private Hashtable book;

    private MyFile book_file;
    final private String[] notation_list={"??","?","?!","-","!?","!","!!"};

    public void record_eval(String fen, String san, int eval) {
        Hashtable pos = get_pos(fen);
        if(pos.get(san) == null) {
            BookMove new_book_move = new BookMove(san);
            new_book_move.count = 1;
            new_book_move.is_analyzed = true;
            new_book_move.eval = eval;
            pos.put(san,new_book_move.report_hash());
        }
        else {
            BookMove old_book_move = new BookMove(san);
            old_book_move.set_from_hash((Hashtable)pos.get(san));
            old_book_move.is_analyzed = true;
            old_book_move.eval = eval;
            pos.put(san, old_book_move.report_hash());
        }
        store_pos(fen,pos);
        update_book();
    }

    Group start_deep_group;
    Button deep_stop_button;
    //TextArea deep_text;
    Label deep_text;
    MyModal start_deep_modal;
    ProgressBar progress;

    private void create_start_deep_group() {
        int width = 440;
        start_deep_group = new Group();
        deep_stop_button = new Button();
        deep_stop_button.setText("Stop");
        deep_stop_button.setTranslateX(10);
        deep_stop_button.setTranslateY(10);
        deep_text = new Label();
        deep_text.setTranslateX(15);
        deep_text.setTranslateY(10);
        progress = new ProgressBar();
        progress.setMinWidth(width - 20);
        progress.setTranslateX(10);
        VBox deep_vbox = new VBox(20);
        deep_vbox.setMinWidth(width);
        deep_vbox.setMaxWidth(width);
        deep_vbox.setMinHeight(160);
        deep_vbox.setMaxHeight(160);
        deep_vbox.getChildren().add(deep_stop_button);
        deep_vbox.getChildren().add(deep_text);
        deep_vbox.getChildren().add(progress);
        start_deep_group.getChildren().add(deep_vbox);
    }

    private MyRunnable runnable_do_deep_thread;
    private MyRunnable runnable_update_deep_thread;

    private Thread do_deep_thread;
    private Thread update_deep_thread;

    private Boolean interrupt_deep;

    private int do_deep_i;

    private String[] deep_legal_move_list_buffer;
    private int deep_legal_move_list_buffer_cnt=0;

    private Boolean deep_going;

    private String deep_san;

    Board test_board = new Board(false);

    public void do_deep() {
        do_deep_i = 0;

        String fen = b.getFEN();

        for(int i = 0; i < deep_legal_move_list_buffer_cnt; i++) {
            Platform.runLater(new Runnable() {
                public void run() {
                    deep_san = deep_legal_move_list_buffer[do_deep_i++];
                    b.set_from_fen(fen);
                    b.make_san_move(deep_san, false);
                    b.go_infinite();
                    try {
                        Thread.sleep(250);
                    }
                    catch(InterruptedException ex) {}
                    b.stop_engine();
                    b.set_from_fen(fen);
                    record_eval(fen,deep_san,-b.score_numerical);

                }

            });

            try
            {
                Thread.sleep(300);
            }
            catch(InterruptedException ex)
            {

            }

            if(interrupt_deep)
            {

                break;

            }

        }

        deep_going=false;

        Platform.runLater(new Runnable()
        {

            public void run()
            {
                start_deep_modal.close();
            }

        });

    }

    public void update_deep()
    {

        try
        {
            Thread.sleep(500);
        }
        catch(InterruptedException ex)
        {

        }

        while(deep_going)
        {

            Platform.runLater(new Runnable()
            {

                public void run()
                {
                    deep_text.setText("Examining: "+deep_san);
                    double p=(double)do_deep_i/(double)deep_legal_move_list_buffer_cnt;

                    progress.setProgress(p);
                }

            });

            try
            {
                Thread.sleep(1000);
            }
            catch(InterruptedException ex)
            {

            }
        }
    }

    private void copy_content(String content_as_string)
    {
        ClipboardContent content = new ClipboardContent();
        content.putString(content_as_string);
        clip.setContent(content);
        System.out.println("Content copied to clipboard:\n\n"+content_as_string);
    }

    private String get_content()
    {
        String content_as_string=clip.getString();
        System.out.println("Content copied from clipboard:\n\n"+content_as_string);
        return content_as_string;
    }

    private void highlight_name_in_path() {
        String path = pgn_name_text.getText();
        if(path.length() < 5) return;
        Pattern get_name = Pattern.compile("([^\\"+File.separator+"]+\\.pgn$)");
        Matcher name_matcher = get_name.matcher(path);

        if(name_matcher.find()) {
            int index = path.indexOf(name_matcher.group(0));
            pgn_name_text.requestFocus();
            pgn_name_text.positionCaret(index);
            pgn_name_text.selectRange(index,path.length() - 4);
        }
    }

    private EventHandler<MouseEvent> pgn_name_text_clicked = mouseEvent -> highlight_name_in_path();

}
