package eu.philcar.csg.OBC.controller.map;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.NetworkOnMainThreadException;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.util.LongSparseArray;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.skobbler.ngx.SKCategories.SKPOICategory;
import com.skobbler.ngx.SKCoordinate;
import com.skobbler.ngx.map.SKAnimationSettings;
import com.skobbler.ngx.map.SKAnnotation;
import com.skobbler.ngx.map.SKAnnotationView;
import com.skobbler.ngx.map.SKBoundingBox;
import com.skobbler.ngx.map.SKCalloutView;
import com.skobbler.ngx.map.SKCoordinateRegion;
import com.skobbler.ngx.map.SKMapCustomPOI;
import com.skobbler.ngx.map.SKMapCustomPOI.SKPoiType;
import com.skobbler.ngx.map.SKMapPOI;
import com.skobbler.ngx.map.SKMapSettings;
import com.skobbler.ngx.map.SKMapSettings.SKMapDisplayMode;
import com.skobbler.ngx.map.SKMapSettings.SKMapFollowerMode;
import com.skobbler.ngx.map.SKMapSurfaceListener;
import com.skobbler.ngx.map.SKMapSurfaceView;
import com.skobbler.ngx.map.SKMapSurfaceView.SKOrientationIndicatorType;
import com.skobbler.ngx.map.SKMapViewHolder;
import com.skobbler.ngx.map.SKPOICluster;
import com.skobbler.ngx.map.SKScreenPoint;
import com.skobbler.ngx.map.realreach.SKRealReachSettings;
import com.skobbler.ngx.navigation.SKNavigationListener;
import com.skobbler.ngx.navigation.SKNavigationManager;
import com.skobbler.ngx.navigation.SKNavigationSettings;
import com.skobbler.ngx.navigation.SKNavigationSettings.SKNavigationMode;
import com.skobbler.ngx.navigation.SKNavigationSettings.SKNavigationType;
import com.skobbler.ngx.navigation.SKNavigationState;
import com.skobbler.ngx.navigation.SKNavigationState.SKStreetType;
import com.skobbler.ngx.positioner.SKCurrentPositionProvider;
import com.skobbler.ngx.positioner.SKPosition;
import com.skobbler.ngx.positioner.SKPositionerManager;
import com.skobbler.ngx.reversegeocode.SKReverseGeocoderManager;
import com.skobbler.ngx.routing.SKRouteInfo;
import com.skobbler.ngx.routing.SKRouteJsonAnswer;
import com.skobbler.ngx.routing.SKRouteListener;
import com.skobbler.ngx.routing.SKRouteManager;
import com.skobbler.ngx.routing.SKRouteSettings;
import com.skobbler.ngx.routing.SKRouteSettings.SKRouteConnectionMode;
import com.skobbler.ngx.routing.SKRouteSettings.SKRouteMode;
import com.skobbler.ngx.sdktools.navigationui.SKToolsNavigationListener;
import com.skobbler.ngx.search.SKSearchResult;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import eu.philcar.csg.OBC.ABase;
import eu.philcar.csg.OBC.AMainOBC;
import eu.philcar.csg.OBC.AMainOBC.FuelStation;
import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.SystemControl;
import eu.philcar.csg.OBC.controller.FBase;
import eu.philcar.csg.OBC.controller.map.asynctask.GHAsyncTask;
import eu.philcar.csg.OBC.controller.map.util.GeoUtils;
import eu.philcar.csg.OBC.db.Pois;
import eu.philcar.csg.OBC.controller.welcome.adapter.LADamages;
import eu.philcar.csg.OBC.db.DbManager;
import eu.philcar.csg.OBC.db.Events;
import eu.philcar.csg.OBC.db.Poi;
import eu.philcar.csg.OBC.devices.LowLevelInterface;
import eu.philcar.csg.OBC.helpers.AudioPlayer;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.Debug;
import eu.philcar.csg.OBC.helpers.ProTTS;
import eu.philcar.csg.OBC.helpers.UrlTools;
import eu.philcar.csg.OBC.service.CarInfo;
import eu.philcar.csg.OBC.service.MessageFactory;

@SuppressLint("SimpleDateFormat")
public class FMap extends FBase implements OnClickListener {

	public static FMap newInstance() {
		FMap fm = new FMap();
		return fm;
	}

	private DLog dlog = new DLog(this.getClass());

	public final static int  BASE_TOLLERANCE = 50;					// Circle radius in meters (for fuel station on-map-tap)
	public final static byte BASE_ZOOM_LEVEL = 17;				// Maps-forge level
	public final static byte BASE_TOLLERANCE_INCREMENT = 50;	// meters


	private final static int  MSG_SHOW_REAL_REACH  = 1;
	private final static int  MSG_ZOOM_REAL_REACH  = 2;
	private final static int  MSG_HIDE_REAL_REACH  = 3;
	private final static int  MSG_START_NAVIGATION = 4;
	private final static int  MSG_STOP_NAVIGATION  = 5;
	private final static int  MSG_CENTER_MAP = 6;
	private final static int  MSG_CLOSE_CALLOUT = 7;
	private final static int  MSG_CLOSE_SOC_ALERT = 8;
	private final static int  MSG_OPEN_SOC_ALERT = 9;

	private static View  rootView;
	public static SKMapViewHolder mapHolder;
	private static SKMapSurfaceView mapView;


	private FrameLayout navigationFL, fmapAlarm, fmapRange;
	private Button endB, navigationB,  findDestinationB, fuelStationsB, homeB;
	private ImageView parkingStatusIV, parkingDirectionIV,  adIV, no3gIV;
	private TextView  dayTV, timeTV, tvRange, fmapAlertTV;
	private boolean seen=false,drawCharging=true,firstUpReceived=false;
	private static int statusAlertSOC=0,lastInside=0; //0 none played | 1 played 20km | 2 player 5km

	//anim
	private Animation alertAnimation;
	private static boolean animToggle=true, animFull=false ;
	private int playing=0;//0:no anim 1:anim3g 2:animArea 3:alertAnimation
	private List<String> animQueue =new ArrayList<String>();
	private long updateArea=0;
	private CarInfo localCarInfo;

	private static SKCoordinate calloutCoordinate=null;



	private static View panelRealReach, panelNavigation, panelNavMenu, no3gwarning;;

	private TextView no3gtxt,txtCurrentStreet;
	private TextView  txtNextStreet, titleAnnotation, descriptionAnnotation;
	private ArrayList<SKAnnotation> annotationList = new ArrayList<SKAnnotation>();

	public static CountDownTimer timer_2min, timer_5sec;

	private static List<View> panels = new ArrayList<View>();
	
	private RelativeLayout customView;
	private static ArrayList<Bundle> Icons = new ArrayList<Bundle>();
	private static ArrayList<Bundle> Pois = new ArrayList<Bundle>();
	

	private SKCurrentPositionProvider currentPositionProvider;
	private static SKPosition currentPosition = new SKPosition();
	private int range = 0;
	private static boolean navigationActive = false;
	private static boolean firstLaunch=true;
	private static String jsonmd5 = "";
	private static Boolean handleClick=false;
	public static Boolean started=false; //flag per non ripetere CDtimr



    private ProTTS tts;
	private AudioPlayer player;
	private UtteranceProgressListener utteranceListener;

	private boolean navigatorAT;

	private Uri uri;
	public FMap Instance;

    private boolean mapLoaded = false;
    private boolean showingFuelStations = false;
	private RelativeLayout fmap_top_LL;
	private static Boolean RequestBanner=false;
	public static Boolean firstRun=true;
	private static Context context;


	private static Boolean isSecondCallout=false;


	private static Bundle calloutPoi =null;

	public static Boolean getIsSecondCallout() {
		return isSecondCallout;
	}

	public static void setIsSecondCallout(Boolean isSecondCallout) {
		FMap.isSecondCallout = isSecondCallout;
	}

	private static View calloutView=null;
	public static View getCalloutView() {
		return calloutView;
	}
	public static Bundle getCalloutPoi() {
		return calloutPoi;
	}
	public FMap(){
		Instance = this;
	}





