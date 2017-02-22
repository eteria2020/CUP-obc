package eu.philcar.csg.OBC.controller.map;

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

import eu.philcar.csg.OBC.ABase;
import eu.philcar.csg.OBC.ASOS;
import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.AMainOBC;
import eu.philcar.csg.OBC.SystemControl;
import eu.philcar.csg.OBC.controller.FBase;
import eu.philcar.csg.OBC.db.Events;
import eu.philcar.csg.OBC.db.Poi;
import eu.philcar.csg.OBC.devices.LowLevelInterface;
import eu.philcar.csg.OBC.helpers.BannerJsInterface;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.UrlTools;
import eu.philcar.csg.OBC.service.CarInfo;
import okhttp3.HttpUrl;

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
import android.os.NetworkOnMainThreadException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

@SuppressLint("SimpleDateFormat")
public class FHome extends FBase implements OnClickListener {

    private DLog dlog = new DLog(this.getClass());

    public static FHome newInstance() {
        FHome fm = new FHome();
        return fm;
    }


    private View rootView;


    private TextView  no3gtxt, alertTV;
    private View no3gwarning;
    private RelativeLayout rlBody;
    private LinearLayout fmap_top_LL;
    public static FHome Instance;
    private Animation alertAnimation;
    private static boolean animToggle=true, animFull=false;
    private TextView dayTV, timeTV, tvRange;
    private FrameLayout  fmapAlarm, fmapRange;
    private ImageView parkingStatusIV, adIV, no3gIV; // parkingDirectionIV,
   // private View  outsideAreaWarning;

    private List<String> animQueue =new ArrayList<String>();
    private Uri uri;
    private int  lastInside=0;//0:no anim 1:anim3g 2:animArea 3:animBoth
    private static Boolean RequestBanner=false;

    private static Boolean handleClick=false;


