package eu.philcar.csg.OBC.service;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsManager;

//import com.google.gson.Gson;

import org.acra.ACRA;
import org.joda.time.LocalDateTime;
import org.json.JSONException;
import org.json.JSONObject;

//import java.io.BufferedInputStream;
//import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
//import java.io.InputStream;
//import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

//import ch.qos.logback.classic.spi.STEUtil;
import eu.philcar.csg.OBC.AGoodbye;
import eu.philcar.csg.OBC.AMainOBC;
import eu.philcar.csg.OBC.AWelcome;
import eu.philcar.csg.OBC.App;
//import eu.philcar.csg.OBC.BuildConfig;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.SystemControl;
import eu.philcar.csg.OBC.controller.map.FPdfViewer;
import eu.philcar.csg.OBC.controller.map.FRadio;
import eu.philcar.csg.OBC.controller.welcome.FMaintenance;
import eu.philcar.csg.OBC.controller.welcome.FWelcome;
import eu.philcar.csg.OBC.data.common.ErrorResponse;
import eu.philcar.csg.OBC.data.datasources.repositories.EventRepository;
import eu.philcar.csg.OBC.data.datasources.repositories.SharengoApiRepository;
import eu.philcar.csg.OBC.data.datasources.repositories.SharengoBeaconRepository;
import eu.philcar.csg.OBC.data.datasources.repositories.SharengoPhpRepository;
import eu.philcar.csg.OBC.data.model.BeaconResponse;
import eu.philcar.csg.OBC.db.Customer;
import eu.philcar.csg.OBC.db.Customers;
import eu.philcar.csg.OBC.db.DbManager;
import eu.philcar.csg.OBC.db.Events;
//import eu.philcar.csg.OBC.db.Poi;
//import eu.philcar.csg.OBC.db.Pois;
import eu.philcar.csg.OBC.db.Trips;
import eu.philcar.csg.OBC.devices.Hik_io;
import eu.philcar.csg.OBC.devices.LowLevelInterface;
import eu.philcar.csg.OBC.helpers.AudioPlayer;
import eu.philcar.csg.OBC.helpers.Clients;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.Debug;
import eu.philcar.csg.OBC.helpers.ProTTS;
import eu.philcar.csg.OBC.helpers.RxUtil;
import eu.philcar.csg.OBC.helpers.ServiceTestActivity;
import eu.philcar.csg.OBC.interfaces.OnTripCallback;
import eu.philcar.csg.OBC.scheduler.SuperSocScheduler;
import eu.philcar.csg.OBC.server.AdminsConnector;
import eu.philcar.csg.OBC.server.Connectors;
import eu.philcar.csg.OBC.server.ServerCommand;
import eu.philcar.csg.OBC.server.UdpServer;
import eu.philcar.csg.OBC.server.UploaderLog;
//import eu.philcar.csg.OBC.server.ZmqRequester;
import eu.philcar.csg.OBC.server.ZmqSubscriber;
import eu.philcar.csg.OBC.task.GetTimeFromNetwork;
import eu.philcar.csg.OBC.task.OldLogCleamup;
import eu.philcar.csg.OBC.task.OptimizeDistanceCalc;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

//import eu.philcar.csg.OBC.task.DataLogger;

public class ObcService extends Service implements OnTripCallback {

	private DLog dlog = new DLog(this.getClass());

	//BUILD OPTIONS
	public static final boolean WITH_UDPSERVER = false;
	public static final boolean WITH_UDPQUERY = false;
	public static final boolean WITH_AUTORESET3G = true;
	public static final boolean WITH_OUTOFORDER_WATCHDOG = true;

	public static final boolean WITH_HTTP_NOTIFIES = true;

	public static boolean WITH_ZMQNOTIFY = false;
//	public static boolean WITH_ZMQREQREP = false;

	//PUBLIC CONSTANTS

	public static final int MSG_PING = 1;
	public static final int MSG_CLIENT_REGISTER = 2;
	public static final int MSG_CLIENT_UNREGISTER = 3;
	public static final int MSG_CMD_TIMEOUT = 4;
	public static final int MSG_OBC_REINIT = 5;
	public static final int MSG_UI_CHECK = 6;
	public static final int MSG_SERVICE_STOP = 9;

//	public static final int MSG_IO_GETSTATUS = 10;
	public static final int MSG_IO_RFID = 20;
	public static final int MSG_IO_ENGINE = 21;
	public static final int MSG_IO_LEDS = 22;
	public static final int MSG_IO_DOORS = 23;
//	public static final int MSG_IO_CHARGE = 24;
	public static final int MSG_IO_PLATE = 25;
	public static final int MSG_IO_LCD = 26;
	public static final int MSG_IO_RESETADMINS = 27;

	public static final int MSG_CAR_INFO = 30;
	public static final int MSG_CAR_UPDATE = 31;
	public static final int MSG_CAR_REMOTEUPDATECYCLE = 32;
	public static final int MSG_CAR_END_CHARGING = 33;
	public static final int MSG_CAR_LOCATION = 34;
	public static final int MSG_CAR_CAN_UPDATE = 35;
	public static final int MSG_CAR_START_CHARGING = 36;
	public static final int MSG_CAR_KEY_CHECK = 37;

	public static final int MSG_CUSTOMER_INFO = 40;
	public static final int MSG_CUSTOMER_CHECKPIN = 41;
	public static final int MSG_CAR_CLEANLINESS = 42;
	public static final int MSG_CUSTOMER_SOS = 43;
	public static final int MSG_CUSTOMER_DMG = 44;

	public static final int MSG_TRIP_BEGIN = 50;
	public static final int MSG_TRIP_END = 51;
//	public static final int MSG_TRIP_EVENT = 52;
	public static final int MSG_TRIP_PARK = 53;
	public static final int MSG_TRIP_PARK_CARD_BEGIN = 54;
	public static final int MSG_TRIP_PARK_CARD_END = 55;
	public static final int MSG_TRIP_SELFCLOSE = 56;
	public static final int MSG_TRIP_CLOSE_FORCED = 500;
	public static final int MSG_TRIP_SCHEDULE_SELFCLOSE = 57;
	public static final int MSG_TRIP_SENDBEACON = 58;
	public static final int MSG_TRIP_NEAR_POI = 59;

	public static final int MSG_SERVER_NOTIFY = 60;
	public static final int MSG_SERVER_RESERVATION = 61;
	public static final int MSG_SERVER_COMMAND = 62;
	public static final int MSG_SERVER_HTTPNOTIFY = 63;
	public static final int MSG_SERVER_CHANGE_IP = 64;
	public static final int MSG_SERVER_CHANGE_LOG = 65;
	public static final int MSG_SERVER_COMMAND_NEW = 66;

	public static final int MSG_RADIO_SET_CHANNEL = 70;
	public static final int MSG_RADIO_SET_VOLUME = 71;
	public static final int MSG_RADIO_SET_SEEK = 72;

	public static final int MSG_RADIO_CURPLAY_INFO = 75;
	public static final int MSG_RADIO_SEEK_INFO = 76;
	public static final int MSG_RADIO_SEEK_VALID_INFO = 77;
	public static final int MSG_RADIO_SEEK_STATUS = 78;
	public static final int MSG_RADIO_VOLUME_INFO = 79;

	public static final int MSG_AUDIO_CHANNEL = 80;
	public static final int MSG_NAVIGATE_TO = 81;
	public static final int MSG_CHECK_TIME = 82;
	public static final int MSG_FAILED_SOS = 83;
	public static final int MSG_RESTART_UI = 84;

	public static final int MSG_DEBUG_CARD = 90;
	public static final int MSG_DEBUG_CARD_OPEN = 91;

	public static final int MSG_ZMQ_RESTART = 100;
	public static final int MSG_CHECK_LOG_SIZE = 101;

	public static final int MSG_API_TRIP_CALLBACK = 110;
	public static final int MSG_CHECK_SPEGNIMENTO = 111;

	public static final int SERVER_NOTIFY_RAW = 0;
	public static final int SERVER_NOTIFY_RESERVATION = 1;
	public static final int SERVER_NOTIFY_COMMAND = 2;

//	public static final String EVENT_CARD_OPEN = "OPEN";
//	public static final String EVENT_CARD_CLOSE = "CLOSE";

	//private constants

	final String SYSTEM_ALARM_NAME = "eu.philcar.csg.OBC.syncalarm";

	Messenger messenger = null;

	private ArrayList<Clients> clients = new ArrayList<>();

	private boolean ampError = false;

//	private boolean isStarted;
//	private boolean isStopRequested = false;

	@Inject
	SharengoApiRepository apiRepository;
	@Inject
	SharengoPhpRepository phpRepository;
	@Inject
	EventRepository eventRepository;
	@Inject
	SharengoBeaconRepository beaconRepository;
	@Inject
	DataManager dataManager;
	@Inject
	GPSController gpsController;

	//ENCAPSULATED OBJECTS

	private UdpServer udpServer;
	private LowLevelInterface obc_io;
	private CarInfo carInfo;
	private TripInfo tripInfo;

//	private Thread udpBroadcastReceiver;

	private ZmqSubscriber zmqSubscriber;
//	ZmqRequester zmqRequester;

	private AlarmManager alarmManager;
	private PendingIntent alarmPendingIntent;

	private GpsStatus gpsStatus;

	ScheduledExecutorService serverUpdateScheduler;
	ScheduledExecutorService tripUpdateScheduler;
	ScheduledExecutorService dataLoggerScheduler;
//	ScheduledExecutorService tripPoiUpdateScheduler;
	ScheduledExecutorService closeTripScheduler;
	ScheduledExecutorService virtualBMSUpdateScheduler;
	ScheduledExecutorService gpsCheckScheduler;
	ScheduledExecutorService timeCheckScheduler;
	ScheduledExecutorService superSOCScheduler;
	ScheduledFuture timeCheckFuture;

	Runnable timeCheckRunnable = new Runnable() {

//		private int lastResponseCode;

//		private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd.HHmmss", Locale.getDefault());

		@Override
		public void run() {

//            long sharengoTime=0;
//            long fix =SystemClock.elapsedRealtime();
//            try {
//                sharengoTime = Long.parseLong(doGet(App.URL_Time));
//            }catch (Exception e){
//                dlog.e("Exception while retreiving sharengoTime",e);
//                lastResponseCode=0;
//            }

			new GetTimeFromNetwork(ObcService.this).execute(carInfo);

//            long sharengoTime=0;
//            long fix =SystemClock.elapsedRealtime();
//            try {
//                sharengoTime = Long.parseLong(doGet(App.URL_Time));
//            }catch (Exception e){
//                dlog.e("Exception while retreiving sharengoTime",e);
//                lastResponseCode=0;
//            }
//
//
//            Runtime rt = Runtime.getRuntime();
//            try {
//                Date sharengo_time=null;
//                if(lastResponseCode==200) {
//                    sharengo_time = new Date(sharengoTime*1000 + SystemClock.elapsedRealtime() - fix);
//                }
//                Date gps_time = new Date(carInfo.intGpsLocation.getTime() + SystemClock.elapsedRealtime() - carInfo.intGpsLocation.getElapsedRealtimeNanos() / 1000000);
//                Date android_time = new Date(System.currentTimeMillis());
//
//
//
//                if(sharengo_time!=null && sharengo_time.getTime() > 1234567890000L){
//
//                    dlog.d("timeCheckScheduler: imposto ora Sharengo "+sharengo_time.toString()+ "ora android: "+android_time.toString());
//                    rt.exec(new String[]{"/system/xbin/su", "-c", "date -s " + sdf.format(sharengo_time) + ";\n"}); //
//                    rt.exec(new String[]{"/system/xbin/su","-c", "settings put global auto_time 0"}); //date -s 20120423.130000
//
//                }else if(carInfo.intGpsLocation.getTime()>1234567890000L) {
//                    //if(android_time.getTime()<1234567890000L) {
//                    dlog.d("timeCheckScheduler: imposto ora gps "+gps_time.toString()+ "ora android: "+android_time.toString());
//                    rt.exec(new String[]{"/system/xbin/su", "-c", "date -s " + sdf.format(gps_time) + ";\n"}); //
//                    rt.exec(new String[]{"/system/xbin/su","-c", "settings put global auto_time 0"}); //date -s 20120423.130000
//                    //}
//
//                }
//                else
//                    rt.exec(new String[]{"/system/xbin/su","-c", "settings put global auto_time 1"}); //date -s 20120423.130000*/
//                dlog.d("timeCheckScheduler: rawGpsTime: " + new Date(carInfo.intGpsLocation.getTime()).toString() + " elapsed: " + (System.currentTimeMillis() - (carInfo.intGpsLocation.getElapsedRealtimeNanos() / 1000000)) + " android time: " + android_time.toString() + " fixed gps time: " + gps_time.toString() +" Sharengo time: "+(sharengo_time!=null?sharengo_time.toString():" response code "+lastResponseCode));
//
//                rt.exec(new String[]{"/system/xbin/su", "-c", "settings put global auto_time_zone 0"}); //date -s 20120423.130000
//                AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
//                am.setTimeZone(App.timeZone);
//                am = null;
//
//
//
//
//            } catch (Exception e) {
//                dlog.e("timeCheckScheduler error", e);
//            }
		}

/*		public String doGet(String url) throws Exception {
			String result = "";
			HttpURLConnection urlConnection = null;
			try {
				URL requestedUrl = new URL(url);
				urlConnection = (HttpURLConnection) requestedUrl.openConnection();

				urlConnection.setRequestMethod("GET");
				urlConnection.setRequestProperty("Accept", "application/json");
				urlConnection.setRequestProperty("Content-Type", "application/json");
				urlConnection.setConnectTimeout(60000);
				urlConnection.setDefaultUseCaches(false);
				urlConnection.setUseCaches(false);
				urlConnection.setAllowUserInteraction(false);

				urlConnection.setReadTimeout(60000);
				urlConnection.setDoInput(true);
				lastResponseCode = urlConnection.getResponseCode();
				if (lastResponseCode > 300) {
					result = urlConnection.getResponseCode() + " -> " + readFully(urlConnection.getErrorStream());
				} else {
					result = readFully(urlConnection.getInputStream());
				}
				//result = urlConnection.getResponseCode() + " -> " + IOUtil.readFully(urlConnection.getErrorStream());
			} catch (Exception ex) {
				dlog.e("Exception inside APIdoGet", ex);
				lastResponseCode = 0;
				result = "0";
			} finally {
				if (urlConnection != null) {
					urlConnection.disconnect();
				}
			}
			return result;
		}*/

/*		private String readFully(InputStream inputStream) throws IOException {

			if (inputStream == null) {
				return "";
			}

			BufferedInputStream bufferedInputStream = null;
			ByteArrayOutputStream byteArrayOutputStream = null;

			try {
				bufferedInputStream = new BufferedInputStream(inputStream);
				byteArrayOutputStream = new ByteArrayOutputStream();

				final byte[] buffer = new byte[1024];
				int available = 0;

				while ((available = bufferedInputStream.read(buffer)) >= 0) {
					byteArrayOutputStream.write(buffer, 0, available);
				}

				return byteArrayOutputStream.toString();

			} finally {
				if (bufferedInputStream != null) {
					bufferedInputStream.close();
				}
			}
		}*/

	};

	private LocationManager locationManager;

	private WakeLock screenLockTrip;
//	private boolean RequireDisplayOn = false;

	public static Message obtainMessage(int what) {
		return Message.obtain(null, what);
	}

