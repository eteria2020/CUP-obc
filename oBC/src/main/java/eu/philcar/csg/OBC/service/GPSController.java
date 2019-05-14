package eu.philcar.csg.OBC.service;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Singleton;

import eu.philcar.csg.OBC.injection.ApplicationContext;

/**
 * This class receiver all the location update and know if i'm moving or not
 * Created by Fulvio on 08/08/2018.
 */
@Singleton
public class GPSController {

	private final static int MSG_HAS_STOPPED = 1;

	private final static long MILLI_STOP_MOVING = 60 * 1000;

	private final Context mContext;

	private Location lastLocation;
	private boolean moving = false;

	private static Handler localHandler;

	@Inject
	public GPSController(@ApplicationContext Context mContext) {
		this.mContext = mContext;
		lastLocation = new Location("default");
		moving = false;
		localHandler = new ResetHandler(this);
	}

	public void onNewLocation(@NonNull Location newLocation) {
		if (lastLocation.distanceTo(newLocation) > 70) {
			updateLocation(newLocation);
		}
	}

	private void updateLocation(Location location) {
		lastLocation.set(location);
		moving = true;
		localHandler.removeMessages(MSG_HAS_STOPPED);
		localHandler.sendEmptyMessageDelayed(MSG_HAS_STOPPED, MILLI_STOP_MOVING);
	}

	public void stopMoving() {
		moving = false;
	}

	public boolean isMoving() {
		return moving;
	}

	static public class ResetHandler extends Handler {

		GPSController controller;

		public ResetHandler(GPSController mContext) {
			this.controller = mContext;
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MSG_HAS_STOPPED:
					resetController();
					break;
			}
		}

		private void resetController() {
			controller.stopMoving();
		}

	}

}