package eu.philcar.csg.OBC.service;

import com.Hik.Mercury.SDK.LeaseItaly.LeaseInfoItaly;

import android.location.Location;
import android.os.Bundle;
import android.os.Message;

import eu.philcar.csg.OBC.db.Poi;

public class MessageFactory {

	
	public static final int LED_GREEN = 0;
	public static final int LED_YELLOW = 1;
	public static final int LED_BLUE = 2;
	public static final int LED_RED = 3;
	
	public static final int LED_ON = LeaseInfoItaly.LED_STATUS_LIGHT_ON;
	public static final int LED_OFF = LeaseInfoItaly.LED_STATUS_LIGHT_OFF;
	public static final int LED_FLASH = LeaseInfoItaly.LED_STATUS_LIGHT_FLASH;
	
	public static Message stopService() {
		return Message.obtain(null,ObcService.MSG_SERVICE_STOP);
	}
	
	public static Message ping() {
		return Message.obtain(null, ObcService.MSG_PING);
	}
	
	
	public static Message setPlate(String plate) {
		Message msg  =  Message.obtain(null, ObcService.MSG_IO_PLATE);
		msg.obj =  plate;
		return msg;
	}
	
	public static Message setLed(int led, int  state) {
		Message msg  =  Message.obtain(null, ObcService.MSG_IO_LEDS);
		msg.arg1 = led;
		msg.arg2 = state;
		return msg;
	}
	
	public static Message setEngine(boolean on) {
		Message msg  =  Message.obtain(null, ObcService.MSG_IO_ENGINE);
		msg.arg1= (on?1:0);
		return msg;
	}

	public static Message openDoors() {
		Message msg  =  Message.obtain(null, ObcService.MSG_IO_DOORS);
		msg.arg1= 1;
		return msg;
	}
	public static Message useDefaultIP() {
		Message msg  =  Message.obtain(null, ObcService.MSG_SERVER_CHANGE_IP);
		msg.arg1= 0;
		return msg;
	}
	public static Message useAlternativeIP() {
		Message msg  =  Message.obtain(null, ObcService.MSG_SERVER_CHANGE_IP);
		msg.arg1= 1;
		return msg;
	}
	public static Message enableLog() {
		Message msg  =  Message.obtain(null, ObcService.MSG_SERVER_CHANGE_LOG);
		msg.arg1= 1;
		return msg;
	}
	public static Message enableKeycheck() {
		Message msg  =  Message.obtain(null, ObcService.MSG_CAR_KEY_CHECK);
		msg.arg1= 1;
		return msg;
	}
	public static Message disableKeycheck() {
		Message msg  =  Message.obtain(null, ObcService.MSG_CAR_KEY_CHECK);
		msg.arg1= 0;
		return msg;
	}
	public static Message disableLog() {
		Message msg  =  Message.obtain(null, ObcService.MSG_SERVER_CHANGE_LOG);
		msg.arg1= 0;
		return msg;
	}

	public static Message closeDoors() {
		Message msg  =  Message.obtain(null, ObcService.MSG_IO_DOORS);
		msg.arg1= 0;
		return msg;
	}
	
	public static Message setDisplayStatus(boolean on) {
		Message msg  =  Message.obtain(null, ObcService.MSG_IO_LCD);
		msg.arg1= (on?1:0);
		return msg;
	}
	
	public static Message requestDisplay(boolean on) {
		Message msg  =  Message.obtain(null, ObcService.MSG_IO_LCD);
		msg.arg1= 10+(on?1:0);
		return msg;
	}
	
	public static Message resetAdminCards() {
		Message msg  =  Message.obtain(null, ObcService.MSG_IO_RESETADMINS);
		return msg;
	}
	
	public static Message requestInfo() {
		Message msg  =  Message.obtain(null, ObcService.MSG_IO_GETSTATUS);
		return msg;
	}
	
	public static Message requestCarInfo() {
		Message msg  =  Message.obtain(null, ObcService.MSG_CAR_INFO);	
		return msg;
	}
	
	public static Message requestCallCenterCallSOS(String cellulare) {
		Message msg  =  Message.obtain(null, ObcService.MSG_CUSTOMER_SOS);	
		msg.obj =  cellulare;
		return msg;				
	}

	public static Message requestCallCenterCallDMG(String cellulare) {
		Message msg  =  Message.obtain(null, ObcService.MSG_CUSTOMER_DMG);
		msg.obj =  cellulare;
		return msg;
	}
	
	public static Message checkPin(String pin) {
		Message msg  =  Message.obtain(null, ObcService.MSG_CUSTOMER_CHECKPIN);	
		msg.obj =  pin;
		return msg;		
	}
	
	public static Message notifyCard(String id, String event) {
		
		Message msg  =  Message.obtain(null, ObcService.MSG_IO_RFID);	
		Bundle b = new Bundle();
		b.putString("id", id);
		b.putString("event", event);
		msg.setData(b);
		
		return msg;
	}
	
	
	public static Message notifyTripBegin(TripInfo tripInfo) {
		Message msg  =  Message.obtain(null, ObcService.MSG_TRIP_BEGIN);
		msg.obj = tripInfo;
		
		return msg;		
	}

	public static Message notifyTripEnd(TripInfo tripInfo) {
		Message msg  =  Message.obtain(null, ObcService.MSG_TRIP_END);
		msg.obj = tripInfo;
		
		return msg;		
	}