	public static Message obtainRegistrationMessage(String clientName) {
		Message msg = Message.obtain(null, MSG_CLIENT_REGISTER);
		msg.obj = clientName;
		return msg;
	}

	GpsStatus.Listener gpsStatusListener = new GpsStatus.Listener() {

		@Override
		public void onGpsStatusChanged(int event) {
			switch (event) {
				case GpsStatus.GPS_EVENT_STARTED:
//                    dlog.d("GPS STARTED");
					break;
				case GpsStatus.GPS_EVENT_STOPPED:
//                    dlog.d("GPS STOPPED");
					break;
				case GpsStatus.GPS_EVENT_FIRST_FIX:
//                    dlog.d("GPS FIRST FIX");
					break;
				case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
					break;
			}

//			gpsStatus = locationManager.getGpsStatus(gpsStatus);
			if ( ContextCompat.checkSelfPermission( getBaseContext(), android.Manifest.permission.ACCESS_FINE_LOCATION  ) == getPackageManager().PERMISSION_GRANTED ) {
				gpsStatus = locationManager.getGpsStatus(gpsStatus);
			} else {
				dlog.e("ObcService.gpsStatusListener();GPS permission fail");
			}

		}
	};

	@Override
	public void onCreate() {
		super.onCreate();

		App.get(this).getComponent().inject(this);

		dlog.d("ObcService.onCreate();");

		//UDP SERVER to receive broadcast from mobile app
		//udpBroadcastReceiver = new Thread(new UDPServer());
		//udpBroadcastReceiver.start();

		if (App.Instance.loadZmqDisabledConfig()) {
			WITH_ZMQNOTIFY = false;
			//this.WITH_HTTP_NOTIFIES = true;
			dlog.d("ObcService.onCreate();Notify protocol: HTTP");
		} else {
			WITH_ZMQNOTIFY = true;
			// this.WITH_HTTP_NOTIFIES = true;
			dlog.d("ObcService.onCreate(),Notify protocol: ZMQ");
		}

		if (Debug.IGNORE_HARDWARE) {
			return;
		}

		PowerManager powerManager = ((PowerManager) getSystemService(POWER_SERVICE));
		if(powerManager !=null) {
			screenLockTrip =powerManager.newWakeLock(
					PowerManager.ON_AFTER_RELEASE |
							PowerManager.ACQUIRE_CAUSES_WAKEUP |
							PowerManager.FULL_WAKE_LOCK, "ObcService:tag");
		} else {
			dlog.e("ObcService.onCreate();getSystemService is null");
		}

/*		screenLockTrip = ((PowerManager) getSystemService(POWER_SERVICE)).newWakeLock(
				PowerManager.ON_AFTER_RELEASE |
						PowerManager.ACQUIRE_CAUSES_WAKEUP |
						PowerManager.FULL_WAKE_LOCK, "ObcService:tag");*/

		//Start messenger

		messenger = new Messenger(localHandler);

		if (WITH_UDPSERVER) {
			// Start Udp Server
			udpServer = new UdpServer();
			udpServer.init(localHandler);
		}

		if (WITH_ZMQNOTIFY) {
			zmqSubscriber = new ZmqSubscriber(localHandler);
			localHandler.sendMessageDelayed(MessageFactory.zmqRestart(), 15000);
			//zmqSubscriber.Start(localHandler);
		}

		// Start low level IO module
		//obc_io = new Obc_io(this);
		obc_io = new Hik_io(this);
		obc_io.init();

		//Create and init data structures

		carInfo = new CarInfo(localHandler);
		carInfo.setId(App.CarPlate);

		tripInfo = new TripInfo(this);
		tripInfo.init();

		//Init GPS

		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

		if(locationManager!=null){
			dlog.d("ObcService.onCreate();GPS provider is : " + (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ? "ENABLED" : "DISABLED"));
		} else {
			dlog.e("ObcService.onCreate();locationManager null ");
		}

		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setAltitudeRequired(false);
		criteria.setBearingRequired(true);
		criteria.setPowerRequirement(Criteria.POWER_HIGH);

		setLocationMode(30000);  // If car is idle  30sec is min time between locations updates
//		locationManager.addGpsStatusListener(gpsStatusListener);

		if ( ContextCompat.checkSelfPermission( getBaseContext(), android.Manifest.permission.ACCESS_FINE_LOCATION  ) == getPackageManager().PERMISSION_GRANTED ) {
			locationManager.addGpsStatusListener(gpsStatusListener);
		} else {
			dlog.e("ObcService.gpsStatusListener.onCreate();GPS permission fail");
		}

		//Check if UI is active and if not restart the proper page
		checkAndRestartUI();

		// Reacquire lock if trip open
		if (tripInfo.isOpen) {
			App.currentTripInfo = tripInfo;
			setDisplayStatus(true, 0);
			dlog.d("ObcService.onCreate();Restarting from open trip. Acquiring lock");
			obc_io.setLed(null, LowLevelInterface.ID_LED_BLUE, LowLevelInterface.ID_LED_ON);
			startRemoteUpdateCycle();
//			startRemotePoiCheckCycle();
		} else {
			setDisplayStatus(false, 15);

			if (App.reservation != null)
				setReservation(App.reservation);
			else
				obc_io.setLed(null, LowLevelInterface.ID_LED_GREEN, LowLevelInterface.ID_LED_ON);
		}

		// Send a first startup beacon (NOTE: some data may not be yet initialized)
		sendBeacon();

		//Init alarms

		alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
		alarmPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(SYSTEM_ALARM_NAME), 0);
		registerReceiver(AlarmReceiver, new IntentFilter(SYSTEM_ALARM_NAME));

		alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 1000, AlarmManager.INTERVAL_FIFTEEN_MINUTES, alarmPendingIntent);

		//Send boot event
		/*final Events eventi = App.Instance.dbManager.getEventiDao();
		eventi.sendEvent(Events.EVT_SWBOOT, App.sw_Version);*/
		eventRepository.eventSwBoot(App.sw_Version);

		//Start whitelist update
		startDownloadCustomers();
