public class MoveDescriptor {

    public int to_i;
    public int to_j;

    public boolean end_piece;
    public boolean castling;
    public boolean promotion;
    char prom_piece;

    public int next_vector;

    public MoveDescriptor() {
        end_piece = false;
        castling = false;
        promotion = false;
        prom_piece = ' ';
    }
}
