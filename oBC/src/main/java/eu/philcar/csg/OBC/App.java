package eu.philcar.csg.OBC;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import static org.acra.ReportField.*;

import org.apache.http.conn.util.InetAddressUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



import org.slf4j.LoggerFactory;

import com.Hik.Mercury.SDK.Manager.CANManager;
import com.skobbler.ngx.SKMaps;
import com.skobbler.ngx.SKMapsInitSettings;
import com.skobbler.ngx.SKPrepareMapTextureListener;
import com.skobbler.ngx.SKPrepareMapTextureThread;
import com.skobbler.ngx.map.SKMapViewStyle;
import com.skobbler.ngx.navigation.SKAdvisorSettings;
import com.skobbler.ngx.packages.SKPackage;
import com.skobbler.ngx.packages.SKPackageManager;
import com.skobbler.ngx.packages.SKPackageURLInfo;
import com.skobbler.ngx.search.SKSearchResult;
import com.skobbler.ngx.versioning.SKVersioningManager;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;
import eu.philcar.csg.OBC.controller.map.util.GeoUtils;
import eu.philcar.csg.OBC.db.Trips;
import eu.philcar.csg.OBC.db.DbManager;
import eu.philcar.csg.OBC.db.Events;
import eu.philcar.csg.OBC.devices.RadioSetup;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.Debug;
import eu.philcar.csg.OBC.helpers.Encryption;
import eu.philcar.csg.OBC.helpers.ServiceTestActivity;
import eu.philcar.csg.OBC.helpers.SkobblerSearch;
import eu.philcar.csg.OBC.server.AreaConnector;
import eu.philcar.csg.OBC.server.HttpConnector;
import eu.philcar.csg.OBC.server.SslConnection;
import eu.philcar.csg.OBC.service.AdvertisementService;
import eu.philcar.csg.OBC.service.CarInfo;
import eu.philcar.csg.OBC.service.ObcService;
import eu.philcar.csg.OBC.service.ParkMode;
import eu.philcar.csg.OBC.service.Reservation;
import eu.philcar.csg.OBC.service.ServiceConnector;
import eu.philcar.csg.OBC.service.TripInfo;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.FloatMath;
import android.util.Log;
import android.widget.Toast;



@ReportsCrashes(
        formKey = "", // This is required for backward compatibility but not used
        formUri = "http://core.sharengo.it/api/post_crashreport.php",
        mode = ReportingInteractionMode.SILENT, 
        customReportContent = {APP_VERSION_CODE , APP_VERSION_NAME,/* SETTINGS_GLOBAL,*/ AVAILABLE_MEM_SIZE, CUSTOM_DATA, STACK_TRACE, USER_APP_START_DATE ,LOGCAT/*, DEVICE_ID, SHARED_PREFERENCES*/ },
        reportType=org.acra.sender.HttpSender.Type.JSON,
        resToastText = R.string.Acra_message
)



public class App extends Application {

	private DLog dlog = new DLog(this.getClass());
	
	public static App Instance;
	public App() {
		Instance = this;
		initSharengo();
		
	}
	

	public static class Versions {
		
		static  {
			AndroidDevice = Build.DEVICE;
			AndroidModel = Build.MODEL;
			AndroidBuild = Build.DISPLAY;
			AndroidRadio = Build.getRadioVersion();
			AndroidSDK =  Build.VERSION.SDK_INT;
		}
		
		public static String AndroidDevice;
		public static String AndroidBuild;
		public static String AndroidModel;
		public static String AndroidRadio;
		public static int    AndroidSDK;
		public static String AppName;
		public static int    AppCode;
		public static int    Service;
		public static String SDK;
		public static String MCU;
		public static String Release;
		public static String VehicleType;
		public static String DeviceSN;
		public static String HwVersion;
		public static String MCUModel;
		public static String TBoxHw;
		public static String TBoxSw;
		public static String VINCode;
		public static String HbVer;
		
		public static int getLevel() {
			if (AndroidDevice==null)
				return 0;
			
			switch (AndroidDevice) {
			
			case "tiny4412":
				return 0;
				
			case "ita_d1":
				return 1;

			default:
				return 0;
			}
					
		}
		
		
		public static String toJson() {
			JSONObject jo = new JSONObject();
			try {
				
				jo.put("AndroidDevice", AndroidDevice);
				jo.put("AndroidBuild",AndroidBuild);
				
				jo.put("AppName", AppName);
				jo.put("AppCode", AppCode);
				
				
				jo.put("Service", Service);
				jo.put("SDK", SDK);
				jo.put("MCU", MCU);
				jo.put("Release", Release);
				jo.put("VehicleType", VehicleType);
				jo.put("DeviceSN", DeviceSN);
				jo.put("HwVersion", HwVersion);
				jo.put("MCUModel", MCUModel);
				jo.put("TBoxHw", TBoxHw);
				jo.put("TBoxSw", TBoxSw);
				jo.put("VINCode", VINCode);				
				jo.put("HbVer", HbVer);
			} catch (JSONException e) {
				DLog.E("Versions to json",e);
			}
			return jo.toString();

		}
		
	}

	

	public void initSharengo() {

		if(App.Instance.ServerIP==1) { //App.Instance.ServerIP==1

			URL_Area = "http://corestage.sharengo.it/api/zone/json.php?";
			URL_Beacon = "http://corestage.sharengo.it/api/pushbeacon.php?";
			URL_Callcenter = "http://mobile.sharengo.it/soscar.php?";
			//URL_Clienti = "http://core.sharengo.it/api/whitelist.php?";
			URL_Clienti = "https://corestage.sharengo.it:8123/whitelist";
			URL_Commands = "http://corestage.sharengo.it/api/get_commands.php?";
			URL_Corse = "http://corestage.sharengo.it/api/pushcorsa.php?";
			URL_Eventi = "http://corestage.sharengo.it/api/pushevent.php?";
			URL_Pois = "http://corestage.sharengo.it/api/get_pois.php?";
			URL_PoisIcons = "http://manage.sharengo.it/pois_Icon.php";
			URL_PoisBanner = "http://manage.sharengo.it/pois_Pos.php";
			URL_Reservations = "http://corestage.sharengo.it/api/get_reservations.php?";
			URL_Notifies = "http://corestage.sharengo.it:7600/notifies?";
			URL_UploadLogs = "http://corestage.sharengo.it/api/pushlogs.php?";
			URL_Configs = "https://corestage.sharengo.it:8123/configs";
			URL_Admins = "https://CSDEMO01@corestage.sharengo.it:8123/admins";
			//URL_Admins = "http://core.sharengo.it:8121/v1/admins";
			URL_ZMQNotifier = "tcp://185.81.1.24:8001";
			URL_AdsBuilder = "http://manage.sharengo.it/banner2.php";
			URL_AdsBuilderCar = "http://manage.sharengo.it/banner2_offline.php";
			URL_AdsBuilderStart = "http://manage.sharengo.it/banner4_offline.php";
			URL_AdsBuilderEnd = "http://manage.sharengo.it/banner5_offline.php";
			IP_UDP_Beacon = "185.81.1.24";
			Port_UDP_Beacon = 7600;
		}
		else {
			URL_Area = "http://core.sharengo.it/api/zone/json.php?";
			URL_Beacon = "http://api.sharengo.it/api/pushbeacon.php?";
			URL_Callcenter = "http://mobile.sharengo.it/soscar.php?";
			//URL_Clienti = "http://core.sharengo.it/api/whitelist.php?";
			URL_Clienti = "https://api.sharengo.it:8123/whitelist";
			URL_Commands = "http://core.sharengo.it/api/get_commands.php?";
			URL_Corse =  "http://core.sharengo.it/api/pushcorsa.php?";
			URL_Eventi = "http://core.sharengo.it/api/pushevent.php?";
			URL_Pois = "http://core.sharengo.it/api/get_pois.php?";
			URL_PoisIcons = "http://manage.sharengo.it/pois_Icon.php";
			URL_PoisBanner = "http://manage.sharengo.it/pois_Pos.php";
			URL_Reservations = "http://core.sharengo.it/api/get_reservations.php?";
			URL_Notifies = "http://core.sharengo.it:7600/notifies?";
			URL_UploadLogs = "http://core.sharengo.it/api/pushlogs.php?";
			URL_Configs = "https://api.sharengo.it:8123/configs";
			URL_Admins = "https://CSDEMO01@api.sharengo.it:8123/admins";
			//URL_Admins = "http://core.sharengo.it:8121/v1/admins";
			URL_ZMQNotifier = "tcp://185.58.119.117:8001";
			URL_AdsBuilder = "http://manage.sharengo.it/banner2.php";
			URL_AdsBuilderCar = "http://manage.sharengo.it/banner2_offline.php";
			URL_AdsBuilderStart = "http://manage.sharengo.it/banner4_offline.php";
			URL_AdsBuilderEnd = "http://manage.sharengo.it/banner5_offline.php";
			IP_UDP_Beacon = "185.58.119.117";


			Port_UDP_Beacon = 7600;


			/*URL_Area = "http://corecina.sharengo.it/api/zone/json.php?";
			URL_Beacon = "http://corecina.sharengo.it/api/pushbeacon.php?";
			URL_Callcenter = "http://mobile.sharengo.it/soscar.php?";
			//URL_Clienti = "http://core.sharengo.it/api/whitelist.php?";
			URL_Clienti = "https://corecina.sharengo.it:8123/whitelist";
			URL_Commands = "http://corecina.sharengo.it/api/get_commands.php?";
			URL_Corse =  "http://corecina.sharengo.it/api/pushcorsa.php?";
			URL_Eventi = "http://corecina.sharengo.it/api/pushevent.php?";
			URL_Pois = "http://corecina.sharengo.it/api/get_pois.php?";
			URL_PoisIcons = "http://manage.sharengo.it/pois_Icon.php";
			URL_PoisBanner = "http://manage.sharengo.it/pois_Pos.php";
			URL_Reservations = "http://corecina.sharengo.it/api/get_reservations.php?";
			URL_Notifies = "http://corecina.sharengo.it:7600/notifies?";
			URL_UploadLogs = "http://corecina.sharengo.it/api/pushlogs.php?";
			URL_Configs = "https://corecina.sharengo.it:8123/configs";
			URL_Admins = "https://CSDEMO01@corecina.sharengo.it:8123/admins";
			//URL_Admins = "http://core.sharengo.it:8121/v1/admins";
			URL_ZMQNotifier = "tcp://122.227.189.54:8001";
			URL_AdsBuilder = "http://manage.sharengo.it/banner2.php";
			URL_AdsBuilderCar = "http://manage.sharengo.it/banner2_offline.php";
			URL_AdsBuilderStart = "http://manage.sharengo.it/banner4_offline.php";
			URL_AdsBuilderEnd = "http://manage.sharengo.it/banner5_offline.php";
			IP_UDP_Beacon = "122.227.189.54";*/
		}

	}