	@Override
    public void onCreate(Bundle savedInstanceState) {

    	super.onCreate(savedInstanceState);
		context=getActivity();
		player=new AudioPlayer(getActivity());
        App.isCloseable = false;


    }


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.f_map, container, false);

		dlog.d("OnCreareView FMap");
		rootView = view;
		if(App.Instance.BannerName.getBundle("CAR")==null&&!RequestBanner){          //App.Instance.BannerName.getBundle("CAR")==null&&
			//controllo se ho il banner e se non ho già iniziato a scaricarlo.
			RequestBanner=true;
			new Thread(new Runnable() {
				@Override
				public void run() {

					loadBanner(App.URL_AdsBuilderCar,"CAR",false);		//scarico banner
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							try{
								FMap fMap = (FMap)getFragmentManager().findFragmentByTag(FMap.class.getName());
								if (fMap != null) {

									updateBanner("CAR");                            //Modifico l'IV
									//RequestBanner = false;
								}
							}catch(Exception e){
								dlog.e("updateBanner: eccezione in chiamata",e);
							}
						}
					});
				}
			}).start();
		}


		if(App.Instance.BannerName.getBundle("END")==null){          //App.Instance.BannerName.getBundle("CAR")==null&&
			//controllo se ho il banner e se non ho già iniziato a scaricarlo.

			new Thread(new Runnable() {
				@Override
				public void run() {

					loadBanner(App.URL_AdsBuilderEnd,"END",false);		//scarico banner

				}
			}).start();
		}

		//uri = Uri.parse("android.resource://eu.philcar.csg.OBC/"+ R.raw.out_operative_area_tts_it);//avviso sonoro




    	FrameLayout frame = (FrameLayout)rootView.findViewById(R.id.fmapMapMV);

		if (new File("/sdcard/SKMaps/Maps/v1/20150413/meta/").exists()) {
			if (mapHolder == null) {
				mapHolder = new SKMapViewHolder(this.getActivity());
				mapHolder.setMapSurfaceListener(mapSurfaceListener);
				frame.addView(mapHolder);
			} else {
				frame.addView(mapHolder);
			}
		}





		tts=new ProTTS(getActivity());


		//seen=false;

		adIV = (ImageView)view.findViewById(R.id.fmapLeftBorderIV);

		navigationFL = (FrameLayout)view.findViewById(R.id.fmapNavitagionFL);
		navigationB = (Button)view.findViewById(R.id.fmapNavigationB);
		tvRange =  (TextView)view.findViewById(R.id.tvRange);
		fmap_top_LL=(RelativeLayout)view.findViewById(R.id.fmap_top_LL);

		titleAnnotation = (TextView)view.findViewById(R.id.titleAnnotationTV);
		descriptionAnnotation = (TextView)view.findViewById(R.id.descriptionAnnotationTV);

		no3gIV = (ImageView) view.findViewById(R.id.no3gIV_MAP);
		no3gwarning = view.findViewById(R.id.ll3G_MAP);
		no3gtxt = (TextView) view.findViewById(R.id.no3gtxt_MAP);

		endB = (Button)view.findViewById(R.id.fmapENDB);
		findDestinationB = (Button)view.findViewById(R.id.fmapSearchB);
		//fuelStationsB = (Button)view.findViewById(R.id.fmapFuelStationsB);
		homeB = (Button)view.findViewById(R.id.fmapHomeB);

		Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "interstateregular.ttf");




		parkingStatusIV = (ImageView)view.findViewById(R.id.fmapParkingStatusIV);
		parkingDirectionIV = (ImageView)view.findViewById(R.id.fmapParkingDirectionIV);
		fmapAlarm = (FrameLayout)view.findViewById(R.id.fmapAlarm);
		fmapRange = (FrameLayout)view.findViewById(R.id.fmapRange);


		fmapAlarm.setVisibility(View.INVISIBLE);
		fmapRange.setVisibility(View.INVISIBLE);

    	txtCurrentStreet = (TextView)rootView.findViewById(R.id.txtCurrentStreet);
    	txtCurrentStreet.setVisibility(View.INVISIBLE);
    	txtNextStreet = (TextView)rootView.findViewById(R.id.txtNextStreet);
    	txtNextStreet.setVisibility(View.INVISIBLE);

		fmapAlertTV = (TextView)rootView.findViewById(R.id.fmapAlertTV);
		navigationB.setTypeface(font);
		homeB.setTypeface(font);



		dayTV = (TextView)view.findViewById(R.id.fmap_date_TV);
		timeTV = (TextView)view.findViewById(R.id.fmap_hour_TV);



		((AMainOBC)getActivity()).setFragmentHandler(localActivityHandler);


		findDestinationB.setOnClickListener(this);
		endB.setOnClickListener(this);
		homeB.setOnClickListener(this);
		navigationB.setOnClickListener(this);
		Button btnCloseNav = (Button)view.findViewById(R.id.btnCloseNav);
		btnCloseNav.setOnClickListener(this);
		adIV.setOnClickListener(this);
		view.findViewById(R.id.fmapAlertCloseBTN).setOnClickListener(this);

		buildLeftPanels();

		//webViewBanner = (WebView) view.findViewById(R.id.WebViewBanner);

		//animation for the blink of the alert in the top right corner
		alertAnimation = new AlphaAnimation(0.0f, 1.0f);
		alertAnimation.setDuration(500); //You can manage the time of the blink with this parameter
		alertAnimation.setStartOffset(200);
		alertAnimation.setRepeatMode(Animation.REVERSE);
		alertAnimation.setRepeatCount(Animation.INFINITE);
		alertAnimation.setAnimationListener(new Animation.AnimationListener() {

			int index=0;
			String playing = "" ;

			@Override
			public void onAnimationStart(Animation animator) {
				dlog.d(FMap.class.toString()+"StartAnimation: alertAnimation");
				index=0;

				playing=animQueue.get(0);



				switch(playing){

					case "none"://no anim
						no3gwarning.setVisibility(View.INVISIBLE);
						break;
					case "area"://out of area
						no3gIV.setVisibility(View.GONE); //icona no3G
						// no3gIV.setImageResource(R.drawable.img_parking_p_green);
						no3gtxt.setText(R.string.outside_area);
						animToggle = !animToggle;
						no3gwarning.setBackgroundResource(R.drawable.sha_whiteroundedredbox);
						break;
					case "3g"://3G
						no3gIV.setVisibility(View.GONE);
						no3gIV.setImageResource(R.drawable.no_connection);
						no3gtxt.setText("NO 3G");
						animToggle = !animToggle;
						no3gwarning.setBackgroundResource(R.drawable.sha_whiteroundedredbox);
						break;
					case "bonus"://Bonus
						no3gIV.setVisibility(View.GONE);
						no3gtxt.setText("BONUS");
						no3gwarning.setBackgroundResource(R.drawable.sha_whiteroundedorangebox);
						animToggle = !animToggle;
						break;




				}
				animToggle=true;
				animFull=false;

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				dlog.d(FMap.class.toString()+"EndAnimation: alertAnimation");

			}

			@Override
			public void onAnimationRepeat(Animation animation) {
				if(animQueue.size()==0) {
					no3gwarning.clearAnimation();
					return;
				}

				if (animFull){
					index++;

					if(index>=animQueue.size())
						index=0;

					playing=animQueue.get(index);



					switch(playing){

						case "none"://no anim
							no3gwarning.setVisibility(View.INVISIBLE);
							break;
						case "area"://out of area
							no3gIV.setVisibility(View.GONE); //icona no3G
							// no3gIV.setImageResource(R.drawable.img_parking_p_green);
							no3gtxt.setText(R.string.outside_area);
							animToggle = !animToggle;
							no3gwarning.setBackgroundResource(R.drawable.sha_whiteroundedredbox);
							break;
						case "3g"://3G
							no3gIV.setVisibility(View.VISIBLE);
							no3gIV.setImageResource(R.drawable.no_connection);
							no3gtxt.setText("NO 3G");
							animToggle = !animToggle;
							no3gwarning.setBackgroundResource(R.drawable.sha_whiteroundedredbox);
							break;
						case "bonus"://Bonus
							no3gIV.setVisibility(View.GONE);
							no3gtxt.setText("BONUS");
							no3gwarning.setBackgroundResource(R.drawable.sha_whiteroundedorangebox);
							animToggle = !animToggle;
							break;




					}

					animFull=!animFull;
				}
				else
					animFull=!animFull;
			}

		});

		String jsonFile = "";

		File outDir = new File(App.POI_POSITION_FOLDER);
		if (!outDir.isDirectory()) {
			outDir.mkdir();
		}
		try {
			File outputFile = new File(outDir, "POIS_POS.json");
			if (outputFile.exists()) {
				jsonFile=getStringFromFile(outputFile.getPath());
				jsonmd5=md5(jsonFile);
				parseJsonPos(jsonFile);
			}

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}


		AMainOBC activity = (AMainOBC)getActivity();
		if (activity != null) {
			updateParkAreaStatus(activity.isInsideParkArea(), activity.getRotationToParkAngle());
		}

		if(App.first_UP_poi && App.hasNetworkConnection){
			new Thread(new Runnable() {
				public void run() {
					dlog.d(FMap.class.toString()+" onCreateView: Primo aggiornamento Poi");
					updatePoiIcon(App.URL_PoisIcons);
					updatePoiPos(App.URL_PoisBanner);
				}
			}).start();

		}

		if((new Date().getTime()- App.update_Poi.getTime())>600000 && App.hasNetworkConnection && !App.first_UP_poi) {    //3600000 = 1 ora


			new Thread(new Runnable() {
				public void run() {
					updatePoiIcon(App.URL_PoisIcons);
					updatePoiPos(App.URL_PoisBanner);
				}
			}).start();
		}

		App.first_UP_poi=false;







		if (App.currentTripInfo!=null && App.currentTripInfo.isMaintenance) {
			fmap_top_LL.setBackgroundColor(getResources().getColor(R.color.background_red));

		} else {
			fmap_top_LL.setBackgroundColor(getResources().getColor(R.color.background_green));
		}

		initNoTouchArea();






		//timer for advertisment update
		timer_2min = new CountDownTimer((120)*1000,1000) {
		@Override
		public void onTick(long millisUntilFinished) {

		}

		@Override
		public void onFinish() {
			new Thread(new Runnable() {
				@Override
				public void run() {
					loadBanner(App.URL_AdsBuilderCar,"CAR",false);
					try{
						if(getActivity()!=null)
						getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
								if(FMap.this.isVisible()) {
								updateBanner("CAR");
								//Modifico l'IV
							}
							else{
								FHome fHome = (FHome)getFragmentManager().findFragmentByTag(FHome.class.getName());
								if (fHome != null) {


									fHome.updateBanner("CAR");
									//Modifico l'IV
								}
							}
						}
					});

				}catch(Exception e){
						dlog.e("runOnUiThread: eccezione ",e);
					}
				timer_2min.start();
					started=false;
				}

			}).start();
		}

	};



	timer_5sec = new CountDownTimer((5)*1000,1000) {
		@Override
		public void onTick(long millisUntilFinished) {

		}

		@Override
		public void onFinish() {
			new Thread(new Runnable() {
				@Override
				public void run() {
					loadBanner(App.URL_AdsBuilderCar,"CAR",false);
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if(FMap.this.isVisible()) {
								updateBanner("CAR");
								timer_2min.start();//Modifico l'IV
							}
							else{
								FHome fHome = (FHome)getFragmentManager().findFragmentByTag(FHome.class.getName());
								if (fHome != null) {


									fHome.updateBanner("CAR");
									timer_2min.start();//Modifico l'IV
								}
								else{
									timer_2min.cancel();
								}
							}

						}
					});

				}
			}).start();
		}
	};


		if(firstRun) {
			firstRun=!firstRun;
			timer_2min.cancel();
			timer_2min.start();
		}
		updateBanner("CAR");

		if(App.fuel_level!=0)
			tvRange.setText( App.fuel_level + " Km");
		else
			fmapRange.setVisibility(View.INVISIBLE);


		if(!SystemControl.hasNetworkConnection(getActivity())){
			if(!animQueue.contains("3g"))
				animQueue.add("3g");
			if(no3gwarning.getAnimation()==null)
				no3gwarning.startAnimation(alertAnimation);
		}

		if(navigationActive){
			homeB.setVisibility(View.INVISIBLE);
			findDestinationB.setVisibility(View.INVISIBLE);
		}

		
		return view;
	}

