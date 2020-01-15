package eu.philcar.csg.OBC.devices;

import android.app.Activity;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Parcelable;

import eu.philcar.csg.OBC.helpers.DLog;

public class UsbReceiverActivity extends Activity {
	private DLog dlog = new DLog(this.getClass());

	public static final String ACTION_USB_DEVICE_ATTACHED = "eu.philcar.csg.ACTION_USB_DEVICE_ATTACHED";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		dlog.d("UsbReceiverActivity.onCreate();");
	}

	@Override
	protected void onResume() {
		super.onResume();
		dlog.d("UsbReceiverActivity.onResume();");

		Intent intent = getIntent();
		if (intent != null) {
			dlog.d("UsbReceiverActivity.onResume();Action: " + intent.getAction());
			if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
				Parcelable usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

				// Create a new intent and put the usb device in as an extra
				Intent broadcastIntent = new Intent(ACTION_USB_DEVICE_ATTACHED);
				broadcastIntent.putExtra(UsbManager.EXTRA_DEVICE, usbDevice);

				dlog.d("UsbReceiverActivity.onResume();Extra_device:" + usbDevice.toString());
				// Broadcast this event so we can receive it
				sendBroadcast(broadcastIntent);
				dlog.d("UsbReceiverActivity.onResume();Rebroadcasted intent");
			}
		}

		// Close the activity
		finish();
	}

}
