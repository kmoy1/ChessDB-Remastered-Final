import java.util.Hashtable;

public class BookMove {

    public String san;
    public int notation;
    public Boolean is_analyzed;
    public int eval;
    public int count;

    public Hashtable report_hash() {
        Hashtable hash = new Hashtable();
        hash.put("notation", "" + notation);
        hash.put("is_analyzed", "" + is_analyzed);
        hash.put("eval", "" + eval);
        hash.put("count", "" + count);
        return hash;
    }

    public void set_from_hash(Hashtable hash) {
        if(hash.get("notation") != null) {
            notation = Integer.parseInt(hash.get("notation").toString());
        }

        if(hash.get("is_analyzed") != null) {
            is_analyzed=hash.get("is_analyzed").toString().equals("true");
        }

        if(hash.get("eval") != null) {
            eval = Integer.parseInt(hash.get("eval").toString());
        }

        if(hash.get("count")!=null) {
            count = Integer.parseInt(hash.get("count").toString());
        }
    }

    public BookMove(String set_san) {
        san = set_san;
        notation = -1;
        is_analyzed = false;
        eval = 0;
        count = 0;
    }
    
}