	public enum Fleets { SHARENGO };
	
	
	public static final Fleets FLEET_IDENTITY = Fleets.SHARENGO;
	
	public static final int AWELCOME_UID = 0x0001;
	public static final int ASOS_UID     = 0x0010;
	public static final int AMAINOBC_UID = 0x0100;
	public static final int AGOODBYE_UID = 0x1000;
	public static final int AFAQ_UID	 = 0x0011;
	
	
	public static String URL_Area;
	public static String URL_Beacon;
	public static String URL_PoisIcons;
	public static String URL_PoisBanner;
	public static String URL_Callcenter;
	public static String URL_Clienti;
	public static String URL_Commands;
	public static String URL_Corse;
	public static String URL_Eventi;
	public static String URL_Pois;
	public static String URL_Reservations;
	public static String URL_Notifies;
	public static String URL_UploadLogs;
	public static String URL_Admins;
	public static String URL_ZMQNotifier;
	public static String URL_AdsBuilder;
	public static String URL_AdsBuilderCar;
	public static String URL_AdsBuilderStart;
	public static String URL_AdsBuilderEnd;
	public static String URL_UpdateStartImages;
	public static String URL_UpdateEndImages;
	public static String URL_Configs;
	
	public static String IP_UDP_Beacon;
	public static int    Port_UDP_Beacon;
	
	
	public static final String COMMON_PREFERENCES = "eu.philcar.csg.preferences";
//	public static final String USE_NAVIGATOR = COMMON_PREFERENCES + ".use_navigator";
	
	public DbManager dbManager;	
	
	private ServiceConnector serviceConnector;
	
	private SharedPreferences preferences;
	

	private static final String  KEY_CarPlate = "CarPlate";
	private static final String  KEY_fw_version = "fw_version";
	private static final String  KEY_fuel_level="fuel_level";
	private static final String  KEY_current_apm="current_amp";
	private static final String  KEY_charging_amp="charging_apm";
	private static final String  KEY_max_amp="max_apm";
	private static final String  KEY_MaxVoltage="max_voltage";
	private static final String  KEY_Km="km";
	private static final String  KEY_parkModeStarted="parkModeStarted";
	private static final String  KEY_useNavigator = "use_navigator";
	private static final String  KEY_damages="danni";
	private static final String  KEY_pulizia_int="pulizia_int";
	private static final String  KEY_pulizia_ext="pulizia_ext";
	private static final String	 KEY_Fuelcard_PIN="fuelcard_pin";
	private static final String  KEY_ParkMode = "park_mode";
	private static final String  KEY_ChargeControlEnabled = "charge_control_enabled";
	private static final String  KEY_AlarmEnabled ="alarm_enabled";
	private static final String  KEY_AlarmSmsNumber = "alarm_sms_number";
	private static final String  KEY_BatteryAlarmSmsNumbers = "battery_alarm_sms_numbers";
	private static final String  KEY_Charging = "Charging";
	private static final String  KEY_UseExternalGPS ="use_external_gps";
	private static final String  KEY_RadioSetup ="radio_setup";
	private static final String  KEY_Watchdog = "watchdog";
	private static final String  KEY_BatteryShutdownLevel = "battery_shutdown_level";
	private static final String  KEY_FleetId = "fleet_id";
	private static final String  KEY_ServerIP = "server_ip";
	private static final String  KEY_RebootTime = "reboot_time";
	private static final String  KEY_PersistLog = "persist_log";
	private static final String  KEY_IsAskClose = "is_ask_close";
	private static final String  KEY_IdAskClose = "id_ask_close";
	
	
	public static final String  KEY_LastAdvertisementListDownloaded = "last_time_ads_list_downloaded";
	

	public static RadioSetup radioSetup;
	
	public static String CarPlate="ND";
	public static String Damages = "";
	public static String FuelCard_PIN = null;
	
	public static int    Pulizia_int=0;
	public static int    Pulizia_ext=0;
	
	public static boolean isNavigatorEnabled = true;
	public static int isAdmin=0; //1=MAGGIMI PRIVILEGI; 2=PRIVILEGI RIDOTTI;
	
	public static int     id_Version;
	public static String  sw_Version="ND";
	public static String  fw_version="ND";
	public static Location mockLocation =  null;

	public static int   fuel_level=0;
	public static double chargingAmp;
	public static double currentAmp;
	public static double maxAmp;


	public static float max_voltage=83f;
	public static float getMax_voltage() {
		return max_voltage;
	}

	public static int   km=0;
	public static long  whiteListSize=0;
	public static int   tabletBatteryTemperature=0;
	public static int   tabletBatteryLevel=0;
	public static int   tabletBatteryPlugged=0;
	public static long   networkExceptions=0;
	public static Location lastLocation;
	public static Boolean saveLog =true;


	
	public static TripInfo  currentTripInfo;	
	
	public static Reservation reservation;
	
	public static String MacAddress="";
	public static String IMEI="";
	public static String PhoneNumber="";
	public static String SimSerialNumber="";
	
	private static Date   ParkModeStarted;
	public static ParkMode parkMode = ParkMode.PARK_OFF;
	public static boolean motoreAvviato=false;
	
	public static boolean pinChecked = false;
	public static boolean userDrunk = false;
	
	public static boolean isCloseable=true;
	public static boolean isClosing=false;
	
	public static boolean  ObcIoError=false;
	public static boolean  Charging = false;
	
	public static boolean  AlarmSOCSent=false;
	
