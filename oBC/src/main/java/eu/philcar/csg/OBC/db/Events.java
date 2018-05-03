package eu.philcar.csg.OBC.db;


import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;

import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.support.ConnectionSource;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.data.datasources.repositories.SharengoPhpRepository;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.server.Connectors;
import eu.philcar.csg.OBC.server.HttpConnector;
import eu.philcar.csg.OBC.service.TripInfo;
import eu.philcar.csg.OBC.task.OptimizeDistanceCalc;

public class Events extends DbTable<Event,Integer> {

	public static final int EVT_SWBOOT =1;
	public static final int EVT_RFID   =3;
	public static final int EVT_BATTERY=4;
	public static final int EVT_SPEED  =5;
	public static final int EVT_AREA   =6;
	public static final int EVT_CHARGE   =7;
	public static final int EVT_ENGINE =8;
	public static final int EVT_SOS    =9;
	public static final int EVT_PARK   =10;
	public static final int EVT_CMD    =11;
	public static final int EVT_CLEANLINESS =12;
	public static final int EVT_OBCFAIL = 13;
	public static final int EVT_OBCOK = 14;	
	public static final int EVT_KEY = 15;
	public static final int EVT_READY = 16;
	public static final int EVT_GEAR = 17;
	public static final int EVT_DIAG = 18;
	public static final int EVT_CARPLATE = 19;
	public static final int EVT_3G = 20;
	public static final int EVT_MAINTENANCE = 21;
	public static final int EVT_OUTOFORDER=22;
	public static final int EVT_SELFCLOSE = 23;
	public static final int EVT_DEVICEINFO= 24;
	public static final int EVT_SHUTDOWN= 25;
	public static final int EVT_LEASE= 26;
	public static final int EVT_REBOOT= 27;
	public static final int EVT_SOC   	 =28;
	public static final int EVT_OUTOFAREA	=29;
	public static final int EVT_MENU_CLICK	=30;
	public static final int EVT_CAN_ANOMALIES	=31;
	
	
	
	private Map<Integer,String> labels = new HashMap<Integer,String>();

	public static long lastCanAnomalies=0;
	
	private DLog dlog = new DLog(this.getClass());
	
	 
	public  Events(ConnectionSource connectionSource, Class dataClass)
			throws SQLException {
		super(connectionSource, dataClass);
		
		labels.put(EVT_SWBOOT, "SW_BOOT");
		labels.put(EVT_RFID, "RFID");
		labels.put(EVT_BATTERY, "BATTERY");
		labels.put(EVT_SPEED, "SPEED");
		labels.put(EVT_AREA, "AREA");
		labels.put(EVT_CHARGE, "CHARGE");
		labels.put(EVT_KEY, "KEY");
		labels.put(EVT_READY, "READY");		
		labels.put(EVT_SOS, "SOS");
		labels.put(EVT_PARK, "PARK");
		labels.put(EVT_CMD, "CMD");
		labels.put(EVT_GEAR, "GEAR");
		labels.put(EVT_CLEANLINESS, "CLEANLINESS");
		labels.put(EVT_OBCFAIL, "OBC_FAIL");
		labels.put(EVT_OBCOK, "OBC_OK");		
		labels.put(EVT_DIAG, "DIAG");
		labels.put(EVT_CARPLATE, "CARPLATE");
		labels.put(EVT_3G, "3G");
		labels.put(EVT_MAINTENANCE, "MAINTENANCE");
		labels.put(EVT_OUTOFORDER, "OUT_OF_ORDER");
		labels.put(EVT_SELFCLOSE, "SELFCLOSE");
		labels.put(EVT_DEVICEINFO, "DEVICEINFO");
		labels.put(EVT_SHUTDOWN, "SHUTDOWN");
		labels.put(EVT_LEASE, "LEASE");
		labels.put(EVT_REBOOT, "REBOOT");
		labels.put(EVT_SOC, "SOC");
		labels.put(EVT_OUTOFAREA, "AREA");
		labels.put(EVT_MENU_CLICK, "MENU_CLICK");
		labels.put(EVT_CAN_ANOMALIES, "CAN_ANOMALIES");

		
		
	}
	
	
	public  static Class GetRecordClass() {
		return Event.class;
	}

