package eu.philcar.csg.OBC.data.datasources.repositories;

import android.support.annotation.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.data.common.ErrorResponse;
import eu.philcar.csg.OBC.data.datasources.SharengoDataSource;
import eu.philcar.csg.OBC.data.model.Area;
import eu.philcar.csg.OBC.data.model.Config;
import eu.philcar.csg.OBC.data.model.EventResponse;
import eu.philcar.csg.OBC.data.model.ModelResponse;
import eu.philcar.csg.OBC.data.model.TripResponse;
import eu.philcar.csg.OBC.db.BusinessEmployee;
import eu.philcar.csg.OBC.db.Customer;
import eu.philcar.csg.OBC.db.Event;
import eu.philcar.csg.OBC.db.Poi;
import eu.philcar.csg.OBC.db.Trip;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.RxUtil;
import eu.philcar.csg.OBC.server.ServerCommand;
import eu.philcar.csg.OBC.service.DataManager;
import eu.philcar.csg.OBC.service.Reservation;
import eu.philcar.csg.OBC.service.TripInfo;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Fulvio on 15/02/2018.
 */
@Singleton
public class SharengoApiRepository {
    private DataManager mDataManager;
    private SharengoDataSource mRemoteDataSource;
    private boolean needUpdateCustomer;

    private Disposable customerDisposable = null;
    private Disposable employeeDisposable = null;
    private Disposable configDisposable = null;
    private Disposable modelDisposable = null;
    private Disposable reservationDisposable = null;
    private Disposable areaDisposable = null;
    private Disposable commandDisposable = null;

    @Inject
    public SharengoApiRepository(DataManager mDataManager, SharengoDataSource mRemoteDataSource) {
        this.mDataManager = mDataManager;
        this.mRemoteDataSource = mRemoteDataSource;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                            //
    //                                      CUSTOMER                                              //
    //                                                                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    public void stopCustomer(){
        if (customerDisposable != null)
            customerDisposable.dispose();
    }


    public void getCustomer(int millidelay){
        needUpdateCustomer=false;

        if(!RxUtil.isRunning(customerDisposable)) {
            Observable.just(1)
                    .delay(millidelay, TimeUnit.MILLISECONDS)
                    .concatMap(i->mRemoteDataSource.getCustomer(mDataManager.getMaxCustomerLastupdate()))
                    .concatMap(n -> mDataManager.saveCustomer(n))
                    .subscribe(new Observer<Customer>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {
                            customerDisposable = d;
                        }

                        @Override
                        public void onNext(@NonNull Customer ribot) {
                            //TODO schedule new call in case of >10000
                            needUpdateCustomer=true;

                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            if(e instanceof ErrorResponse)
                                DLog.E("Error syncing getCustomer", ((ErrorResponse)e).error);
                            RxUtil.dispose(customerDisposable);
                        }

                        @Override
                        public void onComplete() {
                            DLog.I("Synced successfully!");
                            RxUtil.dispose(customerDisposable);
                            if(needUpdateCustomer)
                                getCustomer(150);
                        }
                    });
        }else{
            DLog.D("CustomerDisposable is running");
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                            //
    //                                      EMPLOYEE                                              //
    //                                                                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    public void stopEmployee(){
        if (employeeDisposable != null)
            employeeDisposable.dispose();
    }

