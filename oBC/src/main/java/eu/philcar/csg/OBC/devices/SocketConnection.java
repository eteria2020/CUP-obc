package eu.philcar.csg.OBC.devices;

import android.content.Context;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import eu.philcar.csg.OBC.helpers.DLog;

//import rfidTest.qnet.it.DemoActivity.myconnectThread;

public class SocketConnection {

    private Context context;
    private Handler extHandler;

    private ServerSocket serverSocket;
    private Socket clientSocket;

    private InputStream inStream;
    private OutputStream outStream;

    private ConnectThread connectThread = null;
    private ReceiveThread receiveThread = null;
    private int CurrentState;
    private boolean connected = false;

    public static final int MSG_STATE_CHANGE = 1;
    public static final int MSG_RECEIVE = 2;

    public SocketConnection(Context context, Handler handler) {

        setContext(context);
        setHandler(handler);
    }

    public synchronized void listen() {

        connectThread = new ConnectThread();
        connectThread.start();

    }

    public void setHandler(Handler handler) {
        if (extHandler != null) {
            extHandler.removeCallbacks(connectThread);
        }

        this.extHandler = handler;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void Close() {

        if (connectThread != null) {
            connectThread.interrupt();
            connectThread = null;
        }

        if (receiveThread != null) {
            receiveThread.interrupt();

            try {
                receiveThread.join(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            receiveThread = null;
        }

        if (extHandler != null) {
            extHandler.removeCallbacks(connectThread);
        }
        connected = false;
    }

    private class ConnectThread extends Thread {

        int countDownTime = 0;
        int timeout = 0;
        boolean isConnected;

        @Override
        public void run() {
            DLog.I("Starting socket connection loop");

            Thread.currentThread().setName("SocketConnection-ConnectThread");

            try {
                serverSocket = new ServerSocket(8881);
                serverSocket.setSoTimeout(2000);
            } catch (IOException e) {
            }

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    clientSocket = serverSocket.accept();

                    ReceiveThread receiveThread = new ReceiveThread(clientSocket);
                    new Thread(receiveThread).start();
                } catch (IOException e) {

                } catch (Exception e) {
                    break;
                }

            }

        }

    }

    public void SendBytes(byte data[]) {
        if (clientSocket == null || clientSocket.isClosed())
            return;

        OutputStream os;

        try {
            os = clientSocket.getOutputStream();
            os.write(data);
        } catch (IOException e) {

        }
    }

    public void SendString(String str) {

        if (str == null || clientSocket == null || clientSocket.isClosed())
            return;

        OutputStream os;
        try {
            os = clientSocket.getOutputStream();
            byte[] buffer = str.getBytes();
            os.write(buffer);
            os.flush();
            DLog.D("BT send string:" + str);
        } catch (IOException e) {

        }
    }

    private class ReceiveThread extends Thread {
        StringBuilder sb;
        Socket clientSocket;

        public ReceiveThread(Socket socket) {
            clientSocket = socket;
            try {
                inStream = socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {

            Thread.currentThread().setName("SocketConnection-ReceiveLoop");

            sb = new StringBuilder();
            int ch = 0;
            while (!this.isInterrupted() && clientSocket.isConnected()) {

                try {
                    ch = inStream.read();
                } catch (IOException e) {
                    return;
                } catch (Exception e) {
                    break;
                }

                if (ch < 0) {
                    DLog.E("Input stream closed...");
                    return;
                }

                if (ch == 0)
                    continue;

                if (ch != '\n') {
                    sb.append((char) ch);
                } else {
                    extHandler.obtainMessage(MSG_RECEIVE, sb.toString()).sendToTarget();
                    sb = new StringBuilder();
                }

            }

        }
    }

}