//        Customers customers = App.Instance.dbManager.getClientiDao();
//        customers.startWhitelistDownload(this, privateHandler);

		//Start employee update
		startDownloadEmployees();

		startPoiDownload();

		startAreaPolygonDownload();

		//Start reservation update
		startDownloadReservations();

		//Start download admin cards :  DISABLED FOR BUGS ON HARDWARE
		//startDownloadAdmins();

		//Start download configs
		startDownloadConfigs();

		//Start download model
		startDownloadModel();

		//Start dequeueing  trips  and events
		privateHandler.sendEmptyMessage(Connectors.MSG_TRIPS_SENT_OFFLINE);
		privateHandler.removeMessages(Connectors.MSG_EVENTS_SENT_OFFLINE);
		privateHandler.sendEmptyMessageDelayed(Connectors.MSG_EVENTS_SENT_OFFLINE, 10000);

		dataLoggerScheduler = Executors.newSingleThreadScheduledExecutor();
		// Start scheduler for server query
		serverUpdateScheduler = Executors.newSingleThreadScheduledExecutor();
		// Start scheduler for SOC
		superSOCScheduler = Executors.newSingleThreadScheduledExecutor();
		//Start scheduler for CAN query
		virtualBMSUpdateScheduler = Executors.newSingleThreadScheduledExecutor();
		//Start scheduler for GPS query
		gpsCheckScheduler = Executors.newSingleThreadScheduledExecutor();
		//Start schedule for time sync
		timeCheckScheduler = Executors.newSingleThreadScheduledExecutor();

		// Start internal loop with period of  10 sec

		serverUpdateScheduler.scheduleAtFixedRate(new Runnable() {

//			private boolean _secondRun = false;
			private int minutePrescaler = 0;
			private int fiveminutePrescaler = 0;
			private int threeminutePrescaler = 0;
			private int notifiesPrescaler = 0;

			@Override
			public void run() {
				try {

					//If UDP query is enabled send query packet
					if (WITH_UDPSERVER && WITH_UDPQUERY)
						udpServer.sendQuery();
					//If HTTP query is enabled schedule an HTTP notifies download
					if (WITH_HTTP_NOTIFIES && notifiesPrescaler++ > 4 && tripInfo != null && tripInfo.isOpen /*&& (App.parkMode == null || !App.parkMode.isOn())*/) {
						notifiesPrescaler = 0;
						sendBeacon(carInfo);
//                        HttpConnector http = new HttpConnector(ObcService.this);
//                        http.SetHandler(localHandler);
//                        NotifiesConnector nc = new NotifiesConnector();
//                        nc.setTarga(App.CarPlate);
//                        nc.setBeacon(carInfo.getJson(false));
//                        http.Execute(nc);
					}

					//If there is no open trip or the car is in park mode  ensure display backlight is off
					if (tripInfo != null && tripInfo.isOpen && (App.parkMode == null || !App.parkMode.isOn())) {
						setDisplayStatus(true, 0);
					}

					//If there is a reservation and it is just expired remote it
					if (App.reservation != null && App.reservation.isTimedout()) {
						setReservation(null);
					}
					//If we lost or recovered connection with OBC-IO send event and change status
					if (obc_io.lastContactDelay() > 30 && !App.ObcIoError) {
						App.ObcIoError = true;
						eventRepository.eventObcFail(obc_io.lastContactDelay());
					} else if (obc_io.lastContactDelay() <= 30 && App.ObcIoError) {
						App.ObcIoError = false;
						eventRepository.eventObcOk();
					}
					if (threeminutePrescaler++ >= 18) {
						threeminutePrescaler = 0;

						//Check network link
						CheckNetwork();
					}
					if (fiveminutePrescaler++ >= 6*5) {
						fiveminutePrescaler = 0;

						privateHandler.sendEmptyMessage(Connectors.MSG_TRIPS_SENT_OFFLINE);
					}
					if (minutePrescaler++ >= 6) {
						minutePrescaler = 0;

						//If there is  an open trip check if it exceeded a time cap
						if (tripInfo != null && tripInfo.isOpen) {
							int timecap = App.Instance.loadSplitTripConfig();
							tripInfo.HandleMaxDurata(carInfo, timecap, ObcService.this);
						}
					}

				} catch (Exception e) {
					dlog.e("ObcService.serverUpdateScheduler.scheduleAtFixedRate();error", e);
				}
			}

		}, 10, 10, TimeUnit.SECONDS);

		superSOCScheduler.scheduleAtFixedRate(new SuperSocScheduler(obc_io, dataManager), 10, 10, TimeUnit.SECONDS);

		virtualBMSUpdateScheduler.scheduleAtFixedRate(new Runnable() {

//			int canAmpAnomalies = 0, canCellAnomalies = 0, canBmsAnomalies = 0, canAllAnomalies = 0;
			int canAmpAnomalies = 0, canCellAnomalies = 0, canBmsAnomalies = 0;
			boolean bmsSocError = false, bmsCellError = false;

			@Override
			public void run() {
				try {

					float currVolt = 0;
					boolean cellLow = false;
					String lowCellNumber = " ";
					String cellsVoltage = "";

					//retrieve all can data
					carInfo.cellVoltageValue = getCellVoltages();
					carInfo.bmsSOC = getSOCValue();
					carInfo.setOutAmp(getCurrentValue());

					if(!App.virtualSocEnabled){
						dlog.d("ObcService.virtualBMSUpdateScheduler();disabled virtualSOC set " + carInfo.bmsSOC);
						carInfo.setBatteryLevel(carInfo.bmsSOC);
						return;
					}

					//check amp error
					if (carInfo.getOutAmp() == 350 || carInfo.getOutAmp() == -1037) {
						setAmpError(true);
						if (canAmpAnomalies++ == 3) {
							eventRepository.CanAnomalies("350 Amp");
						}
					} else {
						setAmpError(false);

						if (carInfo.bmsSOC == 0) {
							bmsSocError = true;
							if (canAmpAnomalies++ == 3) {
								eventRepository.CanAnomalies("SOC 0");
							}
						} else {
							bmsSocError = false;
							canAmpAnomalies = 0;
						}
					}

					//type of battery

					carInfo.minVoltage = carInfo.cellVoltageValue.length>20 ? 2.8f : 2.5f;
					carInfo.batteryType = carInfo.cellVoltageValue.length>20 ? "HNLD" : "DFD" ;
					//}
					App.Instance.setMaxVoltage(carInfo.batteryType.equalsIgnoreCase("HNLD") ? 82 : 83);

					//voltage sum
					bmsCellError = false;
					for (int i = 0; i < carInfo.cellVoltageValue.length; i++) {
						if (i < (carInfo.batteryType.equalsIgnoreCase("HNLD") ? 24 : 20) && carInfo.cellVoltageValue[i] < 1) {
							currVolt = 0;
							bmsCellError = true;
						}

						if (!bmsCellError)
							currVolt += carInfo.cellVoltageValue[i];    //		battery cell voltages

						cellsVoltage = cellsVoltage.concat(" " + carInfo.cellVoltageValue[i]);
						if (carInfo.cellVoltageValue[i] < carInfo.minVoltage && carInfo.cellVoltageValue[i] != 0) {
							cellLow = true;
							lowCellNumber = lowCellNumber.concat((i + 1) + " ");
						}
					}
					if (!bmsCellError && !bmsSocError && !isAmpError()) {
						dlog.i("ObcService.virtualBMSUpdateScheduler();reset count to 0 " + bmsCellError + " " + bmsSocError + " " + isAmpError());
						App.Instance.setBmsCountTo90(0);
					}

					//fill carinfo attribute
					carInfo.isCellLowVoltage = cellLow;
					carInfo.lowCells = lowCellNumber;
					carInfo.currVoltage = (float) Math.round(currVolt * 100) / 100f;

					if (bmsCellError) {
						if (canCellAnomalies++ == 3) {
							eventRepository.CanAnomalies("0 CellsVolt");
						}
					} else {
						canCellAnomalies = 0;
					}

					//SOC2 calculation
					if (carInfo.batteryType.equalsIgnoreCase("DFD"))
						carInfo.virtualSOC = ((float) Math.round((100 - 90 * (App.getMax_voltage() - currVolt) / 12) * 10) / 10f);//DFD:HNLD
					else
						carInfo.virtualSOC = ((float) Math.round((100 - 90 * (App.getMax_voltage() - currVolt) / 9.5) * 10) / 10f);//DFD:HNLD

					//SOCR calculation
					if (isAmpError() || bmsCellError || bmsSocError) {

						if (bmsCellError) {

							if (bmsSocError) {
								if (App.Instance.incrementBmsCountTo90() < 90) {
									dlog.i("ObcService.virtualBMSUpdateScheduler();all fault keep last valid value to 0 is " + App.getBmsCountTo90() + " times | error: " + bmsCellError + bmsSocError + isAmpError());
									carInfo.SOCR = carInfo.batteryLevel;
								} else {
									obc_io.close();
									obc_io.init();
									carInfo.SOCR = 0;
								}

							} else {

								if (App.Instance.incrementBmsCountTo90() < 90) {
									dlog.i("ObcService.virtualBMSUpdateScheduler();bms cells fault using bms SOC to 0 is " + App.getBmsCountTo90() + " times | error: " + bmsCellError + bmsSocError + isAmpError());
									carInfo.SOCR = Math.min(carInfo.bmsSOC, carInfo.bmsSOC_GPRS);
								} else {
									obc_io.close();
									obc_io.init();
									carInfo.SOCR = 0;
								}

							}

						} else if (bmsSocError) {
							if (App.Instance.incrementBmsCountTo90() < 90) {
								dlog.i("ObcService.virtualBMSUpdateScheduler();bms SOC fault using SOC2 to 0 is " + App.getBmsCountTo90() + " times | error: " + bmsCellError + bmsSocError + isAmpError());
								carInfo.SOCR = carInfo.virtualSOC;
							} else {
								obc_io.close();
								obc_io.init();
								carInfo.SOCR = 0;
							}
						} else {
							carInfo.SOCR = Math.min(carInfo.bmsSOC, carInfo.virtualSOC);
						}
						dlog.i("ObcService.virtualBMSUpdateScheduler();error calculation VBATT: " + carInfo.currVoltage + "V V100%: " + App.max_voltage + "V cell voltage: " + cellsVoltage + " soc: " + carInfo.bmsSOC + "% SOCR: " + carInfo.SOCR + "% SOC2:" + carInfo.virtualSOC + "% SOC.ADMIN:" + carInfo.batteryLevel + "% bmsSOC_GPRS:" + carInfo.bmsSOC_GPRS + "%");

					} else {
						if (Math.abs(carInfo.bmsSOC - carInfo.bmsSOC_GPRS) <= 2) {
							if ((carInfo.bmsSOC == 0 || carInfo.bmsSOC_GPRS == 0) && carInfo.currVoltage != 0) { //BMS SOC 0 Voltage OK
								if (carInfo.bmsSOC == 0) {
									if (canBmsAnomalies++ == 3) {
										eventRepository.CanAnomalies("0 BMS");
									} else
										canBmsAnomalies = 0;
								}
								carInfo.SOCR = carInfo.virtualSOC;
							} else {
								if ((carInfo.bmsSOC == 0 || carInfo.bmsSOC_GPRS == 0) && carInfo.currVoltage == 0) {
									carInfo.SOCR = carInfo.batteryLevel;
								}

								carInfo.SOCR = Math.min(Math.min(carInfo.bmsSOC, carInfo.bmsSOC_GPRS), carInfo.virtualSOC);
							}
						} else {
							carInfo.SOCR = (carInfo.bmsSOC == 0 || carInfo.bmsSOC_GPRS == 0) ? Math.min(Math.max(carInfo.bmsSOC_GPRS, carInfo.bmsSOC), carInfo.virtualSOC) : Math.min(Math.max(carInfo.bmsSOC_GPRS, carInfo.bmsSOC), carInfo.virtualSOC);
						}

						dlog.cr("ObcService.virtualBMSUpdateScheduler();VBATT: " + carInfo.currVoltage +
								"V V100%: " + App.max_voltage +
								"V cell voltage: " + cellsVoltage +
								" soc: " + carInfo.bmsSOC +
								"% SOCR: " + carInfo.SOCR +
								"% SOC2:" + carInfo.virtualSOC +
								"% SOC.ADMIN:" + carInfo.batteryLevel +
								"% bmsSOC_GPRS:" + carInfo.bmsSOC_GPRS +
								"% outAmp: " + carInfo.getOutAmp());

						//check car bms usage
						if (carInfo.currVoltage <= 0 || (carInfo.getOutAmp() >= 25 && carInfo.getOutAmp() != 350)) {
							carInfo.setBatteryLevel((Math.min(carInfo.batteryLevel, Math.min(carInfo.bmsSOC, carInfo.bmsSOC_GPRS))));
							dlog.i("ObcService.virtualBMSUpdateScheduler();value " + (carInfo.currVoltage <= 0 ? "packVoltage null" : "outAmp greater than 25") + " ignoring virtual data.");
							dlog.i("ObcService.virtualBMSUpdateScheduler();VBATT: " + carInfo.currVoltage + "V V100%: " + App.max_voltage + "V cell voltage: " + cellsVoltage + " soc: " + carInfo.bmsSOC + "% SOCR: " + carInfo.SOCR + "% SOC2:" + carInfo.virtualSOC + "% SOC.ADMIN:" + carInfo.batteryLevel + "% bmsSOC_GPRS:" + carInfo.bmsSOC_GPRS + "%");
							return;
						}
						if ((carInfo.isCellLowVoltage) || carInfo.currVoltage <= 67f) {

							carInfo.SOCR = Math.min(carInfo.virtualSOC, 0f);
						}
					}

					//set SOCR valued
					dlog.i("ObcService.virtualBMSUpdateScheduler();alarm state: amp: " + isAmpError() + " cell: " + bmsCellError + " soc: " + bmsSocError);

					carInfo.setBatteryLevel(Math.round(carInfo.SOCR));

					//CHECK FOR REBOOT BMS ERROR


				} catch (Exception e) {
					dlog.e("ObcService.virtualBMSUpdateScheduler();", e);
				}
			}

		}, 15, 120, TimeUnit.SECONDS);

		dataLoggerScheduler.scheduleAtFixedRate(new Runnable() {

			//  private DataLogger dataLogger = new DataLogger();

			@Override
			public void run() {
				try {

					Run();

				} catch (Exception e) {
					dlog.e("ObcService.dataLoggerScheduler.run();", e);
				}
			}

		}, 10, 10, TimeUnit.SECONDS);

		gpsCheckScheduler.scheduleAtFixedRate(new Runnable() {

			final Context context = ObcService.this;
			int intCount = 0, extCount = 0;
			Location lastIntGpsLocation = new Location(LocationManager.GPS_PROVIDER);
			Location lastExtGpsLocation = new Location(LocationManager.GPS_PROVIDER);

			@Override
			public void run() {
				try {
					dlog.d(
							String.format("ObcService.gpsCheckScheduler();lastIntGpsLocation:%s;lastExtGpsLocation:%s; newIntGpslocation:%s; newExtGpslocation:%s;UseExternalGPS:%s",
							lastIntGpsLocation,
							lastExtGpsLocation,
							carInfo.intGpsLocation,
							carInfo.extGpsLocation,
							App.UseExternalGPS
							)
					);

//					dlog.d("ObcService.gpsCheckScheduler();lastIntGpsLocation: " + lastIntGpsLocation + " lastExtGpsLocation: " + lastExtGpsLocation + " newIntGpslocation: " + carInfo.intGpsLocation + " newExtGpslocation " + carInfo.extGpsLocation + " UseExternalGPS: " + App.UseExternalGPS);

					if (!App.UseExternalGPS) {

						if (checkIsOutOfBorder()) {
							App.Instance.setUseExternalGps(true);
							dlog.d("ObcService.gpsCheckScheduler();setUseExternalGps(true) IntGpsLocation out from Italy or location 0.0");
							sendBeacon();//update remoto per aggiornare int/ext
							intCount = 0;
							return;
						}

						if (carInfo.intGpsLocation.getLatitude() == lastIntGpsLocation.getLatitude() &&
								carInfo.intGpsLocation.getLongitude() == lastIntGpsLocation.getLongitude()) {
							intCount++;
							if (intCount >= 3) {
								App.Instance.setUseExternalGps(true);
								dlog.d("ObcService.gpsCheckScheduler();setUseExternalGps(true) same coordinate for " + intCount + " times");
								intCount = 0;
								sendBeacon();
							}
//							return;
						} else {
							intCount = 0;
							lastIntGpsLocation.set(carInfo.intGpsLocation);
							App.resetGPSSwitchCount();
						}
					} else {

						if (checkIsOutOfBorder()) {
							App.Instance.setUseExternalGps(false);
							dlog.d("ObcService.gpsCheckScheduler();setUseExternalGps(false) ExtGpsLocation out from Italy or location 0.0");
							sendBeacon();
							extCount = 0;
							return;
						}
						if (carInfo.extGpsLocation.getLatitude() == lastExtGpsLocation.getLatitude() && carInfo.extGpsLocation.getLongitude() == lastExtGpsLocation.getLongitude()) {
							extCount++;
							if (extCount >= 3) {
								App.Instance.setUseExternalGps(false);
								dlog.d("ObcService.gpsCheckScheduler();setUseExternalGps(false) same coordinate for " + extCount + " times");
								extCount = 0;
								sendBeacon();
							}
//							return;
						} else {
							extCount = 0;
							lastExtGpsLocation.set(carInfo.extGpsLocation);
							App.resetGPSSwitchCount();
						}

					}

				} catch (Exception e) {
					dlog.e("ObcService.gpsCheckScheduler();", e);
				}
			}

		}, 40, 300, TimeUnit.SECONDS);

		timeCheckFuture = timeCheckScheduler.scheduleAtFixedRate(timeCheckRunnable, 0, 10, TimeUnit.MINUTES);//360

		//Register receiver for battery data

		registerReceiver(this.BatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		//Register receiver for connectivity status change
		registerReceiver(this.ConnectivityChangeReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));

		//If configured in debug heler force service page opening
		if (Debug.FORCE_SERVICE_PAGE) {

			Intent i = new Intent(ObcService.this, ServiceTestActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(i);
		}

/*		if (WITH_ZMQREQREP) {
			zmqRequester = new ZmqRequester();
			zmqRequester.Send("Hello", null, null);
		}
		*/

		//Request an NTP resync
		//SystemControl.ResycNTP();

		localHandler.sendMessageDelayed(MessageFactory.checkLogSize(), 5 * 60 * 1000);
		dlog.d("ObcService.onCreate();Service created");
//		isStarted = true;
        /*
        control if documenti of the veichele is present in the device before starting
         */
		if (App.hasNetworkConnection()) {
			new DocumentControl().execute();
		}

		localHandler.sendEmptyMessageDelayed(MSG_CHECK_SPEGNIMENTO,30000);
	}

	public void sendMessage(Message msg) {
		localHandler.sendMessage(msg);
	}

	/**
	 * Check if the actual position is inside of operative are defined in the build.grandle.
	 *
	 * @return
	 */
	private boolean checkIsOutOfBorder() {
		boolean result = false;

		Context context = getApplicationContext();

		if (
				(carInfo.extGpsLocation.getLongitude() > Float.parseFloat(context.getResources().getString(R.string.maxLongitudeMargin)) ||
				 carInfo.extGpsLocation.getLongitude() < Float.parseFloat(context.getResources().getString(R.string.minLongitudeMargin)) ||
				 carInfo.extGpsLocation.getLatitude()  > Float.parseFloat(context.getResources().getString(R.string.maxLatitudeMargin))  ||
				 carInfo.extGpsLocation.getLatitude()  < Float.parseFloat(context.getResources().getString(R.string.minLatitudeMargin)))
			||
				(carInfo.extGpsLocation.getLatitude()  == 0 ||
				 carInfo.extGpsLocation.getLatitude()  == 0 ||
				 carInfo.extGpsLocation.getLongitude() == 0 ||
				 carInfo.extGpsLocation.getLongitude() == 0)) {
			result = true;
		}

		return result;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		dlog.d("Service started");
		return Debug.IGNORE_HARDWARE ? Service.START_NOT_STICKY : Service.START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return Debug.IGNORE_HARDWARE ? null : messenger.getBinder();
	}

	@Override
	public void onDestroy() {
		dlog.d("On destroy");

		if (Debug.IGNORE_HARDWARE) {
			super.onDestroy();
			return;
		}

//		isStopRequested = true;

		if (serverUpdateScheduler != null)
			serverUpdateScheduler.shutdown();
		if (superSOCScheduler != null)
			superSOCScheduler.shutdown();
		if (virtualBMSUpdateScheduler != null)
			virtualBMSUpdateScheduler.shutdown();
		if (gpsCheckScheduler != null)
			gpsCheckScheduler.shutdown();
		if (timeCheckScheduler != null)
			timeCheckScheduler.shutdown();

		stopRemoteUpdateCycle();
//		stopRemotePoiCheckCycle();

		locationManager.removeUpdates(carInfo.serviceLocationListener);
		locationManager.removeGpsStatusListener(gpsStatusListener);

		alarmManager.cancel(alarmPendingIntent);

		unregisterReceiver(BatteryInfoReceiver);
		unregisterReceiver(AlarmReceiver);
		unregisterReceiver(ConnectivityChangeReceiver);

		if (WITH_UDPSERVER)
			udpServer.close();

		obc_io.close();
		dlog.d("Stopping");
		stopSelf();
	}

	// Set the speed of locations updates
	@SuppressLint("MissingPermission")
	private void setLocationMode(long minTime) {
		if (locationManager != null) {
			dlog.i("ObcService.setLocationMode " + minTime);
			locationManager.removeUpdates(carInfo.serviceLocationListener);
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, 0, carInfo.serviceLocationListener);
		}
	}

	private void setNetworkConnectionStatus(boolean status) {

		//Force led blinking if connection is lost
		obc_io.setForcedLedBlink(!status);

		//Detect loss or recover connection
		if (App.hasNetworkConnection() && !status) {
			App.setHasNetworkConnection(false);
		} else if (!App.hasNetworkConnection() && status) {
			App.setHasNetworkConnection(true);
			App.lastNetworkOn = new Date();

			//Force zmq restart for faster reconnection
			localHandler.removeMessages(MessageFactory.zmqRestart().what);
			localHandler.sendMessageDelayed(MessageFactory.zmqRestart(), 15000);

			//Dequeue eventual offline trip or events
			privateHandler.sendEmptyMessage(Connectors.MSG_TRIPS_SENT_OFFLINE);
			privateHandler.removeMessages(Connectors.MSG_EVENTS_SENT_OFFLINE);
			privateHandler.sendEmptyMessageDelayed(Connectors.MSG_EVENTS_SENT_OFFLINE, 10000);

			//Force time check for best time
			localHandler.sendMessage(MessageFactory.sendTimeCheck());

			sendBeacon();

		}

	}

	private static int restart3GCount = 0;

	private void CheckNetwork() {
		if (!SystemControl.hasNetworkConnection(this, eventRepository)) {
			DLog.D(ObcService.class.toString() + " NO NETWORK CONNECTION");
			setNetworkConnectionStatus(false);
			if (restart3GCount >= 8 && !this.tripInfo.isOpen) {
				restart3GCount = 0;
				eventRepository.Reboot("No 3G Reboot");
				dlog.cr("ObcService.CheckNetwork(),Eseguo reboot schedulato NO3G");
				SystemControl.doReboot(SystemControl.RebootCause.NO_3G);
			} else {

				App.Instance.setDNS();

				if (WITH_AUTORESET3G) {
					SystemControl.Reset3G(this);
				}
				restart3GCount++;
			}

			if (WITH_OUTOFORDER_WATCHDOG && App.lastNetworkOn != null) {
				long delta = (new Date().getTime() - App.lastNetworkOn.getTime()) / 1000;
				if (delta > 31 * 60 & App.reservation == null) {
					dlog.w("ObcService.CheckNetwork();Timeout max network offline : out of order local reservation");
					this.setReservation(Reservation.createOutOfOrderReservation());
					eventRepository.BeginOutOfOrder("NET");
				}
			}

		} else {
			App.lastNetworkOn = new Date();
			restart3GCount = 0;
			if (App.reservation != null && App.reservation.isMaintenance() && App.reservation.isLocal()) {
				dlog.w("ObcService.CheckNetwork();Network on - removing out of order local reservation");
				setReservation(null);
				eventRepository.EndOutOfOrder();
			}
			setNetworkConnectionStatus(true);
		}
	}

	public void sendBeacon() {

		if (!App.hasNetworkConnection()) {
			dlog.w("ObcService.sendBeacon();No connection. Beacon aborted");
			return;
		}

		sendBeacon(carInfo);
        /*if (msg != null) {
            dlog.d("Sending beacon : " + msg);
            //udpServer.sendBeacon(msg);

            beaconRepository.sendBeacon(msg).subscribeOn(Schedulers.io())
                    .subscribe(new Observer<BeaconResponse>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d){
                        }

                        @Override
                        public void onNext(@NonNull BeaconResponse beaconResponse) {
                            DLog.D("BeaconResponse is: " + beaconResponse.toString());
                            parseServerNotify(beaconResponse.getJson());
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                        }

                        @Override
                        public void onComplete() {
                        }
                    });

//            NotifiesConnector nc = new NotifiesConnector();
//            nc.setTarga(App.CarPlate);
//            nc.setBeacon(msg);
//
//            HttpConnector httpConnector = new HttpConnector(ObcService.this);
//            httpConnector.HttpMethod = HttpConnector.METHOD_GET;
//            httpConnector.Execute(nc);

        }*/

	}

	public void sendBeacon(final CarInfo carinfo) {

		if (!App.hasNetworkConnection()) {
			dlog.w("ObcService.sendBeacon();No connection. Beacon aborted");
			return;
		}

//        String msg = carInfo.getJson(true);
		if (carinfo != null) {
			//dlog.d("Sending beacon : " + carinfo.);
			//udpServer.sendBeacon(msg);
			Observable.just(1).delay(1500, TimeUnit.MILLISECONDS)
					.concatMap(i ->
							beaconRepository.sendBeacon(carinfo.getJson(true)))
					.subscribeOn(Schedulers.io())
					.observeOn(Schedulers.computation())
					.subscribe(new Observer<BeaconResponse>() {
						Disposable disposable;

						@Override
						public void onSubscribe(Disposable d) {
							disposable = d;
						}

						@Override
						public void onNext(@NonNull BeaconResponse beaconResponse) {
							DLog.D("ObcService.sendBeacon();BeaconResponse is: " + beaconResponse.toString());
							parseServerNotify(beaconResponse.getJson());
						}

						@Override
						public void onError(@NonNull Throwable e) {
							RxUtil.dispose(disposable);
						}

						@Override
						public void onComplete() {
							DLog.I("ObcService.sendBeacon();Beacon sent successfully!");
							RxUtil.dispose(disposable);
						}
					});

//            NotifiesConnector nc = new NotifiesConnector();
//            nc.setTarga(App.CarPlate);
//            nc.setBeacon(msg);
//
//            HttpConnector httpConnector = new HttpConnector(ObcService.this);
//            httpConnector.HttpMethod = HttpConnector.METHOD_GET;
//            httpConnector.Execute(nc);

		}

	}

	public void sendAll(Message msg) {

//		Message myMsg = Message.obtain();
		if (msg == null) {
			dlog.e("ObcService.sendAll();Message==null client: " + clients.size());
			return;
		}
//TODO change with foreach

		if (msg.what != ObcService.MSG_CAR_INFO &&
				msg.what != ObcService.MSG_RADIO_SEEK_INFO &&
				msg.what != ObcService.MSG_CAR_LOCATION) {
			dlog.i("ObcService.sendAll();Sending to " + clients.size() + " clients MSG id " + msg.what);
		}

		try {
			if (clients.size() > 0){
				for (Clients cliente : clients) {
//                    dlog.d("Sending to client "+cliente.getClass());
					Message myMsg = Message.obtain();
					myMsg.copyFrom(msg);
					cliente.getClient().send(myMsg);
					if (msg.what == 50){
						dlog.d("ObcService.sendAll();perf: sent to client");
					}

				}
				//clients.get(clients.size() - 1).getClient().send(msg);
			}
			else {
				if (!_pendingUiCheck) {
					_pendingUiCheck = true;
					dlog.d("ObcService.sendAll();Empty clients list. Scheduling check in 2sec");
					localHandler.sendMessageDelayed(localHandler.obtainMessage(ObcService.MSG_UI_CHECK), 2000);
				}
			}
		} catch (RemoteException e1) {
			dlog.e("ObcService.sendAll();Remote Exception", e1);

		}

//		for (Messenger client : clients) {
//			try {
//				client.send(msg);
//			} catch (RemoteException e) {
//				dlog.e("Service client does not respond. Removing",e);
//				clients.remove(client);
//			}
//		}
	}

	public void setDisplayStatus(boolean on, int delay) {

		dlog.i("ObcService.setDisplayStatus();displayStatus:" + on + ";delay:" + delay);

		if (screenLockTrip != null) {
			if (on) {
				screenLockTrip.acquire(10000);	//TODO: check if timeout is right
			} else {
				if (screenLockTrip.isHeld()){
					screenLockTrip.release();
				}
			}
		}

/*		if (on) {
			if (screenLockTrip != null)
				screenLockTrip.acquire();

		} else {
			if (screenLockTrip != null && screenLockTrip.isHeld())
				screenLockTrip.release();
		}*/

		if (!on && delay > 0) {
			Message msg = MessageFactory.setDisplayStatus(on);
			localHandler.sendMessageDelayed(msg, delay * 1000);
		} else {
			if (obc_io != null) {
				obc_io.setDisplayStatus(null, on);
			}
		}

	}


	/**
	 * Message routing and handling
	 */
	public void obc_ioInit() {
		if (tripInfo != null) {
			if (tripInfo.isOpen) {
				dlog.d("ObcService.obc_ioInit();Sending cardCode to OBC_IO: " + tripInfo.cardCode);
				if (!App.motoreAvviato && App.getParkModeStarted() != null && (App.parkMode == ParkMode.PARK_STARTED)) {
					obc_io.setLed(null, LowLevelInterface.ID_LED_BLUE, LowLevelInterface.ID_LED_ON);//LowLevelInterface.ID_LED_BLINK
					obc_io.setLcd(null, " IN SOSTA");
				} else {
					obc_io.setLed(null, LowLevelInterface.ID_LED_BLUE, LowLevelInterface.ID_LED_ON);
					obc_io.setLcd(null, " Auto in uso");
				}
				obc_io.setTag(null, tripInfo.cardCode);
//				if (!App.inSosta) {
//					obc_io.setDoors(null, 0, "");
//					obc_io.setEngine(null, 0);
//				}
			} else {
				dlog.d("ObcService.obc_ioInit();Sending cardCode to OBC_IO: '*' ");
				if (App.reservation == null) {
					obc_io.setLed(null, LowLevelInterface.ID_LED_GREEN, LowLevelInterface.ID_LED_ON);

					obc_io.setLcd(null, "Auto libera");
				} else {
					obc_io.setLed(null, LowLevelInterface.ID_LED_YELLOW, LowLevelInterface.ID_LED_ON);
					obc_io.setLcd(null, "Auto prenotata");
				}
				obc_io.setDoors(null, 0, "");
				obc_io.setEngine(null, 0);
				obc_io.setTag(null, "*");
			}
		} else {
			obc_io.setLed(null, LowLevelInterface.ID_LED_GREEN, LowLevelInterface.ID_LED_ON);
		}
	}

	public void notifyObcIoBoot() {
		dlog.d("ObcService.notifyObcIoBoot();OBC_IO booted");
		//Eventi.eventFwBoot(App.fw_version);
		obc_ioInit();
	}

	public void notifyCard(String id, String event, boolean ObcIohandled) {
		notifyCard(id, event, ObcIohandled, false);
	}

	public void notifyCard(String id, String event, boolean ObcIohandled, boolean forced) {
//		DbManager dbm = App.Instance.dbManager;
		//Customers clienti = dbm.getClientiDao();

		if (id != null && event != null){
			dlog.d("ObcService.notifyCard();id:" + id + ", event:" + event);
		}
		else {
			dlog.e("ObcService.notifyCard();id or event == null");
		}

		//First notify to all registered consumers the card event
		Message msg = MessageFactory.notifyCard(id, event);
		sendAll(msg);

		//... then schedule a whitelist update, so before PIN dialog we have updated db

		startDownloadCustomers();
		//clienti.startWhitelistDownload(this, privateHandler);

		// and update employees
		startDownloadEmployees();

		//..finally update properly tripInfo opening or closing trip
		if (tripInfo == null) {
			dlog.e(ObcService.class.toString() + " notifyCard: Tripinfo NULL!");
		} else {
			dlog.d("ObcService.notifyCard();perf: start handleCard");
			Message tripMsg = tripInfo.handleCard(id, event, carInfo, obc_io, this, screenLockTrip, forced ? TripInfo.CloseType.forced : TripInfo.CloseType.normal);
			dlog.d("ObcService.notifyCard();perf: end handleCard");
			if (tripMsg != null) {
				if (tripMsg.what == MSG_TRIP_END){
					if(App.reservation != null) {
						setReservation(App.reservation);
					}

					Message myMsg = Message.obtain();
					myMsg.copyFrom(tripMsg);
					localHandler.sendMessageDelayed(myMsg,10000);
				}
				dlog.d("ObcService.notifyCard();perf: sendAll " + tripMsg.what);
				sendAll(tripMsg);
			}
			carInfo.updateTrips();
			//sendBeacon();
		}
	}

	private boolean firstSOCReceived = false;

	public void notifyCarInfo(Bundle b) {

		if (b == null) {
			dlog.e("ObcService.notifyCarInfo();bundle == null");
			return;
		}
		if (tripInfo == null) {
			dlog.e("ObcService.notifyCarInfo();tripinfo == null");
			return;
		}

		if (carInfo == null) {
			carInfo = new CarInfo(localHandler);
			dlog.d("ObcService.notifyCarInfo();CarInfo created");
		}

		Bundle res = carInfo.betterHandleUpdate(b, this);

		if (res.getBoolean("force")) {
			dlog.i("ObcService.notifyCarInfo();force beaconUpdate");
			sendBeacon();
		}
		if (!res.getBoolean("changed"))
			return;

		//TODO: FORZATURA DA RIMUOVERE UNA VOLTA RISOLTO IL PROBLEMA AGGIORNAMENTO DALL AUTO
		if (false && (CarInfo.getKeyStatus().equalsIgnoreCase("ON") || CarInfo.getKeyStatus().equalsIgnoreCase("ACC"))) {

			if (!App.motoreAvviato) {
				dlog.d("ObcService.notifyCarInfo();set motore avviato:" + App.motoreAvviato);
				App.motoreAvviato = true;
				App.Instance.persistMotoreAvviato();
			}
		} else {
			if (App.motoreAvviato) {
				dlog.d("ObcService.notifyCarInfo();set motore avviato: " + App.motoreAvviato);
				App.motoreAvviato = false;
				App.Instance.persistMotoreAvviato();
			}

		}

		if (b.containsKey("SOC")) {
			firstSOCReceived = true;
		}

		long delta = (System.currentTimeMillis() - App.AppStartupTime.getTime()) / 1000;

		if (carInfo.batteryLevel != 0 && carInfo.batteryLevel <= 20 && !App.AlarmSOCSent && delta > 60 && firstSOCReceived) {

            /*String text = String.format("%s -  %d%% SOC", App.CarPlate, carInfo.batteryLevel);
            if (App.currentTripInfo != null) {
                text += String.format(" - Utente %s %s - Tel: %s", App.currentTripInfo.customer.name, App.currentTripInfo.customer.surname, App.currentTripInfo.customer.mobile);
            } else {
                text += " - Macchina libera";
            }*/

			//sendSMS(App.BatteryAlarmSmsNumbers,text);

			App.AlarmSOCSent = true;
		} else if (carInfo.batteryLevel > 25) {
			App.AlarmSOCSent = false;
		}

		//If there is no open trip, at lest 3min after boot, no demo kit , battery level < BatteryShutdownLevel% , and no charge plug : do android shutdown.
        /*if (false && !this.tripInfo.isOpen && App.getAppRunningSeconds() > 30 && carInfo.batteryLevel != 0 && App.BatteryShutdownLevel > 0 && carInfo.batteryLevel <= App.BatteryShutdownLevel && !carInfo.chargingPlug) {
            if(App.startShutdownTimer()){
                dlog.d("Shutdown because of battery: " + carInfo.batteryLevel);
                obc_io.disableWatchdog();
                SystemControl.doShutdown();
            }
        }else
        App.stopShutdownTimer();*/

		//TODO: ottimizzare con invio messaggio solo se dati effettivamente cambiati.
		Message msg = MessageFactory.notifyCarInfoUpdate(carInfo);
		sendAll(msg);

	}

