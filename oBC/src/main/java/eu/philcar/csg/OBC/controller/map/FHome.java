package eu.philcar.csg.OBC.controller.map;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import eu.philcar.csg.OBC.ABase;
import eu.philcar.csg.OBC.AGoodbye;
import eu.philcar.csg.OBC.AMainOBC;
import eu.philcar.csg.OBC.ASOS;
import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.BuildConfig;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.controller.FBase;
import eu.philcar.csg.OBC.controller.welcome.FDamages;
import eu.philcar.csg.OBC.data.datasources.repositories.EventRepository;
import eu.philcar.csg.OBC.data.datasources.repositories.SharengoApiRepository;
import eu.philcar.csg.OBC.data.datasources.repositories.SharengoPhpRepository;
import eu.philcar.csg.OBC.db.Poi;
import eu.philcar.csg.OBC.devices.LowLevelInterface;
import eu.philcar.csg.OBC.helpers.AudioPlayer;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.ProTTS;
import eu.philcar.csg.OBC.helpers.UrlTools;
import eu.philcar.csg.OBC.service.CarInfo;
import eu.philcar.csg.OBC.service.MessageFactory;

@SuppressLint("SimpleDateFormat")
public class FHome extends FBase implements OnClickListener {

	private DLog dlog = new DLog(this.getClass());
	@Inject
	SharengoApiRepository repository;
	@Inject
	SharengoPhpRepository repositoryPhp;
	@Inject
	EventRepository eventRepository;

	public static FHome newInstance() {
		FHome fm = new FHome();
		return fm;
	}

	private final static int MSG_OPEN_ALERT_AREA = 7;
	private final static int MSG_CLOSE_SOC_ALERT = 8;
	private final static int MSG_OPEN_SOC_ALERT = 9;

	private View rootView;

	private boolean seenA = false, seenB = false;
	private TextView no3gtxt, alertTV;
	private View no3gwarning;
	private RelativeLayout rlBody;
	private LinearLayout fmap_top_LL;
	public static FHome Instance;
	private Animation alertAnimation;
	private static boolean animToggle = true, animFull = false;
	private TextView dayTV, timeTV, tvRange;
	private FrameLayout fmapAlarm, fmapRange;
	private CarInfo localCarInfo;
	private ImageView parkingStatusIV, adIV, no3gIV; // parkingDirectionIV,
	// private View  outsideAreaWarning;

	public static Boolean firstRun = true;
	public static CountDownTimer timer_2min, timer_5sec;

	public static Boolean started = false;

	private List<String> animQueue = new ArrayList<String>();
	private boolean lastInsideArea = true;
	private static int statusAlertSOC = 0, lastInside = 0;//0 none played | 1 played 20km | 2 player 5km  //0:no anim 1:anim3g 2:animArea 3:animBoth
	private static Boolean RequestBanner = false;
	private long updateArea = 0;
	private int alertFLMode = 0;
	private static final int NO_ALERT = 0;
	private static final int ALERT_SOC = 1;
	private static final int ALERT_AREA = 2;

	private static Boolean handleClick = false;

	private ProTTS tts;
	private AudioPlayer player;

	public FHome() {
		Instance = this;

	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		App.get(getActivity()).getComponent().inject(this);

		player = new AudioPlayer(getActivity());

		App.setIsCloseable(false);
	}

	/**
	 * This function create the view for the home fragment
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.f_home, container, false);

		rootView = view;

       /* if(!RequestBanner){
			//controllo se ho il banner e se non ho già iniziato a scaricarlo.
            RequestBanner=true;
            new Thread(new Runnable() {
                @Override
                public void run() {

                    loadBanner(App.URL_AdsBuilderCar,"CAR",false);		//scarico banner
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(FHome.this.isVisible()) {

                                updateBanner("CAR");                        //Modifico l'IV

                                RequestBanner = false;
                            }
                        }
                    });
                }
            }).start();
        }*/

		//uri = Uri.parse("android.resource://eu.philcar.csg.OBC/"+R.raw.out_operative_area_tts_it);

		if (tts != null) {
			tts.shutdown();
			tts = null;
		}
		tts = new ProTTS(getActivity());

		view.findViewById(R.id.fmapVideo).setOnClickListener(this);
		view.findViewById(R.id.fmapSOSB).setOnClickListener(this);
		view.findViewById(R.id.fmapSearchB).setOnClickListener(this);
		view.findViewById(R.id.fmapRadioB).setOnClickListener(this);
		view.findViewById(R.id.fmapDamagesIB).setOnClickListener(this); // momo debug
		//((Button) view.findViewById(R.id.fmapFuelStationsB)).setOnClickListener(this); rimosso su richiesta mkt
		view.findViewById(R.id.fmapCloseTriplB).setOnClickListener(this);
		view.findViewById(R.id.fmapParkB).setOnClickListener(this);
		view.findViewById(R.id.fmapAssicurazione).setOnClickListener(this);
		view.findViewById(R.id.fmapLibretto).setOnClickListener(this);
		view.findViewById(R.id.fmapAlertCloseBTN).setOnClickListener(this);
		view.findViewById(R.id.fmapAlertSOCFL).setOnClickListener(this);

