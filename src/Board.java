import java.io.InputStream;
import java.util.Hashtable;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.paint.Color;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.TextField;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import java.util.Arrays;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.event.Event;
import javafx.stage.FileChooser;

/** Board class which represents code for playable action on a board interface.
 * This class handles all mouse listening, movement, and gameplay logic.
 * @author Kevin Moy**/
class Board {
    boolean deep_going = false; //Implement engine later on.
    Stage s = new Stage();
    Game g = null;
    private ListView<String> list = new ListView<>();
    int fullmove_number;
    int turnToMove;

    private FileChooser f = new FileChooser();
    private boolean trueBoard; //Board that GUI shows.

    ////////////////////////////////////////////////////////
    // static members

    //Constants indicating white's turn and black's turn.
    final static int WTURN = 1;
    final static int BTURN = -1;

    private static Hashtable lightSquareMap;
    private static Hashtable darkSquareMap;

    private static InputStream fontStream = Main.class.getResourceAsStream("resources/fonts/MERIFONTNEW.TTF");
    private static Font pieceFont;

    // FEN board rep
    private String castling_rights;
    private String ep_square_algeb;
    private int halfmove_clock;
    private String rep;
    private char[][] board = new char[8][8]; //NOTE: This 2D array is indexed by (RANK, FILE). Contains information about pieces.
    private char[][] fonts = new char[8][8]; //indexed by (FILE, RANK). Contains information about square color as well as piece.

    public boolean flip;
    ////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////
    // For piece drag/drop.
    private boolean mouseDragging;
    private char drag_piece;
    private char orig_piece;
    private char orig_empty;
    private int pieceSourceX;
    private int pieceSourceY;
    //Destination of piece coordinates, top left origin (a8 = (0,0))
    private int pieceDestX;
    private int pieceDestY;
    private int piecePixelSourceX;
    private int piecePixelSourceY;
    private int drag_dx;
    private int drag_dy;
    ////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////
    // gc attributes
    private Group canvas_group = new Group();

    HBox main_box = new HBox(2);
    private VBox vertical_box = new VBox(2);
    private HBox game_controls_box = new HBox(2);
    private HBox controls_box = new HBox(2);

    private TextField fen_text = new TextField();

    private Canvas canvas;
    private Canvas highlight_canvas;
    private Canvas upper_canvas;

    private GraphicsContext gc;
    private GraphicsContext highlight_gc;
    private GraphicsContext upper_gc;

    private static int padding;
    private static int piece_size;
    private static int margin;
    private static int board_size;
    private static int info_bar_size;
    private static int font_size;

    private Color board_color;
    private Color piece_color;
    ////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////
    // uci out
    Move currentMove = new Move();
    public int score_numerical;
    ////////////////////////////////////////////////////////


    ////////////////////////////////////////////////////////
    // move generation
    static MoveDescriptor moveTable[] = new MoveDescriptor[20000];
    static int mtPtr[][][] = new int[8][8][64];

    /* ---- Piece Representation ---- */
    //We use a 6-bit system to inherently give the encoding of a piece (multiple) move capabilities.
    // If a piece has bit 5, it has SLIDING capabilities, i.e. can move any number of steps in an allowed direction (diagonal/lat/long).
    final static int SLIDING = 32;
    // If a piece has bit 4, it has STRAIGHT capabilities, i.e. can move laterally.
    final static int STRAIGHT = 16;
    // If a piece has bit 3, it has DIAGONAL capabilities, can move in a diagonal.
    final static int DIAGONAL = 8;
    // If a piece has bit 2, it has SINGLE capabilities, i.e. it can't do shit.
    final static int SINGLE = 4;
    // bit 1 indicates a pawn.
    final static int IS_PAWN = 2;
    // bit 0 is for color

    final static int QUEEN = SLIDING|STRAIGHT|DIAGONAL;
    final static int ROOK = SLIDING|STRAIGHT;
    final static int BISHOP = SLIDING|DIAGONAL;
    final static int KING = SINGLE|STRAIGHT|DIAGONAL;
    final static int KNIGHT = SINGLE;
    final static int PAWN = SINGLE|IS_PAWN;

    final static int all_pieces[] = {KING,QUEEN,ROOK,BISHOP,KNIGHT,PAWN};
    final static char promotion_pieces[] = {'q','r','b','n'};

    final static int PIECE_TYPE = 62;
    final static int PIECE_COLOR = 1;

    final static int WHITE = 1;
    final static int BLACK = 0;

    private int testFile = 0; //Current, y axis
    private int testRank = 0; //Current, x axis.

    private int genMoveInd = 0;
    private char testPiece = ' ';
    private int testPieceCode = 0;
    private int testPieceType = 0;
    private boolean testPieceSlides = false;
    private int testPieceColor = 0;
    private Move current_move = new Move();
    ////////////////////////////////////////////////////////

    /** Constructor for standard Board instance: sets HBox + VBox chain with
     * Board + Pieces, commentary, movelist, etc. Run with GUI class. **/
    public Board(boolean tb) {
        trueBoard = tb;
        if (trueBoard) {
            //Build GUI to display.
            flip = false;
            canvas = new Canvas(board_size, board_size);
            highlight_canvas = new Canvas(board_size, board_size);
            upper_canvas = new Canvas(board_size, board_size);

            canvas_group.getChildren().add(canvas);
            canvas_group.getChildren().add(highlight_canvas);
            canvas_group.getChildren().add(upper_canvas);

            Button flip_button = new Button();
            flip_button.setText("Flip");
            flip_button.setOnAction(e -> flip());

            Button set_fen_button = new Button();
            set_fen_button.setText("Generate FEN");
            set_fen_button.setOnAction(e -> setFromFEN(fen_text.getText()));

            Button report_fen_button = new Button();
            report_fen_button.setText("Load From FEN");
            report_fen_button.setOnAction(e -> drawBoard());

            Button reset_button = new Button();
            reset_button.setText("New Game");
            reset_button.setOnAction(e -> reset());

            Button delete_button = new Button();
            delete_button.setText("Take Back Move");
            delete_button.setOnAction(e -> {
                fenHelper(g.takeback(),false);
                make_move_show(null);
            });

            controls_box.getChildren().add(flip_button);
            controls_box.getChildren().add(set_fen_button);
            controls_box.getChildren().add(report_fen_button);
            controls_box.getChildren().add(reset_button);
            controls_box.getChildren().add(delete_button);

            vertical_box.getChildren().add(canvas_group);

            Button to_begin_button = new Button();
            to_begin_button.setText("<<");
            to_begin_button.setOnAction(e -> g.to_begin());

            Button back_button = new Button();
            back_button.setText("<");
            back_button.setOnAction(e -> g.back());

            Button forward_button = new Button();
            forward_button.setText(">");
            forward_button.setOnAction(e -> g.forward());

            Button to_end_button = new Button();
            to_end_button.setText(">>");
            to_end_button.setOnAction(e -> g.to_end());

            game_controls_box.getChildren().add(to_begin_button);
            game_controls_box.getChildren().add(back_button);
            game_controls_box.getChildren().add(forward_button);
            game_controls_box.getChildren().add(to_end_button);

            vertical_box.getChildren().add(game_controls_box);
            vertical_box.getChildren().add(fen_text);
            vertical_box.getChildren().add(controls_box);

            main_box.getChildren().add(vertical_box);
            //Set mouse handler for board.
            upper_canvas.setOnMouseDragged(mouseHandler);
            upper_canvas.setOnMouseClicked(mouseHandler);
            upper_canvas.setOnMouseReleased(mouseHandler);

            gc = canvas.getGraphicsContext2D();
            //Set 2-square highlighting for before square + after square
            //after making a move.
            highlight_gc = highlight_canvas.getGraphicsContext2D();
            highlight_canvas.setOpacity(0.2);
            highlight_gc.setFill(Color.rgb(255,255,0));

            upper_gc = upper_canvas.getGraphicsContext2D();

            board_color = Color.rgb(255,255,255);
            piece_color = Color.rgb(0, 0, 0);
        }
        reset();
    }

