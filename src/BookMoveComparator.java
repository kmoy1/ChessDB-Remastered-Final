import java.util.Comparator;

class BookMoveComparator implements Comparator<BookMove>
{
    public int compare(BookMove b1, BookMove b2)
    {
        if(b1.notation!=b2.notation)
        {
            return b2.notation-b1.notation;
        }
        else if(b1.eval!=b2.eval)
        {
            return b2.eval-b1.eval;
        }
        else
        {
            return b2.count-b1.count;
        }
    }
}