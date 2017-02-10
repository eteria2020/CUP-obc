package eu.philcar.csg.OBC.service;


import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.acra.ACRA;
import org.json.JSONException;
import org.json.JSONObject;

import eu.philcar.csg.OBC.AGoodbye;
import eu.philcar.csg.OBC.AWelcome;
import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.AMainOBC;
import eu.philcar.csg.OBC.db.Poi;
import eu.philcar.csg.OBC.helpers.AudioPlayer;
import eu.philcar.csg.OBC.SystemControl;
import eu.philcar.csg.OBC.controller.map.FRadio;
import eu.philcar.csg.OBC.controller.welcome.FWelcome;
import eu.philcar.csg.OBC.db.Customers;
import eu.philcar.csg.OBC.db.Trips;
import eu.philcar.csg.OBC.db.DbManager;
import eu.philcar.csg.OBC.db.Events;
import eu.philcar.csg.OBC.db.Pois;
import eu.philcar.csg.OBC.devices.Hik_io;
import eu.philcar.csg.OBC.devices.LowLevelInterface;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.Debug;
import eu.philcar.csg.OBC.helpers.ServiceTestActivity;
import eu.philcar.csg.OBC.server.AdminsConnector;
import eu.philcar.csg.OBC.server.CallCenterConnector;
import eu.philcar.csg.OBC.server.CommandsConnector;
import eu.philcar.csg.OBC.server.ConfigsConnector;
import eu.philcar.csg.OBC.server.Connectors;
import eu.philcar.csg.OBC.server.HttpsConnector;
import eu.philcar.csg.OBC.server.NotifiesConnector;
import eu.philcar.csg.OBC.server.ServerCommand;
import eu.philcar.csg.OBC.server.CustomersConnector;
import eu.philcar.csg.OBC.server.HttpConnector;
import eu.philcar.csg.OBC.server.ReservationConnector;
import eu.philcar.csg.OBC.server.UdpServer;
import eu.philcar.csg.OBC.server.UploaderLog;
import eu.philcar.csg.OBC.server.ZmqRequester;
import eu.philcar.csg.OBC.server.ZmqSubscriber;

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
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.telephony.SmsManager;

public class ObcService extends Service {


    private DLog dlog = new DLog(this.getClass());


    //BUILD OPTIONS
    public static final boolean WITH_UDPSERVER = true;
    public static final boolean WITH_UDPQUERY = false;
    public static final boolean WITH_AUTORESET3G = true;
    public static final boolean WITH_OUTOFORDER_WATCHDOG = true;


    public static boolean WITH_HTTP_NOTIFIES = true;

    public static boolean WITH_ZMQNOTIFY = false;
    public static boolean WITH_ZMQREQREP = false;

    //PUBLIC CONSTANTS

    public static final int MSG_PING = 1;
    public static final int MSG_CLIENT_REGISTER = 2;
    public static final int MSG_CLIENT_UNREGISTER = 3;
    public static final int MSG_CMD_TIMEOUT = 4;
    public static final int MSG_OBC_REINIT = 5;
    public static final int MSG_UI_CHECK = 6;
    public static final int MSG_SERVICE_STOP = 9;


    public static final int MSG_IO_GETSTATUS = 10;
    public static final int MSG_IO_RFID = 20;
    public static final int MSG_IO_ENGINE = 21;
    public static final int MSG_IO_LEDS = 22;
    public static final int MSG_IO_DOORS = 23;
    public static final int MSG_IO_CHARGE = 24;
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

    public static final int MSG_CUSTOMER_INFO = 40;
    public static final int MSG_CUSTOMER_CHECKPIN = 41;
    public static final int MSG_CAR_CLEANLINESS = 42;
    public static final int MSG_CUSTOMER_SOS = 43;

    public static final int MSG_TRIP_BEGIN = 50;
    public static final int MSG_TRIP_END = 51;
    public static final int MSG_TRIP_EVENT = 52;
    public static final int MSG_TRIP_PARK = 53;
    public static final int MSG_TRIP_PARK_CARD_BEGIN = 54;
    public static final int MSG_TRIP_PARK_CARD_END = 55;
    public static final int MSG_TRIP_SELFCLOSE = 56;
    public static final int MSG_TRIP_SCHEDULE_SELFCLOSE = 57;
    public static final int MSG_TRIP_SENDBEACON = 58;

    public static final int MSG_SERVER_NOTIFY = 60;
    public static final int MSG_SERVER_RESERVATION = 61;
    public static final int MSG_SERVER_COMMAND = 62;
    public static final int MSG_SERVER_HTTPNOTIFY = 63;
    public static final int MSG_SERVER_CHANGE_IP = 64;
    public static final int MSG_SERVER_CHANGE_LOG = 65;

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


    public static final int MSG_DEBUG_CARD = 90;
    public static final int MSG_DEBUG_CARD_OPEN = 91;


    public static final int MSG_ZMQ_RESTART = 100;

    public static final int SERVER_NOTIFY_RAW = 0;
    public static final int SERVER_NOTIFY_RESERVATION = 1;
    public static final int SERVER_NOTIFY_COMMAND = 2;


    public static final String EVENT_CARD_OPEN = "OPEN";
    public static final String EVENT_CARD_CLOSE = "CLOSE";

    //private constants

    private final String SYSTEM_ALARM_NAME = "eu.philcar.csg.OBC.syncalarm";

    Messenger messenger = null;

    private ArrayList<Messenger> clients = new ArrayList<Messenger>();

    private boolean isStarted;
    private long lastValidSOC = 0;
    private boolean isStopRequested = false;



    //ENCAPSULATED OBJECTS

    private UdpServer udpServer;
    private LowLevelInterface obc_io;
    private CarInfo carInfo;
    private TripInfo tripInfo;

    private ZmqSubscriber zmqSubscriber;
    private ZmqRequester zmqRequester;

    private AlarmManager alarmManager;
    private PendingIntent alarmPendingIntent;

    private GpsStatus gpsStatus;

    ScheduledExecutorService serverUpdateScheduler;
    ScheduledExecutorService tripUpdateScheduler;
    ScheduledExecutorService virtualBMSUpdateScheduler;
    ScheduledExecutorService gpsCheckeScheduler;


    private LocationManager locationManager;

