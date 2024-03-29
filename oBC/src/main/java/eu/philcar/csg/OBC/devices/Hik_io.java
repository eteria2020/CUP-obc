package eu.philcar.csg.OBC.devices;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
//import android.support.v4.app.FragmentManager;

import com.Hik.Mercury.SDK.Audio.AudioInfo;
import com.Hik.Mercury.SDK.Audio.AudioObserver;
import com.Hik.Mercury.SDK.CAN.BatteryInfo;
import com.Hik.Mercury.SDK.CAN.BatteryInfoChangeObserver;
import com.Hik.Mercury.SDK.CAN.SDKConstants;
import com.Hik.Mercury.SDK.CAN.VehicleInfo;
import com.Hik.Mercury.SDK.CAN.VehicleInfoChangeObserver;
import com.Hik.Mercury.SDK.GPS.GPSInfoParcel;
import com.Hik.Mercury.SDK.Lease.LeaseObserver;
import com.Hik.Mercury.SDK.LeaseItaly.LeaseInfoItaly;
import com.Hik.Mercury.SDK.LeaseItaly.LeaseObserverItaly;
import com.Hik.Mercury.SDK.Manager.CANManager;
import com.Hik.Mercury.SDK.Manager.HIKAudioManager;
import com.Hik.Mercury.SDK.Manager.LeaseManager;
import com.Hik.Mercury.SDK.Manager.LeaseManagerItaly;
import com.Hik.Mercury.SDK.Manager.RadioManager;
import com.Hik.Mercury.SDK.Manager.VersionManager;
import com.Hik.Mercury.SDK.Radio.RadioInfo;
import com.Hik.Mercury.SDK.Radio.RadioObserver;
import com.Hik.Mercury.SDK.bluetooth.BTManager;
import com.Hik.Mercury.SDK.bluetooth.BluetoothStateCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import javax.inject.Inject;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.BuildConfig;
import eu.philcar.csg.OBC.data.datasources.repositories.EventRepository;
import eu.philcar.csg.OBC.helpers.AudioPlayer;
import eu.philcar.csg.OBC.helpers.Converts;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.Debug;
import eu.philcar.csg.OBC.helpers.ProTTS;
import eu.philcar.csg.OBC.service.ObcService;
import eu.philcar.csg.OBC.service.Reservation;
import eu.philcar.csg.OBC.task.OdoController;
import eu.philcar.csg.OBC.task.OptimizeDistanceCalc;

//import eu.philcar.csg.OBC.task.DataLogger;

//import eu.philcar.csg.OBC.R;

//Led
//  0 :  Green - Free
//  1 :  Blue - in use
//  2 :  orange - booked
//  3 :  red - broken

public class Hik_io implements LowLevelInterface {

	private DLog dlog = new DLog(this.getClass());

	private static final boolean WITH_RADIO = true;

	@Inject
	EventRepository eventRepository;

	private CANManager mCANManager;
	private BTManager mBTManager;
	private LeaseManagerItaly mLeaseManagerItaly;
	private LeaseManager mLeaseManager;
	private RadioManager mRadioManager;
	private HIKAudioManager mAudioManager;

	private Context context;

	private ObcService obcService;
//	private ObcParser obcParser;
	private boolean displayStatus = true;
	private byte[][] ledStatuses = new byte[4][2];

	private Thread threadKeepAlive;
	private Thread threadGpsUpdate;
	private boolean watchdogActive = false;

	private long lastContact = 0;

	private boolean forceLedBlinking;

	private String lastGpsInfo = "";

	private String SDKVersion = "";
//	private int ServiceVersion = -1;

//	private FragmentManager getSupportFragmentManager;

	private boolean getMinorGPSDisabled = false;

	public Hik_io(ObcService service) {
		context = service;

		obcService = service;
//		obcParser = new ObcParser(obcService);

		ledStatuses[LeaseInfoItaly.LED_INDEX_LED1][0] = LeaseInfoItaly.LED_INDEX_LED1;
		ledStatuses[LeaseInfoItaly.LED_INDEX_LED2][0] = LeaseInfoItaly.LED_INDEX_LED2;
		ledStatuses[LeaseInfoItaly.LED_INDEX_LED3][0] = LeaseInfoItaly.LED_INDEX_LED3;
		ledStatuses[LeaseInfoItaly.LED_INDEX_LED4][0] = LeaseInfoItaly.LED_INDEX_LED4;

		ledStatuses[LeaseInfoItaly.LED_INDEX_LED1][1] = LeaseInfoItaly.LED_STATUS_LIGHT_OFF;
		ledStatuses[LeaseInfoItaly.LED_INDEX_LED2][1] = LeaseInfoItaly.LED_STATUS_LIGHT_OFF;
		ledStatuses[LeaseInfoItaly.LED_INDEX_LED3][1] = LeaseInfoItaly.LED_STATUS_LIGHT_OFF;
		ledStatuses[LeaseInfoItaly.LED_INDEX_LED4][1] = LeaseInfoItaly.LED_STATUS_LIGHT_OFF;

		App.get(service).getComponent().inject(this);
	}