    /*DO NOT TOUCH THIS BELOW: HANDLES GUI + SMOOTH PIECE MOVEMENT*/
    private EventHandler<MouseEvent> mouseHandler = new EventHandler<>() {
        @Override
        public void handle(MouseEvent mouseEvent) {
            //Grab coordinates of mouse event, poll-based
            int x = (int) mouseEvent.getX();
            int y = (int) mouseEvent.getY();
            String type = mouseEvent.getEventType().toString();
            //Possible types: MOUSE_RELEASED, MOUSE_DRAGGED, MOUSE_CLICKED
            if (type.equals("MOUSE_DRAGGED")) {
                //Handle updating board as dragging piece around. DO NOT TOUCH.
                if (mouseDragging) {
                    upper_gc.clearRect(0, 0, board_size, board_size);//Toggle to have lit dragging effect.
                    put_piece_xy(upper_gc, x + drag_dx, y + drag_dy, drag_piece);
                }
                else {
                    mouseDragging = true;
                    pieceSourceX = pb_x(x);
                    pieceSourceY = pb_y(y);
                    piecePixelSourceX = bp_x(pieceSourceX);
                    piecePixelSourceY = bp_y(pieceSourceY);
                    drag_dx = piecePixelSourceX - x;
                    drag_dy = piecePixelSourceY - y;
                    orig_piece = board[pieceSourceX][pieceSourceY];
                    drag_piece = (char) lightSquareMap.get(orig_piece);
                    orig_empty = darkSquare(pieceSourceX, pieceSourceY)? '+' : ' ';
                    put_piece_xy(gc, piecePixelSourceX, piecePixelSourceY, orig_empty);
                }
            }
            if (type.equals("MOUSE_RELEASED")) { //Move finished.
                if (mouseDragging) {
                    upper_gc.clearRect(0, 0, board_size, board_size);
                    mouseDragging = false;
                    pieceDestX = pb_x(x);
                    pieceDestY = pb_y(y);
                    // same square or wrong turn (trying to move a white piece when black's turn).
                    if (((pieceDestX == pieceSourceX) && (pieceDestY == pieceSourceY))
                            || (turn_of(orig_piece) != turnToMove)) {
                        drawBoard();
                        return;
                    }
                    //inbounds move
                    if ((pieceDestX >= 0) && (pieceDestY >= 0) && (pieceDestX <= 7) && (pieceDestY <= 7)) {
                        //Set Move and test legality.
                        currentMove.i1 = pieceSourceX;
                        currentMove.j1 = pieceSourceY;
                        currentMove.i2 = pieceDestX;
                        currentMove.j2 = pieceDestY;
                        currentMove.promotionPiece = ' ';

                        if (is_move_legal(currentMove)) {
                            make_move_show(currentMove);
                        }
                        else {
                            //System.out.println("Illegal move!");
                            drawBoard();
                            return;
                        }
                    }
                    //Reset board if (accidentally) moving piece off screen.
                    else {
                        drawBoard();
                        return;
                    }
                }
            }
        }
    };

    /**Helper function used in generating legal moves from this position: resets the
     * legal move "trackers" testFile and testRank, which simply represent a file and rank
     * respectively. **/
    private void resetMoveTrackers() {
        testFile = -1;
        testRank = 0;
        nextSquare();
    }

    private void nextSquare() {
        boolean stop;
        do {
            testFile++;
            if(testFile > 7) {
                testFile = 0;
                testRank++;
            }
            if(testRank > 7) {
                stop = true;
            }
            else {
                char gen_piece = board[testFile][testRank];
                stop = ((gen_piece != ' ') && (turn_of(gen_piece) == turnToMove));
            }
        }
        while(!stop);

        if(testRank < 8) {
            testPiece = board[testFile][testRank];
            testPieceCode = code_of(testPiece);
            testPieceType = testPieceCode & PIECE_TYPE;
            testPieceColor = color_of(testPiece);
            testPieceSlides = ((testPieceCode & SLIDING) != 0);
            genMoveInd = mtPtr[testFile][testRank][testPieceCode];
        }
    }