	public static boolean  AlarmEnabled=false;
	public static String   AlarmSmsNumber="";
	public static List<String> BatteryAlarmSmsNumbers;
	public static String   DefaultCity="";
	public static boolean  UseExternalGPS=false;
	public static int      Watchdog = 0;
	public static int      BatteryShutdownLevel = 0;
	public static int	   FleetId = 0;
	public static int	   ServerIP = 0;
	
	public static boolean hasNetworkConnection=false;
	public static Date    lastNetworkOn = new Date();
	
	public static Date    AppStartupTime = new Date(), AppScheduledReboot=new Date();
	public static Date    lastUpdateCAN =new Date();
	
	public static final String APP_DATA_PATH = "/sdcard/csg/";
	public static final String  POI_ICON_FOLDER ="/sdcard/csg/PoisIcon/";
	public static final String  POI_POSITION_FOLDER ="/sdcard/csg/PoisPos/";
	public static final String BANNER_IMAGES_FOLDER ="/sdcard/csg/BannerImages/";
	public static final String END_IMAGES_FOLDER ="/sdcard/csg/BannerImages/";
	
	private final String  CONFIG_FILE ="/sdcard/csg/config";
	private final String  STOP_FILE ="/sdcard/csg/stop";

	private final String  AVDPOIS_FILE ="/sdcard/csg/";
	
	private final String  SPLIT_TRIP_CONFIG_FILE ="/sdcard/csg/splittrip.txt";
	private final String  DEFAULT_CITY_CONFIG_FILE = "/sdcard/csg/default_city.txt";
	private final String  MOCKLOCATION_CONFIG_FILE ="/sdcard/csg/mockgps.txt";
	private final String  ZMQ_DISABLE_CONFIG_FILE = "/sdcard/csg/zmq_disable.txt";
	
	private static String foregroundActivity="";
	private static boolean loaded=false;
	
	private SensorManager sensorManager;
	private Sensor        motionDetector;
	
	private SMSreceiver smsReceiver;
	private IntentFilter intentFilter;

	public CANManager CanManager;
	
	private ABase mCurrentActivity = null;
	public static Date update_Poi = new Date();
	public static Date update_StartImages = new Date();
	public static Date update_EndImages = new Date();
	public static boolean first_UP_poi=true; //flag per primo update Start Images
	public static boolean first_UP_Start=true; //flag per primo update Start Images
	public static boolean first_UP_End=true; //flag per primo update End Images
	public static Bundle BannerName= new Bundle();
	public static Bundle askClose=new Bundle();
	
	public static String AreaPolygonMD5;
	public static ArrayList<double[]> AreaPolygons;
	
	//public static final double[] polygon = new double[]{46.091832, 13.235410, 46.096125, 13.234584, 46.097628, 13.239723, 46.092658, 13.240678};	// Udine
	//public static final double[] polygon = new double[]{45.402867 , 9.276161, 45.402867 , 9.053406, 45.542358 , 9.053406, 45.542358 , 9.276161};		// Milano
	//public static final double[] polygon = new double[]{46.090560, 13.067453, 46.090560, 13.174187, 46.123334, 13.174187, 46.123334, 13.067453};		// Fagagna
	public static final double[] polygonCenter = new double[]{45.464189, 9.191181};	// Duomo di Milano
	//public static final double[] polygonCenter = new double[]{46.105813, 13.116209};	// Castello di villalta
	
	
	public void persistInSosta() {
		if (this.preferences != null) {
			Editor e = this.preferences.edit();
			e.putInt(KEY_ParkMode, App.parkMode.toInt());
			e.apply();
		}
	}public void persistAskClose() {
		if (preferences != null) {
			Editor e = this.preferences.edit();
			if(App.askClose!=null) {
				e.putInt(KEY_IdAskClose, App.askClose.getInt("id",0));
				e.putBoolean(KEY_IsAskClose, App.askClose.getBoolean("close",false));
			}
			else{
				e.putInt(KEY_IdAskClose, 0);
				e.putBoolean(KEY_IsAskClose, false);
			}
			e.apply();
		}
	}
	public void persistMotoreAvviato() {
		if (this.preferences != null) {
			Editor e = this.preferences.edit();
			e.putBoolean("motoreAvviato", App.motoreAvviato);
			e.apply();
		}
	}
	public void persistParkModeStarted() {
		if (this.preferences != null) {
			Editor e = this.preferences.edit();
			e.putLong(KEY_parkModeStarted, (ParkModeStarted != null) ? ParkModeStarted.getTime() : 0);
			e.apply();
		}
	}
	public void persistPinChecked() {
		if (this.preferences != null) {
			Editor e = this.preferences.edit();
			e.putBoolean("pinChecked", App.pinChecked);
			e.apply();
		}
	}
	public void persistUserDrunk() {
		if (this.preferences != null) {
			Editor e = this.preferences.edit();
			e.putBoolean("userDrunk", App.userDrunk);
			e.apply();
		}
	}

	public void persistReservation() {
		if (this.preferences != null) {
			Editor e = this.preferences.edit();
			if (App.reservation!=null)
				e.putString("reservation", App.reservation.toJson());
			else
				e.putString("reservation", null);
			e.apply();
		}
	}
	
	public void persistCharging() {
		if (this.preferences != null) {
			Editor e = this.preferences.edit();
			e.putBoolean(KEY_Charging, App.Charging);
			e.apply();
		}
	}
	
	public void persistRadioSetup() {
		if (this.preferences != null ) {
			Editor e = this.preferences.edit();
			if (radioSetup!=null)
				e.putString(KEY_RadioSetup, radioSetup.toJson());
			else
				e.putString(KEY_RadioSetup, "");
			e.apply();
		}
	}
	
	public void persistWatchdog() {
		if (this.preferences != null ) {
			Editor e = this.preferences.edit();
			e.putInt(KEY_Watchdog, Watchdog);
			e.apply();
		}
	}
	
	public void persistBatteryShutdownLevel() {
		if (this.preferences != null ) {
			Editor e = this.preferences.edit();
			e.putInt(KEY_BatteryShutdownLevel, BatteryShutdownLevel);
			e.apply();
		}		
		
	}

	public void persistFleetId() {
		if (this.preferences != null ) {
			Editor e = this.preferences.edit();
			e.putInt(KEY_FleetId, FleetId);
			e.apply();
		}
	}
	public void persistServerIP() {
		if (this.preferences != null ) {
			Editor e = this.preferences.edit();
			e.putInt(KEY_ServerIP, ServerIP);
			e.apply();
		}
	}
	public void persistRebootTime() {
		if (this.preferences != null ) {
			Editor e = this.preferences.edit();
			e.putLong(KEY_RebootTime, SystemControl.rebootInProgress);
			e.apply();
		}
	}
	public void persistSaveLog() {
		if (this.preferences != null ) {
			Editor e = this.preferences.edit();
			e.putBoolean(KEY_PersistLog, saveLog);
			e.apply();
		}
	}

	public void loadInSosta() {
		if (this.preferences != null) {
			App.parkMode = ParkMode.fromInt( this.preferences.getInt(KEY_ParkMode, ParkMode.PARK_OFF_VALUE) );
		} else {
			App.parkMode = ParkMode.PARK_OFF;
		}
	}

	public void loadAskClose() {
		if (this.preferences != null) {
			askClose.putInt("id",preferences.getInt(KEY_IdAskClose,0));
			askClose.putBoolean("close",preferences.getBoolean(KEY_IsAskClose,false));
		} else {

			askClose.putInt("id",0);
			askClose.putBoolean("close",false);
		}
	}

	public void loadMotoreAvviato() {
		if (this.preferences != null) {
			App.motoreAvviato = this.preferences.getBoolean("motoreAvviato", false);
		} else {
			App.motoreAvviato = false;
		}
	}
	public void loadParkModeStarted() {
		if (this.preferences != null) {
			long longDate = this.preferences.getLong(KEY_parkModeStarted, 0);
			App.ParkModeStarted = longDate > 0 ? new Date(longDate) : null;
		} else {
			App.ParkModeStarted = null;
		}
	}
	public void loadPinChecked() {
		if (this.preferences != null) {
			App.pinChecked = this.preferences.getBoolean("pinChecked", false);
		} else {
			App.pinChecked = false;
		}
	}
	public void loadUserDrunk() {
		if (this.preferences != null) {
			App.userDrunk = this.preferences.getBoolean("userDrunk", false);
		} else {
			App.userDrunk = false;
		}
	}
	

