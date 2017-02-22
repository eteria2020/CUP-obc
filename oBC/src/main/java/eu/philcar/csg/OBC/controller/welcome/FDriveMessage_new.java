package eu.philcar.csg.OBC.controller.welcome;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.NetworkOnMainThreadException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import eu.philcar.csg.OBC.AMainOBC;
import eu.philcar.csg.OBC.ASOS;
import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.controller.FBase;
import eu.philcar.csg.OBC.helpers.BannerJsInterface;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.UrlTools;
import okhttp3.HttpUrl;

public class FDriveMessage_new extends FBase {
	
	private DLog dlog = new DLog(this.getClass());

	public static FDriveMessage_new newInstance(boolean login) {
		
		FDriveMessage_new fi = new FDriveMessage_new();
		
		fi.login = login;
		
		return fi;
	}


	private final static int  MSG_CLOSE_FRAGMENT  = 1;
	private boolean login;
	//private WebView webViewBanner;
	private static Boolean handleClick=false;
	private static CountDownTimer timer_5sec;

	private ArrayList<Bundle> startImages = new ArrayList<Bundle>();


    private ImageView adIV;
	private static Boolean RequestBanner=false;

	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		/*if(App.first_UP_Start){
			App.first_UP_Start=false;

			new Thread(new Runnable() {
				public void run() {
					updatePoiIcon(App.URL_UpdateStartImages);
				}
			}).start();
		}
		if((new Date().getTime()-App.update_StartImages.getTime())>3600000) {    //3600000 = 1 ora


			new Thread(new Runnable() {
				public void run() {
					updatePoiIcon(App.URL_UpdateStartImages);
				}
			}).start();
		}*/

	}

	private Handler localHandler = new Handler()  {

		@Override
		public void handleMessage(Message msg) {

			switch (msg.what)  {


				case MSG_CLOSE_FRAGMENT:
					try {
						dlog.d("FDriveMessage selfclose : " + login);
						Intent i = new Intent(getActivity(), AMainOBC.class);
						i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						startActivity(i);
						getActivity().finish();
					}catch(Exception e){
						dlog.e("localHandler: MSG_CLOSE_FRAGMENT Exception",e);
					}
					break;
			}
		}
	};



	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		System.gc();
		
		final View view = inflater.inflate(R.layout.f_lanes_new, container, false);
		dlog.d("OnCreareView FDriveMessage_new");
		if(App.Instance.BannerName.getBundle("START")==null&&!RequestBanner){
			//controllo se ho il banner e se non ho già iniziato a scaricarlo.
			RequestBanner=true;
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						loadBanner(App.URL_AdsBuilderStart, "START", false);        //scarico banner
						getActivity().runOnUiThread(new Runnable() {
							@Override
							public void run() {
								if (FDriveMessage_new.this.isVisible()) {

									updateBanner("START");                            //Modifico l'IV
								}
							}
						});
					}catch(Exception e){
						dlog.e("Eccezione durante caricamento banner iniziale");
					}
				}
			}).start();
		}

		((View)view.findViewById(R.id.btnNext)).setVisibility(View.INVISIBLE);
		((View)view.findViewById(R.id.tvCountdown)).setVisibility(View.VISIBLE);
		
		((ImageButton)view.findViewById(R.id.btnNext)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dlog.d("FDriveMessage btnNext click : " + login);
					Intent i = new Intent(getActivity(), AMainOBC.class);
					i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(i);
					getActivity().finish();

			}
		});
		
		((Button)view.findViewById(R.id.btnSOS)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(getActivity(), ASOS.class));
			}
		});
		
		Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "interstateregular.ttf");

		//webViewBanner=(WebView) view.findViewById(R.id.bannerfsWV);



		timer_5sec = new CountDownTimer((5)*1000,1000) {
			@Override
			public void onTick(long millisUntilFinished) {

			}

			@Override
			public void onFinish() {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							loadBanner(App.URL_AdsBuilderCar, "START", false);
							getActivity().runOnUiThread(new Runnable() {
								@Override
								public void run() {
									if (FDriveMessage_new.this.isVisible()) {

										updateBanner("START");
									}
								}
							});
						}catch(Exception e){
							dlog.e("Exception during loadbanner:",e);
						}

					}
				}).start();
			}
		};

        adIV = (ImageView)view.findViewById(R.id.imgfsIV);

		adIV.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(App.Instance.BannerName.getBundle("START")!=null){
					if(App.Instance.BannerName.getBundle("START").getString("CLICK").compareTo("null")!=0){

						if(!handleClick) {
							adIV.setColorFilter(R.color.overlay_banner);
							handleClick=true;
							dlog.i(FDriveMessage.class.toString()+" Click su banner ");
							new Thread(new Runnable() {
								@Override
								public void run() {
									try {
										loadBanner(App.BannerName.getBundle("START").getString("CLICK"), "START", true);
										getActivity().runOnUiThread(new Runnable() {
											@Override
											public void run() {
												if (FDriveMessage_new.this.isVisible()) {
													adIV.clearColorFilter();
													updateBanner("START");
													timer_5sec.start();//Modifico l'IV
													handleClick = false;
												}
											}
										});
									}catch(Exception e){
										dlog.e("Exception during start loadbanner:",e);
									}

								}
							}).start();
						}
					}
				}
			}
		});



		new CountDownTimer(4000,1000) {
				@Override
			     public void onTick(long millisUntilFinished) {
			    	 ((TextView)view.findViewById(R.id.tvCountdown)).setText((millisUntilFinished/1000)+ " s");
			     }

				@Override
				public void onFinish() {
					((View)view.findViewById(R.id.tvCountdown)).setVisibility(View.INVISIBLE);
					((View)view.findViewById(R.id.btnNext)).setVisibility(View.VISIBLE);
					localHandler.sendEmptyMessageDelayed(MSG_CLOSE_FRAGMENT,10000);

				}

		}.start();

		if (App.currentTripInfo!= null && App.currentTripInfo.isMaintenance) {
			view.findViewById(R.id.fchn_right_FL).setBackgroundColor(getResources().getColor(R.color.background_red));

		} else {
			view.findViewById(R.id.fchn_right_FL).setBackgroundColor(getResources().getColor(R.color.background_green));
		}
		updateBanner("START");
		return view;
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
		localHandler.removeMessages(MSG_CLOSE_FRAGMENT);
	}

    /*private void  initWebViewBanner() {

		File outDir = new File(App.BANNER_IMAGES_FOLDER);
		if (!outDir.isDirectory()) {
			outDir.mkdir();
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
				adIV.setVisibility(View.VISIBLE);
				dlog.d("WebBanner: disabled");
			}

		});
		webViewBanner.addJavascriptInterface(new BannerJsInterface(), "OBC");


		String Url;

        if (!App.hasNetworkConnection) {
            dlog.e("initWebBanner: webView==null or no connection);");
            adIV.setVisibility(View.VISIBLE);
            return;
        }

		List<NameValuePair> paramsList =  new ArrayList<NameValuePair>();

		if (App.currentTripInfo!=null && App.currentTripInfo.customer!=null)
			paramsList.add(new BasicNameValuePair("id", App.currentTripInfo.customer.id+""));

		if (App.lastLocation!=null) {
			paramsList.add(new BasicNameValuePair("lat", App.lastLocation.getLatitude()+""));
			paramsList.add(new BasicNameValuePair("lon", App.lastLocation.getLongitude()+""));
		}
		paramsList.add(new BasicNameValuePair("id_fleet", App.FleetId+""));
		paramsList.add(new BasicNameValuePair("carplate", App.CarPlate));

		final List<NameValuePair> FinalparamsList = paramsList;


		Url=UrlTools.buildQuery(App.URL_AdsBuilderStart.concat("?"),paramsList).toString();
  		//connessione per scaricare id immagine
		StringBuilder builder = new StringBuilder();
		HttpClient client = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(Url);
		try {
			HttpResponse response = client.execute(httpGet);
			StatusLine statusLine = response.getStatusLine();
			int statusCode = statusLine.getStatusCode();
			if (statusCode == 200) {
				App.update_StartImages = new Date();
				HttpEntity entity = response.getEntity();
				InputStream content = entity.getContent();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(content));
				String line;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}
			} else {

				Log.e(FDriveMessage_new.class.toString(), "Failed to download file");
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (NetworkOnMainThreadException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String jsonStr = builder.toString();

		try {
			JSONObject json = new JSONObject(jsonStr);
			Log.i(FDriveMessage_new.class.getName(), "creazione oggetto Json");
			//Get the instance of JSONArray that contains JSONObjects
			JSONArray jsonArray = json.optJSONArray("Image");

			//Iterate the jsonArray and print the info of JSONObjects

				Bundle Image = new Bundle();
				JSONObject jsonObject = jsonArray.getJSONObject(0);

				Image.putString("ID",jsonObject.getString("ID"));
				Image.putString("URL",jsonObject.getString("URL"));
				//ricavo nome file
				URL urlImg = new URL(Image.getString("URL"));
				String extension = urlImg.getFile().substring(urlImg.getFile().lastIndexOf('.') + 1);
				String filename = Image.getString("ID").concat(".").concat(extension);

				final File ImageV =new File(outDir,filename);



			(getActivity()).runOnUiThread(new Runnable() {

				@Override
				public void run() {
					// your stuff to update the UI
					try{
						if(ImageV.exists()){
							Bitmap myBitmap = BitmapFactory.decodeFile(ImageV.getAbsolutePath());
							webViewBanner.setVisibility(View.INVISIBLE);
							adIV.setImageBitmap(myBitmap);
							adIV.setVisibility(View.VISIBLE);
							adIV.invalidate();

						}
						else {
							HttpUrl url = UrlTools.buildQuery(App.URL_AdsBuilder, FinalparamsList);
							if (url!=null) {
								String  strUrl =  url.toString();

								webViewBanner.setVisibility(View.VISIBLE);
								webViewBanner.loadUrl(strUrl);
								adIV.setVisibility(View.INVISIBLE);
								dlog.d("WebBanner enabled  : " + strUrl);
							}
						}

					}
					catch (Exception e){

					}
				}
			});


		} catch (Exception e) {
			e.printStackTrace();
		}



    }*/


	private void updatePoiIcon(String Url) {

		String jsonStr= "";
		if(startImages!=null)
			startImages.clear();

		File outDir = new File(App.BANNER_IMAGES_FOLDER);
		if (!outDir.isDirectory()) {
			outDir.mkdir();
		}

		Url=Url.concat("?ID=").concat(maxIconID(App.BANNER_IMAGES_FOLDER));
		StringBuilder builder = new StringBuilder();
		HttpClient client = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(Url);
		try {
			HttpResponse response = client.execute(httpGet);
			StatusLine statusLine = response.getStatusLine();
			int statusCode = statusLine.getStatusCode();
			if (statusCode == 200) {
				App.update_StartImages = new Date();
				HttpEntity entity = response.getEntity();
				InputStream content = entity.getContent();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(content));
				String line;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}
			} else {

				Log.e(FDriveMessage_new.class.toString(), "Failed to download file");
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (NetworkOnMainThreadException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		jsonStr=builder.toString();
		parseJsonIcon(jsonStr);

		for(Bundle Image:startImages) {    //-- start loop download and save image
			try {
				URL url = new URL(Image.getString("URL"));
				String extension = url.getFile().substring(url.getFile().lastIndexOf('.') + 1);
				String filename = Image.getString("ID").concat(".").concat(extension);
				Log.i("Local filename:", "" + filename);
				File file = new File(outDir, filename);

				if(file.exists())
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
				int totalSize = urlConnection.getContentLength();
				int downloadedSize = 0;
				byte[] buffer = new byte[1024];
				int bufferLength = 0;
				while ((bufferLength = inputStream.read(buffer)) > 0) {
					fileOutput.write(buffer, 0, bufferLength);
					downloadedSize += bufferLength;
					Log.i("Progress:", "downloadedSize:" + downloadedSize + "totalSize:" + totalSize);
				}
				fileOutput.close();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		//return filepath;


		return ;

	}

	private void parseJsonIcon(String jsonStr) {



		try {
			JSONObject json = new JSONObject(jsonStr);
			Log.i(FDriveMessage_new.class.getName(), "creazione oggetto Json");
			//Get the instance of JSONArray that contains JSONObjects
			JSONArray jsonArray = json.optJSONArray("Image");

			//Iterate the jsonArray and print the info of JSONObjects
			for(int k=0;k<jsonArray.length();k++) {
				Bundle Image = new Bundle();
				JSONObject jsonObject = jsonArray.getJSONObject(k);

				Image.putString("ID",jsonObject.getString("ID"));
				Image.putString("URL",jsonObject.getString("URL"));
				startImages.add(Image);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

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

	public void loadBanner(String Url, String type, Boolean isClick) {

		File outDir = new File(App.BANNER_IMAGES_FOLDER);
		if (!outDir.isDirectory()) {
			outDir.mkdir();
		}



		if (!App.hasNetworkConnection) {
			dlog.e(" loadBanner: nessuna connessione");
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
				App.Instance.BannerName.putBundle(type,null);//null per identificare nessuna connessione, caricare immagine offline
				return;
			}
		}catch (Exception e){
			dlog.e(" loadBanner: eccezione in connessione ",e);
			App.Instance.BannerName.putBundle(type,null);//null per identificare nessuna connessione, caricare immagine offline
			return;
		}
		String jsonStr = builder.toString();
		if(jsonStr.compareTo("")==0){
			dlog.e(" loadBanner: nessuna connessione");
			App.Instance.BannerName.putBundle(type,null);//null per identificare nessuna connessione, caricare immagine offline
			return;
		}

		DLog.D(" loadBanner: risposta "+jsonStr);
		File file = new File(outDir, "placeholder.lol");;

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
			App.Instance.BannerName.putBundle(type,Image);
			dlog.d(" loadBanner: File scaricato e creato "+filename);


		} catch (Exception e) {
			if(file.exists()) file.delete();
			App.Instance.BannerName.putBundle(type,null);//null per identificare nessuna connessione, caricare immagine offline
			dlog.e(" loadBanner: eccezione in creazione e download file ",e);

			e.printStackTrace();
		}



	}

	private void updateBanner(String type){

		File ImageV;
		Bundle Banner = App.Instance.BannerName.getBundle(type);
		if(Banner!=null){
			File outDir = new File(App.BANNER_IMAGES_FOLDER);
			if (!outDir.isDirectory()) {
				dlog.e(FDriveMessage_new.class.toString()+" updateBanner: la cartella non esiste visualizzo offline ");

				//webViewBanner.setVisibility(View.INVISIBLE);
				adIV.setImageResource(R.drawable.offline_welcome);
				adIV.setVisibility(View.VISIBLE);
				return;
			}
			ImageV=new File(outDir,Banner.getString("FILENAME",null));

			try{
				if(ImageV!=null && ImageV.exists()){
					dlog.i(FDriveMessage_new.class.toString()+" updateBanner: file trovato imposto immagine "+Banner.getString("FILENAME","ERRORE CONTROLLARE"));
					Bitmap myBitmap = BitmapFactory.decodeFile(ImageV.getAbsolutePath());
					if(myBitmap==null){

						dlog.e(FDriveMessage_new.class.toString()+" updateBanner: file corrotto, elimino e visualizzo offline ");
						ImageV.delete();
						//initWebBanner(Banner.getString("URL",null));
						//webViewBanner.setVisibility(View.INVISIBLE);
						adIV.setImageResource(R.drawable.offline_welcome);
						adIV.setVisibility(View.VISIBLE);
						return;
					}
					//webViewBanner.setVisibility(View.INVISIBLE);
					adIV.setImageBitmap(myBitmap);
					adIV.setVisibility(View.VISIBLE);
					adIV.invalidate();
					return;

				}
				dlog.e(FDriveMessage_new.class.toString()+" updateBanner: file non esiste visualizzo offline ");
				//webViewBanner.setVisibility(View.INVISIBLE);
				adIV.setImageResource(R.drawable.offline_welcome);
				adIV.setVisibility(View.VISIBLE);
			}catch (Exception e){
				dlog.e(FDriveMessage_new.class.toString()+" updateBanner: eccezione in caricamento file visualizzo offline ",e);
				e.printStackTrace();
				//initWebBanner(Banner.getString("URL",null));
				//webViewBanner.setVisibility(View.INVISIBLE);
				adIV.setImageResource(R.drawable.offline_welcome);
				adIV.setVisibility(View.VISIBLE);
				return;
			}
		}
		else{
			dlog.e(FDriveMessage_new.class.toString()+" updateBanner: Bundle null, visualizzo offline");
			//initWebBanner(Banner.getString("URL",null));
			//webViewBanner.setVisibility(View.INVISIBLE);
			adIV.setImageResource(R.drawable.offline_welcome);
			adIV.setVisibility(View.VISIBLE);
			return;
		}

	}

	@Override
	public void onDestroy() {
		adIV=null;
		startImages=null;
		super.onDestroy();
	}
}