/**
 * Resuming map and broadcast receiver
 * */
	@Override
	public void onResume() {
		super.onResume();
		if (new File("/sdcard/SKMaps/Maps/v1/20150413/meta/").exists()) {
			mapHolder.onResume();
			FrameLayout frame = (FrameLayout) rootView.findViewById(R.id.fmapMapMV);
			if (frame.getChildCount() == 0 && mapHolder != null) {
				frame.addView(mapHolder);
			}
		}

		try {
			getActivity().registerReceiver(this.ConnectivityChangeReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
		}catch(Exception e){
			dlog.e("onResume: eccezione nella registrazione del broadcast receiver ",e);
		}

		updateUI();
		//initWebBanner();
		//lastInside=true;



	}
/**
 * Pausing map and broadcast receiver*/
	public void onPause() {
		super.onPause();
		//seen=false;
		//timer_2min.cancel();
		//timer_5sec.cancel();
		//statusAlertSOC=0;
		//lastInside=1;
		//alertAnimation.cancel();
		if (new File("/sdcard/SKMaps/Maps/v1/20150413/meta/").exists()) {
			if(mapHolder!=null)
				mapHolder.onPause();
			//SKRouteManager.getInstance().clearCurrentRoute();

			//if(navigationActive) {
			//	stopRouteNavigation();
			//}

			FrameLayout frame = (FrameLayout) rootView.findViewById(R.id.fmapMapMV);


			frame.removeAllViews();
		}
		try{
			getActivity().unregisterReceiver(this.ConnectivityChangeReceiver);
		}catch(Exception e){
			dlog.e("onPause: Exception while unresisting receiver ",e);
		}


	}


	@Override
	public void onDetach() {
		super.onDetach();
		mapHolder=null;
		statusAlertSOC=0;

		localHandler.removeCallbacksAndMessages(null);
		if (currentPositionProvider!=null)
			currentPositionProvider.stopLocationUpdates();

		timer_2min.cancel();
		timer_5sec.cancel();
		firstRun=true;
		firstLaunch=true;
		drawCharging=true;
		if (tts!=null) {
			tts.shutdown();
			tts=null;
		}

	}
/**
 * Deleting references to free memory*/
	@Override
	public void onDestroy() {
		if(navigationActive){
			stopRouteNavigation();
		}
		adIV=null;
		rootView=null;
		panelNavigation=null;
		panelNavMenu=null;
		panelRealReach=null;
		panels=null;
		panels = new ArrayList<View>();
		fmapAlarm=null;
		fmapRange=null;
		tvRange=null;
		descriptionAnnotation=null;
		txtCurrentStreet=null;
		txtNextStreet=null;
		fmap_top_LL=null;
		navigationFL=null;
		navigationB=null;
		no3gwarning=null;
		no3gtxt=null;
		no3gIV=null;
		timeTV=null;
		dayTV=null;
		homeB=null;
		findDestinationB=null;
		endB=null;
		titleAnnotation=null;
		parkingDirectionIV=null;
		parkingStatusIV=null;
		fmapAlertTV=null;
		super.onDestroy();
	}
/**
 * Init no touch area for crash onClick on the bottom right write "provided by Scout by OSM"*/
	public void initNoTouchArea() {
		((FrameLayout)rootView.findViewById(R.id.flNoTouch)).setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent e) {
				return true;
			}

		});

	}

	/*public void initWebBanner() {

		if (webViewBanner==null || !App.hasNetworkConnection) {
			dlog.e("initWebBanner: webView==null or no connection);");
			adIV.setVisibility(View.VISIBLE);;
			return;
		}

		WebSettings webSettings =  webViewBanner.getSettings();
		webSettings.setAllowFileAccess(false);
		webSettings.setBuiltInZoomControls(false);
		webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
		webSettings.setSupportZoom(false);


		webViewBanner.setWebViewClient(new WebViewClient() {

			@Override
			public void onReceivedError (WebView view, int errorCode, String description, String failingUrl) {
				dlog.e("WebBanner: "+ failingUrl +" -- ErrorCode: " + errorCode);
				adIV.setImageResource(R.drawable.car_banner_offline);
				adIV.setVisibility(View.VISIBLE);
				dlog.d("WebBanner: disabled");
			}

		});

		// Attach JS interface object
		webViewBanner.addJavascriptInterface(new BannerJsInterface(), "OBC");

		List<NameValuePair> paramsList =  new ArrayList<NameValuePair>();

		if (App.currentTripInfo!=null && App.currentTripInfo.customer!=null)
			paramsList.add(new BasicNameValuePair("id", App.currentTripInfo.customer.id+""));

		if (App.lastLocation!=null) {
			paramsList.add(new BasicNameValuePair("lat", App.lastLocation.getLatitude()+""));
			paramsList.add(new BasicNameValuePair("lon", App.lastLocation.getLongitude()+""));
		}
		paramsList.add(new BasicNameValuePair("id_fleet", App.FleetId+""));
		paramsList.add(new BasicNameValuePair("carplate", App.CarPlate));

		HttpUrl url = UrlTools.buildQuery(App.URL_AdsBuilder, paramsList);
		if (url!=null) {
			String  strUrl =  url.toString();
			webViewBanner.loadUrl(strUrl);
			adIV.setVisibility(View.INVISIBLE);
			dlog.d("WebBanner enabled  : " + strUrl);
		}


	}
	public void initWebBanner(String Url) {

		if (webViewBanner==null || !App.hasNetworkConnection) {
			dlog.e(FMap.class.toString()+" initWebBanner: webView==null or no connection, load offline banner);");
			webViewBanner.setVisibility(View.GONE);
			adIV.setImageResource(R.drawable.car_banner_offline);
			adIV.setVisibility(View.VISIBLE);
			return;
		}

		WebSettings webSettings =  webViewBanner.getSettings();
		webSettings.setAllowFileAccess(false);
		webSettings.setBuiltInZoomControls(false);
		webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
		webSettings.setSupportZoom(false);


		webViewBanner.setWebViewClient(new WebViewClient() {

			@Override
			public void onReceivedError (WebView view, int errorCode, String description, String failingUrl) {
				dlog.e(FMap.class.toString()+"WebBanner: "+ failingUrl +" -- ErrorCode: " + errorCode+" load offline banner");
				adIV.setImageResource(R.drawable.car_banner_offline);
				adIV.setVisibility(View.VISIBLE);
				webViewBanner.setVisibility(View.GONE);
			}

		});

		// Attach JS interface object
		webViewBanner.addJavascriptInterface(new BannerJsInterface(), "OBC");

		//non ci sono parametri, visualizzo l'immagine.

		//HttpUrl url = UrlTools.buildQuery(Url, paramsList);
		if (Url!=null) {
			//String  strUrl =  url.toString();
			webViewBanner.loadUrl(Url);
			webViewBanner.setVisibility(View.VISIBLE);
			adIV.setVisibility(View.INVISIBLE);
			dlog.d(FMap.class.toString()+" initWebBanner: load single image " + Url);
		}else{

			dlog.e(FMap.class.toString()+" initWebBanner: Url is null banner offline ");
			adIV.setImageResource(R.drawable.car_banner_offline);
			adIV.setVisibility(View.VISIBLE);

		}


	}*/

	/**
	 * unused function for advertisement update*/
	public void updateAd(File theFile) {

    	if(theFile != null && theFile.exists()){

    		Bitmap myBitmap = null;
    		BitmapFactory.Options options = new BitmapFactory.Options();
    		options.inSampleSize = 1;

    		boolean success = false;
    		while (!success) {

    			try {
    				myBitmap = BitmapFactory.decodeFile(theFile.getAbsolutePath(), options);
    				success = true;
    			} catch (OutOfMemoryError err) {
    				options.inSampleSize*=2;
    			}
    	    }

    		try {
    			adIV.setImageBitmap(myBitmap);
    		} catch (OutOfMemoryError err) {

    		}
    	}
    }

	/**
	 * Update the area status for alert the user via top right alert and vocal message*/
	public void updateParkAreaStatus(boolean isInside, float rotationAngle) {


		//isInside = false;
		//rotationAngle =0;
		//dlog.d(FMap.class.toString()+"updateParkAreaStatus: "+String.valueOf(isInside));

		if (isInside) {

				if(animQueue.contains("area")){
					animQueue.remove("area");
				}
				if(lastInside!=0 && lastInside!=1)
					Events.outOfArea(false);
				lastInside=0;


		} else {
			updateArea=0;
			if(!animQueue.contains("area")){
				animQueue.add("area");
			}
			if(no3gwarning.getAnimation()==null)
				no3gwarning.startAnimation(alertAnimation);



			if (lastInside==0) {
				lastInside=2;


					//((AMainOBC) getActivity()).player.inizializePlayer();
				if(App.USE_TTS_ALERT)
					queueTTS(getActivity().getResources().getString(R.string.alert_area));
				else
					playAlertAdvice(R.raw.out_operative_area_tts," alert area");
				}
			else{
				if(lastInside==1){
					lastInside=2;


				}
			}



		}

		//parkingDirectionIV.setRotation((float)rotationAngle);
	}

   /* public void updateParkAreaStatus(boolean isInside, float rotationAngle) {

		//isInside = false;
		//rotationAngle =0;

		if (isInside) {
			outsideAreaWarning.setVisibility(View.INVISIBLE);
			parkingStatusIV.setImageResource(R.drawable.img_parking_p_green);
    		//parkingDirectionIV.setImageResource(R.drawable.img_parking_arrow_off);
    	} else {

			((AMainOBC)getActivity()).player.inizializePlayer();
			((AMainOBC)getActivity()).player.reqSystem=true;
			((AMainOBC)getActivity()).setAudioSystem(LowLevelInterface.AUDIO_SYSTEM);
			new Thread(new Runnable() {
				public void run() {
					((AMainOBC)getActivity()).player.playFile(uri);
				}
			}).start();



			outsideAreaWarning.setVisibility(View.VISIBLE);
    		parkingStatusIV.setImageResource(R.drawable.img_parking_p_green);
    		//parkingDirectionIV.setImageResource(R.drawable.img_parking_arrow);
    	}

		//parkingDirectionIV.setRotation((float)rotationAngle);
	}*/
	/**
	 * Update the Range indicator and handle the out of charge button display
	 * */
    public void updateCarInfo(CarInfo carInfo) {

    	if (carInfo==null)
    		return;

		localCarInfo=carInfo;
		int SOC = carInfo.batteryLevel;

		// if Soc==0 means that software has just started, or it is a demo kit or we lost some connection
		// don't show anything for now
		if (SOC==0) {
			fmapAlarm.setVisibility(View.INVISIBLE);
			fmapRange.setVisibility(View.INVISIBLE);
			return;
		}

		if(FMap.this.isVisible())
		if(SOC<15) {
			/*rootView.findViewById(R.id.fmapAlarmIV).setBackgroundResource(R.drawable.outofcharge);
			if(!seen) {
				//drawChargingStation();
				seen=true;
				rootView.findViewById(R.id.fmapAlarmIV).setBackgroundResource(R.drawable.outofcharge);
				rootView.findViewById(R.id.fmapAlertSOCFL).setVisibility(View.VISIBLE);
				((FrameLayout) (rootView.findViewById(R.id.fmapAlertSOCFL))).setBackgroundResource(R.drawable.sha_redalertbox);
				((TextView) (rootView.findViewById(R.id.fmapAlertTitleTV))).setText(R.string.alert_warning_title);
				((TextView) (rootView.findViewById(R.id.fmapAlertDescTV))).setText(R.string.alert_5km);
				localHandler.sendEmptyMessageDelayed(MSG_OPEN_SOC_ALERT, 120000);
				//localHandler.removeMessages(MSG_CLOSE_SOC_ALERT);
				//localHandler.sendEmptyMessageDelayed(MSG_CLOSE_SOC_ALERT, 20000);
				//(rootView.findViewById(R.id.fmapAlertSOCFL)).invalidate();
			}*/
			if(statusAlertSOC<=1) {
				dlog.d("Display popup 5km");
				statusAlertSOC=2;
				Events.eventSoc(SOC,"Popup 5km");
				rootView.findViewById(R.id.fmapAlertSOCFL).setVisibility(View.VISIBLE);
				rootView.findViewById(R.id.fmapAlarmIV).setBackgroundResource(R.drawable.outofcharge);
				((FrameLayout) (rootView.findViewById(R.id.fmapAlertSOCFL))).setBackgroundResource(R.drawable.sha_redalertbox);
				((TextView) (rootView.findViewById(R.id.fmapAlertTitleTV))).setText(R.string.alert_warning_title);
				((TextView) (rootView.findViewById(R.id.fmapAlertDescTV))).setText(R.string.alert_5km);
				localHandler.sendEmptyMessageDelayed(MSG_OPEN_SOC_ALERT, 120000);
				//((AMainOBC) getActivity()).player.inizializePlayer();
				if(App.USE_TTS_ALERT)
					queueTTS(getActivity().getResources().getString(R.string.alert_5km));
				else
					playAlertAdvice(R.raw.alert_tts_5km," alert 5km");
			}
		}else
			if(SOC<=30) {
				/*rootView.findViewById(R.id.fmapAlarmIV).setBackgroundResource(R.drawable.almostoutofcharge);
				if(!seen) {
					//drawChargingStation();
					seen=true;
					rootView.findViewById(R.id.fmapAlertSOCFL).setVisibility(View.VISIBLE);
					rootView.findViewById(R.id.fmapAlarmIV).setBackgroundResource(R.drawable.almostoutofcharge);
					((FrameLayout) (rootView.findViewById(R.id.fmapAlertSOCFL))).setBackgroundResource(R.drawable.sha_orangealertbox);
					((TextView) (rootView.findViewById(R.id.fmapAlertTitleTV))).setText(R.string.alert_warning_title);

					((TextView) (rootView.findViewById(R.id.fmapAlertDescTV))).setText(R.string.alert_20km);
					//localHandler.removeMessages(MSG_CLOSE_SOC_ALERT);
					//localHandler.sendEmptyMessageDelayed(MSG_CLOSE_SOC_ALERT, 20000);
					//(rootView.findViewById(R.id.fmapAlertSOCFL)).invalidate();
				}*/
				if (statusAlertSOC <= 0) {
					dlog.d("Display popup 20km");

					statusAlertSOC = 1;
					Events.eventSoc(SOC,"Popup 20km");
					rootView.findViewById(R.id.fmapAlarmIV).setBackgroundResource(R.drawable.almostoutofcharge);
					rootView.findViewById(R.id.fmapAlertSOCFL).setVisibility(View.VISIBLE);
					((FrameLayout) (rootView.findViewById(R.id.fmapAlertSOCFL))).setBackgroundResource(R.drawable.sha_orangealertbox);
					((TextView) (rootView.findViewById(R.id.fmapAlertTitleTV))).setText(R.string.alert_warning_title);

					((TextView) (rootView.findViewById(R.id.fmapAlertDescTV))).setText(R.string.alert_20km);
					//localHandler.sendEmptyMessageDelayed(MSG_CLOSE_SOC_ALERT, 20000);
					//(rootView.findViewById(R.id.fmapAlertSOCFL)).invalidate(); testinva

					//((AMainOBC) getActivity()).player.inizializePlayer();
					if(App.USE_TTS_ALERT)
						queueTTS(getActivity().getResources().getString(R.string.alert_20km));
					else
						playAlertAdvice(R.raw.alert_tts_20km," alert 20km");
				}
			}



		if (SOC<=30) {
			if(SOC>15)
				rootView.findViewById(R.id.fmapAlarmIV).setBackgroundResource(R.drawable.almostoutofcharge);
			else
				rootView.findViewById(R.id.fmapAlarmIV).setBackgroundResource(R.drawable.outofcharge);
			fmapAlarm.setVisibility(View.VISIBLE);
			fmapRange.setVisibility(View.INVISIBLE);

		} else {
			//statusAlertSOC=0;  //commented to limit
			localHandler.sendEmptyMessage(MSG_CLOSE_SOC_ALERT);
			rootView.findViewById(R.id.fmapAlertSOCFL).setVisibility(View.GONE);
			fmapAlarm.setVisibility(View.INVISIBLE);
			fmapRange.setVisibility(View.VISIBLE);
			tvRange.setText((SOC>=50?SOC:SOC-10) + " Km");
		}

		range = carInfo.rangeKm;
		//ShowRealReachTimed(carInfo.rangeKm,10000);

    }

	/**
	 * Update the ui for display the navigation button*/
	private void updateUI() {


		if (this.navigationActive) {
			((Button)rootView.findViewById(R.id.btnCloseNav)).setVisibility(View.VISIBLE);
		}



/*		else
			((Button)rootView.findViewById(R.id.btnCloseNav)).setVisibility(View.INVISIBLE);*/


	}