	public void loadReservation() {
		if (this.preferences != null) {
			String r = this.preferences.getString("reservation", "");
			if (r!=null)
				App.reservation = Reservation.createFromString(r);
		} else {
			App.reservation = null;
		}
	}
	
	public void loadCharging() {
		if (this.preferences != null) {
			App.Charging = this.preferences.getBoolean(KEY_Charging, false);
		} else {
			App.Charging = false;
		}
	}
	
	
	public void loadBatteryAlarmSmsNumbers() {
		
		BatteryAlarmSmsNumbers = new ArrayList<String>();			
		
		if (this.preferences != null) {
			String json = preferences.getString(KEY_BatteryAlarmSmsNumbers, "[]");
			JSONArray ja;
			try {
				ja = new JSONArray(json);
				for(int i=0; i< ja.length(); i++) {
					if (!ja.isNull(i)) {
						BatteryAlarmSmsNumbers.add(ja.getString(i));
						dlog.d("Add to BatteryAlarmSmsNumbers : " +ja.getString(i));
					}
				}
			} catch (JSONException e) {
				dlog.e("Parsing BatteryAlarmSmsNumbers",e);
			}
		}
		
		if (BatteryAlarmSmsNumbers.size()==0) {
			BatteryAlarmSmsNumbers.add("3442653987");
			BatteryAlarmSmsNumbers.add("3703322640");
			BatteryAlarmSmsNumbers.add("3423316934");
		}

	}
	
public void loadRadioSetup() {
		
			
		if (this.preferences != null) {
			String json = preferences.getString(KEY_RadioSetup, "");
			dlog.d("RadioSetup from preferences : " + json);
			radioSetup = RadioSetup.fromJson(json);
			
		}
		
		if (radioSetup==null) {
			radioSetup = new RadioSetup();
			radioSetup.addChannel("FM", 105.10, "LifeGate");
			radioSetup.addChannel("FM", 107.60, "Radio Popolare");
			radioSetup.addChannel("FM", 99.70, "Radio Deejay");
			radioSetup.addChannel("FM", 104.50, "Virgin Radio");
			
			persistRadioSetup();
			
			dlog.d("RadioSetup from defaults : " + radioSetup.toJson());
		}

	}
	
	
	public boolean loadZmqDisabledConfig() {
		File f = new File(ZMQ_DISABLE_CONFIG_FILE);
		
		return f.exists();
	}
	
	public int loadSplitTripConfig() {
		File f = new File(SPLIT_TRIP_CONFIG_FILE);
		if (f.exists()) {
			try {
			    BufferedReader br = new BufferedReader(new FileReader(f));
			    String line;
			    if ((line = br.readLine()) != null) {
			    	br.close();
			    	return Integer.parseInt(line);
			    }
				br.close();
			} catch (Exception e) {
				dlog.e("Loading split trip config : " + SPLIT_TRIP_CONFIG_FILE,e);
			}
	    
		}
		return 0;
		
	}
	
	
	public String loadDefaultCity() {
		File f = new File(DEFAULT_CITY_CONFIG_FILE);
		if (f.exists()) {
			try {
			    BufferedReader br = new BufferedReader(new FileReader(f));
			    String line;
			    if ((line = br.readLine()) != null) {
			    	br.close();
			    	App.DefaultCity = line;
			    	return  line;
			    }
				br.close();
			} catch (Exception e) {
				dlog.e("Loading split trip config : " + DEFAULT_CITY_CONFIG_FILE,e);
			}
	    
		}
		return "";
		
	}
	
	
	public void SaveDefaultCity(String city) {
		
		App.DefaultCity = city;
		
		File f = new File(DEFAULT_CITY_CONFIG_FILE);
		
		if (city==null) {
			f.delete();
		} else {		
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(f));				
				bw.write(city);
				bw.close();
			} catch (IOException e) {
				dlog.e("Writing default city",e);
	
			}
		}
		
			
	}


	public void setMockLocation(String json) {
		
		if (json==null || json.isEmpty() || json.equalsIgnoreCase("null")) {
			setMockLocation(0,0);
			return;
		}
		
		try {
			JSONArray ja = new JSONArray(json);
			if (ja.length()>=2) {
				setMockLocation(ja.getDouble(0),ja.getDouble(1));
			}
			
		} catch (JSONException e) {
			dlog.e("Setting MockLocation",e);
		}
		
		
	}
	
	public void setMockLocation(double lat, double lon) {
		File f = new File(MOCKLOCATION_CONFIG_FILE);
		
		if (lat==0 && lon==0) {
			f.delete();
		} else {		
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(f));
				String line = String.format(Locale.US,"%f;%f", lat,lon);
				bw.write(line);
				bw.close();
			} catch (IOException e) {
				dlog.e("Writing MOCKLOCATION",e);
	
			}
		}
		
		loadMockLocationConfig();
	}
	
	public void loadMockLocationConfig() {
		File f = new File(MOCKLOCATION_CONFIG_FILE);
		
		if (f.exists()) {
			try {
			    BufferedReader br = new BufferedReader(new FileReader(f));
			    String line;
			    if ((line = br.readLine()) != null) {
			    	String p[] = line.split(";");

			    	if (p!=null && p.length>=2) {
				    	App.mockLocation = new Location(LocationManager.GPS_PROVIDER);
				    	App.mockLocation.setLatitude(Double.parseDouble(p[0]));
				    	App.mockLocation.setLongitude(Double.parseDouble(p[1]));
				    	App.mockLocation.setAccuracy(1);
				    	
				    	App.lastLocation = App.mockLocation;
			    	}
			    	
			    }
			    br.close();
			} catch (Exception e) {
				dlog.e("Loading split trip config : " + MOCKLOCATION_CONFIG_FILE,e);
			}
	    
		} else {
			App.mockLocation = null;
		}
		
		
	}
	
	
	
	
	@Override
	public void onCreate() {
		super.onCreate();
		

		

		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		StatusPrinter.print(lc);
		
		//SystemControl.InsertAPN(this, "");
		
        FontsOverride.setDefaultFont(this, "DEFAULT", "interstateregular.ttf");
        FontsOverride.setDefaultFont(this, "MONOSPACE", "interstateregular.ttf");
        FontsOverride.setDefaultFont(this, "SERIF", "interstateregular.ttf");
        FontsOverride.setDefaultFont(this, "SANS_SERIF", "interstateregular.ttf");
		
        if(Debug.IGNORE_HARDWARE) {
        	
        	preferences = this.getSharedPreferences(COMMON_PREFERENCES, Context.MODE_PRIVATE);
        	
        	loadPreferences();
        	
        	File f = new File("/sdcard/csg/ads/");
    	    if (!f.exists()) {
    	    	f.mkdirs();
    	    }
        	
        	return;
        }
        

        
		SslConnection.init(this);
		


      
        
        
		
		dlog.d("App starting");
		dlog.i("MANUFACTURER: " +Build.MANUFACTURER);		
		dlog.i("DEVICE: " +Build.DEVICE);
		dlog.i("MODEL: " +Build.MODEL);
		dlog.i("ID: " +Build.DISPLAY);
		dlog.i("CPU_ABI: " +Build.CPU_ABI);
		dlog.i("RADIO: " +Build.getRadioVersion());
		dlog.i("ANDROID VERSION: " +Build.VERSION.SDK_INT);
		/*
		File skmaps = new File("/sdcard/skmaps.zip");
		if ( skmaps.exists()) {
			Compression.unzip(skmaps, new File("/sdcard"));
			skmaps.renameTo(new File ("skmaps.old"));
		}
		*/
		
		SKPrepareMapTextureListener skListener = new SKPrepareMapTextureListener() {

			@Override
			public void onMapTexturesPrepared(boolean arg0) {
				
				SKMaps.getInstance().setApiKey("c9ad8a7f8cccd97a7cd5dfc09213ed7f7abbd71321be48985014a14909a67c24");
				
				SKMapsInitSettings initSettings = new SKMapsInitSettings();
				
				initSettings.setConnectivityMode(SKMaps.CONNECTIVITY_MODE_OFFLINE);
				initSettings.setMapsPath("/sdcard/SKMaps/");
				initSettings.setMapResourcesPaths("/sdcard/SKMaps/", new SKMapViewStyle("/sdcard/SKMaps/daystyle/","daystyle.json"));
				initSettings.setPreinstalledMapsPath("/sdcard/SKMaps/PreinstalledMaps/");
				

				initSettings.setMapDetailLevel(SKMapsInitSettings.SK_MAP_DETAIL_LIGHT);

				
				SKAdvisorSettings advisorSettings = new SKAdvisorSettings();
				advisorSettings.setLanguage(SKAdvisorSettings.SKAdvisorLanguage.LANGUAGE_IT);
				advisorSettings.setAdvisorConfigPath("/sdcard/SKMaps/Advisor");
				advisorSettings.setResourcePath("/sdcard/SKMaps/Advisor/Languages");
				advisorSettings.setAdvisorVoice("it");
				advisorSettings.setAdvisorType( SKAdvisorSettings.SKAdvisorType.TEXT_TO_SPEECH);
				initSettings.setAdvisorSettings(advisorSettings);
				
				SKMapViewStyle style = new SKMapViewStyle("/sdcard/skmaps/daystyle/","daystyle.json");				
				initSettings.setCurrentMapViewStyle(style);
				

				SKMaps.getInstance().initializeSKMaps(App.this, initSettings);
				
				if (!new File("/sdcard/SKMaps/Maps/v1/20150413/meta/").exists()) {
	                SKVersioningManager manager = SKVersioningManager.getInstance();
	                boolean updated = manager.updateMapsVersion(20150413);
				}
				
				
				SKPackage[] packages = SKPackageManager.getInstance().getInstalledPackages();
				if (packages==null || packages.length==0) {
					int i1 = SKPackageManager.getInstance().addOfflinePackage("/sdcard/skpackages/", "ITREG09");
					int i2 = SKPackageManager.getInstance().addOfflinePackage("/sdcard/skpackages/", "IT");
					packages = SKPackageManager.getInstance().getInstalledPackages();
				}

				
				
				String xmlurl = SKPackageManager.getInstance().getMapsXMLPathForVersion(1);
				SKPackageURLInfo skinfo = SKPackageManager.getInstance().getURLInfoForPackageWithCode("IT",false);
				String mapURL = skinfo.getMapURL();
				String nameBrowserFilesURL = skinfo.getNameBrowserFilesURL();
				
			}
			
		};


		CanManager= CANManager.get(Instance);
		
		if (new File("/sdcard/SKMaps/").exists()) { 
			skListener.onMapTexturesPrepared(false);
		} else {
			final SKPrepareMapTextureThread prepThread = new SKPrepareMapTextureThread(this, "/sdcard/SKMaps", "SKMapsFull_IT.zip", skListener);
			prepThread.start();
		
			Toast.makeText(this, "Preparazione mappe in corso", Toast.LENGTH_LONG).show();
		
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e2) {
				e2.printStackTrace();
			}
		}
		

		
		/*
		Beacon b = Beacon.newBuilder()
				.setTarga("DEMO123")
				.setAnalogVoltage(123)
				.setBatteryLevel(100)
				.setLatitude(30.4)
				.setLongitude(12.2)
				
				.build();
	
		
		byte[] msg = b.toByteArray();
		*/
		
		initPhase2();
}

	private final Handler localHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {


			DLog.D("Service: received msg id:" + msg.what);

			switch (msg.what) {

				case ObcService.MSG_SERVER_CHANGE_IP:

					break;
			}
		}
	};
	
		
