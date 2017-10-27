package eu.philcar.csg.OBC.server;

import java.io.UnsupportedEncodingException;

import org.json.JSONException;
import org.json.JSONObject;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.Encryption;
import eu.philcar.csg.OBC.service.MessageFactory;
import eu.philcar.csg.OBC.service.ObcService;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ZmqSubscriber {
	private DLog dlog = new DLog(this.getClass());

	private ZmqRunnable zmqRunnable;
	private Thread  zmqThread;
	private Handler handler;

	public void Start(Handler serviceHandler) {
		try {

			dlog.d("Starting ZMQ thread");
			handler = serviceHandler;
			zmqRunnable = new ZmqRunnable();
			zmqThread = new Thread(zmqRunnable);
			zmqThread.start();
		}catch (Exception e){
			dlog.e("Exception while starting thread ",e);
		}
	}

	public void Restart(Handler serviceHandler) {
		dlog.d("Restarting ZMQ thread");
		if (zmqThread!=null && zmqRunnable!=null) {

			if (zmqRunnable.isStarting)
				return;

			zmqRunnable.zmqStop(zmqThread);
			try {
				zmqThread.join(5000);
			} catch (InterruptedException e) {
				dlog.e("ZMQException ",e);

			}	
			
		}

		Start(serviceHandler);


	}


	public void Stop() {
		dlog.d("Stopping ZMQ thread");
		if (zmqThread!=null) {
			zmqThread.interrupt();
		}
	}

	private void handleZmqMessage(String channel, String payload) {
		dlog.d("Received ZMQ message from channel '"+channel+"' with payload : '"+payload+"'");

		Message msg = handler.obtainMessage(ObcService.MSG_SERVER_NOTIFY);
		msg.arg1= ObcService.SERVER_NOTIFY_RAW;
		msg.obj = payload;

		handler.sendMessage(msg);

	}

	private class ZmqRunnable implements Runnable {
		private ZMQ.Context context;
		private ZMQ.Socket socket;

		public void zmqStop(final Thread th) {

			if (th==null)
				return;

			Thread t = new Thread()	{
				public void run()    {
					try {

						context.term();
						dlog.d("ZMQ context terminated");
						th.interrupt();
						dlog.d("Thread interrupted");
					}catch(Exception e){
						dlog.e("Exception in zmqStop",e);
					}
				}

			};

			t.start();

		}

		public boolean isStarting=false;

		@Override
		public void run() {

			try{
				Thread.currentThread().setName("ZMQSubscriber");
				isStarting = true;
				context = ZMQ.context(1);

				socket = context.socket(ZMQ.SUB);
				socket.setLinger(0);
				socket.setSndHWM(10);
				socket.setRcvHWM(10);
				socket.setTCPKeepAlive(1);
				socket.setTCPKeepAliveCount(1);
				socket.setTCPKeepAliveIdle(60);
				socket.setTCPKeepAliveInterval(60);
				socket.setReconnectIVL(1000);  //TODO: Add random  component

				dlog.d("ZMQ Connecting to:" + App.URL_ZMQNotifier);
				socket.connect(App.URL_ZMQNotifier);
				//socket.subscribe(ZMQ.SUBSCRIPTION_ALL);
				dlog.d("ZMQ Subscribing to channels: COMMON," + App.CarPlate);
				socket.subscribe(App.CarPlate.getBytes());
				socket.subscribe("COMMON".getBytes());

				String channel = "";
				dlog.d("ZMQ thread started");
				isStarting = false;
				while (!Thread.currentThread().isInterrupted()) {
					try {
						channel = socket.recvStr();
					} catch (ZMQException e) {
						if (e != null && e.getErrorCode() == ZMQ.Error.ETERM.getCode()) {
							dlog.w("ZMQ recvStr terminated");
							break;
						} else {
							dlog.e("ZMQ recvStr", e);
							break;
						}
					}
					if (socket.hasReceiveMore()) {

						byte[] zmsg = socket.recv(0);

						String str = "";
						try {
							String strRaw = new String(zmsg, "UTF-8");
							str = Encryption.decryptAes(strRaw);
							handleZmqMessage(channel, str);
						} catch (UnsupportedEncodingException e) {
							dlog.e("ZMQ message exception", e);
						}
						Log.d("ZMQ", str);

					}
				}

				dlog.d("ZMQ thread exited");
				socket.close();
				dlog.d("ZMQ socket closed");


			}catch(Exception e){

				isStarting=false;
				dlog.e("ZMQException ",e);
				handler.sendMessageDelayed(MessageFactory.zmqRestart(),5000);
			}

		}

	}

}
