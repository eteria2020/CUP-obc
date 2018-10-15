package eu.philcar.csg.OBC;

import android.util.Log;

import org.zeromq.ZMQ;

import java.io.UnsupportedEncodingException;

import eu.philcar.csg.OBC.helpers.Encryption;

public class zmqTest implements Runnable {

	@Override
	public void run() {

		ZMQ.Context context = ZMQ.context(1);
		ZMQ.Socket socket = context.socket(ZMQ.SUB);
		socket.connect("tcp://api.sharengo.it:3001");
		//socket.subscribe(ZMQ.SUBSCRIPTION_ALL);
		socket.subscribe("DEMO4".getBytes());

		while (!Thread.currentThread().isInterrupted()) {
			String channel = socket.recvStr();
			if (socket.hasReceiveMore()) {
				byte[] msg = socket.recv(0);
				String str = "";
				try {
					String strRaw = new String(msg, "UTF-8");
					str = Encryption.decrypt(strRaw);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				Log.d("ZMQ", str);
			}
		}
		socket.close();
		context.term();

	}

}
