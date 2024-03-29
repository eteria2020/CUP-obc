package eu.philcar.csg.OBC.service;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.util.JsonWriter;

import com.google.gson.Gson;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.BuildConfig;
import eu.philcar.csg.OBC.data.datasources.repositories.EventRepository;
import eu.philcar.csg.OBC.data.model.Beacon;
import eu.philcar.csg.OBC.db.Trip;
import eu.philcar.csg.OBC.db.Trips;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.UrlTools;
import eu.philcar.csg.OBC.task.OdoController;
import eu.philcar.csg.OBC.task.OptimizeDistanceCalc;

public class CarInfo {

	private DLog dlog = new DLog(this.getClass());
//	public static CarInfo Shared ;

	@Inject
	EventRepository eventRepository;

	@Inject
	GPSController gpsController;

	private Handler serviceHandler;

	private String id = "";

//	public String VIN = "";

	private int speed = 0;

	public int batteryLevel = App.fuel_level;
	public int bmsSOC = App.fuel_level;
	public int bmsSOC_GPRS = App.fuel_level;
	public float SOCR = App.fuel_level, virtualSOC = App.fuel_level;
	private int km = 0;
	private float voltage = 0;
	private int isKeyOn = 0;
	private static String keyStatus = "OFF";
	private String gear = "";
	private boolean batterySafety = false;
	private String fakeCard = "00000000";
	private String fakePin = "0000";
	private boolean lastBatterySafety = batterySafety;
	private Date lastBatterySafetyTx = new Date();
	private int outAmp = 0;
	private Long timestampCurrent = null;
	private Long lastForceReady = System.currentTimeMillis();

	public float[] cellVoltageValue = new float[24];
	float minVoltage = 2.6f;
	public String batteryType = "", lowCells = "";
	public Boolean isCellLowVoltage = false;
//	public Boolean Charging = false;
	public float currVoltage = App.max_voltage;

	private static boolean ready = false;
	private boolean brakes = false;
	private boolean chargingPlug = false;
	public double currentAmpere = App.currentAmp;
	public double chargingAmpere = App.chargingAmp;
	public double maxAmpere = App.maxAmp;

	private String fw_version = "";
	private String sdk_version = "";
	private String versions = "";
	private static Boolean sent = false;

	public Location location = new Location("");

	public Location intGpsLocation = new Location(LocationManager.GPS_PROVIDER);

	Location extGpsLocation = new Location(LocationManager.GPS_PROVIDER) {
		@Override
		public void setLatitude(double latitude) {
			super.setLatitude(latitude);
			beacon.setExt_lat(gpsRound(latitude));
		}

		@Override
		public void setLongitude(double longitude) {
			super.setLongitude(longitude);
			beacon.setExt_lon(gpsRound(longitude));
		}

		@Override
		public void setTime(long time) {
			super.setTime(time);
			beacon.setExt_time(time);
		}
	};
//	public Location ntwkLocation = new Location(LocationManager.NETWORK_PROVIDER);

	private double longitude = 0;
	private double latitude = 0;
//	private double accuracy = 0;

	private long lastLocationChange = 0;
	private long lastUpdate = 0;

	private long tripsOpened = 0;
	private long tripsToSend = 0;

	private int rangeKm = (App.fuel_level <= 50 ? Math.max(App.fuel_level - 10, 0) : App.fuel_level);

	private final Bundle allData;

	private Beacon beacon;

	ServiceLocationListener serviceLocationListener;

	private SimpleDateFormat log_sdf = new SimpleDateFormat("dd/MM/yyyy HH.mm.ss", Locale.ITALY);

	private boolean checkLastBatterySafety(boolean newBatterySafety) {
		return lastBatterySafetyTx.getTime() + 1000 * 60 * 10 <= System.currentTimeMillis();
	}

	private void setLastBatterySafety(boolean lastBatterySafety) {
		this.lastBatterySafety = lastBatterySafety;
		lastBatterySafetyTx.setTime(System.currentTimeMillis());
	}

	public CarInfo(Handler handler) {
		App.Instance.getComponent().inject(this);
//		Shared = this;
		serviceHandler = handler;
		serviceLocationListener = new ServiceLocationListener();
		allData = new Bundle();
		beacon = new Beacon();
	}

	private String getFakePin() {
		return fakePin;
	}

	private boolean setFakePin(String fakePin) {
		this.fakePin = fakePin;
		return !this.fakePin.equalsIgnoreCase("0000");
	}

	private String getFakeCard() {
		return fakeCard;
	}

	private boolean setFakeCard(String fakeCard) {
		this.fakeCard = fakeCard;
		return !this.fakeCard.equalsIgnoreCase("00000000");
	}

	public boolean isBatterySafety() {

		return batterySafety;
	}

	private void setBatterySafety(boolean newBatterySafety) {
		if (newBatterySafety != batterySafety) {
			if (newBatterySafety == lastBatterySafety) {
				if (checkLastBatterySafety(newBatterySafety)) {
					dlog.d("CarInfo.setBatterySafety();set new BS to" + newBatterySafety);
					batterySafety = newBatterySafety;
					beacon.setBatterySafety(batterySafety);
				} else {
					dlog.d("CarInfo.setBatterySafety();wait to set new battery safety lastBStime" + lastBatterySafetyTx.toString());
				}
			} else {
				setLastBatterySafety(newBatterySafety);
			}
		}

		batterySafety = newBatterySafety;
		beacon.setBatterySafety(batterySafety);
	}