		dayTV = (TextView) view.findViewById(R.id.fmap_date_TV);
		timeTV = (TextView) view.findViewById(R.id.fmap_hour_TV);

		fmap_top_LL = (LinearLayout) view.findViewById(R.id.fmap_top_LL);
		rlBody = (RelativeLayout) view.findViewById(R.id.rlBody);

		parkingStatusIV = (ImageView) view.findViewById(R.id.fmapParkingStatusIV);
		no3gIV = (ImageView) view.findViewById(R.id.no3gIV_HOME);
		// outsideAreaWarning = view.findViewById(R.id.llOutsideArea);
		no3gwarning = view.findViewById(R.id.ll3G_HOME);
		//  parkingDirectionIV = (ImageView) view.findViewById(R.id.fmapParkingDirectionIV);
		fmapAlarm = (FrameLayout) view.findViewById(R.id.fmapAlarm);
		fmapRange = (FrameLayout) view.findViewById(R.id.fmapRange);
		tvRange = (TextView) view.findViewById(R.id.tvRange);
		no3gtxt = (TextView) view.findViewById(R.id.no3gtxt_HOME);
		alertTV = (TextView) view.findViewById(R.id.alertTV);

		alertAnimation = new AlphaAnimation(0.0f, 1.0f);
		alertAnimation.setDuration(500); //You can manage the time of the blink with this parameter
		alertAnimation.setStartOffset(200);
		alertAnimation.setRepeatMode(Animation.REVERSE);
		alertAnimation.setRepeatCount(Animation.INFINITE);
		alertAnimation.setAnimationListener(new Animation.AnimationListener() {

			int index = 0;
			String playing = "";

			@Override
			public void onAnimationStart(Animation animator) {
				dlog.d(FMap.class.toString() + "StartAnimation: alertAnimation");
				index = 0;

				playing = animQueue.get(0);

				switch (playing) {

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
				animToggle = true;
				animFull = false;

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				dlog.d(FMap.class.toString() + "EndAnimation: alertAnimation");

			}

			@Override
			public void onAnimationRepeat(Animation animation) {
				if (animQueue.size() == 0) {
					no3gwarning.clearAnimation();
					return;
				}

				if (animFull) {
					index++;

					if (index >= animQueue.size())
						index = 0;

					playing = animQueue.get(index);

					dlog.d("onAnimationRepeat:repeat anim " + playing);

					switch (playing) {

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

					animFull = !animFull;
				} else
					animFull = !animFull;
			}

		});

		// outsideAreaWarning.setVisibility(View.INVISIBLE); //TODO rimuovere commento e llOutsideArea riga 286
		no3gwarning.setVisibility(View.INVISIBLE);

		fmapAlarm.setVisibility(View.GONE);
		fmapRange.setVisibility(View.GONE);

		if (App.fuel_level != 0)
			tvRange.setText(App.fuel_level + " Km");
		else
			fmapRange.setVisibility(View.INVISIBLE);

		adIV = (ImageView) view.findViewById(R.id.fmapLeftBorderIV);
		adIV.setOnClickListener(this);
		//webViewBanner = (WebView) view.findViewById(R.id.WebViewBanner);

		AMainOBC activity = (AMainOBC) getActivity();
		if (activity != null) {
			updateParkAreaStatus(activity.isInsideParkArea(), activity.getRotationToParkAngle());
		}

		//mapView.getModel().displayModel.setFixedTileSize(256);

		((AMainOBC) getActivity()).setFragmentHandler(localHandler);

		if (App.currentTripInfo != null && App.currentTripInfo.isMaintenance) {
			fmap_top_LL.setBackgroundColor(getResources().getColor(R.color.background_red));
			rlBody.setBackgroundColor(getResources().getColor(R.color.background_red));

		} else {
			fmap_top_LL.setBackgroundColor(getResources().getColor(R.color.background_green));
			rlBody.setBackgroundColor(getResources().getColor(R.color.background_green));
		}

		//timer per aggiornamento advertisment
		int second = 120;
//        if(BuildConfig.FLAVOR.equalsIgnoreCase("develop"))
//            second = 10;
		timer_2min = new CountDownTimer((second) * 1000, 1000) {
			@Override
			public void onTick(long millisUntilFinished) {

			}

			@Override
			public void onFinish() {
				new Thread(new Runnable() {
					@Override
					public void run() {
						loadBanner(App.URL_AdsBuilderCar, "CAR", false);
						try {
							if (getActivity() != null)
								getActivity().runOnUiThread(new Runnable() {
									@Override
									public void run() {
										if (FHome.this.isVisible()) {
											updateBanner("CAR");
											//Modifico l'IV
										} else {
											FMap fMap = (FMap) getFragmentManager().findFragmentByTag(FMap.class.getName());
											if (fMap != null) {

												fMap.updateBanner("CAR");
												//Modifico l'IV
											}
										}
									}
								});

						} catch (Exception e) {
							dlog.e("runOnUiThread: eccezione ", e);
						}
						timer_2min.start();
						started = false;
					}

				}).start();
			}

		};
		timer_5sec = new CountDownTimer((5) * 1000, 1000) {
			@Override
			public void onTick(long millisUntilFinished) {

			}

			@Override
			public void onFinish() {
				new Thread(new Runnable() {
					@Override
					public void run() {
						loadBanner(App.URL_AdsBuilderCar, "CAR", false);
						getActivity().runOnUiThread(new Runnable() {
							@Override
							public void run() {
								if (FHome.this.isVisible()) {
									updateBanner("CAR");
									FHome.timer_2min.start();//Modifico l'IV
								} else {
									FMap fMap = (FMap) getFragmentManager().findFragmentByTag(FMap.class.getName());
									if (fMap != null) {

										fMap.updateBanner("CAR");
										FHome.timer_2min.start();//Modifico l'IV
									} else {
										FHome.timer_2min.cancel();
									}
								}

							}
						});

					}
				}).start();
			}
		};

		if (firstRun) {
			firstRun = !firstRun;
			timer_2min.cancel();
			timer_2min.start();
		}
		updateBanner("CAR");

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		try {
			getActivity().registerReceiver(this.ConnectivityChangeReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
		} catch (Exception e) {
			dlog.e("onResume: Exception while registering receiver ", e);
		}

		//initWebViewBanner();

	}

	public void onPause() {
		super.onPause();
		//actualAnim=0;
		//lastInside=1;
		seenA = false;
		seenB = false;
		lastInsideArea = true;
		try {
			getActivity().unregisterReceiver(this.ConnectivityChangeReceiver);
		} catch (Exception e) {
			dlog.e("onPause: Exception while unresisting receiver ", e);
		}

	}

	/**
	 * Here I clean all the reference to free memory
	 */
	@Override
	public void onDestroy() {
		rootView.findViewById(R.id.fmapVideo).setOnClickListener(null);
		rootView.findViewById(R.id.fmapSOSB).setOnClickListener(null);
		rootView.findViewById(R.id.fmapSearchB).setOnClickListener(null);
		rootView.findViewById(R.id.fmapRadioB).setOnClickListener(null);
		rootView.findViewById(R.id.fmapCloseTriplB).setOnClickListener(null);
		rootView.findViewById(R.id.fmapParkB).setOnClickListener(null);

		localHandler.removeCallbacksAndMessages(null);
		rootView = null;
		tvRange = null;
		fmapRange = null;
		fmap_top_LL = null;
		rlBody = null;
		adIV = null;
		alertAnimation.cancel();
		fmapAlarm = null;
		timeTV = null;
		dayTV = null;
		no3gwarning = null;
		no3gIV = null;
		no3gtxt = null;
		parkingStatusIV = null;
		alertTV = null;
		super.onDestroy();
	}

	@Override
	public void onDetach() {
		super.onDetach();

		if (tts != null) {
			tts.shutdown();
			tts = null;
		}

		firstRun = true;

		statusAlertSOC = 0;

	}

	/**
	 * Handling the click
	 */
	@Override
	public void onClick(View v) {

		switch (v.getId()) {

			case R.id.fmapVideo://SOS
				dlog.cr("MENU CLICK VIDEO");
				((ABase) getActivity()).pushBackFragment(FVideo.newInstance(), FVideo.class.getName(), true);
				break;

			case R.id.fmapSOSB://SOS
				dlog.cr("MENU CLICK SOS");
				startActivity(new Intent(getActivity(), ASOS.class));
				break;

			case R.id.fmapSearchB://Search
				dlog.cr("MENU CLICK MAP");
				((ABase) getActivity()).pushBackFragment(FMap.newInstance(), FMap.class.getName(), true);

				break;

			case R.id.fmapDamagesIB://Search
				dlog.cr("MENU CLICK DAMAGES");
				//((ABase) getActivity()).pushFragment(OptimizeDistanceCalc.newInstance(), OptimizeDistanceCalc.class.getName(), true);

				((ABase) getActivity()).pushFragment(FDamages.newInstance(false), FDamages.class.getName(), true);

				break;

			case R.id.fmapRadioB://Radio
				dlog.cr("MENU CLICK RADIO");
				((ABase) getActivity()).pushFragment(FRadio.newInstance(), FRadio.class.getName(), true);
				break;

			case R.id.fmapCloseTriplB://End Trip
				dlog.cr("MENU CLICK END RENT");
				//((ABase) getActivity()).pushFragment(FMenu.newInstance(FMenu.REQUEST_END_RENT), FMenu.class.getName(), true);
				if (((AMainOBC) getActivity()).checkisInsideParkingArea()) {
					if (App.checkKeyOff && !CarInfo.isKeyOn()) {
						closeTrip();
					} else
						((ABase) getActivity()).pushFragment(FMenu.newInstance(FMenu.REQUEST_END_RENT), FMenu.class.getName(), true);
				} else {
					localHandler.sendEmptyMessage(MSG_OPEN_ALERT_AREA);
					dlog.i("Open Alert Area");
				}
				break;

			case R.id.fmapParkB://End Trip
				dlog.cr("MENU CLICK PARK RENT");
				((ABase) getActivity()).pushFragment(FMenu.newInstance(FMenu.REQUEST_PARK), FMenu.class.getName(), true);
				break;

			case R.id.fmapNavigationB://Deprecated

				break;

      /*    case R.id.fmapMusicB:

              repository.getConfig();
                /*((AMainOBC) getActivity()).setAudioSystem(LowLevelInterface.AUDIO_AUX,30);
                Toast.makeText(App.Instance.getApplicationContext(),getResources().getString(R.string.music_hint),Toast.LENGTH_LONG).show();

                final Resources res = this.getResources();
                final int id = Resources.getSystem().getIdentifier(
                        "config_ntpServer", "string","android");
                final String defaultServer = res.getString(id);*/
			//   break;

			case R.id.fmapAssicurazione:
				dlog.cr("MENU CLICK ASSICURAZIONE");
				((ABase) getActivity()).pushFragment(FPdfViewer.newInstance("ASSICURAZIONE"), FPdfViewer.class.getName(), true);
				//startActivity(new Intent(getActivity(), FPdfViewer.class)); //pushfragment usare

				break;
			case R.id.fmapLibretto:
				dlog.cr("MENU CLICK LIBRETTO");
				((ABase) getActivity()).pushFragment(FPdfViewer.newInstance("LIBRETTO"), FPdfViewer.class.getName(), true);
				//startActivity(new Intent(getActivity(), FPdfViewer.class)); //pushfragment usare

				break;
			case R.id.fmapLeftBorderIV://Banner
				try {
					if (App.BannerName.getBundle("CAR") != null) {
						if (App.BannerName.getBundle("CAR").getString("CLICK").compareTo("null") != 0) {

							if (!handleClick) {
								adIV.setColorFilter(R.color.overlay_click);
								timer_2min.cancel();
								handleClick = true;
								dlog.d(" Banner: Click su banner ");
								new Thread(new Runnable() {
									@Override
									public void run() {

										loadBanner(App.BannerName.getBundle("CAR").getString("CLICK"), "CAR", true);
										getActivity().runOnUiThread(new Runnable() {
											@Override
											public void run() {
												if (FHome.this.isVisible()) {
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
				} catch (Exception e) {
					dlog.e("Exception while clicking on banner", e);
				}
				break;
			case R.id.fmapAlertCloseBTN:
			case R.id.fmapAlertSOCFL:

				localHandler.sendEmptyMessage(MSG_CLOSE_SOC_ALERT);

				break;
		}
	}

	private void closeTrip() {
		AMainOBC activity = (AMainOBC) getActivity();
		if (activity != null && activity.checkisInsideParkingArea()) {
			eventRepository.menuclick("END RENT");
			resetBanner();
			dlog.d("Banner: end rent stopping update, start countdown");
			//((AMainOBC)getActivity()).sendMessage(MessageFactory.setEngine(false));

			Intent i = new Intent(getActivity(), AGoodbye.class);
			i.putExtra(AGoodbye.EUTHANASIA, false);

			startActivity(i);
			((AMainOBC) this.getActivity()).sendMessage(MessageFactory.scheduleSelfCloseTrip(39));
			getActivity().finish();
		}
	}

	private void resetBanner() {
		try {
			FHome.timer_2min.cancel();
			FHome.timer_5sec.cancel();
		} catch (Exception e) {
			dlog.e("Exeption while park", e);
		}
		FHome.firstRun = true;
	}

	/**
	 * Update the area status handle the animation for out of area
	 */
	public void updateParkAreaStatus(boolean isInside, float rotationAngle) {

		//isInside = false;
		//rotationAngle =0;

		updateCloseButton(isInside);

		if (isInside) {

			if (animQueue.contains("area")) {
				animQueue.remove("area");
			}
			if (lastInside != 0 && lastInside != 1)
				eventRepository.outOfArea(false);
			lastInside = 0;

		} else {
			updateArea = 0;
			if (!animQueue.contains("area")) {
				animQueue.add("area");
			}
			if (no3gwarning.getAnimation() == null)
				no3gwarning.startAnimation(alertAnimation);

			if (lastInside == 0) {
				lastInside = 2;

				//((AMainOBC) getActivity()).player.inizializePlayer();
				if (App.USE_TTS_ALERT)
					queueTTS(getActivity().getResources().getString(R.string.alert_area));
				else
					playAlertAdvice(R.raw.out_operative_area_tts, " alert area");

			} else {
				if (lastInside == 1) {
					lastInside = 2;

				}
			}

		}

		//parkingDirectionIV.setRotation((float)rotationAngle);
		lastInsideArea = isInside;
	}

	private void updateCloseButton(boolean isInsideParkArea) {
		if (lastInsideArea != isInsideParkArea) {
			Button closeTripBtn = ((Button) rootView.findViewById(R.id.fmapCloseTriplB));
			if (closeTripBtn == null) {
				dlog.i("updateCloseTripButton: button not found");
				return;
			}
			if (isInsideParkArea) {
				dlog.i("updateCloseTripButton: enabling Button");
				closeTripBtn.setEnabled(true);
				closeTripBtn.setAlpha(1);

			} else {
				dlog.i("updateCloseTripButton: disabling Button");
				//closeTripBtn.setEnabled(false);
				closeTripBtn.setAlpha(0.5f);

			}
		}
	}

	/**
	 * Update the Range indicator and handle the out of charge button display
	 */
	public void updateCarInfo(CarInfo carInfo) {

		try {

			if (carInfo == null)
				return;

			localCarInfo = carInfo;
			refreshConnectionStatus();

			int SOC = carInfo.batteryLevel;

			// if Soc==0 means that software has just started, or it is a demo kit or we lost some connection
			// don't show anything for now
			if (SOC == 0) {
				fmapAlarm.setVisibility(View.VISIBLE);
				fmapRange.setVisibility(View.GONE);
				return;
			}

			if (FHome.this.isVisible())
				if (SOC < 15) {

					if (statusAlertSOC <= 1) {
						dlog.cr("Display popup 5km");
						statusAlertSOC = 2;
						alertFLMode = ALERT_SOC;
						eventRepository.eventSoc(SOC, "Popup 5km");
						rootView.findViewById(R.id.fmapAlertSOCFL).setVisibility(View.VISIBLE);
						rootView.findViewById(R.id.ivDamages).setBackgroundResource(R.drawable.outofcharge);
						rootView.findViewById(R.id.fmapAlertSOCFL).setBackgroundResource(R.drawable.sha_redalertbox);
						((TextView) (rootView.findViewById(R.id.fmapAlertTitleTV))).setText(R.string.alert_warning_title);
						((TextView) (rootView.findViewById(R.id.fmapAlertDescTV))).setText(R.string.alert_5km);
						localHandler.sendEmptyMessageDelayed(MSG_OPEN_SOC_ALERT, 120000);
						//((AMainOBC) getActivity()).player.inizializePlayer();
						if (App.USE_TTS_ALERT)
							queueTTS(getActivity().getResources().getString(R.string.alert_5km));
						else
							playAlertAdvice(R.raw.alert_tts_5km, " alert 5km");
					}
				} else if (SOC <= 30) {
					if (statusAlertSOC <= 0) {
						dlog.cr("Display popup 20km");

						statusAlertSOC = 1;
						alertFLMode = ALERT_SOC;
						eventRepository.eventSoc(SOC, "Popup 20km");
						rootView.findViewById(R.id.ivDamages).setBackgroundResource(R.drawable.almostoutofcharge);
						rootView.findViewById(R.id.fmapAlertSOCFL).setVisibility(View.VISIBLE);
						rootView.findViewById(R.id.fmapAlertSOCFL).setBackgroundResource(R.drawable.sha_orangealertbox);
						((TextView) (rootView.findViewById(R.id.fmapAlertTitleTV))).setText(R.string.alert_warning_title);

						((TextView) (rootView.findViewById(R.id.fmapAlertDescTV))).setText(R.string.alert_20km);
						//localHandler.sendEmptyMessageDelayed(MSG_CLOSE_SOC_ALERT, 20000);
						//(rootView.findViewById(R.id.fmapAlertSOCFL)).invalidate(); testinva

						//((AMainOBC) getActivity()).player.inizializePlayer();
						if (App.USE_TTS_ALERT)
							queueTTS(getActivity().getResources().getString(R.string.alert_20km));
						else
							playAlertAdvice(R.raw.alert_tts_20km, " alert 20km");
					}

				}

			if (SOC <= 30) {
				if (SOC > 15)
					rootView.findViewById(R.id.ivDamages).setBackgroundResource(R.drawable.almostoutofcharge);
				else
					rootView.findViewById(R.id.ivDamages).setBackgroundResource(R.drawable.outofcharge);
				fmapAlarm.setVisibility(View.VISIBLE);
				fmapRange.setVisibility(View.GONE);
			} else {

				localHandler.sendEmptyMessage(MSG_CLOSE_SOC_ALERT);
				rootView.findViewById(R.id.fmapAlertSOCFL).setVisibility(View.GONE);
				fmapAlarm.setVisibility(View.GONE);
				fmapRange.setVisibility(View.VISIBLE);
				tvRange.setText((SOC >= 50 ? SOC : SOC - 10) + " Km");
			}

		} catch (Exception e) {
			dlog.e("Exception while handling carInfo", e);
		}
	}

	/***
	 * Update the information related to the bonus near a poi
	 */

	public void updatePoiInfo(int status, Poi Poi) {

		if (status > 0) {
			if (!animQueue.contains("bonus")) {
				if (App.DefaultCity.toLowerCase().equals("milano")) {
					// AMainOBC.player.reqSystem = true;

				}
				animQueue.add("bonus");
			}
			if (no3gwarning.getAnimation() == null)
				no3gwarning.startAnimation(alertAnimation);
		} else {

			if (animQueue.contains("bonus")) {
				animQueue.remove("bonus");
			}
		}
	}


    /*private void initWebViewBanner() {


        if (webViewBanner == null || !App.hasNetworkConnection) {
            dlog.e("initWebBanner: webView==null or no connection);");
            adIV.setVisibility(View.VISIBLE);

            return;
        }

        WebSettings webSettings = webViewBanner.getSettings();
        webSettings.setAllowFileAccess(false);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
        webSettings.setSupportZoom(false);


        webViewBanner.setWebViewClient(new WebViewClient() {

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                dlog.e("WebBanner: " + failingUrl + " -- ErrorCode: " + errorCode);
                adIV.setVisibility(View.VISIBLE);
                dlog.d("WebBanner: disabled");
            }

        });

        // Attach JS interface object
        webViewBanner.addJavascriptInterface(new BannerJsInterface(), "OBC");

        List<NameValuePair> paramsList = new ArrayList<NameValuePair>();

        if (App.currentTripInfo != null && App.currentTripInfo.customer != null)
            paramsList.add(new BasicNameValuePair("id", App.currentTripInfo.customer.id + ""));

        if (App.lastLocation != null) {
            paramsList.add(new BasicNameValuePair("lat", App.lastLocation.getLatitude() + ""));
            paramsList.add(new BasicNameValuePair("lon", App.lastLocation.getLongitude() + ""));
        }
        paramsList.add(new BasicNameValuePair("id_fleet", App.FleetId + ""));
        paramsList.add(new BasicNameValuePair("carplate", App.CarPlate));

        HttpUrl url = UrlTools.buildQuery(App.URL_AdsBuilder, paramsList);
        if (url != null) {
            String strUrl = url.toString();
            webViewBanner.loadUrl(strUrl);
            adIV.setVisibility(View.INVISIBLE);
            dlog.d("WebBanner enabled  : " + strUrl);
        }


    }
    public void initWebBanner(String Url) {

        if (webViewBanner==null || !App.hasNetworkConnection) {
            dlog.e("initWebBanner: webView==null or no connection);");
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
                dlog.e("WebBanner: "+ failingUrl +" -- ErrorCode: " + errorCode);
                adIV.setImageResource(R.drawable.car_banner_offline);
                adIV.setVisibility(View.VISIBLE);
                dlog.d("WebBanner: disabled");
            }

        });

        // Attach JS interface object
        webViewBanner.addJavascriptInterface(new BannerJsInterface(), "OBC");

        //non ci sono parametri, visualizzo l'immagine.

        //HttpUrl url = UrlTools.buildQuery(Url, paramsList);
        if (Url!=null) {
            //String  strUrl =  url.toString();
            webViewBanner.loadUrl(Url);
            adIV.setVisibility(View.INVISIBLE);
            dlog.d("WebBanner enabled single image : " + Url);
        }else{
            adIV.setImageResource(R.drawable.car_banner_offline);
            adIV.setVisibility(View.VISIBLE);

        }


    }*/
	/**
	 * Handle message to update the time
	 */
	@SuppressLint("HandlerLeak")
	private Handler localHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {

				case AMainOBC.MSG_UPDATE_DATE:
					if (msg.obj != null && dayTV != null) {
						dayTV.setText((String) msg.obj);
					}
					break;

				case AMainOBC.MSG_UPDATE_TIME:
					if (msg.obj != null && timeTV != null) {
						timeTV.setText((String) msg.obj);
					}
					break;
				case MSG_CLOSE_SOC_ALERT:
					alertFLMode = NO_ALERT;
					localHandler.removeMessages(MSG_CLOSE_SOC_ALERT);
					//dlog.cr("Chiusura alert SOC");
					rootView.findViewById(R.id.fmapAlertSOCFL).setVisibility(View.GONE);
					//rootView.findViewById(R.id.fmapAlertSOCFL).invalidate(); testinva
					break;
				case MSG_OPEN_SOC_ALERT:
					alertFLMode = ALERT_SOC;
					localHandler.removeMessages(MSG_OPEN_SOC_ALERT);
					if (localCarInfo != null && localCarInfo.batteryLevel <= 15) {
						rootView.findViewById(R.id.fmapAlertSOCFL).setVisibility(View.VISIBLE);
						rootView.findViewById(R.id.ivDamages).setBackgroundResource(R.drawable.outofcharge);
						((rootView.findViewById(R.id.fmapAlertSOCFL))).setBackgroundResource(R.drawable.sha_redalertbox);
						((TextView) (rootView.findViewById(R.id.fmapAlertTitleTV))).setText(R.string.alert_warning_title);
						((TextView) (rootView.findViewById(R.id.fmapAlertDescTV))).setText(R.string.alert_5km);
						localHandler.sendEmptyMessageDelayed(MSG_OPEN_SOC_ALERT, 120000);
						rootView.findViewById(R.id.fmapAlertSOCFL).setVisibility(View.VISIBLE);
						//rootView.findViewById(R.id.fmapAlertSOCFL).invalidate(); testinva
					}
					break;
				case MSG_OPEN_ALERT_AREA:
					alertFLMode = ALERT_AREA;
					dlog.cr("DISPLAY ALERT AREA");
					localHandler.removeMessages(MSG_OPEN_ALERT_AREA);
					rootView.findViewById(R.id.fmapAlertSOCFL).setVisibility(View.VISIBLE);
					((rootView.findViewById(R.id.fmapAlertSOCFL))).setBackgroundResource(R.drawable.sha_orangealertbox);
					((TextView) (rootView.findViewById(R.id.fmapAlertTitleTV))).setText(R.string.alert_warning_title);
					((TextView) (rootView.findViewById(R.id.fmapAlertDescTV))).setText(R.string.alert_area_box);
					//rootView.findViewById(R.id.fmapAlertSOCFL).invalidate(); testinva

					break;

			}
		}
	};

	/**
	 * Receive ConnectivityChangeBroadcast to update the animation for NO3G
	 */
	private final BroadcastReceiver ConnectivityChangeReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context c, Intent i) {
			refreshConnectionStatus();

		}

	};

	private void refreshConnectionStatus() {
		try {
			if (getActivity() == null && getActivity() == null)
				return;
			boolean status = App.hasNetworkConnection();

			if (status) {
				if (animQueue.contains("3g")) {
					animQueue.remove("3g");
				}

			} else {
				if (!animQueue.contains("3g")) {
					animQueue.add("3g");
				}
				if (no3gwarning.getAnimation() == null)
					no3gwarning.startAnimation(alertAnimation);
			}
		} catch (Exception e) {
			dlog.e("Exception while refreshing ConnectionStatus", e);
		}
	}

	/**
	 * With a given URL and type (END-START...) load the Banner ID, if present Use the downloaded one otherwise download it.
	 * After Put the ID inside the App variable to share the current banner
	 */
	private void loadBanner(String Url, String type, Boolean isClick) {
		try {

			File outDir = new File(App.getBannerImagesFolder());
			if (!outDir.isDirectory()) {
				outDir.mkdir();
			}

			if (!App.hasNetworkConnection()) {
				dlog.e(FHome.class.toString() + " loadBanner: nessuna connessione");
				App.BannerName.putBundle(type, null);//null per identificare nessuna connessione, caricare immagine offline
				return;
			}
			StringBuilder builder = new StringBuilder();
			List<NameValuePair> paramsList = new ArrayList<NameValuePair>();
			if (!isClick) {

				if (App.currentTripInfo != null && App.currentTripInfo.customer != null)
					if (BuildConfig.BUILD_TYPE.equalsIgnoreCase("debug"))
						paramsList.add(new BasicNameValuePair("id", "26740"));// App.currentTripInfo.customer.id + "")); //"3"));
				paramsList.add(new BasicNameValuePair("id", App.currentTripInfo.customer.id + ""));// App.currentTripInfo.customer.id + "")); //"3"));

				if (App.getLastLocation() != null) {
					paramsList.add(new BasicNameValuePair("lat", App.getLastLocation().getLatitude() + ""));
					paramsList.add(new BasicNameValuePair("lon", App.getLastLocation().getLongitude() + ""));
				}
				paramsList.add(new BasicNameValuePair("id_fleet", App.FleetId + ""));
				paramsList.add(new BasicNameValuePair("carplate", App.CarPlate));
			}
			try {
				if (App.BannerName.getBundle(type) != null)
					paramsList.add(new BasicNameValuePair("index", App.BannerName.getBundle(type).getString("INDEX", null)));

				Url = UrlTools.buildQuery(Url.concat("?"), paramsList).toString();
				//connessione per scaricare id immagine

				HttpClient client = new DefaultHttpClient();
				HttpGet httpGet = new HttpGet(Url);

				HttpResponse response = client.execute(httpGet);
				DLog.I(FHome.class.toString() + " loadBanner: Url richiesta " + Url);
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
					reader.close();
				} else {

					dlog.e(" loadBanner: Failed to connect " + String.valueOf(statusCode));
					App.BannerName.putBundle(type, null);//null per identificare nessuna connessione, caricare immagine offline
					return;
				}
			} catch (Exception e) {
				dlog.e(" loadBanner: eccezione in connessione ", e);
				App.BannerName.putBundle(type, null);//null per identificare nessuna connessione, caricare immagine offline
				return;
			}
			String jsonStr = builder.toString();
			if (jsonStr.compareTo("") == 0) {
				dlog.e(" loadBanner: nessuna connessione");
				App.BannerName.putBundle(type, null);//null per identificare nessuna connessione, caricare immagine offline
				return;
			}

			DLog.I(FHome.class.toString() + " loadBanner: risposta " + jsonStr);
			File file = new File(outDir, "placeholder.lol");

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

				App.BannerName.putBundle(type, Image);

				//ricavo nome file
				URL urlImg = new URL(Image.getString("URL").replace(" ", "%20"));
				String extension = urlImg.getFile().substring(urlImg.getFile().lastIndexOf('.') + 1);
				String filename = Image.getString("ID").concat(".").concat(extension);

				//download imagine se non esiste
				file = new File(outDir, filename);

				if (file.exists()) {
					Image.putString(("FILENAME"), filename);
					App.BannerName.putBundle(type, Image);
					dlog.i(FHome.class.toString() + " loadBanner: file già esistente: " + filename);
					return;
				}

				dlog.i(FHome.class.toString() + " loadBanner: file mancante inizio download a url: " + urlImg.toString());
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
				inputStream.close();
				Image.putString(("FILENAME"), filename);
				App.BannerName.putBundle(type, Image);
				dlog.i(FHome.class.toString() + " loadBanner: File scaricato e creato " + filename);
				urlConnection.disconnect();

			} catch (Exception e) {
				if (file.exists()) file.delete();
				dlog.e(FHome.class.toString() + " loadBanner: eccezione in creazione e download file ", e);

				e.printStackTrace();
			}

		} catch (Exception e) {
			dlog.e("loadBanner: Exception", e);
		}
	}

	/**
	 * Set to screen the current banner from the App variable
	 */
	public void updateBanner(String type) {
		try {

			File ImageV;
			Bundle Banner = App.BannerName.getBundle(type);
			if (Banner != null) {
				ImageV = new File(App.getBannerImagesFolder(), Banner.getString("FILENAME", "placeholder.lol"));

				try {
					if (ImageV != null && ImageV.exists()) {
						dlog.i(FHome.class.toString() + " updateBanner: file trovato imposto immagine " + ImageV.getName());
						Bitmap myBitmap = BitmapFactory.decodeFile(ImageV.getAbsolutePath());
						if (myBitmap == null) {

							dlog.e(FHome.class.toString() + " updateBanner: file corrotto, elimino e visualizzo offline ");
							ImageV.delete();
							//initWebBanner(Banner.getString("URL",null));
							// webViewBanner.setVisibility(View.INVISIBLE);
							adIV.setImageResource(R.drawable.car_banner_offline);
							adIV.setVisibility(View.VISIBLE);
							if (!started) {
								started = !started;
								timer_2min.cancel();
								timer_2min.start();
							}
							return;
						}
						//webViewBanner.setVisibility(View.INVISIBLE);
						adIV.setImageBitmap(myBitmap);
						adIV.setVisibility(View.VISIBLE);
						adIV.invalidate();

					}
				} catch (Exception e) {
					dlog.e(FHome.class.toString() + " updateBanner: eccezione in caricamento file visualizzo offline ", e);
					e.printStackTrace();
					//initWebBanner(Banner.getString("URL",null));
					// webViewBanner.setVisibility(View.INVISIBLE);
					adIV.setImageResource(R.drawable.car_banner_offline);
					adIV.setVisibility(View.VISIBLE);
				}
			} else {
				dlog.e(FHome.class.toString() + " updateBanner: Bundle null, visualizzo offline");
				//initWebBanner(Banner.getString("URL",null));
				//webViewBanner.setVisibility(View.INVISIBLE);
				adIV.setImageResource(R.drawable.car_banner_offline);
				adIV.setVisibility(View.VISIBLE);
			}

			try {
				if (!started && timer_2min != null) {
					started = !started;
					timer_2min.cancel();
					timer_2min.start();
				}
			} catch (Exception e) {
				dlog.e("Exception trying to start timer", e);
			}

		} catch (Exception e) {
			dlog.e("Exception updating Banner");
		}
	}

	private void queueTTS(String text) {
		try {
			if (!ProTTS.reqSystem) {
				ProTTS.askForSystem();
				((AMainOBC) getActivity()).setAudioSystem(LowLevelInterface.AUDIO_SYSTEM, LowLevelInterface.AUDIO_LEVEL_ALERT);
			}
			tts.speak(text);
			dlog.d("queueTTS: leggo " + text);

		} catch (Exception e) {
			dlog.e("queueTTS exception while start speak", e);
		}

	}

	private void playAlertAdvice(int resID, String name) {
		try {
			if (!AudioPlayer.reqSystem) {
				AudioPlayer.askForSystem();
				((AMainOBC) getActivity()).setAudioSystem(LowLevelInterface.AUDIO_SYSTEM, LowLevelInterface.AUDIO_LEVEL_ALERT);
			}
			player.waitToPlayFile(Uri.parse("android.resource://eu.philcar.csg.OBC/" + resID));
			dlog.d("playAlertAdvice: play " + name);
			dlog.cr("Riproduco avviso vocale: play " + name);

		} catch (Exception e) {
			dlog.e("playAlertAdvice exception while start speak", e);
		}

	}
}
