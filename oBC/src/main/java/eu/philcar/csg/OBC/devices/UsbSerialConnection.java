package eu.philcar.csg.OBC.devices;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Handler;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.usbserial.driver.UsbSerialDriver;
import eu.philcar.usbserial.driver.UsbSerialPort;
import eu.philcar.usbserial.driver.UsbSerialProber;
import eu.philcar.usbserial.util.SerialInputOutputManager;

public class UsbSerialConnection {
	private DLog dlog = new DLog(this.getClass());

	private UsbSerialPort sPort = null;
	private List<UsbSerialPort> usbSerialPortList;

	private SerialInputOutputManager mSerialIoManager;
	private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

	public static final int MSG_RECEIVE = 2;
	private Handler extHandler = null;

	private final SerialInputOutputManager.Listener mListener =
			new SerialInputOutputManager.Listener() {

				@Override
				public void onRunError(Exception e) {
					dlog.d("Runner stopped.");
				}

				StringBuilder sBuffer;

				@Override
				public void onNewData(final byte[] data) {

					try {
						if (sBuffer == null)
							sBuffer = new StringBuilder();

						sBuffer.append(new String(data, "UTF-8"));

						int i;
						while ((i = sBuffer.indexOf("\n")) >= 0) {
							String str = sBuffer.substring(0, i);
							if (i < sBuffer.length())
								sBuffer = new StringBuilder(sBuffer.substring(i + 1));
							else
								sBuffer = null;

							dlog.d("USB RX: " + str);

							if (extHandler != null)
								extHandler.obtainMessage(MSG_RECEIVE, str).sendToTarget();
						}

					} catch (UnsupportedEncodingException e) {

					}

				}
			};

	private final String permissionAction = "PermissionGranted";

	public void startIntentFilter(Context ctx) {

		IntentFilter filter = new IntentFilter(UsbReceiverActivity.ACTION_USB_DEVICE_ATTACHED);
		filter.addAction(permissionAction);
		ctx.registerReceiver(mUsbAttachedReceiver, filter);

	}

	private final BroadcastReceiver mUsbAttachedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (action == null)
				return;

			dlog.d("Received action : " + action);

			if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action) || UsbReceiverActivity.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
				synchronized (this) {
					UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
					dlog.d("Extra_device:" + device.toString());
					enumerate(context);
					setPort(1);
					if (device != null) {
						connect(context, device);
					}
				}
			} else if (permissionAction.equals(action)) {
				connect(context);
			}
		}
	};

	public void setPort(int n) {
		if (n > 0 && usbSerialPortList != null && usbSerialPortList.size() >= n) {
			sPort = usbSerialPortList.get(n - 1);
			dlog.i("Selected port n. : " + n);
		}
	}

	public void setHandler(Handler handler) {
		extHandler = handler;
	}

	public int enumerate(Context ctx) {
		final UsbManager usbManager = (UsbManager) ctx.getSystemService(Context.USB_SERVICE);
		dlog.i("UsbManager :" + usbManager != null ? "OK" : "fail");

		final List<UsbSerialDriver> drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);

		final List<UsbSerialPort> result = new ArrayList<UsbSerialPort>();
		for (final UsbSerialDriver driver : drivers) {
			final List<UsbSerialPort> ports = driver.getPorts();
			dlog.d(String.format("+ %s: %s port%s",
					driver, Integer.valueOf(ports.size()), ports.size() == 1 ? "" : "s"));
			result.addAll(ports);
		}

		usbSerialPortList = result;

		return result.size();
	}

	public void connect(Context ctx) {
		connect(ctx, null);
	}

	public void connect(Context ctx, UsbDevice usbDevice) {

		if (sPort == null) {
			dlog.e("Port is null");
			return;
		}

		final UsbManager usbManager = (UsbManager) ctx.getSystemService(Context.USB_SERVICE);

		if (usbDevice == null)
			usbDevice = sPort.getDriver().getDevice();

		if (!usbManager.hasPermission(usbDevice)) {
			PendingIntent pi = PendingIntent.getBroadcast(ctx, 0, new Intent(permissionAction), 0);
			usbManager.requestPermission(usbDevice, pi);
			return;
		}
		UsbDeviceConnection connection = usbManager.openDevice(usbDevice);

		if (connection == null) {
			dlog.e("Opening device failed");
			return;
		}

		try {
			sPort.open(connection);
			sPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
			dlog.d("Port opened");
			startIoManager();
		} catch (Exception e) {
			dlog.e("Opening or setting port: ", e);
			try {
				sPort.close();
			} catch (Exception e1) {

			}
			return;
		}

	}

	private void connectDevice() {

	}

	private void stopIoManager() {
		if (mSerialIoManager != null) {
			dlog.i("Stopping io manager ..");
			mSerialIoManager.stop();
			mSerialIoManager = null;
		}
	}

	private void startIoManager() {
		if (sPort != null) {
			dlog.i("Starting io manager ..");
			mSerialIoManager = new SerialInputOutputManager(sPort, mListener);
			mExecutor.submit(mSerialIoManager);
		}
	}

	private void onDeviceStateChange() {
		stopIoManager();
		startIoManager();
	}

}
