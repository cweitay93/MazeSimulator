package comms;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class CommsMgr {

    private static CommsMgr _commsMgr = null;

    // For communication with the Raspberry-Pi
    private static final String HOST = "192.168.2.200";
    //private static final String HOST = "localhost";
    private static final int PORT = 1264;

    public static final String MSG_TYPE_ARDUINO = "a";
    public static final String MSG_TYPE_ANDROID = "b";

    private static Socket _conn = null;

    //private static BufferedOutputStream _bos = null;
    //private static OutputStreamWriter _osw = null;
    private static DataOutputStream _dos = null;
    private static BufferedReader _br = null;

    /**
     * Private constructor used to support the Singleton design pattern
     * <p>
     */
    private CommsMgr() {

    }

    /**
     * Public static function used to get hold of the CommMgr
     *
     * @return The static instance of the CommMgr
     */
    public static CommsMgr getCommsMgr() {
        if (_commsMgr == null) {
            _commsMgr = new CommsMgr();
        }

        return _commsMgr;
    }

    public boolean setConnection(int timeoutInMs) {

        try {

            _conn = new Socket();
            _conn.connect(new InetSocketAddress(HOST, PORT), timeoutInMs);
            _conn.setSoTimeout(timeoutInMs);
            //_bos = new BufferedOutputStream(_conn.getOutputStream());
            _dos = new DataOutputStream(_conn.getOutputStream());
            //_osw = new OutputStreamWriter(_bos, "US-ASCII");
            _br = new BufferedReader(new InputStreamReader(_conn.getInputStream()));
            
            // Successful connection, return true
            System.out.println("setConnection() ->" + " Connection established successfully!");
            return true;

        } catch (UnknownHostException e) {
            System.out.println("setConnection() -> Unknown Host Exception");
        } catch (IOException e) {
            System.out.println("setConnection() -> IO Exception");
        } catch (Exception e) {
            System.out.println("setConnection() -> Exception");
        }

        System.out.println("Failed to establish connection!");
        return false;
    }

    public void closeConnection() {
        try {
//            if (_bos != null) {
//                _bos.close();
//            }
//            if (_osw != null) {
//                _osw.close();
//            }
            if (_br != null) {
                _br.close();
            }
            
            if (_dos != null){
                _dos.close();
            }

            if (_conn != null) {
                _conn.close();
                _conn = null;
            }

        } catch (IOException e) {
            System.out.println("closeConnection() -> IO Exception");
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.out.println("closeConnection() -> Null Pointer Exception");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("closeConnection() -> Exception");
            e.printStackTrace();
        }
    }

    public boolean sendMsg(String msg, String msgType, boolean ack) {
        try {
            String outputMsg = msgType + msg;

            outputMsg = String.format("%-128s", outputMsg);
            System.out.println("Sending out msg: " + outputMsg);
            byte[] mBytes = outputMsg.getBytes();
            _dos.write(mBytes, 0, mBytes.length);

            //_osw.write(outputMsg);
            _dos.flush();

            return true;
        } catch (IOException e) {
            System.out.println("sendMsg() -> IOException");
        } catch (Exception e) {
            System.out.println("sendMsg() -> Exception");
        }

        return false;
    }

    public String recvMsg() {
        try {
            String input = _br.readLine();
            if (input != null && input.length() > 0) {
                System.out.println(input);
                return input;
            }

        } catch (IOException e) {
            //System.out.println("recvMsg() -> IO exception");
        } catch (Exception e) {
            //System.out.println("recvMsg() -> Exception");
        }

        return null;
    }

    public boolean isConnected() {
        return _conn.isConnected();
    }

}
