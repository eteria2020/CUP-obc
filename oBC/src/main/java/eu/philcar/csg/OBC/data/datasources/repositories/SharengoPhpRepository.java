package eu.philcar.csg.OBC.data.datasources.repositories;

import android.support.annotation.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.data.common.ErrorResponse;
import eu.philcar.csg.OBC.data.datasources.SharengoPhpDataSource;
import eu.philcar.csg.OBC.data.model.Area;
import eu.philcar.csg.OBC.data.model.EventResponse;
import eu.philcar.csg.OBC.data.model.TripResponse;
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
 * Created by Fulvio on 16/02/2018.
 */
@Singleton
public class SharengoPhpRepository {

    private DataManager mDataManager;
    private SharengoPhpDataSource mRemoteDataSource;

    private Disposable areaDisposable;
    private Disposable commandDisposable;
    private Disposable openTripDisposable;

    @Inject
    public SharengoPhpRepository(DataManager mDataManager, SharengoPhpDataSource mRemoteDataSource) {
        this.mDataManager = mDataManager;
        this.mRemoteDataSource = mRemoteDataSource;
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

    @Deprecated
    public void getArea(){

        if(!RxUtil.isRunning(areaDisposable)) {
            mRemoteDataSource.getArea(App.CarPlate,App.AreaPolygonMD5)
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
                                    DLog.E("Error inside getArea", e);
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

    public void stopCommands(){
        if (commandDisposable != null)
            commandDisposable.dispose();
    }


    @Deprecated
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

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                            //
    //                                      TRIPS                                                 //
    //                                                                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    public void stopOpenTrip(){
        if (openTripDisposable != null)
            openTripDisposable.dispose();
    }


    @Deprecated
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

    @Deprecated
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

    @Deprecated
    public Observable<TripResponse> closeTrip(final Trip trip) {//TODO gestire la parte di salvataggio di begin_sent dentro la parte di comunicazione corsa conun concatMap avendo cura di passare il mDataManager
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

    @Deprecated
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

    @Deprecated
    public void sendEvent(final Event event){
        mDataManager.saveEvent(event)
                .concatMap(e -> mRemoteDataSource.sendEvent(e,mDataManager))
                .subscribeOn(Schedulers.io())
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

    private static boolean sendingEvents;

    @Deprecated
    public void sendEvents(final List<Event> event){
        if(!sendingEvents)
            Observable.interval(30,TimeUnit.SECONDS)
                    .take(event.size())
                    .concatMap(i->Observable.just(event.get(i.intValue())))
                    .concatMap(mDataManager::saveEvent)
            /*mDataManager.saveEvents(event)
                    .delay(10, TimeUnit.SECONDS)*/
                    .concatMap(e -> mRemoteDataSource.sendEvent(e,mDataManager))
                    .subscribeOn(Schedulers.io())
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

    @Deprecated
    public Observable<Reservation> getReservation(final String plate){

        return mRemoteDataSource.getReservation(App.CarPlate)
                .concatMap(reservations -> mDataManager.handleReservations(reservations))
                //.concatMap(Reservation::init)
                .doOnSubscribe(n -> {
                    //RxUtil.dispose(openTripDisposable);
                    //openTripDisposable = n;
                })
                .doOnError(e -> {
                    if(e instanceof ErrorResponse)
                        if(((ErrorResponse)e).errorType!= ErrorResponse.ErrorType.EMPTY)
                            DLog.E("Error insiede getReservation",e);
                    //RxUtil.dispose(openTripDisposable);
                })
                .doOnComplete(() ->{});

    }

    @Deprecated
    public void consumeReservation(final int reservation_id){

        mRemoteDataSource.consumeReservation(reservation_id)
                .subscribeOn(Schedulers.io())
                .subscribe(new Observer<Void>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Void v) {

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

    @Deprecated
    public void getPois(){
         mRemoteDataSource.getPois(mDataManager.getMaxPoiLastupdate())
                .concatMap(n -> mDataManager.savePoi(n))
                 .subscribeOn(Schedulers.io())
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

}