	/*public static void eventEngine(int state) {
		generateEvent(EVT_ENGINE,state,null);
	}

	public static void eventSwBoot(String version) {
		generateEvent(EVT_SWBOOT,0,version);
	}


	public static void eventRfid(int result, String card) {
		generateEvent(EVT_RFID,result,card);
	}

	public static void eventBattery(int value) {
		generateEvent(EVT_BATTERY,value,null);
	}

	public static void eventParkBegin() {
		generateEvent(EVT_PARK,1,null);
	}

	public static void eventParkEnd() {
		generateEvent(EVT_PARK,2,null);
	}

	public static void eventSos(String number) {
		generateEvent(EVT_SOS,1,number);
	}
	public static void eventDmg(String number) { generateEvent(EVT_SOS,2,number); }

	public static void eventCmd(String cmd) {
		generateEvent(EVT_CMD,0,cmd);
	}

	public static void eventCleanliness(int intVal, int extVal) {
		generateEvent(EVT_CLEANLINESS,0,intVal+";"+extVal);
	}

	public static void eventCharge(int status) {
		generateEvent(EVT_CHARGE,status,null);
	}

	public static void eventKey(int status) {
		generateEvent(EVT_KEY,status,null);
	}

	public static void eventReady(int status) {
		generateEvent(EVT_READY,status,null);
	}

	public static void outOfArea(Boolean status) {
		if(App.lastLocation.getLongitude()!=0.0 && App.lastLocation.getLatitude()!=0.0)
		generateEvent(EVT_OUTOFAREA,status?1:0,status?"Uscita Area Operativa":"Rientro Area Operativa");
	}

	public static void menuclick(String clicked) {
		generateEvent(EVT_MENU_CLICK,0,"Click on "+clicked);
	}

	public static void eventGear(String position) {
		generateEvent(EVT_GEAR,0,position);
	}

	public static void eventObcFail(long delay) {
		int idelay=0;
		if (delay > Integer.MAX_VALUE)
			idelay = Integer.MAX_VALUE;
		else
			idelay = (int) delay;
		generateEvent(EVT_OBCFAIL,idelay,null);
	}

	public static void eventObcOk() {
		generateEvent(EVT_OBCOK,0,null);
	}

	public static void DiagnosticPage(int pwd) {
		generateEvent(EVT_DIAG,pwd,null);
	}

	public static void DiagnosticPageFail(String pwd) {
		generateEvent(EVT_DIAG,-1,pwd);
	}

	public static void CarPlateChange(String oldPlate, String newPlate) {
		generateEvent(EVT_CARPLATE,0,oldPlate+"->"+newPlate);
	}

	public static void Restart3G(int count, String network) {
		generateEvent(EVT_3G,count,network);
	}

	public static void Maintenance(String action) {
		generateEvent(EVT_MAINTENANCE,0,action);
	}


	public static void BeginOutOfOrder(String reason) {
		generateEvent(EVT_OUTOFORDER,1,reason);
	}

	public static void EndOutOfOrder() {
		generateEvent(EVT_OUTOFORDER,2,null);
	}

	public static void TripOutOfOrder(String card) {
		generateEvent(EVT_OUTOFORDER,3,card);
	}

	public static void selfCloseTrip(int idTrip,int beforePin) {
		generateEvent(EVT_SELFCLOSE,idTrip,(beforePin>0?"NOPAY":null));
	}

	public static void DeviceInfo(String versions) {
		generateEvent(EVT_DEVICEINFO,App.id_Version,versions);
	}
	public static void eventSoc(int level, String type) {
		generateEvent(EVT_SOC,level,type);
	}

	public static void Shutdown() {
		generateEvent(EVT_SHUTDOWN,0,"Shutting Down");
	}

	public static void StartShutdown() {
		generateEvent(EVT_SHUTDOWN,0,"Start Shutdown timer");
	}

	public static void StopShutdown() {
		generateEvent(EVT_SHUTDOWN,0,"Stop Shutdown timer");
	}
	public static void Reboot(String text) {
		generateEvent(EVT_REBOOT,0,text+" " +App.sw_Version);
	}


	public static void CanAnomalies(String text) {
		if(SystemClock.elapsedRealtime()-lastCanAnomalies>60*60*1000) {
			lastCanAnomalies=SystemClock.elapsedRealtime();
			generateEvent(EVT_CAN_ANOMALIES, 0, text);
		}
	}

	public static void LeaseInfo(int status,int card) {
		String Hex=Integer.toHexString(card);
		generateEvent(EVT_LEASE,status,Hex);
	}

	public static void generateEvent(int eventId, int intValue, String strValue) {
		Events eventi  = App.Instance.dbManager.getEventiDao();
		eventi.sendEvent(eventId, intValue, strValue);
	}

	public  Event v(int event, int value) {
		return sendEvent(event,value,null);
	}

	public  Event sendEvent(int event, String value) {
		return sendEvent(event,0,value);
	}

	public  Event sendEvent(int eventId, int intValue, String strValue) {
		//

		Event event = new Event();
		event.timestamp = DbManager.getTimestamp();

		event.event = eventId;

		if (labels.containsKey(eventId))
			event.label = labels.get(eventId);

		event.intval = intValue;
		event.txtval = strValue;

		if(OptimizeDistanceCalc.totalDistance != 0)
			event.km = (int) OptimizeDistanceCalc.totalDistance; // momo
		else
			event.km = 0;
		event.battery = App.fuel_level;

		if (App.currentTripInfo!=null  && App.currentTripInfo.trip!=null) {
			event.id_trip = App.currentTripInfo.trip.remote_id;
			event.id_customer = App.currentTripInfo.trip.id_customer;
		}

		if (App.lastLocation!=null) {
			event.lon = App.lastLocation.getLongitude();
			event.lat = App.lastLocation.getLatitude();
		}

		try {
			this.create(event);
		} catch (SQLException e) {
			dlog.e("Error inserting evento:",e);

		}

//		HttpConnector http = new HttpConnector(App.Instance.getApplicationContext());
//		http.SetHandler(null);
//		EventsConnector ec = new EventsConnector();
//		ec.event = event;
//
//
//		http.Execute(ec);

		return event;

	}*/

