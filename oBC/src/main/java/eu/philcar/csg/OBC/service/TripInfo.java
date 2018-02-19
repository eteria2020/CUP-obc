package eu.philcar.csg.OBC.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.j256.ormlite.stmt.UpdateBuilder;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.controller.map.FRadio;
import eu.philcar.csg.OBC.data.datasources.repositories.SharengoPhpRepository;
import eu.philcar.csg.OBC.db.BusinessEmployee;
import eu.philcar.csg.OBC.db.BusinessEmployees;
import eu.philcar.csg.OBC.db.Customer;
import eu.philcar.csg.OBC.db.Customers;
import eu.philcar.csg.OBC.db.Trip;
import eu.philcar.csg.OBC.db.Trips;
import eu.philcar.csg.OBC.db.DbManager;
import eu.philcar.csg.OBC.db.Events;
import eu.philcar.csg.OBC.devices.LowLevelInterface;
import eu.philcar.csg.OBC.helpers.CardRfid;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.UrlTools;
import eu.philcar.csg.OBC.server.TripsConnector;
import eu.philcar.csg.OBC.server.HttpConnector;
import eu.philcar.csg.OBC.server.ReservationConnector;
import eu.philcar.csg.OBC.task.OdoController;
import eu.philcar.csg.OBC.task.OptimizeDistanceCalc;

import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.os.PowerManager.WakeLock;
import android.widget.Toast;

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

import javax.inject.Inject;

/**
 * This Class represents all information of a Trip
 * 
 * @author massimo.belluz@bulweria.com
 */
public class TripInfo {

    public enum CloseType{
        normal,
        forced,
        maintainer
    }

    private static final String DEFAULT_PLATE = "XH123KM";

    @Inject
    SharengoPhpRepository repositoryPhp;

    // Properties
    public Customer customer;
    public Trip trip;
    public String cardCode;
    public Date openEventTime;
    public long openEventTimestamp;
    public static Boolean processing=false;

    // Server Data
    public int serverResult;
    public String serverMessage;

    // Status
    public boolean isOpen;
    public boolean isBonusEnabled=false;
    public boolean isMaintenance;
    public boolean hasBeenStopped = false;
    public boolean reopenSuspend = false;

    // Standard Log Object
    private DLog  dlog = new DLog(TripInfo.class);

    private final Context mContext;

    public TripInfo(Context context) {
        mContext = context;
        App.get(mContext).getComponent().inject(this);

    }

    /**
     * Class Initializer
     * 
     * @throws SQLException
     * @throws Exception
     */



    public void init() {
        // Getting data from DB
        DbManager dbm = App.Instance.dbManager;
        Customers customers = dbm.getClientiDao();

        // Extracting data
        Trips trips = dbm.getCorseDao();
        Trip lastOpenTrip = trips.getLastOpenTrip();

        dlog.d("Loading from db: ");

        if (lastOpenTrip == null) {
            dlog.d("No records open. Null fields");
            this.trip = null;
        } else {
            dlog.d("One open trip. Loading....");
            this.trip = lastOpenTrip;

            try {
                customer = customers.queryForId(trip.id_customer);
                customer.decrypt();
            } catch (SQLException e){
                dlog.e("Error retriving customer:",e);
            } catch (Exception e) {
                dlog.e("Unknown Exception on retriving customer:",e);
            }
        }

        if (trip != null) {
            try {
                dlog.d("Loading: " + trip.toString());
                customer = customers.queryForId(trip.id_customer);
                customer.decrypt();
                isOpen = true;
                cardCode = customer.card_code;
                OptimizeDistanceCalc.init(); //change to retrive data from sharedPreferences
            } catch (SQLException e) {
                dlog.e("Error retriving customer:",e);
            } catch (Exception e) {
                dlog.e("Uknown Exception on retriving customer:",e);
            }
        } else {
            customer = null;
            cardCode = null;
            isOpen = false;
        }
    }


    public Message handleCard(String code, String event,CarInfo carInfo,LowLevelInterface obc_io,ObcService service, WakeLock screenLock) {
        return handleCard( code,  event, carInfo, obc_io, service,  screenLock, CloseType.normal) ;
    }

