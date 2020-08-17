import java.io.InputStream;
import java.io.IOException;

public class MyRunnable implements Runnable {

    public String kind;
    public InputStream std_in;
    public Board b;

    public void run(){
        int i=0;
        String buffer="";

        if(kind.equals("do_deep")) {
            b.g.do_deep();
            return;
        }

        if(kind.equals("update_deep")) {
            b.g.update_deep();
            return;
        }

        if(kind.equals("engine_read")) {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    char chunk = (char) std_in.read();
                    if(chunk == '\n') {
                        buffer = "";//Clear buffer
                    }
                    else {
                        buffer += chunk;
                    }
                }
                catch(IOException e) {
                    //Shouldn't reach here
                    System.out.println("engine read IO exception");
                }
            }
            return;
        }
    }
}