private Handler searchHandler = new Handler() {
	
	 SkobblerSearch sks = new SkobblerSearch();
	
	 @Override
	 public void handleMessage(Message msg) {
		 List<SKSearchResult> list = (List<SKSearchResult>)msg.obj;
		 long parent;
		 
		 switch (msg.what) {
		 case 0:
			 sks.setHandler(this);
			 sks.SearchCity("Milano");
			 break;
		 case SkobblerSearch.MSG_FOUND_CITY : 
			 parent = list.get(0).getId();
			 sks.SearchStreet("Turati", parent);
			 break;
		 case SkobblerSearch.MSG_FOUND_STREET : 
			 parent = list.get(0).getId();
			 sks.SearchHouseNumber("38", parent);
			 break;
		 case SkobblerSearch.MSG_FOUND_HOUSENUMBER :
			 dlog.d(""+list.get(0).describeContents());
			 break;
		 }
		 
	 }
};
	
private void  initPhase2() {		
	
		//SkobblerSearch sks = new SkobblerSearch();
		//sks.preselect(new String[] {"Milano","Via Massena", "10"}, null);		
		//searchHandler.sendEmptyMessage(0);

	
		ACRA.init(this);

		PackageInfo pInfo;
		try {
			pInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS);			
			App.sw_Version = pInfo.versionName;
			App.id_Version = pInfo.versionCode;
			App.Versions.AppCode = pInfo.versionCode;
			App.Versions.AppName = pInfo.versionName;
			if (pInfo.requestedPermissions!=null) {
				for (String s : pInfo.requestedPermissions) {
					dlog.d("Uses permission: " + s);
				}
			}
				
		} catch (NameNotFoundException e1) {
			dlog.e("App package not found",e1);

		}
		
		try {
			WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			WifiInfo info = manager.getConnectionInfo();
			MacAddress = info.getMacAddress();
			dlog.d("Got MAC address: " + MacAddress);
		} catch (Exception e){
			dlog.e("Failed reading MAC address",e);
		}
		
		try {
			TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
			IMEI = telephonyManager.getDeviceId();
			SimSerialNumber =  telephonyManager.getSimSerialNumber();
			PhoneNumber = telephonyManager.getLine1Number();
			dlog.d("Got IMEI : " + IMEI);
		} catch (Exception e){
			dlog.e("Failed reading IMEI ",e);
		}
		
		File DataPath = new File(APP_DATA_PATH);
		DataPath.mkdirs();
		
		preferences = this.getSharedPreferences(COMMON_PREFERENCES, Context.MODE_PRIVATE);
		
		if (preferences.getLong(KEY_parkModeStarted,0) == 0) {
			setParkModeStarted(null);
		}
		
		loadPreferences();
		loadMockLocationConfig();
		
		ACRA.getErrorReporter().putCustomData("OBC_TARGA", App.CarPlate);
		
		if ((ParkModeStarted != null && !App.parkMode.isOn()) || (App.parkMode.isOn() && ParkModeStarted == null)) {
			ParkModeStarted = null;
			parkMode = ParkMode.PARK_OFF;
			persistInSosta();
			persistParkModeStarted();
		}
		
		initAreaPolygon();

		
		dbManager =  DbManager.getInstance(this);
		dbManager.getReadableDatabase();
		
		
		
		
		Trips corse = dbManager.getCorseDao();
		corse.sendOffline(this, null);

		
        //SMS event receiver
		smsReceiver = new SMSreceiver();
		intentFilter = new IntentFilter();
		intentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
        registerReceiver(smsReceiver, intentFilter);
		
       
        serviceConnector = new ServiceConnector(this,null);
        serviceConnector.startService();
    
        // start handler which starts pending-intent after Application-Crash
	    Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
	    
	    // TODO: enable the line below to schedule advertisement updates
	    //scheduleAdvertisementUpdate();
	    
	    File f = new File("/sdcard/csg/ads/");
	    if (!f.exists()) {
	    	f.mkdirs();
	    }
	    
	   
	    sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
	    motionDetector = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	    
	    if (motionDetector!=null) {
		    
	    	SensorEventListener listener = new SensorEventListener() {

				@Override
				public void onAccuracyChanged(Sensor arg0, int arg1) {		
					
				}
				
				private float[] pAcc;

				@Override
				public void onSensorChanged(SensorEvent event) {
					if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
						float[] acc = event.values.clone();
						if (pAcc!=null) {
							float x = pAcc[0]-acc[0];
							float y = pAcc[1]-acc[1];
							float z = pAcc[2]-acc[2];
							
							
							float module = FloatMath.sqrt(x*x+y*y+z*z);
							
							if (module>1 && App.AlarmEnabled && App.currentTripInfo==null) {
							  dlog.d("Motion alarm. Module=" + module);
							  sendAlarmSms(AlarmSmsNumber);
							}
						}
						pAcc = acc;
						
					
					}
					
				}
	    		
	    	};
		 
		    sensorManager.registerListener(listener, motionDetector, 1000000);
	    }
	   


	    AppStartupTime = new Date();
		AppScheduledReboot = new Date();

	    
	    
	}
	
	
	
	public ABase getCurrentActivity(){
          return mCurrentActivity;
    }
	
    public void setCurrentActivity(ABase mCurrentActivity){
          this.mCurrentActivity = mCurrentActivity;
    }
	
	public  void setDevice(String name, String address) {

	}

	
	public  void setCarPlate(String carPlate) {
		
		//Null or empty Plate should be ignored 
		if (carPlate==null || carPlate.isEmpty() || carPlate.equalsIgnoreCase("NULL"))
			return;
		
		
		//Plate with non alphanumeric chars should be ignored		
		if (carPlate.matches("^.*[^a-zA-Z0-9 ].*$")) 
			return;
		
		//If plate is unchanged do nothing
		if (carPlate.equals(App.CarPlate))
			return;
		
		Events.CarPlateChange(App.CarPlate,carPlate);
		
		Editor editor = preferences.edit();
		editor.putString(KEY_CarPlate, carPlate);
		editor.apply();
		loadPreferences();
	}

	
	public  void setFwVersion(String fw_version) {
		
		if (fw_version.equals(App.fw_version))
			return;
		
		Editor editor = preferences.edit();
		editor.putString(KEY_fw_version, fw_version);
		editor.apply();
		loadPreferences();
	}

	public  void setBatteryLevel( int fuel_level) {
		Editor editor = preferences.edit();
		float currVoltage=0;


		if (fuel_level==App.fuel_level || fuel_level==0)
			return;
		

		editor.putInt(KEY_fuel_level, fuel_level);
		editor.apply();
		loadPreferences();
	}

	public  void setChargingAmp( double Amp) {
		Editor editor = preferences.edit();

		if (Amp==App.chargingAmp || Amp < 0)
			return;

		editor.putLong(KEY_charging_amp, (long) Amp);
		editor.apply();
		loadPreferences();
	}
	public  void setCurrentAmp( double Amp) {
		Editor editor = preferences.edit();

		if (Amp==App.currentAmp || Amp < 0)
			return;

		editor.putLong(KEY_current_apm, (long) Amp);
		editor.apply();
		loadPreferences();
	}


	public  void setMaxAmp( double Amp) {
		Editor editor = preferences.edit();

		if (Amp==App.maxAmp || Amp < 0)
			return;

		editor.putLong(KEY_max_amp, (long) Amp);
		editor.apply();
		loadPreferences();
	}
	
	public  void setMaxVoltage( float vMax) {

		if (vMax<=80f || vMax>=85f)
			vMax=83f;


		Editor editor = preferences.edit();
		editor.putFloat(KEY_MaxVoltage, vMax);
		editor.apply();
		loadPreferences();

	}

	public  void setKm( int km) {

		if (km==App.km || km==0)
			return;

		Editor editor = preferences.edit();
		editor.putInt(KEY_Km, km);
		editor.apply();
		loadPreferences();
	}
	
		
	
	public void setDamages( String damages) {
		if (damages==null) 
			damages = "";

		Editor editor = preferences.edit();
		editor.putString(KEY_damages, damages);
		editor.apply();
		loadPreferences();
	}
	
	public void setUseExternalGps(boolean use) {

		Editor editor = preferences.edit();
		editor.putBoolean(KEY_UseExternalGPS, use);
		editor.apply();
		loadPreferences();
	}
	
	
	public ArrayList<String> getDamagesList() {
		ArrayList<String> list = new ArrayList<String>();

		if (Damages!=null && !Damages.isEmpty() && !Damages.equalsIgnoreCase("null")) {
			JSONArray jsonarray;
			
			try {
				jsonarray = new JSONArray(Damages);
			} catch (JSONException e) {
				dlog.e("Parse damages string:",e);
				return list;
			}
			
			for(int i=0; i < jsonarray.length(); i++) {
				try {
					list.add(jsonarray.getString(i));
				} catch (JSONException e) {
					dlog.e("Parse damage:",e);
				}
			}		   
		}
		
		return list;
	}
	
	
	public void setConfig(String config, String payload) {
		
		try {			
			JSONObject jo = new JSONObject(config);
			Iterator<?> keys = jo.keys();
			while (keys.hasNext()) {
				
				String key = (String)keys.next();
				String value = jo.getString(key);
				
				if (key.equalsIgnoreCase("BatteryAlarmSMSNumbers")) {
					setBatteryAlarmSmsNumbers(value);
				} else if (key.equalsIgnoreCase("DefaultCity")) {
					SaveDefaultCity(value);
				} else if (key.equalsIgnoreCase("UseExternalGPS")) {
					setUseExternalGps(jo.getBoolean(key));					
				} else if (key.equalsIgnoreCase("RadioSetup")) {
					if (payload!=null) 
						radioSetup =  RadioSetup.fromJson(payload);
					else
						radioSetup =  RadioSetup.fromJson(value);
					persistRadioSetup();
				} else if (key.equalsIgnoreCase("Watchdog")) {
					Watchdog = jo.getInt(key);
					persistWatchdog();
				} else if (key.equalsIgnoreCase("BatteryShutdownLevel")) {
					BatteryShutdownLevel = jo.getInt(key);
					persistBatteryShutdownLevel();
				} else if (key.equalsIgnoreCase("FleetId")) {
					FleetId = jo.getInt(key);
					persistFleetId();
				} else if (key.equalsIgnoreCase("ServerIP")) {
					ServerIP = jo.getInt(key);
					persistServerIP();
				};
			}
		} catch (JSONException e) {
			dlog.e("Parsing setConfig :",e);			
		}
		
	}
	
	public void setFuelCardPIN(String Pin) {
		if (Pin==null) 
			Pin = "";

		Pin = Encryption.encrypt(Pin);
		Editor editor = preferences.edit();
		editor.putString(KEY_Fuelcard_PIN, Pin);
		editor.apply();
		loadPreferences();		
	}

	public  void setNavigatorEnable( boolean enable) {
		
		if (enable==App.isNavigatorEnabled)
			return;
		
		Editor editor = preferences.edit();
		editor.putBoolean(KEY_useNavigator, enable);
		editor.apply();
		loadPreferences();
	}
	
	public void setAlarmEnable(boolean enable) {
		Editor editor = preferences.edit();
		editor.putBoolean(KEY_AlarmEnabled, enable);
		editor.apply();
		loadPreferences();		
	}

	
	public void setAlarmSmsNumber(String number) {
		Editor editor = preferences.edit();
		editor.putString(KEY_AlarmSmsNumber, number);
		editor.apply();
		loadPreferences();		
	}
	
	public void setBatteryAlarmSmsNumbers(List<String> numbers) {
		String str ="[]";
		
		if (numbers!=null) {
			JSONArray ja = new JSONArray();
			for(String number : numbers) {
				ja.put(number);
			}
			str = ja.toString();
		}
		
		setBatteryAlarmSmsNumbers(str);
	}

	
	public void setBatteryAlarmSmsNumbers(String numbers) {
		Editor editor = preferences.edit();
		editor.putString(KEY_BatteryAlarmSmsNumbers, numbers);
		editor.apply();
		loadPreferences();		
	}
	
	
	
	
	public static Date getParkModeStarted() {
		return App.ParkModeStarted;
	}
	
	// Warning!!! Calling this method results in parkMode set to OFF.
	// Do NOT change this behavior, otherwise UI will become berserk
	public  void setParkModeStarted(Date date) {
		
		Editor editor = preferences.edit();
		
		parkMode = ParkMode.PARK_OFF;
		
		long value=0;
		if (date != null) {
			
			dlog.d("Set parkmModeStart to : " + date.toString());
			value = date.getTime();
			App.ParkModeStarted = new Date(value);
			
		} else {
			
			dlog.d("Set parkmModeStart to null ");
			App.ParkModeStarted = null;
		}
		
		editor.putLong(KEY_parkModeStarted, value);
		editor.putInt(KEY_ParkMode, parkMode.toInt());
		editor.apply();
		
		dlog.d("Write " + KEY_parkModeStarted + ":" + value);
	}
	
	public boolean isStopped() {
		File f = new File(STOP_FILE);
		return f.exists();
	}
	
	public boolean isConfigMode() {
		File f = new File(CONFIG_FILE);
		if ( f.exists()  ) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean checkParkArea(double latitude, double longitude) {
		
		if (latitude==0 || longitude==0 || AreaPolygons == null) { 
			return true;
		}
		
		ArrayList<double[]> polygons = App.AreaPolygons;
		
		double[] aPolygon = null;
		for (int i=0; i<polygons.size(); i++) {
				
			aPolygon = polygons.get(i);
			if (aPolygon != null && aPolygon.length > 0 && aPolygon.length % 2 == 0) {
					
				if (GeoUtils.contains(latitude, longitude, aPolygon)) {
						
					return true;
				}
			}
		}
			
		return false;
	}
	
	public void loadPreferences() {
		//MegaCompact : 00:06:71:00:00:06
		//Mega2560 : 98:D3:31:B1:85:1F

		
		
		FuelCard_PIN = preferences.getString(KEY_Fuelcard_PIN, null);
		
		if (FuelCard_PIN !=null && FuelCard_PIN.isEmpty()) 
			FuelCard_PIN=null;
		
		if (FuelCard_PIN!=null) {
			FuelCard_PIN = Encryption.decrypt(FuelCard_PIN);
		}
		CarPlate = preferences.getString(KEY_CarPlate,"NO_PLATE");
		fw_version = preferences.getString(KEY_fw_version,"nd");
		fuel_level = preferences.getInt(KEY_fuel_level, 0);
		max_voltage = preferences.getFloat(KEY_MaxVoltage,83);
			max_voltage=(max_voltage > 85f || max_voltage < 80f ? 83f : max_voltage); //controllo bontà maxVoltage
		km = preferences.getInt(KEY_Km, 0);		
		isNavigatorEnabled =  preferences.getBoolean(KEY_useNavigator, true);
		
		Damages = preferences.getString(KEY_damages, "");
		Pulizia_int = preferences.getInt(KEY_pulizia_int, 0);
		Pulizia_ext = preferences.getInt(KEY_pulizia_ext, 0);

		currentAmp =(double) preferences.getLong(KEY_current_apm, 0);
		chargingAmp =(double) preferences.getLong(KEY_charging_amp, 0);
		maxAmp =(double) preferences.getLong(KEY_max_amp, 0);
		
		AlarmEnabled = preferences.getBoolean(KEY_AlarmEnabled, false);
		AlarmSmsNumber = preferences.getString(KEY_AlarmSmsNumber, "");
		
		UseExternalGPS = preferences.getBoolean(KEY_UseExternalGPS, false);
		
		Watchdog = preferences.getInt(KEY_Watchdog, 0);
		
		BatteryShutdownLevel = preferences.getInt(KEY_BatteryShutdownLevel, 0);

		FleetId = preferences.getInt(KEY_FleetId, 0);

		ServerIP = preferences.getInt(KEY_ServerIP, 0);
		saveLog = preferences.getBoolean(KEY_PersistLog, true);
		try {
		askClose.putInt("id",preferences.getInt(KEY_IdAskClose,0));
		askClose.putBoolean("close",preferences.getBoolean(KEY_IsAskClose,false));

			SystemControl.rebootInProgress = preferences.getLong(KEY_RebootTime, 0);
		}catch(Exception e){
			dlog.e("loadPreferences: errore in reboot time ",e);
		}
		if(!loaded)
			initSharengo();  //non proprio il massimo, da trovare una soluzione
		loaded=true;
		loadInSosta();
		loadMotoreAvviato();
		loadParkModeStarted();
		loadPinChecked();
		loadUserDrunk();
		loadReservation();
		loadCharging();
		loadBatteryAlarmSmsNumbers();
		loadRadioSetup();
		loadDefaultCity();
	}
	
	public static String getipAddress() { 
        try {
            for (Enumeration en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = (NetworkInterface) en.nextElement();
                for (Enumeration enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = (InetAddress) enumIpAddr.nextElement();
                    String ipaddress = inetAddress.getHostAddress();
                    if (!inetAddress.isLoopbackAddress() && InetAddressUtils.isIPv4Address(ipaddress)) {
                        return ipaddress;
                    }
                }
            }
        } catch (SocketException ex) {
            DLog.E("Socket exception in GetIP Address of Utilities", ex);
        }
        return null; 
	}
	
	public static int getCpuTemp() {
		
		int temp =0;
		if (Build.VERSION.SDK_INT<19) {  //TODO: Controllare con il nome dispositivo
		File file = new File("/sys/devices/virtual/thermal/thermal_zone0/temp");
		
		if (file.exists() && file.canRead()) {
			try {
				FileInputStream is = new FileInputStream(file);
				InputStreamReader ir = new InputStreamReader(is);
				BufferedReader reader = new BufferedReader(ir);
				String str = reader.readLine();
				temp = Integer.parseInt(str) / 1000;
				is.close();
				ir.close();
				reader.close();
			} catch (Exception e) {
				DLog.E("GetCpuTemp:",e);
			}
		}
		}

		return temp;
	}

	public static long getAppRunningSeconds() {
		long now = new Date().getTime();
		long boot =  App.AppStartupTime.getTime();
		
		return (now-boot)/1000;
	}
	
	public void startAreaPolygonDownload(Context ctx, Handler handler) {
		DLog.D("Start area download..");
		AreaConnector cn = new AreaConnector();
		HttpConnector http = new HttpConnector(ctx);
		http.SetHandler(handler);
		http.Execute(cn);
	}
	
	public void initAreaPolygon() {
		
		File areaFile = new File(APP_DATA_PATH,"area.json");
		
		if (!areaFile.exists()) {
			try {
				areaFile.createNewFile();
			} catch (IOException e) {
				dlog.e("Impossibile creare area.json",e);
			}
			
			try {
				AssetManager assetManager = getAssets();
				InputStream ims = assetManager.open("area.json");		
				BufferedReader reader = new BufferedReader(new InputStreamReader(ims));
				
				OutputStream oms =  new FileOutputStream(areaFile);
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(oms));
				
				String str;
				while ((str = reader.readLine())!=null) {
					writer.write(str);
				};
				writer.close(); reader.close();
				oms.close();ims.close();
			} catch (IOException e) {
				dlog.e("initAreaPolygon:",e);
			}
			
		}
		
	
		

		StringBuilder sb = new StringBuilder();
	    try {
	    	
			InputStream ims = new FileInputStream(areaFile);	
			BufferedReader reader = new BufferedReader(new InputStreamReader(ims));
			String str;
			while ((str=reader.readLine())!=null) {
				sb.append(str);
			}
		
			String json = sb.toString();
			AreaPolygonMD5 = Encryption.md5(json);
			decodeAreaPolygon(sb.toString());
			ims.close();
			reader.close();
			
		} catch (IOException e) {
			dlog.e("initAreaPolygon:",e);
		}	
	}
	
	
	private void decodeAreaPolygon(String json) {
		
		AreaPolygons = new ArrayList<double[]>();
		
		JSONArray jArray;
		try {

			jArray = new JSONArray(json);
		} catch (JSONException e) {
			dlog.e("decodeAreaPolygon : ", e);
			return;
		}
		
		

		int n = jArray.length();
				
		for (int i = 0; i < n; i++) {

			try {
				JSONObject jobj = jArray.getJSONObject(i);
				JSONArray jarr = jobj.getJSONArray("coordinates");
				int x  = jarr.length();
				
				double[] points = new double[x/3*2];
				
				int y =0;
				for (int j=0;j<x;j+=3) {
					points[y++]=jarr.getDouble(j+1);
					points[y++]=jarr.getDouble(j);
				}
				
				AreaPolygons.add(points);
				
			} catch (Exception e) {
				
			}
		}
		

	}
	
	public DbManager  getDbManager() {
		if (dbManager==null)
			dbManager =  DbManager.getInstance(this);
		
		
		return dbManager;
	}

	public static void setForegroundActivity(Activity activity) {
		if (activity!=null)
			foregroundActivity = activity.getClass().getName();
	}

	public static void setForegroundActivity(String stato) {
		if (stato!=null)
			foregroundActivity = stato;
	}
	
	public static boolean isForegroundActivity(Activity activity) {
		
		if (foregroundActivity!=null && foregroundActivity.equals(activity.getClass().getName())) {
			return true;
		} else {
			return false;
		}
	}
	
	
	long lastAlarmSms=0;
	
	public void sendAlarmSms(String number) {
		
		if (!App.AlarmEnabled || App.currentTripInfo!=null || (System.currentTimeMillis() - lastAlarmSms < (15*60*1000)))
			return;
		
		SmsManager sms = SmsManager.getDefault();
		
		String text;
		
		double lat=0;
		double lon=0;
		
		if (App.lastLocation!=null) {
			lat = App.lastLocation.getLatitude();
			lon = App.lastLocation.getLongitude();
		}
		
		text = "*ALARM: " + App.CarPlate + "  https://maps.google.com/maps?q="+lat+","+lon;
		
		dlog.d("SEND SMS: " + text);
		
		sms.sendTextMessage(AlarmSmsNumber, null, text, null, null);
		
		lastAlarmSms= System.currentTimeMillis();
		
	}
	
	public  void sendLocationSms(String number) {
		SmsManager sms = SmsManager.getDefault();
		
		String text;
		
		if (App.lastLocation!=null) 
			text = App.CarPlate + ":" + App.lastLocation.getLatitude() +","+ App.lastLocation.getLongitude();
		else
			text = App.CarPlate + ": NO GPS";
		
		sms.sendTextMessage(number, null, text, null, null);
		
	}
	
	
	public void setMobileDataEnabled( boolean enabled) {
		
	try {
		
	   final ConnectivityManager conman = (ConnectivityManager)
			   
	   getSystemService(Context.CONNECTIVITY_SERVICE);
	   
	   final Class conmanClass = Class.forName(conman.getClass().getName());
	   
	   final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
	   iConnectivityManagerField.setAccessible(true);
	   
	   final Object iConnectivityManager = iConnectivityManagerField.get(conman);
	   final Class iConnectivityManagerClass =     Class.forName(iConnectivityManager.getClass().getName());
	   final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
	   setMobileDataEnabledMethod.setAccessible(true);

	   setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);
	   
	} catch (IllegalAccessException | IllegalArgumentException
			| InvocationTargetException | ClassNotFoundException | NoSuchFieldException | NoSuchMethodException e) {

		dlog.e("Error changing mobile data state.",e);
	}
	
	}
	
	
	public boolean isAirplaneModeActive() {
		int mode=0;
		
		try {
			mode = Settings.Global.getInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON);
		} catch (SettingNotFoundException e) {
			dlog.e("Can't get airplane mode status",e);
		}
		
		return  (mode==0?false:true);
	}
	
	public void ToggleAirplaneMode() {
		int mode=0;
		
		try {
			mode = Settings.Global.getInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON);
		} catch (SettingNotFoundException e) {
			e.printStackTrace();
		}
		
		mode = 1-mode;
		
		
		Settings.Global.putInt( getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, mode);
		
		Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
		intent.putExtra("state", mode);
		sendBroadcast(intent);
		
		dlog.d("Toggled airplane mode. Current state: " + mode);
	}
	
	public boolean isOnline() {
	    ConnectivityManager cm =   (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo netInfo = cm.getActiveNetworkInfo();
	    if (netInfo != null && netInfo.isConnected()) {
	        return true;
	    }
	    return false;
	}
	
	
	public void changeRadioComponentEnabled(Context context, String type, boolean radio_component_enabled, boolean reset){     
        // now toggle airplane mode from on to off, or vice versa
        Settings.System.putInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, radio_component_enabled ? 0 : 1);

        // now change system behavior so that only one component is turned off
        // this also affects the future behavior of the system button for toggling air-plane mode. 
        // to reset it in order to maintain the system behavior, set reset to true, otherwise we lazily make sure mobile voice is always on
        Settings.System.putString(context.getContentResolver(), Settings.System.AIRPLANE_MODE_RADIOS, type); 

        // post an intent to reload so the menu button switches to the new state we set
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", radio_component_enabled ? false : true);
        context.sendBroadcast(intent);

        // revert to default system behavior or not
        if (reset){ Settings.System.putString(context.getContentResolver(), Settings.System.AIRPLANE_MODE_RADIOS, "cell,bluetooth,wifi,nfc"); }
        // if reset to default is not chosen, always enable mobile cell at  least
        // but only if NOT changing the mobile connection...
        else if (type.indexOf("cell") == 0) { Settings.System.putString(context.getContentResolver(), Settings.System.AIRPLANE_MODE_RADIOS, "cell");}
}
	
	private class SMSreceiver extends BroadcastReceiver {
		private final String TAG = this.getClass().getSimpleName();

		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle extras = intent.getExtras();

			String strMessage = "";

			if (extras != null) {
				Object[] smsextras = (Object[]) extras.get("pdus");

				for (int i = 0; i < smsextras.length; i++) {
					SmsMessage smsmsg = SmsMessage
							.createFromPdu((byte[]) smsextras[i]);

					String strMsgBody = smsmsg.getMessageBody();
					String strMsgSrc = smsmsg.getOriginatingAddress();

					strMessage += "SMS from " + strMsgSrc + " : " + strMsgBody;

					if (strMsgBody.equals("PodPal14")) {
						Intent intentDiag = new Intent(App.this,ServiceTestActivity.class);
						intentDiag.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						App.this.startActivity(intentDiag);
					} else if (strMsgBody.equals("PodPal14-xy")) {
						sendLocationSms(strMsgSrc); 

					} else if (strMsgBody.startsWith("alarm on")) {
						setAlarmEnable(true);
						setAlarmSmsNumber(strMsgSrc);
						lastAlarmSms=0;
						dlog.d("Alarm enabled for : " + strMsgSrc );
					} else if (strMsgBody.startsWith("alarm off")) {
						setAlarmEnable(false);
						dlog.d("Alarm disabled by :" + strMsgSrc);
					}
					dlog.i(strMessage);
				}

			}

		}

	}
	
	private void scheduleAdvertisementUpdate() {
		
		AlarmManager alarmMgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
	    PendingIntent alarmIntent;
	    
	    Intent intent = new Intent(this, AdvertisementService.class);
	    alarmIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

	    int randomnessTime = (int)(Math.random()*AdvertisementService.ADVERTISEMENT_UPDATE_RANGE);
	    Log.e(App.class.getCanonicalName(), "App: Advertisement random time " + randomnessTime);
	    
	    // Set the alarm to start at 0:00 a.m plus some randomness
	    GregorianCalendar calendar = (GregorianCalendar)GregorianCalendar.getInstance();
	    calendar.setTimeInMillis(System.currentTimeMillis());
	    calendar.set(GregorianCalendar.HOUR_OF_DAY, 0 + randomnessTime/60);
	    calendar.set(GregorianCalendar.MINUTE, 0 + randomnessTime%60);

	    DLog.W("App: Advertisement alarm scheduled at " + randomnessTime/60 + ":" + randomnessTime%60 + " a.m.");
	    
	    // Schedule the alarm
	    alarmMgr.setInexactRepeating(AlarmManager.RTC, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, alarmIntent);
	    
	    long currentTimestamp = System.currentTimeMillis();
	    long lastTimeAdsDownloaded = preferences.getLong(KEY_LastAdvertisementListDownloaded, 0);
	    
	    if ((currentTimestamp - lastTimeAdsDownloaded) >= AdvertisementService.ADVERTISEMENT_UPDATE_PERIOD) {
	    	
	    	Log.e(App.class.getCanonicalName(), "App: going to update advertisement right now");
	    	
	    	Intent advertisementIntent = new Intent(this, AdvertisementService.class);
	    	advertisementIntent.setAction(AdvertisementService.ACTION_UPDATE_ADVERTISEMENT);
	    	advertisementIntent.putExtra(AdvertisementService.PARAM_AUTENTICATION_ID, "YY999YY");
	    	startService(advertisementIntent);
	    } else {
	    	Log.e(App.class.getCanonicalName(), "App: no advertisement update required");
	    }
	}
}