	@Override
	public void init() {

		dlog.i("Hik_io.init();request");

		mCANManager = CANManager.get(context);

		if (mCANManager != null) {
			mCANManager.attachBatteryInfoObserver(mBatteryObserver);
			mCANManager.attachVehicleInfoObserver(mVehicleObserver);
			App.Versions.SDK = mCANManager.getSDKVersion();
			SDKVersion = App.Versions.SDK;
			dlog.i("Hik_io.init();SDK version:" + SDKVersion);
		}

		mBTManager = BTManager.get(context);
		if (mBTManager != null) {

			mBTManager.switchOn();
			mBTManager.isBTConnected();
			//mBTManager.enterPairMode();
			mBTManager.registerAdapter(mBTObserver);
		}

		mLeaseManagerItaly = LeaseManagerItaly.get(context);
		if (mLeaseManagerItaly != null) {
			mLeaseManagerItaly.attachLeaseInfoObserver(mLeaseObserverItaly);
		}

		mLeaseManager = LeaseManager.get(context);
		if (mLeaseManager != null) {
			mLeaseManager.attachLeaseInfoObserver(mLeaseObserver);
//			int status = mLeaseManager.GetLeaseStatus();
//			int memberId = mLeaseManager.GetLeaseMerberID();
			//eventRepository.LeaseInfo(status, memberId);
		}

		if (WITH_RADIO) {

			mAudioManager = HIKAudioManager.get(context);

			if (mAudioManager != null) {
				mAudioManager.audioSetChannel(AudioInfo.HIK_AUDIO_CHANNEL_RADIO);
				mAudioManager.audioSetVol(0);
				mAudioManager.AudioSetAMP(AudioInfo.AUDIO_AMP_STATUS_OFF);
				mAudioManager.attachAudioObserver(mAudioObserver);
			}

			mRadioManager = RadioManager.get(context);
			if (mRadioManager != null) {
				mRadioManager.radioInit();
				mRadioManager.attachRadioObserver(mRadioObserver);
			}
		}

		int ServiceVersion = getServiceVersion();
		App.Versions.Service = ServiceVersion;
		dlog.i("Hik_io.init();SDK SERVICE VERSION: " + ServiceVersion);

		try {
			if (App.Versions.getLevel() > 0) {
				VersionManager versionManager = VersionManager.get(context);

				App.Versions.MCU = versionManager.getMCUSoftwareVersion();
				App.Versions.Release = versionManager.getReleaseVersion();
				App.Versions.VehicleType = versionManager.getVehicleType();
				App.Versions.DeviceSN = versionManager.requestDeviceSN();
				App.Versions.HwVersion = versionManager.requestHwVersion();
				App.Versions.MCUModel = versionManager.requestMCUModel();
				App.Versions.TBoxHw = versionManager.requestTBoxHwVersion();
				App.Versions.TBoxSw = versionManager.requestTBoxSwVersion();
				App.Versions.VINCode = versionManager.requestVINCode();
			}
		} catch (Exception e) {
			dlog.e("Hik_io.init();Exception while retrieving version information", e);
		}

		//mCANManager.getAllVehicleInfo();

		//this.resetAdminCards();
		//this.addAdminCard("9DCAE1E4");

		resetLastContact();

		threadKeepAlive = new Thread() {
			public void run() {
				int prescaler = 6;
				dlog.i("Hik_io.init();threadKeepAlive started");
				while (!this.isInterrupted()) {

					setDisplayStatus(null, displayStatus);

					for (int i = 0; i < 4; i++) {
						if (ledStatuses[i][1] != LeaseInfoItaly.LED_STATUS_LIGHT_OFF)
							setLed(null, ledStatuses[i][0], ledStatuses[i][1]);
					}

					if (prescaler-- == 0) {
						dlog.i("Hik_io.init(); Prescaler fired");
						refreshInfo();
						prescaler = 6;
					}

					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						break;
					}
				}
			}
		};

		threadKeepAlive.setName("Hik_io.init();KeepAlive");
		threadKeepAlive.start();

		setSecondaryGPS(10000);