/*
	public void notifyCANData(Bundle b) {

		if (b == null) {
			dlog.e("ObcService.notifyCANData();bundle == null");
			return;
		}
		if (tripInfo == null) {
			dlog.e("ObcService.notifyCANData();tripinfo == null");
			return;
		}

		if (carInfo == null) {
			carInfo = new CarInfo(localHandler);
			dlog.d("ObcService.notifyCANData();CarInfo created");
		}

		//Use BetterHandleUpdate
        */
/*if (carInfo.handleUpdate(b))
            sendBeacon();*//*


		if (b.containsKey("PackAmp") && b.containsKey("timestampAmp")) {

		    double ampere = ((double) b.getInt("PackAmp") * 1000) / (double) b.getLong("timestampAmp");

			if (!carInfo.isChargingPlug()) {
				carInfo.currentAmpere += ampere;
				App.Instance.setCurrentAmp(carInfo.currentAmpere);
			} else {
				carInfo.chargingAmpere += ampere;
				App.Instance.setChargingAmp(carInfo.chargingAmpere);
			}

		}

		Message msg = MessageFactory.notifyCANDataUpdate(carInfo);
		sendAll(msg);

	}
*/

	public void notifyBatteryInfo(Bundle b) {

		if (b != null) {
			dlog.d("ObcService.notifyBatteryInfo();Received battery info: " + b.toString());
		} else {
			dlog.d("ObcService.notifyBatteryInfo();Received battery info: NULL bundle");
		}

	}

	public void notifyRadioInfo(Bundle b) {

		String what = b.getString("what", "");
		Message msg = new Message();

		switch (what) {

			case "RadioVolume":
				msg.what = MSG_RADIO_VOLUME_INFO;
				msg.arg1 = b.getInt("volume", 0);
				if (FRadio.savedInstance != null)
					FRadio.setVolume(msg.arg1);
				break;
			case "CurPlayInfo":
				msg.what = MSG_RADIO_CURPLAY_INFO;
				msg.arg1 = b.getInt("freq", 0);
				msg.obj = b.getString("band", "FM");
				break;
			case "SeekInfo":
				msg.what = MSG_RADIO_SEEK_INFO;
				msg.arg1 = b.getInt("freq", 0);
				msg.obj = b.getString("band", "FM");
				break;
			case "SeekValidFreqInfo":
				msg.what = MSG_RADIO_SEEK_VALID_INFO;
				msg.arg1 = b.getInt("freq", 0);
				msg.obj = b.getString("band", "FM");
				break;
			case "SeekStatus":
				msg.what = MSG_RADIO_SEEK_STATUS;
				//msg.arg1 = b.getInt("status", 0);
				msg.arg1 = b.getInt("freq", 0);
				msg.obj = b.getString("status", "");
				break;
		}

		if (msg.what != 0) {
			sendAll(msg);
		}
	}

	public void notifyNavigateTo(String label, String json) {
		double longitude, latitude;
		Location location = null;
		try {
			JSONObject jobj = new JSONObject(json);
			longitude = jobj.getDouble("longitude");
			latitude = jobj.getDouble("latitude");
			location = new Location("dummy");
			location.setLongitude(longitude);
			location.setLatitude(latitude);

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Message msg = localHandler.obtainMessage(MSG_NAVIGATE_TO);
		msg.obj = location;
		sendAll(msg);

	}

	private void setReservation(Reservation r) {

		//There is a reservation to set
		if (r != null) {
			//If it is the same reservation do nothing;
			if (App.reservation != null && App.reservation.equals(r))
				return;

			App.reservation = r;
			App.Instance.persistReservation();

			//If there is no open trip we can set the new state
			if (App.currentTripInfo == null) {
				obc_io.setReservation(r);
				dlog.d("ObcService.setReservation();received new reservation " + r.toString());

				try {
					if (FWelcome.Instance.isVisible())
						FWelcome.Instance.maintenanceBackground(r);
				} catch (Exception e) {
					dlog.e("ObcService.setReservation();Exception changing fwelcome background", e);
				}
			}
		} else { //If there is a reservation you can remove it
			if (App.reservation != null) {
				App.reservation = null;
				App.Instance.persistReservation();

				if (App.currentTripInfo == null) {
					obc_io.setReservation(null);

					try {
						if (FWelcome.Instance.isVisible())
							FWelcome.Instance.maintenanceBackground(r);
					} catch (Exception e) {
						dlog.e("ObcService.setReservation();Exception changing fwelcome background", e);
					}
					dlog.d("ObcService.setReservation();Ricevuta nuova prenotazione vuota, rimuovo quella precedente");
				}
			}
		}

	}

	private void executeServerCommand(String rawCmd) {

		executeServerCommands(ServerCommand.createFromString(rawCmd));

	}

	private void executeServerCommands(ServerCommand command) {
		List<ServerCommand> list = new ArrayList<>();
		list.add(command);
		executeServerCommands(list);
	}

	private void executeServerCommands(List<ServerCommand> list) {
		Customers clienti;

		if (list == null || list.size() == 0)
			return;

		for (ServerCommand cmd : list) {

			dlog.d("ObcService.executeServerCommands();Executing: " + cmd.command);
			eventRepository.eventCmd(cmd.command + " : " + cmd.intarg1 + " - " + cmd.txtarg1);

			switch (cmd.command.toUpperCase()) {

				case "WLUPDATE":

					startDownloadCustomers();
//                    clienti = App.Instance.dbManager.getClientiDao();
//                    clienti.startWhitelistDownload(this, privateHandler);
					break;

				case "WLCLEAN":
					clienti = App.Instance.dbManager.getClientiDao();
					clienti.deleteAll();

					startDownloadCustomers();
					//clienti.startWhitelistDownload(this, localHandler);
					break;

				case "START_TRIP":
				case "OPEN_TRIP":
					if (App.currentTripInfo == null)
						this.notifyCard(cmd.txtarg1, "OPEN", false, false);
					else
						dlog.w("ObcService.executeServerCommands();OPEN_TRIP ignored since there is already an open trip");
					break;

				case "PARK_TRIP":
					if (App.currentTripInfo != null && App.getParkModeStarted() != null && !App.parkMode.isOn()) {
						this.notifyCard(cmd.txtarg1, "PARK", false, false);
					} else
						dlog.w(ObcService.class.toString() + "ObcService.executeServerCommands();PARK_TRIP ignored since there isn't an open trip or not waiting park start");
					break;

				case "UNPARK_TRIP":
					if (App.currentTripInfo != null && App.getParkModeStarted() != null && App.parkMode.isOn()) {
						this.notifyCard(cmd.txtarg1, "UNPARK", false, false);
					} else
						dlog.w("ObcService.executeServerCommands();UNPARK_TRIP ignored since there isn't an open trip or not in park ");
					break;

				case "CLOSE_TRIP":
					if (App.currentTripInfo != null) {
						boolean forced = (cmd.txtarg1 == null || cmd.txtarg1.isEmpty());
						dlog.d("ObcService.executeServerCommands();CLOSE_TRIP forced : " + forced);
						localHandler.sendMessage(MessageFactory.AudioChannel(LowLevelInterface.AUDIO_NONE, 1));
						if ((cmd.txtarg2 == null || cmd.txtarg2.equalsIgnoreCase("null")) && (cmd.txtarg1 == null || cmd.txtarg1.isEmpty())) {
							long now = new Date().getTime() / 1000;
							if ((cmd.ttl <= 0 || cmd.queued + cmd.ttl > now) || cmd.command.equalsIgnoreCase("CLOSE_TRIP"))
								this.notifyCard(App.currentTripInfo.cardCode, "CLOSE", false, forced);
							else
								dlog.d("ObcService.executeServerCommands();Received expired close_trip");
						} else if (cmd.txtarg1 != null && cmd.txtarg1.equalsIgnoreCase(App.currentTripInfo.customer.card_code) && !App.currentTripInfo.remoteCloseRequested) {//chiusura tramite API
							startRequestCloseTrip(cmd.txtarg1);
						} else
							try {
								JSONObject commandJson = new JSONObject(cmd.txtarg2);

								int tripId = commandJson.optInt("TripId");
								int customersId = commandJson.optInt("CustomerId");
								String timestampBegin = commandJson.optString("TimestampBeginning");

								if (tripId == App.currentTripInfo.trip.remote_id) {
									this.notifyCard(App.currentTripInfo.cardCode, "CLOSE", false, forced);
								} else {
									SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
									if (customersId == App.currentTripInfo.trip.id_customer && timestampBegin.equalsIgnoreCase(simpleDateFormat.format(App.currentTripInfo.trip.begin_time))) {
										try {
											DbManager dbm = App.Instance.dbManager;

											Customers customers = dbm.getClientiDao();
											Customer customer = customers.queryForId(customersId);
											if (customer != null) {
												this.notifyCard(customer.card_code, "CLOSE", false, forced);

											} else
												throw new Exception("No customer found");
										} catch (Exception e) {
											dlog.e("ObcService.executeServerCommands();queryForId - customers not found ", e);
										}

									} else {
										dlog.e("ObcService.executeServerCommands();No match tripId , customersId and timestamp");
									}
								}
							} catch (Exception e) {

								dlog.e("ObcService.executeServerCommands();close_trips - Error parsing json", e);
							}

					} else
						dlog.w("ObcService.executeServerCommands();CLOSE_TRIP ignored since there is a no open trip");
					break;

				case "RESEND_TRIP":
					Trips corse = App.Instance.getDbManager().getTripDao();
					corse.ResetFailed();
					privateHandler.sendEmptyMessage(Connectors.MSG_TRIPS_SENT_OFFLINE);
					break;
				case "RESEND_EVENTS":
					Events events = App.Instance.getDbManager().getEventiDao();
					events.ResetFailed();
					privateHandler.sendEmptyMessage(Connectors.MSG_EVENTS_SENT_OFFLINE);
					break;

				case "SET_DOORS":
					obc_io.setDoors(null, cmd.intarg1, "COMANDO DA  CALL-CENTER");
					dlog.d("ObcService.executeServerCommands();Received SET_DOORS command : " + cmd.intarg1);
					break;

				case "SET_ENGINE":
					obc_io.setEngine(null, cmd.intarg1);
					dlog.d("ObcService.executeServerCommands();Received SET_ENGINE command : " + cmd.intarg1);
					break;

				case "SET_NAVIGATOR":
					dlog.d("ObcService.executeServerCommands();Set navigator: " + cmd.intarg1);
					App.Instance.setNavigatorEnable(cmd.intarg1 == 1);
					break;

				case "SET_DAMAGES":
					dlog.d("ObcService.executeServerCommands();Set damages: " + cmd.txtarg1);
					App.Instance.setDamages(cmd.txtarg1);
					break;

				case "SET_FUELCARD_PIN":
					dlog.d("ObcService.executeServerCommands();Set FuelCard PIN");
					App.Instance.setFuelCardPIN(cmd.txtarg1);
					break;

				case "OPEN_SERVICE":
					dlog.d("ObcService.executeServerCommands();Open service page");
					Intent i = new Intent(ObcService.this, ServiceTestActivity.class);
					i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(i);
					break;

				case "SEND_LOGS":
					File file = null;

					if (cmd.txtarg1 != null && !cmd.txtarg1.isEmpty()) {
						file = new File(cmd.txtarg1);
						dlog.d("ObcService.executeServerCommands();Requested specific file:" + cmd.txtarg1);
					}

					UploaderLog.StartLogUploadTask(file, null);
					break;

				case "SEND_SYSREPORT":
					ACRA.getErrorReporter().handleSilentException(null);
					break;

				case "SET_LOCATION":
					dlog.d("ObcService.executeServerCommands();Setting location :" + cmd.txtarg1);
					if (cmd.txtarg1 != null)
						App.Instance.setMockLocation(cmd.txtarg1);
					break;

				case "END_CHARGE":
					dlog.d("ObcService.executeServerCommands();Remote end charge ");
					localHandler.sendMessage(MessageFactory.sendEndCharging());
					break;

				case "ADMINS_UPDATE":
					dlog.d("ObcService.executeServerCommands();Update admins cards list");
					//startDownloadAdmins();
					break;

				case "ADMINS_CLEAN":
					dlog.d("ObcService.executeServerCommands();Update admins cards list");
					startCleanAdmins();
					break;

				case "SHUTDOWN":
					dlog.d("ObcService.executeServerCommands(); Received shutdown command");
					obc_io.disableWatchdog();
					SystemControl.doShutdown(60000);
					break;
				case "REBOOT":
					dlog.d("ObcService.executeServerCommands();Received reboot command");
					obc_io.disableWatchdog();
					SystemControl.doReboot(SystemControl.RebootCause.ADMIN);
					break;
				case "FORCE_REBOOT":
					SystemControl.ForceReboot();
					break;

				case "SET_CONFIG":
					dlog.d("ObcService.executeServerCommands();Setting config :" + cmd.txtarg1);
					App.Instance.setConfig(cmd.txtarg1, cmd.payload);
					break;

				case "START_JOB":
					String job = cmd.txtarg1;
					if (job == null)
						break;
					dlog.d("ObcService.executeServerCommands();Starting job: " + job);
/*
					if (job.equalsIgnoreCase("TestBatteryAlarm")) {
						//sendSMS(App.BatteryAlarmSmsNumbers,"Battery alarm test : "+App.CarPlate);
					} else
*/

					if (job.equalsIgnoreCase("GetDeviceInfo")) {
						eventRepository.DeviceInfo(App.Versions.toJson());
					} else {
						dlog.e("ObcService.executeServerCommands();Unknown job");
					}
					break;

				case "NAVIGATE_TO":
					dlog.d("ObcService.executeServerCommands();Received navigate_to command : " + cmd.txtarg1 + ":" + cmd.payload);
					notifyNavigateTo(cmd.txtarg1, cmd.payload);
					break;
				case "DISABLE_SPEGNIMENTO":
					App.spegnimentoDisabled = cmd.txtarg1.equalsIgnoreCase("true");
					App.Instance.persistSpegnimentoDisabled();
					break;
			}

		}

	}

	private void parseServerNotify(String payload) {

		if (payload == null || payload.isEmpty() || !payload.contains("{"))
			return;

		dlog.i("ObcService.parseServerNotify();");

		JSONObject jobj;
		try {
			jobj = new JSONObject(payload);
		} catch (JSONException e) {
			dlog.e("ObcService.parseServerNotify();Error parsing json payload", e);
			return;
		}

		if (jobj.has("reservations")) {
			int n = 0;
			try {
				n = jobj.getInt("reservations");
			} catch (JSONException e) {
				dlog.e("ObcService.parseServerNotify();Error getting json 'reservations' field", e);
			}
			if (localHandler != null && n > 0) {
				Message msg = localHandler.obtainMessage(ObcService.MSG_SERVER_NOTIFY);
				msg.arg1 = ObcService.SERVER_NOTIFY_RESERVATION;
				msg.arg2 = n;
				localHandler.sendMessage(msg);
				dlog.i("ObcService.parseServerNotify();MSG_SERVER_NOTIFY - SERVER_NOTIFY_RESERVATION sent :" + n);
			}
		}

		if (jobj.has("commands")) {
			int n = 0;
			try {
				n = jobj.getInt("commands");
			} catch (JSONException e) {
				dlog.e("ObcService.parseServerNotify();Error getting json 'commands' field", e);
			}
			if (localHandler != null && n > 0) {
				Message msg = localHandler.obtainMessage(ObcService.MSG_SERVER_NOTIFY);
				msg.arg1 = ObcService.SERVER_NOTIFY_COMMAND;
				msg.arg2 = n;
				localHandler.sendMessage(msg);
				dlog.i("ObcService.parseServerNotify();MSG_SERVER_NOTIFY - SERVER_NOTIFY_COMMAND sent :" + n);
			}
		}

		if (jobj.has("rawCommand")) {
			try {
				executeServerCommand(jobj.getString("rawCommand"));
			} catch (JSONException e) {
				dlog.e("ObcService.parseServerNotify();Invalid rawCommand", e);
			}
		}

	}

	public void startDownloadCustomers() {
		DLog.D("ObcService.startDownloadCustomers();");

		apiRepository.getCustomer(0);

//        Customers customers = App.Instance.dbManager.getClientiDao();
//        customers.startWhitelistDownload(this, privateHandler);
	}

	public void startDownloadEmployees() {
		DLog.D("ObcService.startDownloadEmployees();");

		apiRepository.getEmployee();

//        BusinessEmployeeConnector connector = BusinessEmployeeConnector.GetDownloadConnector();
//        HttpsConnector http = new HttpsConnector(this);
//        http.SetHandler(localHandler);
//        http.Execute(connector);
	}

	public void startAreaPolygonDownload() {
		DLog.D("ObcService.startAreaPolygonDownload()");

		if (App.fullNode)
			apiRepository.getArea();
		else
			phpRepository.getArea();
	}

	public void startPoiDownload() {
		DLog.D("ObcService.startPoiDownload()");

		if (App.fullNode)
			apiRepository.getPois();
		else
			phpRepository.getPois();
	}

	public void startDownloadReservations() {
		dlog.d("ObcService.startDownloadReservations()");
		Observable.just(1)
				.delay(2000, TimeUnit.MILLISECONDS)
				.concatMap(i -> {
					if (App.fullNode)
						return apiRepository.getReservation();
					else
						return phpRepository.getReservation(App.CarPlate);
				})

				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Observer<Reservation>() {
					Disposable disposable;

					@Override
					public void onSubscribe(Disposable d) {
						disposable = d;
					}

					@Override
					public void onNext(Reservation reservation) {
						setReservation(reservation);
					}

					@Override
					public void onError(Throwable e) {

						if (e instanceof ErrorResponse) {
							if (((ErrorResponse) e).errorType == ErrorResponse.ErrorType.EMPTY) {
								setReservation(null);
							}
						} else if (e instanceof NullPointerException)
							setReservation(null);
						RxUtil.dispose(disposable);
					}

					@Override
					public void onComplete() {
						RxUtil.dispose(disposable);

					}
				});

//        HttpConnector http = new HttpConnector(this);
//        http.SetHandler(localHandler);
//        ReservationConnector rc = new ReservationConnector();
//        rc.setTarga(App.CarPlate);
//        http.Execute(rc);
	}

	public void startDownloadCommands() {

		dlog.d("ObcService.startDownloadCommands()");
		Observable.just(1)
				.concatMap(i -> {
					if (App.fullNode){
                        return apiRepository.getCommands(App.CarPlate);
                    }
					else {
                        return phpRepository.getCommands(App.CarPlate);
                    }
				})
				.subscribeOn(Schedulers.io())
				.subscribe(new Observer<ServerCommand>() {
					Disposable disposable;

					@Override
					public void onSubscribe(Disposable d) {
						disposable = d;
					}

					@Override
					public void onNext(@NonNull ServerCommand command) {
						executeServerCommands(command);
					}

					@Override
					public void onError(@NonNull Throwable e) {
						RxUtil.dispose(disposable);
					}

					@Override
					public void onComplete() {
						DLog.I("ObcService.startDownloadCommands.onComplete();Synced successfully!");
						RxUtil.dispose(disposable);
					}
				});
//            HttpConnector http = new HttpConnector(this);
//            http.SetHandler(localHandler);
//            CommandsConnector rc = new CommandsConnector();
//            rc.setTarga(App.CarPlate);
//            http.Execute(rc);

	}

	@Deprecated
	public void startDownloadAdmins() {
        /*dlog.d("Start Downloading admins");
        HttpsConnector http = new HttpsConnector(this);
        http.SetHandler(privateHandler);
        AdminsConnector rc = new AdminsConnector();
        rc.setCarPlate(App.CarPlate);
        http.Execute(rc);*/
	}

	public void startCleanAdmins() {
		obc_io.resetAdminCards();
	}

	public void startDownloadConfigs() {
		dlog.d("ObcService.startDownloadConfigs()");
		apiRepository.getConfig();
        /*HttpsConnector http = new HttpsConnector(this);
        http.SetHandler(privateHandler);
        ConfigsConnector rc = new ConfigsConnector();
        rc.setCarPlate(App.CarPlate);
        http.Execute(rc);*/
	}

	public void startDownloadModel() {
		dlog.d("ObcService.startDownloadModel()");
		apiRepository.getModel();
        /*HttpsConnector http = new HttpsConnector(this);
        http.SetHandler(privateHandler);
        ConfigsConnector rc = new ConfigsConnector();
        rc.setCarPlate(App.CarPlate);
        http.Execute(rc);*/
	}

	public void notifyServerMessage(int what, int value, String response) {
		dlog.i("ObcService.notifyServerMessage();" + what + " => " + value);
		switch (what) {

			case SERVER_NOTIFY_RAW:
				parseServerNotify(response);
				break;
			case SERVER_NOTIFY_RESERVATION:
				if (value > 0)
					startDownloadReservations();
				break;
			case SERVER_NOTIFY_COMMAND:
				if (value > 0)
					startDownloadCommands();
		}

	}

