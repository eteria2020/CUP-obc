package eu.philcar.csg.OBC.data.datasources.repositories;

import android.os.SystemClock;
import android.util.SparseArray;

import javax.inject.Inject;
import javax.inject.Singleton;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.db.Event;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.service.ObcService;

import static eu.philcar.csg.OBC.db.Events.*;

/**
 * Created by Fulvio on 26/02/2018.
 */
@Singleton
public class EventRepository {
    @Inject SharengoApiRepository apiRepository;
    @Inject SharengoPhpRepository phpRepository;


    private SparseArray<String> labels = new SparseArray<>();

    @Inject
    public EventRepository(SharengoApiRepository apiRepository) {
        this.apiRepository = apiRepository;
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
    public  void eventEngine(int state) {
        generateEvent(EVT_ENGINE,state,null);
    }

    public  void eventSwBoot(String version) {
        generateEvent(EVT_SWBOOT,0,version);
    }


    public  void eventRfid(int result, String card) {
        generateEvent(EVT_RFID,result,card);
    }

    public  void eventBattery(int value) {
        generateEvent(EVT_BATTERY,value,null);
    }

    public  void eventParkBegin() {
        generateEvent(EVT_PARK,1,null);
    }

    public  void eventParkEnd() {
        generateEvent(EVT_PARK,2,null);
    }

    public  void eventSos(String number) {
        generateEvent(EVT_SOS,1,number);
    }
    public  void eventSos(String number, ObcService service) {
        DLog.CR("Prenotata chiamata asistenza: " + number);
        generateEvent(EVT_SOS,1,number, service);
    }

    public  void eventDmg(String number) {
        generateEvent(EVT_SOS,2,number);
    }

    public  void eventCmd(String cmd) {
        generateEvent(EVT_CMD,0,cmd);
    }

    public  void eventCleanliness(int intVal, int extVal) {
        generateEvent(EVT_CLEANLINESS,0,intVal+";"+extVal);
    }

    public  void eventCharge(int status) {
        generateEvent(EVT_CHARGE,status,null);
    }

    public  void eventKey(int status) {
        generateEvent(EVT_KEY,status,null);
    }

    public  void eventReady(int status) {
        generateEvent(EVT_READY,status,null);
    }

    public  void outOfArea(Boolean status) {
        if(App.getLastLocation().getLongitude()!=0.0 && App.getLastLocation().getLatitude()!=0.0)
            generateEvent(EVT_OUTOFAREA,status?1:0,status?"Uscita Area Operativa":"Rientro Area Operativa");
    }

    public  void menuclick(String clicked) {
        generateEvent(EVT_MENU_CLICK,0,"Click on "+clicked);
    }

    public  void eventGear(String position) {
        generateEvent(EVT_GEAR,0,position);
    }

    public  void eventObcFail(long delay) {
        int idelay=0;
        if (delay > Integer.MAX_VALUE)
            idelay = Integer.MAX_VALUE;
        else
            idelay = (int) delay;
        generateEvent(EVT_OBCFAIL,idelay,null);
    }

    public  void eventObcOk() {
        generateEvent(EVT_OBCOK,0,null);
    }

    public  void DiagnosticPage(int pwd) {
        generateEvent(EVT_DIAG,pwd,null);
    }

    public  void DiagnosticPageFail(String pwd) {
        generateEvent(EVT_DIAG,-1,pwd);
    }

    public  void CarPlateChange(String oldPlate, String newPlate) {
        generateEvent(EVT_CARPLATE,0,oldPlate+"->"+newPlate);
    }

    public  void Restart3G(int count, String network) {
        generateEvent(EVT_3G,count,network);
    }

    public  void Maintenance(String action) {
        generateEvent(EVT_MAINTENANCE,0,action);
    }


    public  void BeginOutOfOrder(String reason) {
        generateEvent(EVT_OUTOFORDER,1,reason);
    }

    public  void EndOutOfOrder() {
        generateEvent(EVT_OUTOFORDER,2,null);
    }

    public  void TripOutOfOrder(String card) {
        generateEvent(EVT_OUTOFORDER,3,card);
    }

    public  void selfCloseTrip(int idTrip,int beforePin) {
        generateEvent(EVT_SELFCLOSE,idTrip,(beforePin>0?"NOPAY":null));
    }

    public  void DeviceInfo(String versions) {
        generateEvent(EVT_DEVICEINFO,App.id_Version,versions);
    }
    public  void eventSoc(int level, String type) {
        generateEvent(EVT_SOC,level,type);
    }

    public  void Shutdown() {
        generateEvent(EVT_SHUTDOWN,0,"Shutting Down");
    }

    public  void StartShutdown() {
        generateEvent(EVT_SHUTDOWN,0,"Start Shutdown timer");
    }

    public  void StopShutdown() {
        generateEvent(EVT_SHUTDOWN,0,"Stop Shutdown timer");
    }
    public  void Reboot(String text) {
        generateEvent(EVT_REBOOT,0,text+" " +App.sw_Version);
    }


    public  void CanAnomalies(String text) {
        if(SystemClock.elapsedRealtime()-lastCanAnomalies>60*60*1000) {
            lastCanAnomalies=SystemClock.elapsedRealtime();
            generateEvent(EVT_CAN_ANOMALIES, 0, text);
        }
    }

    public  void LeaseInfo(int status,int card) {
        String Hex=Integer.toHexString(card);
        generateEvent(EVT_LEASE,status,Hex);
    }


    public void generateEvent(int eventId, int intValue, String strValue){
        generateEvent(eventId, intValue, strValue, null);
    }

    public void generateEvent(int eventId, int intValue, String strValue, ObcService service) {
       Event event = new Event();
        event.timestamp = System.currentTimeMillis();

        event.event = eventId;

        if (labels.get(eventId)!=null)
            event.label = labels.get(eventId);

        event.intval = intValue;
        event.txtval = strValue==null?"":strValue;

        event.km = App.km;
        event.battery = App.fuel_level;

        if (App.currentTripInfo!=null  && App.currentTripInfo.trip!=null) {
            event.level = App.currentTripInfo.trip.id;
            event.id_trip = App.currentTripInfo.trip.remote_id;
            event.id_customer = App.currentTripInfo.trip.id_customer;
        }

        if (App.getLastLocation() !=null) {
            event.lon = App.getLastLocation().getLongitude();
            event.lat = App.getLastLocation().getLatitude();
        }
        DLog.CR("Invio evento " +event.toStringOneLine());
        if(App.fullNode)
            apiRepository.sendEvent(event,service);
        else
            phpRepository.sendEvent(event);

    }

}
