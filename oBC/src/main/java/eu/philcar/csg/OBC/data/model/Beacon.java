package eu.philcar.csg.OBC.data.model;

import android.os.Bundle;

import com.google.gson.Gson;

import java.lang.reflect.Field;

import eu.philcar.csg.OBC.data.common.ExcludeSerialization;

/**
 * Created by Fulvio on 13/03/2018.
 */

public class Beacon {

    //CarInfo

    private String IMEI;
    private String Versions;
    private double ext_lon;
    private double ext_lat;
    private long ext_time;
    private double int_lon;
    private double int_lat;
    private long int_time;
    private double lon;
    private double lat;
    private String fwVer;
    private String hwVer;
    private String swVer;
    private String SDK;
    private String sdkVer;
    private String gsmVer;
    private String AndroidDevice;
    private String AndroidBuild;
    private String TBoxSw;
    private String TBoxHw;
    private String MCUModel;
    private String MCU;
    private String HwVersion;
    private String HbVer;
    private String VehicleType;
    private String GPS;

    //Cars

    private String VIN;
    private Boolean parking;
    private Boolean parkEnabled;
    private Boolean batterySafety;
    private Boolean ReadyOn;
    private long wlsize;
    private Boolean charging;
    private String KeyStatus;
    private String gps_info;
    private Boolean PPStatus = false;
    private Boolean noGPS;
    private int SOC;
    private int Speed;
    private int Km;
    private int on_trip;
    private long id_trip;
    private int offLineTrips;
    private int PackV;
    private Boolean closeEnabled;
    private String log_tx_time;
    @ExcludeSerialization
    private int keyOn;
    @ExcludeSerialization
    private int PackAmp;
    @ExcludeSerialization
    private long timestampAmp;
    @ExcludeSerialization
    private float MotV;
    @ExcludeSerialization
    private String GearStatus;
    @ExcludeSerialization
    private Boolean BrakesOn;
    @ExcludeSerialization
    private String clock;
    @ExcludeSerialization
    private String SIM_SN;
    @ExcludeSerialization
    private int openTrips;
    @ExcludeSerialization
    private String GPSBOX;

    public String getGPSBOX() {
        return GPSBOX;
    }

    public void setGPSBOX(String GPSBOX) {
        this.GPSBOX = GPSBOX;
    }

    public int getOpenTrips() {
        return openTrips;
    }

    public void setOpenTrips(int openTrips) {
        this.openTrips = openTrips;
    }

    public String getSIM_SN() {
        return SIM_SN;
    }

    public void setSIM_SN(String SIM_SN) {
        this.SIM_SN = SIM_SN;
    }

    public String getClock() {
        return clock;
    }

    public void setClock(String clock) {
        this.clock = clock;
    }

    public String getLog_tx_time() {
        return log_tx_time;
    }

    public void setLog_tx_time(String log_tx_time) {
        this.log_tx_time = log_tx_time;
    }

    public long getExt_time() {
        return ext_time;
    }

    public void setExt_time(long ext_time) {
        this.ext_time = ext_time;
    }

    public long getInt_time() {
        return int_time;
    }

    public void setInt_time(long int_time) {
        this.int_time = int_time;
    }

    public Boolean getCloseEnabled() {
        return closeEnabled;
    }

    public void setCloseEnabled(Boolean closeEnabled) {
        this.closeEnabled = closeEnabled;
    }

    public Boolean getBrakesOn() {
        return BrakesOn;
    }

    public void setBrakesOn(Boolean brakesOn) {
        BrakesOn = brakesOn;
    }

    public String getGearStatus() {
        return GearStatus;
    }

    public void setGearStatus(String gearStatus) {
        GearStatus = gearStatus;
    }

    public String getVIN() {
        return VIN;
    }

    public void setVIN(String VIN) {
        this.VIN = VIN;
    }

    public String getIMEI() {
        return IMEI;
    }

    public void setIMEI(String IMEI) {
        this.IMEI = IMEI;
    }

    public String getVersions() {
        return Versions;
    }

    public void setVersions(String versions) {
        Versions = versions;
    }

    public double getExt_lon() {
        return ext_lon;
    }

    public void setExt_lon(double ext_lon) {
        this.ext_lon = ext_lon;
    }

    public double getExt_lat() {
        return ext_lat;
    }

    public void setExt_lat(double ext_lat) {
        this.ext_lat = ext_lat;
    }

    public double getInt_lon() {
        return int_lon;
    }

    public void setInt_lon(double int_lon) {
        this.int_lon = int_lon;
    }

    public double getInt_lat() {
        return int_lat;
    }