/*	private void sendSMS(List<String> numbers, String text) {
		for (String number : numbers) {
			sendSMS(number, text);
		}
	}*/

	private void sendSMS(String number, String text) {

		SmsManager sms = SmsManager.getDefault();
		sms.sendTextMessage(number, null, text, null, null);

	}

/*	private byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];

		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
		}

		return data;
	}*/

	void startRemoteUpdateCycle() {

		stopRemoteUpdateCycle();

		setLocationMode(1000);  // During trips lower min time to 1 sec
		obc_io.setSecondaryGPS(1000);

		tripUpdateScheduler = Executors.newSingleThreadScheduledExecutor();

		tripUpdateScheduler.scheduleAtFixedRate(new Runnable() {

//			private boolean _secondRun = false;

			@SuppressWarnings("unused")
			@Override
			public void run() {
				try {
					// Generate fake gps positions for debug, if required
					if (Debug.GENERATE_FAKE_GPS) {
						ObcService.this.sendAll(MessageFactory.sendLocationChange(Debug.getCircleLocation()));
					}

					if (Debug.LOG_TRIP_POSITIONS && App.getLastLocation() != null) {
						dlog.d("ObcService.startRemoteUpdateCycle();Cooord:" + App.getLastLocation().getLatitude() + ":" + App.getLastLocation().getLongitude());
						if (!App.checkParkArea(App.getLastLocation().getLatitude(), App.getLastLocation().getLongitude())){
							dlog.d("Outside area");
						}
					}

					if (WITH_UDPSERVER){
						udpServer.sendBeacon(carInfo.getJsonGson(false));
					}

				} catch (Exception e) {
					dlog.e("ObcService.startRemoteUpdateCycle();Exception inside tripUpdateScheduler", e);
				}

			}

		}, 0, 30, TimeUnit.SECONDS);

		dlog.d("ObcService.startRemoteUpdateCycle();Started remote Update Cycle");
	}

	void stopRemoteUpdateCycle() {

		if (tripUpdateScheduler != null) {
			tripUpdateScheduler.shutdown();
			dlog.d("ObcService.stopRemoteUpdateCycle();Stopped remote Update Cycle");
		}

		tripUpdateScheduler = null;

		setLocationMode(30000);  // If car is idle  30sec is min time between locations updates
		obc_io.setSecondaryGPS(10000);
	}

	void startRequestCloseTrip(final String card_code) {

		stopRequestCloseTrip();

		eventRepository.remoteTripClose("RICEVUTA RICHIESTA card " +card_code);

		closeTripScheduler = Executors.newSingleThreadScheduledExecutor();


		final ScheduledFuture future = closeTripScheduler.scheduleAtFixedRate(new Runnable() {
			/**
			 * usato per chiudere la corsa appena la machcina è ferma
			 */
			int closeTries = 0;
			int globalTries = 0;
			@Override
			public void run() {
				try {
					if (App.currentTripInfo == null || !App.currentTripInfo.isOpen || globalTries++>12) { //ci provo
						stopRequestCloseTrip();
						return;
					}
					boolean keyOff = App.checkKeyOff && !CarInfo.isKeyOn();
					boolean isStop = !gpsController.isMoving();
					if (isStop && keyOff) {//closeTrip
						if(App.checkIsInsideParkingArea()) {
							App.setIsCloseable(true);
							notifyCard(card_code, "CLOSE", false, true);
							dlog.d("ObcService.startRequestCloseTrip();scheduled close trip");
						}
						if(closeTries++>3){
							stopRequestCloseTrip();
							eventRepository.remoteTripClose("ABORT CLOSE isStop: "+isStop +" keyOff: " +keyOff );
						}
					} else {
						dlog.d("ObcService.startRequestCloseTrip();unable to close trip is Stop : " + isStop + " keyOff: " + keyOff);
					}

				} catch (Exception e) {
					dlog.e("ObcService.startRequestCloseTrip();Exception inside closeTripScheduler", e);
					stopRequestCloseTrip();
					return;
				}

			}

		}, 2, 10, TimeUnit.SECONDS);

		App.currentTripInfo.remoteCloseRequested = true;
		dlog.d("ObcService.startRequestCloseTrip();Started startRequestCloseTrip");
	}

	void stopRequestCloseTrip() {
		try {
			if (App.currentTripInfo != null)
				App.currentTripInfo.remoteCloseRequested = false;
		} catch (Exception e) {
			dlog.e("ObcService.stopRequestCloseTrip();Exception", e);
		}

		if (closeTripScheduler != null) {
			closeTripScheduler.shutdown();
			dlog.d("ObcService.stopRequestCloseTrip();close tripScheduler Cycle");
		}

		closeTripScheduler = null;

	}