	void setBatteryLevel(int batteryLevel) {

        /*if(this.batteryLevel<0){
			this.batteryLevel=batteryLevel;
            return;
        }*/
		if (batteryLevel > 100)
			batteryLevel = 100;
		dlog.i("CarInfo.setBatteryLevel();first " + this.batteryLevel + " target: " + batteryLevel);

		if (this.batteryLevel - batteryLevel >= 5) {
			this.batteryLevel = (this.batteryLevel > batteryLevel ? this.batteryLevel - 5 : this.batteryLevel + 5);
		} else
			this.batteryLevel = (batteryLevel);

		if (BuildConfig.BUILD_TYPE.equals("debug")){
			this.batteryLevel = 99;//FOR DEVELOP PURPOSE
		}

		App.Instance.setBatteryLevel(this.batteryLevel);
		beacon.setSOC(this.batteryLevel);
		dlog.i("CarInfo.setBatteryLevel();result " + this.batteryLevel);
	}

	private void setLocation(Location loc) {

		if (App.mockLocation != null) {
			location = App.mockLocation;
//			long a = loc.getTime();
			setLongitude(App.mockLocation.getLongitude());
			setLatitude(App.mockLocation.getLatitude());
//			accuracy = App.mockLocation.getAccuracy();
			App.setLastLocation(App.mockLocation);
			return;
		}

		if (loc != null) {

			location.set(loc);
//			long a = loc.getTime();
//			String time = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS").format(location.getTime());
			if (getLongitude() != loc.getLongitude() || getLatitude() != loc.getLatitude()) {
				lastLocationChange = System.nanoTime();
				setLongitude(loc.getLongitude());
				setLatitude(loc.getLatitude());
//				accuracy = loc.getAccuracy();
			}

			if (serviceHandler != null)
				serviceHandler.sendMessage(MessageFactory.sendLocationChange(location));

			App.setLastLocation(location);
			gpsController.onNewLocation(location);
		}

	}

	@Deprecated
	public boolean handleUpdate(Bundle b) {
		int i;
		boolean forceBeacon = false;
//		List<NameValuePair> paramsList = new ArrayList<NameValuePair>();

		//Update all data in local bundle
        /*if(b.containsKey("SOC"))
            b.putInt("SOC",batteryLevel); //replace the value read from the can with the calculated one*/
		allData.putAll(b);

		//Scan receive budle element to update relevant local values and events

		for (String key : b.keySet()) {

			if (key.equalsIgnoreCase("keyOn")) {
				int k = b.getBoolean(key) ? 1 : 0;
				if (k != getIsKeyOn()) {
					setIsKeyOn(k);
					eventRepository.eventKey(k);

				}
			}

			if (key.equalsIgnoreCase("PackAmp")) {

				int k = b.getInt(key);
				setOutAmp(k);

			}

			if (key.equalsIgnoreCase("timestampAmp")) {

				setTimestampCurrent(b.getLong(key));

			} else if (key.equalsIgnoreCase("SOC")) {
				//batteryLevel = b.getInt(key);
				bmsSOC_GPRS = b.getInt(key);
				dlog.d("CarInfo.handleUpdate();" + b.get(key));
				if (batteryLevel > 20)
					sent = false;
				if (batteryLevel <= 15 && batteryLevel > 0 && !sent && sendMail("http://manage.sharengo.it/mailObcLowBattery.php")) {
					eventRepository.eventSoc(batteryLevel, "Email");
					sent = true;
				}
				App.Instance.setBatteryLevel(batteryLevel);
				if (batteryLevel <= 50)
					rangeKm = Math.max(batteryLevel - 10, 0);
				else if (batteryLevel <= 60)
					rangeKm = batteryLevel;
				else
					rangeKm = batteryLevel;
			} else if (key.equalsIgnoreCase("Speed")) {
				setSpeed(Math.round(b.getFloat(key)));
			}
/*			
			else if (key.equalsIgnoreCase("RPM")) {
				i =b.getInt(key);
					Eventi.eventEngine(1);
				if (rpm>0  && i==0)
					Eventi.eventEngine(0);
				rpm=i;
			}
*/
			else if (key.equalsIgnoreCase("Km")) {
				i = b.getInt(key);
				if (i >= 0)
					App.Instance.setKm(i);
				setKm(i);
			} else if (key.equalsIgnoreCase("MotV")) {
				setVoltage(b.getFloat(key));
			} else if (key.equalsIgnoreCase("VIN")) {
				setId(b.getString(key));
				eventRepository.CarPlateChange(App.CarPlate, getId());
				App.Instance.setCarPlate(getId());
			} else if (key.equalsIgnoreCase("VER")) {
				App.Instance.setFwVersion(b.getString(key));
			} else if (key.equalsIgnoreCase("keyStatus")) {
				this.setKeyStatus(b.getString(key));
			} else if (key.equalsIgnoreCase("GearStatus")) {
				String str = b.getString(key);
				if (!this.getGear().equals(str)) {
					this.setGear(str);
					eventRepository.eventGear(str);
				}
			} else if (key.equalsIgnoreCase("PPStatus")) {
				boolean v = b.getBoolean(key);
				if (v != this.isChargingPlug()) {
					this.setChargingPlug(v);
					eventRepository.eventCharge(v ? 1 : 0);
				}

				if (!App.isCharging() && this.isChargingPlug()) {
					App.setCharging(true);
					serviceHandler.sendMessage(MessageFactory.notifyStartCharging(this));
					App.Instance.persistCharging();
					forceBeacon = true;
				}

			} else if (key.equalsIgnoreCase("ReadyOn")) {
				boolean v = b.getBoolean(key);
				if (v != isReady()) {
					this.setReady(v);
					eventRepository.eventReady(v ? 1 : 0);
				}
			} else if (key.equalsIgnoreCase("BrakesOn")) {
				this.setBrakes(b.getBoolean(key));
			} else if (key.equalsIgnoreCase("SDKVer")) {
				this.setSdk_version(b.getString(key));
			} else if (key.equalsIgnoreCase("Versions")) {
				this.versions = b.getString(key);
			} else if (key.equalsIgnoreCase("GPSBOX")) {
				String j = b.getString(key);
				if (j != null && !j.isEmpty()) {
					JSONObject jo;
					try {
						jo = new JSONObject(j);

						extGpsLocation.setLatitude(jo.getDouble("lat"));
						extGpsLocation.setLongitude(jo.getDouble("lon"));
						extGpsLocation.setAccuracy((float) jo.getDouble("acc"));
						extGpsLocation.setSpeed((float) jo.getDouble("spd"));
						extGpsLocation.setTime(jo.getLong("ts"));
						extGpsLocation.setBearing((float) jo.getDouble("head"));

						if (App.UseExternalGPS) {
							setLocation(extGpsLocation);
						}

					} catch (JSONException e) {
						DLog.E("CarInfo.handleUpdate();Parsing GPSBOX:", e);
					}
				} else {
					DLog.W("CarInfo.handleUpdate();Empty GPSBOX json");
				}

			}
/*
			else if (key.equalsIgnoreCase("CellInfo")) {
//				Bundle c = b.getBundle(key);
				//this.cellVoltageValue[c.getInt("CellIndex")]=c.getFloat("CellVoltage");
			}
*/

			/*else if (key.equalsIgnoreCase("CellsInfo")) {

				//this.cellVoltageValue=b.getFloatArray(key);

			}*/
		}

		return forceBeacon;
	}

