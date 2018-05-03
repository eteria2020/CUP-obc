package eu.philcar.csg.OBC.server;

public class Connectors {
	
	public static final int MSG_DN_CLIENTI =3;
	public static final int MSG_DN_ADMINS =4;
	public static final int MSG_DN_AZIENDE =1048;
	public static final int MSG_DN_POIS =1020;
	public static final int MSG_DN_AREAS =1021;
	public static final int MSG_DN_CONFIGS =1022;
	public static final int MSG_DN_ADS = 1111;
	public static final int MSG_SEND_EMAIL = 1112;
	

	public static final int MSG_UP_BEACON =2020;

	public static final int MSG_CALL_CENTER =1;
	
	
	public static final int MSG_TRIPS_SENT_REALTIME = 1;
	public static final int MSG_TRIPS_SENT_OFFLINE = 2;
	
	public static final int MSG_EVENTS_SENT_REALTIME = 11;
	public static final int MSG_EVENTS_SENT_OFFLINE = 12;
	
	public static final int MSG_SENT_REALTIME = 21;
	public static final int MSG_SENT_OFFLINE = 22;
	
	public static RemoteEntityInterface getAdminsConnector() {
		return new AdminsConnector();
	}
	
}