    private boolean nextLegalMove() {
        while(testRank < 8) {
            while(!moveTable[genMoveInd].end_piece) {
                MoveDescriptor md = moveTable[genMoveInd];
                int to_i = md.to_i;
                int to_j = md.to_j;
                char to_piece = board[to_i][to_j];
                int to_piece_color = color_of(to_piece);
                current_move = new Move();

                current_move.i1 = testFile;
                current_move.j1 = testRank;
                current_move.i2 = to_i;
                current_move.j2 = to_j;
                current_move.promotionPiece = md.promotionPiece;
                if(md.castling) {
                    genMoveInd++;
                    if((testRank == 0) && (to_i == 6)) {
                        // black kingside
                        if((board[6][0] == ' ') && (board[5][0] == ' ')
                                &&
                                (castling_rights.indexOf('k') >= 0)
                                &&
                                (!checkHelper(4, 0, BLACK))
                                &&
                                (!checkHelper(5, 0, BLACK))
                        ) {
                            return true;
                        }
                    }

                    if((testRank ==0)&&(to_i==2)) {
                        // black queenside
                        if(
                                (board[3][0]==' ')
                                        &&
                                        (board[2][0]==' ')
                                        &&
                                        (board[1][0]==' ')
                                        &&
                                        (castling_rights.indexOf('q')>=0)
                                        &&
                                        (!checkHelper(4,0,BLACK))
                                        &&
                                        (!checkHelper(3,0,BLACK))
                        ) {
                            return true;
                        }
                    }

                    if((testRank ==7)&&(to_i==6)) {
                        // white kingside
                        if(
                                (board[6][7]==' ')
                                        &&
                                        (board[5][7]==' ')
                                        &&
                                        (castling_rights.indexOf('K')>=0)
                                        &&
                                        (!checkHelper(4,7,WHITE))
                                        &&
                                        (!checkHelper(5,7,WHITE))
                        )
                        {
                            return true;
                        }
                    }

                    if((testRank ==7)&&(to_i==2))
                    {
                        // white queenside
                        if(
                                (board[3][7]==' ')
                                        &&
                                        (board[2][7]==' ')
                                        &&
                                        (board[1][7]==' ')
                                        &&
                                        (castling_rights.indexOf('Q')>=0)
                                        &&
                                        (!checkHelper(4,7,WHITE))
                                        &&
                                        (!checkHelper(3,7,WHITE))
                        )
                        {
                            return true;
                        }
                    }

                }
                else if((to_piece != ' ') && (to_piece_color == testPieceColor)) {
                    // own piece
                    if(testPieceSlides)
                        genMoveInd =md.next_vector;
                    else
                        genMoveInd++;
                }
                else {
                    boolean is_capture = to_piece != ' ';
                    if(is_capture) {
                        // capture
                        if(testPieceSlides)
                            genMoveInd = md.next_vector;
                        else
                            genMoveInd++;
                    }
                    else
                    {
                        genMoveInd++;
                    }
                    if(testPieceType == PAWN) {
                        if(testFile != to_i) {
                            // sidewise move may be ep capture
                            String test_algeb = Move.ij_to_algeb(to_i, to_j);
                            if(test_algeb.equals(ep_square_algeb))
                                is_capture = true;
                        }

                        if(is_capture) {
                            // pawn captures only to the sides
                            if(testFile != to_i)
                                return true;
                        }
                        else {
                            // pawn moves only straight ahead
                            if(testFile ==to_i) {
                                if(Math.abs(to_j - testRank) < 2 || board[testFile][testRank + (to_j - testRank)/2] == ' ')
                                    // can always move one square forward
                                    return true;
                            }
                        }
                    }
                    else
                        return true;
                }
            }
            nextSquare();
        }
        return false;
    }

    private static int code_of(int piece) {
        if(piece=='p'){return BLACK|PAWN;}
        if(piece=='P'){return WHITE|PAWN;}
        if(piece=='n'){return BLACK|KNIGHT;}
        if(piece=='N'){return WHITE|KNIGHT;}
        if(piece=='b'){return BLACK|BISHOP;}
        if(piece=='B'){return WHITE|BISHOP;}
        if(piece=='r'){return BLACK|ROOK;}
        if(piece=='R'){return WHITE|ROOK;}
        if(piece=='q'){return BLACK|QUEEN;}
        if(piece=='Q'){return WHITE|QUEEN;}
        if(piece=='k'){return BLACK|KING;}
        if(piece=='K'){return WHITE|KING;}
        return 0;
    }

    private static boolean square_ok(int i,int j)
    {
        if((i>=0)&&(i<=7)&&(j>=0)&&(j<=7))
        {
            return true;
        }
        return false;
    }

    public static void initializeClass() {
        //Set dimensions for board GUI
        piece_size = 52;
        padding = 7;
        margin = 10;
        font_size = 15;
        board_size = (piece_size + padding) * 8 + (2 * margin);
        pieceFont = Font.loadFont(fontStream, piece_size); //Set piece fonts.
        // Create maps to white and black pieces.
        mapWhite();
        mapBlack();
        initializeMoveTablePtr();
    }


    /** Populate dark square hashtable, mapping pieces on dark squares to suitable font.**/
    private static void mapBlack() {
        darkSquareMap = new Hashtable();
        darkSquareMap.put(' ', '+');
        darkSquareMap.put('P', 'P');
        darkSquareMap.put('N', 'N');
        darkSquareMap.put('B', 'B');
        darkSquareMap.put('R', 'R');
        darkSquareMap.put('Q', 'Q');
        darkSquareMap.put('K', 'K');
        //Ran out of letters so put some random shit here.
        darkSquareMap.put('p', 'O');
        darkSquareMap.put('n', 'M');
        darkSquareMap.put('b', 'V');
        darkSquareMap.put('r', 'T');
        darkSquareMap.put('q', 'W');
        darkSquareMap.put('k', 'L');
    }

    /** Populate light square hashtable, mapping pieces on light squares to suitable font. **/
    private static void mapWhite() {
        lightSquareMap = new Hashtable();
        lightSquareMap.put(' ', ' ');
        lightSquareMap.put('P', 'p');
        lightSquareMap.put('N', 'n');
        lightSquareMap.put('B', 'b');
        lightSquareMap.put('R', 'r');
        lightSquareMap.put('Q', 'q');
        lightSquareMap.put('K', 'k');
        lightSquareMap.put('p', 'o');
        lightSquareMap.put('n', 'm');
        lightSquareMap.put('b', 'v');
        lightSquareMap.put('r', 't');
        lightSquareMap.put('q', 'w');
        lightSquareMap.put('k', 'l');
    }

    /** Return true if cartesian coordinate (x,y) is a valid coordinate
     * in a zero-indexed 8x8 graph, i.e. our board **/
    private static boolean inbounds(int x, int y) {
        if((x >= 0) && (x <= 7) && (y >= 0) && (y <= 7)) {
            return true;
        }
        return false;
    }

    /** Helper method that returns true if a piece actually has moved, i.e. its overall displacement (dx+dy) is positive**/
    private static boolean moves(int dx, int dy) {
        return Math.abs(dx) + Math.abs(dy) > 0;
    }

    /** Helper method that returns true if a diagonal piece (bishop, king, queen) has positive displacement. **/
    private static boolean diagPieceMoved(int piece_type, int dx, int dy) {
        return ((dx * dy) != 0) &&
                ((Math.abs(dx) != 2) && (Math.abs(dy) != 2))
                &&
                ((piece_type & DIAGONAL) != 0);
    }