	Bundle betterHandleUpdate(Bundle b, ObcService service) {
		int i;
		float f;
		Long l;
		String s;
		boolean bo;
		boolean forceBeacon = false;
		boolean hasChanged = false;
		Bundle res = new Bundle();

		//Update all data in local bundle
		/*if(b.containsKey("SOC"))
			b.putInt("SOC",batteryLevel); //replace the value read from the can with the calculated one*/
		allData.putAll(b);
		beacon = Beacon.handleUpdate(beacon, b);
		//Scan receive budle element to update relevant local values and events

		for (String key : b.keySet()) {

			if (key.equalsIgnoreCase("keyOn")) {
				i = b.getBoolean(key) ? 1 : 0;
				if (i != getIsKeyOn()) {
					hasChanged = true;
					setIsKeyOn(i);
					eventRepository.eventKey(i);

					if (i == 1 && App.currentTripInfo != null && App.currentTripInfo.isOpen && App.pinChecked && App.getParkModeStarted() == null && !App.isClosing && System.currentTimeMillis() - lastForceReady > 5000) {        //Ask Rosco if good idea
						lastForceReady = System.currentTimeMillis();
						serviceHandler.sendMessage(MessageFactory.setEngine(true));
					}

				}
			} else if (key.equalsIgnoreCase("PackAmp")) {

				i = b.getInt(key);
				if (i != getOutAmp()) {
					hasChanged = true;
					setOutAmp(i);
				}

			} else if (key.equalsIgnoreCase("batterySafety")) {

				bo = b.getBoolean(key);
				if (bo != isBatterySafety()) {
					hasChanged = true;
					setBatterySafety(bo);
				}

			} else if (key.equalsIgnoreCase("fakeCard")) {

				s = b.getString(key);

				if (s != null && !s.equalsIgnoreCase(getFakeCard())) {
					hasChanged = true;
					if (setFakeCard(s) && App.currentTripInfo == null) {
						dlog.d("CarInfo.betterHandleUpdate();received fakeCard code" + s);
						service.notifyCard(s, "OPEN", false, false);
					}
				}

			}else if (key.equalsIgnoreCase("fakePin")) {

				s = b.getString(key);

				if (s != null && !s.equalsIgnoreCase(getFakePin())) {
					hasChanged = true;
					if (setFakePin(s) && App.currentTripInfo != null) {
						eventRepository.eventPin(getFakePin());
//						service.notifyCard(s, "OPEN", false, false);
						service.sendAll(MessageFactory.checkPin(getFakePin()));
//						service.sendMessage(MessageFactory.restartUI());
					}
				}

			} else if (key.equalsIgnoreCase("timestampAmp")) {
				l = b.getLong(key);
				if (!l.equals(getTimestampCurrent()) ) {
					hasChanged = true;
					setTimestampCurrent(l);
				}

			} else if (key.equalsIgnoreCase("SOC")) {

				i = b.getInt(key);
				if (i != bmsSOC_GPRS) {
					hasChanged = true;
					//batteryLevel = b.getInt(key);
					bmsSOC_GPRS = i;
					dlog.d("CarInfo.betterHandleUpdate();UpdateBmsSoc_GPRS: " + i);
                    /*if (batteryLevel > 20)
                        sent = false;
                    if (batteryLevel <= 15 && batteryLevel > 0 && !sent && sendMail("http://manage.sharengo.it/mailObcLowBattery.php")) {
                        Events.eventSoc(batteryLevel, "Email");
                        sent = true;
                    }*/
					if (batteryLevel <= 50)
						rangeKm = Math.max(batteryLevel - 10, 0);
					else if (batteryLevel <= 60)
						rangeKm = batteryLevel;
					else
						rangeKm = batteryLevel;
				}
			} else if (key.equalsIgnoreCase("Speed")) {
				i = Math.round(b.getFloat(key));
				if (i != getSpeed()) {
					hasChanged = true;
					setSpeed(i);
				}
			}
/*
			else if (key.equalsIgnoreCase("RPM")) {
				i =b.getInt(key);
					Eventi.eventEngine(1);
				if (rpm>0  && i==0)
					Eventi.eventEngine(0);
				rpm=i;
			}
*/
			else if (key.equalsIgnoreCase("Km")) {
				i = b.getInt(key);
				if (i >= 0 && i != getKm()) {
					hasChanged = true;
					App.Instance.setKm(i);
					setKm(i);
				}
			} else if (key.equalsIgnoreCase("MotV")) {
				f = b.getFloat(key);
				if (f != getVoltage()) {
					hasChanged = true;
					setVoltage(b.getFloat(key));
				}
			} else if (key.equalsIgnoreCase("VIN")) {
				s = b.getString(key);
				if (s!=null && !s.equals(App.CarPlate)) {
					hasChanged = true;
					setId(s);
					eventRepository.CarPlateChange(App.CarPlate, getId());
					App.Instance.setCarPlate(getId());
					if (App.canRestartZMQ) {
						App.canRestartZMQ = false;
						serviceHandler.sendMessage(MessageFactory.zmqRestart());
						dlog.d("CarInfo.betterHandleUpdate();new vin restarting ZMQ");
					}
				}
			} else if (key.equalsIgnoreCase("VER")) {
				s = b.getString(key);
				if (s!=null && !s.equals(App.fw_version)) {
					hasChanged = true;
					setFw_version(s);
					App.Instance.setFwVersion(s);
				}
			} else if (key.equalsIgnoreCase("keyStatus")) {
				s = b.getString(key);
				if (s!=null && !s.equals(getKeyStatus())) {
					hasChanged = true;
					setKeyStatus(b.getString(key));
				}
			} else if (key.equalsIgnoreCase("GearStatus")) {
				s = b.getString(key);
				if (!this.getGear().equals(s)) {
					hasChanged = true;
					setGear(s);
					eventRepository.eventGear(s);
				}
			} else if (key.equalsIgnoreCase("PPStatus")) {
				bo = b.getBoolean(key);
				if (bo != this.isChargingPlug()) {
					this.setChargingPlug(bo);
					eventRepository.eventCharge(bo ? 1 : 0);
					forceBeacon = true;

				}
				if (!App.isCharging() && this.isChargingPlug()) {

					App.setCharging(true);
					serviceHandler.sendMessage(MessageFactory.notifyStartCharging(this));
					App.Instance.persistCharging();
					forceBeacon = true;
				}

			} else if (key.equalsIgnoreCase("ReadyOn")) {
				bo = b.getBoolean(key);

				if (bo != ready) {
					hasChanged = true;
					this.setReady(bo);
					eventRepository.eventReady(bo ? 1 : 0);
				}
			} else if (key.equalsIgnoreCase("BrakesOn")) {
				bo = b.getBoolean(key);
				if (bo != ready) {
					hasChanged = true;
					this.setBrakes(bo);
				}
			} else if (key.equalsIgnoreCase("SDKVer")) {
				s = b.getString(key);
				if (s!=null && !s.equals(this.getSdk_version())) {
					hasChanged = true;
					this.setSdk_version(s);
				}
			} else if (key.equalsIgnoreCase("Versions")) {
				s = b.getString(key);
				if (s!=null && !s.equals(this.versions)) {
					hasChanged = true;
					this.versions = s;
				}
			} else if (key.equalsIgnoreCase("GPSBOX")) {
				String j = b.getString(key);
				if (j != null && !j.isEmpty()) {
					JSONObject jo;
					try {
						jo = new JSONObject(j);

						extGpsLocation.setLatitude(jo.getDouble("lat"));
						extGpsLocation.setLongitude(jo.getDouble("lon"));
						extGpsLocation.setAccuracy((float) jo.getDouble("acc"));
						extGpsLocation.setSpeed((float) jo.getDouble("spd"));
						extGpsLocation.setTime(jo.getLong("ts"));
						extGpsLocation.setBearing((float) jo.getDouble("head"));

						if (App.UseExternalGPS) {
							hasChanged = true;
							setLocation(extGpsLocation);
						}

					} catch (JSONException e) {
						DLog.E("CarInfo.betterHandleUpdate();Parsing GPSBOX:", e);
					}
				} else {
					DLog.W("CarInfo.betterHandleUpdate();Empty GPSBOX json");
				}

			}

/*
			else if (key.equalsIgnoreCase("CellInfo")) {
				//Bundle c=b.getBundle(key);
				//this.cellVoltageValue[c.getInt("CellIndex")]=c.getFloat("CellVoltage");

			} else if (key.equalsIgnoreCase("CellsInfo")) {
				//this.cellVoltageValue=b.getFloatArray(key);
			}
*/
		}
		if (System.currentTimeMillis() - lastUpdate > 12000) {
			lastUpdate = System.currentTimeMillis();
			hasChanged = true;
		}
		res.putBoolean("changed", hasChanged);
		res.putBoolean("force", forceBeacon);

		return res;
	}

