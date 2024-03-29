package eu.philcar.csg.OBC;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
//import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.util.LongSparseArray;
import android.view.View;

import java.io.File;
//import java.io.FilenameFilter;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;

import eu.philcar.csg.OBC.controller.map.FDriving;
import eu.philcar.csg.OBC.controller.map.FHome;
import eu.philcar.csg.OBC.controller.map.FMap;
import eu.philcar.csg.OBC.controller.map.FMenu;
import eu.philcar.csg.OBC.controller.map.FPark;
import eu.philcar.csg.OBC.controller.map.FRadio;
import eu.philcar.csg.OBC.controller.map.FSearch;
import eu.philcar.csg.OBC.controller.map.util.GeoUtils;
import eu.philcar.csg.OBC.db.Poi;
import eu.philcar.csg.OBC.db.Pois;
import eu.philcar.csg.OBC.devices.LowLevelInterface;
import eu.philcar.csg.OBC.helpers.AudioPlayer;
import eu.philcar.csg.OBC.helpers.Clients;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.Debug;
import eu.philcar.csg.OBC.helpers.ProTTS;
import eu.philcar.csg.OBC.service.CarInfo;
import eu.philcar.csg.OBC.service.MessageFactory;
import eu.philcar.csg.OBC.service.ObcService;
import eu.philcar.csg.OBC.service.ServiceConnector;
import eu.philcar.csg.OBC.task.OdoController;
import eu.philcar.csg.OBC.task.OptimizeDistanceCalc;

public class AMainOBC extends ABase implements LocationListener {

	public final static int MSG_UPDATE_TIME = 10;
	public final static int MSG_UPDATE_DATE = 11;
//	private final static int MIN_MOVE_TOL = 1; // meters
//	private static final int AD_TIME = 10000;
	public static AudioPlayer player;
	public boolean firstUpPoi = true;
	Handler timerHandler = new Handler();
	Handler fragmentHandler = null;
	Runnable timerRunnable = new Runnable() {

		SimpleDateFormat day = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
		SimpleDateFormat time = new SimpleDateFormat("HH:mm", Locale.getDefault());

		@Override
		public void run() {

			Date d = new Date();
			if (fragmentHandler != null) {
				Message msg = fragmentHandler.obtainMessage(MSG_UPDATE_DATE, day.format(d));
				fragmentHandler.sendMessage(msg);

				msg = fragmentHandler.obtainMessage(MSG_UPDATE_TIME, time.format(d));
				fragmentHandler.sendMessage(msg);
			}

			if (timerHandler != null) {
				timerHandler.postDelayed(this, 1000);
			}
		}
	};
	private DLog dlog = new DLog(this.getClass());
	private ProTTS tts;
	private Boolean firstUpCharging = true;
	private ServiceConnector serviceConnector;
	private LongSparseArray<POI> pois;
	private LongSparseArray<FuelStation> fuelStations;
	private POI selectedPOI;
	private FuelStation selectedFS;
	private Location currentPosition, endingPosition;
//	private String currentRouting;
	private Date lastUpdate = new Date();
//	private File[] theAds;
//	private int adPosition;
//	private long lastTimeAdChanged;
//	private LocationManager locationManager;
	private boolean isInsideParkingArea = true;
	private boolean lastIsInsideParkingArea = isInsideParkingArea;
	private long lastIsInsideParkingAreaTs = System.currentTimeMillis();
	private float rotationToParkingArea = 0.0f;
//	private int bearing = 0;
	private MainOBCServiceHandler serviceHandler = new MainOBCServiceHandler(new WeakReference<AMainOBC>(this));

	public void setInsideParkingArea(boolean insideParkingArea) {
		if (System.currentTimeMillis() - lastIsInsideParkingAreaTs >= 10000) {
			lastIsInsideParkingArea = this.isInsideParkingArea;
			lastIsInsideParkingAreaTs = System.currentTimeMillis();
		}
		this.isInsideParkingArea = insideParkingArea;
	}

/*	public boolean getisInsideParkingArea() {
		return isInsideParkingArea;
	}*/