    /** Helper method that returns true if a STRAIGHT-moving piece (queen, rook, king) has positive displacement. **/
    private static boolean straightPieceMoved(int piece_type, int dx, int dy) {
        return ((dx*dy)==0)
                && ((Math.abs(dx) != 2) && (Math.abs(dy) != 2))
                && ((piece_type & STRAIGHT) != 0);
    }

    /** Helper method that returns true if a KNIGHT has moved **/
    private static boolean knightMoved(int piece_type, int dx, int dy) {
        return ((Math.abs(dx * dy) == 2) && (piece_type == KNIGHT));
    }

    /** Helper method that returns true if a pawn has made a capture.**/
    private static boolean pawnCaptures(int piece_type, int piece_color, int i, int j, int dx, int dy) {
        return ((piece_type == PAWN) && (Math.abs(dx) < 2) && (Math.abs(dy) > 0) &&
                (((piece_color==WHITE) && (dy < 0) && ((Math.abs(dy) == 1)
                        || ((j==6) && (dx==0))))  //enpassant for black
                        || ((piece_color==BLACK) && (dy>0) && ((Math.abs(dy)==1) || ((j==1) && (dx==0))))) //enpassant for white.
        );
    }

    /** Return true if a castling move, i.e.
     * if the move p -> (x,y) with displacement (dx,dy) specifies castling. **/
    private static boolean isCastlingHelper(int p, int x, int y, int dx, int dy) {
        return ((p==(WHITE|KING)) && (((x==4)&&(y==7)&&(dx==2)&&(dy==0))
                ||
                ((x==4)&&(y==7)&&(dx==-2)&&(dy==0)))
                ||
                ((p==(BLACK|KING)) && (((x==4)&&(y==0)&&(dx==2)&&(dy==0))
                        || ((x==4)&&(y==0)&&(dx==-2)&&(dy==0)))
                ));
    }
    /** Helper function for init_class() that returns true iff
     * the move specified by p -> (x,y) is a promotion or not **/
    private static boolean isPromotionMove(int p, int x, int y) {
        return ((p==(WHITE|PAWN))&&(y==0)) || ((p==(BLACK|PAWN))&&(y==7));
    }

    /** Return true if piece_type is 0-5, specifying piece enumeration. **/
    private static boolean isPiece(int piece_type) {
        return (piece_type==PAWN)||(piece_type==KNIGHT)||(piece_type==BISHOP)
                ||(piece_type==ROOK)||(piece_type==QUEEN)||(piece_type==KING);
    }

    private void reset_game() {
        if(g != null) g.reset(getFEN());
    }


    /**Helper function to populate fonts array (for pieces and squares)
     * based on current position.*/
    private void populateFonts(char[][] board) {
        for(int i = 0; i < 8; i++) {
            for(int j = 0; j < 8; j++) {
                if (darkSquare(i,j)) {
                    fonts[i][j] = (char) darkSquareMap.get(board[i][j]);
                }
                else {
                    fonts[i][j] = (char) lightSquareMap.get(board[i][j]);
                }
            }
        }
    }

    /** Return if square at (i,j) is a dark square or not
     * (must be a light square if false) **/
    private boolean darkSquare(int i, int j) {
        return (i+j) % 2 == 1;
    }

    /** Convert a string representation of a position REP
     * and set the position via board array.
     * @param rep string representation of position
     */
    private char[][] setPosition(String rep) {
        for(int i = 0; i < 8; i++) {
            for(int j = 0; j < 8; j++) {
                //Convert 2D coordinates (8x8) to 1D (64).
                board[i][j] = rep.charAt(i + j*8);
            }
        }
        return board;
    }

    /** Converts board x-coordinate to corresponding pixel location*/
    private int bp_x(int i) {
        int offset = i;
        if (flip) {
            offset = 7 - i;
        }
        return margin + offset * (piece_size + padding);
    }

    /** Converts board y-coordinate to corresponding pixel location*/
    private int bp_y(int j) {
        int offset = j;
        if (flip) {
            offset = 7 - j;
        }
        return margin + offset * (piece_size + padding);
    }

    /** Converts pixel (screen) x-coordinate to corresponding board (file) coordinate (0-7). */
    private int pb_x(int x) {
        int i = (x - margin) / (piece_size + padding);
        return flip? (7-i) : i;
    }

    /** Converts pixel (screen) x-coordinate to corresponding board (rank) coordinate. (0-7) */
    private int pb_y(int y) {
        int j = (y - margin) / (piece_size + padding);
        return(flip?(7-j):j);
    }

    /** Place piece onto (x,y) pixel location on graphicsContext SELECT_GC (Should only be board).
     * This method ensures pieces actually show up as an image from the TTF file,
     * rather than a character. Additionally, handles maintaining
     * coloration of squares AFTER move (original square and dest square).*/
    private void put_piece_xy(GraphicsContext select_gc, int x, int y, char piece) {
        if(select_gc == gc) {
            //Ensure source square remains same color as before.
            select_gc.setFill(board_color);
            //Ensure piece is removed from original square upon movement.
            select_gc.fillRect(x, y, piece_size + padding, piece_size + padding);
        }
        select_gc.setFill(piece_color);
        select_gc.setFont(pieceFont);
        select_gc.fillText(Character.toString(piece), x,y + piece_size + padding);
    }

    public void drawBoard() {
//        if(deep_going)
//            return;
        populateFonts(board);
//        System.out.println(Arrays.deepToString(fonts));
        gc.setFont(pieceFont);
        gc.setFill(board_color);
        gc.fillRect(0, 0, board_size, board_size);

        gc.setFill(Color.rgb(200, 255, 200));
        gc.fillRect(0, board_size, board_size, info_bar_size );

        gc.setFill(piece_color);

        for(int i = 0; i < 8; i++) {
            for(int j = 0; j < 8; j++) {
                gc.fillText(Character.toString(fonts[i][j]), bp_x(i), bp_y(j) + piece_size + padding);
            }
        }

        gc.setFont(Font.font("Courier New", font_size));
        //Update FEN
        String gc_text = " t: "+(turnToMove ==1? "w" : "b")+
                ", c: "+castling_rights+
                ", ep: "+ep_square_algeb+
                ", hm: "+halfmove_clock+
                ", fm: "+fullmove_number+
                ", flp: "+(flip? "y":"n")+
                (is_in_check(turnToMove)?", +":"");
        gc.fillText(gc_text, 0,board_size + padding + font_size);
        gc.strokeRect(0, 0, board_size, board_size);
        fen_text.setText(getFEN());
        listLegalMoves();
    }

    /** Convert current board position to string representation. **/
    private String board_to_rep() {
        rep = "";
        for(int i = 0; i < 8; i++) {
            for(int j = 0; j < 8; j++) {
                rep += board[j][i];
            }
        }
        return rep;
    }