/*	@Deprecated
	void startCallCenterCall(Messenger replyTo, String number) {
		try {
            *//*CallCenterConnector ccc = new CallCenterConnector();
            ccc.setTripInfo(App.currentTripInfo);
            ccc.setMobileNumber(number);

            HttpConnector httpConnector = new HttpConnector(this);
            httpConnector.setMessenger(replyTo);
            httpConnector.Execute(ccc);*//*

		} catch (Exception e) {
			dlog.e("ObcService.startCallCenterCall();", e);

		}
	}*/

	private boolean _pendingUiCheck = false;
	private long _lastRestart;

	private void checkAndRestartUI() {
		dlog.d("ObcService.checkAndRestartUI();Checking UI: clients = " + clients.size());
		if (clients.size() == 0 && (System.currentTimeMillis() - _lastRestart > 15000)) {

			App.Instance.loadPinChecked();

			dlog.d("ObcService.checkAndRestartUI();userDrunk:" + App.userDrunk);

			if (false && App.userDrunk) {
				dlog.e("ObcService.checkAndRestartUI();Beware! You should not be here");
				Intent i = new Intent(this, AGoodbye.class);
				i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
				i.putExtra(AGoodbye.JUMP_TO_END, true);
				startActivity(i);
			} else if ((tripInfo.isOpen && App.pinChecked)) {
				App.Instance.loadAskClose();

				if ((App.askClose != null && App.askClose.getInt("id", 0) == tripInfo.trip.remote_id && App.askClose.getBoolean("close", false) && App.getParkModeStarted() == null)) {

				/*try {
                    while (((App) getApplicationContext()).getCurrentActivity()!=null)
						if(((App) getApplicationContext()).getCurrentActivity() instanceof AGoodbye) {
							((App) getApplicationContext()).getCurrentActivity().finish();
							dlog.d("kill AGoodbye");
						}
				}catch(Exception e){
					dlog.e("Eccezione durante kill activity",e);
				}
				removeSelfCloseTrip();*/
					dlog.d("ObcService.checkAndRestartUI();Starting AGoodbye");
					Intent i = new Intent(this, AGoodbye.class);
					i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
					i.putExtra(AGoodbye.JUMP_TO_END, true);
					i.putExtra(AGoodbye.EUTHANASIA, false);
					startActivity(i);

					_lastRestart = System.currentTimeMillis();

				} else {

				/*try {
                    while (((App) getApplicationContext()).getCurrentActivity()!=null)
						if(((App) getApplicationContext()).getCurrentActivity() instanceof AGoodbye) {
							((App) getApplicationContext()).getCurrentActivity().finish();
							dlog.d("kill AGoodbye");
						}
				}catch(Exception e){
					dlog.e("Eccezione durante kill activity",e);
				}
				removeSelfCloseTrip();*/
					dlog.d("ObcService.checkAndRestartUI();Restarting map");
					App.Instance.loadInSosta();
					App.Instance.loadMotoreAvviato();
					App.Instance.loadParkModeStarted();
					Intent i = new Intent(ObcService.this, AMainOBC.class);
					i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					//sperimental!!

					startActivity(i);

					_lastRestart = System.currentTimeMillis();
				}
			} else {

				/*try {
					if(((App) getApplicationContext()).getCurrentActivity()!=null)
						((App) getApplicationContext()).getCurrentActivity().finish();
				}catch(Exception e){
					dlog.e("Eccezione durante kill activity",e);
				}
				removeSelfCloseTrip();
				*/
				dlog.d("ObcService.checkAndRestartUI();Restarting welcome");
				//Intent i  = new Intent(ObcService.this, ServiceTestActivity.class);
				Intent i = new Intent(ObcService.this, AWelcome.class);
				try {
					if (tripInfo != null &&
                            tripInfo.trip != null &&
                            tripInfo.trip.n_pin <= 0){
                        scheduleSelfCloseTrip(300, true);
                    }
				} catch (Exception e) {
					dlog.e("ObcService.checkAndRestartUI();null trip", e);
				}
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(i);
			}
		}

		localHandler.removeMessages(MSG_UI_CHECK);
		_pendingUiCheck = false;
	}

	public boolean checkParkArea() {

		if (carInfo == null) {
			return true;
		}

		return App.checkParkArea(this.carInfo.getLatitude(), this.carInfo.getLongitude());
	}

	public void scheduleSelfCloseTrip(int seconds, boolean beforePin) {

		if (App.currentTripInfo == null)
			return;//no openTrip
		if (App.getParkModeStarted() == null && !beforePin) {
			App.askClose.putInt("id", App.currentTripInfo.trip.remote_id);
			App.askClose.putBoolean("close", true);
			App.Instance.persistAskClose();
		}
		Handler handler = getHandler();
		dlog.d("ObcService.scheduleSelfCloseTrip();Schedule selfclose in " + seconds + "secs. BeforePin=" + beforePin);
		Message msg = handler.obtainMessage(ObcService.MSG_TRIP_SELFCLOSE);  //TODO chiudere porte e corsa
		msg.arg1 = beforePin ? 1 : 0;
		msg.obj = "Tempo scaduto";
		handler.sendMessageDelayed(msg, seconds * 1000);
	}

	public void removeSelfCloseTrip() {
		App.askClose.putBoolean("close", false);
		App.Instance.persistAskClose();
		Handler handler = getHandler();
		handler.removeMessages(MSG_TRIP_SELFCLOSE);
	}

	private final BroadcastReceiver BatteryInfoReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context c, Intent i) {
			App.tabletBatteryTemperature = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
			App.tabletBatteryLevel = i.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
			App.tabletBatteryPlugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
		}
	};

	private final BroadcastReceiver AlarmReceiver = new BroadcastReceiver() {

//		int fourHurScheduler = 0;

		@Override
		public void onReceive(Context c, Intent i) {

/*			if (App.currentTripInfo == null) {
//				Schedulers.shutdown();
//				Schedulers.start();
			}*/
			localHandler.sendEmptyMessage(MSG_CHECK_SPEGNIMENTO);
			sendBeacon();

			localHandler.sendMessage(MessageFactory.checkLogSize());

			startDownloadCustomers();
			//App.Instance.dbManager.getClientiDao().startWhitelistDownload(ObcService.this, privateHandler);
			//App.Instance.dbManager.getPoisDao().startDownload(ObcService.this, localHandler);
            /*if(fourHurScheduler++>4*4) {
                fourHurScheduler=0;
                startAreaPolygonDownload(ObcService.this, null);
                startDownloadConfigs();
            }*/
			startDownloadReservations();
			if (App.AreaPolygons.size() == 0)
				startAreaPolygonDownload();
			//startDownloadCommands();

			//Dequeue eventual offline trip or events
			privateHandler.sendEmptyMessageDelayed(Connectors.MSG_TRIPS_SENT_OFFLINE, 20000);
			privateHandler.removeMessages(Connectors.MSG_EVENTS_SENT_OFFLINE);
			privateHandler.sendEmptyMessageDelayed(Connectors.MSG_EVENTS_SENT_OFFLINE, 10000);

			//if(fourHurScheduler%2==0) {
			localHandler.sendMessageDelayed(MessageFactory.zmqRestart(), 10000);
			App.canRestartZMQ = true;
			//}



			if (App.currentTripInfo == null && SystemClock.elapsedRealtime() - App.AppScheduledReboot.getTime() > 24 * 60 * 60 * 1000 && !startedReboot) {
				startedReboot = true;
				if (App.reservation != null) {
					if (!App.reservation.isMaintenance()) {
						dlog.d("ObcService.AlarmReceiver();found reservation while scheduling reboot, aborting");
						startedReboot = false;
						App.AppScheduledReboot.setTime(App.AppScheduledReboot.getTime() + 35 * 60 * 1000);
						return;
					}
				}
				dlog.d("ObcService.AlarmReceiver();Excecuting scheduled reboot");
				eventRepository.Reboot("Scheduled reboot");
				SystemControl.doReboot(SystemControl.RebootCause.DAILY);
			}
			//SystemControl.ResycNTP();

			if (gpsStatus != null) {
				dlog.d("ObcService.AlarmReceiver();GPS: tff = " + gpsStatus.getTimeToFirstFix());
				for (GpsSatellite satellite : gpsStatus.getSatellites()) {
					dlog.d("ObcService.AlarmReceiver();FPS: sat " + satellite.getSnr() + " " + satellite.usedInFix());
				}
			}

		}
	};

	private final BroadcastReceiver ConnectivityChangeReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context c, Intent i) {
			boolean status = SystemControl.hasNetworkConnection(ObcService.this, eventRepository);
			dlog.d("ObcService.ConnectivityChangeReceiver();Broadcast CONNECTIVITY_CHANGE " + i.getAction() + " " + status);
			setNetworkConnectionStatus(status);
		}

	};

