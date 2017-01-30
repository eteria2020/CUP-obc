package eu.philcar.csg.OBC.devices;

import java.util.List;

import eu.philcar.csg.OBC.service.Reservation;
import android.os.Message;
import android.os.Messenger;

public interface LowLevelInterface {

	public final static int ID_LED_GREEN = 0;
	public final static int ID_LED_BLUE = 1;
	public final static int ID_LED_YELLOW = 2;
	public final static int ID_LED_RED = 3;
	
	
	public final static int ID_LED_OFF = 1;
	public final static int ID_LED_ON = 0;
	public final static int ID_LED_BLINK = 2;
	
	public final static int AUDIO_NONE = 0;
	public final static int AUDIO_RADIO = 1;
	public final static int AUDIO_SYSTEM = 2;
	public final static int AUDIO_AUX = 3;

//  0 :  Green - Free
//  1 :  Blue - in use
//  2 :  orange - booked
//  3 :  red - broken


	
	public void init();
	
	public void init(boolean bond);	
	
	public void close();
	
	
	public void btSend(byte data[]);
	
	public void btSend(ObcCommand cmd) ;
	
	public void btSend(String cmd) ;	
	
	public void Ping(Messenger replyTo, Message timeout);
	
	public void setCarPlate(Messenger replyTo, String plate);
	
	public void setDisplayStatus(Messenger replyTo, boolean on);
	
	public void setLed(Messenger replyTo, int led, int state);
	
	
	public void setDoors(Messenger replyTo, int state, String message);
	
	public void setEngine(Messenger replyTo, int state) ;

	
	public void setLcd(Messenger replyTo, String txt) ;
	
	
	public void setTag(Messenger replyTo, String code) ;
	
	
	public void setCharger(Messenger replyTo, int state);
	
	public void setReservation(Reservation r) ;
	
	public void RequestWhitelistUpload() ;
	
	public void setSecondaryGPS(long period);
	
	public long lastContactDelay() ;

	public float getCellVoltageValue(int index);

	public int getSOCValue();

	public int getPackCurrentValue();

	void addAdminCard(List<String> cards);
	void addAdminCard(String card);
	void delAdminCard(String card);
	void resetAdminCards();

	void setForcedLedBlink(boolean status);

	void setAudioChannel(int channel);
	void SetRadioChannel(String Band, int freq);
	void SetRadioVolume(int vol);

	void SetSeek(int direction, boolean auto);
	
	void enableWatchdog(int secs);
	void disableWatchdog();
	
	
	
	
}