	private Boolean sendMail(String Url) {

		if (!App.hasNetworkConnection()) {
			dlog.e("CarInfo.sendMail();no network connection");
			return false;
		}
		StringBuilder builder = new StringBuilder();
		List<NameValuePair> paramsList = new ArrayList<>();
		HttpResponse response = null;

		HttpClient client = new DefaultHttpClient();
		try {
			if (App.currentTripInfo != null && App.currentTripInfo.customer != null && App.currentTripInfo.isOpen) {
				paramsList.add(new BasicNameValuePair("soc", batteryLevel + ""));
				paramsList.add(new BasicNameValuePair("plate", App.CarPlate + ""));
				paramsList.add(new BasicNameValuePair("name", App.currentTripInfo.customer.name));
				paramsList.add(new BasicNameValuePair("surname", App.currentTripInfo.customer.surname));
				paramsList.add(new BasicNameValuePair("tel", App.currentTripInfo.customer.mobile));
				paramsList.add(new BasicNameValuePair("id", App.currentTripInfo.trip.remote_id + ""));
				Url = UrlTools.buildQuery(Url.concat("?"), paramsList).toString();
			}

			HttpGet httpGet = new HttpGet(Url);

			response = client.execute(httpGet);
			DLog.D("CarInfo.sendMail();url: " + Url);
			StatusLine statusLine = response.getStatusLine();
			int statusCode = statusLine.getStatusCode();
			if (statusCode == 200) {
				//App.update_StartImages = new Date();
				HttpEntity entity = response.getEntity();
				InputStream content = entity.getContent();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(content));
				String line;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}
				reader.close();
				content.close();
			} else {

				dlog.e("CarInfo.sendMail();Failed to connect " + statusCode);
				return false;
			}
		} catch (Exception e) {
			dlog.e("CarInfo.sendMail();connection exception ", e);
			return false;
		} finally {
			client.getConnectionManager().shutdown();
		}

		String jsonStr = builder.toString();
		if (jsonStr.compareTo("0") == 0) {
			dlog.e("CarInfo.sendMail();send fail");

			return false;
		} else if (jsonStr.compareTo("1") == 0) {
			dlog.d("CarInfo.sendMail();send successful");

			return true;
		}

		DLog.D("CarInfo.sendMail();response:" + jsonStr);
		return false;
	}

	void updateTrips() {
		Trips trips = App.Instance.getDbManager().getTripDao();
		if (trips != null) {
			tripsToSend = trips.getNTripsToSend();

			List<Trip> list = trips.getOpenTrips();
			if (list != null) {
				tripsOpened = list.size();
			}
		}
	}

	private String getGpsJson() {

		if (location == null)
			return "";

		StringWriter sw = new StringWriter();
		JsonWriter jw = new JsonWriter(sw);

		try {
			jw.beginObject();

			String timestamp = (String) android.text.format.DateFormat.format("dd/MM/yyyy kk:mm:ss", new java.util.Date());
			jw.name("time").value(timestamp);

			jw.name("accuracy").value(location.getAccuracy());

			long age = (SystemClock.elapsedRealtimeNanos() - location.getElapsedRealtimeNanos()) / 1000000000;
			jw.name("fix_age").value(age);

			age = (System.nanoTime() - lastLocationChange) / 1000000000;
			jw.name("change_age").value(age);

			Bundle b = location.getExtras();
			int sats = 0;
			if (b != null)
				sats = b.getInt("satellites");

			jw.name("satellites").value(sats);

			jw.endObject();
			jw.close();
		} catch (IOException e) {
			DLog.E("CarInfo.getGpsJson();Error creating  gps json:", e);
		}

		return sw.toString();
	}

	private void appendJson(JsonWriter jw, String key, Object value) {
		try {

			if (value instanceof Boolean) {
				jw.name(key).value((boolean) value);
			} else if (value instanceof Integer) {
				jw.name(key).value((int) value);
			} else if (value instanceof Float) {
				jw.name(key).value((float) value);
			} else if (value instanceof String) {
				jw.name(key).value((String) value);
			}

		} catch (IOException e) {
			dlog.e("CarInfo.appendJson();Exception while append json", e);
		}

	}