    /**
     * Manage card action.
     * TODO Refactor extracting this method to a specific Class.
     *
     * @param code
     * @param event
     * @param carInfo
     * @param obc_io
     * @param service
     * @param screenLock
     * @param closeType
     * @return
     * 
     * TODO @throws StandardPlateException The vehicle plate is the default vehicle plate.
     */
    public Message handleCard(
            String code,
            String event,
            CarInfo carInfo,
            LowLevelInterface obc_io,
            ObcService service,
            WakeLock screenLock,
            CloseType closeType ) {

        if (code==null) {
            dlog.e(TripInfo.class.toString()+" handleCard: Handle card - null code :"+event);
            return null;
        }

        if (event==null) {
            dlog.e(TripInfo.class.toString()+" handleCard: Handle card - null event :"+event);
            return null;
        }
        //Check if open only card
        if(App.openDoorsCards!=null){
            CardRfid card = (CardRfid) App.openDoorsCards.find(new CardRfid(code,""));
            if(card!=null && !isOpen){
                dlog.d("Passaggio card doorsOnly apertura porte in corso! id card: "+card.toString());
                obc_io.setDoors(null, 1,"Porte Aperte");  //Sole se trip registrata su db apri le portiere
                Events.eventRfid(6, code + " "+ card.getName());
                Events.eventCleanliness(0, 0);
                return null;
            }
        }


        /**
         * Prevent any operation on vehicle with the default standard plate
         * TODO @throws StandardPlateException
         */
        if (App.CarPlate.equalsIgnoreCase( DEFAULT_PLATE )) {
            dlog.e(TripInfo.class.toString()+" handleCard: Can't do any operation with default car plate");
            // TODO throw new StandardPlateException("The vehicle plate is the standard vehicle plate.");
            return null;
        }

        // Getting DB
        DbManager dbm = App.Instance.dbManager;
        HttpConnector http;

        Customers customers = dbm.getClientiDao();
        Customer customer = customers.getClienteByCardCode(code);

        // If the Card is unknown, send a visual alert and do nothing
        if (customer==null) {			
            obc_io.setLcd(null,"SCONOSCIUTA");
            dlog.d(TripInfo.class.toString()+" handleCard: Card unknown :" + code);
            Events.eventRfid(0, code);
            return null;
        }

        customer.decrypt();

        dlog.d(TripInfo.class.toString()+" handleCard: Card :  N." + customer.id + " " + customer.name + " " + customer.surname);



        if (this.reopenSuspend) {
            dlog.d(TripInfo.class.toString()+" handleCard: Reopen suspension active.");
            obc_io.setLcd(null, " Attendi 10 sec");
            return null;
        }



        //Se non ci sono trips aperte e ...
        if (!isOpen) {

            dlog.d(TripInfo.class.toString()+" handleCard: No pending trips. New trip");
            // ... l'utente ? abilitato apri la trip 
            if (customer.enabled) {

                if (App.reservation!=null) {   // Se c'? una prenotazione in piedi ... 
                    dlog.d(TripInfo.class.toString()+" handleCard: There is a reservation : " + App.reservation.toString());
                    if  (App.reservation.checkCode(code)) {   //...ed ? arrivato l'utente : procedi e segnala l'uso della prenotazione
                        dlog.d(TripInfo.class.toString()+" handleCard: User owns reservation");
                        this.isMaintenance = App.reservation.isMaintenance();					
                        if (!App.reservation.isLocal()) {
                            //if (!this.isMaintenance) {
                            HttpConnector rhttp = new HttpConnector(service);
                            ReservationConnector rc = new ReservationConnector();
                            rc.setTarga(App.CarPlate);
                            rc.setConsumed(App.reservation.id);
                            rhttp.Execute(rc);
                            App.reservation = null;  //Cancella la prenotazione in locale
                            App.Instance.persistReservation();
                            //}
                        } else {
                            dlog.d(TripInfo.class.toString()+" handleCard: Local out of order reservation");
                            Events.TripOutOfOrder(code);
                        }

                    } else {    //... altrimenti segnala che ? prenotata e non fare nulla
                        obc_io.setLcd(null, "Auto prenotata");
                        dlog.d(TripInfo.class.toString()+" handleCard: Users does not own this reservation");
                        return null;
                    }
                }

                this.customer = customer;

                cardCode = code;					

                if (OpenTrip(carInfo, customer)) {
                    obc_io.setLcd(null, " Auto in uso");
                    obc_io.setDoors(null, 1,customer.info_display);  //Sole se trip registrata su db apri le portiere
                    //obc_io.setEngine(null, 1); //TODO: RIMUOVERE!!!! Il motore si dovr? abilitare solo dopo il check del pin
                    obc_io.setLed(null, LowLevelInterface.ID_LED_BLUE, LowLevelInterface.ID_LED_ON);
                    obc_io.setTag(null,cardCode);
                    Events.eventRfid(1, code+" "+event);
                    hasBeenStopped =false;
                    service.sendBeacon();
                    dlog.d(TripInfo.class.toString() + " handleCard: Car opened ");
                    OptimizeDistanceCalc.init(); // momo inizializzare il tutto per poter calcolare la distanza percorsa.
                } else
                    obc_io.setLcd(null,"Errore sistema");

                TripsConnector cc = new TripsConnector(this, service);

                http = new HttpConnector(service);
                http.Execute(cc);

                // Prepara un messaggio ritardato che chiude l'auto se non viene abilitata la trip entro un timeout
                service.scheduleSelfCloseTrip(300,true);

                service.getHandler().sendMessage(MessageFactory.RadioVolume(1));
                service.getHandler().sendMessage(MessageFactory.RadioVolume(0));

                service.setDisplayStatus(true,0);


                FRadio.savedInstance = null;

                service.getHandler().sendMessage(MessageFactory.startRemoteUpdateCycle());

                App.pinChecked = false;
                App.Instance.persistPinChecked();

                if(!processing) {
                    processing=true;
                    new Thread(new Runnable() {
                        public void run() {//inizzializzazione banner inizio e fine
                            dlog.d(" handleCard: inizzializzazione banner");
                            App.BannerName.clear();
                            App.first_UP_poi=true;
                            loadBanner(App.URL_AdsBuilderStart, "START", false);
                            loadBanner(App.URL_AdsBuilderCar, "CAR", false);
                            loadBanner(App.URL_AdsBuilderEnd, "END", false);
                            processing=false;
                        }
                    }).start();
                }

                return (MessageFactory.notifyTripBegin(this));
            } else {
                dlog.w(TripInfo.class.toString()+" handleCard: Card disabled");
                obc_io.setLcd(null,customer.info_display);
            }
        } else {  // se c'? una trip aperta...
            dlog.d(TripInfo.class.toString()+" handleCard: Pending trips. Check condition...");
            if (code.equalsIgnoreCase(cardCode)) {  //e la card ? dell'utente che ha aperto la trip  chiudi  o sospendi trip

                if (App.getParkModeStarted() !=null) {  // siamo in modalit? parcheggio

                    if (!App.parkMode.isOn()) {  // customer esce e chiude...

                        dlog.d(TripInfo.class.toString()+" handleCard: Pending trips. Park mode ON speed up begin.");

                        App.Instance.setParkModeStarted(new Date());
                        App.Instance.persistParkModeStarted();
                        obc_io.setLcd(null, " Auto prenotata");
                        obc_io.setDoors(null, 0,"IN SOSTA");
                        obc_io.setEngine(null, 0);
                        obc_io.setLed(null, LowLevelInterface.ID_LED_BLUE, LowLevelInterface.ID_LED_ON);
                        Events.eventRfid(3, code);
                        Events.eventParkBegin();
                        hasBeenStopped = true;
                        App.parkMode = ParkMode.PARK_STARTED;
                        App.Instance.persistInSosta();

                        service.getHandler().sendMessage(MessageFactory.stopRemoteUpdateCycle());   
                        service.removeSelfCloseTrip();

                        service.setDisplayStatus(false,15);
                        service.sendBeacon();

                        OptimizeDistanceCalc.Controller(OdoController.PAUSE); // momo metti in pausa il calcolo, la macchina e' ferma
                        return MessageFactory.notifyTripParkModeCardBegin();
                    } else {    //customer rientra

                        obc_io.setLcd(null, " Auto prenotata");
                        if (closeType !=CloseType.forced)  { //Se non ? una chiusura forzata da remoto  apri le portiere ed abilita il motore
                            obc_io.setDoors(null, 1,"BENTORNATO");
                            //obc_io.setEngine(null, 1);
                            dlog.d(TripInfo.class.toString()+" handleCard: Pending trips. Park mode ON user returned, open car");
                        }else{
                            dlog.d("End Park forced trip close");
                        }
                        obc_io.setLed(null, LowLevelInterface.ID_LED_BLUE, LowLevelInterface.ID_LED_ON);
                        obc_io.setTag(null,cardCode);						
                        Events.eventRfid(4, code);
                        Events.eventParkEnd();
                        App.parkMode = ParkMode.PARK_ENDED;
                        App.Instance.persistInSosta();

                        if (closeType==CloseType.forced) {   // Se ? una chiusura forzata chiudi la sosta e richiama ricorsivamente  questa funzione per chiudere anche la trip.
                            setParkMode(0, obc_io);
                            return handleCard( code,  event, carInfo, obc_io, service,  screenLock,  closeType);
                        } else {
                        	 service.getHandler().sendMessage(MessageFactory.startRemoteUpdateCycle());  
                            service.getHandler().sendMessage(MessageFactory.RadioVolume(0));
                            service.setDisplayStatus(true,0);

                            SuspendRfid(obc_io," Auto in uso");
                        }
                        service.sendBeacon();
                        return MessageFactory.notifyTripParkModeCardEnd();
                    }
                } else {  // l'auto viene rilasciata

                    if (! App.isCloseable && closeType !=CloseType.forced) {
                        obc_io.setLcd(null, "CHIUDERE CORSA");
                        dlog.d("handleCard: corsa non chiudibile");
                        return null;
                    }
                    if (service.checkParkArea() || closeType ==CloseType.forced) {

                        cardCode="";
                        obc_io.setLcd(null, "   Auto Libera");
                        obc_io.setDoors(null, 0,"ARRIVEDERCI");
                        obc_io.setEngine(null, 0);
                        obc_io.setLed(null, LowLevelInterface.ID_LED_GREEN, LowLevelInterface.ID_LED_ON);			
                        obc_io.setTag(null,"*");
                        dlog.d(TripInfo.class.toString()+" handleCard: Pending trips. END RENT, disable engine and close doors");



                        Events.eventRfid(2, code);
                        CloseTrip(carInfo);

                        TripsConnector cc = new TripsConnector(this);

                        http = new HttpConnector(service);
                        http.SetHandler(service.getPrivateHandler());
                        dlog.d("Sending close trip");
                        http.Execute(cc);

                        service.setDisplayStatus(false,15);
                        service.getHandler().sendMessage(MessageFactory.RadioVolume(0));						
                        FRadio.savedInstance = null;

                        SuspendRfid(obc_io,"  Auto libera");
                        service.removeSelfCloseTrip();

                        service.getHandler().sendMessage(MessageFactory.stopRemoteUpdateCycle()); 

                        App.pinChecked = false;
                        App.Instance.persistPinChecked();



                        service.sendBeacon();
                        return (MessageFactory.notifyTripEnd(this));
                    } else {
                        dlog.d("Unable to close trip, out of operative area");

                        Toast.makeText(App.Instance.getBaseContext(), "Out of Operative Area", Toast.LENGTH_SHORT).show();
                        obc_io.setLcd(null, "   FUORI AREA");
                        return null;
                    }

                }


            } else {  //se di un'altro utente non fare nulla

                obc_io.setLcd(null,"AUTO IN USO");
                dlog.d(TripInfo.class.toString()+" handleCard: Different card, nothing to do");
                Events.eventRfid(5, code);
                return null;
            }


        }

        return null;
    }