    public boolean setFromFEN(String fen) {
        return fenHelper(fen,true);
    }

    public boolean fenHelper(String fen, boolean resetGame) {
        rep = "";
        String[] fen_parts = fen.split(" ");
        fen = fen_parts[0];
        for(int i = 0; i < fen.length(); i++) {
            char current = fen.charAt(i);
            if(rep.length() < 64) {
                if(current=='/') {
                    //Do nothing
                }
                else {
                    if((current >= '1') && (current <= '8')) {
                        for(int j = 0; j < Integer.parseInt("" + current); j++) {
                            rep += " ";
                        }
                    }
                    else {
                        rep += current;
                    }
                }
            }
            else {
                break;
            }
        }

        if(rep.length() < 64) {
            board_to_rep();
            return false;
        }

        setPosition(rep);
        if(fen_parts.length >= 2) {
            String turn_part = fen_parts[1];
            if(turn_part.charAt(0) == 'w') {
                turnToMove = WTURN;
            }
            else {
                turnToMove = BTURN;
            }
        }

        if(fen_parts.length >= 3) {
            String castling_rights_part = fen_parts[2];
            castling_rights = castling_rights_part;
        }

        if(fen_parts.length >= 4) {
            String ep_square_algeb_part = fen_parts[3];
            ep_square_algeb = ep_square_algeb_part;
        }

        if(fen_parts.length >= 5) {
            String halfmove_clock_part = fen_parts[4];
            halfmove_clock = Integer.parseInt(halfmove_clock_part);
        }

        if(fen_parts.length >= 6) {
            String fullmove_number_part = fen_parts[5];
            fullmove_number = Integer.parseInt(fullmove_number_part);
        }

        if((trueBoard) && (!deep_going)) {
            drawBoard();
            if(resetGame) {
                reset_game();
            }
        }
        return true;
    }

    public static String fen_to_raw(String fen) {
        String raw_fen = fen.replaceAll(" [^ ]+ [^ ]+$", "");
        return raw_fen;
    }

    public String getFEN() {
        String fen = "";
        board_to_rep();
        for(int j = 0; j < 8; j++) {
            int empty_cnt = 0;
            for(int i = 0; i < 8; i++) {
                int index = i + j * 8;
                char current = rep.charAt(index);
                if(current == ' ')
                    empty_cnt++;
                else {
                    if(empty_cnt > 0) {
                        fen += empty_cnt;
                        empty_cnt = 0;
                    }
                    fen += current;
                }
            }
            if(empty_cnt > 0) {
                fen += empty_cnt;
            }
            if(j < 7) {
                fen += "/";
            }
        }
        fen+= " " +(turnToMove == WTURN ?"w":"b")
                        + " "
                        +castling_rights
                        +" "
                        +ep_square_algeb
                        +" "
                        +halfmove_clock
                        +" "
                        +fullmove_number;
        return(fen);
    }

    public void flip() {
        flip = !flip;
        drawBoard();
        g.update_game();
    }

    /** Make a move M on the board **/
    private void make_move(Move m) {
        m.orig_piece = board[m.i1][m.j1];
        // Remove piece from its original square on the board.
        board[m.i1][m.j1] = ' ';
        //Save piece on dest_square prior and place original piece on dest_square.
        char dest_piece = board[m.i2][m.j2];
        board[m.i2][m.j2] = m.orig_piece;
        //Switch turns.
        turnToMove = -turnToMove;
        // clear ep
        ep_square_algeb = "-";
        // promotion
        if( ((m.orig_piece=='P')&&(m.j2==0)) || ((m.orig_piece=='p')&&(m.j2==7)) )
        {
            if(m.promotionPiece != ' ') {
                if((m.promotionPiece >='a')&&(m.promotionPiece <='z'))
                {
                    if(m.orig_piece=='P')
                    {
                        m.promotionPiece +='A'-'a';
                    }
                }
                else
                {
                    if(m.orig_piece=='p')
                    {
                        m.promotionPiece +='a'-'A';
                    }
                }
            }
            else
            {
                Object[] options = {"Queen",
                        "Rook",
                        "Bishop",
                        "Knight"
                };

                int n=0;
                n = JOptionPane.showOptionDialog(null,
                        "Select:",
                        "Promote piece",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[0]);

                switch(n)
                {
                    case 0:m.promotionPiece =m.orig_piece=='P'?'Q':'q';break;
                    case 1:m.promotionPiece =m.orig_piece=='P'?'R':'r';break;
                    case 2:m.promotionPiece =m.orig_piece=='P'?'B':'b';break;
                    case 3:m.promotionPiece =m.orig_piece=='P'?'N':'n';break;
                    default:m.promotionPiece =' ';
                }

            }

            board[m.i2][m.j2]=m.promotionPiece;

        }

        // halfmove clock
        boolean is_capture=(dest_piece!=' ');

        boolean is_pawn_move=((m.orig_piece=='p')||(m.orig_piece=='P'));

        if(is_capture||is_pawn_move)
        {
            halfmove_clock=0;
        }
        else
        {
            halfmove_clock++;
        }

        // fullmove number
        if(turnToMove == WTURN)
        {
            fullmove_number++;
        }

        // pawn push by two
        if(
                ((m.orig_piece=='P')&&(m.j1==6)&&(m.j2==4))
                        ||
                        ((m.orig_piece=='p')&&(m.j1==1)&&(m.j2==3))
        )
        {
            ep_square_algeb=Move.ij_to_algeb(m.i1,m.j1+(m.j2-m.j1)/2);
        }

        // castling rights

        if(m.orig_piece=='k')
        {
            castling_rights=castling_rights.replace("k","");
            castling_rights=castling_rights.replace("q","");

        }

        if(m.orig_piece=='K')
        {
            castling_rights=castling_rights.replace("K","");
            castling_rights=castling_rights.replace("Q","");

        }

        if(board[0][0]==' ')
        {
            castling_rights=castling_rights.replace("q","");
        }

        if(board[0][7]==' ')
        {
            castling_rights=castling_rights.replace("Q","");
        }

        if(board[7][0]==' ')
        {
            castling_rights=castling_rights.replace("k","");
        }

        if(board[7][7]==' ')
        {
            castling_rights=castling_rights.replace("K","");
        }

        if(castling_rights.length()<=0)
        {
            castling_rights="-";
        }

        // castling
        if(m.orig_piece=='k')
        {
            if((m.j1==0)&&(m.i1==4)&&(m.j2==0)&&(m.i2==6))
            {
                board[7][0]=' ';
                board[5][0]='r';
            }

            if((m.j1==0)&&(m.i1==4)&&(m.j2==0)&&(m.i2==2))
            {
                board[0][0]=' ';
                board[3][0]='r';
            }
        }

        if(m.orig_piece == 'K') {
            if((m.j1 == 7) && (m.i1 == 4) && (m.j2 == 7) && (m.i2 == 6)) {
                board[7][7] = ' ';
                board[5][7] = 'R';
            }

            if((m.j1 == 7) && (m.i1 == 4) && (m.j2 == 7) && (m.i2 == 2)) {
                board[0][7]=' ';
                board[3][7]='R';
            }
        }

        //en passant
        if(((m.orig_piece == 'p') || (m.orig_piece == 'P')) && (dest_piece == ' ') && (m.i1 != m.i2)) {
            board[m.i2][m.j1] = ' ';
        }

    }