/*	private void appendJsonGson(com.google.gson.stream.JsonWriter jw, String key, Object value) {
		try {

			if (value instanceof Boolean) {
				jw.name(key).value((boolean) value);
			} else if (value instanceof Integer) {
				jw.name(key).value((int) value);
			} else if (value instanceof Float) {
				jw.name(key).value((float) value);
			} else if (value instanceof String) {
				jw.name(key).value((String) value);
			}

		} catch (IOException e) {
			dlog.e("CarInfo.appendJson();Exception while append json", e);
		}

	}*/

	private double gpsRound(double value) {
		return Math.round(value * 100000D) / 100000D;
	}

	String getJson(boolean longVersion) {
		StringWriter sw = new StringWriter();
		JsonWriter jw = new JsonWriter(sw);
		//Per chiudere da remoto una corsa (da app utente finale) dobbiamo essere nell'ultima schermata, essere in area accettabile e non in sosta.
		boolean enableRemoteClose = App.isIsCloseable() && App.checkParkArea(getLatitude(), getLongitude()) && (App.getParkModeStarted() == null);

		try {
			jw.beginObject();

			for (String key : allData.keySet()) {
				if (!key.equals("GPSBOX") ) {
					if (key.equals("SOC")) {
						appendJson(jw, key, batteryLevel);
					} else
						appendJson(jw, key, allData.get(key));
				}
			}

			if (App.mockLocation != null) {
				jw.name("lon").value(gpsRound(App.mockLocation.getLongitude()));
				jw.name("lat").value(gpsRound(App.mockLocation.getLatitude()));
			} else {
				jw.name("lon").value(gpsRound(getLongitude()));
				jw.name("lat").value(gpsRound(getLatitude()));
			}

			jw.name("GPS").value(App.UseExternalGPS ? "EXT" : "INT");
			jw.name("SOC").value(batteryLevel);
			jw.name("km").value(App.km);
			//jw.name("batterySafety").value(batterySafety);
			jw.name("cputemp").value(App.getCpuTemp());

			jw.name("on_trip").value(App.currentTripInfo != null ? 1 : 0);

			jw.name("closeEnabled").value(enableRemoteClose);
			jw.name("parkEnabled").value(!App.motoreAvviato && App.getParkModeStarted() != null);
			jw.name("parking").value(App.getParkModeStarted() != null && App.parkMode.isOn());
			jw.name("charging").value(App.isCharging());
			jw.name("noGPS").value(App.getNoGPSAlarm());
			jw.name("PPStatus").value(this.isChargingPlug());
			if (App.currentTripInfo != null && App.currentTripInfo.trip != null) {
				jw.name("id_trip").value(App.currentTripInfo.trip.remote_id);
			}

			if (intGpsLocation != null) {
				jw.name("int_lon").value(gpsRound(intGpsLocation.getLongitude()));
				jw.name("int_lat").value(gpsRound(intGpsLocation.getLatitude()));
				jw.name("int_time").value(intGpsLocation.getTime());
			}

			if (extGpsLocation != null) {
				jw.name("ext_lon").value(gpsRound(extGpsLocation.getLongitude()));
				jw.name("ext_lat").value(gpsRound(extGpsLocation.getLatitude()));
				jw.name("ext_time").value(extGpsLocation.getTime());
			}

			jw.name("log_tx_time").value(log_sdf.format(new Date()));

			if (longVersion) {
				updateTrips();

				jw.name("fwVer").value(getFw_version().equals("") ? App.fw_version : getFw_version());
				jw.name("swVer").value(App.sw_Version);
				jw.name("sdkVer").value(getSdk_version());
				jw.name("hwVer").value(android.os.Build.MODEL + " " + android.os.Build.DEVICE + " " + android.os.Build.ID);
				jw.name("gsmVer").value(android.os.Build.getRadioVersion());
				jw.name("clock").value(DateFormat.format("dd/MM/yyyy kk:mm:ss", new Date()).toString());

				jw.name("IMEI").value(App.IMEI);
				jw.name("SIM_SN").value(App.SimSerialNumber);

				jw.name("wlsize").value(App.whiteListSize);

				jw.name("offLineTrips").value(tripsToSend);
				jw.name("openTrips").value(tripsOpened);

				if (App.mockLocation == null) {
					String gpsInfo = getGpsJson();
					jw.name("gps_info").value(gpsInfo);
				}

				jw.name("Versions").value(App.Versions.toJson());

				if (allData.containsKey("GPSBOX"))
					jw.name("GPSBOX").value(allData.getString("GPSBOX"));

			}

			jw.endObject();

			jw.close();
		} catch (IOException e) {
			DLog.E("CarInfo.getJson();Error creating json:", e);
		}

//		String jsonStr = sw.toString();
		return sw.toString();
	}

	String getJsonGson(boolean longVersion) {
		Gson gson = new Gson();
		try {

			//Per chiudere da remoto una corsa (da app utente finale) dobbiamo essere nell'ultima schermata, essere in area accettabile e non in sosta.
			boolean enableRemoteClose = App.isIsCloseable() && App.checkParkArea(getLatitude(), getLongitude()) && (App.getParkModeStarted() == null);

			try {
				beacon.setGPS(App.UseExternalGPS ? "EXT" : "INT");
				//beacon.setSOC(batteryLevel);
				//jw.name("batterySafety").value(batterySafety);
				//jw.name("cputemp").value(App.getCpuTemp());

				beacon.setOn_trip(App.currentTripInfo != null ? 1 : 0);

				beacon.setCloseEnabled(enableRemoteClose);
				beacon.setParkEnabled(!App.motoreAvviato && App.getParkModeStarted() != null);
				beacon.setParking(App.getParkModeStarted() != null && App.parkMode.isOn());
				beacon.setCharging(App.isCharging());
				beacon.setNoGPS(App.getNoGPSAlarm());

				if (App.currentTripInfo != null && App.currentTripInfo.trip != null) {
					beacon.setId_trip(App.currentTripInfo.trip.remote_id);
				}

				if (intGpsLocation != null) {
					beacon.setInt_lon(gpsRound(intGpsLocation.getLongitude()));
					beacon.setInt_lat(gpsRound(intGpsLocation.getLatitude()));
					beacon.setInt_time(intGpsLocation.getTime());
				}

				beacon.setLog_tx_time(log_sdf.format(new Date()));

				if (longVersion) {
					updateTrips();

					beacon.setFwVer(App.fw_version);
					beacon.setSwVer(App.sw_Version);
					//beacon.setSdkVer(getSdk_version());
					beacon.setHwVer(android.os.Build.MODEL + " " + android.os.Build.DEVICE + " " + android.os.Build.ID);
					beacon.setGsmVer(android.os.Build.getRadioVersion());
					beacon.setClock(DateFormat.format("dd/MM/yyyy kk:mm:ss", new Date()).toString());

					beacon.setIMEI(App.IMEI);
					beacon.setSIM_SN(App.SimSerialNumber);

					beacon.setWlsize(App.whiteListSize);

					beacon.setOffLineTrips((int) tripsToSend);
					beacon.setOpenTrips((int) tripsOpened);

					if (App.mockLocation == null) {
						String gpsInfo = getGpsJson();
						beacon.setGps_info(gpsInfo);
					}

					beacon.setVersions(App.Versions.toJson());

					if (allData.containsKey("GPSBOX"))
						beacon.setGPSBOX(allData.getString("GPSBOX"));

				}
			} catch (Exception e) {
				DLog.E("Error creating json:", e);
			}

		} catch (Exception e) {
			dlog.e("Esception serializing beacon with gson", e);
		}
		return gson.toJson(beacon);
	}

	public String getJson_GPRS(boolean longVersion) {
		StringWriter sw = new StringWriter();
		JsonWriter jw = new JsonWriter(sw);

		//Per chiudere da remoto una corsa (da app utente finale) dobbiamo essere nell'ultima schermata, essere in area accettabile e non in sosta.
		boolean enableRemoteClose = App.isIsCloseable() && App.checkParkArea(getLatitude(), getLongitude()) && (App.getParkModeStarted() == null);

		try {
			jw.beginObject();

			for (String key : allData.keySet()) {
				if (!key.equals("GPSBOX")) {
					appendJson(jw, key, allData.get(key));
				}
			}

			if (App.mockLocation != null) {
				jw.name("lon").value(gpsRound(App.mockLocation.getLongitude()));
				jw.name("lat").value(gpsRound(App.mockLocation.getLatitude()));
			} else {
				jw.name("lon").value(gpsRound(getLongitude()));
				jw.name("lat").value(gpsRound(getLatitude()));
			}

			jw.name("GPS").value(App.UseExternalGPS ? "EXT" : "INT");
			jw.name("cputemp").value(App.getCpuTemp());
			//jw.name("batterySafety").value(batterySafety);
			jw.name("on_trip").value(App.currentTripInfo != null ? 1 : 0);

			jw.name("closeEnabled").value(enableRemoteClose);
			jw.name("parkEnabled").value(!App.motoreAvviato && App.getParkModeStarted() != null);
			jw.name("parking").value(App.getParkModeStarted() != null && App.parkMode.isOn());
			jw.name("charging").value(App.isCharging());
			jw.name("noGPS").value(App.getNoGPSAlarm());
			if (App.currentTripInfo != null && App.currentTripInfo.trip != null) {
				jw.name("id_trip").value(App.currentTripInfo.trip.remote_id);
			}

			if (intGpsLocation != null) {
				jw.name("int_lon").value(gpsRound(intGpsLocation.getLongitude()));
				jw.name("int_lat").value(gpsRound(intGpsLocation.getLatitude()));
				jw.name("int_time").value(intGpsLocation.getTime());
			}

			if (extGpsLocation != null) {
				jw.name("ext_lon").value(gpsRound(extGpsLocation.getLongitude()));
				jw.name("ext_lat").value(gpsRound(extGpsLocation.getLatitude()));
				jw.name("ext_time").value(extGpsLocation.getTime());
			}

			jw.name("log_tx_time").value(log_sdf.format(new Date()));

			if (longVersion) {
				updateTrips();

				jw.name("fwVer").value(getFw_version().equals("") ? App.fw_version : getFw_version());
				jw.name("swVer").value(App.sw_Version);
				jw.name("sdkVer").value(getSdk_version());
				jw.name("hwVer").value(android.os.Build.MODEL + " " + android.os.Build.DEVICE + " " + android.os.Build.ID);
				jw.name("gsmVer").value(android.os.Build.getRadioVersion());
				jw.name("clock").value(DateFormat.format("dd/MM/yyyy kk:mm:ss", new Date()).toString());

				jw.name("IMEI").value(App.IMEI);
				jw.name("SIM_SN").value(App.SimSerialNumber);

				jw.name("wlsize").value(App.whiteListSize);

				jw.name("offLineTrips").value(tripsToSend);
				jw.name("openTrips").value(tripsOpened);

				if (App.mockLocation == null) {
					String gpsInfo = getGpsJson();
					jw.name("gps_info").value(gpsInfo);
				}

				jw.name("Versions").value(App.Versions.toJson());

				if (allData.containsKey("GPSBOX"))
					jw.name("GPSBOX").value(allData.getString("GPSBOX"));

			}

			jw.endObject();

			jw.close();
		} catch (IOException e) {
			DLog.E("CarInfo.getJson_GPRS();Error creating json:", e);
		}

//		String jsonStr = sw.toString();
		return sw.toString();
	}

	public Bundle getBundle() {
		Bundle b = new Bundle();
		try {
			synchronized (allData) {
				b.putAll(allData);
			}
		} catch (Exception e) {
			dlog.e("CarInfo.getBundle();Exception while retrieving all data ", e);
		}

		return b;
	}

	public int getIsKeyOn() {
		return isKeyOn;
	}

	private void setIsKeyOn(int isKeyOn) {
		this.isKeyOn = isKeyOn;
		beacon.setKeyOn(this.isKeyOn);
	}

	public int getOutAmp() {
		return outAmp;
	}

	void setOutAmp(int outAmp) {
		this.outAmp = outAmp;
		beacon.setPackAmp(this.outAmp);
	}

	private Long getTimestampCurrent() {
		return timestampCurrent;
	}

	private void setTimestampCurrent(Long timestampCurrent) {
		this.timestampCurrent = timestampCurrent;
		beacon.setTimestampAmp(this.timestampCurrent);
	}

	public int getSpeed() {
		return speed;
	}

	private void setSpeed(int speed) {
		this.speed = speed;
		beacon.setSpeed(this.speed);
	}

	public int getKm() {
		return km;
	}

	public void setKm(int km) {
		this.km = km;
		beacon.setKm(this.km);
	}

	public float getVoltage() {
		return voltage;
	}

	private void setVoltage(float voltage) {
		this.voltage = voltage;
		beacon.setMotV(this.voltage);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
		beacon.setVIN(this.id);
	}

	public String getFw_version() {
		return fw_version;
	}

	private void setFw_version(String fw_version) {
		this.fw_version = fw_version;
		beacon.setFwVer(this.fw_version);
	}

	public static String getKeyStatus() {
		return keyStatus;
	}

	public static boolean isKeyOn() {
		return CarInfo.getKeyStatus() == null || !CarInfo.getKeyStatus().equalsIgnoreCase("OFF");
	}

	private void setKeyStatus(String keyStatus) {
		CarInfo.keyStatus = keyStatus;
		beacon.setKeyStatus(CarInfo.keyStatus);
	}

	private String getGear() {
		return gear;
	}

	private void setGear(String gear) {
		this.gear = gear;
		beacon.setGearStatus(this.gear);
	}

	public boolean isChargingPlug() {
		return chargingPlug;
	}

	private void setChargingPlug(boolean chargingPlug) {
		this.chargingPlug = chargingPlug;
		beacon.setPPStatus(this.chargingPlug);
	}

	public static boolean isReady() {
		return ready;
	}

	public void setReady(boolean newReady) {
		ready = newReady;
		beacon.setReadyOn(ready);
	}

