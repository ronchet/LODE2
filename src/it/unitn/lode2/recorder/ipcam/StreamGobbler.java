package it.unitn.lode2.recorder.ipcam;

import java.io.*;

/**
 * User: tiziano
 * Date: 09/03/15
 * Time: 11:03
 */
public class StreamGobbler extends Thread {

    InputStream is;
    Boolean toTerminate = Boolean.FALSE;

    public StreamGobbler(InputStream is) {
        this.is = is;
    }

    @Override
    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while( !toTerminate && (line = br.readLine()) != null ) {
                System.out.println(line);
            }
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void terminate() {
        toTerminate = Boolean.TRUE;
    }
}