/**
 * reset the ui before going in home*/
	private boolean resetUI() {

		if (((AMainOBC)getActivity()).getFuelStation() != null) {

			((AMainOBC)getActivity()).setFuelStation(null);
			((AMainOBC)getActivity()).setEndingPosition(null);
			((AMainOBC)getActivity()).setCurrentRouting(null);


			if (showingFuelStations) {
				drawFuelStationsOnMap();
			}

			updateUI();

			return true;

		} else if (((AMainOBC)getActivity()).getPOI() != null) {

			((AMainOBC)getActivity()).setPOI(null);
			((AMainOBC)getActivity()).setEndingPosition(null);
			((AMainOBC)getActivity()).setCurrentRouting(null);


			if (showingFuelStations) {
				drawFuelStationsOnMap();
			}

			updateUI();

			return true;
		}

		return false;
	}









	@Override
	public void onClick(View v) {

		switch (v.getId()) {

		case R.id.fmapSearchB://Search
			if (!navigationActive)
				((ABase)getActivity()).pushFragment(FSearch.newInstance(), FSearch.class.getName(), true);
			break;

		case R.id.fmapHomeB:
			if (!navigationActive) {
				((ABase) getActivity()).pushBackFragment(FHome.newInstance(), FHome.class.getName(), true);

			}else{
				Toast toast = Toast.makeText(getActivity(),R.string.navigation_active_error , Toast.LENGTH_LONG);
				toast.show();
			}
				//((AMainOBC)getActivity()).setGPS(true);

			break;

		case R.id.fmapRadioB:
			if (!navigationActive) {
				((ABase) getActivity()).pushFragment(FRadio.newInstance(), FRadio.class.getName(), true);
			}else{

				Toast toast = Toast.makeText(getActivity(),R.string.navigation_active_error , Toast.LENGTH_LONG);
				toast.show();
			}
			break;

		case R.id.fmapAlertCloseBTN:

			localHandler.sendEmptyMessage(MSG_CLOSE_SOC_ALERT);

			break;

		/*case R.id.fmapPOIIB:
			((ABase)getActivity()).pushFragment(FPOI.newInstance(true), FPOI.class.getName(), true);
			break;*/

		/*case R.id.fmapFuelStationsB:

			((AMainOBC)getActivity()).setFuelStation(null);
			((AMainOBC)getActivity()).setPOI(null);
			((AMainOBC)getActivity()).setEndingPosition(null);
			((AMainOBC)getActivity()).setCurrentRouting(null);

			if (showingFuelStations) {
				fuelStationsB.setSelected(false);
				showingFuelStations = false;
				updateUI();
			} else {
				fuelStationsB.setSelected(true);
				drawFuelStationsOnMap();
			}

			break;*/

			
		case R.id.fmapENDB:
			((ABase)getActivity()).pushFragment(FMenu.newInstance(), FMenu.class.getName(), true);

			break;

		case R.id.fmapCancelB:
			timer_2min.cancel();
			timer_5sec.cancel();
			firstRun=true;
			if (!resetUI()) {
				((ABase)getActivity()).pushFragment( new FMenu(), FMenu.class.getName(), true);
			}

			break;

		case R.id.fmapNavigationB:
		case R.id.btnCloseNav:

			if (navigationActive)
				localHandler.sendEmptyMessage(MSG_STOP_NAVIGATION);

			mapView.deleteAnnotation(9);
			mapView.deleteAnnotation(0);
			break;

		case R.id.fmapLeftBorderIV:
			if(App.Instance.BannerName.getBundle("CAR")!=null){
				if(App.Instance.BannerName.getBundle("CAR").getString("CLICK").compareTo("null")!=0){

					if(!handleClick) {
						adIV.setColorFilter(R.color.overlay_banner);
						timer_2min.cancel();
						handleClick=true;
						dlog.d(FMap.class.toString()+" Banner: Click su banner ");
						new Thread(new Runnable() {
							@Override
							public void run() {

								loadBanner(App.BannerName.getBundle("CAR").getString("CLICK"), "CAR", true);
								getActivity().runOnUiThread(new Runnable() {
									@Override
									public void run() {
										if(FMap.this.isVisible()) {
											adIV.clearColorFilter();
											updateBanner("CAR");
											timer_2min.cancel();
											timer_5sec.start();//Modifico l'IV
											handleClick = false;
										}
									}
								});

							}
						}).start();
					}
				}
			}
			break;

		}
	}


	private void drawDestination(SKCoordinate destination) {

		SKAnnotation annotation = new SKAnnotation(0);
		annotation.setLocation(destination);
		annotation.setUniqueID(0);
		annotation.setMininumZoomLevel(5);
		annotation.setAnnotationType(SKAnnotation.SK_ANNOTATION_TYPE_DESTINATION_FLAG);
		mapView.addAnnotation(annotation, SKAnimationSettings.ANIMATION_PIN_DROP);

	}
	private boolean drawPOIS() {

		if(mapView==null || getActivity()==null) {
			dlog.d(" drawPois: mapViw null or contemporary called, or activity null");
			return false;
		}

		try {
			mapView.deleteAllAnnotationsAndCustomPOIs();
			customView=(RelativeLayout) ((LayoutInflater) (getActivity()).getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
					R.layout.layout_custom_view_poi, null, false);
			List<SKAnnotation> MapAnnotations = mapView.getAllAnnotations();
			ArrayList<Bundle> PoisTemp = Pois;

			annotationList.clear();
			customView.requestLayout();
			for (Bundle poi : PoisTemp) {
				Bitmap myBitmap;
				SKAnnotation annotation = new SKAnnotation(poi.getInt("INDEX"));
				annotation.setLocation(new SKCoordinate(poi.getDouble("LON"), poi.getDouble("LAT")));
				switch(poi.getInt("type",0)){
					case 0://bring me there
						annotation.setMininumZoomLevel(9);
						break;
					case 1://gift event
						annotation.setMininumZoomLevel(9);
						break;
					case 2://christmas event
						annotation.setMininumZoomLevel(17);
						break;
					case 3://Charging station
						continue;
				}
				//annotation.setAnnotationType(SKAnnotation.SK_ANNOTATION_TYPE_GREEN);
				SKAnnotationView annotationView = new SKAnnotationView();



				if (customView.findViewById(R.id.customView_poi) != null) {
					File imgFile = new File(App.POI_ICON_FOLDER.concat(poi.getString("id_icon")).concat(".png"));
					if(imgFile==null || !imgFile.exists())
						continue;
					myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
					((ImageView) (customView.findViewById(R.id.customView_poi))).setImageBitmap(myBitmap);

					//((ImageView) (customView.findViewById(R.id.customView_poi))).invalidate(); testinva
					annotationView.setView(customView.findViewById(R.id.customView_poi));

				}
				annotation.setAnnotationView(annotationView);

				//annotation.setAnnotationType(SKAnnotation.SK_ANNOTATION_TYPE_DESTINATION_FLAG);

					annotationList.add(annotation);
					mapView.addAnnotation(annotation, SKAnimationSettings.ANIMATION_NONE);

			}
			//mapView.deleteAllAnnotationsAndCustomPOIs();

			customView.removeAllViews();
		}catch (Exception e){
			DLog.E("error handling bitmap", e);

			return false;
		}


		return true;
	}
	
	private void drawFuelStationsOnMap() {

		LongSparseArray<FuelStation> fuelStations = ((AMainOBC)getActivity()).computeFuelStations();

		for (int i=0; i<fuelStations.size(); i++) {
			FuelStation aFuelStation = fuelStations.valueAt(i);
			//mapView.getLayerManager().getLayers().add(createMarker(aFuelStation.location, R.drawable.ico_map_fuel_station));
		}

		showingFuelStations = true;
	}


	/*private void showCustomPois() {
		
		DbManager dbm = App.Instance.getDbManager();
		Pois dao = dbm.getPoisDao();
		List<Poi> pois = dao.getPois("charge");
		
		SKAnnotationView annotationView = new SKAnnotationView();
		ImageView img = new ImageView(this.getActivity());
		img.setImageResource(R.drawable.poi_charge);
		annotationView.setView(img);
		
		
	    for (Poi p : pois) {
			SKCoordinate point = new SKCoordinate();
			point.setLatitude(p.lat);
			point.setLongitude(p.lon);
			
			SKAnnotation annotation = new SKAnnotation(p.id);
			annotation.setLocation(point);
			annotation.setUniqueID(p.id);
			annotation.setMininumZoomLevel(5);
			annotation.setAnnotationView(annotationView);
			
			annotation.setOffset(new SKScreenPoint(0,32));
			
			annotation.setAnnotationType(SKAnnotation.SK_ANNOTATION_TYPE_MARKER);
			mapView.addAnnotation(annotation, SKAnimationSettings.ANIMATION_NONE);
			
	    }
		

	   
		
	}*/
	
	



	private String  createMarker(Location  location, int resource) {
		return "";
	}




	private static SKCalloutView navigationCallout = null;

	private  void confirmNavigation(final SKScreenPoint point) {

		if (navigationCallout!=null)
			navigationCallout.hide();

		final SKCoordinate position = mapView.pointToCoordinate(point);
		navigationCallout = mapHolder.getCalloutView();
		navigationCallout.setTitle("Conferma destinazione");
		SKSearchResult address = SKReverseGeocoderManager.getInstance().reverseGeocodePosition(mapView.pointToCoordinate(point));
		navigationCallout.setDescription(address!=null?address.getName():"" );
		//navigationCallout.setDescription("Destinazione");
		navigationCallout.setLeftImage(null);
		navigationCallout.setOnRightImageClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				navigationCallout.hide();
				startRoute(position,true);
			}

		});
		navigationCallout.setOnTextClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				navigationCallout.hide();
				startRoute(position,true);
			}

		});
		navigationCallout.showAtLocation(position, true);

	}
	private  void confirmNavigation(final SKCoordinate position, Bundle Poi) {

		if (navigationCallout!=null)
			navigationCallout.hide();

		//final SKCoordinate position = mapView.pointToCoordinate(point);
		navigationCallout = mapHolder.getCalloutView();
		navigationCallout.setTitle(Poi.getString("nome","Raggiungi destinazione"));
		navigationCallout.setTitleTextSize(25);
		navigationCallout.setDescription(Poi.getString("address"," "));
		navigationCallout.setLeftImage(null);
		navigationCallout.setOnRightImageClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				navigationCallout.hide();
				startRoute(position,true);
			}

		});
		navigationCallout.setOnTextClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				navigationCallout.hide();
				startRoute(position,true);
			}

		});
		navigationCallout.showAtLocation(position, true);


	}

	private  void eventCallout(final SKCoordinate position, Bundle Poi, int topOffset) {

		calloutPoi = Poi;
		calloutCoordinate = position;

		if(getActivity()==null){
			dlog.e("eventCallout: Activity null");
			return;}
		customView=(RelativeLayout) ((LayoutInflater) (getActivity()).getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.layout_custom_view, null, false);
		customView.requestLayout();


		if (navigationCallout!=null) {
			navigationCallout.hide();
			navigationCallout.removeAllViews();
		}

		//final SKCoordinate position = mapView.pointToCoordinate(point);
		navigationCallout = mapHolder.getCalloutView();
		//navigationCallout.setTitle(Poi.getString("nome","Raggiungi destinazione"));
		//navigationCallout.setTitleTextSize(25);
		//navigationCallout.setDescription("1111111111222222222233333333334444444444");//Poi.getString("address"," "));
		//navigationCallout.setLeftImage(null);
		if (customView.findViewById(R.id.customView) != null) {
			try {
				switch(Poi.getInt("type",0)) {
					case 0:
						((ImageView) (customView.findViewById(R.id.customView))).setImageResource(R.drawable.bring_me_callout);
						((TextView) (customView.findViewById(R.id.customTitleTV))).setText(Poi.getString("nome","Raggiungi destinazione"));
						((TextView) (customView.findViewById(R.id.customDescTV))).setText(Poi.getString("address"," "));
						break;
					case 1:
						((ImageView) (customView.findViewById(R.id.customView))).setImageResource(R.drawable.gift_callout_rev);
						((TextView) (customView.findViewById(R.id.customTitleTV))).setText(Poi.getString("nome", "Gift"));
						((TextView) (customView.findViewById(R.id.customDescTV))).setText(Poi.getString("description", ""));
						break;
					case 2:
						((ImageView) (customView.findViewById(R.id.customView))).setImageResource(R.drawable.santa_callout);
						((TextView) (customView.findViewById(R.id.customTitleTV))).setText(Poi.getString("nome", "Event"));
						((TextView) (customView.findViewById(R.id.customTitleTV))).setTextColor(Color.BLACK);
						((TextView) (customView.findViewById(R.id.customDescTV))).setText(Poi.getString("description", ""));
						((TextView) (customView.findViewById(R.id.customDescTV))).setTextColor(Color.BLACK);
						break;
					case 3:
						((ImageView) (customView.findViewById(R.id.customView))).setImageResource(R.drawable.bring_me_callout);
						((TextView) (customView.findViewById(R.id.customTitleTV))).setText(R.string.poi_bonus_title);
						((TextView) (customView.findViewById(R.id.customTitleTV))).setTextColor(Color.BLACK);
						((TextView) (customView.findViewById(R.id.customDescTV))).setText(R.string.poi_bonus_body);
						((TextView) (customView.findViewById(R.id.customDescTV))).setTextColor(Color.BLACK);
						break;
				}
				navigationCallout.setCustomView(customView);
				calloutView = customView;
				navigationCallout.setVerticalOffset(topOffset);

			}catch(Exception e){
				dlog.e("Exception during creation of custom layout",e);
			}

		}

		navigationCallout.showAtLocation(position, false);

		mapView.centerMapOnPositionSmooth(position,1);



	}


	public void navigateTo(Location location) {
		SKCoordinate destination = new SKCoordinate();
		destination.setLatitude(location.getLatitude());
		destination.setLongitude(location.getLongitude());
		dlog.d("NavigateTo: set destination to "+location.getLatitude() +" "+ location.getLongitude());
		startRoute(destination);
	}

	private void resetMapSettings() {
    	SKCoordinate currentPoint = new SKCoordinate(12.9788f, 45.9559f);

        if (App.lastLocation!=null && App.lastLocation.getLatitude()!=0 && App.lastLocation.getLongitude()!=0) {
        	currentPoint.setLatitude(App.lastLocation.getLatitude());
        	currentPoint.setLongitude(App.lastLocation.getLongitude());
        }


        //SKCoordinate currentPoint = new SKCoordinate(9.1910f, 45.4602f);
        mapView.setPositionAsCurrent(currentPoint, 0, true);
        mapView.rotateTheMapToNorth();
        mapView.setZoom(18);
        mapView.centerMapOnPosition(currentPoint);
        mapView.getMapSettings().setFollowerMode(SKMapFollowerMode.NAVIGATION);
        mapView.getMapSettings().setOneWayArrows(true);
        mapView.getMapSettings().setOrientationIndicatorType(SKOrientationIndicatorType.CUSTOM_IMAGE);
        mapView.getMapSettings().setHouseNumbersShown(true);
        mapView.getMapSettings().setImportantPoisShown(true);
        mapView.getMapSettings().setInertiaPanningEnabled(true);
        mapView.getMapSettings().setMapZoomingEnabled(true);
        mapView.getMapSettings().setMapRotationEnabled(true);
        mapView.getMapSettings().setCompassShown(true);
        mapView.getMapSettings().setMapDisplayMode(SKMapSettings.SKMapDisplayMode.MODE_2D);
        currentPosition.setCoordinate(currentPoint);
        SKPositionerManager.getInstance().reportNewGPSPosition(currentPosition);
        
        //showCustomPois();
	}

	private void initMapSettings() {
		SKCoordinate currentPoint = new SKCoordinate(12.9788f, 45.9559f);

		if (App.lastLocation!=null && App.lastLocation.getLatitude()!=0 && App.lastLocation.getLongitude()!=0) {
			currentPoint.setLatitude(App.lastLocation.getLatitude());
			currentPoint.setLongitude(App.lastLocation.getLongitude());
		}


		//SKCoordinate currentPoint = new SKCoordinate(9.1910f, 45.4602f);
		mapView.setPositionAsCurrent(currentPoint, 0, true);
		mapView.rotateTheMapToNorth();
		mapView.setZoom(18);
		mapView.centerMapOnPosition(currentPoint);
		mapView.getMapSettings().setFollowerMode(SKMapFollowerMode.NAVIGATION);
		mapView.getMapSettings().setOneWayArrows(true);
		mapView.getMapSettings().setOrientationIndicatorType(SKOrientationIndicatorType.CUSTOM_IMAGE);
		mapView.getMapSettings().setHouseNumbersShown(true);
		mapView.getMapSettings().setImportantPoisShown(true);
		mapView.getMapSettings().setInertiaPanningEnabled(true);
		mapView.getMapSettings().setMapZoomingEnabled(true);
		mapView.getMapSettings().setMapRotationEnabled(true);
		mapView.getMapSettings().setCompassShown(true);
		mapView.getMapSettings().setMapDisplayMode(SKMapSettings.SKMapDisplayMode.MODE_2D);
		currentPosition.setCoordinate(currentPoint);
		SKPositionerManager.getInstance().reportNewGPSPosition(currentPosition);
	}


	public void startRouteCallout(){
		if(navigationCallout!=null)
			navigationCallout.hide();
		if(calloutCoordinate!=null)
			startRoute(calloutCoordinate,true);
	}

	private void startRoute(SKCoordinate destination) {
		SKRouteManager.getInstance().clearAllRoutesFromCache();
        // get a route object and populate it with the desired properties
        SKRouteSettings route = new SKRouteSettings();
        // set start and destination points
        route.setStartCoordinate(currentPosition.getCoordinate());
        route.setDestinationCoordinate(destination);
        // set the number of routes to be calculated
        route.setNoOfRoutes(1);
        route.setRouteExposed(true);
		//route.setTollRoadsAvoided(true);
		route.setHighWaysAvoided(true);
        // set the route mode
        route.setRouteMode(SKRouteMode.CAR_FASTEST);
        // set whether the route should be shown on the map after it's computed
        route.setRouteExposed(true);
        // set the route listener to be notified of route calculation
        // events

        route.setRouteConnectionMode(SKRouteConnectionMode.OFFLINE);
        SKRouteManager.getInstance().setRouteListener(routeListener);
        // pass the route to the calculation routine
        SKRouteManager.getInstance().calculateRoute(route);

        drawDestination(destination);
	}

	private void startRoute(SKCoordinate destination, boolean draw) {
		SKRouteManager.getInstance().clearAllRoutesFromCache();
		// get a route object and populate it with the desired properties
		SKRouteSettings route = new SKRouteSettings();
		// set start and destination points
		route.setStartCoordinate(currentPosition.getCoordinate());
		route.setDestinationCoordinate(destination);
		// set the number of routes to be calculated
		route.setNoOfRoutes(1);
		route.setRouteExposed(true);
		route.setHighWaysAvoided(true);
		// set the route mode
		route.setRouteMode(SKRouteMode.CAR_FASTEST);
		// set whether the route should be shown on the map after it's computed
		route.setRouteExposed(true);
		// set the route listener to be notified of route calculation
		// events

		route.setRouteConnectionMode(SKRouteConnectionMode.OFFLINE);
		SKRouteManager.getInstance().setRouteListener(routeListener);
		// pass the route to the calculation routine
		SKRouteManager.getInstance().calculateRoute(route);

		if(draw)
			drawDestination(destination);
	}


    private void startRouteNavigation() {
		try {
    	/*
        if (TrackElementsActivity.selectedTrackElement != null) {
            mapView.clearTrackElement(TrackElementsActivity.selectedTrackElement);
        }
        */

			showLeftPanel(0, 1);

			// get navigation settings object
			SKNavigationSettings navigationSettings = new SKNavigationSettings();

			// set the desired navigation setting
			navigationSettings.setNavigationType(Debug.SIMULATE_NAVIGATION ? SKNavigationType.SIMULATION : SKNavigationType.REAL);
			navigationSettings.setNavigationMode(SKNavigationMode.CAR);
			navigationSettings.setPositionerVerticalAlignment(-0.25f);

			navigationSettings.setShowRealGPSPositions(true);
			navigationSettings.setCcpAsCurrentPosition(true);
			// get the navigation manager object

			SKNavigationManager navigationManager = SKNavigationManager.getInstance();
			navigationManager.setMapView(mapView);


			// set listener for navigation events
			navigationManager.setNavigationListener(navigationListener);

			// start navigating using the settings
			navigationManager.increaseSimulationSpeed(5);
			navigationManager.startNavigation(navigationSettings);
			navigationActive = true;

			mapView.getMapSettings().setMapDisplayMode(SKMapDisplayMode.MODE_3D);
			mapView.getMapSettings().setStreetNamePopupsShown(true);
			mapView.getMapSettings().setMapZoomingEnabled(false);


			txtCurrentStreet.setVisibility(View.VISIBLE);
			txtNextStreet.setVisibility(View.VISIBLE);
			((Button) rootView.findViewById(R.id.btnCloseNav)).setVisibility(View.VISIBLE);

			dlog.d("startRouteNavigation: Imposto Audio a AUDIO_SYSTEM");
			if(getActivity()!=null)
				((AMainOBC) getActivity()).sendMessage(MessageFactory.AudioChannel(LowLevelInterface.AUDIO_SYSTEM,15));
			else
				dlog.w("startRouteNavigation: getActivity null!!!");

		}catch(Exception e){
			dlog.e("startRouteNavigation: Exception while starting navigation ",e);
		}



    }

    public void stopRouteNavigation() {


		homeB.setVisibility(View.VISIBLE);
		findDestinationB.setVisibility(View.VISIBLE);
			try {
				SKRouteManager.getInstance().clearCurrentRoute();
				SKNavigationManager.getInstance().stopNavigation();


/*	        mapView.getMapSettings().setStreetNamePopupsShown(false);

            mapView.rotateTheMapToNorth();
            SKMapSettings mapSettings = mapView.getMapSettings();
            mapSettings.setInertiaPanningEnabled(true);
            mapSettings.setMapZoomingEnabled(true);
            mapSettings.setMapRotationEnabled(true);
            //mapView.getMapSettings().setCompassPosition(new SKScreenPoint(5, 5));
            //mapView.getMapSettings().setCompassShown(true);
            mapView.getMapSettings().setFollowerMode(SKMapSettings.SKMapFollowerMode.NONE_WITH_HEADING);
            mapView.getMapSettings().setMapDisplayMode(SKMapSettings.SKMapDisplayMode.MODE_2D);
*/

				txtCurrentStreet.setVisibility(View.INVISIBLE);
				txtNextStreet.setVisibility(View.INVISIBLE);
				mapView.getMapSettings().setMapDisplayMode(SKMapDisplayMode.MODE_2D);
				hideLeftPanel();
				((Button) rootView.findViewById(R.id.btnCloseNav)).setVisibility(View.INVISIBLE);


				navigationActive = false;

				initMapSettings();
			}catch(Exception e){
				dlog.e("stopRouteNavigation: Unexpected Exception",e);
			}

    }

    private boolean realReachShown = false;


    private void ShowRealReachTimed(int km, long time) {

    	if (realReachShown || km ==0 || km > 150)
    		return;

    	realReachShown = true;

        localHandler.sendEmptyMessage(MSG_SHOW_REAL_REACH);
        localHandler.sendEmptyMessageDelayed(MSG_ZOOM_REAL_REACH, 1000);
        localHandler.sendEmptyMessageDelayed(MSG_HIDE_REAL_REACH, time);
    }


    private void ShowRealReach(int km) {


    	if (mapView==null) {
    		dlog.w("Invoked ShowRealReach with mapView==null");
    		return;
    	}

    	SKRealReachSettings realReachSettings = new SKRealReachSettings();
    	realReachSettings.setLocation(currentPosition.getCoordinate());
    	realReachSettings.setMeasurementUnit(SKRealReachSettings.SKRealReachMeasurementUnit.METER);
    	realReachSettings.setRange(km*1000);
    	realReachSettings.setTransportMode(SKRealReachSettings.SKRealReachVehicleType.CAR);
    	realReachSettings.setConnectionMode(SKRouteConnectionMode.OFFLINE);
    	mapView.displayRealReachWithSettings(realReachSettings);

    	/*
    	localHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(getActivity(), "Area raggiungibile con l'attuale livello di carica", Toast.LENGTH_LONG).show();

			}

    	}, 2000);
    	*/


    }

    private void ZoomRealReach() {

    	if (mapView==null)
    		return;

    	SKBoundingBox bb =  mapView.getRealReachBoundingBox();
    	mapView.fitRealReachInView(bb, true, 1000);


    }


    private void HideRealReach() {

    	if (mapView==null)
    		return;

    	mapView.clearRealReachDisplay();
    }

    private void centerMap() {

    	if (mapView==null)
    		return;

    	mapView.centerMapOnCurrentPosition();
    	mapView.getMapSettings().setFollowerMode(SKMapFollowerMode.NAVIGATION);
    }


    public static Bitmap decodeFileToBitmap(String pathToFile) {
        Bitmap decodedFile = null;
        try {
            decodedFile = BitmapFactory.decodeFile(pathToFile);
        } catch (OutOfMemoryError ofmerr) {
            return null;
        }
        return decodedFile;
    }

    private String formatTime(int s)  {
    	String str="";
    	int h=0, m=0;

    	if (s > 3600)  {
    		h = s / 3600;
    		s %= 3600;
    	}

    	if (s > 60) {
    		m = s / 60;
    		str += m+":";
    		s %= 60;
    	}

    	if (h>0)
    		str = String.format("%d:%02d:%02d", h,m,s);
    	else
    		str = String.format("%d:%02d", m,s);

    	return str;

    }

    private Spanned distanceToHtml(String s, boolean twoRows) {

    	String p[] = s.split(" ");
    	if (p.length<2) {
    		return Html.fromHtml(s);
    	} else {
    		String br = (twoRows?"<BR>":"&nbsp;");
    		return Html.fromHtml(p[0] + br +"<SMALL><SMALL>"+p[1]+"</SMALL></SMALL>");
    	}
    }

    boolean firstAdviceReceived;

    /**
     * Handles the navigation state update.
     *
     * @param skNavigationState
     * @param mapStyle
     */
    private void handleNavigationState(final SKNavigationState skNavigationState,
                                      final int mapStyle, final ViewGroup view) {


    		if (this.getActivity()==null)
    			return;

            this.getActivity().runOnUiThread(new Runnable() {

                @Override
                public void run() {

                	boolean showDestinationReachedFlag = skNavigationState.isLastAdvice();

                    // Advice image
                    ImageView currentAdviceImage = (ImageView)view.findViewById(R.id.imgAdvice);
                    String currentVisualAdviceImage = skNavigationState.getCurrentAdviceVisualAdviceFile();
                    Bitmap decodedAdvice = decodeFileToBitmap(currentVisualAdviceImage);
                    if (decodedAdvice != null) {
                        currentAdviceImage.setImageBitmap(decodedAdvice);
                        currentAdviceImage.setVisibility(View.VISIBLE);
                    }


                    // Advice distance
                    TextView currentAdviceDistance = (TextView)view.findViewById(R.id.txtAdviceDistance);
                	final int currentDistanceToAdvice = skNavigationState.getCurrentAdviceDistanceToAdvice();
                	String  strCurrentDistanceToAdvice = SKNavigationManager.getInstance().formatDistance(currentDistanceToAdvice);
                	currentAdviceDistance.setText(distanceToHtml(strCurrentDistanceToAdvice,true));

                	// Total distance
                	TextView txtTotalDistance =  (TextView)view.findViewById(R.id.txtTotalDistance);
                	double totalDistance = skNavigationState.getDistanceToDestination();
                	String strTotalDistance = SKNavigationManager.getInstance().formatDistance((int) Math.round(totalDistance));
                	txtTotalDistance.setText(distanceToHtml(strTotalDistance,false));

                	// Total time
                	TextView txtTotalTime =  (TextView)view.findViewById(R.id.txtTotalTime);
                	int totalTime = skNavigationState.getCurrentAdviceTimeToDestination();
                	txtTotalTime.setText(formatTime(totalTime));

                	//Current street
                	TextView txtCurrentStreet = (TextView)rootView.findViewById(R.id.txtCurrentStreet);
                	txtCurrentStreet.setText(skNavigationState.getCurrentAdviceCurrentStreetName());


                	//Next street
                	TextView txtNextStreet = (TextView)rootView.findViewById(R.id.txtNextStreet);
                	txtNextStreet.setText(skNavigationState.getCurrentAdviceNextStreetName());

                	//Arrival time
                	Date eta = new Date();
                	eta.setTime(eta.getTime() + totalTime*1000);
                	TextView txtEta = (TextView)rootView.findViewById(R.id.txtEta);
                	txtEta.setText(String.format("%1$TR", eta));

                    // Instructions
                	String instruction = skNavigationState.getAdviceInstruction();
                	String output = "";
                	if (instruction !=null && instruction.length()>1 )
                		output = instruction.substring(0, 1).toUpperCase() + instruction.substring(1);
                	((TextView)view.findViewById(R.id.txtInstructions)).setText(output);


                	double currentSpeedLimit = skNavigationState.getCurrentSpeedLimit();
                    if (currentSpeedLimit != skNavigationState.getCurrentSpeedLimit()) {
                        currentSpeedLimit = skNavigationState.getCurrentSpeedLimit();
                        //handleSpeedLimitAvailable(countryCode, distanceUnitType, mapStyle);
                    }


                    if (showDestinationReachedFlag) {
                        if (currentAdviceImage != null) {
                            currentAdviceImage.setImageResource(R.drawable.ic_destination_advise_black);
                        }
                        localHandler.sendEmptyMessageDelayed(MSG_STOP_NAVIGATION, 3000);

                    }

                    if (!firstAdviceReceived) {
                        firstAdviceReceived = true;
                    }

                }
            });


    }

    
    /*
	private void startNavigate(SKCoordinate destination) {
		
		SKToolsNavigationConfiguration configuration = new SKToolsNavigationConfiguration();
		configuration.setNavigationType(SKNavigationType.SIMULATION);
		configuration.setRouteType(SKRouteMode.CAR_FASTEST);
		configuration.setDistanceUnitType(SKMaps.SKDistanceUnitType.DISTANCE_UNIT_KILOMETER_METERS);
		configuration.setSpeedWarningThresholdInCity(10.0);
		configuration.setSpeedWarningThresholdOutsideCity(20.0);
		configuration.setAutomaticDayNight(false);
		configuration.setContinueFreeDriveAfterNavigationEnd(true);
		
        configuration.setStartCoordinate(currentPosition.getCoordinate());
        configuration.setDestinationCoordinate(destination);
        
        SKToolsNavigationManager navigationManager = new SKToolsNavigationManager(this.getActivity(), R.id.fmapMapMV);
        navigationManager.setNavigationListener(toolsNavigationListener);
        navigationManager.launchRouteCalculation(configuration, mapHolder);
	}
	*/


    private OnClickListener panelsClickListener  = new OnClickListener() {

		@Override
		public void onClick(View v) {

			switch (v.getId()) {

			case  R.id.btnCloseRealReach :
				CountDownTimer tmr = (CountDownTimer)v.getTag();
				if (tmr!=null) {
					tmr.cancel();
					((Button)v).setText("Chiudi");
				} else {
					hideLeftPanel();
				}
				break;

			case  R.id.btnClose :
				hideLeftPanel();
				break;

			case R.id.btnSearch:
				((ABase)getActivity()).pushFragment(FSearch.newInstance(), FSearch.class.getName(), true);
				break;

			case R.id.btnFavorites:
				rootView.findViewById(R.id.tvSearchMap).setVisibility(View.VISIBLE);
				break;

			}
		}

		};


    private View inflateNavigationLayout(ViewGroup parent,  OnClickListener listener) {
    	LayoutInflater inflater =    (LayoutInflater)this.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	View view = inflater.inflate(R.layout.la_navigation, null);


		return view;
    }

    private View inflateRealReachLayout(ViewGroup parent, OnClickListener listener) {
    	LayoutInflater inflater =    (LayoutInflater)this.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	View view = inflater.inflate(R.layout.la_realreach, null);

    	((Button)view.findViewById(R.id.btnCloseRealReach)).setOnClickListener(listener);

    	return view;
    }


    private View inflateNavMenuLayout(ViewGroup parent, OnClickListener listener) {
    	LayoutInflater inflater =    (LayoutInflater)this.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	View view = inflater.inflate(R.layout.la_navmenu, null);

		((Button)view.findViewById(R.id.btnClose)).setOnClickListener(listener);
		((Button)view.findViewById(R.id.btnSearch)).setOnClickListener(listener);
		((Button)view.findViewById(R.id.btnFavorites)).setOnClickListener(listener);

		return view;
    }

    private void buildLeftPanels() {
		try {
			ViewGroup parent = (ViewGroup) rootView.findViewById(R.id.fmapLeftFrame);
			try {
				parent.removeAllViews();
			} catch (Exception e) {
				dlog.e(" eccezione rimuovendo le view da panels", e);
			}

			panelRealReach = inflateRealReachLayout(parent, panelsClickListener);
			panelRealReach.setVisibility(View.INVISIBLE);
			parent.addView(panelRealReach);
			panels.add(panelRealReach);

			panelNavigation = inflateNavigationLayout(parent, panelsClickListener);
			panelNavigation.setVisibility(View.INVISIBLE);
			parent.addView(panelNavigation);
			panels.add(panelNavigation);

			panelNavMenu = inflateNavMenuLayout(parent, panelsClickListener);
			panelNavMenu.setVisibility(View.INVISIBLE);
			parent.addView(panelNavMenu);
			panels.add(panelNavMenu);
		}catch(Exception e){
			dlog.e("Exception while building left panel",e);
		}

    }



    private void showLeftPanel(int selfClose, int which) {

		try {
			final ViewGroup parent = (ViewGroup) rootView.findViewById(R.id.fmapLeftFrame);
			final View v = rootView.findViewById(R.id.fmapLeftBorderIV);
			//v.animate().translationXBy(-255);
			//v.setVisibility(View.INVISIBLE);

			for (View p : panels) {
				if (p.getVisibility() == View.VISIBLE)
					p.setVisibility(View.INVISIBLE);
			}

			View panel = null;


			switch (which) {
				case 0:
					panel = panelRealReach;
					panel = null;
					if (selfClose > 0) {
						final CountDownTimer tmr = new CountDownTimer(selfClose * 1000, 1000) {
							@Override
							public void onTick(long millisUntilFinished) {
								((Button) parent.findViewById(R.id.btnCloseRealReach)).setText((millisUntilFinished / 1000) + " sec");
							}

							@Override
							public void onFinish() {
								hideLeftPanel();
								((Button) parent.findViewById(R.id.btnCloseRealReach)).setTag(null);
								localHandler.sendEmptyMessage(MSG_HIDE_REAL_REACH);
							}

						}.start();
						((Button) parent.findViewById(R.id.btnCloseRealReach)).setTag(tmr);
					}


					break;
				case 1:
					panel = panelNavigation;
					break;

				case 2:
					panel = panelNavMenu;
					break;

			}

			if (panel == null)
				return;

			panel.setAlpha(0.0f);
		/*if (App.currentTripInfo.isMaintenance) {
			panel.setBackgroundColor(getResources().getColor(R.color.background_red));

		} else {
			//panel.setBackgroundColor(getResources().getColor(R.color.background_green));
		}*/

			panel.setVisibility(View.VISIBLE);
			panel.animate().alpha(1.0f);
			//panel.animate().translationX(0);
			//panel.invalidate();
			dlog.d(FMap.class.toString() + " showLeftPanel: panel " + String.valueOf(which) + " visibile");


		}catch(Exception e){

			homeB.setVisibility(View.VISIBLE);
			findDestinationB.setVisibility(View.VISIBLE);
			dlog.e("Exception while building left panel, restoring home button",e);
		}

    }

    private void hideLeftPanel() {
		try {
			for (View p : panels) {
				if (p.getVisibility() == View.VISIBLE)
					p.setVisibility(View.INVISIBLE);
			}

			//rootView.findViewById(R.id.fmapLeftBorderIV).setVisibility(View.VISIBLE);
			//rootView.findViewById(R.id.fmapLeftBorderIV).animate().translationX(0);
		}catch(Exception e) {
			dlog.e("Exception while hiding left panels",e);
		}
	}


	private void centerMap(Location position) {

	}


	public void onActivityLocationChanged(Location location) {

		if (mapView != null && location!=null) {

        	SKPosition position = new SKPosition();
        	SKCoordinate point = new SKCoordinate();
        	point.setLatitude(location.getLatitude());
        	point.setLongitude(location.getLongitude());
        	position.setCoordinate(point);
        	position.setAltitude(location.getAltitude());
        	position.setHeading(location.getBearing());
        	position.setSpeed(location.getSpeed());
        	SKPositionerManager.getInstance().reportNewGPSPosition(position);
            currentPosition = position;
        }

	}






	private  Handler localActivityHandler = new Handler()  {

		@Override
		public void handleMessage(Message msg) {

			switch (msg.what)  {

			case AMainOBC.MSG_UPDATE_DATE:
	    		if (msg.obj !=null && dayTV!=null) {
	    			dayTV.setText((String)msg.obj);
	    		}
	    		break;

			case AMainOBC.MSG_UPDATE_TIME:
	    		if (msg.obj !=null && timeTV!=null) {
	    			timeTV.setText((String)msg.obj);
	    		}
	    		break;



			}
		}
	};



	private  Handler localHandler = new Handler()  {

		@Override
		public void handleMessage(Message msg) {
			try {

				switch (msg.what) {

					case MSG_SHOW_REAL_REACH:
						//ShowRealReach(range);
						break;

					case MSG_ZOOM_REAL_REACH:
						//ZoomRealReach();
						break;

					case MSG_HIDE_REAL_REACH:
						HideRealReach();
						break;

					case MSG_STOP_NAVIGATION:
						stopRouteNavigation();

					case MSG_CENTER_MAP:
						centerMap();
						break;
					case MSG_CLOSE_CALLOUT:
						hideCustomNavigationCallout();
						break;
					case MSG_CLOSE_SOC_ALERT:

						localHandler.removeMessages(MSG_CLOSE_SOC_ALERT);
						rootView.findViewById(R.id.fmapAlertSOCFL).setVisibility(View.GONE);
						//rootView.findViewById(R.id.fmapAlertSOCFL).invalidate(); testinva
						break;
					case MSG_OPEN_SOC_ALERT:
						localHandler.removeMessages(MSG_OPEN_SOC_ALERT);
						if(localCarInfo.batteryLevel<=15) {
							rootView.findViewById(R.id.fmapAlertSOCFL).setVisibility(View.VISIBLE);
							rootView.findViewById(R.id.fmapAlarmIV).setBackgroundResource(R.drawable.outofcharge);
							((rootView.findViewById(R.id.fmapAlertSOCFL))).setBackgroundResource(R.drawable.sha_redalertbox);
							((TextView) (rootView.findViewById(R.id.fmapAlertTitleTV))).setText(R.string.alert_warning_title);
							((TextView) (rootView.findViewById(R.id.fmapAlertDescTV))).setText(R.string.alert_5km);
							localHandler.sendEmptyMessageDelayed(MSG_OPEN_SOC_ALERT, 120000);
							rootView.findViewById(R.id.fmapAlertSOCFL).setVisibility(View.VISIBLE);
							//rootView.findViewById(R.id.fmapAlertSOCFL).invalidate(); testinva
						}
						break;
				}
			}catch(Exception e){
				dlog.e("Exception while handling message",e);
			}
		}
	};


	private SKMapSurfaceListener mapSurfaceListener = new SKMapSurfaceListener() {

		@Override
		public void onActionPan() {
			if(mapView!=null) {
				localHandler.removeMessages(MSG_CENTER_MAP);
				localHandler.sendEmptyMessageDelayed(MSG_CENTER_MAP, 13000);
				mapView.getMapSettings().setFollowerMode(SKMapFollowerMode.NONE);
			}
			else{
				DLog.W("Click su mapView ancora non inizializzato");
			}
		}

		@Override
		public void onActionZoom() {

		}



		@Override
		public void onAnnotationSelected(SKAnnotation annotation) {

			isSecondCallout=false;

			try{
				eventCallout(annotation.getLocation(), Pois.get(annotation.getUniqueID() - 10), 20); //id annotazione-10 = posizione nel bundle
				localHandler.removeMessages(MSG_CENTER_MAP);
				localHandler.sendEmptyMessageDelayed(MSG_CENTER_MAP, 20000); //evito il reset della vista prima del tempo
				localHandler.sendEmptyMessageDelayed(MSG_CLOSE_CALLOUT,20000);
			}catch(Exception e){
				dlog.e("onAnnotationSelected: Exception on callout creation",e);
			}
		}

		@Override
		public void onBoundingBoxImageRendered(int arg0) {

		}

		private boolean lockNorth = false;
		@Override
		public void onCompassSelected() {

			if (lockNorth) {
				mapView.getMapSettings().setFollowerMode(SKMapFollowerMode.NAVIGATION);
				lockNorth = false;
			} else {
				mapView.rotateTheMapToNorthSmooth(2);
				mapView.getMapSettings().setFollowerMode(SKMapFollowerMode.POSITION);
				lockNorth = true;
			}

		}

		@Override
		public void onCurrentPositionSelected() {


		}

		@Override
		public void onCustomPOISelected(SKMapCustomPOI arg0) {


		}

		@Override
		public void onSingleTap(SKScreenPoint point) {
			if(mapView!=null) {
				if (navigationCallout != null)
					navigationCallout.hide();
				rootView.findViewById(R.id.annotationLL).setVisibility(View.GONE);
				mapView.deleteAnnotation(9);
				mapView.deleteAnnotation(0);
				isSecondCallout=false;
				double pLat, pLon;
				if (mapView.getMapSettings().getMapDisplayMode() == SKMapDisplayMode.MODE_3D) {
					pLat = (double) Math.round(mapView.pointToCoordinate(point).getLatitude() * 1000d) / 1000d;
					pLon = (double) Math.round(mapView.pointToCoordinate(point).getLongitude() * 1000d) / 1000d;
					for (final SKAnnotation annotation : annotationList) {
						double aLat = (double) Math.round(annotation.getLocation().getLatitude() * 1000d) / 1000d;
						double aLon = (double) Math.round(annotation.getLocation().getLongitude() * 1000d) / 1000d;
						if ((pLat == aLat) && (pLon == aLon)) {
							try {
								rootView.findViewById(R.id.annotationLL).setVisibility(View.VISIBLE);
								descriptionAnnotation.setText(Pois.get(annotation.getUniqueID() - 10).getString("address", " "));
								titleAnnotation.setText(Pois.get(annotation.getUniqueID() - 10).getString("nome", "Raggiungi destinazione"));
								titleAnnotation.setOnClickListener(new OnClickListener() {
									@Override
									public void onClick(View v) {

										rootView.findViewById(R.id.annotationLL).setVisibility(View.GONE);
										startRoute(annotation.getLocation(), true);
									}
								});
								rootView.findViewById(R.id.goBtn).setOnClickListener(new OnClickListener() {
									@Override
									public void onClick(View v) {

										rootView.findViewById(R.id.annotationLL).setVisibility(View.GONE);
										startRoute(annotation.getLocation(), true);
									}
								});
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				}

			}
			else{
				DLog.W("mapView null");
			}
		}

		@Override
		public void onDoubleTap(SKScreenPoint arg0) {

		}

		@Override
		public void onGLInitializationError(String arg0) {
		}

		@Override
		public void onInternationalisationCalled(int arg0) {
		}

		@Override
		public void onInternetConnectionNeeded() {
		}

		@Override
		public void onLongPress(SKScreenPoint point) {
			if(mapView!=null) {
				final SKScreenPoint pointF = point;
				/*if (mapView.getMapSettings().getMapDisplayMode() != SKMapDisplayMode.MODE_3D) {
					confirmNavigation(point);
				} else {*/
					try {
						SKAnnotation annotation = new SKAnnotation(9);
						annotation.setLocation(mapView.pointToCoordinate(pointF));
						annotation.setMininumZoomLevel(5);
						annotation.setAnnotationType(SKAnnotation.SK_ANNOTATION_TYPE_DESTINATION_FLAG);
						mapView.addAnnotation(annotation, SKAnimationSettings.ANIMATION_NONE);
						rootView.findViewById(R.id.annotationLL).setVisibility(View.VISIBLE);
						((TextView)rootView.findViewById(R.id.titleAnnotationTV)).setText("Conferma destinazione");
						//titleAnnotation.invalidate(); testinva
						SKSearchResult address = SKReverseGeocoderManager.getInstance().reverseGeocodePosition(mapView.pointToCoordinate(point));
						((TextView)rootView.findViewById(R.id.descriptionAnnotationTV)).setText(address != null ? address.getName() : "");

						titleAnnotation.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {

								mapView.deleteAnnotation(9);
								rootView.findViewById(R.id.annotationLL).setVisibility(View.GONE);
								startRoute(mapView.pointToCoordinate(pointF), true);
							}
						});
						rootView.findViewById(R.id.goBtn).setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {

								mapView.deleteAnnotation(9);
								rootView.findViewById(R.id.annotationLL).setVisibility(View.GONE);
								startRoute(mapView.pointToCoordinate(pointF), true);
							}
						});
					} catch (Exception e) {
						e.printStackTrace();
					}

				//}
			}
			else{
				DLog.W("Mapview null");
			}
		}

		@Override
		public void onMapActionDown(SKScreenPoint arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onMapActionUp(SKScreenPoint arg0) {
		}

		@Override
		public void onMapPOISelected(SKMapPOI arg0) {
		}

		@Override
		public void onMapRegionChangeEnded(SKCoordinateRegion arg0) {
			
		}

		@Override
		public void onMapRegionChangeStarted(SKCoordinateRegion arg0) {
			
		}

		@Override
		public void onMapRegionChanged(SKCoordinateRegion arg0) {
			
		}

		@Override
		public void onObjectSelected(int arg0) {
			
		}

		@Override
		public void onPOIClusterSelected(SKPOICluster arg0) {
			
		}

		@Override
		public void onRotateMap() {
			
		}




		@Override
		public void onSurfaceCreated(SKMapViewHolder holder) {
			Boolean test=false;




			mapView = holder.getMapSurfaceView();
			mapView.deleteAnnotation(9);
			mapView.deleteAnnotation(0);
	        if (firstLaunch) {

	        	initMapSettings();

		        firstLaunch=false;
				try{
					drawPOIS();
					//drawChargingStation();
				}catch(OutOfMemoryError e){
					dlog.e("drawPois: out of memory ",e);
				}catch(Exception e){
					dlog.e("drawPois: Generic Exception ",e);
				}
					return;


	        }
			try {
				if (navigationActive) {
					startRouteNavigation();
				} else
					resetMapSettings();
			}catch(Exception e){
				dlog.e("exception startRouteNavigation() ",e);
			}


		}

		@Override
		public void onScreenshotReady(Bitmap arg0) {
			
		}

	};

	private SKNavigationListener navigationListener = new SKNavigationListener() {

		@Override
		public void onDestinationReached() {

			SKRouteManager.getInstance().clearCurrentRoute();
			SKNavigationManager.getInstance().stopNavigation();
			navigationActive = false;

		}


		@Override
		public void onReRoutingStarted() {
			// TODO Auto-generated method stub

		}

		@Override
		public void onSignalNewAdviceWithAudioFiles(String[] arg0, boolean arg1) {
			if (arg0!=null && arg0.length>0) {
				for(int i=0; i<arg0.length; i++) {
					dlog.d("ADVICE: "+ arg0[i]);
				}
			}

		}

		@Override
		public void onSignalNewAdviceWithInstruction(String arg0) {
			dlog.d("ADVICE STRING:" +  arg0);
				//tts.postpone();
				tts.speak(arg0,TextToSpeech.QUEUE_FLUSH);

		}

		@Override
		public void onSpeedExceededWithAudioFiles(String[] arg0, boolean arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onSpeedExceededWithInstruction(String arg0, boolean arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onTunnelEvent(boolean arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onUpdateNavigationState(SKNavigationState state) {
			//DLog.D(state.toString());
			try {
				if(state!=null)
					handleNavigationState(state, 0, (ViewGroup) rootView.findViewById(R.id.fmapLeftFrame));
			}catch(Exception e){
				dlog.e("Exception handling navigation state",e);
			}

		}

		@Override
		public void onViaPointReached(int arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onVisualAdviceChanged(boolean arg0, boolean arg1,
				SKNavigationState arg2) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onFreeDriveUpdated(String arg0, String arg1, String arg2,
									   SKStreetType arg3, double arg4, double arg5) {
			// TODO Auto-generated method stub

		}

	};



	private SKToolsNavigationListener toolsNavigationListener = new SKToolsNavigationListener() {

		@Override
		public void onNavigationStarted() {
			DLog.D("Start");
			navigationActive = true;
			((AMainOBC)getActivity()).sendMessage(MessageFactory.AudioChannel(LowLevelInterface.AUDIO_SYSTEM,15));
		}

		@Override
		public void onNavigationEnded() {
			navigationActive = false;
			((AMainOBC)getActivity()).sendMessage(MessageFactory.AudioChannel(LowLevelInterface.AUDIO_NONE,-1));

		}

		@Override
		public void onRouteCalculationStarted() {
			// TODO Auto-generated method stub

		}

		@Override
		public void onRouteCalculationCompleted() {
			// TODO Auto-generated method stub

		}

		@Override
		public void onRouteCalculationCanceled() {
			// TODO Auto-generated method stub

		}

	};

	public SKRouteListener routeListener = new SKRouteListener() {

		@Override
		public void onAllRoutesCompleted() {
			// TODO Auto-generated method stub
		}

		@Override
		public void onOnlineRouteComputationHanging(int arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onRouteCalculationCompleted(SKRouteInfo info) {
		  DLog.D(info.toString());
		  startRouteNavigation();
		}

		@Override
		public void onRouteCalculationFailed(SKRoutingErrorCode arg0) {
			CharSequence text = arg0.toString();
			Toast toast = Toast.makeText(getActivity(), text+ " please move into a bigger street and retry", Toast.LENGTH_LONG);
			toast.show();

		}

		@Override
		public void onServerLikeRouteCalculationCompleted(SKRouteJsonAnswer arg0) {
			// TODO Auto-generated method stub

		}
	};

	private String maxIconID(String folder){



		try {
			File f = new File(folder);
			File file[] = f.listFiles();
			Log.d("Files", "Size: " + file.length);
			if (file.length == 0)
				return "0";
			String name = file[file.length - 1].getName();
			name = name.substring(0, name.lastIndexOf('.'));
			return name;
		}catch (Exception e){
			e.printStackTrace();
		}

		/*int i=0;
		File maxID;

		do{
			maxID=new File(folder.concat(String.valueOf(i++)).concat(".png"));
			if(!maxID.exists()){
				maxID=new File(folder.concat(String.valueOf(i++)).concat(".jpg"));
				if(!maxID.exists()){
					maxID=new File(folder.concat(String.valueOf(i++)).concat(".gif"));
					if(!maxID.exists()){
						break;
					}
				}
			}
		}while(maxID.exists());
		return String.valueOf(i-1);*/


		return"0";
	}






	private void updatePoiIcon(String Url) {
		try {

			String jsonStr = "";
			if (Icons != null)
				Icons.clear();

			File outDir = new File(App.POI_ICON_FOLDER);
			if (!outDir.isDirectory()) {
				outDir.mkdir();
			}

			Url = Url.concat("?ID=").concat(maxIconID(App.POI_ICON_FOLDER));
			dlog.d("updatePoiIcon: load url"+Url);
			StringBuilder builder = new StringBuilder();
			HttpClient client = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(Url);
			try {
				HttpResponse response = client.execute(httpGet);
				StatusLine statusLine = response.getStatusLine();
				int statusCode = statusLine.getStatusCode();
				if (statusCode == 200) {
					App.update_Poi = new Date();
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

					dlog.e(FMap.class.toString()+ "Failed to download file");
				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (NetworkOnMainThreadException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}finally {
				client.getConnectionManager().shutdown();
			}
			jsonStr = builder.toString();
			builder.delete(0,builder.length());
			dlog.d("updatePoiIcon got Response: "+jsonStr);
			parseJsonIcon(jsonStr);
			String filename="";

			for (Bundle Icon : Icons) {    //-- start loop download and save image
				try {
					URL url = new URL(Icon.getString("URL"));
					String extension = url.getFile().substring(url.getFile().lastIndexOf('.') + 1);
					filename = Icon.getString("ID").concat(".").concat(extension);
					dlog.d("Local filename:" + filename);
					File file = new File(outDir, filename);

					if (file.exists())
						continue;

					HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
					urlConnection.setRequestMethod("GET");
					urlConnection.setDoOutput(true);
					urlConnection.connect();
					if (file.createNewFile()) {
						file.createNewFile();
					}
					FileOutputStream fileOutput = new FileOutputStream(file);
					InputStream inputStream = urlConnection.getInputStream();
					byte[] buffer = new byte[1024];
					int bufferLength = 0;
					while ((bufferLength = inputStream.read(buffer)) > 0) {
						fileOutput.write(buffer, 0, bufferLength);
					}
					fileOutput.close();
					inputStream.close();
					urlConnection.disconnect();
				} catch (Exception e) {
					dlog.e("Exception while downloading image");
					File file = new File(outDir, filename);
					if(file.exists())
						file.delete();
				}
			}
			//return filepath;
		}catch (Exception e){
			e.printStackTrace();
		}


		return ;

	}



	private void parseJsonIcon(String jsonStr) {



		try {
			JSONObject json = new JSONObject(jsonStr);
			Log.i(FMap.class.getName(), "creazione oggetto Json");
			//Get the instance of JSONArray that contains JSONObjects
			JSONArray jsonArray = json.optJSONArray("Icons");

			//Iterate the jsonArray and print the info of JSONObjects
			for(int k=0;k<jsonArray.length();k++) {
				Bundle Icon = new Bundle();
				JSONObject jsonObject = jsonArray.getJSONObject(k);
				if (jsonObject.has("ID"))
				Icon.putString("ID",jsonObject.getString("ID"));
				if (jsonObject.has("URL"))
				Icon.putString("URL",jsonObject.getString("URL"));
				Icons.add(Icon);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void updatePoiPos(String Url) {
		try {

			String jsonStr = "";

			File outDir = new File(App.POI_POSITION_FOLDER);
			if (!outDir.isDirectory()) {
				outDir.mkdir();
			}
			File outputFile = new File(outDir, "POIS_POS.json");

			StringBuilder builder = new StringBuilder();
			Writer writer;
			HttpClient client = new DefaultHttpClient();
			List<NameValuePair> paramsList = new ArrayList<NameValuePair>();
			if (!jsonmd5.equals("")) {
				paramsList.add(new BasicNameValuePair("md5", jsonmd5 ));//Url = Url.concat("?md5=").concat(jsonmd5);
			}
			paramsList.add(new BasicNameValuePair("versione", "2" )); //corrisponde al type massimo di poi che gestisce
			paramsList.add(new BasicNameValuePair("carplate", App.CarPlate ));
			Url= UrlTools.buildQuery(Url.concat("?"),paramsList).toString();
			dlog.d(FMap.class.toString()+" updatePoiPos: Richiesta url "+Url);
			HttpGet httpGet = new HttpGet(Url);
			try {
				HttpResponse response = client.execute(httpGet);
				StatusLine statusLine = response.getStatusLine();
				int statusCode = statusLine.getStatusCode();
				if (statusCode == 200) {
					App.update_Poi = new Date();
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

					dlog.e(FMap.class.toString()+ "updatePoiPos: Failed to download file: "+statusLine.toString());
				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (NetworkOnMainThreadException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}finally {
				client.getConnectionManager().shutdown();
			}
			jsonStr = builder.toString();
			dlog.d(FMap.class.toString()+" updatePoiPos: Risposta a url "+jsonStr);
			if (jsonStr.equals(""))
				return;
			parseJsonPos(jsonStr);
			try {

				if (!outDir.isDirectory()) {
					throw new IOException(
							"Unable to create directory . Maybe the SD card is mounted?");
				}
				writer = new BufferedWriter(new FileWriter(outputFile));
				writer.write(jsonStr);

				writer.close();
				jsonmd5 = md5(jsonStr);
				if(mapView!=null) {
					mapView.deleteAllAnnotationsAndCustomPOIs();
					drawPOIS();
					drawChargingStation();
				}

				dlog.d(FMap.class.toString()+" updatePoiPos: Poi aggiornati");
			} catch (IOException e) {
				dlog.e(FMap.class.toString()+" updatePoiPos: eccezione nel salvataggio del json o crazione Poi ", e);

			}
		}catch (Exception e){
			dlog.e(FMap.class.toString()+" updatePoiPos: eccezione nel reperimento e salvataggio del json ", e);
			e.printStackTrace();
		}



		return ;

	}



	private void parseJsonPos(String jsonStr) {


		dlog.d(FMap.class.getName()+ " parseJsonPos: parse del json: "+jsonStr);
		if(jsonStr.equals(""))
			return;

		try {


			JSONObject json = new JSONObject(jsonStr);

			//Get the instance of JSONArray that contains JSONObjects
			JSONArray jsonArray = json.optJSONArray("Position");

			//Iterate the jsonArray and print the info of JSONObjects
			ArrayList<Bundle> PoisTemp = new ArrayList<Bundle>();

			int k;
			for(k=0;k<jsonArray.length();k++) {
				Bundle Poi = new Bundle();
				JSONObject jsonObject = jsonArray.getJSONObject(k);
				try {
					if (jsonObject.has("ID"))
						Poi.putInt("ID", Integer.parseInt(jsonObject.getString("ID")));
					if (jsonObject.has("LAT"))
						Poi.putDouble("LAT", Double.parseDouble(jsonObject.getString("LAT")));
					if (jsonObject.has("LON"))
						Poi.putDouble("LON", Double.parseDouble(jsonObject.getString("LON")));
					if (jsonObject.has("id_icon"))
						Poi.putString("id_icon", jsonObject.getString("id_icon"));
					if (jsonObject.has("address"))
						Poi.putString("address", jsonObject.getString("address"));
					if (jsonObject.has("nome"))
						Poi.putString("nome", jsonObject.getString("nome"));
					Poi.putInt("INDEX", k+10);
					if (jsonObject.has("type"))
						Poi.putInt("type", jsonObject.getInt("type"));
					if (jsonObject.has("description"))
						Poi.putString("description", jsonObject.getString("description"));
					if (jsonObject.has("code"))
						Poi.putString("code", jsonObject.getString("code"));
				}

				catch(Exception e){

					dlog.e(FMap.class.getName()+ " parseJsonPos: eccezione durante  parse JsonObject",e);
				}
				PoisTemp.add(Poi);

			}

			List<Poi> PoiList = retriveChargingList();

			for(Poi singlePoi :PoiList){
				Bundle Poi = new Bundle();
				try {
					Poi.putInt("ID", singlePoi.id);
					Poi.putDouble("LAT", singlePoi.lat);
					Poi.putDouble("LON", singlePoi.lon);
					Poi.putString("id_icon", "pois_icon");
					Poi.putString("address", singlePoi.town);
					Poi.putString("nome", singlePoi.name);
					Poi.putInt("INDEX", k+10);
					Poi.putInt("type", 3);
					Poi.putString("description", singlePoi.type);
					Poi.putString("code", singlePoi.code);
				}catch(Exception e){

					dlog.e(FMap.class.getName()+ " parseJsonPos: eccezione durante  parse JsonObject",e);
				}
				PoisTemp.add(Poi);
				k++;
			}


			if(!PoisTemp.equals(Pois))
				Pois=PoisTemp;
		} catch (Exception e) {
			e.printStackTrace();
		}


	}
	public String md5(String encTarget){
		MessageDigest mdEnc = null;
		try {
			mdEnc = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			dlog.e(FMap.class.toString()+" md5: Exception while encrypting to md5",e);
			e.printStackTrace();
		} // Encryption algorithm
		mdEnc.update(encTarget.getBytes(), 0, encTarget.length());
		String md5 = new BigInteger(1, mdEnc.digest()).toString(16);
		while ( md5.length() < 32 ) {
			md5 = "0"+md5;
		}
		mdEnc.reset();
		return md5;
	}

	public static String convertStreamToString(InputStream is) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			sb.append(line);
		}
		reader.close();
		return sb.toString();
	}

	public static String getStringFromFile (String filePath) throws Exception {
		File fl = new File(filePath);
		FileInputStream fin = new FileInputStream(fl);
		String ret = convertStreamToString(fin);
		//Make sure you close all streams.
		fin.close();
		return ret;
	}

	public  void hideCustomNavigationCallout() {
		if(navigationCallout!=null)
			navigationCallout.hide();
	}

	private final BroadcastReceiver ConnectivityChangeReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context c, Intent i) {
			boolean status = SystemControl.hasNetworkConnection(c);

			if (status) {
				if (animQueue.contains("3g")) {
					animQueue.remove("3g");
				}

			} else {
				if (!animQueue.contains("3g")) {
					animQueue.add("3g");
				}
				if(no3gwarning.getAnimation()==null)
					no3gwarning.startAnimation(alertAnimation);
			}
		}


	};

	public void loadBanner(String Url, String type, Boolean isClick) {

		File outDir = new File(App.BANNER_IMAGES_FOLDER);
		if (!outDir.isDirectory()) {
			outDir.mkdir();
		}

		if(App.parkMode.isOn()) {

			dlog.d("loadBanner: Park mode ON");
			return;
		}

		if (!App.hasNetworkConnection) {
			dlog.e(FMap.class.toString()+" loadBanner: nessuna connessione");
			App.Instance.BannerName.putBundle(type,null);//null per identificare nessuna connessione, caricare immagine offline
			return;
		}
		StringBuilder  builder = new StringBuilder();
		List<NameValuePair> paramsList = new ArrayList<NameValuePair>();
		HttpResponse response=null;
		if(!isClick) {


			if (App.currentTripInfo != null && App.currentTripInfo.customer != null)
				paramsList.add(new BasicNameValuePair("id", App.currentTripInfo.customer.id + ""));//App.currentTripInfo.customer.id + "")); //"3"));

			if (App.lastLocation != null) {
				paramsList.add(new BasicNameValuePair("lat", App.lastLocation.getLatitude() + ""));
				paramsList.add(new BasicNameValuePair("lon", App.lastLocation.getLongitude() + ""));
			}
			paramsList.add(new BasicNameValuePair("id_fleet", App.FleetId + ""));
			paramsList.add(new BasicNameValuePair("carplate", App.CarPlate));//"ED93107"));//App.CarPlate));
		}
		try {
			if (App.BannerName.getBundle(type) != null )
				paramsList.add(new BasicNameValuePair("index", App.Instance.BannerName.getBundle(type).getString("INDEX",null)));

			if (App.BannerName.getBundle(type) != null )
				paramsList.add(new BasicNameValuePair("end", App.Instance.BannerName.getBundle(type).getString("END",null)));



			Url= UrlTools.buildQuery(Url.concat("?"),paramsList).toString();
			//connessione per scaricare id immagine

			DLog.D(FMap.class.toString()+" loadBanner: Url richiesta "+Url);
			HttpClient client = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(Url);

			 response = client.execute(httpGet);
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
				content.close();
				reader.close();
			} else {

				dlog.e(FMap.class.toString()+" loadBanner: Failed to connect "+String.valueOf(statusCode));
				App.Instance.BannerName.putBundle(type,null);//null per identificare nessuna connessione, caricare immagine offline
				return;
			}
			client.getConnectionManager().shutdown();
		}catch (Exception e){
			dlog.e(FMap.class.toString()+" loadBanner: eccezione in connessione ",e);
			App.Instance.BannerName.putBundle(type,null);//null per identificare nessuna connessione, caricare immagine offline
			return;
	}
		String jsonStr = builder.toString();
		builder.delete(0,builder.length());
		if(jsonStr.compareTo("")==0){
			dlog.e(FMap.class.toString()+" loadBanner: nessuna connessione");
			App.Instance.BannerName.putBundle(type,null);//null per identificare nessuna connessione, caricare immagine offline
			return;
		}

		DLog.D(FMap.class.toString()+" loadBanner: risposta "+jsonStr);
		File file = new File(outDir, "placeholder.lol");;

		try {
			JSONObject json = new JSONObject(jsonStr);

			//Get the instance of JSONArray that contains JSONObjects
			JSONArray jsonArray = json.optJSONArray("Image");

			//Iterate the jsonArray and print the info of JSONObjects

			Bundle Image = new Bundle();
			JSONObject jsonObject = jsonArray.getJSONObject(0);

				Image.putString("ID", jsonObject.getString("ID"));
				Image.putString("URL", jsonObject.getString("URL"));
				Image.putString(("CLICK"), jsonObject.getString("CLICK"));
				Image.putString(("INDEX"), jsonObject.getString("INDEX"));
				Image.putString(("END"), jsonObject.getString("END"));
				if(jsonObject.has("FLEET"))
					updateFleet(jsonObject.getInt("FLEET"));

			App.Instance.BannerName.putBundle(type,Image);

			//ricavo nome file
			URL urlImg = new URL(Image.getString("URL"));
			String extension = urlImg.getFile().substring(urlImg.getFile().lastIndexOf('.') + 1);
			String filename = Image.getString("ID").concat(".").concat(extension);

			//download imagine se non esiste
			file = new File(outDir, filename);

			if(file.exists()){
				Image.putString(("FILENAME"),filename);
				App.Instance.BannerName.putBundle(type,Image);
				dlog.i(FMap.class.toString()+" loadBanner: file già esistente: "+filename);
				return;
			}


			dlog.i(FMap.class.toString()+" loadBanner: file mancante inizio download a url: "+urlImg.toString());
			HttpURLConnection urlConnection = (HttpURLConnection) urlImg.openConnection();
			urlConnection.setRequestMethod("GET");
			urlConnection.setDoOutput(true);
			urlConnection.connect();
			if (file.createNewFile()) {
				file.createNewFile();
			}
			FileOutputStream fileOutput = new FileOutputStream(file);
			InputStream inputStream = urlConnection.getInputStream();
			int totalSize = urlConnection.getContentLength();
			int downloadedSize = 0;
			byte[] buffer = new byte[1024];
			int bufferLength = 0;
			while ((bufferLength = inputStream.read(buffer)) > 0) {
				fileOutput.write(buffer, 0, bufferLength);
				//downloadedSize += bufferLength;
				//Log.i("Progress:", "downloadedSize:" + downloadedSize + "totalSize:" + totalSize);
			}
			fileOutput.close();
			urlConnection.disconnect();
			inputStream.close();
			Image.putString(("FILENAME"),filename);
			App.Instance.BannerName.putBundle(type,Image);
			dlog.d(FMap.class.toString()+" loadBanner: File scaricato e creato "+filename);


		} catch (Exception e) {
			if(file.exists()) file.delete();
			App.Instance.BannerName.putBundle(type,null);//null per identificare nessuna connessione, caricare immagine offline
			dlog.e(FMap.class.toString()+" loadBanner: eccezione in creazione e download file ",e);

			e.printStackTrace();
		}



	}

	private void updateBanner(String type){

		File ImageV;
		Bundle Banner = App.Instance.BannerName.getBundle(type);
		if(Banner!=null){
			ImageV=new File(App.BANNER_IMAGES_FOLDER,Banner.getString("FILENAME","nope"));

			try{
				if(ImageV!=null && ImageV.exists()){
					dlog.d(FMap.class.toString()+" updateBanner: file trovato imposto immagine "+ImageV.getName());
					Bitmap myBitmap = BitmapFactory.decodeFile(ImageV.getAbsolutePath());
					if(myBitmap==null){

						dlog.e(FMap.class.toString()+" updateBanner: file corrotto, elimino e visualizzo offline ");
						ImageV.delete();
						//initWebBanner(Banner.getString("URL",null));
						//webViewBanner.setVisibility(View.INVISIBLE);
						adIV.setImageResource(R.drawable.car_banner_offline);
						adIV.setVisibility(View.VISIBLE);
						if(!started){
							started= !started;
							timer_2min.cancel();
							timer_2min.start();
						}
						return;
					}
					//webViewBanner.setVisibility(View.INVISIBLE);
					adIV.setImageBitmap(myBitmap);
					adIV.setVisibility(View.VISIBLE);
					//adIV.invalidate(); testinva


				}
			}catch (Exception e){
				dlog.e(FMap.class.toString()+" updateBanner: eccezione in caricamento file visualizzo offline ",e);
				e.printStackTrace();
				//initWebBanner(Banner.getString("URL",null));
				//webViewBanner.setVisibility(View.INVISIBLE);
				adIV.setImageResource(R.drawable.car_banner_offline);
				adIV.setVisibility(View.VISIBLE);

			}
		}
		else{
			dlog.e(FMap.class.toString()+" updateBanner: Bundle null, visualizzo offline");
			//initWebBanner(Banner.getString("URL",null));
			//webViewBanner.setVisibility(View.INVISIBLE);
			adIV.setImageResource(R.drawable.car_banner_offline);
			adIV.setVisibility(View.VISIBLE);

		}

		if(!started){
			started= !started;
			timer_2min.cancel();
			timer_2min.start();
		}
	}

	private void updateFleet(int fleet ){

		dlog.d("updateFleet: ricevuto fleet id " + fleet);
		if(App.FleetId!=fleet && fleet !=0){
			App.FleetId=fleet;
			App.Instance.persistFleetId();
			switch(fleet) {
				case 1:
					App.Instance.SaveDefaultCity("Milano");
					break;
				case 2:
					App.Instance.SaveDefaultCity("Firenze");
					break;
				case 3:
					App.Instance.SaveDefaultCity("Roma");
					break;
				case 4:
					App.Instance.SaveDefaultCity("Modena");
					break;
			}
		}

	}

	private List<Poi> retriveChargingList() {

		DbManager dbm = App.Instance.dbManager;
		eu.philcar.csg.OBC.db.Pois DaoPois = dbm.getPoisDao();
		return DaoPois.getCityPois(App.DefaultCity);

	}
	private boolean drawChargingStation() {

		if(!firstUpReceived){
			return false;
		}
		dlog.d("drawChargingStation: "+firstUpReceived);

		if(mapView==null || getActivity()==null) {
			dlog.d(" drawPois: mapViw null or contemporary called, or activity null");
			return false;
		}

		try {
			//mapView.deleteAllAnnotationsAndCustomPOIs();
			customView=(RelativeLayout) ((LayoutInflater) (getActivity()).getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
					R.layout.layout_custom_view_poi, null, false);
			List<SKAnnotation> MapAnnotations = mapView.getAllAnnotations();
			ArrayList<Bundle> PoisTemp = Pois;

			//annotationList.clear();
			customView.requestLayout();
			for (Bundle poi : PoisTemp) {
				Bitmap myBitmap;
				SKAnnotation annotation = new SKAnnotation(poi.getInt("INDEX"));
				annotation.setLocation(new SKCoordinate(poi.getDouble("LON"), poi.getDouble("LAT")));
				switch(poi.getInt("type",0)){
					case 3://Charging station
						annotation.setMininumZoomLevel(9);
						break;
					default:
						continue;
				}
				//annotation.setAnnotationType(SKAnnotation.SK_ANNOTATION_TYPE_GREEN);
				SKAnnotationView annotationView = new SKAnnotationView();



				if (customView.findViewById(R.id.customView_poi) != null) {

					((ImageView) (customView.findViewById(R.id.customView_poi))).setImageResource(R.drawable.poi_bonus);

					//((ImageView) (customView.findViewById(R.id.customView_poi))).invalidate(); testinva
					annotationView.setView(customView.findViewById(R.id.customView_poi));

				}
				annotation.setAnnotationView(annotationView);


				//annotation.setAnnotationType(SKAnnotation.SK_ANNOTATION_TYPE_DESTINATION_FLAG);

				annotationList.add(annotation);
				mapView.addAnnotation(annotation, SKAnimationSettings.ANIMATION_NONE);

			}
			//mapView.deleteAllAnnotationsAndCustomPOIs();

			customView.removeAllViews();
		}catch (Exception e){
			DLog.E("error handling bitmap", e);

			return false;
		}


		return true;
	}


	public void updatePoiInfo(int status,Poi Poi){


		if(drawCharging) {
			//rootView.findViewById(R.id.fmapAlertSOCFL).setVisibility(View.VISIBLE);
			//localHandler.sendEmptyMessageDelayed(MSG_CLOSE_SOC_ALERT, 60000);
			//((TextView) (rootView.findViewById(R.id.fmapAlertTitleTV))).setText(R.string.alert_bonus_title);
			//((TextView) (rootView.findViewById(R.id.fmapAlertDescTV))).setText(R.string.alert_bonus);
			drawCharging=false;
			firstUpReceived=true;
			drawChargingStation();
			dlog.d("Displayed bonus poi");

		}
		if(status>0){
			if(!animQueue.contains("bonus")){
				animQueue.add("bonus");
				drawChargingStation();
				dlog.d("Stating bonus anim");

			}
			if(no3gwarning.getAnimation()==null)
				no3gwarning.startAnimation(alertAnimation);
		}
		else {

			if(animQueue.contains("bonus")){
				animQueue.remove("bonus");
			}
		}
	}

	private void queueTTS(String text){
		try{
			if(!ProTTS.reqSystem) {
				ProTTS.askForSystem();
				((AMainOBC) getActivity()).setAudioSystem(LowLevelInterface.AUDIO_SYSTEM,15);
			}
			tts.speak(text);
			dlog.d("queueTTS: leggo " +text);

		}catch (Exception e){
			dlog.e("queueTTS exception while start speak",e);
		}

	}

	private void playAlertAdvice(int resID,String name){
		try{
			if(!AudioPlayer.reqSystem) {
				AudioPlayer.askForSystem();
				((AMainOBC) getActivity()).setAudioSystem(LowLevelInterface.AUDIO_SYSTEM,15);
			}
			player.waitToPlayFile(Uri.parse("android.resource://eu.philcar.csg.OBC/"+ resID));
			dlog.d("playAlertAdvice: play " +name);

		}catch (Exception e){
			dlog.e("playAlertAdvice exception while start speak",e);
		}

	}

}