	public boolean checkisInsideParkingArea() {
		boolean inside = true;
//		if(BuildConfig.FLAVOR.equalsIgnoreCase("develop"))
//			inside = false;
		try {
			if (App.getLastLocation() != null)
				inside = App.checkParkArea(App.getLastLocation().getLatitude(), App.getLastLocation().getLongitude());
		} catch (Exception e) {
			dlog.e("AMainOBC.checkisInsideParkingArea();Exception while checking inside area ", e);
		}
		return inside;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		dlog.d("AMainOBC.onCreate();");
		player = new AudioPlayer(this);
		tts = new ProTTS(this);

		setContentView(R.layout.a_base);

		// Service
		serviceConnector = new ServiceConnector(this, serviceHandler);

		// Don't know

		// Fragment
		if (savedInstanceState == null) {

			fuelStations = new LongSparseArray<>();
			pois = new LongSparseArray<AMainOBC.POI>();
			currentPosition = null; //new LatLong(45.464189, 9.191181);

			// Since this is the first fragment, we need to use the "add" method to show it to the user, and not the "replace"
			FragmentTransaction transaction = getFragmentManager().beginTransaction();

			if (App.isNavigatorEnabled) {
				transaction.add(R.id.awelPlaceholderFL, FHome.newInstance(), FHome.class.getName());
				transaction.addToBackStack(FHome.class.getName());
			} else {
				transaction.add(R.id.awelPlaceholderFL, FDriving.newInstance(), FDriving.class.getName());
				transaction.addToBackStack(FDriving.class.getName());
			}
			transaction.commit();
		}

		computeAdsList();

		if (App.getLastLocation() != null)
			currentPosition = App.getLastLocation();
		else
			currentPosition = new Location("");

	}

	@Override
	protected void onResume() {

		super.onResume();

		App.setForegroundActivity(this);

		serviceConnector.connect(Clients.Main);

		if (App.parkMode.isOn() && !App.motoreAvviato && App.getParkModeStarted() != null) {
			pushFragment(FMenu.newInstance(), FMenu.class.getName(), false);
		}

		if (timerHandler != null) {
			timerHandler.postDelayed(timerRunnable, 0);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		App.setForegroundActivity("Pause");

		serviceConnector.unregister();
		serviceConnector.disconnect();

		timerHandler.removeCallbacks(timerRunnable);

		//locationManager.removeUpdates(this);
	}

	@Override
	protected void onDestroy() {

		super.onDestroy();

		if (serviceConnector.isConnected()) {
			serviceConnector.unregister();
			serviceConnector.disconnect();
		}

		if (tts != null) {
			tts.shutdown();
			tts = null;
		}
	}

	@Override
	public void sendMessage(Message msg) {
		serviceConnector.send(msg);
	}

	private void computeAdsList() {

		// Ads
		File adsFolder = null;
		boolean greaterOrEqKitkat = Build.VERSION.SDK_INT >= 19;
		if (greaterOrEqKitkat) {

			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {

				adsFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "/csg/ads/");
			}

		} else {

			adsFolder = new File(Environment.getExternalStorageDirectory(), "/csg/ads/");
		}

		if (adsFolder == null) {
			return;
		}

		if (!adsFolder.exists()) {
			adsFolder.mkdirs();
		}

/*		theAds = adsFolder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String filename) {
				return filename.contains(".png");
			}
		});*/

//		adPosition = 0;
//		lastTimeAdChanged = System.currentTimeMillis();
	}

	@Override
	protected int getPlaceholderResource() {
		return R.id.awelPlaceholderFL;
	}

	public void setParkModeStarted(boolean paused) {

		if (serviceConnector != null) {

			Message msg = MessageFactory.changeTripParkMode(paused);
			serviceConnector.send(msg);
		}
	}

	public LongSparseArray<POI> getPOIs() {
		return this.pois;
	}

