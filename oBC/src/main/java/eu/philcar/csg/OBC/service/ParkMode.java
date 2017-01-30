package eu.philcar.csg.OBC.service;

public enum ParkMode {

	PARK_OFF, PARK_STARTED, PARK_ENDED;
	
	public boolean isOn() {
		return (this != PARK_OFF);
	}
	
	public int toInt() {
		
		switch (this) {
		case PARK_STARTED: return PARK_STARTED_VALUE;
		case PARK_ENDED: return PARK_ENDED_VALUE;
		default: return PARK_OFF_VALUE;
		}
	}
	
	public static ParkMode fromInt(int parkMode) {
		
		switch (parkMode) {
		case PARK_STARTED_VALUE: return PARK_STARTED;
		case PARK_ENDED_VALUE: return PARK_ENDED;
		default: return PARK_OFF;
		}
	}
	
	public static final int PARK_OFF_VALUE = 0;
	public static final int PARK_STARTED_VALUE = 1;
	public static final int PARK_ENDED_VALUE = 2;
}
