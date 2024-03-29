package eu.philcar.csg.OBC.service;

import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.os.PowerManager.WakeLock;
import android.support.annotation.NonNull;

import com.j256.ormlite.stmt.UpdateBuilder;

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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.BuildConfig;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.controller.map.FRadio;
import eu.philcar.csg.OBC.data.common.ErrorResponse;
import eu.philcar.csg.OBC.data.datasources.repositories.EventRepository;
import eu.philcar.csg.OBC.data.datasources.repositories.SharengoApiRepository;
import eu.philcar.csg.OBC.data.datasources.repositories.SharengoPhpRepository;
import eu.philcar.csg.OBC.data.model.TripResponse;
import eu.philcar.csg.OBC.db.BusinessEmployee;
import eu.philcar.csg.OBC.db.BusinessEmployees;
import eu.philcar.csg.OBC.db.Customer;
import eu.philcar.csg.OBC.db.Customers;
import eu.philcar.csg.OBC.db.DbManager;
import eu.philcar.csg.OBC.db.Trip;
import eu.philcar.csg.OBC.db.Trips;
import eu.philcar.csg.OBC.devices.LowLevelInterface;
import eu.philcar.csg.OBC.helpers.CardRfid;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.RxUtil;
import eu.philcar.csg.OBC.helpers.UrlTools;
import eu.philcar.csg.OBC.task.OdoController;
import eu.philcar.csg.OBC.task.OptimizeDistanceCalc;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * This Class represents all information of a Trip
 *
 * @author massimo.belluz@bulweria.com
 *///TODO implement Singleton to handle all information about the currentTrip
public class TripInfo {

	public enum CloseType {
		normal,
		forced,
		maintainer
	}

	private static final String DEFAULT_PLATE = "XH123KM";

	@Inject
	SharengoPhpRepository phpRepository;
	@Inject
	EventRepository eventRepository;
	@Inject
	SharengoApiRepository apiRepository;

	// Properties
	public Customer customer;
	public Trip trip;
	public String cardCode;
//	public Date openEventTime;
//	public long openEventTimestamp;
	public static Boolean processing = false;

	// Server Data
	public int serverResult;
	public String serverMessage;

	// Status
	public boolean isOpen;
	public boolean remoteCloseRequested = false;
	public boolean isBonusEnabled = false;
	public boolean isMaintenance;
	public boolean hasBeenStopped = false;
	private boolean reopenSuspend = false;

	// Standard Log Object
	private DLog dlog = new DLog(TripInfo.class);

	private final Context mContext;

	public TripInfo(Context context) {
		mContext = context;
		App.get(mContext).getComponent().inject(this);
	}


	/**
	 * Class Initializer
	 */
	public void init() {
		// Getting data from DB
		DbManager dbm = App.Instance.dbManager;
		Customers customers = dbm.getClientiDao();

		// Extracting data
		Trips trips = dbm.getTripDao();
		Trip lastOpenTrip = trips.getLastOpenTrip();

		if (lastOpenTrip == null) {
			dlog.d("TripInfo.init();No records open. Null fields");
			this.trip = null;
		} else {
			dlog.d("TripInfo.init();One open trip. Loading....");
			this.trip = lastOpenTrip;

			try {
				customer = customers.queryForId(trip.id_customer);
				customer.decrypt();
			} catch (SQLException e) {
				dlog.e("TripInfo.init();Error retriving customer:", e);
			} catch (Exception e) {
				dlog.e("TripInfo.init();Unknown Exception on retriving customer:", e);
			}
		}

		if (trip != null) {
			try {
				dlog.d("TripInfo.init();Loading: " + trip.toString());
				customer = customers.queryForId(trip.id_customer);
				customer.decrypt();
				isOpen = true;
				cardCode = customer.card_code;
				OptimizeDistanceCalc.init(); //change to retrive data from sharedPreferences
			} catch (SQLException e) {
				dlog.e("TripInfo.init();Error retriving customer:", e);
			} catch (Exception e) {
				dlog.e("TripInfo.init();Uknown Exception on retriving customer:", e);
			}
		} else {
			customer = null;
			cardCode = null;
			isOpen = false;
			remoteCloseRequested = false;
		}
	}

	/**
	 * @param code Code
	 * @param event Event
	 * @param carInfo CarInfo
	 * @param obc_io OBC input output
	 * @param service Service
	 * @param screenLock screen locke
	 * @return handler
	 */
	public Message handleCard(String code, String event, CarInfo carInfo, LowLevelInterface obc_io, ObcService service, WakeLock screenLock) {
		return handleCard(code, event, carInfo, obc_io, service, screenLock, CloseType.normal);
	}

