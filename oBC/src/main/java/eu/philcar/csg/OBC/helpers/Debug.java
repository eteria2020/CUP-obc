package eu.philcar.csg.OBC.helpers;

import android.location.Location;

public final class Debug {

	public static final boolean ON = false;

	// Make APP runnable also without specific hardware support
	public static final boolean IGNORE_HARDWARE = false;

	// Generate fake GPS positions
	public static final boolean GENERATE_FAKE_GPS = false;

	// Log positions during trips and if are inside areas
	public static final boolean LOG_TRIP_POSITIONS = false;

	public static final boolean SIMULATE_NAVIGATION = false;

	public static final boolean FORCE_BACKLIGHT_ON = false;

	public static final boolean FORCE_SERVICE_PAGE = false;

	public static Location getFixedLocation() {
		Location l = new Location("debug");
		l.setAltitude(100);
		l.setLatitude(45.9613);
		l.setLongitude(12.9769);
		l.setBearing(90);

		return l;
	}

	private static double _angle = 0;

	public static Location getCircleLocation() {

		Location l = new Location("debug");
		double r = 0.1;
		double x = r * Math.cos(_angle / 360 * Math.PI);
		double y = r * Math.sin(_angle / 360 * Math.PI);

		l.setAltitude(100);
		l.setLatitude(45.9613 + y);
		l.setLongitude(12.9769 + x);
		l.setBearing((float) _angle);

		_angle = (_angle + 6) % 360;
		return l;

	}

}
