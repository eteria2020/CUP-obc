package eu.philcar.csg.OBC.server;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import java.io.UnsupportedEncodingException;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.Encryption;
import eu.philcar.csg.OBC.service.MessageFactory;
import eu.philcar.csg.OBC.service.ObcService;
import zmq.ZError;

public class ZmqSubscriber {
	private DLog dlog = new DLog(this.getClass());

	private ZmqRunnable zmqRunnable;
	private Thread zmqThread;
	private final Handler handler;  //csd 2086

	public ZmqSubscriber(Handler handler) {
		this.handler = handler;
	}

	public void Start(Handler serviceHandler) {
		try {

			dlog.i("ZmqSubscriber.start();ZMQ thread");
			//handler = serviceHandler;
			zmqRunnable = new ZmqRunnable();
			zmqThread = new Thread(zmqRunnable);
			zmqThread.start();
		} catch (Exception e) {
			dlog.e("ZmqSubscriber.start();Exception while starting thread ", e);
		}
	}

	public void Restart(Handler serviceHandler) {
		dlog.i("ZmqSubscriber.Restart();ZMQ thread");
		if (zmqThread != null && zmqRunnable != null) {

			if (zmqRunnable.isStarting)
				return;

			zmqRunnable.zmqStop(zmqThread);
			try {
				zmqThread.join(5000);
			} catch (InterruptedException e) {
				dlog.e("ZmqSubscriber.start();ZMQException ", e);

			}

		}

		Start(serviceHandler);

	}

	public void Stop() {
		dlog.i("Stopping ZMQ thread");
		if (zmqThread != null) {
			zmqThread.interrupt();
		}
	}

	private void handleZmqMessage(String channel, String payload) {
		dlog.d("ZmqSubscriber.handleZmqMessage();Received ZMQ message from channel '" + channel + "' with payload : '" + payload + "'");

		Message msg = handler.obtainMessage(ObcService.MSG_SERVER_NOTIFY);
		msg.arg1 = ObcService.SERVER_NOTIFY_RAW;
		msg.obj = payload;

		handler.sendMessage(msg);

	}

	private class ZmqRunnable implements Runnable {
		private ZMQ.Context context;
		private ZMQ.Socket socket;

		private void zmqStop(final Thread th) {

			if (th == null)
				return;

			Thread t = new Thread() {
				public void run() {
					try {

						context.term();
						dlog.i("ZmqSubscriber.zmqStop();ZMQ context terminated");
						th.interrupt();
						dlog.i("ZmqSubscriber.zmqStop();Thread interrupted");
					} catch (Exception e) {
						dlog.e("ZmqSubscriber.zmqStop();Exception in zmqStop", e);
					}
				}

			};

			t.start();

		}

		private boolean isStarting = false;

		@Override
		public void run() {

			try {
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

				dlog.i("ZmqSubscriber.run();Connecting to:" + App.Instance.getString(R.string.endpointSharengoZMQ));
				socket.connect(App.Instance.getString(R.string.endpointSharengoZMQ));
				//socket.subscribe(ZMQ.SUBSCRIPTION_ALL);
				dlog.i("ZmqSubscriber.run();Subscribing to channels: COMMON," + App.CarPlate);
				socket.subscribe(App.CarPlate.getBytes());
				socket.subscribe("COMMON".getBytes());

				String channel;
				dlog.i("ZmqSubscriber.run();thread started");
				isStarting = false;
				while (!Thread.currentThread().isInterrupted()) {
					try {
						channel = socket.recvStr();
					} catch (ZMQException e) {
						if (e.getErrorCode() == ZMQ.Error.ETERM.getCode()) {
							dlog.w("ZmqSubscriber.run();recvStr terminated");
							break;
						} else {
							dlog.e("ZmqSubscriber.run();recvStr", e);
							break;
						}
					}
					if (socket.hasReceiveMore()) {

						String str = "";
						try {
							byte[] zmsg = socket.recv(0);
							if(zmsg!=null) {
								String strRaw = new String(zmsg, "UTF-8");
								str = Encryption.decryptAes(strRaw);
								handleZmqMessage(channel, str);
							}else {
								dlog.e("ZmqSubscriber.run();zmsg is null");
							}

						} catch (UnsupportedEncodingException e) {
							dlog.e("ZmqSubscriber.run();message exception", e);
						}
						Log.d("ZMQ", str);
					}
				}

				dlog.i("ZmqSubscriber.run();thread exited");
				socket.close();
				dlog.i("ZmqSubscriber.run();socket closed");

			} catch (ZError.IOException e) {

				isStarting = false;
				dlog.e("ZmqSubscriber.run();Maybe A problem of too many file opened restarting OBC", e);
				throw e;
			} catch (Exception e) {

				isStarting = false;
				dlog.e("ZmqSubscriber.run();", e);
				if (App.hasNetworkConnection())
					handler.sendMessageDelayed(MessageFactory.zmqRestart(), 5000);
			}

		}

	}

}