//	public boolean isBrakes() {
//		return brakes;
//	}

	private void setBrakes(boolean brakes) {
		this.brakes = brakes;
		beacon.setBrakesOn(this.brakes);
	}

	private String getSdk_version() {
		return sdk_version;
	}

	private void setSdk_version(String sdk_version) {
		this.sdk_version = sdk_version;
		beacon.setSdkVer(this.sdk_version);
	}

	public double getLongitude() {
		return longitude;
	}

	private void setLongitude(double longitude) {
		this.longitude = longitude;
		beacon.setLon(this.longitude);
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
		beacon.setLat(this.latitude);
	}
	
	/*
	public static CarInfo fromBundle(Bundle b) {
		
		CarInfo carInfo = new CarInfo();
		carInfo.VIN = b.getString("VIN", "");
		carInfo.speed = b.getInt("speed",0);
		carInfo.batteryLevel = b.getInt("fuelLevel");
		carInfo.km = b.getInt("km");
		carInfo.voltage = b.getInt("mvolt");

		carInfo.id = b.getString("id","");
		carInfo.fw_version = b.getString("ver", "");
		
		if (b.containsKey("GEAR")) carInfo.gear = b.getInt("GEAR"); 
		if (b.containsKey("KEYON")) carInfo.keyon = b.getInt("KEYON");
		if (b.containsKey("Q")) carInfo.isQuadroAcceso = b.getInt("Q");
		if (b.containsKey("AV")) carInfo.mainBatteryVoltage = b.getInt("AV");
		if (b.containsKey("MTEMP")) carInfo.motor_temp = b.getInt("MTEMP");
		if (b.containsKey("WARN_INDEX")) carInfo.warn_index = b.getInt("WARN_INDEX");
		if (b.containsKey("WARN_STATUS")) carInfo.warn_status = b.getInt("WARN_STATUS");
		if (b.containsKey("WORK_STATUS")) carInfo.work_status = b.getInt("WORK_STATUS");
		if (b.containsKey("READY")) carInfo.ready = b.getInt("READY");
		if (b.containsKey("SPD")) carInfo.speed = b.getInt("SPD");
		if (b.containsKey("BRAKE")) carInfo.brake = b.getInt("BRAKE");
		if (b.containsKey("LVL")) carInfo.batteryLevel = b.getInt("LVL");
		
		
		return carInfo;
	}
	*/

	private class ServiceLocationListener implements LocationListener {

		public void onLocationChanged(Location loc) {
			if (loc != null) {
				OptimizeDistanceCalc.Controller(OdoController.RUNonChangGps, loc);
			}

			intGpsLocation = loc;
			//dlog.d("NEW LOCATION UPDATE"+loc);
			if (!App.UseExternalGPS) {
				setLocation(loc);
			}
		}

		public void onProviderDisabled(String provider) {
		}

		public void onProviderEnabled(String provider) {
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	}

/*	public void RecordToLog() {
		//TODO: implement
	}*/

}