		enableWatchdog(App.Watchdog);

	}

	@Override
	public void init(boolean bond) {
		init();
	}

	@Override
	public void close() {
		if (mCANManager != null) {
			mCANManager.detachBatteryInfoObserver(mBatteryObserver);
			mCANManager.detachVehicleInfoObserver(mVehicleObserver);
		}

		if (mLeaseManagerItaly != null) {
			mLeaseManagerItaly.detachLeaseInfoObserver(mLeaseObserverItaly);
		}

		if (mAudioManager != null) {
			mAudioManager.detachAudioObserver(mAudioObserver);
		}

		if (mRadioManager != null) {
			mRadioManager.detachRadioObserver(mRadioObserver);
		}
		threadKeepAlive.interrupt();

	}

	@Override
	public void btSend(byte[] data) {
		// TODO Auto-generated method stub

	}

	@Override
	public void btSend(ObcCommand cmd) {
		// TODO Auto-generated method stub

	}

	@Override
	public void btSend(String cmd) {
		// TODO Auto-generated method stub

	}

	@Override
	public void Ping(Messenger replyTo, Message timeout) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setForcedLedBlink(boolean status) {
		if (this.forceLedBlinking == status)
			return;

		dlog.i("Hik_io.setForcedLedBlink();" + status);
		this.forceLedBlinking = status;
	}

	@Override
	public void setLed(Messenger replyTo, int led, int status) {

		if (led < 0 || led > 3)
			return;

		//If we receive a led on or blink command ensure all the others are off
		if (status != LeaseInfoItaly.LED_STATUS_LIGHT_OFF) {
			for (int i = 0; i < 4; i++) {
				ledStatuses[i][1] = (led == i) ? (byte) status : LeaseInfoItaly.LED_STATUS_LIGHT_OFF;
			}
		}

		ledStatuses[led][1] = (byte) status;

		byte[] data = ledStatuses[led].clone();

		if (mLeaseManagerItaly != null) {

			if (this.forceLedBlinking && data[1] == LeaseInfoItaly.LED_STATUS_LIGHT_ON){
				data[1] = LeaseInfoItaly.LED_STATUS_LIGHT_FLASH;
			}

			//boolean result = mLeaseManagerItaly.SetLEDStatus(data);
			//dlog.i("setLed Led=" + data[0] + ", state="+data[1] + " result=" + result);
			dlog.i("Hik_io.setLed();Led=" + data[0] + ", state="+data[1] + " result=" + mLeaseManagerItaly.SetLEDStatus(data));
		}

	}

	@Override
	public void setDoors(Messenger replyTo, int state, String message) {
		dlog.i("Hik_io.setDoors();setDoors :" + state);
		dlog.cr("Hik_io.setDoors();Ricevuto messaggio per azione su porte: " + state);
		int ctl = (state == 0 ? LeaseInfoItaly.LEASE_CTL_DOOR_CLOSE : LeaseInfoItaly.LEASE_CTL_DOOR_OPEN);
		if (mLeaseManagerItaly != null) {
			mLeaseManagerItaly.SetDoorStatus(ctl);
		}
	}

	@Override
	public void setEngine(Messenger replyTo, int status) {
		dlog.d("Hik_io.setEngine();" + status);
		dlog.cr("Hik_io.setEngine();Ricevuta azione sul motore: " + status);
		int ctl = (status == 0 ? LeaseInfoItaly.LEASE_CTL_DISABLE_POWER : LeaseInfoItaly.LEASE_CTL_ENABLE_POWER);
		if (mLeaseManagerItaly != null) {
			if (mLeaseManagerItaly.SetPowerStatus(ctl))
				DLog.I("Hik_io.setEngine();success ");
			else
				DLog.I("Hik_io.setEngine();failed ");
		} else
			DLog.I("Hik_io.setEngine();failed ");
	}

	@Override
	public void setDisplayStatus(Messenger replyTo, boolean on) {
		int ctl = (on ? LeaseInfoItaly.LEASE_CAR_STATUS_LEASING : LeaseInfoItaly.LEASE_CAR_STATUS_IDLE);
		displayStatus = on || Debug.FORCE_BACKLIGHT_ON;
		if (mLeaseManagerItaly != null) {
			mLeaseManagerItaly.SetLeaseStatus(ctl);
			//dlog.i("setLeaseStatus=" + ctl);
		}
	}

	@Override
	public void setCarPlate(Messenger replyTo, String plate) {

		byte[] data = plate.getBytes();

		if (mLeaseManagerItaly != null) {
			mLeaseManagerItaly.SetConfigs(data);
		}
	}

	@Override
	public void setLcd(Messenger replyTo, String txt) {
	}

	@Override
	public void setTag(Messenger replyTo, String code) {
	}

	@Override
	public void setCharger(Messenger replyTo, int state) {
	}

	@Override
	public void setReservation(Reservation r) {
		if (r != null) {
			if (r.duration > 0)
				setLed(null, LeaseInfoItaly.LED_INDEX_LED3, LeaseInfoItaly.LED_STATUS_LIGHT_ON);
			else
				setLed(null, LeaseInfoItaly.LED_INDEX_LED4, LeaseInfoItaly.LED_STATUS_LIGHT_ON);
		} else {
			setLed(null, LeaseInfoItaly.LED_INDEX_LED3, LeaseInfoItaly.LED_STATUS_LIGHT_OFF);
			setLed(null, LeaseInfoItaly.LED_INDEX_LED1, LeaseInfoItaly.LED_STATUS_LIGHT_ON);
		}

	}

	@Override
	public void addAdminCard(List<String> cards) {
		if (cards == null) {
			dlog.e("Hik_io.addAdminCard();Null admins cards list. Skipping");
			return;
		}

		if (cards.size() == 0) {
			dlog.e("Hik_io.addAdminCard();Empty admins cards list. Skipping");
			return;
		}

		//Suspect operation ... in case to be delayed

		//resetAdminCards();

		for (String card : cards) {
			addAdminCard(card);
		}

		dlog.i("Hik_io.addAdminCard();Added N." + cards.size() + " cards");

	}

	@Override
	public void addAdminCard(String card) {
		dlog.i("Hik_io.addAdminCard();Card:" + card);
//		byte[] data0 = hexStringToByteArray(card);
		byte[] data = Converts.hexStringToByte(card);

		if (mLeaseManagerItaly != null && data != null) {
			boolean result = mLeaseManagerItaly.AddAdmin(data);
			dlog.i("Hik_io.addAdminCard();Added admin card : " + card + ", result=" + result);
		}
	}

	@Override
	public void delAdminCard(String card) {
		byte[] data = hexStringToByteArray(card);

		if (mLeaseManagerItaly != null && data != null) {
			boolean result = mLeaseManagerItaly.DeleteAdmin(data);
			dlog.i("Hik_io.delAdminCard();Removed admin card : " + card + ", result=" + result);
		}
	}

	@Override
	public void resetAdminCards() {
		if (mLeaseManagerItaly != null) {
			boolean result = mLeaseManagerItaly.CleanAllAdmin();
			dlog.i("Hik_io.resetAdminCards();Removed all admin cards, result=" + result);
		}
	}

	@Override
	public void setSecondaryGPS(final long period) {

		if (period <= 0 || getMinorGPSDisabled)
			return;

		if (threadGpsUpdate != null)
			threadGpsUpdate.interrupt();

		threadGpsUpdate = new Thread() {
			public void run() {
				dlog.i("Hik_io.setSecondaryGPS();threadGpsUpdate started with period: " + period);
				while (!this.isInterrupted()) {

					String gpsInfo = getGpsInfo();

					if (gpsInfo != null && !gpsInfo.equals(lastGpsInfo)) {
						Bundle b = new Bundle();
						b.putString("GPSBOX", gpsInfo);
						obcService.notifyCarInfo(b);
						lastGpsInfo = gpsInfo;
					}

					try {
						Thread.sleep(period);
					} catch (InterruptedException e) {
						dlog.i("Hik_io.setSecondaryGPS();Thread interrupted");
						break;
					}
				}
			}
		};
		threadGpsUpdate.setName("GpsUpdate");
		threadGpsUpdate.start();
	}

	@Override
	public void RequestWhitelistUpload() {
	}

	@Override
	public void setAudioChannel(int channel, int volume) {

		if (mAudioManager == null)
			return;

		switch (channel) {

			case AUDIO_NONE:
				mAudioManager.audioSetChannel(AudioInfo.HIK_AUDIO_CHANNEL_RADIO);
				mAudioManager.audioSetVol(0);
				mAudioManager.AudioSetAMP(AudioInfo.AUDIO_AMP_STATUS_OFF);
				dlog.i("Hik_io.setAudioChannel();NONE");
				break;

			case AUDIO_RADIO:
				mAudioManager.audioSetChannel(AudioInfo.HIK_AUDIO_CHANNEL_RADIO);
				if (volume > 0)
					mAudioManager.audioSetVol(volume);
				else
					mAudioManager.audioSetVol(lastVolumeValue);
				mAudioManager.AudioSetAMP(AudioInfo.AUDIO_AMP_STATUS_ON);
				dlog.i("Hik_io.setAudioChannel();RADIO");
				break;

			case AUDIO_SYSTEM:
				mAudioManager.audioSetChannel(AudioInfo.HIK_AUDIO_CHANNEL_ARM);
				if (volume > 0)
					mAudioManager.audioSetVol(volume);
				else
					mAudioManager.audioSetVol(lastVolumeValue);
				mAudioManager.AudioSetAMP(AudioInfo.AUDIO_AMP_STATUS_ON);
				dlog.i("Hik_io.setAudioChannel();SYSTEM");
				break;

			case AUDIO_AUX:
				mAudioManager.audioSetChannel(AudioInfo.HIK_AUDIO_CHANNEL_AUX);
				if (volume > 0)
					mAudioManager.audioSetVol(volume);
				else
					mAudioManager.audioSetVol(lastVolumeValue);
				mAudioManager.AudioSetAMP(AudioInfo.AUDIO_AMP_STATUS_ON);
				dlog.i("Hik_io.setAudioChannel();AUX");
				break;

		}
	}

	@Override
	public void SetRadioChannel(String band, int freq) {
		if (mRadioManager != null) {
			int iBand = RadioInfo.BAND_TYPE_FM;
/*			if (band == null || band.isEmpty() || band.equalsIgnoreCase("FM"))
				iBand = RadioInfo.BAND_TYPE_FM;
			else if (band.equalsIgnoreCase("AM"))
				iBand = RadioInfo.BAND_TYPE_AM;*/

			if (band.equalsIgnoreCase("AM")) {
				iBand = RadioInfo.BAND_TYPE_AM;
			}


			mRadioManager.radioSetBand(iBand);
			dlog.i("Hik_io.SetRadioChannel();Radio: set band : " + iBand + " - " + band);

			if (iBand == RadioInfo.BAND_TYPE_FM) {
				if (freq > RadioInfo.BAND_FM_FREQ_MAX_VALUE)
					freq = RadioInfo.BAND_FM_FREQ_MAX_VALUE;
				if (freq < RadioInfo.BAND_FM_FREQ_MIN_VALUE)
					freq = RadioInfo.BAND_FM_FREQ_MIN_VALUE;
			} else {
				if (freq > RadioInfo.BAND_AM_FREQ_MAX_VALUE)
					freq = RadioInfo.BAND_AM_FREQ_MAX_VALUE;
				if (freq < RadioInfo.BAND_AM_FREQ_MIN_VALUE)
					freq = RadioInfo.BAND_AM_FREQ_MIN_VALUE;
			}

			mRadioManager.radioSetFreq(freq);
			dlog.i("Hik_io.SetRadioChannel();Radio: set freq : " + freq);

		}
	}

	@Override
	public void SetRadioVolume(int volume) {
		if (mAudioManager != null && mRadioManager != null) {

			if (volume < 0) {
				mAudioManager.AudioSetAMP(AudioInfo.AUDIO_AMP_STATUS_ON);
				dlog.i("Hik_io.SetRadioVolume();Radio: set AMPLIFIER ON");
			} else if (volume == 0) {
				mAudioManager.AudioSetAMP(AudioInfo.AUDIO_AMP_STATUS_OFF);
				dlog.i("Hik_io.SetRadioVolume();Radio: set AMPLIFIER OFF");
			} else {
				float coeff = AudioInfo.VOLUME_MAX_VALUE / 100f;
				int vol = Math.min((int) (volume * coeff), AudioInfo.VOLUME_MAX_VALUE);
				mAudioManager.audioSetVol(vol);
				mAudioManager.AudioSetAMP(AudioInfo.AUDIO_AMP_STATUS_ON);
				dlog.i("Hik_io.SetRadioVolume();Radio: set VOLUME : " + volume + " - " + vol);
				mAudioObserver.onVolumeValueChange(AudioInfo.HIK_AUDIO_CHANNEL_RADIO, vol);
			}
		}
	}

	@Override
	public void SetSeek(int direction, boolean auto) {
		if (mRadioManager != null) {
			dlog.i("Hik_io.SetSeek();Radio: set seek dir=" + direction + " auto=" + auto);
			if (direction > 0)
				mRadioManager.radioSetSeekDirection(RadioInfo.SEEK_DIRECTION_DOWN);
			else if (direction < 0)
				mRadioManager.radioSetSeekDirection(RadioInfo.SEEK_DIRECTION_UP);
			else {
				mRadioManager.radioSeekStop();
				return;
			}

			if (auto)
				mRadioManager.radioSeekAuto();
			else
				mRadioManager.radioSeekNext();
		}

	}

	@Override
	public void enableWatchdog(int secs) {
		if (mLeaseManagerItaly != null && secs > 0) {
			try {
				boolean result = mLeaseManagerItaly.configHeartbeatTime(secs);
				watchdogActive = result;
				dlog.i("Hik_io.enableWatchdog();Watchdog timeout set to " + secs + " Result=" + result);
			} catch (Exception e) {
				dlog.e("Hik_io.enableWatchdog();"+e.getMessage());
			}
		}
	}

	@Override
	public void disableWatchdog() {
		if (mLeaseManagerItaly != null && watchdogActive) {
			try {
				mLeaseManagerItaly.cancelHeartbeat();
				dlog.i("Hik_io.disableWatchdog();Watchdog remove");
			} catch (Exception e) {
				dlog.e("Hik_io.disableWatchdog();"+e.getMessage());
			}
		}
	}

	private void resetLastContact() {
		lastContact = System.currentTimeMillis();
	}

	@Override
	public long lastContactDelay() {
		return (System.currentTimeMillis() - lastContact) / 1000;
	}

	@Override
	public float getCellVoltageValue(int index) {
//		BatteryInfo bi = mCANManager.getBatteryInfoStatus();
		return mCANManager.getCellVoltageValue(index);
	}

	@Override
	public int getSOCValue() {
		BatteryInfo bi = mCANManager.getBatteryInfoStatus();
		return bi.SOCPercentValue;
	}

	@Override
	public int getPackCurrentValue() {
		BatteryInfo bi = mCANManager.getBatteryInfoStatus();
		return bi.packCurrentValue;
	}

	private String getKeyStatusName(int value) {
		switch (value) {
			case VehicleInfo.KEY_STATUS_OFF:
				return "OFF";
			case VehicleInfo.KEY_STATUS_ACC:
				return "ACC";
			case VehicleInfo.KEY_STATUS_ON:
				return "ON";
			default:
				return "" + value;
		}
	}

	private String getGearStatusName(int value) {
		switch (value) {
			case VehicleInfo.GEAR_STATUS_N_RANGE:
				return "N";
			case VehicleInfo.GEAR_STATUS_D_RANGE:
				return "D";
			case VehicleInfo.GEAR_STATUS_R_RANGE:
				return "R";
			default:
				return "" + value;
		}
	}