    public FHome() {
        Instance = this;

    }



    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().registerReceiver(this.ConnectivityChangeReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));


        App.isCloseable = false;
    }


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

        uri = Uri.parse("android.resource://eu.philcar.csg.OBC/"+R.raw.out_operative_area_tts);

        ((Button) view.findViewById(R.id.fmapSOSB)).setOnClickListener(this);
        ((Button) view.findViewById(R.id.fmapSearchB)).setOnClickListener(this);
        ((Button) view.findViewById(R.id.fmapRadioB)).setOnClickListener(this);
        //((Button) view.findViewById(R.id.fmapFuelStationsB)).setOnClickListener(this); rimosso su richiesta mkt
        ((Button) view.findViewById(R.id.fmapCancelB)).setOnClickListener(this);

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
                        no3gwarning.setBackgroundResource(R.drawable.sha_whiteroundedyellowbox);
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
                            no3gwarning.setBackgroundResource(R.drawable.sha_whiteroundedyellowbox);
                            animToggle = !animToggle;
                            break;




                    }

                    animFull=!animFull;
                }
                else
                    animFull=!animFull;
            }

        });



       // outsideAreaWarning.setVisibility(View.INVISIBLE); //TODO rimuovere commento e llOutsideArea riga 286
        no3gwarning.setVisibility(View.INVISIBLE);

        fmapAlarm.setVisibility(View.GONE);
        fmapRange.setVisibility(View.GONE);

        adIV = (ImageView) view.findViewById(R.id.fmapLeftBorderIV);
        adIV.setOnClickListener(this);
        //webViewBanner = (WebView) view.findViewById(R.id.WebViewBanner);

        AMainOBC activity = (AMainOBC)getActivity();
        if (activity != null) {
            updateParkAreaStatus(activity.isInsideParkArea(), activity.getRotationToParkAngle());
        }
    

        //mapView.getModel().displayModel.setFixedTileSize(256);

        ((AMainOBC) getActivity()).setFragmentHandler(localHandler);

        if (App.currentTripInfo!=null && App.currentTripInfo.isMaintenance) {
            fmap_top_LL.setBackgroundColor(getResources().getColor(R.color.background_red));
            rlBody.setBackgroundColor(getResources().getColor(R.color.background_red));

        } else {
            fmap_top_LL.setBackgroundColor(getResources().getColor(R.color.background_green));
            rlBody.setBackgroundColor(getResources().getColor(R.color.background_green));
        }

        //timer per aggiornamento advertisment

        updateBanner("CAR");

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        //initWebViewBanner();

    }

    public void onPause() {
        super.onPause();
        //actualAnim=0;
        lastInside=1;
        getActivity().unregisterReceiver(this.ConnectivityChangeReceiver);


    }

    @Override
    public void onDestroy() {
        ((Button) rootView.findViewById(R.id.fmapSOSB)).setOnClickListener(null);
        ((Button) rootView.findViewById(R.id.fmapSearchB)).setOnClickListener(null);
        ((Button) rootView.findViewById(R.id.fmapRadioB)).setOnClickListener(null);
        ((Button) rootView.findViewById(R.id.fmapCancelB)).setOnClickListener(null);
        rootView=null;
        tvRange=null;
        fmapRange=null;
        fmap_top_LL=null;
        rlBody=null;
        adIV=null;
        alertAnimation.cancel();
        fmapAlarm=null;
        timeTV=null;
        dayTV=null;
        no3gwarning=null;
        no3gIV=null;
        no3gtxt=null;
        parkingStatusIV=null;
        alertTV=null;
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();


    }


    @Override
    public void onClick(View v) {

        switch (v.getId()) {


            case R.id.fmapSOSB:
                startActivity(new Intent(getActivity(), ASOS.class));
                break;

            case R.id.fmapSearchB:
                ((ABase) getActivity()).pushBackFragment(FMap.newInstance(), FMap.class.getName(), true);

                break;

            case R.id.fmapRadioB:
                ((ABase) getActivity()).pushFragment(FRadio.newInstance(), FRadio.class.getName(), true);
                break;

            case R.id.fmapCancelB:
                ((ABase) getActivity()).pushFragment(FMenu.newInstance(), FMenu.class.getName(), true);
                break;




            case R.id.fmapNavigationB:


                break;
            case R.id.fmapLeftBorderIV:
                if(App.Instance.BannerName.getBundle("CAR")!=null){
                    if(App.Instance.BannerName.getBundle("CAR").getString("CLICK").compareTo("null")!=0){

                        if(!handleClick) {
                            adIV.setColorFilter(R.color.overlay_banner);
                            FMap.timer_2min.cancel();
                            handleClick=true;
                            dlog.d(" Banner: Click su banner ");
                            new Thread(new Runnable() {
                                @Override
                                public void run() {

                                    loadBanner(App.BannerName.getBundle("CAR").getString("CLICK"), "CAR", true);
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if(FHome.this.isVisible()) {
                                                adIV.clearColorFilter();
                                                updateBanner("CAR");
                                                FMap.timer_2min.cancel();
                                                FMap.timer_5sec.start();//Modifico l'IV
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


    public void updateParkAreaStatus(boolean isInside, float rotationAngle) {

        //isInside = false;
        //rotationAngle =0;

        if (isInside) {
            if(animQueue.contains("area")){
                animQueue.remove("area");
            }
            if(lastInside!=0)
                Events.outOfArea(false);
            lastInside=0;

        } else {
            if(!animQueue.contains("area")){
                animQueue.add("area");
            }
            if(no3gwarning.getAnimation()==null)
                no3gwarning.startAnimation(alertAnimation);



            if (lastInside==0) {
                lastInside=2;


                //((AMainOBC) getActivity()).player.inizializePlayer();
                AMainOBC.player.reqSystem = true;
                ((AMainOBC) getActivity()).setAudioSystem(LowLevelInterface.AUDIO_SYSTEM);
                dlog.d("updateParkAreaStatus: Imposto Audio a AUDIO_SYSTEM");
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            Events.outOfArea(true);
                            AMainOBC.player.waitToPlayFile(uri);
                        } catch (Exception e) {
                            dlog.e("Exception trying to play audio", e);
                        }
                    }
                }).start();
            }
            else{
                if(lastInside==1){
                    lastInside=2;


                }
            }



        }


        //parkingDirectionIV.setRotation((float)rotationAngle);
    }

    public void updateCarInfo(CarInfo carInfo) {

        if (carInfo == null)
            return;


        int SOC = carInfo.batteryLevel;

        // if Soc==0 means that software has just started, or it is a demo kit or we lost some connection
        // don't show anything for now
        if (SOC == 0) {
            fmapAlarm.setVisibility(View.VISIBLE);
            fmapRange.setVisibility(View.GONE);
            return;
        }


        if (SOC <= 20) {
            fmapAlarm.setVisibility(View.VISIBLE);
            fmapRange.setVisibility(View.GONE);
        } else {
            fmapAlarm.setVisibility(View.GONE);
            fmapRange.setVisibility(View.VISIBLE);
            tvRange.setText(carInfo.rangeKm + " Km");
        }


    }

    public void updatePoiInfo(int status,Poi Poi){

        if(status>0){
            if(!animQueue.contains("bonus")){
                if(App.DefaultCity.toLowerCase().equals("milano")) {
                    AMainOBC.player.reqSystem = true;

                }
                animQueue.add("bonus");
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


            }
        }
    };


    private final BroadcastReceiver ConnectivityChangeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context c, Intent i) {

            try {
                if (getActivity() == null)
                    return;
                boolean status = SystemControl.hasNetworkConnection(getActivity());

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
            }catch(Exception e){
                dlog.e("Exception while handling connectivity broadcast",e);
            }
        }


    };

    private void loadBanner(String Url, String type, Boolean isClick) {

        File outDir = new File(App.BANNER_IMAGES_FOLDER);
        if (!outDir.isDirectory()) {
            outDir.mkdir();
        }



        if (!App.hasNetworkConnection) {
            dlog.e(FHome.class.toString()+" loadBanner: nessuna connessione");
            App.Instance.BannerName.putBundle(type,null);//null per identificare nessuna connessione, caricare immagine offline
            return;
        }
        StringBuilder  builder = new StringBuilder();
        List<NameValuePair> paramsList = new ArrayList<NameValuePair>();
        if(!isClick) {


            if (App.currentTripInfo != null && App.currentTripInfo.customer != null)
                paramsList.add(new BasicNameValuePair("id",App.currentTripInfo.customer.id + ""));// App.currentTripInfo.customer.id + "")); //"3"));

            if (App.lastLocation != null) {
                paramsList.add(new BasicNameValuePair("lat", App.lastLocation.getLatitude() + ""));
                paramsList.add(new BasicNameValuePair("lon", App.lastLocation.getLongitude() + ""));
            }
            paramsList.add(new BasicNameValuePair("id_fleet", App.FleetId + ""));
            paramsList.add(new BasicNameValuePair("carplate", App.CarPlate));
        }
        try {
            if (App.BannerName.getBundle(type) != null )
                paramsList.add(new BasicNameValuePair("index", App.Instance.BannerName.getBundle(type).getString("INDEX",null)));



            Url= UrlTools.buildQuery(Url.concat("?"),paramsList).toString();
            //connessione per scaricare id immagine

            HttpClient client = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(Url);

            HttpResponse response = client.execute(httpGet);
            DLog.D(FHome.class.toString()+" loadBanner: Url richiesta "+Url);
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

        DLog.D(FHome.class.toString()+" loadBanner: risposta "+jsonStr);
        File file = new File(outDir, "placeholder.lol");;

        try {
            JSONObject json = new JSONObject(jsonStr);

            //Get the instance of JSONArray that contains JSONObjects
            JSONArray jsonArray = json.optJSONArray("Image");

            //Iterate the jsonArray and print the info of JSONObjects

            Bundle Image = new Bundle();
            JSONObject jsonObject = jsonArray.getJSONObject(0);

            Image.putString("ID",jsonObject.getString("ID"));
            Image.putString("URL",jsonObject.getString("URL"));
            Image.putString(("CLICK"),jsonObject.getString("CLICK"));
            Image.putString(("INDEX"),jsonObject.getString("INDEX"));

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
                dlog.i(FHome.class.toString()+" loadBanner: file già esistente: "+filename);
                return;
            }


            dlog.i(FHome.class.toString()+" loadBanner: file mancante inizio download a url: "+urlImg.toString());
            HttpURLConnection urlConnection = (HttpURLConnection) urlImg.openConnection();
            urlConnection.setDefaultUseCaches(false);
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
            Image.putString(("FILENAME"),filename);
            App.Instance.BannerName.putBundle(type,Image);
            dlog.i(FHome.class.toString()+" loadBanner: File scaricato e creato "+filename);


        } catch (Exception e) {
            if(file.exists()) file.delete();
            dlog.e(FHome.class.toString()+" loadBanner: eccezione in creazione e download file ",e);

            e.printStackTrace();
        }



    }

    public void updateBanner(String type){

        File ImageV;
        Bundle Banner = App.Instance.BannerName.getBundle(type);
        if(Banner!=null){
            ImageV=new File(App.BANNER_IMAGES_FOLDER,Banner.getString("FILENAME",null));

            try{
                if(ImageV!=null && ImageV.exists()){
                    dlog.i(FHome.class.toString()+" updateBanner: file trovato imposto immagine "+ImageV.getName());
                    Bitmap myBitmap = BitmapFactory.decodeFile(ImageV.getAbsolutePath());
                    if(myBitmap==null){

                        dlog.e(FHome.class.toString()+" updateBanner: file corrotto, elimino e visualizzo offline ");
                        ImageV.delete();
                        //initWebBanner(Banner.getString("URL",null));
                       // webViewBanner.setVisibility(View.INVISIBLE);
                        adIV.setImageResource(R.drawable.car_banner_offline);
                        adIV.setVisibility(View.VISIBLE);
                        if(!FMap.started){
                            FMap.started= !FMap.started;
                            FMap.timer_2min.cancel();
                            FMap.timer_2min.start();
                        }
                        return;
                    }
                    //webViewBanner.setVisibility(View.INVISIBLE);
                    adIV.setImageBitmap(myBitmap);
                    adIV.setVisibility(View.VISIBLE);
                    adIV.invalidate();

                }
            }catch (Exception e){
                dlog.e(FHome.class.toString()+" updateBanner: eccezione in caricamento file visualizzo offline ",e);
                e.printStackTrace();
                //initWebBanner(Banner.getString("URL",null));
               // webViewBanner.setVisibility(View.INVISIBLE);
                adIV.setImageResource(R.drawable.car_banner_offline);
                adIV.setVisibility(View.VISIBLE);
            }
        }
        else{
            dlog.e(FHome.class.toString()+" updateBanner: Bundle null, visualizzo offline");
            //initWebBanner(Banner.getString("URL",null));
            //webViewBanner.setVisibility(View.INVISIBLE);
            adIV.setImageResource(R.drawable.car_banner_offline);
            adIV.setVisibility(View.VISIBLE);
        }

        try {
            if (!FMap.started && FMap.timer_2min!=null) {
                FMap.started = !FMap.started;
                FMap.timer_2min.cancel();
                FMap.timer_2min.start();
            }
        }catch(Exception e){
            dlog.e("Exception tryng to start timer",e);
        }

    }

}
