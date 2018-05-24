package eu.philcar.csg.OBC.data.model;

import com.google.gson.Gson;

import java.util.List;

import eu.philcar.csg.OBC.data.datasources.base.BaseResponse;

/**
 * Created by Fulvio on 16/02/2018.
 */

public class Config extends BaseResponse {
    private String BatteryAlarmSMSNumbers;
    private String DefaultCity;
    private String UseExternalGPS;
    private String RadioSetup;
    private String Watchdog;
    private String NewBatteryShutdownLevel;
    private String FleetId;
    private String ServerIP;
    private String TimeZone;
    private String OpenDoorsCards;

    public Config(String batteryAlarmSMSNumbers, String defaultCity, String useExternalGPS, String radioSetup, String watchdog, String newBatteryShutdownLevel, String fleetId, String serverIP, String timeZone, String openDoorsCards) {
        BatteryAlarmSMSNumbers = batteryAlarmSMSNumbers;
        DefaultCity = defaultCity;
        UseExternalGPS = useExternalGPS;
        RadioSetup = radioSetup;
        Watchdog = watchdog;
        NewBatteryShutdownLevel = newBatteryShutdownLevel;
        FleetId = fleetId;
        ServerIP = serverIP;
        TimeZone = timeZone;
        OpenDoorsCards = openDoorsCards;
    }

    public String getBatteryAlarmSMSNumbers() {
        return BatteryAlarmSMSNumbers;
    }

    public void setBatteryAlarmSMSNumbers(String batteryAlarmSMSNumbers) {
        BatteryAlarmSMSNumbers = batteryAlarmSMSNumbers;
    }

    public String getDefaultCity() {
        return DefaultCity;
    }

    public void setDefaultCity(String defaultCity) {
        DefaultCity = defaultCity;
    }

    public String getUseExternalGPS() {
        return UseExternalGPS;
    }

    public void setUseExternalGPS(String useExternalGPS) {
        UseExternalGPS = useExternalGPS;
    }

    public String getRadioSetup() {
        return RadioSetup;
    }

    public void setRadioSetup(String radioSetup) {
        RadioSetup = radioSetup;
    }

    public String getWatchdog() {
        return Watchdog;
    }

    public void setWatchdog(String watchdog) {
        Watchdog = watchdog;
    }

    public String getNewBatteryShutdownLevel() {
        return NewBatteryShutdownLevel;
    }

    public void setNewBatteryShutdownLevel(String newBatteryShutdownLevel) {
        NewBatteryShutdownLevel = newBatteryShutdownLevel;
    }

    public String getFleetId() {
        return FleetId;
    }

    public void setFleetId(String fleetId) {
        FleetId = fleetId;
    }

    public String getServerIP() {
        return ServerIP;
    }

    public void setServerIP(String serverIP) {
        ServerIP = serverIP;
    }

    public String getTimeZone() {
        return TimeZone;
    }

    public void setTimeZone(String timeZone) {
        TimeZone = timeZone;
    }

    public String getOpenDoorsCards() {
        return OpenDoorsCards;
    }

    public void setOpenDoorsCards(String openDoorsCards) {
        OpenDoorsCards = openDoorsCards;
    }
}