/*	private String getWorkStatusName(int value) {
		switch (value) {
			case VehicleInfo.MOTOR_WORK_STATUS_FOREWARD:
				return "FW";
			case VehicleInfo.MOTOR_WORK_STATUS_REVERSAL:
			case 2:
				return "RW";
			case VehicleInfo.MOTOR_WORK_STATUS_STOP:
				return "STOP";
			default:
				return "" + value;
		}
	}*/

/*	private String getReverseStatusName(int value) {
		switch (value) {
			case VehicleInfo.CAR_REVERSE_IN:
				return "IN";
			case VehicleInfo.CAR_REVERSE_OUT:
				return "OUT";
			default:
				return "" + value;
		}
	}*/

/*	private String getWakeupSourcesName(int value) {
		switch (value) {
			case VehicleInfo.WAKEUP_SOURCE_ACC:
				return "ACC";
			case VehicleInfo.WAKEUP_SOURCE_POWERKEY:
				return "KEY";
			default:
				return "" + value;
		}
	}*/

	private String getPackageStatusName(int value) {
		switch (value) {
			case BatteryInfo.BATTERY_PACKAGE_STATUS_CHARGE:
				return "CHARGE";
			case BatteryInfo.BATTERY_PACKAGE_STATUS_DISCHARGE:
				return "DISCHARGE";
			case BatteryInfo.BATTERY_PACKAGE_STATUS_SHELVE:
				return "SHELVE";
			default:
				return "" + value;
		}
	}

	private String getGpsInfo() {

		if (App.Versions.getLevel() < 1)
			return null;

		if (getMinorGPSDisabled)
			return null;

		String json = null;
		if (mLeaseManagerItaly != null) {
			GPSInfoParcel parcel;
			try {
				parcel = mLeaseManagerItaly.getMinorGPS();
			} catch (Exception e) {
				dlog.e("Hik_io.getMinorGPS();", e);
				getMinorGPSDisabled = true;
				return null;
			}
			if (parcel != null) {
				JSONObject jo = new JSONObject();
				try {
					jo.put("lat", parcel.latitude);
					jo.put("lon", parcel.longitude);
					jo.put("fix", parcel.fix);
					jo.put("acc", parcel.accuracy);
					jo.put("spd", parcel.speed);
					jo.put("ts", parcel.time);
					jo.put("head", parcel.direction);
					json = jo.toString();
				} catch (JSONException e) {
					dlog.e("Hik_io.getMinorGPS();Building JSON", e);
				}

			}
		}
		return json;
	}

	private void refreshInfo() {

		if (mCANManager == null) {
			dlog.e("Hik_io.refreshInfo();ALARM: SDK CANManager not instanced");
			return;
		}

		VehicleInfo mVehicle = mCANManager.getVehicleInfoStatus();
		Bundle b = new Bundle();

		if (mVehicle != null) {

			if (mVehicle.vinCode != null && !mVehicle.vinCode.isEmpty() && !mVehicle.vinCode.equalsIgnoreCase("null")) {
				b.putString("VIN", mVehicle.vinCode);
			}
			b.putInt("Km", mVehicle.vehileOdoMeterValue);
			b.putString("GearStatus", getGearStatusName(mVehicle.gearStatus));
			b.putBoolean("keyOn", mVehicle.keyOnStatusFlag == VehicleInfo.KEY_ON_STATUS_FLAG_TRUE);
			b.putString("KeyStatus", getKeyStatusName(mVehicle.keyStatus));
			b.putBoolean("ReadyOn", mVehicle.readySignalStatus == VehicleInfo.READY_SIGNAL_LIGHTEN);

			b.putFloat("MotV", mVehicle.motorDCVoltageValue);
			b.putInt("MotT", mVehicle.motorTempValue);
			//b.putString("MotWorkStatus", getWorkStatusName(mVehicle.motorWorkStatus));

			b.putFloat("Speed", mVehicle.vehicleSpeedValue);
			b.putBoolean("BrakesOn", mVehicle.brakesStatus == VehicleInfo.BRAKES_STATUS_DOWN);
			b.putBoolean("AccStatus", mVehicle.accStatus == VehicleInfo.ACC_STATUS_ON);
			//b.putBoolean("ArmPowerDown",mVehicle.armPowerDownFlag==VehicleInfo.NOTIFY_ARM_POWER_DOWN_ENABLE);
			b.putBoolean("LcdOn", mVehicle.lcdPowerStatus == VehicleInfo.LCD_POWER_STATUS_ON);
			b.putBoolean("MotFault", mVehicle.motorControllerFaultWarning == SDKConstants.STATUS_EXCEPTION);
			b.putBoolean("MotTempHigh", mVehicle.motorControllerTempHighWarning == SDKConstants.STATUS_EXCEPTION);

			 /*
			 b.putInt("Doors",mVehicle.RFDoorStatus);
			 b.putInt("Windows",mVehicle.RFWindowsStatus);
			 */

			//b.putString("ReverseStatus",getReverseStatusName(mVehicle.reverseStatus));

			//b.putString("WakeupSource",getWakeupSourcesName(mVehicle.wakeupSource));

		} else {
			dlog.e("mVehicle==null");
		}

		BatteryInfo bi = mCANManager.getBatteryInfoStatus();

		 /*
		 b.putInt("SOC",100);
		 b.putBoolean("BmsSystemFault",false) ;
		 b.putBoolean("ChargeCommFault",false);
		 b.putBoolean("vcuCommFault", false);
		 b.putBoolean("PreChargeFault", false);
		 b.putBoolean("ChargeHeatFault", false);
		 b.putInt("PackVoltage", 80);
		 b.putInt("PackCurrent", 20);
		 b.putBoolean("PPStatus", false);
		 b.putBoolean("KSStatus", false);
		 b.putBoolean("KMStatus", false);
		 b.putBoolean("ChargeCommStatus",false);
		 b.putString("PackageStatus",  getPackageStatusName(1));			 
		 */

		if (bi != null) {
			b.putInt("SOC", bi.SOCPercentValue);
			b.putBoolean("BmsFault", bi.bmsSystemFaultWarningStatus == SDKConstants.STATUS_EXCEPTION);
			b.putBoolean("ChFault", bi.chargeCommFaultWarningStatus == SDKConstants.STATUS_EXCEPTION);
			b.putBoolean("vcuFault", bi.vcuCommFaultWarningStatus == SDKConstants.STATUS_EXCEPTION);
			b.putBoolean("PreChFault", bi.preChargeFaultWarningStatus == SDKConstants.STATUS_EXCEPTION);
			b.putBoolean("ChHeatFault", bi.chargeHeatFaultWarningStatus == SDKConstants.STATUS_EXCEPTION);
			b.putInt("PackV", bi.packVoltageValue);
			b.putInt("PackA", bi.packCurrentValue);
			b.putBoolean("PPStatus", bi.PPStatus == BatteryInfo.PP_STATUS_CONNECTED);
			b.putBoolean("KSStatus", bi.KSStatus == BatteryInfo.KS_STATUS_ON);
			b.putBoolean("KMStatus", bi.KMStatus == BatteryInfo.KM_STATUS_ON);
			b.putBoolean("ChCommStatus", bi.chargerCommunicationStatus == BatteryInfo.CHARGER_COMMUNICATION_ON);
			b.putString("PackStatus", getPackageStatusName(bi.packageStatus));
			b.putFloatArray("CellsInfo", bi.cellVoltageValue);
			b.putIntArray("sensorTempValue", bi.sensorTempValue);

			b.putInt("ChStatus", bi.chargerAvaliableStatus);
		} else {
			dlog.e("Hik_io.refreshInfo();batteryInfo==null");
		}

		if (lastGpsInfo != null && !lastGpsInfo.isEmpty())
			b.putString("GPSBOX", lastGpsInfo);

		b.putString("SDKVer", SDKVersion);

		dlog.i("Hik_io.refreshInfo();" + b.toString());

		obcService.notifyCarInfo(b);
	}

	private byte[] hexStringToByteArray(String s) {

		if (s == null)
			return null;

		int len = s.length();

		if ((len % 2) != 0)
			return null;

		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
					+ Character.digit(s.charAt(i + 1), 16));
		}
		return data;
	}

	private int getServiceVersion() {
		int version = -1;
		try {
			PackageInfo pInfo = context.getPackageManager().getPackageInfo("com.Hik.Mercury.SDK.service", PackageManager.GET_META_DATA);
			version = pInfo.versionCode;
		} catch (NameNotFoundException e) {
			dlog.e("Hik_io.getServiceVersion();Getting service package version :", e);
		}
		return version;
	}

	private LeaseObrItaly mLeaseObserverItaly = new LeaseObrItaly();

	private class LeaseObrItaly extends LeaseObserverItaly {

		@Override
		public void onCleanAdminResult(int result) throws RemoteException {
			super.onCleanAdminResult(result);
			dlog.i("Hik_io.onCleanAdminResult();" + result);
		}

		@Override
		public void onLeaseReportCard(int cardID) throws RemoteException {
			super.onLeaseReportCard(cardID);

			String Hex = Integer.toHexString(cardID);
			dlog.i("Hik_io.onLeaseReportCard();" + cardID + ", hex=" + Hex);
			//dlog.i("Hik_io.perf: onLeaseReportCard");
			obcService.notifyCard(Hex, "OPEN RFID", false);

		}

		@Override
		public void onLeaseReportHeartbeat(String carId, String data)
				throws RemoteException {
			super.onLeaseReportHeartbeat(carId, data);

			resetLastContact();

			if (carId == null || carId.isEmpty() || carId.equalsIgnoreCase("null"))
				return;

			Bundle b = new Bundle();
			//b.putString("VIN", carId);
			mVehicleObserver.onVinCodeChange(carId);
			String version;
			if (data.contains("~")) {
				version = data.substring(0, data.indexOf("~"));

				b.putString("VER", version);
				try {
					data = data.substring(data.indexOf("~") + 1);

					String[] cData = data.split(";"); //0: Battery safety 1: custom card
					b.putBoolean("batterySafety", Integer.parseInt(cData[0]) > 0);
					if (cData.length > 1)
						b.putString("fakeCard", cData[1]);
					if (cData.length > 2)
						b.putString("fakePin", cData[2]);
				} catch (Exception e) {
					dlog.e("Hik_io.onLeaseReportHeartbeat();Exception handling extra data inside Heartbeat", e);
				}
			} else {
				version = data;
			}
			if (!SDKVersion.equals(version)) {
				SDKVersion = version;
				App.Versions.HbVer = version;
			}

			obcService.notifyCarInfo(b);

		}

	}

	private LeaseObr mLeaseObserver = new LeaseObr();

	private class LeaseObr extends LeaseObserver {

		@Override
		public void onLeaseCarStatusChange(int result, int memberID) throws RemoteException {
			Bundle b = new Bundle();
			b.putInt("LEASE", result);
			b.putInt("LEASEID", memberID);
			//eventRepository.LeaseInfo(result, memberID);
		}

	}

	private VehicleObr mVehicleObserver = new VehicleObr();

	private class VehicleObr extends VehicleInfoChangeObserver {

		@Override
		public void onVinCodeChange(String vinCode) throws RemoteException {
			super.onVinCodeChange(vinCode);

			if (vinCode == null || vinCode.isEmpty() || vinCode.equalsIgnoreCase("null"))
				return;

			String vin = vinCode;
			if (vinCode.contains("~")) {
				vin = vinCode.substring(0, vinCode.indexOf("~"));
			}

/*
			if (vin.isEmpty() || vin.equalsIgnoreCase("") || vin.length() < 7)
				return;
*/
			if (vin.isEmpty() || vin.length() < 7)
				return;

			Bundle b = new Bundle();
			b.putString("VIN", vin);
			obcService.notifyCarInfo(b);

/*			if (!BuildConfig.BUILD_TYPE.equalsIgnoreCase("debug")){
				dlog.i("Hik_io.VehicleObr();" + vinCode);
			}
			*/

			dlog.i("Hik_io.VehicleObr();" + vinCode);
		}

		@Override
		public void onGearStatusChange(int gearStatus) throws RemoteException {
			super.onGearStatusChange(gearStatus);

			Bundle b = new Bundle();
			b.putString("GearStatus", getGearStatusName(gearStatus));
			obcService.notifyCarInfo(b);

			dlog.i("Hik_io.onGearStatusChange();" + gearStatus);
		}

		@Override
		public void onKeyOnStatusFlagChange(int keyOnStatus) throws RemoteException {
			super.onKeyOnStatusFlagChange(keyOnStatus);

			Bundle b = new Bundle();
			b.putBoolean("keyOn", keyOnStatus == VehicleInfo.KEY_ON_STATUS_FLAG_TRUE);
			obcService.notifyCarInfo(b);

			dlog.i("Hik_io.onKeyOnStatusFlagChange();" + keyOnStatus);
		}

		@Override
		public void onKeyStatusChange(int keyStatus) throws RemoteException {
			super.onKeyStatusChange(keyStatus);
			dlog.i("onKeyStatusChange(keyStatus):" + keyStatus);

			Bundle b = new Bundle();
			b.putString("KeyStatus", getKeyStatusName(keyStatus));

			obcService.notifyCarInfo(b);

			//		FInstructionsClose FInstructionsClose = (FInstructionsClose)  getSupportFragmentManager().findFragmentById("");

		}

		@Override
		public void onMotorDCVoltageValueChange(float motorDCVoltage) throws RemoteException {
			super.onMotorDCVoltageValueChange(motorDCVoltage);

			Bundle b = new Bundle();
			b.putFloat("MotV", motorDCVoltage);
			obcService.notifyCarInfo(b);

			//dlog.i("onMotorDCVoltageValueChange(motorDCVoltage):" + motorDCVoltage );
		}

		@Override
		public void onMotorTempValueChange(int motorTemp) throws RemoteException {
			super.onMotorTempValueChange(motorTemp);
			Bundle b = new Bundle();
			b.putInt("MotT", motorTemp);
			obcService.notifyCarInfo(b);
			//dlog.i("onMotorTempValueChange(motorTemp):" + motorTemp );
		}

		@Override
		public void onMotorWarningStatusChange(int warningIndex, int warningStatus)
				throws RemoteException {
			super.onMotorWarningStatusChange(warningIndex, warningStatus);
			Bundle b = new Bundle();
			b.putInt("WARN_INDEX", warningIndex);
			b.putInt("WARN_STATUS", warningStatus);
			obcService.notifyCarInfo(b);
			//dlog.i("onMotorWarningStatusChange(warningIndex, warningStatus):("+ warningIndex + "," + warningStatus + ")");
		}

		@Override
		public void onMotorWorkStatusChange(int motorWorkStatus) throws RemoteException {
			super.onMotorWorkStatusChange(motorWorkStatus);

	            /*
	            Bundle b = new Bundle();
	            b.putString("MotorWorkStatus", getWorkStatusName(motorWorkStatus));		
				obcService.notifyCarInfo(b);
				
	            dlog.i("onMotorWorkStatusChange(motorWorkStatus):" + motorWorkStatus );
	            */
		}

		@Override
		public void onReadySignalStatusChange(int readySignal) throws RemoteException {
			super.onReadySignalStatusChange(readySignal);

			Bundle b = new Bundle();
			b.putBoolean("ReadyOn", readySignal == VehicleInfo.READY_SIGNAL_LIGHTEN);
			obcService.notifyCarInfo(b);

			dlog.i("Hik_io.onReadySignalStatusChange();" + readySignal);
		}

		private long VehicleSpeedChange_LastLog = 0;

		@Override
		public void onVehicleSpeedChange(float vehicleSpeed) throws RemoteException {
			super.onVehicleSpeedChange(vehicleSpeed);

			Bundle b = new Bundle();
			b.putFloat("Speed", vehicleSpeed);
			obcService.notifyCarInfo(b);
			if (System.currentTimeMillis() - VehicleSpeedChange_LastLog > 10000) {
				dlog.i("Hik_io.onVehicleSpeedChange();" + vehicleSpeed);
				VehicleSpeedChange_LastLog = System.currentTimeMillis();
			}
		}

		@Override
		public void onVehileOdoMeterValueChange(int vehileOdoMeter) throws RemoteException {
			super.onVehileOdoMeterValueChange(vehileOdoMeter);
			dlog.i("Hik_io.onVehileOdoMeterValueChange();" + vehileOdoMeter);

			Bundle b = new Bundle();
			b.putInt("Km", vehileOdoMeter);
			obcService.notifyCarInfo(b);

			if (vehileOdoMeter > 0) {
				OptimizeDistanceCalc.Controller(OdoController.RUNonChangeOdo, vehileOdoMeter);
			}

		}

		@Override
		public void onBrakesStatusChange(int brakesStatus) throws RemoteException {
			super.onBrakesStatusChange(brakesStatus);

			Bundle b = new Bundle();
			b.putBoolean("BrakesOn", brakesStatus == VehicleInfo.BRAKES_STATUS_DOWN);
			obcService.notifyCarInfo(b);

			dlog.i("Hik_io.onBrakesStatusChange();" + brakesStatus);
		}

		@Override
		public void onAccStatusChange(int accStatus) throws RemoteException {
			super.onAccStatusChange(accStatus);

			Bundle b = new Bundle();
			b.putBoolean("AccStatus", accStatus == VehicleInfo.ACC_STATUS_ON);
			obcService.notifyCarInfo(b);

			dlog.i("Hik_io.onAccStatusChange();" + accStatus);
		}

		@Override
		public void onMCUNoitfyPowerDownChange(int status) throws RemoteException {
			super.onMCUNoitfyPowerDownChange(status);

			Bundle b = new Bundle();
			//b.putBoolean("ArmPowerDown",status==VehicleInfo.NOTIFY_ARM_POWER_DOWN_ENABLE);
			obcService.notifyCarInfo(b);

			dlog.i("Hik_io.onMCUNoitfyPowerDownChange();" + status);
		}

		@Override
		public void onReverseStatusChange(int status) throws RemoteException {
			super.onReverseStatusChange(status);

			Bundle b = new Bundle();
			//b.putString("ReverseStatus",getReverseStatusName(status));
			obcService.notifyCarInfo(b);

			dlog.i("Hik_io.onReverseStatusChange();" + status);
		}

	}

	private BatteryObr mBatteryObserver = new BatteryObr();

	private class BatteryObr extends BatteryInfoChangeObserver {

		@Override
		public void onBatteryWarningStatusChange(int warningIndex, int warningStatus)
				throws RemoteException {
			// TODO Auto-generated method stub
			super.onBatteryWarningStatusChange(warningIndex, warningStatus);
			dlog.i("Hik_io.onBatteryWarningStatusChange():("
					+ warningIndex + "," + warningStatus + ")");
		}

		@Override
		public void onCellVoltageValueChange(int cellIndex, float cellVoltageValue)
				throws RemoteException {
			// TODO Auto-generated method stub
			super.onCellVoltageValueChange(cellIndex, cellVoltageValue);



				/*Bundle b = new Bundle(), c = new Bundle();
				b.putInt("CellInxed", cellIndex);
				b.putFloat("CellVoltage", cellVoltageValue);
				c.putBundle("CellInfo",b);
				obcService.notifyCarInfo(c);*/


				/*dlog.i( "onCellVoltageValueChange(cellIndex, cellVoltageValue):("
	                    + cellIndex + "," + cellVoltageValue + ")");*/
		}

		@Override
		public void onChargeCommStatusChange(int commStatus) throws RemoteException {
			// TODO Auto-generated method stub
			super.onChargeCommStatusChange(commStatus);
			Bundle b = new Bundle();
			b.putBoolean("ChFault", commStatus == SDKConstants.STATUS_EXCEPTION);
			obcService.notifyCarInfo(b);

			dlog.i("Hik_io.onChargeCommStatusChange();" + commStatus);
		}

		@Override
		public void onChargerAvaliableStatusChange(int status) throws RemoteException {
			// TODO Auto-generated method stub
			super.onChargerAvaliableStatusChange(status);
			dlog.i("Hik_io.onChargerAvaliableStatusChange();" + status);
		}

		@Override
		public void onEndBalanceStatusChange(int endBalance) throws RemoteException {
			// TODO Auto-generated method stub
			super.onEndBalanceStatusChange(endBalance);
			dlog.i("Hik_io.onEndBalanceStatusChange();" + endBalance);
		}

		@Override
		public void onKMStatusChange(int kmStatus) throws RemoteException {
			// TODO Auto-generated method stub
			super.onKMStatusChange(kmStatus);

			Bundle b = new Bundle();
			b.putBoolean("KMStatus", kmStatus == BatteryInfo.KM_STATUS_ON);
			obcService.notifyCarInfo(b);

			dlog.i("Hik_io.onKMStatusChange();" + kmStatus);
		}

		@Override
		public void onKSStatusChange(int kStatus) throws RemoteException {
			// TODO Auto-generated method stub
			super.onKSStatusChange(kStatus);

			Bundle b = new Bundle();
			b.putBoolean("KSStatus", kStatus == BatteryInfo.KS_STATUS_ON);
			obcService.notifyCarInfo(b);

			dlog.i("Hik_io.onKSStatusChange();" + kStatus);
		}

		@Override
		public void onPPStatusChange(int pStatus) throws RemoteException {
			// TODO Auto-generated method stub
			super.onPPStatusChange(pStatus);

			Bundle b = new Bundle();
			b.putBoolean("PPStatus", pStatus == BatteryInfo.PP_STATUS_CONNECTED);
			obcService.notifyCarInfo(b);

			dlog.i("Hik_io.onPPStatusChange();" + pStatus);
		}

//		int currentValue = 0;

		@Override
		public void onPackCurrentValueChange(int currentValue) throws RemoteException {
			// TODO Auto-generated method stub
			super.onPackCurrentValueChange(currentValue);

				/*currentValue=(3499-currentValue)/10;

	            if (this.currentValue!=currentValue) {
					this.currentValue=currentValue;
					Bundle b = new Bundle();
					b.putInt("PackAmp", currentValue);
					b.putLong("timestampAmp", System.currentTimeMillis() - App.lastUpdateCAN.getTime());
					dlog.i("onPackCurrentValueChange: " + currentValue+" "+b.getLong("timestampAmp"));
					App.lastUpdateCAN = new Date();
					obcService.notifyCANData(b);
				}*/

			//dlog.i("onPackCurrentValueChange(currentValue):" + currentValue );
		}

		@Override
		public void onPackVoltageValueChange(int voltageValue) throws RemoteException {
			// TODO Auto-generated method stub
			super.onPackVoltageValueChange(voltageValue);

			Bundle b = new Bundle();
			b.putInt("PackV", voltageValue);
			obcService.notifyCarInfo(b);

			//dlog.i("onPackVoltageValueChange(voltageValue):" + voltageValue );
		}

		@Override
		public void onPackageStatusChange(int packageStatus) throws RemoteException {
			// TODO Auto-generated method stub
			super.onPackageStatusChange(packageStatus);

			Bundle b = new Bundle();
			b.putString("PackStatus", getPackageStatusName(packageStatus));
			obcService.notifyCarInfo(b);

			dlog.i("Hik_io.onPackageStatusChange();" + packageStatus);
		}

		@Override
		public void onSOCPercentValueChange(int percentValue) throws RemoteException {
			// TODO Auto-generated method stub
			super.onSOCPercentValueChange(percentValue);
			dlog.i("Hik_io.onSOCPercentValueChange();" + percentValue);

			Bundle b = new Bundle();
			b.putInt("SOC", percentValue);

			obcService.notifyCarInfo(b);

		}

		@Override
		public void onSensorTempValueChange(int sensorIndex, int tempValue) throws RemoteException {
			// TODO Auto-generated method stub
			super.onSensorTempValueChange(sensorIndex, tempValue);
			obcService.setOnSensorTempValue(sensorIndex, tempValue);
				/* dlog.i("onSensorTempValueChange(sensorIndex, tempValue):("
	                    + sensorIndex + "," + tempValue + ")");*/
		}

	}

	private AudioObr mAudioObserver = new AudioObr();
	private static int lastVolumeValue = 20;

	private class AudioObr extends AudioObserver {

		@Override
		public void onVolumeValueChange(int channel, int volume) {

			if (channel == AudioInfo.HIK_AUDIO_CHANNEL_RADIO) {
				double coeff = 100d / AudioInfo.VOLUME_MAX_VALUE;
				Bundle b = new Bundle();
				b.putString("what", "RadioVolume");
				b.putInt("volume", (int) ((double) volume * coeff));
				obcService.notifyRadioInfo(b);
				dlog.i("Hik_io.AudioObr();Radio: Volume change = " + volume);

			}
			if (ProTTS.ignoreVolume || AudioPlayer.ignoreVolume) {
				ProTTS.ignoreVolume = false;
				AudioPlayer.ignoreVolume = false;
			} else
				lastVolumeValue = volume;
		}

	}

	private RadioObr mRadioObserver = new RadioObr();

	private class RadioObr extends RadioObserver {

		@Override
		public void onCurPlayInfoChange(int bandType, int freq) throws RemoteException {

			Bundle b = new Bundle();
			b.putString("what", "CurPlayInfo");
			b.putString("band", bandType == RadioInfo.BAND_TYPE_FM ? "FM" : "AM");
			b.putInt("freq", freq);
			obcService.notifyRadioInfo(b);

		}

		@Override
		public void onSeekInfoChange(int bandType, int freq) throws RemoteException {

			Bundle b = new Bundle();
			b.putString("what", "SeekInfo");
			b.putString("band", bandType == RadioInfo.BAND_TYPE_FM ? "FM" : "AM");
			b.putInt("freq", freq);
			obcService.notifyRadioInfo(b);

		}

		@Override
		public void onFreqRangeChange(int minFM, int maxFM, int minAM, int maxAM) throws RemoteException {

		}

		@Override
		public void onSeekValidFreqInfoChange(int bandType, int freq) throws RemoteException {

			Bundle b = new Bundle();
			b.putString("what", "SeekValidFreqInfo");
			b.putString("band", bandType == RadioInfo.BAND_TYPE_FM ? "FM" : "AM");
			b.putInt("freq", freq);
			obcService.notifyRadioInfo(b);

		}

		@Override
		public void onSeekStatusChange(int seekStatus) throws RemoteException {
			super.onSeekStatusChange(seekStatus);

			String status = "";

			switch (seekStatus) {
				case RadioInfo.SEEK_STATUS_AUTO_SEEK_START:
					status = "AutoSeekStart";
					break;

				case RadioInfo.SEEK_STATUS_SEEK_UP_START:
					status = "SeekUpStart";
					break;

				case RadioInfo.SEEK_STATUS_SEEK_DOWN_START:
					status = "SeekDownStart";
					break;

				case RadioInfo.SEEK_STATUS_SEEK_INTERRUPT:
					status = "SeekInterrupt";
					break;

				case RadioInfo.SEEK_STATUS_SEEK_AUTO_STOP:
					status = "SeekAutoStop";
					break;
			}

			Bundle b = new Bundle();
			b.putString("what", "SeekStatus");
			b.putString("status", status);
			obcService.notifyRadioInfo(b);

		}

	}

	private BluetoothObr mBTObserver = new BluetoothObr();

	private class BluetoothObr extends BluetoothStateCallback {

		@Override
		public void onBluetoothSwitchStateChange(int state) throws RemoteException {
			super.onBluetoothSwitchStateChange(state);
		}

		@Override
		public void onBluetoothPairStateChange(int arg0) throws RemoteException {
			super.onBluetoothPairStateChange(arg0);
		}

		@Override
		public void onBluetoothHFPStateChange(int hfpStatus) throws RemoteException {
			super.onBluetoothHFPStateChange(hfpStatus);
		}

		@Override
		public void onBluetoothAVRCPStateChange(int arg0) throws RemoteException {
			super.onBluetoothAVRCPStateChange(arg0);
		}

		@Override
		public void onBluetoothA2DPStateChange(int arg0) throws RemoteException {
			super.onBluetoothA2DPStateChange(arg0);
		}

		@Override
		public void onBluetoothA2DPStreamStateChange(int arg0) throws RemoteException {
			super.onBluetoothA2DPStreamStateChange(arg0);
		}

		@Override
		public void onPairDevAdd(com.Hik.Mercury.SDK.bluetooth.BluetoothDevice mDevice) throws RemoteException {
			super.onPairDevAdd(mDevice);
		}

		@Override
		public void onMoudleNameChange(String name) throws RemoteException {
			super.onMoudleNameChange(name);
		}

		@Override
		public void onMyPairCodeChange(String code) throws RemoteException {
			super.onMyPairCodeChange(code);
		}

		@Override
		public void onPairRequset(String code, String mac) throws RemoteException {
			super.onPairRequset(code, mac);
			mBTManager.agreePairRequest();
		}

		@Override
		public void onRemoteDeviceInfoReport(String mac, String name) throws RemoteException {
			super.onRemoteDeviceInfoReport(mac, name);
		}

		@Override
		public void onPairModeChange(boolean inPair) throws RemoteException {
			super.onPairModeChange(inPair);
		}

		@Override
		public void onHFPPhoneStateChange(int state) throws RemoteException {
			super.onHFPPhoneStateChange(state);
		}

		@Override
		public void onHFPPhoneSCOChange(int state) throws RemoteException {
			super.onHFPPhoneSCOChange(state);
		}

		@Override
		public void onCallingPhoneName(String name) throws RemoteException {
			super.onCallingPhoneName(name);
		}
	}

}