    public void getEmployee(){

        if(!RxUtil.isRunning(employeeDisposable)) {
            mRemoteDataSource.getBusinessEmployees()

                    .concatMap(n -> mDataManager.saveEmployee(n))
                    .subscribeOn(Schedulers.newThread())
                    .subscribe(new Observer<BusinessEmployee>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {
                            employeeDisposable = d;
                        }

                        @Override
                        public void onNext(@NonNull BusinessEmployee ribot) {
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            if(e instanceof ErrorResponse)
                            DLog.E("Error syncing getEmployee", ((ErrorResponse)e).error);
                            RxUtil.dispose(employeeDisposable);
                        }

                        @Override
                        public void onComplete() {
                            DLog.I("Synced successfully!");
                            RxUtil.dispose(employeeDisposable);
                        }
                    });
        }
    }



    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                            //
    //                                      CONFIG                                                //
    //                                                                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////


    public void stopConfig(){
        if (configDisposable != null)
            configDisposable.dispose();
    }

    public void getConfig(){

        if(!RxUtil.isRunning(configDisposable)) {
            mRemoteDataSource.getConfig(App.CarPlate)
                    .doOnNext(n -> mDataManager.saveConfig(n))
                    .subscribeOn(Schedulers.newThread())
                    .subscribe(new Observer<Config>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {
                            configDisposable = d;
                        }

                        @Override
                        public void onNext(@NonNull Config ribot) {
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            if(e instanceof ErrorResponse)
                                DLog.E("Error syncing getConfig" + ((ErrorResponse) e).rawMessage, e);
                            RxUtil.dispose(configDisposable);
                        }

                        @Override
                        public void onComplete() {
                            DLog.I("Synced successfully!");
                            RxUtil.dispose(configDisposable);
                        }
                    });
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                            //
    //                                      MODEL                                                 //
    //                                                                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////


    public void stopModel(){
        if (modelDisposable != null)
            modelDisposable.dispose();
    }

    public void getModel(){

        if(!RxUtil.isRunning(modelDisposable)) {
            mRemoteDataSource.getModel(App.CarPlate)
                    .concatMap(Observable::fromIterable)
                    .doOnNext(n -> mDataManager.saveModel(n))
                    .subscribeOn(Schedulers.newThread())
                    .subscribe(new Observer<ModelResponse>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {
                            modelDisposable = d;
                        }

                        @Override
                        public void onNext(@NonNull ModelResponse ribot) {
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            if(e instanceof ErrorResponse)
                                DLog.E("Error syncing getModel", ((ErrorResponse)e).error);
                            RxUtil.dispose(modelDisposable);
                        }

                        @Override
                        public void onComplete() {
                            DLog.I("Synced successfully!");
                            RxUtil.dispose(modelDisposable);
                        }
                    });
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                            //
    //                                      Reservation                                           //
    //                                                                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////


    public void stopReservation(){
        if (reservationDisposable != null)
            reservationDisposable.dispose();
    }
    public Observable<Reservation> getReservation(){

        return mRemoteDataSource.getReservation(App.CarPlate)
                .concatMap(reservations -> mDataManager.handleReservations(reservations))
                .doOnSubscribe(n -> {
                    reservationDisposable =n;
                })
                .doOnComplete(() ->{});

    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                            //
    //                                      AREA                                                  //
    //                                                                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    public void stopArea(){
        if (areaDisposable != null)
            areaDisposable.dispose();
    }


    public void getArea(){

        if(!RxUtil.isRunning(areaDisposable)) {
            mRemoteDataSource.getArea(App.AreaPolygonMD5)
                    .concatMap(n -> mDataManager.saveArea(n))
                    //Emit single Area Response at time
                    .concatMap(Observable::fromIterable)
                    .subscribeOn(Schedulers.newThread())
                    .subscribe(new Observer<Area>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {
                            areaDisposable = d;
                            //App.polyline = new ArrayList<>();
                        }

                        /**
                         * here I have to parse the response and crate a sort of envelop for the map
                         * @param area
                         */
                        @Override
                        public void onNext(@NonNull Area area) {
                            area.initPoints();
                            area.initEnvelop();
                            App.polyline.add(area.getEnvelope());

                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            if(e instanceof ErrorResponse)
                                if(((ErrorResponse)e).errorType!= ErrorResponse.ErrorType.EMPTY)
                                    DLog.E("Error inside getArea"+ ((ErrorResponse) e).rawMessage);
                            //RxUtil.dispose(areaDisposable);
                        }

                        @Override
                        public void onComplete() {
                            DLog.I("Synced successfully!");
                            //RxUtil.dispose(areaDisposable);
                            App.Instance.initAreaPolygon();
                        }
                    });
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                            //
    //                                      COMMANDS                                              //
    //                                                                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    public void stopCommand(){
        RxUtil.dispose(commandDisposable);
    }


    public Observable<ServerCommand> getCommands(String plate) {

        return  mRemoteDataSource.getCommands(plate)
                .flatMap(Observable::fromIterable)
                .filter(command -> {
                    if ((command.ttl<=0 || command.queued+command.ttl>System.currentTimeMillis()/1000) || command.command.equalsIgnoreCase("CLOSE_TRIP")) {

                        DLog.D("Command accepted :"+command);
                        return true;
                    }else {
                        DLog.D("Command timeout :"+command);
                        return false;
                    }
                })
                .doOnSubscribe(n -> {
                    //RxUtil.dispose(commandDisposable);
                    commandDisposable = n;})
                .doOnError(e -> {
                    if(e instanceof ErrorResponse)
                        if(((ErrorResponse)e).errorType!= ErrorResponse.ErrorType.EMPTY)
                            DLog.E("Error insiede GetCommand",e);
                    //RxUtil.dispose(commandDisposable);
                })
                .doOnComplete(() -> {
                    RxUtil.dispose(commandDisposable);
                });


    }

    public void consumeReservation(final int reservation_id){

        mRemoteDataSource.consumeReservation(reservation_id)
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Observer<Reservation>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Reservation r) {

                        App.reservation = null;  //Cancella la prenotazione in locale
                        App.Instance.persistReservation();
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });

    }



    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                            //
    //                                      POIS                                                  //
    //                                                                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    public void getPois(){
        mRemoteDataSource.getPois(mDataManager.getMaxPoiLastupdate())
                .concatMap(n -> mDataManager.savePoi(n))
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Observer<Poi>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull Poi poi) {
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                    }

                    @Override
                    public void onComplete() {
                        DLog.I("Synced successfully!");
                    }
                });
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                            //
    //                                      EVENTS                                                //
    //                                                                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private static boolean sendingEvents;
    public void sendEvent(final Event event){
        mDataManager.saveEvent(event)
                .concatMap(e -> mRemoteDataSource.sendEvent(e,mDataManager))
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Observer<EventResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(EventResponse eventResponse) {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void sendEvents(final List<Event> event){
        if(!sendingEvents)
            Observable.interval(60,TimeUnit.SECONDS)
                    .take(event.size())
                    .concatMap(i->Observable.just(event.get(i.intValue())))
                    .concatMap(mDataManager::saveEvent)
            /*mDataManager.saveEvents(event)
                    .delay(10, TimeUnit.SECONDS)*/
                    .concatMap(e -> mRemoteDataSource.sendEvent(e,mDataManager))
                    .subscribeOn(Schedulers.newThread())
                    .subscribe(new Observer<EventResponse>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            sendingEvents = true;
                        }

                        @Override
                        public void onNext(EventResponse eventResponse) {
                            DLog.D("Receiver Event response");
                        }

                        @Override
                        public void onError(Throwable e) {
                            sendingEvents = false;
                        }

                        @Override
                        public void onComplete() {
                            sendingEvents = false;

                        }
                    });
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                            //
    //                                      TRIPS                                                 //
    //                                                                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public Observable<TripResponse> openTrip(final Trip trip, final TripInfo tripInfo) {
        return mDataManager.saveTrip(trip)
                .concatMap(n -> mRemoteDataSource.openTrip(n, mDataManager))
                .doOnSubscribe(n -> {
                    //RxUtil.dispose(openTripDisposable);
                    //openTripDisposable = n;
                })
                .doOnError(e -> {
                    if(e instanceof ErrorResponse)
                        if(((ErrorResponse)e).errorType!= ErrorResponse.ErrorType.EMPTY)
                            DLog.E("Error insiede openTrip",e);
                    //RxUtil.dispose(openTripDisposable);
                })
                .doOnComplete(() -> {
                    //RxUtil.dispose(openTripDisposable);
                });

    }

    public Observable<TripResponse> updateServerTripData(Trip trip){
        return mRemoteDataSource.updateTrip(trip)
                .doOnSubscribe(n -> {
                    //RxUtil.dispose(openTripDisposable);
                    //openTripDisposable = n;
                })
                .doOnError(e -> {
                    DLog.E("Error insiede updateTrip",e);
                    //RxUtil.dispose(openTripDisposable);
                })
                .doOnComplete(() -> {
                    //RxUtil.dispose(openTripDisposable);
                });
    }

    public Observable<TripResponse> closeTrip(final Trip trip) {
        return mDataManager.saveTrip(trip)
                .concatMap(n -> {
                    if(!n.begin_sent)
                        return mRemoteDataSource.openTripPassive(n, mDataManager);
                    return Observable.just(n);
                })
                .concatMap(n -> mRemoteDataSource.closeTrip(n, mDataManager))
                .doOnSubscribe(n -> {
                    //RxUtil.dispose(openTripDisposable);
                    //openTripDisposable = n;
                })
                .doOnError(e -> {
                    DLog.E("Error insiede closeTrip",e);
                    //RxUtil.dispose(openTripDisposable);
                })
                .doOnComplete(() -> {
                    //RxUtil.dispose(openTripDisposable);
                });

    }

    public Observable<Trip> closeTrips(final Collection<Trip> trip) {
        return mDataManager.saveTrips(trip)
                .concatMap(n -> {
                    if(!n.begin_sent)
                        return mRemoteDataSource.openTripPassive(n, mDataManager);
                    return Observable.just(n);
                })
                .concatMap(n -> {
                    if(n.end_timestamp!=0)
                        return mRemoteDataSource.closeTripPassive(n, mDataManager);
                    return Observable.just(n);
                })
                .doOnSubscribe(n -> {
                    //RxUtil.dispose(openTripDisposable);
                    //openTripDisposable = n;
                })
                .doOnError(e -> {
                    DLog.E("Error insiede closeTrips",e);
                    //RxUtil.dispose(openTripDisposable);
                })
                .doOnComplete(() -> {
                    //RxUtil.dispose(openTripDisposable);
                });
    }

}