	public LongSparseArray<FuelStation> computeFuelStations() {

		fuelStations.clear();

		SortedMap<Integer, FuelStation> map = new TreeMap<>();

		List<Poi> list;
		if (Debug.IGNORE_HARDWARE) {

			list = new ArrayList<>(12);

			Poi poi = new Poi();

			for (int i = 1; i < 13; i++) {
				poi.id = i;
				poi.type = String.valueOf(i);
				poi.code = String.valueOf(i);
				poi.address = String.valueOf(i);
				poi.town = String.valueOf(i);
				poi.zip = String.valueOf(i);
				poi.province = String.valueOf(i);
				poi.attivo = true;
				poi.lon = 13.229069 + Math.random() * 0.01;
				poi.lat = 46.089805 + Math.random() * 0.01;
				poi.update = System.currentTimeMillis();

				list.add(poi);
			}

		} else {
			Pois dao = App.Instance.dbManager.getPoisDao();
			list = dao.getPois("station");
		}

		Location pll = null;

		if (App.getLastLocation() != null && App.getLastLocation().getLatitude() != 0 && App.getLastLocation().getLatitude() != 0) {
			pll = App.getLastLocation();
		}

		for (Poi p : list) {
			if (p.lat != 0 && p.lon != 0) {
				Location ll = null; //TODO
				int d = (int) Math.round(GeoUtils.harvesineDistance(ll, pll) / 100) * 100;
				map.put(d, new FuelStation(p.id, "Total Erg", p.address, "", d, 0, ll));
			}
		}

		int idx = 0;

		for (FuelStation f : map.values()) {
			fuelStations.put(idx++, f);
		}

		return this.fuelStations;
	}

	public FuelStation getFuelStation() {
		return this.selectedFS;
	}

	public void setFuelStation(FuelStation fuelStation) {
		this.selectedFS = fuelStation;
	}

	public POI getPOI() {
		return this.selectedPOI;
	}

	public void setPOI(POI poi) {
		this.selectedPOI = poi;
	}

	public void navigateTo(Location location) {
		FMap fMap = (FMap) getFragmentManager().findFragmentByTag(FMap.class.getName());
		if (fMap != null) {
			fMap.navigateTo(location);
		}
	}

	public Location getEndingPosition() {
		return this.endingPosition;
	}

	public void setEndingPosition(Location endingPosition) {
		this.endingPosition = endingPosition;
	}

/*	public String getCurrentRouting() {
		return this.currentRouting;
	}*/

	public void setCurrentRouting(String currentRouting) {
//		this.currentRouting = currentRouting;
	}