    @Override
    public String toString() {

        return trip!=null?this.trip.toString():"";
    }

    public void loadBanner(String Url, String type, Boolean isClick) {

        File outDir = new File(App.getBannerImagesFolder());
        if (!outDir.isDirectory()) {
            outDir.mkdir();
        }



        if (!App.hasNetworkConnection()) {
            dlog.w(" loadBanner: nessuna connessione");
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
                reader.close();
                content.close();
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
            dlog.w(TripInfo.class.toString()+" loadBanner: nessuna connessione");
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

            Image.putString("ID", jsonObject.getString("ID"));
            Image.putString("URL", jsonObject.getString("URL"));
            Image.putString(("CLICK"), jsonObject.getString("CLICK"));
            Image.putString(("INDEX"), jsonObject.getString("INDEX"));
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
            inputStream.close();
            urlConnection.disconnect();
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


    private void SuspendRfid(final LowLevelInterface obc_io, final String txt) {
        reopenSuspend =true;
        Timer tmr = new Timer();
        tmr.schedule( new TimerTask() {

            @Override
            public void run() {
                reopenSuspend =false;
                obc_io.setLcd(null, txt);
            }

        }, 1000);

    }

    private Trip buildOpenTrip(){
        Trip trip = new Trip(this.customer.id,App.CarPlate,new Date(), DbManager.getTimestamp(), App.fuel_level, App.km);
        trip.setBeginLocation(App.lastLocation);

        return trip;
    }

    public boolean OpenTripNew(CarInfo carInfo, Customer customer) {

        if (carInfo==null || customer==null) {
            dlog.e(TripInfo.class.toString()+" OpenTrip:  carInfo or customer == NULL");
            return false;
        }


        this.customer = customer;
        this.isOpen=true;

        trip = repositoryPhp.openTrip(buildOpenTrip());



        App.currentTripInfo = this;

        dlog.d("OpenTrip: "+this.toString());
        return (trip!=null);

    }

    public boolean OpenTrip(CarInfo carInfo, Customer customer) {

        if (carInfo==null || customer==null) {
            dlog.e(TripInfo.class.toString()+" OpenTrip:  carInfo or customer == NULL");
            return false;
        }


        //TODO: gestire se gi? aperta

        DbManager dbm = App.Instance.dbManager;
        Trips trips = dbm.getCorseDao();

        this.customer = customer;
        this.isOpen=true;
        if(OptimizeDistanceCalc.totalDistance != 0)
             trip = trips.Begin(App.CarPlate,customer, carInfo.location, App.fuel_level, (int) OptimizeDistanceCalc.totalDistance/1000);
        else
            trip = trips.Begin(App.CarPlate,customer, carInfo.location, App.fuel_level, 0);

        App.currentTripInfo = this;

        dlog.d("OpenTrip: "+this.toString());
        return (trip!=null);

    }

    public void CloseTrip(CarInfo carInfo) {

        CloseCorsa(carInfo);

        if(App.pinChecked && App.currentTripInfo.trip.int_cleanliness==0 && App.currentTripInfo.trip.ext_cleanliness==0){
            App.CounterCleanlines++;
            if(App.CounterCleanlines>=5){
                App.CounterCleanlines=0;
                Events.eventCleanliness(0, 0);
            }
            App.Instance.persistCounterCleanlines();
        }
        //check for charge
        if(App.Charging && !carInfo.chargingPlug){
            App.Charging=false;
        }
        App.currentTripInfo = null;

        this.isOpen=false;
        this.isMaintenance = false;
        OptimizeDistanceCalc.Controller(OdoController.STOP);
    }


    public void CloseCorsa(CarInfo carInfo) {
        //TODO: gestire se non ? aperta

        if (trip==null) {
            dlog.e("CloseTrip: trip == NULL");
            return;
        }


        trip.end_battery = App.fuel_level;
        if(OptimizeDistanceCalc.totalDistance != 0)
            trip.end_km = (int) OptimizeDistanceCalc.totalDistance/1000;
        else
            trip.end_km=0;
        trip.end_time = new Date();
        trip.end_timestamp = System.currentTimeMillis()/1000;

        if (carInfo!=null) {
            trip.end_lat = carInfo.latitude;
            trip.end_lon = carInfo.longitude;
        }

        dlog.d("CloseCorsa: closing trip"+trip.toString());

        UpdateCorsa();
    }

    //1 = PARKED , 0 = RUN
    public Message setParkMode(int mode, LowLevelInterface obc_io) {
        Message rmsg = Message.obtain(null,ObcService.MSG_TRIP_PARK);
        rmsg.arg1 = mode;

        if (trip==null) {
            dlog.d("Starting park mode without trip");
            return null;
        }

        if (App.parkMode == ParkMode.PARK_STARTED) {
            dlog.e("UI park release with car closed");
            return null;
        }

        if (mode==1 && App.getParkModeStarted()==null) {
            dlog.d("Starting park mode");
            App.Instance.setParkModeStarted(new Date());
            App.Instance.persistParkModeStarted();
            rmsg.arg1 = 1;
            OptimizeDistanceCalc.Controller(OdoController.PAUSE);
        } else if (mode == 0 && App.getParkModeStarted() != null) {
            OptimizeDistanceCalc.resume();
            dlog.d("Stopping park mode");
            long diff = (new Date()).getTime() - App.getParkModeStarted().getTime();
            if (hasBeenStopped) {
                trip.park_seconds += diff/1000;
                dlog.d("Stopping park mode :" + trip.park_seconds );
                UpdateCorsa();
            } else {
                dlog.d("Stopping park mode : IGNORED. Not really stopped");
            }
            hasBeenStopped=false;
            obc_io.setEngine(null, 1);
            try {
                Thread.sleep(300);  //TODO : remove
            } catch (InterruptedException e) {}
            obc_io.setEngine(null, 1);
            App.Instance.setParkModeStarted(null);
            App.Instance.persistParkModeStarted();
            rmsg.arg1=0;
        } else {
            dlog.e("Setting invalid park mode. mode="+mode + ", Tms:" + (App.getParkModeStarted()==null?"null":App.getParkModeStarted().toString()));
            return null;

        }

        return rmsg;
    }

    public void UpdateCorsa() {
        Trips trips = App.Instance.dbManager.getCorseDao();
        try {
            dlog.d("UpdateCorsa: updating trip "+trip.toString());
            trips.createOrUpdate(trip);
        } catch (SQLException e) {
            dlog.e("Error updating trip",e);

        }
        if(App.currentTripInfo!= null &&App.currentTripInfo.trip!=null && App.currentTripInfo.trip.id==trip.id){
            if(App.currentTripInfo.trip.remote_id==0) {
                App.currentTripInfo.trip.remote_id = trip.remote_id;
                dlog.d("UpdateCorsa: update current trip "+App.currentTripInfo.trip.toString()+" to trip "+trip.toString());
            }
        }
    }



    public boolean CheckPin(String pin, boolean notify) {

        if (this.customer==null) {
            dlog.e("CheckPin: customer==null");
            return false;
        }

        if (this.trip.recharge== -15) {
            dlog.e("CheckPin : Trip open in other car");
            return false;
        }

        int n_pin = customer.checkPin(pin);
        dlog.d("CheckPin : "+n_pin);

        if (n_pin == Customer.N_COMPANY_PIN) {
            DbManager dbm = App.Instance.dbManager;
            BusinessEmployees employees = dbm.getDipendentiDao();
            BusinessEmployee employee = employees.getBusinessEmployee(customer.id);
            if (!customer.isCompanyPinEnabled() || employee==null || !employee.isBusinessEnabled() || !employee.isWithinTimeLimits()) {
                dlog.e("CheckPin : can't open business trip");
                return false;
            }
        }

        if (n_pin>0) {			
            trip.n_pin=n_pin;
            UpdateCorsa();
            try {
                if(notify) {
                    HttpConnector hConnector = new HttpConnector(App.Instance.getApplicationContext());
                    TripsConnector tc = new TripsConnector(this);
                    hConnector.Execute(tc);
                }
            }catch(Exception e){
                dlog.e("Exception while trying to update server trip",e);
            }
            return true;
        } else
            return false;

    }


    public void setTxOffline() {

        if (this.trip==null)
            return;


        UpdateBuilder<Trip,Integer> builder =  App.Instance.getDbManager().getCorseDao().updateBuilder();
        try {			
            builder.updateColumnValue("offline", true).where().idEq(trip.id);
            builder.update();
            builder.reset();
        } catch (SQLException e) {		
            DLog.E("setTxOffline : ",e);
        }
    }


    public void setTxApertura() {

        if (this.trip==null)
            return;


        UpdateBuilder<Trip,Integer> builder =  App.Instance.getDbManager().getCorseDao().updateBuilder();
        try {
            builder.updateColumnValue("begin_sent", true);
            builder.updateColumnValue("remote_id", trip.remote_id).where().idEq(trip.id);
            builder.update();
            builder.reset();
        } catch (SQLException e) {		
            DLog.E("setTxApertura : ",e);
        }
    }

    public void setTxChiusura() {

        if (this.trip==null)
            return;


        UpdateBuilder<Trip,Integer> builder =  App.Instance.getDbManager().getCorseDao().updateBuilder();
        try {
            builder.updateColumnValue("end_sent", true).where().idEq(trip.id);
            builder.update();
            builder.reset();
        } catch (SQLException e) {		
            DLog.E("setTxChiusura : ",e);
        }
    }

    public void setWarning(String warning) {

        if (this.trip==null)
            return;


        UpdateBuilder<Trip,Integer> builder =  App.Instance.getDbManager().getCorseDao().updateBuilder();
        try {			
            builder.updateColumnValue("warning", warning).where().idEq(trip.id);
            builder.update();
            builder.reset();
        } catch (SQLException e) {		
            DLog.E("setWarning : ",e);
        }
    }


    public void HandleMaxDurata(CarInfo carInfo, int maxDurata, ObcService service) {
        if (isOpen && trip!=null && maxDurata>0) {
            long mins = trip.getMinutiDurata();

            if (mins>=maxDurata) {
                dlog.d("Max durata reached: " + mins + " > " + maxDurata);

                if (trip.id_parent==0) {
                    trip.id_parent = trip.remote_id>0?trip.remote_id:-1;
                }

                //Se era in sosta calcola i secondi accumulati e fai ripartire il conteggio
                if (App.getParkModeStarted()!=null && hasBeenStopped) {
                    Date newStart = new Date(); 
                    long diff = newStart.getTime() - App.getParkModeStarted().getTime();
                    trip.park_seconds += diff/1000;
                    dlog.d("Splitting park mode time :" + (diff/1000) );
                    App.Instance.setParkModeStarted(newStart);
                    App.parkMode = ParkMode.PARK_STARTED;
                    App.Instance.persistInSosta();					
                    App.Instance.persistParkModeStarted();
                }

                int id_parent = trip.id_parent;
                int n_pin = trip.n_pin;
                //Se ? passata la durata massima chiudi la trip amministrativamente...
                CloseCorsa(carInfo);

                //.... inva la corsa incapsulandola in un oggeto tripinfo separato per evitare modifiche
                TripInfo tripInfo =  new TripInfo(mContext);
                tripInfo.trip = trip;
                TripsConnector cc = new TripsConnector(tripInfo);

                HttpConnector http = new HttpConnector(App.Instance);
                http.SetHandler(service.getPrivateHandler());
                http.Execute(cc);				

                // Apri una nuova trip che sostituisce la precedente
                DbManager dbm = App.Instance.dbManager;
                Trips trips = dbm.getCorseDao();
                if(OptimizeDistanceCalc.totalDistance != 0)
                    trip = trips.Begin(App.CarPlate, customer, carInfo.location, App.fuel_level, (int) OptimizeDistanceCalc.totalDistance/1000);
                else
                    trip = trips.Begin(App.CarPlate, customer, carInfo.location, App.fuel_level, 0);
                dlog.d("New trip: " + trip.toString());
                trip.id_parent = id_parent;
                trip.n_pin = n_pin;
                UpdateCorsa();

                //Do not send now, it will collide with previous trip closure. After sending will be scheduled.

                /*
				cc = new CorseConnector();
				cc.tripInfo = this;

				http = new HttpConnector(App.Instance);
				http.Execute(cc);
                 */

            }
        }

    }

    public boolean updateCustomer(){
        boolean result = customer.update();
        if(result){
            cardCode = customer.card_code;

        }
        return result;
    }


    public void setEvent(int what, int arg, String txt) {

    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof TripInfo){
         if(!trip.equals(((TripInfo) o).trip)){
             return false;
         }

         if(!customer.equals(((TripInfo) o).customer)){
             return false;
         }

         return true;

        }

        return false;
    }
}
