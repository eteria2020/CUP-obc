package eu.philcar.csg.OBC.server;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.apache.http.NameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.Encryption;

public class ZmqRequester {
	private DLog dlog = new DLog(this.getClass());

//	private ZMQ.Context zmqContext;
	private ZMQ.Socket zmqSocket;
	private int counter = 0;
	private ExecutorService executor;

	private class Request {

		private Request(RemoteEntityInterface entity, Handler handler, Message msg) {
			this((String) null, handler, msg);
			this.entity = entity;
			if (entity != null) {
				this.payload = ParamsToJson(entity.GetParams());
			}
		}

		private Request(String payload, Handler handler, Message msg) {
			this.payload = payload;
			this.msg = msg;
			this.handler = handler;
			this.sent = System.currentTimeMillis();
			this.entity = null;

		}

		public String sid;
		public String payload;
		public long sent;
		public Message msg;
		public Handler handler;
		public RemoteEntityInterface entity;

	}

	private HashMap<String, Request> requests;

	public ZmqRequester() {

		requests = new HashMap<>();
		executor = Executors.newCachedThreadPool();

//		zmqContext = ZMQ.context(1);
		ZMQ.Context zmqContext = ZMQ.context(1);
		zmqSocket = zmqContext.socket(ZMQ.REQ);
		zmqSocket.setIdentity(App.CarPlate.getBytes());
		zmqSocket.setLinger(5000);
		zmqSocket.setSndHWM(10);
		zmqSocket.setRcvHWM(10);

		Start();
	}

	private ZmqRunnable zmqRunnable;
	private Thread zmqThread;
	private Handler handler;

	private String ParamsToJson(List<NameValuePair> list) {

		if (list == null || list.size() == 0)
			return "{}";

		JSONObject jobj = new JSONObject();

		for (NameValuePair pair : list) {
			try {
				jobj.put(pair.getName(), pair.getValue());
			} catch (JSONException e) {
				dlog.e("Error build JSON", e);
			}
		}

		return jobj.toString();

	}

	public synchronized void Send(RemoteEntityInterface entity, Handler handler, Message msg) {

		Request req = new Request(entity, handler, msg);
		Send(req);

	}

	public void Send(String payload, Handler handler, Message msg) {

		Request req = new Request(payload, handler, msg);
		Send(req);

	}

	private synchronized void Send(final Request req) {

		if (req == null)
			return;

		counter++;

		req.sid = App.CarPlate + "_" + counter;
		requests.put(req.sid, req);
		executor.submit(new Runnable() {

			@Override
			public void run() {
				zmqSocket.send(req.sid);
				zmqSocket.sendMore(req.payload);
			}

		});

	}

	private void Start() {

		dlog.d("ZmqReuester.Restart();Starting thread");
		zmqRunnable = new ZmqRunnable();
		zmqThread = new Thread(zmqRunnable);
		zmqThread.start();
	}

	public void Restart() {
		dlog.d("ZmqReuester.Restart();thread");
		if (zmqThread != null && zmqRunnable != null) {
			zmqRunnable.zmqStop(zmqThread);
			try {
				zmqThread.join(5000);
			} catch (InterruptedException e) {
				DLog.E("ZmqReuester.Restart();", e);
			}

		}

		zmqRunnable = new ZmqRunnable();
		zmqThread = new Thread(zmqRunnable);
		zmqThread.start();

	}

	private void handleZmqMessage(String sid, String payload) {

		dlog.d("ZmqReuester.Restart();Received ZMQ message sid: '" + sid + "' with payload : '" + payload + "'");

		if (requests == null)
			return;

		if (!requests.containsKey(sid)) {
			dlog.e("Requests list does not contain this sid :" + sid);
			return;
		}

		Request req = requests.get(sid);

		Message msg;
		if (req.msg != null)
			msg = req.msg;
		else if (req.handler != null)
			msg = req.handler.obtainMessage();
		else
			msg = new Message();

		//If there is an etitity object let it handle the response
		if (req.entity != null) {
			msg.arg1 = req.entity.DecodeJson(payload);
			msg.what = req.entity.MsgId();
		} else {
			msg.obj = payload;
		}

		if (req.handler != null)
			handler.sendMessage(msg);

		requests.remove(sid);

	}

	private class ZmqRunnable implements Runnable {
		private ZMQ.Context context;

		private void zmqStop(final Thread th) {

			Thread t = new Thread() {
				public void run() {
					context.term();
					th.interrupt();
				}
			};

			t.start();

		}

		@Override
		public void run() {

			zmqSocket.connect(App.Instance.getString(R.string.endpointSharengoZMQ));

			String sid = "";
			while (!Thread.currentThread().isInterrupted()) {
				try {
					sid = zmqSocket.recvStr();
				} catch (ZMQException e) {
					if (e.getErrorCode() == ZMQ.Error.ETERM.getCode()) {
						break;
					}
					try {
						Thread.sleep(100);
					} catch (InterruptedException e1) {
						DLog.E("ZmqRequester.ZmqRunnable.run();", e1);
					}
				}
				if (zmqSocket.hasReceiveMore()) {

					byte[] zmsg = zmqSocket.recv(0);

					String str = "";
					try {
						String strRaw = new String(zmsg, "UTF-8");
						str = Encryption.decryptAes(strRaw);
						handleZmqMessage(sid, str);
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
					DLog.D("ZmqRequester.run();" + str);
				}
			}
			zmqSocket.close();

		}

	}

}
