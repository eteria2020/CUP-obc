package eu.philcar.csg.OBC.data.datasources.repositories;

import android.support.annotation.NonNull;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.data.datasources.SharengoPhpDataSource;
import eu.philcar.csg.OBC.data.model.AreaResponse;
import eu.philcar.csg.OBC.data.model.EventResponse;
import eu.philcar.csg.OBC.data.model.TripResponse;
import eu.philcar.csg.OBC.db.Event;
import eu.philcar.csg.OBC.db.Trip;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.RxUtil;
import eu.philcar.csg.OBC.server.ServerCommand;
import eu.philcar.csg.OBC.service.DataManager;
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

    public void stopCustomer(){
        if (areaDisposable != null)
            areaDisposable.dispose();
    }


    public void getArea(){

        if(!RxUtil.isRunning(areaDisposable)) {
            mRemoteDataSource.getArea(App.CarPlate,App.AreaPolygonMD5)
                    .doOnNext(n -> mDataManager.saveArea(n))
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Observer<List<AreaResponse>>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {
                            areaDisposable = d;
                        }

                        @Override
                        public void onNext(@NonNull List<AreaResponse> ribot) {
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            DLog.E("Error syncing.", e);
                            RxUtil.dispose(areaDisposable);
                        }

                        @Override
                        public void onComplete() {
                            DLog.I("Synced successfully!");
                            RxUtil.dispose(areaDisposable);
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
        if (commandDisposable != null)
            commandDisposable.dispose();
    }


    public Observable<ServerCommand> getCommands(String plate) {

        return  mRemoteDataSource.getCommands(plate)
                .flatMap(n -> mDataManager.handleCommands(n))
                .doOnSubscribe(n -> {
                    //RxUtil.dispose(commandDisposable);
                    commandDisposable = n;})
                .doOnError(e -> {
                    DLog.E("Error insiede GetCommand",e);
                    RxUtil.dispose(commandDisposable);})
                .doOnComplete(() -> RxUtil.dispose(commandDisposable));


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


    public Observable<TripResponse> openTrip(final Trip trip, final TripInfo tripInfo) {
           return mDataManager.saveTrip(trip)
                    .concatMap(n -> mRemoteDataSource.openTrip(n, mDataManager))
                   .doOnSubscribe(n -> {
                       //RxUtil.dispose(openTripDisposable);
                       //openTripDisposable = n;
                   })
                   .doOnError(e -> {
                       DLog.E("Error insiede GetCommand",e);
                       RxUtil.dispose(openTripDisposable);})
                   .doOnComplete(() -> RxUtil.dispose(openTripDisposable));




    }

    public Observable<TripResponse> updateServerTripData(Trip trip){
        return mRemoteDataSource.updateTrip(trip)
                .doOnSubscribe(n -> {
                    //RxUtil.dispose(openTripDisposable);
                    //openTripDisposable = n;
                })
                .doOnError(e -> {
                    DLog.E("Error insiede GetCommand",e);
                    RxUtil.dispose(openTripDisposable);})
                .doOnComplete(() -> RxUtil.dispose(openTripDisposable));
    }


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
                    DLog.E("Error insiede GetCommand",e);
                    RxUtil.dispose(openTripDisposable);})
                .doOnComplete(() -> RxUtil.dispose(openTripDisposable));




    }

    public Observable<TripResponse> closeTrips(final Collection<Trip> trip) {//TODO gestire la parte di salvataggio di begin_sent dentro la parte di comunicazione corsa conun concatMap avendo cura di passare il mDataManager
        return mDataManager.saveTrips(trip)
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
                    DLog.E("Error insiede GetCommand",e);
                    RxUtil.dispose(openTripDisposable);})
                .doOnComplete(() -> RxUtil.dispose(openTripDisposable));
    }

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

    public void sendEvents(final Collection<Event> event){
        mDataManager.saveEvents(event)
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
}