    /** Decode piece from integer form to corresponding character. **/
    private char decodePiece(int code) {
        if(code==(WHITE|KING)){return 'K';}
        if(code==(BLACK|KING)){return 'k';}
        if(code==(WHITE|QUEEN)){return 'Q';}
        if(code==(BLACK|QUEEN)){return 'q';}
        if(code==(WHITE|ROOK)){return 'R';}
        if(code==(BLACK|ROOK)){return 'r';}
        if(code==(WHITE|BISHOP)){return 'B';}
        if(code==(BLACK|BISHOP)){return 'b';}
        if(code==(WHITE|KNIGHT)){return 'N';}
        if(code==(BLACK|KNIGHT)){return 'n';}
        if(code==(WHITE|PAWN)){return 'P';}
        if(code==(BLACK|PAWN)){return 'p';}
        return ' ';
    }

    private boolean checkHelper(int i, int j, int color) {
        int attacker_color = color == WHITE? BLACK:WHITE;
        boolean is_check = false;
        for(int p = 0; p < all_pieces.length; p++) {
            int piece_code = all_pieces[p];
            int piece_type = piece_code & PIECE_TYPE;
            int check_ptr= mtPtr[i][j][piece_code|color];
            char test_piece = decodePiece(piece_code|attacker_color);
            MoveDescriptor md;
            do {
                md = moveTable[check_ptr];
                if (md.castling) {
                    check_ptr++;
                }
                else if (!md.end_piece) {
                    int to_i = md.to_i;
                    int to_j = md.to_j;
                    char to_piece=board[to_i][to_j];
                    if(piece_type==PAWN) {
                        if(to_i==i) {
                            // pawn cannot check forward
                            to_piece=' ';
                        }
                    }
                    if(to_piece==test_piece) {
                        is_check=true;
                    }
                    else {
                        if(to_piece==' ') {
                            check_ptr++;
                        }
                        else {
                            check_ptr=md.next_vector;
                        }
                    }
                }
            }while((!md.end_piece) && (!is_check));
            if (is_check) break;
        }
        return is_check;
    }

    /** Return true if TURN is in check in the given position on the Board. **/
    private boolean is_in_check(int turn) {
        int king_i = kingCoordinates(turn)[0];
        int king_j = kingCoordinates(turn)[1];
        int color;
        if (turn == WTURN) {
            color = WHITE;
        } else {
            color = BLACK;
        }
        return checkHelper(king_i, king_j, color);
    }

    /** Return a 2-element array of king coordinates on the board. **/
    public int[] kingCoordinates(int turn) {
        int[] coords = new int[2];
        boolean found = false;
        char search_king = (turn == WTURN)? 'K' : 'k';
        int king_i = 0;
        int king_j = 0;
        for(int i = 0; i < 8; i++) {
            for(int j = 0; j < 8; j++) {
                if(board[i][j] == search_king) {
                    king_i = i;
                    king_j = j;
                    found = true;
                    break;
                }

                if(found) break;
            }
        }
        coords[0] = king_i;
        coords[1] = king_j;
        return coords;
    }
    //Possibly an ArrayList is more space-efficient
    public String[] currPosLegalMoves = new String[250];
    public int numLegalMoves = 0;

    /**Populate legal moves array for current position, and display on GUI**/
    public void listLegalMoves() {
        resetMoveTrackers();
        numLegalMoves = 0;
        while(nextLegalMove()) {
            String algeb = current_move.toAlgebraic();
            Board dummy=new Board(false);
            dummy.setFromFEN(getFEN());
            dummy.make_move(current_move);
            if(!dummy.is_in_check(turnToMove)) {
                String san = moveSAN(current_move);
                currPosLegalMoves[numLegalMoves++] = san;
            }
        }
        String[] legal_move_list_buffer_slice = Arrays.copyOfRange(currPosLegalMoves, 0, numLegalMoves);
        Arrays.sort(legal_move_list_buffer_slice);
        ObservableList<String> items = FXCollections.observableArrayList(legal_move_list_buffer_slice);
        list.setItems(items);
        list.setOnMouseClicked((EventHandler<Event>) event -> {
            ObservableList<String> selectedItems =  list.getSelectionModel().getSelectedItems();
            for(String s : selectedItems){
                make_san_move(s,true);
            }
        });
    }

    /** Check if Move is legal given current board position**/
    private boolean is_move_legal(Move m) {
        boolean is_legal = false;
        String algeb = m.algebraicHelper(false);
        resetMoveTrackers();
        while((!is_legal)&& (nextLegalMove())) {
            String test_algeb = current_move.algebraicHelper(false);
            if(test_algeb.equals(algeb)) {
                Board dummy = new Board(false);
                dummy.setFromFEN(getFEN());
                dummy.make_move(current_move);
                if(!dummy.is_in_check(turnToMove)) {
                    is_legal = true;
                }
            }
        }
        return is_legal;
    }