	@SuppressWarnings("unchecked")
	public List<Event> getEventsToSend() {
		
		try {
			Where<Event,Integer> where  = queryBuilder().orderBy("timestamp", true).where();
			
			where.and(
				where.eq("sent",false),
				//where.eq("sending_error", false)
				where.ge("timestamp",((System.currentTimeMillis()/1000)-60*60*24*7))
			);


			
			
			PreparedQuery<Event> query =  where.prepare();
			
			dlog.d("Query : " + query.toString());
			
			return this.query(query);
										
		} catch (SQLException e) {
			dlog.e("getEventsToSend fail:",e);
			return null;

		}
	}
	
	
	public boolean spedisciOffline(Context context, Handler handler, SharengoPhpRepository phpRepository) {
		//HttpConnector http;
		List<Event> list = getEventsToSend();
		
		if (list==null) 
			return false;		
		
		dlog.d("Eventi to send : " + list.size());
		
		if (!App.hasNetworkConnection()) {
			dlog.w("No connection: aborted");
			return false;
		}


		phpRepository.sendEvents(list);

		//Dato che l'invio � asincrono viene richiesto l'invio solo della prima corsa non spedito, quando arriva il messaggio di risposto di invio eseguito(o fallito) passa alla successiva 
		/*for(Event e : list) {
			
			dlog.d("Selected  evento to send:" + e.toString());
			
			
			http = new HttpConnector(context);
			http.SetHandler(handler);
			
			EventsConnector ec = new EventsConnector();
			ec.event = e;
			/*if(ec.event.id_trip==0 && ec.event.id_trip_local!=0){
				Trips corse = App.Instance.getDbManager().getCorseDao();
				ec.event.id_trip=corse.getRemoteIDfromLocal(ec.event.id_trip_local);
			}
			ec.returnMessageId = Connectors.MSG_EVENTS_SENT_OFFLINE;
			http.Execute(ec);
						
			return true;
		}*/
		
		return false;
	}



	
	
}
