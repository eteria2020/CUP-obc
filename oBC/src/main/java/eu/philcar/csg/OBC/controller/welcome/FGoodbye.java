package eu.philcar.csg.OBC.controller.welcome;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.NetworkOnMainThreadException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import eu.philcar.csg.OBC.ABase;
import eu.philcar.csg.OBC.AGoodbye;
import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.controller.FBase;
import eu.philcar.csg.OBC.controller.map.FMap;
import eu.philcar.csg.OBC.devices.LowLevelInterface;
import eu.philcar.csg.OBC.helpers.BannerJsInterface;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.Debug;
import eu.philcar.csg.OBC.helpers.UrlTools;
import eu.philcar.csg.OBC.service.MessageFactory;
import eu.philcar.csg.OBC.service.TripInfo;
import okhttp3.HttpUrl;

public class FGoodbye extends FBase {

	private DLog dlog = new DLog(this.getClass());
	//private WebView webViewBanner;
	private ImageView adIV;
	private static Boolean handleClick=false;
	private static CountDownTimer timer_5sec,selfclose;
	private static Boolean RequestBanner=false;
	private final static int  MSG_CLOSE_ACTIVITY  = 1;


	private Handler localHandler = new Handler()  {

		@Override
		public void handleMessage(Message msg) {

			switch (msg.what)  {


				case MSG_CLOSE_ACTIVITY:
					try {
						dlog.d("FInstruction timeout ");
						(getActivity()).finish();
					}catch(Exception e){
						dlog.e("FInstruction : MSG_CLOSE_FRAGMENT Exception",e);
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
		
		App.isCloseable = true;
		App.isClosing=true;
		dlog.d("OnCreareView FGoodbye");
		try {
			if ((App.BannerName != null && App.BannerName.getBundle("END") == null) && !RequestBanner) {
				//controllo se ho il banner e se non ho già iniziato a scaricarlo.
				RequestBanner = true;
				new Thread(new Runnable() {
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
				}).start();
			}
		}catch(Exception e){
			dlog.e("Exception while updating banner end",e);
		}
		dlog.d("FGoodbye: onCreateView");
		//((AMainOBC) getActivity()).player.inizializePlayer();
		((AGoodbye) getActivity()).player.reqSystem = true;
		((AGoodbye) getActivity()).setAudioSystem(LowLevelInterface.AUDIO_SYSTEM);
		dlog.d("updateParkAreaStatus: Imposto Audio a AUDIO_SYSTEM");
		new Thread(new Runnable() {
			public void run() {
				try{
					((AGoodbye) getActivity()).player.waitToPlayFile(Uri.parse("android.resource://eu.philcar.csg.OBC/"+ R.raw.alert_tts_end));
				}catch(Exception e){
					dlog.e("Exception trying to play audio",e);
				}
			}
		}).start();

		Bundle b = this.getArguments();
		if (b==null || !b.containsKey("CLOSE")) {
			dlog.e("Started FGoodbye without CLOSE key, aborting");
			this.getActivity().finish();
			return null;
		}

		final View view = inflater.inflate(R.layout.f_goodbye, container, false);
				
		Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "interstateregular.ttf");
	
		(view.findViewById(R.id.llSelfClose)).setVisibility(View.INVISIBLE);

		
		((TextView)view.findViewById(R.id.fgodTopTV)).setTypeface(font);
		((TextView)view.findViewById(R.id.fgod_Goodbye_Title_TV)).setTypeface(font);
		((TextView)view.findViewById(R.id.fgodGoodbyeTV)).setTypeface(font);
		//webViewBanner = (WebView)view.findViewById(R.id.fgoodWV);
		adIV=(ImageView) view.findViewById(R.id.fgoodIV);
		timer_5sec = new CountDownTimer((5)*1000,1000) {
			@Override
			public void onTick(long millisUntilFinished) {

			}

			@Override
			public void onFinish() {
				new Thread(new Runnable() {
					@Override
					public void run() {
						loadBanner(App.URL_AdsBuilderEnd,"END",false);
						try {
							getActivity().runOnUiThread(new Runnable() {
								@Override
								public void run() {
									if (FGoodbye.this.isVisible()) {
										updateBanner("END");
									}
								}
							});
						}catch(Exception e){
							dlog.e("Exception during update UI ",e);
						}

					}
				}).start();
			}
		};
		adIV.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(App.BannerName.getBundle("END")!=null){
					try {
						if (App.BannerName.getBundle("END").getString("CLICK", "null").compareTo("null") != 0) {

							if (!handleClick) {
								adIV.setColorFilter(R.color.overlay_banner);
								handleClick = true;
								dlog.i(FDriveMessage.class.toString() + " Click su banner ");
								new Thread(new Runnable() {
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
								}).start();
							}
						}
					} catch (Exception e) {
						dlog.e("Exception during onclick", e);
					}
				}
			}
		});
		String name = "";
		if (App.currentTripInfo!=null && App.currentTripInfo.customer!=null) {
			name = " " + App.currentTripInfo.customer.name + " " + App.currentTripInfo.customer.surname;
		} else if (Debug.IGNORE_HARDWARE) {
			name = "Gino Panino";
		}
		((TextView)view.findViewById(R.id.fgodGoodbyeTV)).setText(name);
		
