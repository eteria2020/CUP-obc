package eu.philcar.csg.OBC.server;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.channels.ClosedByInterruptException;
import java.security.MessageDigest;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.helpers.Compression;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.Encryption;
import eu.philcar.csg.OBC.service.ObcService;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Message;

public class UdpServer {
	private DLog dlog = new DLog(this.getClass());
	
	private final String ServerIp = App.IP_UDP_Beacon;
	private final int    ServerPort = App.Port_UDP_Beacon;
	
	private DatagramSocket socket;
	private ReceiveThread receiveThread;
	private MessageDigest md;
	
	private int errorCount = 0;
	
	private Handler serviceHandler;
	
	public void init(Handler handler) {
		
		serviceHandler = handler;
		
		try {
			if (receiveThread !=null) 
				receiveThread.interrupt();
			
			if (socket!=null)
				socket.close();
			
			socket = new DatagramSocket(ServerPort);
			socket.setSoTimeout(0);	


			md = MessageDigest.getInstance("MD5");

				
			receiveThread = new ReceiveThread();
			receiveThread.start();
			
		} catch (Exception e) {
			dlog.e("Init:",e);
		}
	}
	
	
	private void errorHandler(Exception e) {
		if (errorCount++==1) {
			dlog.e("Socket error:",e);			
			return;
		}
		
		if (errorCount>100) {
			dlog.e("Socket error (100 times) :",e);			
			errorCount=0;
		}
		
	}
	
	public void sendQuery() {
		
		String str = "?"+App.CarPlate;
		//byte[] data = str.getBytes();
		byte[] data = Compression.compress(str);
		
		InetAddress ip;
		try {
			ip = InetAddress.getByName(ServerIp);
		} catch (UnknownHostException e) {
			return;
		}
		
		final DatagramPacket rpack = new DatagramPacket(data, data.length,ip,ServerPort);
		new Thread() {
			public void run() {
				try {

					if (socket!=null) {
						socket.send(rpack);
						errorCount=0;
					}

				} catch (IOException e) {	
					errorHandler(e);					
				} 
				
			}
		}.start();
	
}
	
	public void sendBeacon(final String id) {
			
			
			byte[] data = Compression.compress(id);
			
			InetAddress ip;
			try {
				ip = InetAddress.getByName(ServerIp);
			} catch (UnknownHostException e) {
				return;
			}
			
			final DatagramPacket rpack = new DatagramPacket(data, data.length,ip,ServerPort);
			new Thread() {
				public void run() {
					try {
						//DatagramSocket sendSocket = new DatagramSocket(ServerPort+2);
						if (socket!=null) {
							dlog.d("Sending beacon: length="+rpack.getLength()+" raw=" + id.length());
							socket.send(rpack);
							errorCount=0;
						}
						//sendSocket.close();
					} catch (IOException e) {	
						errorHandler(e);
					} 
				
				}
			}.start();
		
	}
	
	private class ReceiveThread extends Thread {

		
		public ReceiveThread() {
			
		}

		@Override
		public void run() {
			
			Thread.currentThread().setName("UdpServer-ReceiveLoop");

			try {
				socket.setSoTimeout(1000);
			} catch (SocketException e1) {
			}
			
			byte[] data = new byte[1024];
			DatagramPacket pack = new DatagramPacket(data, data.length);
			while (! isInterrupted()) {
				try {
					socket.receive(pack);
										
					//byte[] hash = md.digest(data);
					
					String str = new String(pack.getData());
					//dlog.d("UDP received: " + str + "["+data.length+"]");					
					//String plainstr = Encryption.decrypt(str);
					String plainstr = str;
					//dlog.d("UDP received: " + plainstr);
					if (serviceHandler!=null) {
							Message  msg = Message.obtain();
							msg.what = ObcService.MSG_SERVER_NOTIFY;
							msg.arg1 = ObcService.SERVER_NOTIFY_RAW;
							msg.obj = plainstr;
							serviceHandler.sendMessage(msg);
							dlog.d("MSG_SERVER_NOTIFY RAW sent");
					}
					
					
					//InetAddress ip = pack.getAddress();
					//int port = pack.getPort();
					
					//DatagramPacket rpack = new DatagramPacket(hash, hash.length,ip,port);
					//socket.send(rpack);
				}	catch (SocketTimeoutException e) {
					
					}
				catch (IOException e) {
					if (!isInterrupted())
						init(serviceHandler);
					break;
				} 
		
			}
			
			return;			
		}

	}

	
	public synchronized void close() {
		if (receiveThread!=null) {
			receiveThread.interrupt();
			try {
				receiveThread.join(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			if (receiveThread.isAlive()) 
				receiveThread.stop();
			
			receiveThread=null;
		}
	}
	
	
	
}