    private String moveSANRaw(Move m) {
        char from_piece = board[m.i1][m.j1];
        int from_piece_code = code_of(from_piece);
        int from_piece_type = from_piece_code & PIECE_TYPE;
        String algeb = m.toAlgebraic();
        if(from_piece_type==KING) {
            if(algeb.equals("e1g1")){return "O-O";}
            if(algeb.equals("e8g8")){return "O-O";}
            if(algeb.equals("e1c1")){return "O-O-O";}
            if(algeb.equals("e8c8")){return "O-O-O";}
        }

        char to_piece=board[m.i2][m.j2];
        String target_algeb=""+algeb.charAt(2)+algeb.charAt(3);

        if(from_piece_type == PAWN) {
            if(m.i1 == m.i2) {
                // pawn push
                return target_algeb;
            }
            else {
                return algeb.charAt(0) + "x" + target_algeb;
            }
        }
        else {
            int test_ptr = mtPtr[m.i2][m.j2][from_piece_code];
            MoveDescriptor md;
            boolean ambiguity=false;
            boolean same_rank=false;
            boolean same_file=false;
            int from_rank_list[] = new int[50];
            int from_rank_cnt = 0;
            int from_file_list[] = new int[50];
            int from_file_cnt = 0;
            do {
                md = moveTable[test_ptr];
                char to_piece_test = board[md.to_i][md.to_j];
                if(to_piece_test == ' ') {
                    test_ptr++;
                }
                else {
                    if((to_piece_test == from_piece) && ((md.to_i != m.i1) || (md.to_j != m.j1))) {
                        Move test_move = new Move();
                        test_move.orig_piece = from_piece;
                        test_move.promotionPiece = ' ';
                        test_move.i1 = md.to_i;
                        test_move.j1 = md.to_j;
                        test_move.i2 = m.i2;
                        test_move.j2 = m.j2;
                        Board dummy = new Board(false);
                        dummy.setFromFEN(getFEN());
                        dummy.make_move(test_move);
                        if(!dummy.is_in_check(turnToMove)) {
                            ambiguity = true;
                            from_rank_list[from_rank_cnt++] = md.to_j;
                            from_file_list[from_file_cnt++] = md.to_i;
                            for(int r = 0; r < from_rank_cnt; r++) {
                                if(m.j1 == from_rank_list[r]) {
                                    same_rank = true;
                                }
                            }
                            for(int f = 0; f < from_file_cnt; f++) {
                                if(m.i1 == from_file_list[f]) {
                                    same_file = true;
                                }
                            }
                        }
                    }
                    if((from_piece_type & SLIDING) != 0) {
                        test_ptr = md.next_vector;
                    }
                    else {
                        test_ptr++;
                    }
                }

            }while(!moveTable[test_ptr].end_piece);

            String san = "" + Character.toUpperCase(from_piece);

            if(ambiguity && (!same_file) && (!same_rank)) {
                san += algeb.charAt(0);
            }
            else {
                if(same_rank)
                    san += algeb.charAt(0);
                if(same_file)
                    san += algeb.charAt(1);
            }
            if(to_piece != ' ')
                san += "x";

            san += target_algeb;
            return san;
        }
    }

    /** Convert Move M to standard algebraic notation (SAN). **/
    public String moveSAN(Move m) {
        String san = moveSANRaw(m);
        if(m.promotionPiece != ' ')
            san += "=" + Character.toUpperCase(m.promotionPiece);

        Board test = new Board(false);
        test.setFromFEN(getFEN());
        test.make_move(m);
        boolean is_check = test.is_in_check(test.turnToMove);
        test.resetMoveTrackers();
        boolean has_legal = false;
        while((test.nextLegalMove()) && (!has_legal)) {
            Board test2 = new Board(false);
            test2.setFromFEN(test.getFEN());
            test2.make_move(test.current_move);
            if(!test2.is_in_check(test.turnToMove)) {
                has_legal = true;
            }
        }

        if(is_check) {
            if(has_legal) {
                san += "+";
            }
            else {
                san += "#";
            }
        }
        else if(!has_legal) {
            san += "=";
        }
        return san;
    }

    public void make_move_show(Move m) {
        if(m != null) {
            String san = moveSAN(m);
            make_move(m);
            g.add_move(san, getFEN());
        }
        drawBoard();
    }

    private static int turn_of(char piece) {
        if((piece >= 'a') && (piece <= 'z')) {
            return BTURN;
        }
        return WTURN;
    }

    private static int color_of(char piece) {
        if((piece >= 'a') && (piece <= 'z')) {
            return BLACK;
        }
        return WHITE;
    }


    public void reset() {
        //Set relevant FEN information.
        rep="rnbqkbnrpppppppp                                PPPPPPPPRNBQKBNR";
        setPosition(rep);
        castling_rights = "KQkq";
        ep_square_algeb = "-";
        halfmove_clock = 0;
        fullmove_number = 1;
        turnToMove = WTURN;
        //Reset position.
        if(trueBoard) {
            mouseDragging = false;
            drawBoard();
            reset_game();
        }
    }