	public static Message apiTripCallback(TripInfo response) {
		Message msg  =  Message.obtain(null, ObcService.MSG_API_TRIP_CALLBACK);
		msg.obj = response;

		return msg;
	}

	
	public static Message changeTripParkMode(boolean paused) {
		Message msg  =  Message.obtain(null, ObcService.MSG_TRIP_PARK);
		msg.arg1 = (paused?1:0);
		
		return msg;	
	}
	
	public static Message notifyTripParkModeCardBegin() {
		Message msg = Message.obtain(null, ObcService.MSG_TRIP_PARK_CARD_BEGIN);
		return msg;
	}
	
	public static Message notifyTripParkModeCardEnd() {
		Message msg = Message.obtain(null, ObcService.MSG_TRIP_PARK_CARD_END);
		return msg;
	}
	
	
	public static Message requestCarUpdate() {
		Message msg  =  Message.obtain(null, ObcService.MSG_CAR_UPDATE);			
		return msg;
	}
	
	public static Message selfCloseTrip() {
		Message msg  =  Message.obtain(null, ObcService.MSG_TRIP_SELFCLOSE);			
		return msg;	
	}
	
	public static Message startRemoteUpdateCycle() {
		Message msg = Message.obtain(null, ObcService.MSG_CAR_REMOTEUPDATECYCLE);
		msg.arg1 = 1;
		return msg;
	}
	
	public static Message stopRemoteUpdateCycle() {
		Message msg = Message.obtain(null, ObcService.MSG_CAR_REMOTEUPDATECYCLE);
		msg.arg1 = 0;
		return msg;
	}
	
	public static Message scheduleSelfCloseTrip(int seconds) {
		Message msg  =  Message.obtain(null, ObcService.MSG_TRIP_SCHEDULE_SELFCLOSE);			
		msg.arg1 = seconds;
		return msg;	
	}

	public static Message forceCloseTrip() {
		Message msg  =  Message.obtain(null, ObcService.MSG_TRIP_CLOSE_FORCED);
		return msg;
	}
	
	
	public static Message notifyCarInfoUpdate(CarInfo carInfo) {
		Message msg  =  Message.obtain(null, ObcService.MSG_CAR_INFO);	
		msg.obj = carInfo;
		if (carInfo!=null)
			msg.setData(carInfo.getBundle());
		return msg;
	}

	public static Message notifyCANDataUpdate(CarInfo carInfo) {
		Message msg  =  Message.obtain(null, ObcService.MSG_CAR_CAN_UPDATE);
		msg.obj = carInfo;
		if (carInfo!=null)
			msg.setData(carInfo.getBundle());
		return msg;
	}
	public static Message notifyTripPoiUpdate(int status,Poi poi) {
		Message msg = Message.obtain(null, ObcService.MSG_TRIP_NEAR_POI);
		msg.arg1=status;
		msg.obj=poi;
		return msg;
	}

	public static Message notifyStartCharging(CarInfo carInfo) {
		Message msg = Message.obtain(null,ObcService.MSG_CAR_START_CHARGING);
		msg.obj = carInfo;
		return msg;
	}
	
	public static Message AudioChannel(int channel,int volume)  {
		Message msg  =  Message.obtain(null, ObcService.MSG_AUDIO_CHANNEL);			
		msg.arg1 = channel;
		msg.arg2=volume;
		return msg;
	}
	
	public static Message RadioChannel(String band, double freq) {
		Message msg  =  Message.obtain(null, ObcService.MSG_RADIO_SET_CHANNEL);	
		msg.obj = band;
		msg.arg1 = (int)Math.round(freq*1000);
		return msg;
	}
	
	public static Message RadioVolume(int vol) {
		Message msg  =  Message.obtain(null, ObcService.MSG_RADIO_SET_VOLUME);	
		msg.arg1 = vol;
		return msg;
	}
	
	public static Message RadioSeek(int direction,boolean auto) {
		Message msg  =  Message.obtain(null, ObcService.MSG_RADIO_SET_SEEK);	
		msg.arg1 = direction;
		msg.arg2 = (auto?1:0);
		return msg;
	}
	
	
	public static Message zmqRestart() {
		Message msg  =  Message.obtain(null, ObcService.MSG_ZMQ_RESTART);	
		return msg;
	}
	public static Message checkLogSize() {
		Message msg  =  Message.obtain(null, ObcService.MSG_CHECK_LOG_SIZE);
		return msg;
	}
	
	public static Message sendBeacon() {
		Message msg  =  Message.obtain(null, ObcService.MSG_TRIP_SENDBEACON);	
		return msg;
	}
	
	public static Message sendEndCharging() {
		Message msg = Message.obtain(null,ObcService.MSG_CAR_END_CHARGING);
		return msg;
	}

	public static Message sendLocationChange(Location location) {
		Message msg = Message.obtain(null,ObcService.MSG_CAR_LOCATION);
		msg.obj = location;
		return msg;
	}

	public static Message sendDebugCard(String hex) {
		Message msg = Message.obtain(null,ObcService.MSG_DEBUG_CARD);
		msg.obj = hex;
		return msg;
	}

	public static Message sendDebugCardOpen(String hex) {
		Message msg = Message.obtain(null,ObcService.MSG_DEBUG_CARD_OPEN);
		msg.obj = hex;
		return msg;
	}

	public static Message sendTimeCheck () {
		Message msg = Message.obtain(null,ObcService.MSG_CHECK_TIME);
		return msg;
	}

	public static Message failedSOS(){
		Message msg = Message.obtain(null,ObcService.MSG_FAILED_SOS);
		return msg;
	}
	
}
