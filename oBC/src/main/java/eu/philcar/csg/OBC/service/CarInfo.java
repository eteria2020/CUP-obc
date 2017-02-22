package eu.philcar.csg.OBC.service;

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

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.db.Trip;
import eu.philcar.csg.OBC.db.Trips;
import eu.philcar.csg.OBC.db.Events;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.UrlTools;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.util.JsonWriter;

public class CarInfo {

    private DLog dlog = new DLog(this.getClass());
    private Handler serviceHandler;

    public String id = "";

    public String VIN = "";


    public int speed = 0;


    public int batteryLevel = App.fuel_level;
    public int bmsSOC = App.fuel_level;
    public int bmsSOC_GPRS = App.fuel_level;
    public float SOCR = App.fuel_level, virtualSOC = App.fuel_level;
    public int km = 0;
    public float voltage = 0;
    public int isKeyOn = 0;
    public String keyStatus = "";
    public String gear = "";
    public int outAmp = 0;
    public Long timestampCurrent = null;
    public Long lastForceReady=System.currentTimeMillis();




    public float[] cellVoltageValue = new float[24];
    public float minVoltage = 2.5f;
    public String batteryType = "", lowCells = "";
    public Boolean isCellLowVoltage = false;
    public Boolean Charging = false;
    public float currVoltage = App.max_voltage;


    public boolean ready;
    public boolean brakes;
    public boolean chargingPlug;
    public double currentAmpere = App.currentAmp;
    public double chargingAmpere = App.chargingAmp;
    public double maxAmpere = App.maxAmp;

    public String fw_version = "";
    public String sdk_version = "";
    public String versions = "";
    private static Boolean sent = false;

    public Location location;

    public Location intGpslocation = new Location(LocationManager.GPS_PROVIDER);
    public Location extGpslocation = new Location(LocationManager.GPS_PROVIDER);
    ;

    public double longitude;
    public double latitude;
    public double accuracy;

    public long lastLocationChange;
    public long lastUpdate = 0;

    public String danni;
    public int pulizia_int = 2;
    public int pulizia_ext = 2;

    public long tripsOpened = 0;
    public long tripsToSend = 0;

    public int rangeKm = (App.fuel_level <= 50 ? Math.max(App.fuel_level - 10, 0) : App.fuel_level);

    public Bundle allData;


    public ServiceLocationListener serviceLocationListener;

    private SimpleDateFormat log_sdf = new SimpleDateFormat("dd/MM/yyyy HH.mm.ss", Locale.ITALY);


    public CarInfo(Handler handler) {
        serviceHandler = handler;
        serviceLocationListener = new ServiceLocationListener();
        allData = new Bundle();
    }



    public void setBatteryLevel(int batteryLevel) {

        if (Math.abs(this.batteryLevel - batteryLevel) >= 5) {
            this.batteryLevel = (Math.round(this.batteryLevel > batteryLevel ? this.batteryLevel - 5 : this.batteryLevel + 5));
        } else
            this.batteryLevel = (Math.round(SOCR));

    }

    private void setLocation(Location loc) {

        if (App.mockLocation != null) {
            location = App.mockLocation;
            long a=loc.getTime();
            longitude = App.mockLocation.getLongitude();
            latitude = App.mockLocation.getLatitude();
            accuracy = App.mockLocation.getAccuracy();
            App.lastLocation = App.mockLocation;
            return;
        }

        if (loc != null) {
            location = loc;
            long a=loc.getTime();
            String time = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS").format(location.getTime());
            if (longitude != loc.getLongitude() || latitude != loc.getLatitude()) {
                lastLocationChange = System.nanoTime();
                longitude = loc.getLongitude();
                latitude = loc.getLatitude();
                accuracy = loc.getAccuracy();
            }

            if (serviceHandler != null)
                serviceHandler.sendMessage(MessageFactory.sendLocationChange(location));

            App.lastLocation = location;
        }

    }

