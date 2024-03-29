package eu.philcar.csg.OBC.controller.welcome;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import eu.philcar.csg.OBC.App;
//import eu.philcar.csg.OBC.BuildConfig;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.controller.FBase;
import eu.philcar.csg.OBC.data.datasources.repositories.EventRepository;
import eu.philcar.csg.OBC.devices.LowLevelInterface;
import eu.philcar.csg.OBC.helpers.AudioPlayer;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.Debug;
import eu.philcar.csg.OBC.helpers.ProTTS;
import eu.philcar.csg.OBC.helpers.UrlTools;
import eu.philcar.csg.OBC.service.MessageFactory;

public class FGoodbye extends FBase {

	@Inject
	EventRepository eventRepository;

	private final static int MSG_CLOSE_ACTIVITY = 1;
	private final static int MSG_PLAY_ADVICE = 2;
	private static int closingTripid;
	private static Boolean handleClick = false;
	private static CountDownTimer timer_5sec, selfclose = null;
	private static Boolean RequestBanner = false;
//	private static boolean played = false;
	private DLog dlog = new DLog(this.getClass());
	//private WebView webViewBanner;
	private ImageView adIV;
	private ProTTS tts;
	private AudioPlayer player;
	private Handler localHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {

				case MSG_CLOSE_ACTIVITY:
					try {
						dlog.d("FGoodbye timeout ");
						if (getActivity() != null)
							if (App.currentTripInfo == null)
								(getActivity()).finish();
							else if (App.currentTripInfo.isOpen) {
								dlog.d("handleMessage:MSG_CLOSE_ACTIVITY Unable to close trip, retry close trip");
								if (App.currentTripInfo.trip.id == closingTripid) {
									((AGoodbye) getActivity()).sendMessage(MessageFactory.forceCloseTrip());
								}
								localHandler.removeMessages(MSG_CLOSE_ACTIVITY);
								localHandler.sendEmptyMessageDelayed(MSG_CLOSE_ACTIVITY, 10000);

								/*Intent i = new Intent(getActivity(), AMainOBC.class);
								i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
								startActivity(i);
								getActivity().finish();*/
							}
					} catch (Exception e) {
						dlog.e("FGoodbye : MSG_CLOSE_ACTIVITY Exception", e);
					}
					break;
				case MSG_PLAY_ADVICE:
					try {
						if (App.USE_TTS_ALERT)
							queueTTS(getResources().getString(R.string.alert_key));
						else
							playAlertAdvice(R.raw.alert_tts_end, " alert end key");
					} catch (Exception e) {
						dlog.e("FGoodbye : MSG_CLOSE_FRAGMENT Exception", e);
					}
					break;
			}
		}
	};

	public static FGoodbye newInstance() {

		return new FGoodbye();
	}

	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		closingTripid = App.currentTripInfo.trip.id;
		App.Instance.getComponent().inject(this);
        /*if(App.first_UP_End && App.hasNetworkConnection){
			App.first_UP_End=false;

			new Thread(new Runnable() {
				public void run() {
					updatePoiIcon(App.URL_UpdateEndImages);
				}
			}).start();
		}
		if((new Date().getTime()-App.update_EndImages.getTime())>3600000 && App.hasNetworkConnection) {    //3600000 = 1 ora


			new Thread(new Runnable() {
				public void run() {
					updatePoiIcon(App.URL_UpdateEndImages);
				}
			}).start();
		}*/

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		App.setIsCloseable(true);
		App.isClosing = true;
		dlog.d("OnCreareView FGoodbye");
		try {
			if ((App.BannerName != null && App.BannerName.getBundle("END") == null) && !RequestBanner) {
				//controllo se ho il banner e se non ho già iniziato a scaricarlo.
				RequestBanner = true;
				Thread bannerUpdate = new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							loadBanner(App.URL_AdsBuilderEnd, "END", false);        //scarico banner
							getActivity().runOnUiThread(new Runnable() {
								@Override
								public void run() {
									try {
										FGoodbye fGoodbye = (FGoodbye) getFragmentManager().findFragmentByTag(FGoodbye.class.getName());
										if (fGoodbye != null) {
											updateBanner("END");                            //Modifico l'IV
										}
									} catch (Exception e) {
										dlog.e("updateBanner: eccezione in chiamata", e);
									}
								}
							});
						} catch (Exception e) {
							dlog.e("FGoodbye: Eccezione durante l'update dell'immagine", e);
						}
					}
				});
				bannerUpdate.start();
			}
		} catch (Exception e) {
			dlog.e("Exception while updating banner end", e);
		}
		dlog.d("FGoodbye: onCreateView");
		//((AMainOBC) getActivity()).player.inizializePlayer();
		//getActivity().getResources().getString(R.string.alert_key));

		Bundle b = this.getArguments();
		if (b == null || !b.containsKey("CLOSE")) {
			dlog.e("Started FGoodbye without CLOSE key, aborting");
			this.getActivity().finish();
			return null;
		}

		final View view = inflater.inflate(R.layout.f_goodbye, container, false);

		Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "interstateregular.ttf");

		(view.findViewById(R.id.llSelfClose)).setVisibility(View.INVISIBLE);

		tts = new ProTTS(getActivity());
		player = new AudioPlayer(getActivity());

		((TextView) view.findViewById(R.id.fgodTopTV)).setTypeface(font);
		((TextView) view.findViewById(R.id.fgod_Goodbye_Title_TV)).setTypeface(font);
		((TextView) view.findViewById(R.id.fgodGoodbyeTV)).setTypeface(font);
		//webViewBanner = (WebView)view.findViewById(R.id.fgoodWV);
		adIV = (ImageView) view.findViewById(R.id.fgoodIV);
		timer_5sec = new CountDownTimer((5) * 1000, 1000) {
			@Override
			public void onTick(long millisUntilFinished) {

			}

			@Override
			public void onFinish() {
				Thread afterClick = new Thread(new Runnable() {
					@Override
					public void run() {
						loadBanner(App.URL_AdsBuilderEnd, "END", false);
						try {
							getActivity().runOnUiThread(new Runnable() {
								@Override
								public void run() {
									if (FGoodbye.this.isVisible()) {
										updateBanner("END");
									}
								}
							});
						} catch (Exception e) {
							dlog.e("Exception during update UI ", e);
						}

					}
				});
				afterClick.start();
			}
		};
		adIV.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (App.BannerName.getBundle("END") != null) {
					try {
						if (App.BannerName.getBundle("END").getString("CLICK", "null").compareTo("null") != 0) {

							if (!handleClick) {
								adIV.setColorFilter(R.color.overlay_click);
								handleClick = true;
								dlog.i(" Click su banner ");
								Thread onBannerClick = new Thread(new Runnable() {
									@Override
									public void run() {
										try {
											loadBanner(App.BannerName.getBundle("END").getString("CLICK"), "END", true);
											getActivity().runOnUiThread(new Runnable() {
												@Override
												public void run() {
													if (FGoodbye.this.isVisible()) {
														adIV.clearColorFilter();
														updateBanner("END");
														timer_5sec.start();//Modifico l'IV
														handleClick = false;
													}
												}
											});
										} catch (Exception e) {
											dlog.e("Exception while updating banner", e);
										}

									}
								});
								onBannerClick.start();
							}
						}
					} catch (Exception e) {
						dlog.e("Exception during onclick", e);
					}
				}
			}
		});
		view.findViewById(R.id.cancelActionIB).setOnClickListener(this::undoCloseTrip);
		String name = "";
		if (App.currentTripInfo != null && App.currentTripInfo.customer != null) {
			name = " " + App.currentTripInfo.customer.name + " " + App.currentTripInfo.customer.surname;
		} else if (Debug.IGNORE_HARDWARE) {
			name = "Gino Panino";
		}
