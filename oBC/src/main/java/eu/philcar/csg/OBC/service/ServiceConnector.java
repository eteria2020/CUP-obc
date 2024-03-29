package eu.philcar.csg.OBC.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import eu.philcar.csg.OBC.helpers.DLog;

public class ServiceConnector {

	private boolean mIsBound;
	private Context context;
	private String name;
	private Messenger localMessenger = null;
	private Messenger serviceMessenger = null;
	private int type;

	public ServiceConnector(Context ctx, Handler handler) {
		this(ctx, handler, null);
	}

	private ServiceConnector(Context ctx, Handler handler, String name) {
		this.context = ctx;

		if (handler != null)
			this.localMessenger = new Messenger(handler);

		this.name = name;
	}

	public boolean isConnected() {
		return mIsBound;
	}

	public void startService() {
		if (context != null) {
			context.startService(new Intent(context, ObcService.class));
			DLog.I("ServiceConnector.startService();Requested service start");
		} else {
			DLog.E("ServiceConnector.startService();Requested service start failed. Context null ");
		}
	}

	public void connect() {
		if (context != null) {
			context.bindService(new Intent(context, ObcService.class), mConnection, Context.BIND_AUTO_CREATE);
			DLog.I("ServiceConnector.connect();Requested service connection");
		} else {
			DLog.E("ServiceConnector.connect();Requested service connection failed. Context null ");
		}
	}

	public void connect(int type) {
		if (context != null) {
			context.bindService(new Intent(context, ObcService.class), mConnection, Context.BIND_AUTO_CREATE);
			this.type = type;
			DLog.I("ServiceConnector.connect();Requested service connection");
		} else {
			DLog.E("ServiceConnector.connect();Requested service connection failed. Context null ");
		}
	}

	public void unregister() {
		if (serviceMessenger != null) {
			try {
				Message msg = Message.obtain(null, ObcService.MSG_CLIENT_UNREGISTER);
				msg.replyTo = localMessenger;
				msg.arg1 = type;
				serviceMessenger.send(msg);
				DLog.I("ServiceConnector.unregister();Requested service unregistration");
			} catch (RemoteException e) {
				DLog.E("ServiceConnector.unregister();Unregistration failed", e);
			}
		}

	}

	public void disconnect() {
		if (context != null) {
			try {

				Message msg = Message.obtain(null, ObcService.MSG_CLIENT_UNREGISTER);
				msg.replyTo = localMessenger;
				msg.arg1 = type;
				serviceMessenger.send(msg);

				context.unbindService(mConnection);
				DLog.I("ServiceConnector.disconnect();Requested unbinding");
			} catch (Exception e) {
				DLog.E("ServiceConnector.disconnect();Error diconnecting service", e);
			}
			mIsBound = false;
		} else {
			DLog.E("ServiceConnector.disconnect();Unbinding failed");
		}

	}

	public boolean send(Message msg) {
		if (serviceMessenger != null) {
			msg.replyTo = localMessenger;
			try {
				serviceMessenger.send(msg);
				DLog.I("ServiceConnector.send();Sent message : " + msg.what);

				return true;
			} catch (RemoteException e) {
				DLog.E("ServiceConnector.send();Error sending message", e);
			}

		}
		return false;
	}

	private ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {

			mIsBound = true;
			DLog.I("ServiceConnector.onServiceConnected();Service connected");

			serviceMessenger = new Messenger(service);

			try {
				//Register client in service
				Message msg = ObcService.obtainRegistrationMessage(ServiceConnector.this.name);
				msg.replyTo = localMessenger;
				msg.arg1 = type;
				serviceMessenger.send(msg);

				// Notify connection to client
				localMessenger.send(Message.obtain(null, ObcService.MSG_CLIENT_REGISTER));

			} catch (RemoteException e) {
				DLog.E("ServiceConnector.onServiceConnected();Client registration failed", e);
			}

		}

		public void onServiceDisconnected(ComponentName className) {
			DLog.E("ServiceConnector.onServiceConnected();Service disconnected");
			// This is called when the connection with the service has been
			// unexpectedly disconnected - process crashed.
			try {
				localMessenger.send(Message.obtain(null, ObcService.MSG_CLIENT_UNREGISTER));
			} catch (RemoteException e) {
				DLog.E("ServiceConnector.onServiceConnected();",e);
			}
			serviceMessenger = null;
			//context.stopService(new Intent(context, ObcService.class));

		}
	};

}
