package eu.philcar.csg.OBC.service;

import eu.philcar.csg.OBC.helpers.DLog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

public class ServiceConnector {
	
	private boolean mIsBound;
	private Context context;
	private String name;
	private Messenger localMessenger = null;
	private Messenger serviceMessenger = null;
	private int type;
	
	public ServiceConnector(Context ctx, Handler handler) {
		this(ctx,handler,null);
	}
	
	public ServiceConnector(Context ctx, Handler handler, String name) {
		this.context = ctx;

		if (handler!=null)
			this.localMessenger = new Messenger(handler);

		this.name = name;
	}
	
	
	public boolean isConnected() {
		return mIsBound;
	}
	
	public void startService() {
		if (context != null) {
			context.startService(new Intent(context, ObcService.class));
			DLog.I("Requested service start");
		} else {
			DLog.E("Requested service start failed. Context null ");
		}
	}
	

	public void connect() {
		if (context != null) {
			context.bindService(new Intent(context,ObcService.class),mConnection,Context.BIND_AUTO_CREATE);
			DLog.I("Requested service connection");
		} else {
			DLog.E("Requested service connection failed. Context null ");
		}
	}


	public void connect(int type) {
		if (context != null) {
			context.bindService(new Intent(context,ObcService.class),mConnection,Context.BIND_AUTO_CREATE);
			this.type=type;
			DLog.I("Requested service connection");
		} else {
			DLog.E("Requested service connection failed. Context null ");
		}
	}
	
	public void unregister() {
		if (serviceMessenger!=null) {
			try {
				Message msg = Message.obtain(null, ObcService.MSG_CLIENT_UNREGISTER);
				msg.replyTo = localMessenger;
				msg.arg1=type;
				serviceMessenger.send(msg);
				DLog.I("Requested service unregistration");
			} catch (RemoteException e) {
				DLog.E("Unregistration failed",e);
			}
		}
				
	}

	
	public void disconnect() {
		if (context != null) {
			try {

				Message msg = Message.obtain(null, ObcService.MSG_CLIENT_UNREGISTER);
				msg.replyTo = localMessenger;
				msg.arg1=type;
				serviceMessenger.send(msg);

				context.unbindService(mConnection);
				DLog.I("Requested unbinding");
			} catch (Exception e) {
				DLog.E("Error diconnecting service",e);
			} 
			mIsBound=false;
		} else {
			DLog.E("Unbinding failed");
		}
		
	}
	
	public boolean send(Message msg) {
		if (serviceMessenger != null) {
			msg.replyTo = localMessenger;
			try {
				serviceMessenger.send(msg);
				DLog.I("Sent message : " + msg.what);
				
				return true;
			} catch (RemoteException e) {
				DLog.E("Error sending message",e);
			}
			
		}
		return false;
	}
	
	private ServiceConnection mConnection = new ServiceConnection() {
		
		public void onServiceConnected(ComponentName className, IBinder service) {
			
			mIsBound = true;
			DLog.I("Service connected");
			
			serviceMessenger = new Messenger(service);
			
			try {
				//Register client in service
				Message msg =ObcService.obtainRegistrationMessage(ServiceConnector.this.name); 
				msg.replyTo = localMessenger;
				msg.arg1=type;
				serviceMessenger.send(msg);
				
				
				// Notify connection to client 
				localMessenger.send(Message.obtain(null, ObcService.MSG_CLIENT_REGISTER));
				
			} catch (RemoteException e) {
				DLog.E("Client registration failed",e);
			}
			
		}

		public void onServiceDisconnected(ComponentName className) {
			DLog.E("Service disconnected");
			// This is called when the connection with the service has been
			// unexpectedly disconnected - process crashed.			
			try {
				localMessenger.send(Message.obtain(null, ObcService.MSG_CLIENT_UNREGISTER));
			} catch (RemoteException e) { }
			serviceMessenger = null;
			//context.stopService(new Intent(context, ObcService.class));
			
		}
	};

}
