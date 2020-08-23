public class MoveDescriptor {

    public int to_i;
    public int to_j;

    public boolean end_piece;
    public boolean castling;
    public boolean promotion;
    char promotionPiece;

    public int next_vector;

    public MoveDescriptor() {
        end_piece = false;
        castling = false;
        promotion = false;
        promotionPiece = ' ';
        to_i = 0;
        to_j = 0;
    }
}