    public boolean handleUpdate(Bundle b) {
        int i;
        boolean forceBeacon = false;
        List<NameValuePair> paramsList = new ArrayList<NameValuePair>();

        //Update all data in local bundle
        /*if(b.containsKey("SOC"))
			b.putInt("SOC",batteryLevel); //replace the value read from the can with the calculated one*/
        allData.putAll(b);

        //Scan receive budle element to update relevant local values and events

        for (String key : b.keySet()) {

            if (key.equalsIgnoreCase("keyOn")) {
                int k = b.getBoolean(key) ? 1 : 0;
                if (k != isKeyOn) {
                    isKeyOn = k;
                    Events.eventKey(k);

                }
            }

            if (key.equalsIgnoreCase("PackAmp")) {

                int k = b.getInt(key);
                outAmp = k;


            }

            if (key.equalsIgnoreCase("timestampAmp")) {

                timestampCurrent = b.getLong(key);


            } else if (key.equalsIgnoreCase("SOC")) {
                //batteryLevel = b.getInt(key);
                bmsSOC_GPRS = b.getInt(key);
                dlog.d("UpdateBmsSoc: " + b.get(key));
                if (batteryLevel > 20)
                    sent = false;
                if (batteryLevel <= 15 && batteryLevel > 0 && !sent && sendMail("http://manage.sharengo.it/mailObcLowBattery.php")) {
                    Events.eventSoc(batteryLevel, "Email");
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
                speed = (int) Math.round(b.getFloat(key));
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
                km = i;
            } else if (key.equalsIgnoreCase("MotV")) {
                voltage = b.getFloat(key);
            } else if (key.equalsIgnoreCase("VIN")) {
                id = b.getString(key);
                App.Instance.setCarPlate(id);
            } else if (key.equalsIgnoreCase("VER")) {
                App.Instance.setFwVersion(b.getString(key));
            } else if (key.equalsIgnoreCase("keyStatus")) {
                this.keyStatus = b.getString(key);
            } else if (key.equalsIgnoreCase("GearStatus")) {
                String str = b.getString(key);
                if (!this.gear.equals(str)) {
                    this.gear = str;
                    Events.eventGear(str);
                }
            } else if (key.equalsIgnoreCase("PPStatus")) {
                boolean v = b.getBoolean(key);
                if (v != this.chargingPlug) {
                    this.chargingPlug = v;
                    Events.eventCharge(v ? 1 : 0);
                }

                if (!App.Charging && this.chargingPlug) {
                    App.Charging = true;
                    serviceHandler.sendMessage(MessageFactory.notifyStartCharging(this));
                    App.Instance.persistCharging();
                    forceBeacon = true;
                }

            } else if (key.equalsIgnoreCase("ReadyOn")) {
                boolean v = b.getBoolean(key);
                if (v != this.ready) {
                    this.ready = v;
                    Events.eventReady(v ? 1 : 0);
                }
            } else if (key.equalsIgnoreCase("BrakesOn")) {
                this.brakes = b.getBoolean(key);
            } else if (key.equalsIgnoreCase("SDKVer")) {
                this.sdk_version = b.getString(key);
            } else if (key.equalsIgnoreCase("Versions")) {
                this.versions = b.getString(key);
            } else if (key.equalsIgnoreCase("GPSBOX")) {
                String j = b.getString(key);
                if (j != null && !j.isEmpty()) {
                    JSONObject jo;
                    try {
                        jo = new JSONObject(j);

                        extGpslocation.setLatitude(jo.getDouble("lat"));
                        extGpslocation.setLongitude(jo.getDouble("lon"));
                        extGpslocation.setAccuracy((float) jo.getDouble("acc"));
                        extGpslocation.setSpeed((float) jo.getDouble("spd"));
                        extGpslocation.setTime(jo.getLong("ts"));
                        extGpslocation.setBearing((float) jo.getDouble("head"));

                        if (App.UseExternalGPS) {
                            setLocation(extGpslocation);
                        }

                    } catch (JSONException e) {
                        DLog.E("Parsing GPSBOX:", e);
                    }
                } else {
                    DLog.W("Empty GPSBOX json");
                }


            } else if (key.equalsIgnoreCase("CellInfo")) {
                Bundle c = b.getBundle(key);
                //this.cellVoltageValue[c.getInt("CellIndex")]=c.getFloat("CellVoltage");

            } else if (key.equalsIgnoreCase("CellsInfo")) {

                //this.cellVoltageValue=b.getFloatArray(key);

            }
        }

        return forceBeacon;
    }

    public Bundle betterHandleUpdate(Bundle b) {
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

        //Scan receive budle element to update relevant local values and events

        for (String key : b.keySet()) {

            if (key.equalsIgnoreCase("keyOn")) {
                i = b.getBoolean(key) ? 1 : 0;
                if (i != isKeyOn) {
                    hasChanged = true;
                    isKeyOn = i;
                    Events.eventKey(i);

                    if(i==1&& App.currentTripInfo!=null && App.currentTripInfo.isOpen && App.pinChecked && App.getParkModeStarted()==null && !App.isClosing && System.currentTimeMillis()-lastForceReady>5000){        //Ask Rosco if good idea
                        lastForceReady=System.currentTimeMillis();
                        serviceHandler.sendMessage(MessageFactory.setEngine(true));
                    }

                }
            } else if (key.equalsIgnoreCase("PackAmp")) {

                i = b.getInt(key);
                if (i != outAmp) {
                    hasChanged = true;
                    outAmp = i;
                }


            } else if (key.equalsIgnoreCase("timestampAmp")) {
                l = b.getLong(key);
                if (l != timestampCurrent) {
                    hasChanged = true;
                    timestampCurrent = l;
                }


            } else if (key.equalsIgnoreCase("SOC")) {

                i = b.getInt(key);
                if (i != bmsSOC_GPRS) {
                    hasChanged = true;
                    //batteryLevel = b.getInt(key);
                    bmsSOC_GPRS = i;
                    dlog.d("UpdateBmsSoc: " + i);
                    if (batteryLevel > 20)
                        sent = false;
                    if (batteryLevel <= 15 && batteryLevel > 0 && !sent && sendMail("http://manage.sharengo.it/mailObcLowBattery.php")) {
                        Events.eventSoc(batteryLevel, "Email");
                        sent = true;
                    }
                    App.Instance.setBatteryLevel(batteryLevel);
                    if (batteryLevel <= 50)
                        rangeKm = Math.max(batteryLevel - 10, 0);
                    else if (batteryLevel <= 60)
                        rangeKm = batteryLevel;
                    else
                        rangeKm = batteryLevel;
                }
            } else if (key.equalsIgnoreCase("Speed")) {
                i = (int) Math.round(b.getFloat(key));
                if (i != speed) {
                    hasChanged = true;
                    speed = i;
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
                if (i >= 0 && i != km) {
                    hasChanged = true;
                    App.Instance.setKm(i);
                    km = i;
                }
            } else if (key.equalsIgnoreCase("MotV")) {
                f = b.getFloat(key);
                if (f != voltage) {
                    hasChanged = true;
                    voltage = b.getFloat(key);
                }
            } else if (key.equalsIgnoreCase("VIN")) {
                s = b.getString(key);
                if (!s.equals(App.CarPlate)) {
                    hasChanged = true;
                    id = s;
                    App.Instance.setCarPlate(id);
                    serviceHandler.sendMessage(MessageFactory.zmqRestart());
                }
            } else if (key.equalsIgnoreCase("VER")) {
                s = b.getString(key);
                if (!s.equals(App.fw_version)) {
                    hasChanged = true;
                    App.Instance.setFwVersion(s);
                }
            } else if (key.equalsIgnoreCase("keyStatus")) {
                s = b.getString(key);
                if (!s.equals(keyStatus)) {
                    hasChanged = true;
                    this.keyStatus = b.getString(key);
                }
            } else if (key.equalsIgnoreCase("GearStatus")) {
                s = b.getString(key);
                if (!this.gear.equals(s)) {
                    hasChanged = true;
                    this.gear = s;
                    Events.eventGear(s);
                }
            } else if (key.equalsIgnoreCase("PPStatus")) {
                bo = b.getBoolean(key);
                if (bo != this.chargingPlug) {
                    this.chargingPlug = bo;
                    Events.eventCharge(bo ? 1 : 0);

                    if (!App.Charging && this.chargingPlug ) {

                        App.Charging = true;
                        serviceHandler.sendMessage(MessageFactory.notifyStartCharging(this));
                        App.Instance.persistCharging();
                        forceBeacon = true;
                    }

                }
            } else if (key.equalsIgnoreCase("ReadyOn")) {
                bo = b.getBoolean(key);
                if (bo != this.ready) {
                    hasChanged = true;
                    this.ready = bo;
                    Events.eventReady(bo ? 1 : 0);
                }
            } else if (key.equalsIgnoreCase("BrakesOn")) {
                bo = b.getBoolean(key);
                if (bo != this.ready) {
                    hasChanged = true;
                    this.brakes = bo;
                }
            } else if (key.equalsIgnoreCase("SDKVer")) {
                s = b.getString(key);
                if (!s.equals(this.sdk_version)) {
                    hasChanged = true;
                    this.sdk_version = s;
                }
            } else if (key.equalsIgnoreCase("Versions")) {
                s = b.getString(key);
                if (!s.equals(this.versions)) {
                    hasChanged = true;
                    this.versions = s;
                }
            } else if (key.equalsIgnoreCase("GPSBOX")) {
                String j = b.getString(key);
                if (j != null && !j.isEmpty()) {
                    JSONObject jo;
                    try {
                        jo = new JSONObject(j);

                        extGpslocation.setLatitude(jo.getDouble("lat"));
                        extGpslocation.setLongitude(jo.getDouble("lon"));
                        extGpslocation.setAccuracy((float) jo.getDouble("acc"));
                        extGpslocation.setSpeed((float) jo.getDouble("spd"));
                        extGpslocation.setTime(jo.getLong("ts"));
                        extGpslocation.setBearing((float) jo.getDouble("head"));

                        if (App.UseExternalGPS) {
                            hasChanged = true;
                            setLocation(extGpslocation);
                        }

                    } catch (JSONException e) {
                        DLog.E("Parsing GPSBOX:", e);
                    }
                } else {
                    DLog.W("Empty GPSBOX json");
                }


            } else if (key.equalsIgnoreCase("CellInfo")) {
                //Bundle c=b.getBundle(key);
                //this.cellVoltageValue[c.getInt("CellIndex")]=c.getFloat("CellVoltage");

            } else if (key.equalsIgnoreCase("CellsInfo")) {

                //this.cellVoltageValue=b.getFloatArray(key);

            }
        }
        if (System.currentTimeMillis() - lastUpdate > 12000) {
            lastUpdate = System.currentTimeMillis();
            hasChanged = true;
        }
        res.putBoolean("changed", hasChanged);
        res.putBoolean("force", forceBeacon);

        return res;
    }

    public Boolean sendMail(String Url) {


        if (!App.hasNetworkConnection) {
            dlog.e(" sendMail: nessuna connessione");
            return false;
        }
        StringBuilder builder = new StringBuilder();
        List<NameValuePair> paramsList = new ArrayList<NameValuePair>();
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
            DLog.D(" sendMail: Url richiesta " + Url);
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

                dlog.e(" sendMail: Failed to connect " + String.valueOf(statusCode));
                return false;
            }
        } catch (Exception e) {
            dlog.e(" sendMail: eccezione in connessione ", e);
            return false;
        } finally {
            client.getConnectionManager().shutdown();
        }

        String jsonStr = builder.toString();
        if (jsonStr.compareTo("0") == 0) {
            dlog.e(" sendMail: invio mail fallito");

            return false;
        } else if (jsonStr.compareTo("1") == 0) {
            dlog.d(" sendMail: invio mail riuscito");

            return true;
        }

        DLog.D(" sendMail: risposta " + jsonStr);
        return false;
    }


