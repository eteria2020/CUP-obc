package eu.philcar.csg.OBC.task;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import eu.philcar.csg.OBC.helpers.DLog;

/**
 * Created by Fulvio on 17/10/2017.
 */

public class UDPServer implements Runnable {
    //TODO handle broadcast connectivity change
    @Override
    public void run() {
        Thread.currentThread().setName("UDPServer");

        while (!Thread.currentThread().isInterrupted()) {
            String text;
            int server_port = 50594;
            byte[] message = new byte[1500];
            try {
                DatagramPacket p = new DatagramPacket(message, message.length);
                DatagramSocket s = new DatagramSocket(server_port);
                s.receive(p);
                text = new String(message, 0, p.getLength());
                DLog.D("UDPServermessage:" + text);
                s.close();
            } catch (Exception e) {
                DLog.E("error  ", e);
            }
        }
    }
}