    public void setInt_lat(double int_lat) {
        this.int_lat = int_lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public String getFwVer() {
        return fwVer;
    }

    public void setFwVer(String fwVer) {
        this.fwVer = fwVer;
    }

    public String getHwVer() {
        return hwVer;
    }

    public void setHwVer(String hwVer) {
        this.hwVer = hwVer;
    }

    public String getSwVer() {
        return swVer;
    }

    public void setSwVer(String swVer) {
        this.swVer = swVer;
    }

    public String getSDK() {
        return SDK;
    }

    public void setSDK(String SDK) {
        this.SDK = SDK;
    }

    public String getSdkVer() {
        return sdkVer;
    }

    public void setSdkVer(String sdkVer) {
        this.sdkVer = sdkVer;
    }

    public String getGsmVer() {
        return gsmVer;
    }

    public void setGsmVer(String gsmVer) {
        this.gsmVer = gsmVer;
    }

    public String getAndroidDevice() {
        return AndroidDevice;
    }

    public void setAndroidDevice(String androidDevice) {
        AndroidDevice = androidDevice;
    }

    public String getAndroidBuild() {
        return AndroidBuild;
    }

    public void setAndroidBuild(String androidBuild) {
        AndroidBuild = androidBuild;
    }

    public String getTBoxSw() {
        return TBoxSw;
    }

    public void setTBoxSw(String TBoxSw) {
        this.TBoxSw = TBoxSw;
    }

    public String getTBoxHw() {
        return TBoxHw;
    }

    public void setTBoxHw(String TBoxHw) {
        this.TBoxHw = TBoxHw;
    }

    public String getMCUModel() {
        return MCUModel;
    }

    public void setMCUModel(String MCUModel) {
        this.MCUModel = MCUModel;
    }

    public String getMCU() {
        return MCU;
    }

    public void setMCU(String MCU) {
        this.MCU = MCU;
    }

    public String getHwVersion() {
        return HwVersion;
    }

    public void setHwVersion(String hwVersion) {
        HwVersion = hwVersion;
    }

    public String getHbVer() {
        return HbVer;
    }

    public void setHbVer(String hbVer) {
        HbVer = hbVer;
    }

    public String getVehicleType() {
        return VehicleType;
    }

    public void setVehicleType(String vehicleType) {
        VehicleType = vehicleType;
    }

    public String getGPS() {
        return GPS;
    }

    public void setGPS(String GPS) {
        this.GPS = GPS;
    }

    public Boolean getParking() {
        return parking;
    }

    public void setParking(Boolean parking) {
        this.parking = parking;
    }

    public Boolean getParkEnabled() {
        return parkEnabled;
    }

    public void setParkEnabled(Boolean parkEnabled) {
        this.parkEnabled = parkEnabled;
    }

    public Boolean getBatterySafety() {
        return batterySafety;
    }

    public void setBatterySafety(Boolean batterySafety) {
        this.batterySafety = batterySafety;
    }

    public Boolean getReadyOn() {
        return ReadyOn;
    }

    public void setReadyOn(Boolean readyOn) {
        ReadyOn = readyOn;
    }

    public long getWlsize() {
        return wlsize;
    }

    public void setWlsize(long wlsize) {
        this.wlsize = wlsize;
    }

    public Boolean getCharging() {
        return charging;
    }

    public void setCharging(Boolean charging) {
        this.charging = charging;
    }

    public String getKeyStatus() {
        return KeyStatus;
    }

    public void setKeyStatus(String keyStatus) {
        KeyStatus = keyStatus;
    }

    public String getGps_info() {
        return gps_info;
    }

    public void setGps_info(String gps_info) {
        this.gps_info = gps_info;
    }

    public Boolean getPPStatus() {
        return PPStatus;
    }

    public void setPPStatus(Boolean PPStatus) {
        this.PPStatus = PPStatus;
    }

    public Boolean getNoGPS() {
        return noGPS;
    }

    public void setNoGPS(Boolean noGPS) {
        this.noGPS = noGPS;
    }

    public int getSOC() {
        return SOC;
    }

    public void setSOC(int SOC) {
        this.SOC = SOC;
    }

    public int getSpeed() {
        return Speed;
    }

    public void setSpeed(int speed) {
        Speed = speed;
    }

    public int getKm() {
        return Km;
    }

    public void setKm(int km) {
        Km = km;
    }

    public int getOn_trip() {
        return on_trip;
    }

    public void setOn_trip(int on_trip) {
        this.on_trip = on_trip;
    }

    public long getId_trip() {
        return id_trip;
    }

    public void setId_trip(long id_trip) {
        this.id_trip = id_trip;
    }

    public int getOffLineTrips() {
        return offLineTrips;
    }

    public void setOffLineTrips(int offLineTrips) {
        this.offLineTrips = offLineTrips;
    }

    public int getPackV() {
        return PackV;
    }

    public void setPackV(int packV) {
        PackV = packV;
    }

    public int getKeyOn() {
        return keyOn;
    }

    public void setKeyOn(int keyOn) {
        this.keyOn = keyOn;
    }

    public int getPackAmp() {
        return PackAmp;
    }

    public void setPackAmp(int packAmp) {
        PackAmp = packAmp;
    }

    public long getTimestampAmp() {
        return timestampAmp;
    }

    public void setTimestampAmp(long timestampAmp) {
        this.timestampAmp = timestampAmp;
    }

    public float getMotV() {
        return MotV;
    }

    public void setMotV(float motV) {
        MotV = motV;
    }

    public static Beacon handleUpdate(Beacon beacon, Bundle b) {
        Gson gson = new Gson();
        try {
            Beacon received = gson.fromJson(gson.toJson(b), Beacon.class);
            return mergeObjects(beacon, received);
        } catch (Exception e) {
            return beacon;
        }
    }

    public static <T> T mergeObjects(T first, T second) throws IllegalAccessException, InstantiationException {
        Class<?> clazz = first.getClass();
        Field[] fields = clazz.getDeclaredFields();
        Object returnValue = clazz.newInstance();
        for (Field field : fields) {
            field.setAccessible(true);
            Object value1 = field.get(first);
            Object value2 = field.get(second);
            Object value = (value1 != null) ? value1 : value2;
            field.set(returnValue, value);
        }
        return (T) returnValue;
    }
}