    public void updateTrips() {
        Trips corse = App.Instance.getDbManager().getCorseDao();
        if (corse != null) {
            tripsToSend = corse.getNTripsToSend();

            List<Trip> list = corse.getOpenTrips();
            if (list != null) {
                tripsOpened = list.size();
            }
        }
    }


    public String getGpsJson() {

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
            DLog.E("Error creating  gps json:", e);
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
            dlog.e("Exception while append json", e);
        }

    }

    private double gpsRound(double value) {
        return Math.round(value * 100000D) / 100000D;
    }

    public String getJson(boolean longVersion) {
        StringWriter sw = new StringWriter();
        JsonWriter jw = new JsonWriter(sw);

        //Per chiudere da remoto una corsa (da app utente finale) dobbiamo essere nell'ultima schermata, essere in area accettabile e non in sosta.
        boolean enableRemoteClose = App.isCloseable && App.checkParkArea(latitude, longitude) && (App.getParkModeStarted() == null);

        try {
            jw.beginObject();

            for (String key : allData.keySet()) {
                if (key != "GPSBOX") {
                    if (key == "SOC") {
                        appendJson(jw, key, batteryLevel);
                    } else
                        appendJson(jw, key, allData.get(key));
                }
            }


            if (App.mockLocation != null) {
                jw.name("lon").value(gpsRound(App.mockLocation.getLongitude()));
                jw.name("lat").value(gpsRound(App.mockLocation.getLatitude()));
            } else {
                jw.name("lon").value(gpsRound(longitude));
                jw.name("lat").value(gpsRound(latitude));
            }

            jw.name("GPS").value(App.UseExternalGPS ? "EXT" : "INT");
            jw.name("SOC").value(batteryLevel);
            jw.name("cputemp").value(App.getCpuTemp());

            jw.name("on_trip").value(App.currentTripInfo != null ? 1 : 0);

            jw.name("closeEnabled").value(enableRemoteClose);
            jw.name("parkEnabled").value(!App.motoreAvviato && App.getParkModeStarted() != null);
            jw.name("parking").value(App.getParkModeStarted() != null && App.parkMode.isOn());
            jw.name("charging").value(App.Charging);
            jw.name("PPStatus").value(this.chargingPlug);
            if (App.currentTripInfo != null && App.currentTripInfo.trip != null) {
                jw.name("id_trip").value(App.currentTripInfo.trip.remote_id);
            }


            if (intGpslocation != null) {
                jw.name("int_lon").value(gpsRound(intGpslocation.getLongitude()));
                jw.name("int_lat").value(gpsRound(intGpslocation.getLatitude()));
                jw.name("int_time").value(intGpslocation.getTime());
            }

            if (extGpslocation != null) {
                jw.name("ext_lon").value(gpsRound(extGpslocation.getLongitude()));
                jw.name("ext_lat").value(gpsRound(extGpslocation.getLatitude()));
                jw.name("ext_time").value(extGpslocation.getTime());
            }


            jw.name("log_tx_time").value(log_sdf.format(new Date()));

            if (longVersion) {
                updateTrips();

                jw.name("fwVer").value(fw_version.equals("") ? App.fw_version : fw_version);
                jw.name("swVer").value(App.sw_Version);
                jw.name("sdkVer").value(sdk_version);
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
            DLog.E("Error creating json:", e);
        }

        String jsonStr = sw.toString();
        return jsonStr;
    }

    public String getJson_GPRS(boolean longVersion) {
        StringWriter sw = new StringWriter();
        JsonWriter jw = new JsonWriter(sw);

        //Per chiudere da remoto una corsa (da app utente finale) dobbiamo essere nell'ultima schermata, essere in area accettabile e non in sosta.
        boolean enableRemoteClose = App.isCloseable && App.checkParkArea(latitude, longitude) && (App.getParkModeStarted() == null);

        try {
            jw.beginObject();

            for (String key : allData.keySet()) {
                if (key != "GPSBOX") {

                    appendJson(jw, key, allData.get(key));
                }
            }


            if (App.mockLocation != null) {
                jw.name("lon").value(gpsRound(App.mockLocation.getLongitude()));
                jw.name("lat").value(gpsRound(App.mockLocation.getLatitude()));
            } else {
                jw.name("lon").value(gpsRound(longitude));
                jw.name("lat").value(gpsRound(latitude));
            }

            jw.name("GPS").value(App.UseExternalGPS ? "EXT" : "INT");
            jw.name("cputemp").value(App.getCpuTemp());

            jw.name("on_trip").value(App.currentTripInfo != null ? 1 : 0);

            jw.name("closeEnabled").value(enableRemoteClose);
            jw.name("parkEnabled").value(!App.motoreAvviato && App.getParkModeStarted() != null);
            jw.name("parking").value(App.getParkModeStarted() != null && App.parkMode.isOn());
            jw.name("charging").value(App.Charging);
            if (App.currentTripInfo != null && App.currentTripInfo.trip != null) {
                jw.name("id_trip").value(App.currentTripInfo.trip.remote_id);
            }


            if (intGpslocation != null) {
                jw.name("int_lon").value(gpsRound(intGpslocation.getLongitude()));
                jw.name("int_lat").value(gpsRound(intGpslocation.getLatitude()));
                jw.name("int_time").value(intGpslocation.getTime());
            }

            if (extGpslocation != null) {
                jw.name("ext_lon").value(gpsRound(extGpslocation.getLongitude()));
                jw.name("ext_lat").value(gpsRound(extGpslocation.getLatitude()));
                jw.name("ext_time").value(extGpslocation.getTime());
            }


            jw.name("log_tx_time").value(log_sdf.format(new Date()));

            if (longVersion) {
                updateTrips();

                jw.name("fwVer").value(fw_version.equals("") ? App.fw_version : fw_version);
                jw.name("swVer").value(App.sw_Version);
                jw.name("sdkVer").value(sdk_version);
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
            DLog.E("Error creating json:", e);
        }

        String jsonStr = sw.toString();
        return jsonStr;
    }


    public Bundle getBundle() {
        Bundle b = new Bundle();
        try {
            Bundle a = allData;
            b.putAll(a);
        } catch (Exception e) {
            dlog.e("Exception while retrieving all data ", e);
        }


        return b;
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

            intGpslocation = loc;

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

    public void RecordToLog() {
        //TODO: implement
    }

}