	/**
	 * Manage card action.
	 * TODO Refactor extracting this method to a specific Class.
	 *
	 * @param code code
	 * @param event event
	 * @param carInfo car info
	 * @param obc_io obc input output
	 * @param service service
	 * @param screenLock screen lock
	 * @param closeType close type
	 * @return TODO @throws StandardPlateException The vehicle plate is the default vehicle plate.
	 */
	public Message handleCard(
			String code,
			String event,
			CarInfo carInfo,
			LowLevelInterface obc_io,
			ObcService service,
			WakeLock screenLock,
			CloseType closeType) {

		if (code == null) {
			dlog.e("TripInfo.handleCard();handleCard: Handle card - null code :" + event);
			return null;
		}

		if (event == null) {
			dlog.e("TripInfo.handleCard();Handle card - null event :" + event);
			return null;
		}
		//Check if open only card
		if (App.openDoorsCards != null) {
			CardRfid card = (CardRfid) App.openDoorsCards.find(new CardRfid(code, ""));
			if (card != null && !isOpen) {
				dlog.d("TripInfo.handleCard();Passaggio card doorsOnly apertura porte in corso! id card: " + card.toString());
				obc_io.setDoors(null, 1, "Porte Aperte");  //Sole se trip registrata su db apri le portiere
				eventRepository.eventRfid(6, code + " " + card.getName());
				eventRepository.eventCleanliness(0, 0);
				return null;
			}
		}

		/**
		 * Prevent any operation on vehicle with the default standard plate
		 * TODO @throws StandardPlateException
		 */
		if (App.CarPlate.equalsIgnoreCase(DEFAULT_PLATE)) {
			dlog.e("TripInfo.handleCard();Can't do any operation with default car plate");
			// TODO throw new StandardPlateException("The vehicle plate is the standard vehicle plate.");
			return null;
		}

		// Getting DB
		DbManager dbm = App.Instance.dbManager;
		//HttpConnector http;


		Customers customers = dbm.getClientiDao();
		Customer customer = customers.getClienteByCardCode(code);

		// If the Card is unknown, send a visual alert and do nothing
		if (customer == null) {
			if (App.reservation != null && App.reservation.getCustomer_id() != null) {
				customer = new Customer(true);
				customer.id = Integer.parseInt(App.reservation.getCustomer_id());
				customer.name = App.reservation.getName();
				customer.surname = App.reservation.getSurname();
				customer.enabled = true;
				customer.language = "";
				customer.info_display = "";
				customer.update_timestamp = 0;
				customer.mobile = App.reservation.getMobile();
				customer.pin = App.reservation.getPin().getJson();
				customer.card_code = App.reservation.getCard_code();
				customer.encrypt();
				try {
					customers.createOrUpdate(customer);
				} catch (Exception e) {
					dlog.e("TripInfo.handleCard();Exception while creating customer from reservation", e);
				}

			} else {
				obc_io.setLcd(null, "SCONOSCIUTA");
				dlog.d("TripInfo.handleCard();Card unknown :" + code);
				eventRepository.eventRfid(0, code);
				return null;
			}
		}

		customer.decrypt();

		DLog.CR("TripInfo.handleCard();Ricevuto codice RFID appartenente a: " + customer.toString());

		dlog.d("TripInfo.handleCard();Card :  N." + customer.id + " " + customer.name + " " + customer.surname);

		if (this.reopenSuspend) {
			dlog.d("TripInfo.handleCard();Reopen suspension active.");
			obc_io.setLcd(null, " Attendi 10 sec");
			return null;
		}

		//Se non ci sono trips aperte e ...
		if (!isOpen) {

			dlog.d("TripInfo.handleCard();No pending trips. New trip");
			// ... l'utente ? abilitato apri la trip
			if (customer.enabled) {

				if (App.reservation != null) {   // Se c'? una prenotazione in piedi ...
					dlog.d("TripInfo.handleCard();There is a reservation : " + App.reservation.toString());
					if (App.reservation.checkCode(code)) {   //...ed ? arrivato l'utente : procedi e segnala l'uso della prenotazione
						dlog.d("TripInfo.handleCard();User owns reservation");
						this.isMaintenance = App.reservation.isMaintenance();
						if (!App.reservation.isLocal()) {
							//if (!this.isMaintenance) {

							if (App.fullNode)
								apiRepository.consumeReservation(App.reservation.id);
							else
								phpRepository.consumeReservation(App.reservation.id);

                            /*HttpConnector rhttp = new HttpConnector(service);
							ReservationConnector rc = new ReservationConnector();
                            rc.setTarga(App.CarPlate);
                            rc.setConsumed(App.reservation.id);
                            rhttp.Execute(rc);*/
							//}
						} else {
							dlog.d("TripInfo.handleCard();Local out of order reservation");
							eventRepository.TripOutOfOrder(code);
						}

					} else {    //... altrimenti segnala che ? prenotata e non fare nulla
						obc_io.setLcd(null, "Auto prenotata");
						dlog.d("TripInfo.handleCard();Users does not own this reservation");
						return null;
					}
				}

				this.customer = customer;

				cardCode = code;

				if (OpenTripNew(carInfo, customer, service, obc_io)) {

					eventRepository.eventRfid(1, code + " " + event);
					hasBeenStopped = false;
					dlog.d("TripInfo.handleCard();Car opened ");
					OptimizeDistanceCalc.init(); // momo inizializzare il tutto per poter calcolare la distanza percorsa.
				} else
					obc_io.setLcd(null, "Errore sistema");

               /* TripsConnector cc = new TripsConnector(this, service);

                http = new HttpConnector(service);
                http.Execute(cc);*/

				// Prepara un messaggio ritardato che chiude l'auto se non viene abilitata la trip entro un timeout

				if (!processing) {
					processing = true;
					new Thread(new Runnable() {
						public void run() {//inizzializzazione banner inizio e fine
							dlog.d("TripInfo.handleCard();init banner");
							App.BannerName.clear();
							App.first_UP_poi = true;
							//for countries outside Italy use local banners
							dlog.d("TripInfo.handleCard();GET def lang: " + App.Instance.getDefaultLang());
							if (App.Instance.getDefaultLang().equals("it")) {
								loadBanner(App.URL_AdsBuilderStart, "START", false);
								loadBanner(App.URL_AdsBuilderCar, "CAR", false);
								loadBanner(App.URL_AdsBuilderEnd, "END", false);
							}
							processing = false;
						}
					}).start();
				}
				return (MessageFactory.notifyTripBegin(this));
			} else {
				dlog.w("TripInfo.handleCard();Card disabled");
				//TODO check for remote card validation
				obc_io.setLcd(null, customer.info_display);
			}
		} else {  // se c'? una trip aperta...
			if("OPEN".equalsIgnoreCase(event)){
				dlog.d("TripInfo.handleCard();event OPEN trip already present");
				return null;
			}
			dlog.d("TripInfo.handleCard();Pending trips. Check condition...");
			if (code.equalsIgnoreCase(cardCode)) {  //e la card ? dell'utente che ha aperto la trip  chiudi  o sospendi trip

				if (App.getParkModeStarted() != null) {  // siamo in modalit? parcheggio

					if (!App.parkMode.isOn()) {  // customer esce e chiude...

						dlog.d("TripInfo.handleCard(); Pending trips. Park mode ON speed up begin.");

						App.Instance.setParkModeStarted(new Date());
						App.Instance.persistParkModeStarted();
						obc_io.setLcd(null, " Auto prenotata");
						obc_io.setDoors(null, 0, "IN SOSTA");
						obc_io.setEngine(null, 0);
						obc_io.setLed(null, LowLevelInterface.ID_LED_BLUE, LowLevelInterface.ID_LED_ON);
						eventRepository.eventRfid(3, code);
						eventRepository.eventParkBegin();
						hasBeenStopped = true;
						App.parkMode = ParkMode.PARK_STARTED;
						App.Instance.persistInSosta();

						service.getHandler().sendMessage(MessageFactory.stopRemoteUpdateCycle());
						service.removeSelfCloseTrip();

						service.setDisplayStatus(false, 15);
//                        service.sendBeacon();

						OptimizeDistanceCalc.Controller(OdoController.PAUSE); // momo metti in pausa il calcolo, la macchina e' ferma
						return MessageFactory.notifyTripParkModeCardBegin();
					} else {    //customer rientra

						obc_io.setLcd(null, " Auto prenotata");
						if (closeType != CloseType.forced) { //Se non ? una chiusura forzata da remoto  apri le portiere ed abilita il motore
							obc_io.setDoors(null, 1, "BENTORNATO");
							//obc_io.setEngine(null, 1);
							dlog.d("TripInfo.handleCard();Pending trips. Park mode ON user returned, open car");
						} else {
							dlog.d("TripInfo.handleCard();End Park forced trip close");
						}
						obc_io.setLed(null, LowLevelInterface.ID_LED_BLUE, LowLevelInterface.ID_LED_ON);
						obc_io.setTag(null, cardCode);
						eventRepository.eventRfid(4, code);
						eventRepository.eventParkEnd();
						App.parkMode = ParkMode.PARK_ENDED;
						App.Instance.persistInSosta();

						if (closeType == CloseType.forced) {   // Se ? una chiusura forzata chiudi la sosta e richiama ricorsivamente  questa funzione per chiudere anche la trip.
							setParkMode(0, obc_io);
							return handleCard(code, "CLOSE", carInfo, obc_io, service, screenLock, closeType);
						} else {
							service.getHandler().sendMessage(MessageFactory.startRemoteUpdateCycle());
							service.getHandler().sendMessage(MessageFactory.RadioVolume(0));
							service.setDisplayStatus(true, 0);

							SuspendRfid(obc_io, " Auto in uso");
						}
//                        service.sendBeacon();
						return MessageFactory.notifyTripParkModeCardEnd();
					}
				} else {  // l'auto viene rilasciata

					if (!App.isIsCloseable() && closeType != CloseType.forced) {
						obc_io.setLcd(null, "CHIUDERE CORSA");
						dlog.d("TripInfo.handleCard();corsa non chiudibile");
						return null;
					}
					if (service.checkParkArea() || closeType == CloseType.forced || !App.pinChecked) {

						eventRepository.eventRfid(2, code);
						CloseTrip(carInfo, obc_io, service);

                        /*TripsConnector cc = new TripsConnector(this);

                        http = new HttpConnector(service);
                        http.SetHandler(service.getPrivateHandler());
                        dlog.d("Sending close trip");
                        http.Execute(cc);*/

						service.setDisplayStatus(false, 15);
						service.getHandler().sendMessage(MessageFactory.RadioVolume(0));
						FRadio.savedInstance = null;

						SuspendRfid(obc_io, "  Auto libera");
						service.removeSelfCloseTrip();

						service.getHandler().sendMessage(MessageFactory.stopRemoteUpdateCycle());

						App.pinChecked = false;
						App.Instance.persistPinChecked();

//                        service.sendBeacon();
						return (MessageFactory.notifyTripEnd(this));
					} else {
						dlog.d("TripInfo.handleCard();Unable to close trip, out of operative area");

						//Toast.makeText(App.Instance.getBaseContext(), "Out of Operative Area", Toast.LENGTH_SHORT).show();
						obc_io.setLcd(null, "   FUORI AREA");
						return null;
					}

				}

			} else {  //se di un'altro utente non fare nulla

				obc_io.setLcd(null, "AUTO IN USO");
				dlog.d("TripInfo.handleCard();Different card, nothing to do");
				eventRepository.eventRfid(5, code);
				return null;
			}

		}

		return null;
	}