		((AGoodbye)this.getActivity()).sendMessage(MessageFactory.scheduleSelfCloseTrip(40));
		(view.findViewById(R.id.llSelfClose)).setVisibility(View.VISIBLE);
		final Activity activity =this.getActivity();
		
		selfclose = new CountDownTimer(41000,1000) {
			@Override
		     public void onTick(long millisUntilFinished) {
		    	 ((TextView)view.findViewById(R.id.tvCountdown)).setText((millisUntilFinished/1000)+ " s");
		     }

			@Override
			public void onFinish() {
				dlog.d(FGoodbye.class.toString()+" onFinish: finished countdown, ending activity");
				try {
					wait(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if(App.currentTripInfo!=null) {
					((AGoodbye) activity).sendMessage(MessageFactory.scheduleSelfCloseTrip(1));
				}
				localHandler.removeMessages(MSG_CLOSE_ACTIVITY);
				localHandler.sendEmptyMessageDelayed(MSG_CLOSE_ACTIVITY,1000);

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

		if(App.currentTripInfo.isBonusEnabled){
			(view.findViewById(R.id.fgodTopTV)).setVisibility(View.GONE);
			((TextView)view.findViewById(R.id.fgodInstructionTV)).setText(R.string.instruction_close_4);
			(view.findViewById(R.id.fgodBonusLL)).setVisibility(View.VISIBLE);
			((TextView)view.findViewById(R.id.fgodBonusTV)).setText(R.string.bonus_message_poi);
		}
		else{
			(view.findViewById(R.id.fgodTopTV)).setVisibility(View.VISIBLE);
			((TextView)view.findViewById(R.id.fgodInstructionTV)).setText(R.string.goodbye_instructions);
			((TextView)view.findViewById(R.id.fgodInstructionTV)).setTextSize(24f);
			((TextView)view.findViewById(R.id.fgodTopTV)).setTextSize(35f);
			(view.findViewById(R.id.fgodBonusLL)).setVisibility(View.GONE);

			((TextView)view.findViewById(R.id.fgodBonusTV)).setText("");

		}


		updateBanner("END");
		return view;
	}

	@Override
	public boolean handleBackButton() {
		
		return true;
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


	public void loadBanner(String Url, String type, Boolean isClick) {

		File outDir = new File(App.BANNER_IMAGES_FOLDER);
		if (outDir.mkdir()) {
			dlog.d("Banner directory created");
		}



		if (!App.hasNetworkConnection) {
			dlog.e(" loadBanner: nessuna connessione");
			App.BannerName.putBundle(type,null);//null per identificare nessuna connessione, caricare immagine offline
			return;
		}
		StringBuilder  builder = new StringBuilder();
		List<NameValuePair> paramsList = new ArrayList<>();
		HttpResponse response;
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
			if (App.BannerName!=null && App.BannerName.getBundle(type) != null )
				paramsList.add(new BasicNameValuePair("index", App.BannerName.getBundle(type).getString("INDEX",null)));

			if (App.BannerName!=null && App.BannerName.getBundle(type) != null )
				paramsList.add(new BasicNameValuePair("end", App.BannerName.getBundle(type).getString("END",null)));



			Url= UrlTools.buildQuery(Url.concat("?"),paramsList).toString();
			//connessione per scaricare id immagine

			HttpClient client = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(Url);

			response = client.execute(httpGet);
			DLog.D(" loadBanner: Url richiesta "+Url);
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
			} else {

				dlog.e(" loadBanner: Failed to connect "+String.valueOf(statusCode));
				App.BannerName.putBundle(type,null);//null per identificare nessuna connessione, caricare immagine offline
				return;
			}
		}catch (Exception e){
			dlog.e(" loadBanner: eccezione in connessione ",e);
			App.BannerName.putBundle(type,null);//null per identificare nessuna connessione, caricare immagine offline
			return;
		}
		String jsonStr = builder.toString();
		if(jsonStr.compareTo("")==0){
			dlog.e(" loadBanner: nessuna connessione");
			App.BannerName.putBundle(type,null);//null per identificare nessuna connessione, caricare immagine offline
			return;
		}

		DLog.D(" loadBanner: risposta "+jsonStr);
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

			App.BannerName.putBundle(type,Image);

			//ricavo nome file
			URL urlImg = new URL(Image.getString("URL"));
			String extension = urlImg.getFile().substring(urlImg.getFile().lastIndexOf('.') + 1);
			String filename = Image.getString("ID").concat(".").concat(extension);

			//download imagine se non esiste
			file = new File(outDir, filename);

			if(file.exists()){
				Image.putString(("FILENAME"),filename);
				App.BannerName.putBundle(type,Image);
				dlog.i(" loadBanner: file già esistente: "+filename);
				return;
			}


			dlog.i(" loadBanner: file mancante inizio download a url: "+urlImg.toString());
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
			Image.putString(("FILENAME"),filename);
			App.BannerName.putBundle(type,Image);
			dlog.d(" loadBanner: File scaricato e creato "+filename);


		} catch (Exception e) {
			if(file.exists()) file.delete();
			App.BannerName.putBundle(type,null);//null per identificare nessuna connessione, caricare immagine offline
			dlog.e(" loadBanner: eccezione in creazione e download file ",e);

			e.printStackTrace();
		}



	}

	private void updateBanner(String type){

		File ImageV;
		Bundle Banner = App.BannerName.getBundle(type);
		if(Banner!=null){
			ImageV=new File(App.BANNER_IMAGES_FOLDER,Banner.getString("FILENAME",null));

			try{
				if(ImageV!=null && ImageV.exists()){
					dlog.i(FGoodbye.class.toString()+" updateBanner: file trovato imposto immagine "+ImageV.getName());
					Bitmap myBitmap = BitmapFactory.decodeFile(ImageV.getAbsolutePath());
					if(myBitmap==null){

						dlog.e(FGoodbye.class.toString()+" updateBanner: file corrotto, elimino e visualizzo offline ");
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
					return;

				}
			}catch (Exception e){
				dlog.e(FGoodbye.class.toString()+" updateBanner: eccezione in caricamento file visualizzo offline ",e);
				e.printStackTrace();
				//initWebBanner(Banner.getString("URL",null));
				//webViewBanner.setVisibility(View.INVISIBLE);
				adIV.setImageResource(R.drawable.offline_goodbye);
				adIV.setVisibility(View.VISIBLE);
				return;
			}
		}
		else{
			dlog.e(FGoodbye.class.toString()+" updateBanner: Bundle null, visualizzo offline");
			//initWebBanner(Banner.getString("URL",null));
			//webViewBanner.setVisibility(View.INVISIBLE);
			adIV.setImageResource(R.drawable.offline_goodbye);
			adIV.setVisibility(View.VISIBLE);
			return;
		}

	}

	@Override
	public void onDestroy() {
		try{
		adIV=null;
		timer_5sec.cancel();
		handleClick=null;
		RequestBanner=null;
		if(selfclose!=null)
			selfclose.cancel();
		selfclose=null;
		}catch(Exception e){
			dlog.e("Exception while cleaning memory",e);
		}finally {

			super.onDestroy();
		}
	}
}