    /** Convert standard algebraic notation to a move on the chess board.**/
    public Move san_to_move(String san) {
        Move m = new Move();
        m.from_algeb("a1a1");
        if(san.equals("O-O")) {
            if(turnToMove == WTURN) {
                m.from_algeb("e1g1");
            }
            else {
                m.from_algeb("e8g8");
            }
            return m;
        }

        if(san.equals("O-O-O")) {
            if(turnToMove == WTURN) {
                m.from_algeb("e1c1");
            }
            else {
                m.from_algeb("e8c8");
            }
            return m;
        }

        if(san.length() < 2) return m;
        Move dummy = new Move();
        char file_algeb = ' ';
        char rank_algeb = ' ';
        String target_algeb = "";
        String algeb = "";

        char piece = san.charAt(0);

        if((piece >= 'a') && (piece <= 'z')) {
            // pawn move
            file_algeb = piece;
            piece = 'P';
            san = san.substring(1);
        }
        else {
            san = san.substring(1);
        }

        boolean takes = false;

        if(san.charAt(0) == 'x') {
            takes = true;
            san = san.substring(1);
        }

        if((piece == 'P') && (!takes)) {
            rank_algeb = san.charAt(0);
            san = san.substring(1);
            target_algeb = "" + file_algeb + rank_algeb;
            m.from_algeb("a1" + target_algeb);
            m.i1 = m.i2;
            if(turnToMove == WTURN) {
                if(board[m.i2][m.j2+1]=='P') {
                    m.j1=m.j2+1;
                }
                else if(board[m.i2][m.j2+2]=='P') {
                    m.j1=m.j2+2;
                }
            }
            else {
                if(board[m.i2][m.j2-1]=='p') {
                    m.j1=m.j2-1;
                }
                else if(board[m.i2][m.j2-2]=='p') {
                    m.j1=m.j2-2;
                }
            }
        }
        else if(piece=='P') {
            if(san.length() < 2) return m;
            target_algeb = "" + san.charAt(0) + san.charAt(1);
            san = san.substring(2);
            m.from_algeb(file_algeb + "1" + target_algeb);
            if(turnToMove == WTURN) {
                m.j1 = m.j2 + 1;
            }
            else {
                m.j1 = m.j2 - 1;
            }
        }
        else {
            // takes carries no information
            san = san.replace("x","");
            Pattern get_algeb = Pattern.compile("([a-z0-9]*)");
            Matcher algeb_matcher = get_algeb.matcher(san);
            if(algeb_matcher.find()) {
                algeb = algeb_matcher.group(0);
                if(algeb.length() == 2) {
                    file_algeb = ' ';
                    rank_algeb = ' ';
                    san = san.substring(2);
                    m.from_algeb("a1" + algeb);
                }
                else if (algeb.length() == 3) {
                    target_algeb = san.substring(1,3);
                    if((algeb.charAt(0) >= 'a') && (algeb.charAt(0) <= 'z')) {
                        file_algeb = algeb.charAt(0);
                        rank_algeb = ' ';
                        m.from_algeb(file_algeb + "1" + target_algeb);
                    }
                    else {
                        rank_algeb = algeb.charAt(0);
                        file_algeb = ' ';
                        m.from_algeb("a" + rank_algeb + target_algeb);
                    }
                    san = san.substring(3);
                }
                else {
                    m.from_algeb(algeb);
                    if(san.length() >= 4) {
                        san = san.substring(4);
                    }
                    else {
                        return m;
                    }
                }
                // disambiguation
                int piece_code = code_of(piece);
                int piece_type = piece_code & PIECE_TYPE;
                boolean is_sliding = ((piece_type & SLIDING)!=0);

                int san_ptr = mtPtr[m.i2][m.j2][piece_code];
                MoveDescriptor md;
                boolean found = false;
                char search_piece = piece;
                if(turnToMove == BTURN)
                    search_piece = Character.toLowerCase(piece);

                do {
                    md = moveTable[san_ptr];
                    char to_piece = board[md.to_i][md.to_j];
                    if(to_piece == ' ') {
                        san_ptr++;
                    }
                    else {
                        if(search_piece == to_piece) {
                            boolean file_match = true;
                            boolean rank_match = true;
                            if(file_algeb != ' ') file_match = (md.to_i == m.i1);
                            if(rank_algeb != ' ') rank_match = (md.to_j == m.j1);

                            if(file_match && rank_match) {
                                found = true;
                                m.i1 = md.to_i;
                                m.j1 = md.to_j;
                                // check for check
                                Board dummy_check_test = new Board(false);
                                dummy_check_test.setFromFEN(getFEN());
                                dummy_check_test.make_move(m);
                                if(dummy_check_test.is_in_check(turnToMove)) {
                                    found = false;
                                }
                            }
                        }
                        if(is_sliding) {
                            san_ptr = md.next_vector;
                        }
                        else {
                            san_ptr++;
                        }
                    }
                }while((!found)&&(!md.end_piece));
            }
        }
        if(san.length() > 1) {
            if(san.charAt(0) == '=') {
                // promotion
                m.promotionPiece = san.charAt(1);
            }
        }
        return m;
    }

    public boolean is_san_move_legal(String san) {
        Move m = san_to_move(san);
        return is_move_legal(m);
    }

    public void make_san_move(String san, boolean show) {
        Move m = san_to_move(san);
        boolean is_legal = is_move_legal(m);
        //System.out.println(m.to_algeb()+" legal: "+is_legal);
        if(is_legal) {
            if(show) {
                make_move_show(m);
            }
            else {
                make_move(m);
            }
        }
    }


    public void highlight_move(Move m) {
        highlight_gc.clearRect(0,0,board_size,board_size);
        if(m == null) {
            return;
        }

        int arc = piece_size / 2;
        int size = piece_size - 4;
        highlight_gc.fillRoundRect(bp_x(m.i1) + 2, bp_y(m.j1) + 5,size, size, arc, arc);
        highlight_gc.fillRoundRect(bp_x(m.i2) + 2, bp_y(m.j2) + 5, size, size, arc, arc);
    }

    private static void initializeMoveTablePtr(){
        int move_table_curr_ptr = 0;
        //For this given position, create all possible move descriptors, i.e. all possible moves, disregarding turn.
        for(int x = 0; x < 8; x++) {
            for(int y = 0; y < 8; y++) {
                //6-bit piece encoding goes up to 2^5 - 1 = 63.
                for(int p = 0; p < 64; p++) {
                    int piece_type = p & PIECE_TYPE;
                    int piece_color = p & PIECE_COLOR;
                    if(isPiece(piece_type)) {
                        boolean is_single = ((piece_type & SINGLE) != 0);
                        mtPtr[x][y][p] = move_table_curr_ptr;
                        //NOTE: top left corner is (0,0), so Black's back rank is 0 and White's is 7.
                        //Left column is 0, right column is 7.
                        for(int dx = -2; dx <= 2; dx++) {
                            for(int dy = -2; dy <= 2; dy++) {
                                boolean is_castling = isCastlingHelper(p,x,y,dx,dy);
                                if (moves(dx,dy) && ((is_castling) || diagPieceMoved(piece_type, dx, dy)
                                        || straightPieceMoved(piece_type, dx, dy)
                                        || knightMoved(piece_type, dx, dy)
                                        || pawnCaptures(piece_type, piece_color, x, y, dx, dy)))
                                {
                                    int start_vector = move_table_curr_ptr;
                                    int possible_dest_x = x;
                                    int possible_dest_y = y;
                                    boolean square_ok;
                                    do {
                                        possible_dest_x += dx;
                                        possible_dest_y += dy;
                                        square_ok = inbounds(possible_dest_x, possible_dest_y);
                                        if(square_ok) {
                                            if(isPromotionMove(p, possible_dest_x, possible_dest_y)) {
                                                for(int prom = 0; prom < promotion_pieces.length; prom++) {
                                                    MoveDescriptor md = new MoveDescriptor();
                                                    md.to_i = possible_dest_x;
                                                    md.to_j = possible_dest_y;
                                                    md.castling = false;
                                                    md.promotion = true;
                                                    md.promotionPiece = promotion_pieces[prom];
                                                    moveTable[move_table_curr_ptr++] = md;
                                                }
                                            }
                                            else {
                                                MoveDescriptor md = new MoveDescriptor();
                                                md.to_i = possible_dest_x;
                                                md.to_j = possible_dest_y;
                                                md.castling = is_castling;
                                                moveTable[move_table_curr_ptr++] = md;
                                            }
                                        }
                                    }while(square_ok && (!is_single));

                                    for (int ptr = start_vector; ptr < move_table_curr_ptr; ptr++) {
                                        moveTable[ptr].next_vector = move_table_curr_ptr;
                                    }
                                }
                            }
                        }
                        //Update move table.
                        moveTable[move_table_curr_ptr] = new MoveDescriptor();
                        moveTable[move_table_curr_ptr++].end_piece = true;
                    }
                }
            }
        }
    }
}
