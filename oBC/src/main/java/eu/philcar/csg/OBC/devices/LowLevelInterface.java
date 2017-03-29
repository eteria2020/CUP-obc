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


	
void init();

void init(boolean bond);

void close();


void btSend(byte data[]);

void btSend(ObcCommand cmd) ;

void btSend(String cmd) ;

void Ping(Messenger replyTo, Message timeout);

void setCarPlate(Messenger replyTo, String plate);

void setDisplayStatus(Messenger replyTo, boolean on);

void setLed(Messenger replyTo, int led, int state);


void setDoors(Messenger replyTo, int state, String message);

void setEngine(Messenger replyTo, int state) ;


void setLcd(Messenger replyTo, String txt) ;


void setTag(Messenger replyTo, String code) ;


void setCharger(Messenger replyTo, int state);

void setReservation(Reservation r) ;

void RequestWhitelistUpload() ;

void setSecondaryGPS(long period);

long lastContactDelay() ;

float getCellVoltageValue(int index);

int getSOCValue();

int getPackCurrentValue();

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