//	public Handler getPrivateHandler() {
//		return privateHandler;
//	}

	public Handler getHandler() {
		return localHandler;
	}

	private boolean startedReboot = false;

	@SuppressLint("HandlerLeak")
	private final Handler privateHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {

				case Connectors.MSG_TRIPS_SENT_OFFLINE:
				case Connectors.MSG_TRIPS_SENT_REALTIME:
					Trips corse = App.Instance.getDbManager().getTripDao();
					corse.sendOffline(ObcService.this, this, apiRepository, phpRepository);
					carInfo.updateTrips();
					break;

				case Connectors.MSG_EVENTS_SENT_OFFLINE:
					Events eventi = App.Instance.getDbManager().getEventiDao();
					eventi.spedisciOffline(ObcService.this, this, apiRepository, phpRepository);
					break;

				case Connectors.MSG_DN_ADMINS:
					AdminsConnector obj = (AdminsConnector) msg.obj;
					obc_io.addAdminCard(obj.AdminsList);
					break;

				case Connectors.MSG_DN_CLIENTI:/*
                    CustomersConnector cc = (CustomersConnector) msg.obj;
                    if (cc.getReceivedRecords() > 0) {
                        startDownloadCustomers();
                    }*/
					break;

				case Connectors.MSG_DN_CONFIGS:/*
                    ConfigsConnector cfgc = (ConfigsConnector) msg.obj;
                    if (cfgc.ConfigsString != null) {
                        App.Instance.setConfig(cfgc.ConfigsString, null);
                    }*/
					break;

			}

		}

	};

	@SuppressLint("HandlerLeak")
	private final Handler localHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			if (msg.what != MSG_CAR_LOCATION)
				DLog.I("ObcService.localHandler();received msg id:" + msg.what);

			switch (msg.what) {

				case MSG_PING:

//				obc_io.RequestWhitelistUpload();

					DLog.I("ObcService.localHandler();Received ping request");
					Message timeoutMsg = this.obtainMessage(MSG_CMD_TIMEOUT, 5000, -1, msg.replyTo);
					obc_io.Ping(msg.replyTo, timeoutMsg);
					this.sendMessageDelayed(timeoutMsg, 1000);
					break;

				case MSG_CMD_TIMEOUT:
					DLog.D("ObcService.localHandler();Service: CMD timeout");
					if (msg.obj != null) {
						try {
							Messenger m = (Messenger) msg.obj;
							m.send(Message.obtain(null, MSG_CMD_TIMEOUT));
						} catch (Exception e) {
                            DLog.E("ObcService.localHandler();", e);
						}
					}
					break;

				case MSG_OBC_REINIT:
					DLog.D("ObcService.localHandler();Service: OBC reinit");
					obc_io.init(true);
					break;

				case MSG_CLIENT_REGISTER:
					DLog.I("ObcService.localHandler();Received client registration");
					if (msg.replyTo != null) {
						clients.add(new Clients(msg.arg1, msg.replyTo));
						try {
							msg.replyTo.send(Message.obtain(null, MSG_CLIENT_REGISTER));

							if (tripInfo.isOpen) {  // Se c'è una corsa attualmente aperta notificalo subito al subscriber
								Message rmsg = MessageFactory.notifyTripBegin(tripInfo);
								rmsg.arg1 = 1;
								msg.replyTo.send(rmsg);
							}

						} catch (RemoteException e) {
							DLog.E("ObcService.localHandler();", e);
						}

					} else {
						DLog.E("ObcService.localHandler();Failed client registration. Null Messenger");
					}
					break;

				case MSG_CLIENT_UNREGISTER:
					DLog.I("ObcService.localHandler();Received client unregistration");
					if (msg.replyTo != null) {
						try {
							msg.replyTo.send(Message.obtain(null, MSG_CLIENT_UNREGISTER));
						} catch (RemoteException e) {
							DLog.E("ObcService.localHandler();Error sending to client", e);
						}
						clients.remove(new Clients(msg.arg1, msg.replyTo));

						if (clients.size() == 0 && !_pendingUiCheck) {
							_pendingUiCheck = true;
							dlog.d("ObcService.localHandler();Empty clients list. Scheduling check in 2sec");
							this.sendMessageDelayed(this.obtainMessage(ObcService.MSG_UI_CHECK), 2000);
						}

					} else {
						DLog.E("ObcService.localHandler();Failed client registration. Null Messenger");
					}

					break;

				case MSG_UI_CHECK:
					DLog.I("ObcService.localHandler();Received UI Check");
					checkAndRestartUI();
					break;

				case MSG_IO_PLATE:
					DLog.I("ObcService.localHandler();Set plate: " + msg.obj);
					obc_io.setCarPlate(msg.replyTo, (String) msg.obj);
					break;

				case MSG_IO_LEDS:
					DLog.I("ObcService.localHandler();Set led");
					obc_io.setLed(msg.replyTo, msg.arg1, msg.arg2);
					obc_io.setLcd(null, "AUTO IN USO");
					break;

				case MSG_IO_DOORS:
					DLog.I("ObcService.localHandler();Set doors :" + msg.arg1);
					obc_io.setDoors(msg.replyTo, msg.arg1, (String) msg.obj);
					break;

				case MSG_IO_ENGINE:
					DLog.I("ObcService.localHandler();Set engine :" + msg.arg1);
					obc_io.setEngine(msg.replyTo, msg.arg1);
					break;

				case MSG_IO_LCD:
					DLog.I("ObcService.localHandler();Set LCD :" + msg.arg1);
					obc_io.setDisplayStatus(msg.replyTo, msg.arg1 != 0);
					break;

				case MSG_IO_RESETADMINS:
					DLog.I("ObcService.localHandler();Reset admins");
					obc_io.resetAdminCards();
					break;

				case MSG_CAR_INFO:
					//Bundle b = carInfo.getBundle();
					try {
						Message rmsg = MessageFactory.notifyCarInfoUpdate(carInfo);
						//rmsg.setData(b);
						sendAll(rmsg);
					} catch (Exception e) {
						DLog.E("ObcService.localHandler();Error sending to client", e);
					}
					break;

				case MSG_SERVICE_STOP:
//					isStopRequested = true;
					unregisterReceiver(AlarmReceiver);
					obc_io.close();
					stopSelf();
					break;

				case MSG_CAR_CLEANLINESS:
					if (tripInfo != null && tripInfo.isOpen && tripInfo.trip != null) {
						tripInfo.trip.int_cleanliness = msg.arg1;
						tripInfo.trip.ext_cleanliness = msg.arg2;
						tripInfo.UpdateCorsa();
					}
					break;

				case MSG_CUSTOMER_INFO:

					try {
						Message rmsg = Message.obtain(null, MSG_CUSTOMER_INFO);
						rmsg.obj = tripInfo;
						msg.replyTo.send(rmsg);
					} catch (RemoteException e) {
						DLog.E("ObcService.localHandler();Error sending to client", e);
					}
					break;

				case MSG_CUSTOMER_SOS:
					eventRepository.eventSos((String) msg.obj, ObcService.this);
					//startCallCenterCall(msg.replyTo, (String) msg.obj);
					break;

				case MSG_CUSTOMER_DMG:
					eventRepository.eventDmg((String) msg.obj);
					//Events.eventDmg((String) msg.obj);
					//startCallCenterCall(msg.replyTo, (String) msg.obj);
					break;

				case MSG_CUSTOMER_CHECKPIN:
					try {
						String pin = (String) msg.obj;
						Message rmsg = Message.obtain(null, MSG_CUSTOMER_CHECKPIN);
						rmsg.arg1 = 0;

						if (tripInfo != null) {
							boolean isVerified = tripInfo.CheckPin(pin, false);
							if (isVerified) {
								dlog.i("ObcService.localHandler();Pin OK");
								//obc_io.setEngine(rmsg.replyTo, 1);
								App.setIsCloseable(false);
								removeSelfCloseTrip();
							} else {
								dlog.w("ObcService.localHandler();Pin wrong");
							}
							rmsg.arg1 = isVerified ? 1 : 0;
						}
						msg.replyTo.send(rmsg);
					} catch (Exception e) {
						DLog.E("ObcService.localHandler();Error sending to client", e);
					}
					break;

				case MSG_RESTART_UI:
					checkAndRestartUI();
					break;
				case MSG_CAR_REMOTEUPDATECYCLE:
					if (msg.arg1 == 1) {  //Start
						startRemoteUpdateCycle();
//						startRemotePoiCheckCycle();
					} else {             //Stop
						stopRemoteUpdateCycle();
//						stopRemotePoiCheckCycle();
					}
					break;

				case MSG_TRIP_PARK:
					if (tripInfo != null) {
						Message rmsg = tripInfo.setParkMode(msg.arg1, obc_io);
						sendBeacon();
						if (rmsg != null) {
							try {
								msg.replyTo.send(rmsg);
							} catch (RemoteException e) {
								DLog.E("ObcService.localHandler();Error sending to client", e);
							}
						}
					}
					break;

/*				case MSG_TRIP_EVENT:
					if (tripInfo != null) {
						tripInfo.setEvent(msg.arg1, msg.arg2, (String) msg.obj);
					}
					break;*/

				case MSG_TRIP_SELFCLOSE:
					dlog.d("ObcService.localHandler();RECEIVED MSG_TRIP_SELFCLOSE  arg1=" + msg.arg1);
//					if (App.currentTripInfo != null && App.currentTripInfo.isOpen && ((App.getParkModeStarted() == null && App.isIsCloseable()) || App.getParkModeStarted() != null)) {
					if (App.currentTripInfo != null && App.currentTripInfo.isOpen &&(App.getParkModeStarted() != null || App.isIsCloseable())) {

						localHandler.sendMessage(MessageFactory.AudioChannel(LowLevelInterface.AUDIO_NONE, 1));
						eventRepository.selfCloseTrip(App.currentTripInfo.trip.remote_id, msg.arg1);

						ObcService.this.notifyCard(App.currentTripInfo.cardCode, "CLOSE", false, (msg.arg1 > 0 && !App.pinChecked));
					} else {
						dlog.w("ObcService.localHandler();MSG_TRIP_SELFCLOSE discarded");
					}
					break;

				case MSG_TRIP_CLOSE_FORCED:
					dlog.d("ObcService.localHandler();RECEIVED MSG_TRIP_CLOSE_FORCED ");
					if (App.currentTripInfo != null && App.currentTripInfo.isOpen) {

						localHandler.sendMessage(MessageFactory.AudioChannel(LowLevelInterface.AUDIO_NONE, 1));
						eventRepository.selfCloseTrip(App.currentTripInfo.trip.remote_id, 0);

						ObcService.this.notifyCard(App.currentTripInfo.cardCode, "CLOSE", false, true);
					} else {
						dlog.d("ObcService.localHandler();MSG_TRIP_SELFCLOSE discarded already closed");
					}
					break;

				case MSG_TRIP_SCHEDULE_SELFCLOSE:
					removeSelfCloseTrip();
					if (msg.arg1 > 0) {
						scheduleSelfCloseTrip(msg.arg1, false);
					}
					break;

				case MSG_SERVER_NOTIFY:
					notifyServerMessage(msg.arg1, msg.arg2, (String) msg.obj);
					break;

				case MSG_SERVER_HTTPNOTIFY:
                    /*String response = ((NotifiesConnector) msg.obj).response;
                    notifyServerMessage(msg.arg1, msg.arg2, response);*/
					break;

				case MSG_SERVER_COMMAND:
                    /*if (msg.obj != null) {
                        List<ServerCommand> list = null;
                        try {
                            CommandsConnector cc = (CommandsConnector) msg.obj;
                            if (cc != null)
                                list = cc.getComandoServer();
                        } catch (Exception e) {
                            dlog.e("Exception casting server command:", e);
                            break;
                        }

                        executeServerCommands(list);
                    }*/
					break;

				case MSG_SERVER_COMMAND_NEW:
					if (msg.obj != null) {
						ServerCommand command = null;
						try {
							if (msg.obj instanceof ServerCommand)
								command = (ServerCommand) msg.obj;
						} catch (Exception e) {
							dlog.e("ObcService.localHandler();Exception casting server command:", e);
							break;
						}

						executeServerCommands(command);
					}
					break;

				case MSG_SERVER_RESERVATION:
					//Update retrofit
                   /* ReservationConnector r = null;
                    try {
                        r = (ReservationConnector) msg.obj;
                    } catch (Exception e) {
                        dlog.e("Exception casting reservation:", e);
                        break;
                    }

                    setReservation(r.getReservation());*/
					break;

				case MSG_SERVER_CHANGE_IP:
					App.ServerIP = msg.arg1;
					App.Instance.persistServerIP();
					App.Instance.initSharengo();
					break;

				case MSG_CAR_KEY_CHECK:
					App.checkKeyOff = (msg.arg1 == 1);
					App.Instance.checkKeyOff();
					break;
				case MSG_SERVER_CHANGE_LOG:
					App.saveLog = (msg.arg1 == 1);
					App.Instance.persistSaveLog();
					break;

				case MSG_RADIO_SET_CHANNEL:
					String band = (String) msg.obj;
					int freq = msg.arg1;
					obc_io.SetRadioChannel(band, freq);
					break;

				case MSG_RADIO_SET_VOLUME:
					int vol = msg.arg1;
					obc_io.SetRadioVolume(vol);

                    /*try {
                        if (msg.arg1 == 0) {
                            AudioPlayer.lastAudioState = msg.arg1;
                            ProTTS.lastAudioState = msg.arg1;
                        }
                    } catch (Exception e) {

                    }*/
					break;

				case MSG_RADIO_SET_SEEK:
					int direction = msg.arg1;
					boolean auto = msg.arg2 != 0;
					obc_io.SetSeek(direction, auto);
					break;

				case MSG_AUDIO_CHANNEL:
					obc_io.setAudioChannel(msg.arg1, msg.arg2);
					try {

						if (!AudioPlayer.reqSystem && !ProTTS.reqSystem) {               //controllo se la richiesta non parte da audio player
							AudioPlayer.lastAudioState = msg.arg1;
							ProTTS.lastAudioState = msg.arg1;
						}

						//controllo se è stato impostato System
						AudioPlayer.isSystem = msg.arg1 == 2;

						if (AudioPlayer.reqSystem || ProTTS.reqSystem) {                    //se c'era la richiesta la tolgo
							AudioPlayer.reqSystem = false;
							ProTTS.reqSystem = false;
						}

					} catch (Exception e) {
						dlog.e("ObcService.localHandler();Exception while cleaning player state", e);
					}

					break;

				case MSG_ZMQ_RESTART:
					if (WITH_ZMQNOTIFY)
						zmqSubscriber.Restart(localHandler);
					break;

				case MSG_CHECK_LOG_SIZE:
					new OldLogCleamup().execute();
					//new LogCleanup().execute();
					break;

				case MSG_TRIP_SENDBEACON:
					sendBeacon();
					break;

				case MSG_CAR_END_CHARGING:

					dlog.d("ObcService.localHandler();Received endCharging message");
					if (!carInfo.isChargingPlug() && App.isCharging()) {
						App.setCharging(false);
						App.Instance.persistCharging();
						if (FMaintenance.Instance != null)
							FMaintenance.Instance.update(carInfo);
						if (Math.max(carInfo.bmsSOC, carInfo.bmsSOC_GPRS) == 100) {
							if (carInfo.maxAmpere != 0)
								carInfo.maxAmpere = (carInfo.maxAmpere + carInfo.currentAmpere) / 2;
							else
								carInfo.maxAmpere = carInfo.currentAmpere;

							App.Instance.setMaxAmp(carInfo.maxAmpere);
						} else {
							carInfo.currentAmpere = carInfo.chargingAmpere;
							App.Instance.setCurrentAmp(carInfo.currentAmpere);
						}

						sendBeacon();
						if (FMaintenance.Instance != null)
							FMaintenance.Instance.update(carInfo);
					}
					break;
				case ObcService.MSG_CAR_START_CHARGING:

					if (carInfo.isChargingPlug()) {
						carInfo.chargingAmpere = carInfo.currentAmpere;
					}
					break;

				case MSG_CAR_LOCATION:
					//Fan-out message
					Message nmsg = Message.obtain();
					nmsg.copyFrom(msg);
					//if(msg.obj instanceof Location)
					//dlog.d("Location Changed "+((Location)msg.obj).getLongitude()+" "+((Location)msg.obj).getLatitude());
					sendAll(nmsg);
					break;
				case MSG_DEBUG_CARD:

					localHandler.sendMessageDelayed(MessageFactory.sendDebugCardOpen((String) msg.obj), 10000);
					break;

				case MSG_DEBUG_CARD_OPEN:
					//Fan-out message

					notifyCard((String) msg.obj, "OPEN FS", false);
					break;

				case MSG_CHECK_TIME:
					//Fan-out message

					timeCheckScheduler.schedule(timeCheckRunnable, 0, TimeUnit.SECONDS);
					break;

				case MSG_FAILED_SOS:
					sendAll(MessageFactory.failedSOS());
					break;
				case MSG_TRIP_END:
					localHandler.sendEmptyMessageDelayed(MSG_TRIP_SENDBEACON,10*60*1000);
					localHandler.sendEmptyMessageDelayed(MSG_TRIP_SENDBEACON,5*60*1000);
				case MSG_CHECK_SPEGNIMENTO:
					/*try {
						if (App.spegnimentoEnabled && !App.spegnimentoDisabled && Integer.parseInt(carInfo.getSdk_version().replaceAll("[^0-9]", "")) >= 477 && (App.currentTripInfo == null || !App.currentTripInfo.isOpen) && (App.reservation == null || App.reservation.isMaintenance()) && !BuildConfig.BUILD_TYPE.equalsIgnoreCase("debug")) {
							if (!apiRepository.isCustomerRunning()) {
								long time = 20 * 60 * 1000;
								sendBeacon();
								localHandler.sendEmptyMessageDelayed(MSG_TRIP_SENDBEACON,time/4);
								localHandler.sendEmptyMessageDelayed(MSG_TRIP_SENDBEACON,time/2);
								SystemControl.doShutdown(time);//dovrebbe essere 10 minuti
								dlog.d("Spegnimento: Spegnimento iniziato " + time);
							} else {
								localHandler.sendEmptyMessageDelayed(MSG_CHECK_SPEGNIMENTO, 2 * 60 * 1000);
								dlog.d("Spegnimento: Spegnimento Rimandato per Whitelist");
							}
						} else {
							dlog.d("Spegnimento: check condition FASLE");
						}
					}catch (NumberFormatException e) {
						try{
							if (App.spegnimentoEnabled && !App.spegnimentoDisabled && (App.currentTripInfo == null || !App.currentTripInfo.isOpen) && (App.reservation == null || App.reservation.isMaintenance()) && !BuildConfig.BUILD_TYPE.equalsIgnoreCase("debug")) {
								if (!apiRepository.isCustomerRunning()) {
									long time = 20 * 60 * 1000;
									sendBeacon();
									localHandler.sendEmptyMessageDelayed(MSG_TRIP_SENDBEACON,time/4);
									localHandler.sendEmptyMessageDelayed(MSG_TRIP_SENDBEACON,time/2);
									SystemControl.doShutdown(time);//dovrebbe essere 10 minuti
									dlog.d("Spegnimento: Spegnimento iniziato " + time);
								} else {
									localHandler.sendEmptyMessageDelayed(MSG_CHECK_SPEGNIMENTO, 2 * 60 * 1000);
									dlog.d("Spegnimento: Spegnimento Rimandato per Whitelist");
								}
							} else {
								dlog.d("Spegnimento: check condition FASLE");
							}
						}catch (Exception ex) {
						dlog.e( "handleMessage: Exception", ex);
						}
					}catch (Exception e) {
						dlog.e( "handleMessage: Exception", e);
					}*/
					break;

			}

		}
	};

	/**
	 * retreive only the first cell greater than 0
	 *
	 */