	@Override
	public String toString() {

		return trip != null ? this.trip.toString() : "";
	}

	public void loadBanner(String Url, String type, Boolean isClick) {

		File outDir = new File(App.getBannerImagesFolder());
		if (!outDir.isDirectory()) {
			outDir.mkdir();
		}

		if (!App.hasNetworkConnection()) {
			dlog.w("TripInfo.loadBanner();no connection");
			App.BannerName.putBundle(type, null);//null per identificare nessuna connessione, caricare immagine offline
			return;
		}
		StringBuilder builder = new StringBuilder();
		List<NameValuePair> paramsList = new ArrayList<NameValuePair>();
//		HttpResponse response = null;
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
			if (App.BannerName.getBundle(type) != null)
				paramsList.add(new BasicNameValuePair("index", App.BannerName.getBundle(type).getString("INDEX", null)));

			if (App.BannerName.getBundle(type) != null)
				paramsList.add(new BasicNameValuePair("end", App.BannerName.getBundle(type).getString("END", null)));

			Url = UrlTools.buildQuery(Url.concat("?"), paramsList).toString();
			//connessione per scaricare id immagine

			HttpClient client = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(Url);

			HttpResponse response = client.execute(httpGet);
			DLog.I("TripInfo.loadBanner();Url request " + Url);
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

				dlog.e("TripInfo.loadBanner();Failed to connect " + statusCode + " url " + Url);
				App.BannerName.putBundle(type, null);//null per identificare nessuna connessione, caricare immagine offline
				return;
			}
		} catch (Exception e) {
			dlog.e("TripInfo.loadBanner();conn. exception ", e);
			App.BannerName.putBundle(type, null);//null per identificare nessuna connessione, caricare immagine offline
			return;
		}
		String jsonStr = builder.toString();
		if (jsonStr.compareTo("") == 0) {
			dlog.w("TripInfo.loadBanner();no connection");
			App.BannerName.putBundle(type, null);//null per identificare nessuna connessione, caricare immagine offline
			return;
		}

		DLog.I("TripInfo.loadBanner();risposta " + jsonStr);
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
			Image.putString(("END"), jsonObject.getString("END"));

			App.BannerName.putBundle(type, Image);

			//ricavo nome file
			URL urlImg = new URL(Image.getString("URL").replace(" ", "%20"));
			String extension = urlImg.getFile().substring(urlImg.getFile().lastIndexOf('.') + 1);
			String filename = Image.getString("ID") + "." + extension;

			//download imagine se non esiste
			file = new File(outDir, filename);

			if (file.exists()) {
				Image.putString(("FILENAME"), filename);
				App.BannerName.putBundle(type, Image);
				dlog.i("TripInfo.loadBanner();file already exist " + filename);
				return;
			}

			dlog.i("TripInfo.loadBanner();file mancante inizio download a url: " + urlImg.toString());
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
			inputStream.close();
			urlConnection.disconnect();
			Image.putString(("FILENAME"), filename);
			App.BannerName.putBundle(type, Image);
			dlog.i("TripInfo.loadBanner();File scaricato e creato " + filename);

		} catch (Exception e) {
			if (file.exists()) file.delete();
			App.BannerName.putBundle(type, null);//null per identificare nessuna connessione, caricare immagine offline
			dlog.e("TripInfo.loadBanner();eccezione in creazione e download file ", e);

			e.printStackTrace();
		}

	}

	private void SuspendRfid(final LowLevelInterface obc_io, final String txt) {
		reopenSuspend = true;
		Timer tmr = new Timer();
		tmr.schedule(new TimerTask() {

			@Override
			public void run() {
				reopenSuspend = false;
				obc_io.setLcd(null, txt);
			}

		}, 1000);

	}

	private Trip buildOpenTrip() {
		int km = 0;
		if (OptimizeDistanceCalc.totalDistance != 0)
			km = (int) OptimizeDistanceCalc.totalDistance / 1000;
		Trip trip = new Trip();
		trip.id_customer = this.customer.id;
		trip.plate = App.CarPlate;
		trip.begin_time = new Date();
		trip.begin_timestamp = DbManager.getTimestamp();
		trip.begin_battery = App.fuel_level;
		trip.begin_km = km;
//        Trip trip = new Trip(this.customer.id,App.CarPlate,new Date(), DbManager.getTimestamp(), App.fuel_level, km);
		trip.setBeginLocation(App.getLastLocation());

		return trip;
	}

	private boolean OpenTripNew(CarInfo carInfo, Customer customer, final ObcService service, final LowLevelInterface obc_io) {

		if (carInfo == null || customer == null) {
			dlog.e("TripInfo.OpenTripNew();carInfo or customer == NULL");
			return false;
		}

		this.customer = customer;
		this.isOpen = true;

		trip = buildOpenTrip();
		//Scrittura DB
		Observable.just(1)
				.delay(50, TimeUnit.MILLISECONDS)
				.map(n -> {
					if (service != null)
						service.onTripResult(TripInfo.this);

					dlog.cr("TripInfo.OpenTripNew();new trip " + trip.toString());
					obc_io.setLcd(null, " Auto in uso");
					obc_io.setDoors(null, 1, customer.info_display);
					obc_io.setLed(null, LowLevelInterface.ID_LED_BLUE, LowLevelInterface.ID_LED_ON);
					obc_io.setTag(null, cardCode);

					service.scheduleSelfCloseTrip(300, true);

					service.getHandler().sendMessage(MessageFactory.RadioVolume(1));
					service.getHandler().sendMessage(MessageFactory.RadioVolume(0));

					service.setDisplayStatus(true, 0);

					FRadio.savedInstance = null;

					service.getHandler().sendMessage(MessageFactory.startRemoteUpdateCycle());

					App.pinChecked = false;
					App.Instance.persistPinChecked();

//                    service.sendBeacon();
					return n;
				})
				.concatMap(f -> {
					if (App.fullNode)
						return apiRepository.openTrip(trip, this);
					else
						return phpRepository.openTrip(trip, this);
				})
//                .subscribeOn(Schedulers.newThread())
				.subscribeOn(Schedulers.newThread())
				.subscribe(new Observer<TripResponse>() {
					Disposable disposable;

					@Override
					public void onSubscribe(Disposable d) {
						disposable = d;
					}

					@Override
					public void onNext(@NonNull TripResponse tripResponse) {

					}

					@Override
					public void onError(@NonNull Throwable e) {
						if (e instanceof ErrorResponse) {

							dlog.e("TripInfo.OpenTripNew();Error inside Trip Opening " + ((ErrorResponse) e).errorType, e);
						}
						RxUtil.dispose(disposable);
					}

					@Override
					public void onComplete() {
						RxUtil.dispose(disposable);
					}
				});

		App.currentTripInfo = this;

		dlog.d("TripInfo.OpenTripNew();" + this.toString());
		return (trip != null);

	}

	@Deprecated
	public boolean OpenTrip(CarInfo carInfo, Customer customer) {

		if (carInfo == null || customer == null) {
			dlog.e("TripInfo.OpenTripNew();carInfo or customer == NULL");
			return false;
		}

		//TODO: gestire se gi? aperta

		DbManager dbm = App.Instance.dbManager;
		Trips trips = dbm.getTripDao();

		this.customer = customer;
		this.isOpen = true;
		if (OptimizeDistanceCalc.totalDistance != 0)
			trip = trips.Begin(App.CarPlate, customer, carInfo.location, App.fuel_level, (int) OptimizeDistanceCalc.totalDistance / 1000);
		else
			trip = trips.Begin(App.CarPlate, customer, carInfo.location, App.fuel_level, 0);

		App.currentTripInfo = this;

		dlog.d("OpenTrip: " + this.toString());
		return (trip != null);

	}

	private void CloseTrip(CarInfo carInfo, LowLevelInterface obc_io, ObcService service) {

		CloseCorsaNew(carInfo, obc_io, service);

		if (App.pinChecked && App.currentTripInfo.trip.int_cleanliness == 0 && App.currentTripInfo.trip.ext_cleanliness == 0) {
			App.CounterCleanlines++;
			if (App.CounterCleanlines >= 5) {
				App.CounterCleanlines = 0;
				eventRepository.eventCleanliness(0, 0);
			}
			App.Instance.persistCounterCleanlines();
		}
		//check for charge

		App.currentTripInfo = null;
		dlog.i("TripInfo.CloseTrip();closing trip reset currentTripInfo");
		this.isOpen = false;
		this.remoteCloseRequested = false;
		this.isMaintenance = false;
		OptimizeDistanceCalc.Controller(OdoController.STOP);
	}

	private void CloseCorsaMaxDurata(CarInfo carInfo) {
		//TODO: gestire se non ? aperta

		if (trip == null) {
			dlog.e("TripInfo.CloseCorsaMaxDurata();trip == NULL");
			return;
		}

		trip.end_battery = App.fuel_level;
		trip.end_km = App.km;
		trip.end_time = new Date();
		trip.end_timestamp = System.currentTimeMillis() / 1000;

		if (carInfo != null) {
			trip.end_lat = carInfo.getLatitude();
			trip.end_lon = carInfo.getLongitude();
		}

		dlog.d("TripInfo.CloseCorsaMaxDurata();closing trip" + trip.toString());
		Observable.just(1)
				.concatMap(i -> {
					if (App.fullNode)
						return apiRepository.closeTrip(trip);
					else
						return phpRepository.closeTrip(trip);
				})

//                .subscribeOn(Schedulers.newThread())
				.subscribe(new Observer<TripResponse>() {
					Disposable disposable;

					@Override
					public void onSubscribe(Disposable d) {
						disposable = d;
					}

					@Override
					public void onNext(@NonNull TripResponse tripResponse) {

					}

					@Override
					public void onError(@NonNull Throwable e) {
						RxUtil.dispose(disposable);
					}

					@Override
					public void onComplete() {
						RxUtil.dispose(disposable);
					}
				});
	}

	private void CloseCorsaNew(CarInfo carInfo, LowLevelInterface obc_io, ObcService service) {
		//TODO: gestire se non ? aperta

		if (trip == null) {
			dlog.e("TripInfo.CloseCorsaNew();trip == NULL");
			return;
		}

		int km = 0;
		if (OptimizeDistanceCalc.totalDistance != 0)
			km = (int) OptimizeDistanceCalc.totalDistance / 1000;

		trip.end_battery = App.fuel_level;
		trip.end_km = km;
		trip.end_time = new Date();
		trip.end_timestamp = System.currentTimeMillis() / 1000;

		if (carInfo != null) {
			trip.end_lat = carInfo.getLatitude();
			trip.end_lon = carInfo.getLongitude();
		}

		dlog.d("TripInfo.CloseCorsaNew();closing trip" + trip.toString());
		OptimizeDistanceCalc.Controller(OdoController.STOP);

		Observable.just(1)
				.delay(50, TimeUnit.MILLISECONDS)
				.map(n -> {
					cardCode = "";
					dlog.cr("TripInfo.CloseCorsaNew();" + trip.toString());
					obc_io.setLcd(null, "   Auto Libera");
					obc_io.setDoors(null, 0, "ARRIVEDERCI");
					obc_io.setEngine(null, 0);
					obc_io.setLed(null, LowLevelInterface.ID_LED_GREEN, LowLevelInterface.ID_LED_ON);
					obc_io.setTag(null, "*");
					dlog.d("TripInfo.CloseCorsaNew(); Pending trips. END RENT, disable engine and close doors");

					service.setDisplayStatus(false, 15);
					service.getHandler().sendMessage(MessageFactory.RadioVolume(0));
					FRadio.savedInstance = null;
					SuspendRfid(obc_io, "  Auto libera");
					service.removeSelfCloseTrip();
					service.getHandler().sendMessage(MessageFactory.stopRemoteUpdateCycle());
					App.pinChecked = false;
					App.Instance.persistPinChecked();
//                    service.sendBeacon();
					return n;
				})
				.concatMap(n -> {
					if (App.fullNode)
						return apiRepository.closeTrip(trip);
					else
						return phpRepository.closeTrip(trip);
				})
//                .subscribeOn(Schedulers.newThread())
				.subscribe(new Observer<TripResponse>() {
					Disposable disposable;

					@Override
					public void onSubscribe(Disposable d) {
						disposable = d;
					}

					@Override
					public void onNext(@NonNull TripResponse tripResponse) {

					}

					@Override
					public void onError(@NonNull Throwable e) {
						RxUtil.dispose(disposable);
					}

					@Override
					public void onComplete() {
						RxUtil.dispose(disposable);
					}
				});
	}

	@Deprecated
	public void CloseCorsa(CarInfo carInfo) {
		//TODO: gestire se non ? aperta

		if (trip == null) {
			dlog.e("TripInfo.CloseCorsa();trip == NULL");
			return;
		}

		trip.end_battery = App.fuel_level;
		if (OptimizeDistanceCalc.totalDistance != 0)
			trip.end_km = (int) OptimizeDistanceCalc.totalDistance / 1000;
		else
			trip.end_km = 0;
		trip.end_time = new Date();
		trip.end_timestamp = System.currentTimeMillis() / 1000;

		if (carInfo != null) {
			trip.end_lat = carInfo.getLatitude();
			trip.end_lon = carInfo.getLongitude();
		}

		dlog.d("TripInfo.CloseCorsa();closing trip" + trip.toString());

		UpdateCorsa();
	}

	//1 = PARKED , 0 = RUN
	public Message setParkMode(int mode, LowLevelInterface obc_io) {
		Message rmsg = Message.obtain(null, ObcService.MSG_TRIP_PARK);
		rmsg.arg1 = mode;

		if (trip == null) {
			dlog.d("TripInfo.setParkMode();Starting park mode without trip");
			return null;
		}

		if (App.parkMode == ParkMode.PARK_STARTED) {
			dlog.e("TripInfo.setParkMode();UI park release with car closed");
			return null;
		}

		if (mode == 1 && App.getParkModeStarted() == null) {
			dlog.d("TripInfo.setParkMode();Starting park mode");
			dlog.cr("TripInfo.setParkMode();Inizio sosta su trip:" + trip.toString());
			App.Instance.setParkModeStarted(new Date());
			App.Instance.persistParkModeStarted();
			rmsg.arg1 = 1;
			OptimizeDistanceCalc.Controller(OdoController.PAUSE);
		} else if (mode == 0 && App.getParkModeStarted() != null) {
			OptimizeDistanceCalc.resume();
			dlog.d("TripInfo.setParkMode();Stopping park mode");
			dlog.cr("TripInfo.setParkMode();Fine sosta su trip:" + trip.toString());
			long diff = (new Date()).getTime() - App.getParkModeStarted().getTime();
			if (hasBeenStopped) {
				trip.park_seconds += diff / 1000;
				dlog.d("TripInfo.setParkMode();Stopping park mode :" + trip.park_seconds);
				UpdateCorsa();
			} else {
				dlog.d("TripInfo.setParkMode();Stopping park mode : IGNORED. Not really stopped");
			}
			hasBeenStopped = false;
			obc_io.setEngine(null, 1);
			try {
				Thread.sleep(300);  //TODO : remove
			} catch (InterruptedException e) {
				dlog.e("TripInfo.setParkMode();", e);
			}
			obc_io.setEngine(null, 1);
			App.Instance.setParkModeStarted(null);
			App.Instance.persistParkModeStarted();
			rmsg.arg1 = 0;
		} else {
			dlog.e("TripInfo.setParkMode();Setting invalid park mode. mode=" + mode + ", Tms:" + (App.getParkModeStarted() == null ? "null" : App.getParkModeStarted().toString()));
			return null;

		}

		return rmsg;
	}

	public void UpdateCorsa() {
		Trips trips = App.Instance.dbManager.getTripDao();
		try {
			dlog.d("TripInfo.UpdateCorsa();updating trip " + trip.toString());
			trips.createOrUpdate(trip);
		} catch (SQLException e) {
			dlog.e("TripInfo.UpdateCorsa();Error updating trip", e);

		}
		if (App.currentTripInfo != null && App.currentTripInfo.trip != null && App.currentTripInfo.trip.id == trip.id) {
			if (App.currentTripInfo.trip.remote_id == 0) {
				App.currentTripInfo.trip.remote_id = trip.remote_id;
				dlog.d("TripInfo.UpdateCorsa();update current trip " + App.currentTripInfo.trip.toString() + " to trip " + trip.toString());
			}
		}
	}

	public boolean CheckPin(String pin, boolean notify) {

		if (pin == null) {
			dlog.e("TripInfo.CheckPin();pin==null");
//			return false;
		}

		if (this.customer == null) {
			dlog.e("TripInfo.CheckPin();customer==null");
			return false;
		}

		if (this.trip.recharge == -15) {
			dlog.e("TripInfo.CheckPin();Trip open in other car");
			return false;
		}

		int n_pin = customer.checkPin(pin);
		dlog.i("TripInfo.CheckPin();" + n_pin);
		dlog.cr("TripInfo.CheckPin();Inserito pin corretto, tipo: " + n_pin);

		if (n_pin == Customer.N_COMPANY_PIN) {
			DbManager dbm = App.Instance.dbManager;
			BusinessEmployees employees = dbm.getDipendentiDao();
			BusinessEmployee employee = employees.getBusinessEmployee(customer.id);
			if (BuildConfig.BUILD_TYPE.equalsIgnoreCase("debug"))
				employee.isBusinessEnabled = true;
			if (!customer.isCompanyPinEnabled() || !employee.isBusinessEnabled() || !employee.isWithinTimeLimits()) {
				dlog.e("TripInfo.CheckPin();can't open business trip");
				return false;
			}
		}

		if (n_pin > 0) {

			App.pinChecked = true;
			App.Instance.persistPinChecked();

			trip.n_pin = n_pin;
			UpdateCorsa();
			try {
				if (notify && n_pin == Customer.N_COMPANY_PIN) {
					sendUpdateTrip(trip);
				}
			} catch (Exception e) {
				dlog.e("TripInfo.CheckPin();Exception while trying to update server trip", e);
			}
			return true;
		} else
			return false;

	}

	private void sendUpdateTrip(Trip trip) {
		Observable.just(1)
				.concatMap(i -> {
					if (App.fullNode)
						return apiRepository.updateServerTripData(trip);
					else
						return phpRepository.updateServerTripData(trip);
				})
//                .subscribeOn(Schedulers.newThread())
				.subscribe(new Observer<TripResponse>() {
					Disposable disposable;

					@Override
					public void onSubscribe(Disposable d) {
						disposable = d;
					}

					@Override
					public void onNext(TripResponse tripResponse) {

					}

					@Override
					public void onError(Throwable e) {
						RxUtil.dispose(disposable);

					}

					@Override
					public void onComplete() {
						RxUtil.dispose(disposable);

					}
				});
	}

	public void setTxOffline() {

		if (this.trip == null)
			return;

		UpdateBuilder<Trip, Integer> builder = App.Instance.getDbManager().getTripDao().updateBuilder();
		try {
			builder.updateColumnValue("offline", true).where().idEq(trip.id);
			builder.update();
			builder.reset();
		} catch (SQLException e) {
			DLog.E("TripInfo.setTxOffline();", e);
		}
	}

	public void setTxApertura() {

		if (this.trip == null)
			return;

		UpdateBuilder<Trip, Integer> builder = App.Instance.getDbManager().getTripDao().updateBuilder();
		try {
			builder.updateColumnValue("begin_sent", true);
			builder.updateColumnValue("remote_id", trip.remote_id).where().idEq(trip.id);
			builder.update();
			builder.reset();
		} catch (SQLException e) {
			DLog.E("TripInfo.setTxApertura();", e);
		}
	}

	public void setTxChiusura() {

		if (this.trip == null)
			return;

		UpdateBuilder<Trip, Integer> builder = App.Instance.getDbManager().getTripDao().updateBuilder();
		try {
			builder.updateColumnValue("end_sent", true).where().idEq(trip.id);
			builder.update();
			builder.reset();
		} catch (SQLException e) {
			DLog.E("TripInfo.setTxApertura();", e);
		}
	}

	public void setWarning(String warning) {

		if (this.trip == null)
			return;

		UpdateBuilder<Trip, Integer> builder = App.Instance.getDbManager().getTripDao().updateBuilder();
		try {
			builder.updateColumnValue("warning", warning).where().idEq(trip.id);
			builder.update();
			builder.reset();
		} catch (SQLException e) {
			DLog.E("TripInfo.setWarning();", e);
		}
	}

	public void HandleMaxDurata(CarInfo carInfo, int maxDurata, ObcService service) {
		if (isOpen && trip != null && maxDurata > 0) {
			long mins = trip.getMinutiDurata();

			if (mins >= maxDurata) {
				dlog.d("TripInfo.HandleMaxDurata();Max durata reached: " + mins + " > " + maxDurata);

				if (trip.getId_parent() == 0) {
					trip.setId_parent(trip.remote_id > 0 ? trip.remote_id : -1);
				}

				//Se era in sosta calcola i secondi accumulati e fai ripartire il conteggio
				if (App.getParkModeStarted() != null && hasBeenStopped) {
					Date newStart = new Date();
					long diff = newStart.getTime() - App.getParkModeStarted().getTime();
					trip.park_seconds += diff / 1000;
					dlog.d("TripInfo.HandleMaxDurata();Splitting park mode time :" + (diff / 1000));
					App.Instance.setParkModeStarted(newStart);
					App.parkMode = ParkMode.PARK_STARTED;
					App.Instance.persistInSosta();
					App.Instance.persistParkModeStarted();
				}

				int id_parent = trip.getId_parent();
				int n_pin = trip.n_pin;
				//Se ? passata la durata massima chiudi la trip amministrativamente...
				CloseCorsaMaxDurata(carInfo);
				//CloseCorsa(carInfo);

				//.... inva la corsa incapsulandola in un oggeto tripinfo separato per evitare modifiche
                /*TripInfo tripInfo =  new TripInfo(mContext);
                tripInfo.trip = trip;
                TripsConnector cc = new TripsConnector(tripInfo);

                HttpConnector http = new HttpConnector(App.Instance);
                http.SetHandler(service.getPrivateHandler());
                http.Execute(cc);	*/

				// Apri una nuova trip che sostituisce la precedente
//                DbManager dbm = App.Instance.dbManager;
//                Trips trips = dbm.getTripDao();

				trip = buildOpenTrip();
				trip.setId_parent(id_parent);
				trip.n_pin = n_pin;
				Observable.just(1)
						.concatMap(i -> {
							if (App.fullNode)
								return apiRepository.openTrip(trip, this);
							else
								return phpRepository.openTrip(trip, this);
						})
//                        .+(Schedulers.newThread())
						.subscribeOn(Schedulers.newThread())
						.subscribe(new Observer<TripResponse>() {
							Disposable disposable;

							@Override
							public void onSubscribe(Disposable d) {
								disposable = d;
							}

							@Override
							public void onNext(@NonNull TripResponse tripResponse) {

							}

							@Override
							public void onError(@NonNull Throwable e) {
								RxUtil.dispose(disposable);
							}

							@Override
							public void onComplete() {
								RxUtil.dispose(disposable);
							}
						});
				//trip = trips.Begin(App.CarPlate,customer, carInfo.location, App.fuel_level, App.km);
				dlog.d("TripInfo.HandleMaxDurata();New trip: " + trip.toString());
				//UpdateCorsa();

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

/*	public boolean updateCustomer() {
		boolean result = customer.update();
		if (result) {
			cardCode = customer.card_code;

		}
		return result;
	}*/

/*	public void setEvent(int what, int arg, String txt) {

	}*/

	@Override
	public boolean equals(Object o) {
		if (o instanceof TripInfo) {
			if (!trip.equals(((TripInfo) o).trip)) {
				return false;
			}

			return customer.equals(((TripInfo) o).customer);

		}

		return false;
	}
}