    private WakeLock screenLockTrip;
    private boolean RequireDisplayOn = false;


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
                    dlog.d("GPS STARTED");
                    break;
                case GpsStatus.GPS_EVENT_STOPPED:
                    dlog.d("GPS STOPPED");
                    break;
                case GpsStatus.GPS_EVENT_FIRST_FIX:
                    dlog.d("GPS FIRST FIX");
                    break;
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    break;
            }

            gpsStatus = locationManager.getGpsStatus(gpsStatus);
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();

        dlog.d("Service onCreate");


        if (App.Instance.loadZmqDisabledConfig()) {
            this.WITH_ZMQNOTIFY = false;
            this.WITH_HTTP_NOTIFIES = true;
            dlog.d("** Notify protocol: HTTP");
        } else {
            this.WITH_ZMQNOTIFY = true;
            this.WITH_HTTP_NOTIFIES = false;
            dlog.d("** Notify protocol: ZMQ");
        }


        if (Debug.IGNORE_HARDWARE) {
            return;
        }

        screenLockTrip = ((PowerManager) getSystemService(POWER_SERVICE)).newWakeLock(
                PowerManager.ON_AFTER_RELEASE | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.FULL_WAKE_LOCK, "ObcService");


        //Start messenger

        messenger = new Messenger(localHandler);


        if (WITH_UDPSERVER) {
            // Start Udp Server
            udpServer = new UdpServer();
            udpServer.init(localHandler);
        }

        if (this.WITH_ZMQNOTIFY) {
            zmqSubscriber = new ZmqSubscriber();
            localHandler.sendMessageDelayed(MessageFactory.zmqRestart(), 20000);
            //zmqSubscriber.Start(localHandler);
        }

        // Start low level IO module
        //obc_io = new Obc_io(this);
        obc_io = new Hik_io(this);
        obc_io.init();

        //Create and init data structures

        carInfo = new CarInfo(localHandler);
        carInfo.id = App.CarPlate;

        tripInfo = new TripInfo();
        tripInfo.init();

        //Init GPS

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        dlog.d("GPS provider is : " + (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ? "ENABLED" : "DISABLED"));
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(true);
        criteria.setPowerRequirement(Criteria.POWER_HIGH);


        setLocationMode(30000);  // If car is idle  30sec is min time between locations updates
        locationManager.addGpsStatusListener(gpsStatusListener);

        //Check if UI is active and if not restart the proper page
        checkAndRestartUI();

        // Reacquire lock if trip open
        if (tripInfo.isOpen) {
            App.currentTripInfo = tripInfo;
            setDisplayStatus(true, 0);
            dlog.d("Restarting from open trip. Acquiring lock");
            obc_io.setLed(null, LowLevelInterface.ID_LED_BLUE, LowLevelInterface.ID_LED_ON);
            startRemoteUpdateCycle();
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
        final Events eventi = App.Instance.dbManager.getEventiDao();
        eventi.sendEvent(Events.EVT_SWBOOT, App.sw_Version);

        //Start whitelist update
        Customers customers = App.Instance.dbManager.getClientiDao();
        customers.startWhitelistDownload(this, privateHandler);


        Pois pois = App.Instance.dbManager.getPoisDao();
        pois.startDownload(this, localHandler);

        App.Instance.startAreaPolygonDownload(this, null);

        //Start reservation update
        startDownloadReservations();

        //Start download admin cards :  DISABLED FOR BUGS ON HARDWARE
        //startDownloadAdmins();

        //Start download configs
        startDownloadConfigs();

        //Start dequeueing  trips  and events
        privateHandler.sendEmptyMessage(Connectors.MSG_TRIPS_SENT_OFFLINE);
        privateHandler.sendEmptyMessage(Connectors.MSG_EVENTS_SENT_OFFLINE);

        // Start scheduler for server query
        serverUpdateScheduler = Executors.newSingleThreadScheduledExecutor();
        //Start scheduler for CAN query
        virtualBMSUpdateScheduler = Executors.newSingleThreadScheduledExecutor();
        //Start scheduler for GPS query
        gpsCheckeScheduler = Executors.newSingleThreadScheduledExecutor();

        // Start internal loop with period of  10 sec

        serverUpdateScheduler.scheduleAtFixedRate(new Runnable() {

            private boolean _secondRun = false;
            private int minutePrescaler = 0;
            private int threeminutePrescaler = 0;

            @Override
            public void run() {
                try {

                    //If UDP query is enabled send query packet
                    if (WITH_UDPSERVER && WITH_UDPQUERY)
                        udpServer.sendQuery();
                    //If HTTP query is enabled schedule an HTTP notifies download
                    if (WITH_HTTP_NOTIFIES) {
                        HttpConnector http = new HttpConnector(ObcService.this);
                        http.SetHandler(localHandler);
                        NotifiesConnector nc = new NotifiesConnector();
                        nc.setTarga(App.CarPlate);
                        http.Execute(nc);
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
                        Events.eventObcFail(obc_io.lastContactDelay());
                    } else if (obc_io.lastContactDelay() <= 30 && App.ObcIoError) {
                        App.ObcIoError = false;
                        Events.eventObcOk();
                    }
                    if (threeminutePrescaler++ >= 18) {
                        threeminutePrescaler = 0;

                        //Check network link
                        CheckNetwork();
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
                    dlog.e("serverUpdateScheduler error", e);
                }
            }

        }, 10, 10, TimeUnit.SECONDS);

        virtualBMSUpdateScheduler.scheduleAtFixedRate(new Runnable() {


            @Override
            public void run() {
                try {

                    float currVolt = 0;
                    boolean cellLow = false;
                    String lowCellNumber = " ";
                    String cellsVoltage = "";

                    carInfo.cellVoltageValue = getCellVoltages();
                    carInfo.bmsSOC = getSOCValue();
                    carInfo.outAmp = getCurrentValue();

                    carInfo.minVoltage = carInfo.cellVoltageValue[22] == 0 ? 2.8f : 2.5f;
                    carInfo.batteryType = carInfo.cellVoltageValue[22] == 0 ? "DFD" : "HNLD";


                    for (int i = 0; i < carInfo.cellVoltageValue.length; i++) {
                        currVolt += carInfo.cellVoltageValue[i];    //		battery cell voltages
                        cellsVoltage = cellsVoltage.concat(" " + carInfo.cellVoltageValue[i]);
                        if (carInfo.cellVoltageValue[i] < carInfo.minVoltage && carInfo.cellVoltageValue[i] != 0) {
                            cellLow = true;
                            lowCellNumber = lowCellNumber.concat((i + 1) + " ");
                        }
                    }

                    carInfo.isCellLowVoltage = cellLow;
                    carInfo.lowCells = lowCellNumber;
                    carInfo.currVoltage = (float) Math.round(currVolt * 100) / 100f;

                    if (carInfo.currVoltage <= 0 && (lastValidSOC - System.currentTimeMillis() < 60000 * 5)) {
                        carInfo.setBatteryLevel(Math.min(carInfo.bmsSOC, carInfo.bmsSOC_GPRS));
                        dlog.d("virtualBMSUpdateScheduler: value null ignoring data.");
                        return;
                    }
                    lastValidSOC = System.currentTimeMillis();

                    if (carInfo.bmsSOC >= 100 || carInfo.bmsSOC_GPRS >= 100) {
                        if (!carInfo.Charging || (carInfo.currVoltage > App.getMax_voltage())) {
                            App.Instance.setMaxVoltage(carInfo.currVoltage > 85f || carInfo.currVoltage < 80f ? 83f : carInfo.currVoltage);
                            dlog.d("virtualBMSUpdateScheduler: set maxVoltage to " + App.getMax_voltage() + "% bmsSOC: " + carInfo.bmsSOC + " % bmsSOC_GPRS: " + carInfo.bmsSOC_GPRS + "% currVoltage " + carInfo.currVoltage + "% Charging: " + carInfo.Charging);
                            carInfo.Charging = true;
                        }
                    } else {
                        carInfo.Charging = false;
                    }
                    carInfo.virtualSOC = (carInfo.cellVoltageValue[22] == 0 ? ((float) Math.round((100 - 90 * (App.Instance.getMax_voltage() - currVolt) / 12) * 10) / 10f) : ((float) Math.round((100 - 90 * (App.Instance.getMax_voltage() - 3 - currVolt) / 9.5) * 10) / 10f));//DFD:H

                    carInfo.SOCR = carInfo.bmsSOC == carInfo.bmsSOC_GPRS ? Math.min(Math.min(carInfo.bmsSOC, carInfo.bmsSOC_GPRS), carInfo.virtualSOC) : carInfo.virtualSOC;
                    if ((carInfo.isCellLowVoltage) || carInfo.currVoltage <= 67f) {

                        carInfo.SOCR = Math.min(carInfo.virtualSOC, 0f);
                    }

                        carInfo.setBatteryLevel(Math.round(carInfo.SOCR));

                    //carInfo.batteryLevel=Math.min(carInfo.bmsSOC,carInfo.bmsSOC_GPRS); //PER VERSIONI NON -BMS SCOMMENTARE E COMMENTARE IF SOPRA


					/*Message msg = MessageFactory.notifyCANDataUpdate(carInfo);
                    sendAll(msg);*/


                    dlog.d("virtualBMSUpdateScheduler: VBATT: " + carInfo.currVoltage + "V V100%: " + App.Instance.max_voltage + "V cell voltage: " + cellsVoltage + " soc: " + carInfo.bmsSOC + "% SOCR: " + carInfo.SOCR + "% SOC2:" + carInfo.virtualSOC + "% SOC.ADMIN:" + carInfo.batteryLevel + "% bmsSOC_GPRS:" + carInfo.bmsSOC_GPRS + "%");


                } catch (Exception e) {
                    dlog.e("virtualBMSUpdateScheduler error", e);
                }
            }

        }, 10, 120, TimeUnit.SECONDS);

        gpsCheckeScheduler.scheduleAtFixedRate(new Runnable() {


            int intCount = 0, extCount = 0;
            Location lastIntGpslocation = new Location(LocationManager.GPS_PROVIDER);
            Location lastExtGpslocation = new Location(LocationManager.GPS_PROVIDER);

            @Override
            public void run() {
                try {


                    if (!App.UseExternalGPS) {

                        if ((carInfo.intGpslocation.getLongitude() >= 18.53 || carInfo.intGpslocation.getLongitude() <= 6.63 || carInfo.intGpslocation.getLatitude() >= 47.10 || carInfo.intGpslocation.getLatitude() <= 36.64) ||
                                (carInfo.intGpslocation.getLongitude() == 0 || carInfo.intGpslocation.getLongitude() == 0 || carInfo.intGpslocation.getLatitude() == 0 || carInfo.intGpslocation.getLatitude() == 0)) {
                            App.Instance.setUseExternalGps(true);
                            dlog.d("GpsCheckeScheduler: setUseExternalGps(true) IntGpsLocation out from Italy or location 0.0");
                            sendBeacon();//update remoto per aggiornare int/ext
                            intCount = 0;
                            return;
                        }

                        if (carInfo.intGpslocation.getLatitude() == lastIntGpslocation.getLatitude() && carInfo.intGpslocation.getLongitude() == lastIntGpslocation.getLongitude()) {
                            intCount++;
                            if (intCount >= 3) {
                                intCount = 0;
                                App.Instance.setUseExternalGps(true);
                                dlog.d("GpsCheckeScheduler: setUseExternalGps(true) same coordinate for " + intCount + " times");
                                sendBeacon();
                            }
                            return;
                        } else {
                            intCount = 0;
                            lastIntGpslocation = carInfo.intGpslocation;
                        }
                    } else {

                        if ((carInfo.extGpslocation.getLongitude() >= 18.53 || carInfo.extGpslocation.getLongitude() <= 6.63 || carInfo.extGpslocation.getLatitude() >= 47.10 || carInfo.extGpslocation.getLatitude() <= 36.64) ||
                                (carInfo.extGpslocation.getLatitude() == 0 || carInfo.extGpslocation.getLatitude() == 0 || carInfo.extGpslocation.getLongitude() == 0 || carInfo.extGpslocation.getLongitude() == 0)) {
                            App.Instance.setUseExternalGps(false);
                            dlog.d("GpsCheckeScheduler: setUseExternalGps(false) ExtGpsLocation out from Italy or location 0.0");
                            sendBeacon();
                            extCount = 0;
                            return;
                        }
                        if (carInfo.extGpslocation.getLatitude() == lastExtGpslocation.getLatitude() && carInfo.extGpslocation.getLongitude() == lastExtGpslocation.getLongitude()) {
                            extCount++;
                            if (extCount >= 3) {
                                extCount = 0;
                                App.Instance.setUseExternalGps(false);
                                dlog.d("GpsCheckeScheduler: setUseExternalGps(false) same coordinate for " + intCount + " times");
                                sendBeacon();
                            }
                            return;
                        } else {
                            extCount = 0;
                            lastExtGpslocation = carInfo.extGpslocation;
                        }

                    }


                } catch (Exception e) {
                    dlog.e("gpsCheckeScheduler error", e);
                }
            }

        }, 2, 2, TimeUnit.MINUTES);


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

        if (this.WITH_ZMQREQREP) {
            zmqRequester = new ZmqRequester();
            zmqRequester.Send("Hello", null, null);
        }

        //Request an NTP resync
        SystemControl.ResycNTP();


        dlog.d("Service created");
        isStarted = true;
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

        isStopRequested = true;

        if (serverUpdateScheduler != null)
            serverUpdateScheduler.shutdown();
        if (virtualBMSUpdateScheduler != null)
            virtualBMSUpdateScheduler.shutdown();
        if (gpsCheckeScheduler != null)
            gpsCheckeScheduler.shutdown();

        stopRemoteUpdateCycle();

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
    private void setLocationMode(long minTime) {
        if (locationManager != null) {
            locationManager.removeUpdates(carInfo.serviceLocationListener);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, 0, carInfo.serviceLocationListener);
        }
    }

    private void setNetworkConnectionStatus(boolean status) {


        //Force led blinking if connection is lost
        obc_io.setForcedLedBlink(!status);

        //Detect loss or recover connection
        if (App.hasNetworkConnection && !status) {
            App.hasNetworkConnection = false;
        } else if (!App.hasNetworkConnection && status) {
            App.hasNetworkConnection = true;
            App.lastNetworkOn = new Date();

            //Force zmq restart for faster reconnection
            localHandler.sendMessage(MessageFactory.zmqRestart());

            //Dequeue eventual offline trip or events
            privateHandler.sendEmptyMessage(Connectors.MSG_TRIPS_SENT_OFFLINE);
            privateHandler.sendEmptyMessage(Connectors.MSG_EVENTS_SENT_OFFLINE);

            sendBeacon();

        }


    }

    private static int restart3GCount = 0;

    private void CheckNetwork() {
        if (!SystemControl.hasNetworkConnection(this)) {
            DLog.D(ObcService.class.toString() + " NO NETWORK CONNECTION");
            setNetworkConnectionStatus(false);
            if (restart3GCount >= 8 && !this.tripInfo.isOpen) {
                restart3GCount = 0;
                SystemControl.doReboot();
            } else {
                if (WITH_AUTORESET3G) {
                    SystemControl.Reset3G(this);
                }
                restart3GCount++;
            }


            if (WITH_OUTOFORDER_WATCHDOG && App.lastNetworkOn != null) {
                long delta = (new Date().getTime() - App.lastNetworkOn.getTime()) / 1000;
                if (delta > 31 * 60 & App.reservation == null) {
                    dlog.w("Timeout max network offline : out of order local reservation");
                    this.setReservation(Reservation.createOutOfOrderReservation());
                    Events.BeginOutOfOrder("NET");
                }
            }


        } else {
            App.lastNetworkOn = new Date();
            restart3GCount = 0;
            if (App.reservation != null && App.reservation.isMaintenance() && App.reservation.isLocal()) {
                dlog.w("Network on - removing out of order local reservation");
                setReservation(null);
                Events.EndOutOfOrder();
            }
            setNetworkConnectionStatus(true);
        }
    }

    public void sendBeacon() {

        if (!App.hasNetworkConnection) {
            dlog.w("No connection. Beacon aborted");
            return;
        }

        String msg = carInfo.getJson(true);
        if (msg != null) {
            dlog.d("Sending beacon : " + msg);
            //udpServer.sendBeacon(msg);

            NotifiesConnector nc = new NotifiesConnector();
            nc.setTarga(App.CarPlate);
            nc.setBeacon(msg);

            HttpConnector httpConnector = new HttpConnector(ObcService.this);
            httpConnector.HttpMethod = HttpConnector.METHOD_GET;
            httpConnector.Execute(nc);

        }

    }

    private void sendAll(Message msg) {


        if (msg == null) {
            dlog.e("sendAll, Message==null");
            return;
        }


        if (msg.what != ObcService.MSG_CAR_INFO &&
                msg.what != ObcService.MSG_RADIO_SEEK_INFO)
            dlog.i("Sending to " + clients.size() + " clients MSG id " + msg.what);

        try {
            if (clients.size() > 0)
                clients.get(clients.size() - 1).send(msg);
            else {
                if (!_pendingUiCheck) {
                    _pendingUiCheck = true;
                    dlog.d("Empty clients list. Scheduling check in 2sec");
                    localHandler.sendMessageDelayed(localHandler.obtainMessage(ObcService.MSG_UI_CHECK), 2000);
                }
            }
        } catch (RemoteException e1) {

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

        dlog.d("Set displayStatus  : " + on + " delay:" + delay);
        if (on) {
            if (screenLockTrip != null)
                screenLockTrip.acquire();

        } else {
            if (screenLockTrip != null && screenLockTrip.isHeld())
                screenLockTrip.release();
        }

        if (!on && delay > 0) {
            Message msg = MessageFactory.setDisplayStatus(on);
            localHandler.sendMessageDelayed(msg, delay * 1000);
        } else {
            if (obc_io != null) {
                obc_io.setDisplayStatus(null, on);
            }
        }

    }

    // Message routing and handling


    public void obc_ioInit() {
        if (tripInfo != null) {
            if (tripInfo.isOpen) {
                dlog.d("Sending cardCode to OBC_IO: " + tripInfo.cardCode);
                if (!App.motoreAvviato && App.getParkModeStarted() != null && (App.parkMode == ParkMode.PARK_STARTED)) {
                    obc_io.setLed(null, LowLevelInterface.ID_LED_BLUE, LowLevelInterface.ID_LED_BLINK);
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
                dlog.d("Sending cardCode to OBC_IO: '*' ");
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
        dlog.d("OBC_IO booted");
        //Eventi.eventFwBoot(App.fw_version);
        obc_ioInit();
    }

    public void notifyCard(String id, String event, boolean ObcIohandled) {
        notifyCard(id, event, ObcIohandled, false);
    }

    public void notifyCard(String id, String event, boolean ObcIohandled, boolean forced) {
        DbManager dbm = App.Instance.dbManager;
        Customers clienti = dbm.getClientiDao();

        if (id != null && event != null)
            dlog.d(ObcService.class.toString() + " notifyCard: id:" + id + ", event:" + event);
        else
            dlog.e(ObcService.class.toString() + " notifyCard: id or event == null");

        //First notify to all registered consumers the card event

        Message msg = MessageFactory.notifyCard(id, event);
        sendAll(msg);

        //... then schedule a whitelist update, so before PIN dialog we have updated db

        clienti.startWhitelistDownload(this, privateHandler);

        //..finally update properly tripInfo opening or closing trip
        if (tripInfo == null) {
            dlog.e(ObcService.class.toString() + " notifyCard: Tripinfo NULL!");
        } else {
            Message tripMsg = tripInfo.handleCard(id, event, carInfo, obc_io, this, screenLockTrip, forced);
            carInfo.updateTrips();
            if (tripMsg != null) {
                if (tripMsg.what == this.MSG_TRIP_END && App.reservation != null) {
                    setReservation(App.reservation);
                }
                sendAll(tripMsg);
            }
            sendBeacon();
        }
    }

    private boolean firstSOCReceived = false;

    public void notifyCarInfo(Bundle b) {

        if (b == null) {
            dlog.e("notifyCarInfo : bundle == null");
            return;
        }
        if (tripInfo == null) {
            dlog.e("notifyCarInfo : tripinfo == null");
            return;
        }

        if (carInfo == null) {
            carInfo = new CarInfo(localHandler);
            dlog.d("CarInfo created");
        }

        Bundle res = carInfo.betterHandleUpdate(b);

        if (res.getBoolean("force"))
            sendBeacon();
        if (!res.getBoolean("changed"))
            return;

        //TODO: FORZATURA DA RIMUOVERE UNA VOLTA RISOLTO IL PROBLEMA AGGIORNAMENTO DALL AUTO
        if (false && (carInfo.keyStatus.equalsIgnoreCase("ON") || carInfo.keyStatus.equalsIgnoreCase("ACC"))) {

            if (App.motoreAvviato == false) {
                dlog.d("set motore avviato:" + App.motoreAvviato);
                App.motoreAvviato = true;
                App.Instance.persistMotoreAvviato();
            }
        } else {
            if (App.motoreAvviato == true) {
                dlog.d("set motore avviato: " + App.motoreAvviato);
                App.motoreAvviato = false;
                App.Instance.persistMotoreAvviato();
            }

        }

        if (b.containsKey("SOC")) {
            firstSOCReceived = true;
        }


        Long delta = (System.currentTimeMillis() - App.AppStartupTime.getTime()) / 1000;

        if (carInfo.batteryLevel != 0 && carInfo.batteryLevel <= 20 && !App.AlarmSOCSent && delta > 60 && firstSOCReceived) {

            String text = String.format("%s -  %d%% SOC", App.CarPlate, carInfo.batteryLevel);
            if (App.currentTripInfo != null) {
                text += String.format(" - Utente %s %s - Tel: %s", App.currentTripInfo.customer.name, App.currentTripInfo.customer.surname, App.currentTripInfo.customer.mobile);
            } else {
                text += " - Macchina libera";
            }

            //sendSMS(App.BatteryAlarmSmsNumbers,text);

            App.AlarmSOCSent = true;
        } else if (carInfo.batteryLevel > 25) {
            App.AlarmSOCSent = false;
        }

        //If there is no open trip, at lest 3min after boot, no demo kit , battery level < BatteryShutdownLevel% , and no charge plug : do android shutdown.
        if (!this.tripInfo.isOpen &&
                App.getAppRunningSeconds() > 180 &&
                carInfo.batteryLevel != 0 &&
                App.BatteryShutdownLevel > 0 &&
                carInfo.batteryLevel <= App.BatteryShutdownLevel &&
                !carInfo.chargingPlug) {
            dlog.d("Shutdown because of battery: " + carInfo.batteryLevel);
            obc_io.disableWatchdog();
            SystemControl.doShutdown();
        }

        //TODO: ottimizzare con invio messaggio solo se dati effettivamente cambiati.
        Message msg = MessageFactory.notifyCarInfoUpdate(carInfo);
        sendAll(msg);

    }

    public void notifyCANData(Bundle b) {

        if (b == null) {
            dlog.e("notifyCANData : bundle == null");
            return;
        }
        if (tripInfo == null) {
            dlog.e("notifyCANData : tripinfo == null");
            return;
        }

        if (carInfo == null) {
            carInfo = new CarInfo(localHandler);
            dlog.d("CarInfo created");
        }

        if (carInfo.handleUpdate(b))
            sendBeacon();


        if (b.containsKey("PackAmp") && b.containsKey("timestampAmp")) {

            if (!carInfo.chargingPlug) {

                carInfo.currentAmpere += ((double) b.getInt("PackAmp") * 1000) / (double) b.getLong("timestampAmp");
                App.Instance.setCurrentAmp(carInfo.currentAmpere);
            } else {
                carInfo.chargingAmpere += (b.getInt("PackAmp") * 1000) / b.getLong("timestampAmp");
                App.Instance.setChargingAmp(carInfo.chargingAmpere);
            }


        }


        Message msg = MessageFactory.notifyCANDataUpdate(carInfo);
        sendAll(msg);


    }


    public void notifyBatteryInfo(Bundle b) {

        if (b != null) {
            dlog.d("Received battery info: " + b.toString());
        } else {
            dlog.d("Received battery info: NULL bundle");
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
                msg.arg1 = b.getInt("status", 0);
                break;

        }

        if (msg.what != 0) {
            sendAll(msg);
        }

    }

    public void notifyNavigateTo(String label, String json) {
        Double longitude, latitude;
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
                dlog.d(ObcService.class.toString() + " setReservation: Ricevuta nuova prenotazione " + r.toString());

                try {
                    if (FWelcome.Instance.isVisible())
                        FWelcome.Instance.maintenanceBackground(r);
                } catch (Exception e) {
                    dlog.e(ObcService.class.toString() + " setReservation: Exception changing fwelcome background", e);
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
                        dlog.e(ObcService.class.toString() + " setReservation: Exception changing fwelcome background", e);
                    }
                    dlog.d(ObcService.class.toString() + " setReservation: Ricevuta nuova prenotazione vuota, rimuovo quella precedente");
                }
            }
        }

    }

    private void executeServerCommand(String rawCmd) {

        executeServerCommands(ServerCommand.createFromString(rawCmd));

    }

    private void executeServerCommands(List<ServerCommand> list) {
        Customers clienti;

        if (list == null || list.size() == 0)
            return;

        for (ServerCommand cmd : list) {

            dlog.d(ObcService.class.toString() + " executeServerCommands: Executing: " + cmd.command);
            Events.eventCmd(cmd.command + " : " + cmd.intarg1 + " - " + cmd.txtarg1);

            switch (cmd.command.toUpperCase()) {

                case "WLUPDATE":
                    clienti = App.Instance.dbManager.getClientiDao();
                    clienti.startWhitelistDownload(this, privateHandler);
                    break;

                case "WLCLEAN":
                    clienti = App.Instance.dbManager.getClientiDao();
                    clienti.deleteAll();
                    clienti.startWhitelistDownload(this, localHandler);
                    break;

                case "OPEN_TRIP":
                    if (App.currentTripInfo == null)
                        this.notifyCard(cmd.txtarg1, "OPEN", false, false);
                    else
                        dlog.w(ObcService.class.toString() + " executeServerCommands: OPEN_TRIP ignored since there is already an open trip");
                    break;

                case "PARK_TRIP":
                    if (App.currentTripInfo != null && App.getParkModeStarted() != null && !App.parkMode.isOn()) {
                        this.notifyCard(cmd.txtarg1, "PARK", false, false);
                    } else
                        dlog.w(ObcService.class.toString() + " executeServerCommands: PARK_TRIP ignored since there isn't an open trip or not waiting park start");
                    break;

                case "UNPARK_TRIP":
                    if (App.currentTripInfo != null && App.getParkModeStarted() != null && App.parkMode.isOn()) {
                        this.notifyCard(cmd.txtarg1, "UNPARK", false, false);
                    } else
                        dlog.w(ObcService.class.toString() + " executeServerCommands: UNPARK_TRIP ignored since there isn't an open trip or not in park ");
                    break;

                case "CLOSE_TRIP":
                    if (App.currentTripInfo != null) {
                        boolean forced = (cmd.txtarg1 == null || cmd.txtarg1.isEmpty());
                        dlog.d(ObcService.class.toString() + " executeServerCommands: CLOSE_TRIP forced : " + forced);
                        this.notifyCard(App.currentTripInfo.cardCode, "CLOSE", false, forced);
                    } else
                        dlog.w(ObcService.class.toString() + " executeServerCommands: CLOSE_TRIP ignored since there is a no open trip");

                    break;

                case "RESEND_TRIP":
                    Trips corse = App.Instance.getDbManager().getCorseDao();
                    corse.ResetFailed();
                    privateHandler.sendEmptyMessage(Connectors.MSG_TRIPS_SENT_OFFLINE);
                    break;

                case "SET_DOORS":
                    obc_io.setDoors(null, cmd.intarg1, "COMANDO DA  CALL-CENTER");
                    dlog.d(ObcService.class.toString() + " executeServerCommands: Received SET_DOORS command : " + cmd.intarg1);
                    break;

                case "SET_ENGINE":
                    obc_io.setEngine(null, cmd.intarg1);
                    dlog.d(ObcService.class.toString() + " executeServerCommands: Received SET_ENGINE command : " + cmd.intarg1);
                    break;

                case "SET_NAVIGATOR":
                    dlog.d(ObcService.class.toString() + " executeServerCommands: Set navigator: " + cmd.intarg1);
                    App.Instance.setNavigatorEnable(cmd.intarg1 == 1);
                    break;

                case "SET_DAMAGES":
                    dlog.d(ObcService.class.toString() + " executeServerCommands: Set damages: " + cmd.txtarg1);
                    App.Instance.setDamages(cmd.txtarg1);
                    break;

                case "SET_FUELCARD_PIN":
                    dlog.d(ObcService.class.toString() + " executeServerCommands: Set FuelCard PIN");
                    App.Instance.setFuelCardPIN(cmd.txtarg1);
                    break;

                case "OPEN_SERVICE":
                    dlog.d(ObcService.class.toString() + " executeServerCommands: Open service page");
                    Intent i = new Intent(ObcService.this, ServiceTestActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    break;

                case "SEND_LOGS":
                    File file = null;

                    if (cmd.txtarg1 != null && !cmd.txtarg1.isEmpty()) {
                        file = new File(cmd.txtarg1);
                        dlog.d(ObcService.class.toString() + " executeServerCommands: Requested specific file:" + cmd.txtarg1);
                    }

                    UploaderLog.StartLogUploadTask(file, null);
                    break;

                case "SEND_SYSREPORT":
                    ACRA.getErrorReporter().handleSilentException(null);
                    break;

                case "SET_LOCATION":
                    dlog.d(ObcService.class.toString() + " executeServerCommands: Setting location :" + cmd.txtarg1);
                    if (cmd.txtarg1 != null)
                        App.Instance.setMockLocation(cmd.txtarg1);
                    break;

                case "END_CHARGE":
                    dlog.d(ObcService.class.toString() + " executeServerCommands: Remote end charge ");
                    localHandler.sendMessage(MessageFactory.sendEndCharging());
                    break;

                case "ADMINS_UPDATE":
                    dlog.d(ObcService.class.toString() + " executeServerCommands: Update admins cards list");
                    startDownloadAdmins();
                    break;

                case "ADMINS_CLEAN":
                    dlog.d(ObcService.class.toString() + " executeServerCommands: Update admins cards list");
                    startCleanAdmins();
                    break;

                case "SHUTDOWN":
                    dlog.d(ObcService.class.toString() + " executeServerCommands: Received shutdown command");
                    obc_io.disableWatchdog();
                    SystemControl.doShutdown();
                    break;
                case "REBOOT":
                    dlog.d(ObcService.class.toString() + " executeServerCommands: Received reboot command");
                    obc_io.disableWatchdog();
                    SystemControl.doReboot();
                    break;

                case "SET_CONFIG":
                    dlog.d(ObcService.class.toString() + " executeServerCommands: Setting config :" + cmd.txtarg1);
                    App.Instance.setConfig(cmd.txtarg1, cmd.payload);
                    break;

                case "START_JOB":
                    String job = cmd.txtarg1;
                    if (job == null)
                        break;
                    dlog.d(ObcService.class.toString() + " executeServerCommands: Starting job: " + job);
                    if (job.equalsIgnoreCase("TestBatteryAlarm")) {
                        //sendSMS(App.BatteryAlarmSmsNumbers,"Battery alarm test : "+App.CarPlate);
                    } else if (job.equalsIgnoreCase("GetDeviceInfo")) {
                        Events.DeviceInfo(App.Versions.toJson());
                    } else {

                        dlog.e(ObcService.class.toString() + " executeServerCommands: Unknown job");
                    }
                    break;

                case "NAVIGATE_TO":
                    dlog.d(ObcService.class.toString() + " executeServerCommands: Received navigate_to command : " + cmd.txtarg1 + ":" + cmd.payload);
                    notifyNavigateTo(cmd.txtarg1, cmd.payload);
                    break;


            }

        }

    }

    private void parseServerNotify(String payload) {

        if (payload == null || payload.isEmpty() || !payload.contains("{"))
            return;

        JSONObject jobj;
        try {
            jobj = new JSONObject(payload);
        } catch (JSONException e) {
            dlog.e("Error parsing json payload", e);
            return;
        }

        if (jobj.has("reservations")) {
            int n = 0;
            try {
                n = jobj.getInt("reservations");
            } catch (JSONException e) {
                dlog.e("Error getting json 'reservations' field", e);
            }
            if (localHandler != null && n > 0) {
                Message msg = localHandler.obtainMessage(ObcService.MSG_SERVER_NOTIFY);
                msg.arg1 = ObcService.SERVER_NOTIFY_RESERVATION;
                msg.arg2 = n;
                localHandler.sendMessage(msg);
                dlog.d("MSG_SERVER_NOTIFY - SERVER_NOTIFY_RESERVATION sent :" + n);
            }
        }

        if (jobj.has("commands")) {
            int n = 0;
            try {
                n = jobj.getInt("commands");
            } catch (JSONException e) {
                dlog.e("Error getting json 'commands' field", e);
            }
            if (localHandler != null && n > 0) {
                Message msg = localHandler.obtainMessage(ObcService.MSG_SERVER_NOTIFY);
                msg.arg1 = ObcService.SERVER_NOTIFY_COMMAND;
                msg.arg2 = n;
                localHandler.sendMessage(msg);
                dlog.d("MSG_SERVER_NOTIFY - SERVER_NOTIFY_COMMAND sent :" + n);
            }
        }

        if (jobj.has("rawCommand")) {
            try {
                executeServerCommand(jobj.getString("rawCommand"));
            } catch (JSONException e) {
                dlog.e("Invalid rawCommand", e);
            }
        }

    }

    public void startDownloadCustomers() {
        Customers customers = App.Instance.dbManager.getClientiDao();
        customers.startWhitelistDownload(this, privateHandler);
    }

    public void startDownloadReservations() {
        dlog.d("Start Downloading reservations");
        HttpConnector http = new HttpConnector(this);
        http.SetHandler(localHandler);
        ReservationConnector rc = new ReservationConnector();
        rc.setTarga(App.CarPlate);
        http.Execute(rc);
    }


    public void startDownloadCommands() {
        dlog.d("Start Downloading comandi");
        HttpConnector http = new HttpConnector(this);
        http.SetHandler(localHandler);
        CommandsConnector rc = new CommandsConnector();
        rc.setTarga(App.CarPlate);
        http.Execute(rc);

    }


    public void startDownloadAdmins() {
        dlog.d("Start Downloading admins");
        HttpsConnector http = new HttpsConnector(this);
        http.SetHandler(privateHandler);
        AdminsConnector rc = new AdminsConnector();
        rc.setCarPlate(App.CarPlate);
        http.Execute(rc);
    }

    public void startCleanAdmins() {
        obc_io.resetAdminCards();
    }

    public void startDownloadConfigs() {
        dlog.d("Start Downloading configs");
        HttpsConnector http = new HttpsConnector(this);
        http.SetHandler(privateHandler);
        ConfigsConnector rc = new ConfigsConnector();
        rc.setCarPlate(App.CarPlate);
        http.Execute(rc);
    }

    public void notifyServerMessage(int what, int value, String response) {
        dlog.d("Handle notifyServerMessage : " + what + " => " + value);
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

    private void sendSMS(List<String> numbers, String text) {
        for (String number : numbers) {
            sendSMS(number, text);
        }
    }

    private void sendSMS(String number, String text) {


        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(number, null, text, null, null);

    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }

        return data;
    }


    void startRemoteUpdateCycle() {

        stopRemoteUpdateCycle();

        setLocationMode(1000);  // During trips lower min time to 1 sec
        obc_io.setSecondaryGPS(1000);

        tripUpdateScheduler = Executors.newSingleThreadScheduledExecutor();

        tripUpdateScheduler.scheduleAtFixedRate(new Runnable() {

            private boolean _secondRun = false;

            @SuppressWarnings("unused")
            @Override
            public void run() {
                try {
                    // Generate fake gps positions for debug, if required
                    if (Debug.GENERATE_FAKE_GPS) {
                        ObcService.this.sendAll(MessageFactory.sendLocationChange(Debug.getCircleLocation()));
                    }

                    if (Debug.LOG_TRIP_POSITIONS && App.lastLocation != null) {
                        dlog.d("Cooord:" + App.lastLocation.getLatitude() + ":" + App.lastLocation.getLongitude());
                        if (!App.checkParkArea(App.lastLocation.getLatitude(), App.lastLocation.getLongitude()))
                            dlog.d("Outside area");
                    }

                    if (WITH_UDPSERVER)
                        udpServer.sendBeacon(carInfo.getJson(false));

                } catch (Exception e) {
                    dlog.e("Exception inside tripUpdateScheduler", e);
                }

            }

        }, 0, 6, TimeUnit.SECONDS);

        dlog.d("Started remote Update Cycle");
    }

    void stopRemoteUpdateCycle() {

        if (tripUpdateScheduler != null) {
            tripUpdateScheduler.shutdown();
            dlog.d("Stopped remote Update Cycle");
        }

        tripUpdateScheduler = null;

        setLocationMode(30000);  // If car is idle  30sec is min time between locations updates
        obc_io.setSecondaryGPS(10000);
    }


    void startCallCenterCall(Messenger replyTo, String number) {
        try {
            CallCenterConnector ccc = new CallCenterConnector();
            ccc.setTripInfo(App.currentTripInfo);
            ccc.setMobileNumber(number);

            HttpConnector httpConnector = new HttpConnector(this);
            httpConnector.setMessenger(replyTo);
            httpConnector.Execute(ccc);


        } catch (Exception e) {
            dlog.e("startCallCenterCall", e);

        }
    }


    private boolean _pendingUiCheck = false;
    private long _lastRestart;

    private void checkAndRestartUI() {
        dlog.d("Checking UI: clients = " + clients.size());
        if (clients.size() == 0 && (System.currentTimeMillis() - _lastRestart > 15000)) {

            App.Instance.loadPinChecked();

            dlog.d("checkAndRestartUI: userDrunk:" + App.userDrunk);

            if (false && App.userDrunk) {
                dlog.e("Beware! You should not be here");
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
                    dlog.d("Restarting map");
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
                dlog.d("Restarting welcome");
                //Intent i  = new Intent(ObcService.this, ServiceTestActivity.class);
                Intent i = new Intent(ObcService.this, AWelcome.class);
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

        return App.checkParkArea(this.carInfo.latitude, this.carInfo.longitude);
    }

    public void scheduleSelfCloseTrip(int seconds, boolean beforePin) {

        if (App.getParkModeStarted() == null) {
            App.askClose.putInt("id", App.currentTripInfo.trip.remote_id);
            App.askClose.putBoolean("close", true);
            App.Instance.persistAskClose();
        }
        Handler handler = getHandler();
        dlog.d("Schedule selfclose in " + seconds + "secs. BeforePin=" + beforePin);
        Message msg = handler.obtainMessage(ObcService.MSG_TRIP_SELFCLOSE);  //TODO chiudere porte e corsa
        msg.arg1 = beforePin ? 1 : 0;
        msg.obj = (String) "Tempo scaduto";
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

        @Override
        public void onReceive(Context c, Intent i) {


            sendBeacon();

            App.Instance.dbManager.getClientiDao().startWhitelistDownload(ObcService.this, privateHandler);
            App.Instance.dbManager.getPoisDao().startDownload(ObcService.this, localHandler);
            App.Instance.startAreaPolygonDownload(ObcService.this, null);
            startDownloadReservations();

            //Dequeue eventual offline trip or events
            privateHandler.sendEmptyMessage(Connectors.MSG_TRIPS_SENT_OFFLINE);
            privateHandler.sendEmptyMessage(Connectors.MSG_EVENTS_SENT_OFFLINE);

            Message msg = MessageFactory.zmqRestart();
            localHandler.sendMessage(msg);

            SystemControl.ResycNTP();

            if (gpsStatus != null) {
                dlog.d("GPS: tff = " + gpsStatus.getTimeToFirstFix());
                for (GpsSatellite satellite : gpsStatus.getSatellites()) {
                    dlog.d("FPS: sat " + satellite.getSnr() + " " + satellite.usedInFix());
                }
            }

        }
    };

    private final BroadcastReceiver ConnectivityChangeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context c, Intent i) {
            boolean status = SystemControl.hasNetworkConnection(ObcService.this);
            dlog.d("Broadcast CONNECTIVITY_CHANGE" + i.getAction() + status);
            setNetworkConnectionStatus(status);
        }

    };

    public Handler getPrivateHandler() {
        return privateHandler;
    }

    public Handler getHandler() {
        return localHandler;
    }


    private final Handler privateHandler = new Handler() {


        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {

                case Connectors.MSG_TRIPS_SENT_OFFLINE:
                case Connectors.MSG_TRIPS_SENT_REALTIME:
                    Trips corse = App.Instance.getDbManager().getCorseDao();
                    corse.sendOffline(ObcService.this, this);
                    carInfo.updateTrips();
                    break;

                case Connectors.MSG_EVENTS_SENT_OFFLINE:
                    Events eventi = App.Instance.getDbManager().getEventiDao();
                    eventi.spedisciOffline(ObcService.this, this);
                    break;

                case Connectors.MSG_DN_ADMINS:
                    AdminsConnector obj = (AdminsConnector) msg.obj;
                    obc_io.addAdminCard(obj.AdminsList);
                    break;

                case Connectors.MSG_DN_CLIENTI:
                    CustomersConnector cc = (CustomersConnector) msg.obj;
                    if (cc.getReceivedRecords() > 0) {
                        startDownloadCustomers();
                    }
                    break;

                case Connectors.MSG_DN_CONFIGS:
                    ConfigsConnector cfgc = (ConfigsConnector) msg.obj;
                    if (cfgc.ConfigsString != null) {
                        App.Instance.setConfig(cfgc.ConfigsString, null);
                    }

            }


        }

    };


    private final Handler localHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {


            DLog.D("Service: received msg id:" + msg.what);

            switch (msg.what) {

                case MSG_PING:

//				obc_io.RequestWhitelistUpload();

                    DLog.I("Received ping request");
                    Message timeoutMsg = this.obtainMessage(MSG_CMD_TIMEOUT, 5000, -1, msg.replyTo);
                    obc_io.Ping(msg.replyTo, timeoutMsg);
                    this.sendMessageDelayed(timeoutMsg, 1000);
                    break;

                case MSG_CMD_TIMEOUT:
                    DLog.D("Service: CMD timeout");
                    if (msg.obj != null) {
                        try {
                            Messenger m = (Messenger) msg.obj;
                            m.send(Message.obtain(null, MSG_CMD_TIMEOUT));
                        } catch (Exception e) {

                        }
                    }
                    break;


                case MSG_OBC_REINIT:
                    DLog.D("Service: OBC reinit");
                    obc_io.init(true);
                    break;

                case MSG_CLIENT_REGISTER:
                    DLog.I("Received client registration");
                    if (msg.replyTo != null) {
                        clients.add(msg.replyTo);
                        try {
                            msg.replyTo.send(Message.obtain(null, MSG_CLIENT_REGISTER));

                            if (tripInfo.isOpen) {  // Se c'è una corsa attualmente aperta notificalo subito al subscriber
                                Message rmsg = MessageFactory.notifyTripBegin(tripInfo);
                                rmsg.arg1 = 1;
                                msg.replyTo.send(rmsg);
                            }

                        } catch (RemoteException e) {
                            DLog.E("Error sending to client", e);
                        }


                    } else {
                        DLog.E("Failed client registration. Null Messenger");
                    }
                    break;

                case MSG_CLIENT_UNREGISTER:
                    DLog.I("Received client unregistration");
                    if (msg.replyTo != null) {
                        try {
                            msg.replyTo.send(Message.obtain(null, MSG_CLIENT_UNREGISTER));
                        } catch (RemoteException e) {
                            DLog.E("Error sending to client", e);
                        }
                        clients.remove(msg.replyTo);

                        if (clients.size() == 0 && !_pendingUiCheck) {
                            _pendingUiCheck = true;
                            dlog.d("Empty clients list. Scheduling check in 2sec");
                            this.sendMessageDelayed(this.obtainMessage(ObcService.MSG_UI_CHECK), 2000);
                        }

                    } else {
                        DLog.E("Failed client registration. Null Messenger");
                    }

                    break;

                case MSG_UI_CHECK:
                    DLog.I("Received UI Check");
                    checkAndRestartUI();
                    break;

                case MSG_IO_PLATE:
                    DLog.I("Set plate: " + msg.obj);
                    obc_io.setCarPlate(msg.replyTo, (String) msg.obj);
                    break;

                case MSG_IO_LEDS:
                    DLog.I("Set led");
                    obc_io.setLed(msg.replyTo, msg.arg1, msg.arg2);
                    obc_io.setLcd(null, "AUTO IN USO");
                    break;

                case MSG_IO_DOORS:
                    DLog.I("Set doors :" + msg.arg1);
                    obc_io.setDoors(msg.replyTo, msg.arg1, (String) msg.obj);
                    break;

                case MSG_IO_ENGINE:
                    DLog.I("Set engine :" + msg.arg1);
                    obc_io.setEngine(msg.replyTo, msg.arg1);
                    break;

                case MSG_IO_LCD:
                    DLog.I("Set LCD :" + msg.arg1);
                    obc_io.setDisplayStatus(msg.replyTo, msg.arg1 != 0);
                    break;

                case MSG_IO_RESETADMINS:
                    DLog.I("Reset admins");
                    obc_io.resetAdminCards();
                    break;

                case MSG_CAR_INFO:
                    Bundle b = carInfo.getBundle();
                    try {
                        Message rmsg = Message.obtain(null, MSG_CAR_INFO);
                        rmsg.setData(b);
                        msg.replyTo.send(rmsg);
                    } catch (RemoteException e) {
                        DLog.E("Error sending to client", e);
                    }
                    break;

                case MSG_SERVICE_STOP:
                    isStopRequested = true;
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
                        DLog.E("Error sending to client", e);
                    }
                    break;


                case MSG_CUSTOMER_SOS:
                    Events.eventSos((String) msg.obj);
                    startCallCenterCall(msg.replyTo, (String) msg.obj);
                    break;


                case MSG_CUSTOMER_CHECKPIN:
                    try {
                        String pin = (String) msg.obj;
                        Message rmsg = Message.obtain(null, MSG_CUSTOMER_CHECKPIN);
                        rmsg.arg1 = 0;

                        if (tripInfo != null) {
                            boolean isVerified = tripInfo.CheckPin(pin);
                            if (isVerified) {
                                dlog.d("Pin OK");
                                //obc_io.setEngine(rmsg.replyTo, 1);
                                App.isCloseable = false;
                                removeSelfCloseTrip();
                            } else {
                                dlog.w("Pin wrong");
                            }
                            rmsg.arg1 = isVerified ? 1 : 0;
                        }
                        msg.replyTo.send(rmsg);
                    } catch (RemoteException e) {
                        DLog.E("Error sending to client", e);
                    }
                    break;

                case MSG_CAR_REMOTEUPDATECYCLE:
                    if (msg.arg1 == 1) {  //Start
                        startRemoteUpdateCycle();
                    } else {             //Stop
                        stopRemoteUpdateCycle();
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
                                DLog.E("Error sending to client", e);
                            }
                        }
                    }
                    break;


                case MSG_TRIP_EVENT:
                    if (tripInfo != null) {
                        tripInfo.setEvent(msg.arg1, msg.arg2, (String) msg.obj);
                    }
                    break;


                case MSG_TRIP_SELFCLOSE:
                    dlog.d("RECEIVED MSG_TRIP_SELFCLOSE  arg1=" + msg.arg1);
                    if (App.currentTripInfo != null && App.currentTripInfo.isOpen && ((App.getParkModeStarted() == null && App.isCloseable) || App.getParkModeStarted() != null)) {

                        localHandler.sendMessage(MessageFactory.AudioChannel(LowLevelInterface.AUDIO_NONE));
                        Events.selfCloseTrip(App.currentTripInfo.trip.remote_id, msg.arg1);


                        ObcService.this.notifyCard(App.currentTripInfo.cardCode, "CLOSE", false, true);
                    } else {
                        dlog.w("MSG_TRIP_SELFCLOSE discarded");
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
                    String response = ((NotifiesConnector) msg.obj).response;
                    notifyServerMessage(msg.arg1, msg.arg2, response);
                    break;

                case MSG_SERVER_COMMAND:
                    if (msg.obj != null) {
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
                    }
                    break;

                case MSG_SERVER_RESERVATION:
                    ReservationConnector r = null;
                    try {
                        r = (ReservationConnector) msg.obj;
                    } catch (Exception e) {
                        dlog.e("Exception casting reservation:", e);
                        break;
                    }

                    setReservation(r.getReservation());
                    break;

                case MSG_SERVER_CHANGE_IP:
                    App.Instance.ServerIP = msg.arg1;
                    App.Instance.persistServerIP();
                    App.Instance.initSharengo();
                    break;

                case MSG_SERVER_CHANGE_LOG:
                    App.saveLog = (msg.arg1 == 1 ? true : false);
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

                    try {
                        if (msg.arg1 == 0)
                            AudioPlayer.Instance.lastAudioState = msg.arg1;
                    } catch (Exception e) {

                    }
                    break;

                case MSG_RADIO_SET_SEEK:
                    int direction = msg.arg1;
                    boolean auto = msg.arg2 != 0;
                    obc_io.SetSeek(direction, auto);
                    break;

                case MSG_AUDIO_CHANNEL:
                    obc_io.setAudioChannel(msg.arg1);
                    try {

                        if (!AudioPlayer.Instance.reqSystem)                    //controllo se la richiesta non parte da audio player
                            AudioPlayer.Instance.lastAudioState = msg.arg1;

                        if (msg.arg1 == 2)
                            AudioPlayer.Instance.isSystem = true;            //controllo se è stato impostato System
                        else
                            AudioPlayer.Instance.isSystem = false;

                        if (AudioPlayer.Instance.reqSystem)                    //se c'era la richiesta la tolgo
                            AudioPlayer.Instance.reqSystem = false;

                    } catch (Exception e) {

                    }

                    break;

                case MSG_ZMQ_RESTART:
                    if (WITH_ZMQNOTIFY)
                        zmqSubscriber.Restart(localHandler);
                    break;

                case MSG_TRIP_SENDBEACON:
                    sendBeacon();
                    break;

                case MSG_CAR_END_CHARGING:

                    if(!carInfo.chargingPlug)
                        carInfo.setPoiAbilited(false);

                    if (!carInfo.chargingPlug && App.Charging) {
                        App.Charging = false;
                        App.Instance.persistCharging();
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
                    }
                    break;
                case ObcService.MSG_CAR_START_CHARGING:

                    carInfo.setPoiAbilited(checkIfAbilitatedPois(App.lastLocation));
                    if (carInfo.chargingPlug) {
                        carInfo.chargingAmpere = carInfo.currentAmpere;
                    }
                    break;

                case MSG_CAR_LOCATION:
                    //Fan-out message
                    Message nmsg = Message.obtain();
                    nmsg.copyFrom(msg);
                    sendAll(nmsg);
                    break;
                case MSG_DEBUG_CARD:

                    localHandler.sendMessageDelayed(MessageFactory.sendDebugCardOpen((String) msg.obj), 10000);
                    break;

                case MSG_DEBUG_CARD_OPEN:
                    //Fan-out message

                    notifyCard((String) msg.obj, "OPEN", false);
                    break;


            }


        }
    };


    public float[] getCellVoltages() {

        float[] values = new float[24];
        for (int i = 0; i < 24; i++) {
            values[i] = obc_io.getCellVoltageValue(i);
        }


        return values;
    }

    public int getSOCValue() {

        return obc_io.getSOCValue();
    }


    public int getCurrentValue() {

        return (3500 - obc_io.getPackCurrentValue()) / 10;
    }

    public Boolean checkIfAbilitatedPois(Location lastLoc) {

        Pois pois = App.Instance.dbManager.getPoisDao();
        List<Poi> poiList = pois.getPoisAbilitedToCustomer();
        for (Poi poi : poiList) {
            if (lastLoc.distanceTo(poi.getLoc()) < 50) {
                return true;
            }
        }


        return false;


    }


}