/*		if (BuildConfig.BUILD_TYPE.equalsIgnoreCase("debug"))
			name = "Gino Panino cotto e rosa";*/
		((TextView) view.findViewById(R.id.fgodGoodbyeTV)).setText(name);

		((AGoodbye) this.getActivity()).sendMessage(MessageFactory.scheduleSelfCloseTrip(40));
		(view.findViewById(R.id.llSelfClose)).setVisibility(View.VISIBLE);
		final Activity activity = this.getActivity();
		if (selfclose != null)
			selfclose.cancel();
		selfclose = new CountDownTimer(40000, 1000) {
			@Override
			public void onTick(long millisUntilFinished) {
				((TextView) view.findViewById(R.id.tvCountdown)).setText((millisUntilFinished / 1000) + " s");
			}

			@Override
			public void onFinish() {
				try {
					((TextView) view.findViewById(R.id.tvCountdown)).setText("0 s");
					dlog.d(FGoodbye.class.toString() + " onFinish: finished countdown, ending activity");
					if (getActivity() == null || App.currentTripInfo == null) {
						dlog.d(" onFinish: no activity return");
						return;
					}
					try {
						wait(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (App.currentTripInfo != null && App.currentTripInfo.trip.id == closingTripid) {
						((AGoodbye) activity).sendMessage(MessageFactory.forceCloseTrip());
					}
					localHandler.removeMessages(MSG_CLOSE_ACTIVITY);
					localHandler.sendEmptyMessageDelayed(MSG_CLOSE_ACTIVITY, 10000);
				} catch (Exception e) {
					dlog.e("Exception while forcing trip close", e);
				}

			}

		}.start();
		dlog.d("FGoodbye: starting countdown");
		
		/*Handler h = new Handler();
		h.postDelayed(new Runnable() {
			@Override
			public void run() {
				startActivity(new Intent(getActivity(), AWelcome.class));
				getActivity().finish();
			}
		}, 5000);*/

		//App.Instance.BannerName.clear();

		dlog.i("Setup isBonusEnabled = " + App.currentTripInfo.isBonusEnabled);
		if (App.currentTripInfo.isBonusEnabled) {
			(view.findViewById(R.id.fgodTopTV)).setVisibility(View.GONE);
			((TextView) view.findViewById(R.id.fgodInstructionTV)).setText(R.string.instruction_close_4);
			(view.findViewById(R.id.fgodBonusLL)).setVisibility(View.VISIBLE);
			((TextView) view.findViewById(R.id.fgodBonusTV)).setText(R.string.bonus_message_poi);
		} else {
			(view.findViewById(R.id.fgodTopTV)).setVisibility(View.VISIBLE);
			((TextView) view.findViewById(R.id.fgodInstructionTV)).setText(R.string.goodbye_instructions);
//			((TextView)view.findViewById(R.id.fgodInstructionTV)).setTextSize(24f);
//			((TextView)view.findViewById(R.id.fgodTopTV)).setTextSize(30f);
			(view.findViewById(R.id.fgodBonusLL)).setVisibility(View.GONE);

			((TextView) view.findViewById(R.id.fgodBonusTV)).setText("");

		}

		updateBanner("END");
        /*if (!played) {
            played = true;
            localHandler.removeMessages(MSG_PLAY_ADVICE);
            localHandler.sendEmptyMessageDelayed(MSG_PLAY_ADVICE, 1000);
        }*/
		return view;
	}

	@Override
	public boolean handleBackButton() {

		return true;
	}

	private void undoCloseTrip(View v) {
		App.setIsCloseable(false);
		((ABase) getActivity()).sendMessage(MessageFactory.scheduleSelfCloseTrip(0));
		eventRepository.menuclick("UNDO END RENT");
		startActivity(new Intent(getActivity(), AMainOBC.class));
		getActivity().finish();

	}

	@Override
	public void onResume() {
		super.onResume();

		/*new Thread(new Runnable() {
			public void run() {
				initWebViewBanner();
			}
		}).start();*/

	}

	@Override
	public void onPause() {
		super.onPause();
	}

	public void loadBanner(String Url, String type, Boolean isClick) {

		//for countries outside Italy use local banners
		if (!App.Instance.getDefaultLang().equals("it")) {
			dlog.e(" loadBanner: get LOCAL banner");
			App.BannerName.putBundle(type, null);//null per identificare nessuna connessione, caricare immagine offline
			return;
		}

		File outDir = new File(App.getBannerImagesFolder());
		if (outDir.mkdir()) {
			dlog.d("Banner directory created");
		}

		if (!App.hasNetworkConnection()) {
			dlog.e(" loadBanner: nessuna connessione");
			App.BannerName.putBundle(type, null);//null per identificare nessuna connessione, caricare immagine offline
			return;
		}
		StringBuilder builder = new StringBuilder();
		List<NameValuePair> paramsList = new ArrayList<>();
		HttpResponse response;
		if (!isClick) {

			if (App.currentTripInfo != null && App.currentTripInfo.customer != null)
				paramsList.add(new BasicNameValuePair("id", App.currentTripInfo.customer.id + ""));//App.currentTripInfo.customer.id + "")); //"3"));

			if (App.getLastLocation() != null) {
				paramsList.add(new BasicNameValuePair("lat", App.getLastLocation().getLatitude() + ""));
				paramsList.add(new BasicNameValuePair("lon", App.getLastLocation().getLongitude() + ""));
			}
			paramsList.add(new BasicNameValuePair("id_fleet", App.FleetId + ""));
			paramsList.add(new BasicNameValuePair("carplate", App.CarPlate));//"ED93107"));//App.CarPlate));
		}
		try {
			if (App.BannerName != null && App.BannerName.getBundle(type) != null)
				paramsList.add(new BasicNameValuePair("index", App.BannerName.getBundle(type).getString("INDEX", null)));

			if (App.BannerName != null && App.BannerName.getBundle(type) != null)
				paramsList.add(new BasicNameValuePair("end", App.BannerName.getBundle(type).getString("END", null)));

			Url = UrlTools.buildQuery(Url.concat("?"), paramsList).toString();
			//connessione per scaricare id immagine

			HttpClient client = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(Url);

			response = client.execute(httpGet);
			DLog.I(" loadBanner: Url richiesta " + Url);
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
				reader.close();
				content.close();
			} else {
				dlog.e(" loadBanner: Failed to connect " + statusCode);
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

		DLog.I(" loadBanner: risposta " + jsonStr);
		File file = new File(outDir, "placeholder.lol");

		try {
			JSONObject json = new JSONObject(jsonStr);

			//Get the instance of JSONArray that contains JSONObjects
			JSONArray jsonArray = json.optJSONArray("Image");

			//Iterate the jsonArray and print the info of JSONObjects

			Bundle Image = new Bundle();
			JSONObject jsonObject = jsonArray.getJSONObject(0);
			if (jsonObject.has("ID"))
				Image.putString("ID", jsonObject.getString("ID"));
			if (jsonObject.has("URL"))
				Image.putString("URL", jsonObject.getString("URL"));
			if (jsonObject.has("CLICK"))
				Image.putString(("CLICK"), jsonObject.getString("CLICK"));
			if (jsonObject.has("INDEX"))
				Image.putString(("INDEX"), jsonObject.getString("INDEX"));
			if (jsonObject.has("END"))
				Image.putString(("END"), jsonObject.getString("END"));

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
				dlog.i(" loadBanner: file già esistente: " + filename);
				return;
			}

			dlog.i(" loadBanner: file mancante inizio download a url: " + urlImg.toString());
			HttpURLConnection urlConnection = (HttpURLConnection) urlImg.openConnection();
			urlConnection.setRequestMethod("GET");
			urlConnection.setDoOutput(true);
			urlConnection.connect();
			if (file.createNewFile()) {
				file.createNewFile();
			}
			FileOutputStream fileOutput = new FileOutputStream(file);
			InputStream inputStream = urlConnection.getInputStream();
//			int totalSize = urlConnection.getContentLength();
//			int downloadedSize = 0;
			byte[] buffer = new byte[1024];
			int bufferLength = 0;
			while ((bufferLength = inputStream.read(buffer)) > 0) {
				fileOutput.write(buffer, 0, bufferLength);
				//downloadedSize += bufferLength;
				//Log.i("Progress:", "downloadedSize:" + downloadedSize + "totalSize:" + totalSize);
			}
			fileOutput.close();
			Image.putString(("FILENAME"), filename);
			App.BannerName.putBundle(type, Image);
			dlog.i(" loadBanner: File scaricato e creato " + filename);
			urlConnection.disconnect();
			inputStream.close();

		} catch (Exception e) {
			if (file.exists()) {
				file.delete();
			}
			App.BannerName.putBundle(type, null);//null per identificare nessuna connessione, caricare immagine offline
			dlog.e(" loadBanner: eccezione in creazione e download file ", e);

			e.printStackTrace();
		}

	}

	private void updateBanner(String type) {
		try {
			File ImageV;
			Bundle Banner = App.BannerName.getBundle(type);
			if (Banner != null) {
				ImageV = new File(App.getBannerImagesFolder(), Banner.getString("FILENAME", ""));

				try {
					if (ImageV.exists()) {
						dlog.i(FGoodbye.class.toString() + " updateBanner: file trovato imposto immagine " + ImageV.getName());
						Bitmap myBitmap = BitmapFactory.decodeFile(ImageV.getAbsolutePath());
						if (myBitmap == null) {

							dlog.e(FGoodbye.class.toString() + " updateBanner: file corrotto, elimino e visualizzo offline ");
							ImageV.delete();
							//initWebBanner(Banner.getString("URL",null));
							//webViewBanner.setVisibility(View.INVISIBLE);
							adIV.setImageResource(R.drawable.offline_goodbye);
							adIV.setVisibility(View.VISIBLE);
							return;
						}
						//webViewBanner.setVisibility(View.INVISIBLE);

						adIV.setImageBitmap(myBitmap);
						adIV.setVisibility(View.VISIBLE);
						adIV.invalidate();
//						return;

					}
				} catch (Exception e) {
					dlog.e(FGoodbye.class.toString() + " updateBanner: eccezione in caricamento file visualizzo offline ", e);
					e.printStackTrace();
					//initWebBanner(Banner.getString("URL",null));
					//webViewBanner.setVisibility(View.INVISIBLE);
					adIV.setImageResource(R.drawable.offline_goodbye);
					adIV.setVisibility(View.VISIBLE);
//					return;
				}
			} else {
				dlog.e("FGoodbye.updateBanner();Bundle null, visualizzo offline");
				//initWebBanner(Banner.getString("URL",null));
				//webViewBanner.setVisibility(View.INVISIBLE);
				adIV.setImageResource(R.drawable.offline_goodbye);
				adIV.setVisibility(View.VISIBLE);
//				return;
			}
		} catch (Exception e) {
			dlog.e("FGoodbye.updateBanner();", e);
		}

	}

	@Override
	public void onDetach() {
		super.onDetach();

		if (tts != null) {
			tts.shutdown();
			tts = null;
		}
	}

	@Override
	public void onDestroy() {
		try {
			localHandler.removeCallbacksAndMessages(null);
			adIV = null;
			timer_5sec.cancel();
			handleClick = null;
			RequestBanner = null;
//			played = false;
			if (selfclose != null)
				selfclose.cancel();

		} catch (Exception e) {
			dlog.e("Exception while cleaning memory", e);
		} finally {

			super.onDestroy();
		}
	}

	private void queueTTS(String text) {
		try {

			if (!ProTTS.reqSystem) {
				ProTTS.askForSystem();
				((AGoodbye) getActivity()).setAudioSystem(LowLevelInterface.AUDIO_SYSTEM, LowLevelInterface.AUDIO_LEVEL_ALERT);
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
				((AGoodbye) getActivity()).setAudioSystem(LowLevelInterface.AUDIO_SYSTEM, LowLevelInterface.AUDIO_LEVEL_ALERT);
			}
			player.waitToPlayFile(Uri.parse("android.resource://eu.philcar.csg.OBC/" + resID));
			dlog.d("playAlertAdvice: play " + name);

		} catch (Exception e) {
			dlog.e("playAlertAdvice exception while start speak", e);
		}

	}
}