	@Override
	public void onLocationChanged(Location location) {

		//updateAd();
		if (location != null)
			OptimizeDistanceCalc.Controller(OdoController.RUNonChangGps, location);

		if (App.isNavigatorEnabled) {

			FMap fMap = (FMap) getFragmentManager().findFragmentByTag(FMap.class.getName());
			if (fMap != null) {
				fMap.onActivityLocationChanged(location);
			}
		}
		//dlog.d("AMainOBC.onLocationChanged(); lastUpdate "+ (new Date().getTime() - lastUpdate.getTime()) );

		//Attenzione il codice commentato sotto per qualche motivo impedisce l'aggiornamento corretto della posizione!!!
		/*if (previousPosition != null && GeoUtils.harvesineDistance(previousPosition, location) < MIN_MOVE_TOL) {
			currentPosition = location;
			return;
		}*/

		if (System.currentTimeMillis() - lastUpdate.getTime() < 5000) {
//			return;
		} else {
			lastUpdate = new Date();
			//dlog.d("AMainOBC.onLocationChanged();lastUpdate "+ (new Date().getTime() - lastUpdate.getTime()) );

			currentPosition = location;

			if (currentPosition == null || App.checkParkArea(currentPosition.getLatitude(), currentPosition.getLongitude())) {
				setInsideParkingArea(true);
				rotationToParkingArea = 0.0f;
				//DLog.D(AMainOBC.class.toString() + " onLocationChanged: " + String.valueOf(isInsideParkingArea) + String.valueOf(currentPosition.getLatitude()) + " : " + String.valueOf(currentPosition.getLongitude()));

			} else {

				setInsideParkingArea(false);
				//DLog.D(AMainOBC.class.toString() + " onLocationChanged: " + String.valueOf(isInsideParkingArea) + String.valueOf(currentPosition.getLatitude()) + " : " + String.valueOf(currentPosition.getLongitude()));

				// Replace this three lines with the ones commented below

			}

			if (App.isNavigatorEnabled && isInsideParkingArea == lastIsInsideParkingArea) {

				FMap fMap = (FMap) getFragmentManager().findFragmentByTag(FMap.class.getName());

				if (fMap != null) {
					fMap.updateParkAreaStatus(isInsideParkingArea, rotationToParkingArea);
				}

				FHome fHome = (FHome) getFragmentManager().findFragmentByTag(FHome.class.getName());
				if (fHome != null && fHome.isVisible()) {
					fHome.updateParkAreaStatus(isInsideParkingArea, rotationToParkingArea);
				}

			} else {

				FDriving fDriving = (FDriving) getFragmentManager().findFragmentByTag(FDriving.class.getName());
				if (fDriving != null) {
					fDriving.updateParkAreaStatus(isInsideParkingArea, rotationToParkingArea);
				}
			}
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	public boolean isInsideParkArea() {
		return isInsideParkingArea;
	}

	public float getRotationToParkAngle() {
		return rotationToParkingArea;
	}

	public void setFragmentHandler(Handler handler) {
		fragmentHandler = handler;
	}

	private void queueTTS(String text) {
		try {
			if (!ProTTS.reqSystem) {
				ProTTS.askForSystem();
				setAudioSystem(LowLevelInterface.AUDIO_SYSTEM, LowLevelInterface.AUDIO_LEVEL_ALERT);
			}
			tts.speak(text);
			dlog.d("AMainOBC.queueTTS();leggo " + text);

		} catch (Exception e) {
			dlog.e("AMainOBC.queueTTS();exception while start speak", e);
		}

	}

	private void playAlertAdvice(int resID, String name) {
		try {
			if (!AudioPlayer.reqSystem) {
				AudioPlayer.askForSystem();
				setAudioSystem(LowLevelInterface.AUDIO_SYSTEM, LowLevelInterface.AUDIO_LEVEL_ALERT);
			}
			player.waitToPlayFile(Uri.parse("android.resource://eu.philcar.csg.OBC/" + resID));
			dlog.d("AMainOBC.playAlertAdvice();play " + name);

		} catch (Exception e) {
			dlog.e("AMainOBC.playAlertAdvice();exception while start speak", e);
		}

	}

	@Override
	public int getActivityUID() {
		return App.AMAINOBC_UID;
	}

	public void setAudioSystem(int mode, int volume) {
		dlog.d("AMainOBC.getActivityUID();" + mode + " TTSreqSystem is: " + ProTTS.reqSystem + " AudioPlayerReqSystem is: " + AudioPlayer.reqSystem);
		this.sendMessage(MessageFactory.AudioChannel(mode, volume));
	}

	public void calloutShowCode(View arg0) {//SHAME ON ME---btw prendo la view salvata per impostare il code del poi relativo al callout
		try {
			if (getActiveFragment() instanceof FMap) {
				Bundle poi = ((FMap) getActiveFragment()).getCalloutPoi();
				View customView = ((FMap) getActiveFragment()).getCalloutView();

				if (poi != null && customView != null) {

					switch (poi.getInt("type", 0)) {

						case 0:
							((FMap) getActiveFragment()).startRouteCallout();
							break;
						case 1:
							//if (((FMap) getActiveFragment()).getIsSecondCallout()) {
							//	((FMap) getActiveFragment()).setIsSecondCallout(false);
							((FMap) getActiveFragment()).hideCustomNavigationCallout();
							//	} else {

							//		((FMap) getActiveFragment()).setIsSecondCallout(true);
							//		((TextView) ((FMap) getActiveFragment()).getCalloutView().findViewById(R.id.customDescTV)).setText(((FMap) getActiveFragment()).getCalloutPoi().getString("code", "SURPRISE!!!!"));
							//	}
							break;
						case 2:
							//if (((FMap) getActiveFragment()).getIsSecondCallout()) {
							//		((FMap) getActiveFragment()).setIsSecondCallout(false);
							((FMap) getActiveFragment()).hideCustomNavigationCallout();
							//} else {

							//	((FMap) getActiveFragment()).setIsSecondCallout(true);
							//	((TextView) ((FMap) getActiveFragment()).getCalloutView().findViewById(R.id.customDescTV)).setText(((FMap) getActiveFragment()).getCalloutPoi().getString("code", "SURPRISE!!!!"));
							//}
							break;
						case 3:
							((FMap) getActiveFragment()).startRouteCallout();
							break;
					}

				}
			}
		} catch (Exception e) {
			dlog.e("AMainOBC.calloutShowCode();", e);
		}

	}

/*	public void setGPS(boolean status) {
		Intent intent = new Intent("android.location.GPS_ENABLED_CHANGE");
		intent.putExtra("enabled", status);
		sendBroadcast(intent);
	}*/

	public static class POI {

		public long id;
		public String title, address, description;
		public int distance, time;
		public Location location;

		private POI(long id, String title, String address, String description, int distance, int time, Location location) {
			this.id = id;
			this.title = title;
			this.address = address;
			this.description = description;
			this.distance = distance;
			this.time = time;
			this.location = location;
		}
	}

	public static class FuelStation extends POI {

		private FuelStation(long id, String title, String address, String description, int distance, int time, Location location) {
			super(id, title, address, description, distance, time, location);
		}
	}

	public static class GeocodedLocation {

		public Location location;
		public String address;

		public GeocodedLocation(String address, Location location) {
			this.address = address;
			this.location = location;
		}
	}

	static class MainOBCServiceHandler extends Handler {

		private final WeakReference<AMainOBC> mainOBCWeakReference;

		private MainOBCServiceHandler(WeakReference<AMainOBC> mainOBCWeakReference) {
			this.mainOBCWeakReference = mainOBCWeakReference;
		}

		@Override
		public void handleMessage(Message msg) {
			FMenu fMenu;
			FMap fMap;
//			FPark fPark;
			FHome fHome;

			// Random random = new Random();
			// Location l = new Location("dummyprovider");
			//(12.9788f, 45.9559f);
			// 9.1910f, 45.4602f

			//MI
			// l.setLatitude(45.4602f + random.nextFloat()*0.0 );
			// l.setLongitude( 9.1910f+ random.nextFloat()*0.0 );

			//Codroipo
			//l.setLatitude(45.9559f + random.nextFloat()*0.0 );
			//l.setLongitude( 12.9788f+ random.nextFloat()*0.0 );

			// l.setBearing(bearing);
			//bearing = (bearing+1) % 359;

			//onLocationChanged(l);

			if (!App.isForegroundActivity(mainOBCWeakReference.get())) {
				DLog.W("AMainOBC.MainOBCServiceHandler();MSG to non foreground activity");
				if (App.currentTripInfo == null) {
					DLog.W("AMainOBC.MainOBCServiceHandler();no trip found kill activity");

					mainOBCWeakReference.get().finish();
				}
				return;
			}

			switch (msg.what) {

				case ObcService.MSG_CLIENT_REGISTER:
					DLog.I("AMainOBC.MainOBCServiceHandler();MSG_CLIENT_REGISTER");
					break;

				case ObcService.MSG_CMD_TIMEOUT:
					DLog.I("AMainOBC.MainOBCServiceHandler();MSG_CMD_TIMEOUT");
					break;

				case ObcService.MSG_PING:
					DLog.I("AMainOBC.MainOBCServiceHandler();MSG_PING");
					break;

				case ObcService.MSG_IO_RFID:
					DLog.I("AMainOBC.MainOBCServiceHandler();MSG_IO_RFID");
					break;

				case ObcService.MSG_CAR_UPDATE:
				case ObcService.MSG_CAR_INFO:

					fMenu = (FMenu) mainOBCWeakReference.get().getFragmentManager().findFragmentByTag(FMenu.class.getName());
					if (fMenu != null) {
						fMenu.updateUIUsingAppValues();
					}

					fHome = (FHome) mainOBCWeakReference.get().getFragmentManager().findFragmentByTag(FHome.class.getName());
					if (fHome != null) {
						fHome.updateCarInfo((CarInfo) msg.obj);
					}

					fMap = (FMap) mainOBCWeakReference.get().getFragmentManager().findFragmentByTag(FMap.class.getName());
					if (fMap != null) {
						fMap.updateCarInfo((CarInfo) msg.obj);
					}

					break;

				case ObcService.MSG_TRIP_PARK:

					fMenu = (FMenu) mainOBCWeakReference.get().getFragmentManager().findFragmentByTag(FMenu.class.getName());

					if (fMenu != null) {

						fMenu.updateUIUsingAppValues();
					}

					break;

				case ObcService.MSG_TRIP_PARK_CARD_BEGIN:
				/*fPark = (FPark)getFragmentManager().findFragmentByTag(FPark.class.getName());

				if (fPark != null) {
					fPark.showBeginPark();
				}	*/

					fMenu = (FMenu) mainOBCWeakReference.get().getFragmentManager().findFragmentByTag(FMenu.class.getName());

					if (fMenu != null) {
						fMenu.updateUIUsingAppValues();
					}

					break;

				case ObcService.MSG_TRIP_PARK_CARD_END:
				/*fPark = (FPark)getFragmentManager().findFragmentByTag(FPark.class.getName());

				if (fPark != null) {
					fPark.showEndPark();
				}*/

					fMenu = (FMenu) mainOBCWeakReference.get().getFragmentManager().findFragmentByTag(FMenu.class.getName());

					if (fMenu != null) {
						fMenu.updateUIUsingAppValues();
					}

					break;

				case ObcService.MSG_TRIP_BEGIN:
					if (!App.pinChecked) {
						Intent in = new Intent(mainOBCWeakReference.get(), AWelcome.class);
						in.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						mainOBCWeakReference.get().startActivity(in);
						mainOBCWeakReference.get().finish();
					}
					break;
				case ObcService.MSG_TRIP_END:
					FMap fragmentMap = (FMap) mainOBCWeakReference.get().getFragmentManager().findFragmentByTag(FMap.class.getName());
					if (fragmentMap != null) {
						fragmentMap.stopRouteNavigation();
					}
/*
					FSearch fragmentSearch = (FSearch) mainOBCWeakReference.get().getFragmentManager().findFragmentByTag(FSearch.class.getName());
					if (fragmentSearch != null) {
						//fragmentSearch.shutdownSearch();
					}*/

					if (mainOBCWeakReference.get().serviceConnector.isConnected()) {
						mainOBCWeakReference.get().serviceConnector.unregister();
						mainOBCWeakReference.get().serviceConnector.disconnect();
					}

					Intent i = new Intent(mainOBCWeakReference.get(), AWelcome.class);
					i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					mainOBCWeakReference.get().startActivity(i);
					mainOBCWeakReference.get().finish();
					break;

				case ObcService.MSG_CAR_LOCATION:
					if (msg.obj instanceof Location) {
						mainOBCWeakReference.get().onLocationChanged((Location) msg.obj);
					}
					break;

				case ObcService.MSG_RADIO_CURPLAY_INFO:
				case ObcService.MSG_RADIO_SEEK_INFO:
				case ObcService.MSG_RADIO_SEEK_VALID_INFO:
				case ObcService.MSG_RADIO_SEEK_STATUS:
				case ObcService.MSG_RADIO_VOLUME_INFO:
					FRadio fRadio = (FRadio) mainOBCWeakReference.get().getFragmentManager().findFragmentByTag(FRadio.class.getName());
					if (fRadio != null) {
						Message fmsg = this.obtainMessage();
						fmsg.copyFrom(msg);
						fRadio.notifyRadioMsg(fmsg);
					}
					break;

				case ObcService.MSG_NAVIGATE_TO:

					if (msg.obj instanceof Location) {
						mainOBCWeakReference.get().navigateTo((Location) msg.obj);
					}

					break;
				case ObcService.MSG_TRIP_NEAR_POI:
					App.currentTripInfo.isBonusEnabled = msg.arg1 > 0;

					if (mainOBCWeakReference.get().firstUpCharging) {
						mainOBCWeakReference.get().firstUpCharging = false;
						if (App.USE_TTS_ALERT)

							mainOBCWeakReference.get().queueTTS(mainOBCWeakReference.get().getResources().getString(R.string.alert_bonus));
						else {
							switch (App.DefaultCity.toLowerCase()) {
								case "milano":
									mainOBCWeakReference.get().playAlertAdvice(R.raw.alert_tts_bonus_park, " alert bonus park");
									break;
								case "firenze":
									mainOBCWeakReference.get().playAlertAdvice(R.raw.alert_tts_bonus_park, " alert bonus park");
									break;
							}
						}
					}

					fMenu = (FMenu) mainOBCWeakReference.get().getFragmentManager().findFragmentByTag(FMenu.class.getName());
					if (fMenu != null) {
						fMenu.updateUIUsingAppValues();
					}

					fHome = (FHome) mainOBCWeakReference.get().getFragmentManager().findFragmentByTag(FHome.class.getName());
					if (fHome != null) {
						fHome.updatePoiInfo(msg.arg1, (Poi) msg.obj);
					}

					fMap = (FMap) mainOBCWeakReference.get().getFragmentManager().findFragmentByTag(FMap.class.getName());
					if (fMap != null) {
						fMap.updatePoiInfo(msg.arg1, (Poi) msg.obj);
					}
					break;

				default:
					super.handleMessage(msg);
			}
		}
	}

}