/*	public float[] getGreaterCellVoltages() {

		ArrayList<Float> values = new ArrayList<>();
		for (int i = 0; i < 24; i++) {
			if (obc_io.getCellVoltageValue(i) == 0)
				break;
			values.add(obc_io.getCellVoltageValue(i));
		}

		float[] finalValues = new float[values.size()];

		for (int i = 0; i < values.size(); i++)
			finalValues[i] = values.get(i);

		return finalValues;
	}*/

	public float[] getCellVoltages() {

		float[] values = new float[24];
		for (int i = 0; i < 24; i++) {
			values[i] = obc_io.getCellVoltageValue(i);
		}
		if(values[22]==0) {
			float[] tmp = new float[20];
			System.arraycopy(values,0,tmp,0,20);
			values = tmp;
		}

		return values;
	}

	public int getSOCValue() {

		return obc_io.getSOCValue();
	}

	public int getCurrentValue() {
		return (3500 - obc_io.getPackCurrentValue()) / 10;
	}



/*
	public void startRemotePoiCheckCycle() {

		stopRemotePoiCheckCycle();

		DbManager dbm = App.Instance.dbManager;
		Pois DaoPois = dbm.getPoisDao();
		final List<Poi> PoiList = DaoPois.getCityPois(App.DefaultCity.toLowerCase());
		if (PoiList == null) {
			dlog.d("ObcService.startRemotePoiCheckCycle();Abort remote PoiCheck Cycle PoiList is null, city: " + App.DefaultCity);
			return;
		}

		tripPoiUpdateScheduler = Executors.newSingleThreadScheduledExecutor();
		tripPoiUpdateScheduler.scheduleAtFixedRate(new Runnable() {

			@SuppressWarnings("unused")
			@Override
			public void run() {
				try {

					if (System.currentTimeMillis() / 1000 - App.currentTripInfo.trip.begin_timestamp < 60 * 5 || carInfo.batteryLevel >= 25 || App.getLastLocation() == null)
						return;

					for (Poi singlePoi : PoiList) {
						if (App.getLastLocation().distanceTo(singlePoi.getLoc()) <= 90) {
							dlog.i("ObcService.tripPoiUpdateScheduler();found poi enabled to BONUS" + singlePoi.toString());
							sendAll(MessageFactory.notifyTripPoiUpdate(1, singlePoi));
							return;
						}

					}

					sendAll(MessageFactory.notifyTripPoiUpdate(0, null));

				} catch (Exception e) {
					dlog.e("ObcService.tripPoiUpdateScheduler();Exception inside tripPoiUpdateScheduler", e);
				}

			}

		}, 20, 10, TimeUnit.SECONDS);

		dlog.d("ObcService.startRemotePoiCheckCycle();Started remote PoiCheck Cycle, city: " + App.DefaultCity);
	}
*/

/*	public void stopRemotePoiCheckCycle() {

		if (tripPoiUpdateScheduler != null) {
			tripPoiUpdateScheduler.shutdown();
			dlog.d("ObcService.stopRemotePoiCheckCycle();Stopped remotePoiCheckCycle");
		}

		tripPoiUpdateScheduler = null;

	}*/

	@Override
	public void onTripResult(final TripInfo response) {
       /* Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
*/
		if (response.trip.id == App.currentTripInfo.trip.id)
			App.currentTripInfo = response;
		sendAll(MessageFactory.apiTripCallback(response));
            /*}
        },15000);*/
	}

	public boolean isAmpError() {
		return ampError;
	}

	public void setAmpError(boolean ampError) {
		if (App.currentTripInfo == null && (App.reservation == null || App.reservation.isMaintenance() || App.reservation.isLocal()) && ampError) {
			SystemControl.doReboot(SystemControl.RebootCause.AMP);
		}

		if(!ampError && isAmpError())
			SystemControl.cancelRebootCause(SystemControl.RebootCause.AMP);

		this.ampError = ampError;
	}

	private static class DocumentControl extends AsyncTask<URL, Integer, Long> {

		@Override
		protected Long doInBackground(URL... urls) {

			FPdfViewer P2 = new FPdfViewer().newInstance("LIBRETTO", false, true);
			P2.control("ASSICURAZIONE");
			P2.control("LIBRETTO");
			return null;
		}

		@Override
		protected void onPostExecute(Long aLong) {

			super.onPostExecute(aLong);
		}

	}

	public static String FileName;
//	public static SimpleDateFormat fileDate;
	public static String directory = Environment.getExternalStorageDirectory() + "/DataLogger/";
	public static JSONObject j = new JSONObject();
	public int Sec = 15;
	public int[] SensorTempValue = {-3000, -3000, -3000, -3000, -3000, -3000, -3000};

	public void Run() throws JSONException {
		SimpleDateFormat d2 = new SimpleDateFormat("yyyyMMdd", Locale.ITALY);
		String TodayDate = d2.format(new Date());
		if (FileName != null) {
			if (Integer.parseInt(FileName) < Integer.parseInt(TodayDate)) {
				deleteOldLog();

				try {
					File file = new File(directory + FileName + ".json");

					FileWriter output = new FileWriter(file, true);
					output.write(']');
					output.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

			}

		}

		FileName = TodayDate;
		getData();
	}

	public void deleteOldLog() {
		SimpleDateFormat d2 = new SimpleDateFormat("yyyyMMdd", Locale.ITALY);
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, -1);
//		App.Instance.getDbManager().getDataLoggersDao().removeOldLog(cal.getTime());
		int LastmonthDate = Integer.parseInt(d2.format(cal.getTime()));
		File folder = new File(directory);
		if (folder.exists()) {
			File[] fList = folder.listFiles();

			if (fList.length > 0) {

			    for(File file: fList){
//                    int x = Integer.parseInt(file.getName().replace(".json", ""));
                    if (Integer.parseInt(file.getName().replace(".json", "")) < LastmonthDate) {
                        if(file.delete()) {
							dlog.d("ObcService.deleteOldLog();"+file.getName());
						} else {
							dlog.e("ObcService.deleteOldLog();"+file.getName());
						}
                    }
                }

/*				for (int i = 0; i < fList.length; i++) {
					int x = Integer.parseInt(fList[i].getName().replace(".json", ""));
					if (Integer.parseInt(fList[i].getName().replace(".json", "")) < LastmonthDate) {
						fList[i].delete();
					}
				}*/

			}

		}

	}

	public void setOnSensorTempValue(int index, int onSensorTempValue) {
		this.SensorTempValue[index] = onSensorTempValue;
	}

	@SuppressLint("DefaultLocale")
	public void getData() throws JSONException {

		for (int i = 0; i < Data.values().length; i++) {
			getValue(Data.values()[i]);
		}
//		Gson js = new Gson();

		float[] Vcell = getCellVoltages();
		float max = Vcell[0];
		float min = Vcell[0];
		float Vcell_sum = 0;

		for(float cell:Vcell) {
            Vcell_sum = Vcell_sum + cell;
            if (cell > max) {
                max = cell;
            }
            if (cell < min) {
                min = cell;
            }
        }

/*		for (int i = 0; i < Vcell.length; i++) {
			Vcell_sum = Vcell_sum + Vcell[i];
			if (Vcell[i] > max) {
				max = Vcell[i];
			}
			if (Vcell[i] < min) {
				min = Vcell[i];
			}
		}
		*/
		setData(Data.V_MAX_CELL.toString(), String.format("%.02f", max));
		setData(Data.V_MIN_CELL.toString(), String.format("%.02f", min));
		try {
			//  Vcell = js.toJson();
			setData(Data.V_BATTERY.toString(), String.format("%.02f", Vcell_sum));
		} catch (Exception e) {
			//Vcell = "-1";

			setData(Data.V_BATTERY.toString(), "-1");
		}
		int i = 0;
		float meanTEMP = 0;
		for (int c = 0; c < 7; c++) {
			if (SensorTempValue[c] > -10 && SensorTempValue[c] < 45) {
				i = i + 1;
				meanTEMP = meanTEMP + SensorTempValue[c];
			}
		}
		if (i > 0) {
			meanTEMP = meanTEMP / i;
			setData(Data.MEAN_TEMP.toString(), String.format("%.02f", meanTEMP));
		} else
			setData(Data.MEAN_TEMP.toString(), "-3000");

		try {
			SaveToFile();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@SuppressLint("DefaultLocale")
	public void getValue(Data data) throws JSONException {
		switch (data) {
			case KM:
				setData(data.toString(), String.format("%.02f", OptimizeDistanceCalc.totalDistance / 1000));
				break;

			case SOC:
				try {
					setData(data.toString(), String.valueOf(getSOCValue()));
				} catch (Exception e) {
					setData(data.toString(), String.valueOf(-1));
				}
				break;

			case AMPER:
				try {
					int AMP = getCurrentValue();
					setData(data.toString(), String.valueOf(AMP));
				} catch (Exception e) {
					setData(data.toString(), String.valueOf(-1));
				}
				break;

			case TIME:
				setData(data.toString(), String.valueOf(LocalDateTime.now().toString()));
				break;

//			case MEAN_TEMP:
//				//   setData(data.toString(), String.valueOf(LocalDateTime.now().getHourOfDay())+':'+LocalDateTime.now().getMinuteOfHour());
//				break;
//
//			case V_BATTERY:
//				//  setData(data.toString(), getCellVoltages().toString());
//				break;
//
//			case V_MAX_CELL:
//
//				break;
//
//			case V_MIN_CELL:
//
//				break;

			case KM_FROM_TRIP_BEG:
				setData(data.toString(), String.format("%.02f", OptimizeDistanceCalc.tripDistanceValue / 1000));
				break;
			default:

		}
	}

	public void setData(String name, String value) throws JSONException {
		j.put(name, value);
	}

	private boolean SaveToFile() throws IOException {
		boolean result = false;

		File file = new File(directory + FileName + ".json");
		FileWriter output;
		if (file.exists()) {
			output = new FileWriter(file, true);
			output.write(',' + j.toString());
			result = true;
		} else {
			if(file.getParentFile().mkdirs()) {
				if(file.createNewFile()){
					output = new FileWriter(file);
					output.write('[' + j.toString());
					output.close();
					result = true;
				}
			}
		}

		//App.Instance.getDbManager().getDataLoggersDao().saveLog(j);
		return result;
	}

	public enum Data {
		TIME,
		V_BATTERY,
		AMPER,
		V_MAX_CELL,
		V_MIN_CELL,
		MEAN_TEMP,
		SOC,
		KM,
		KM_FROM_TRIP_BEG
	}

}
