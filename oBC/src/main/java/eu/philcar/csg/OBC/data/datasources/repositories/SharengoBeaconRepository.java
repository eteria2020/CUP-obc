package eu.philcar.csg.OBC.data.datasources.repositories;

import android.support.annotation.NonNull;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.data.common.ErrorResponse;
import eu.philcar.csg.OBC.data.datasources.SharengoBeaconDataSource;
import eu.philcar.csg.OBC.data.model.AreaResponse;
import eu.philcar.csg.OBC.data.model.BeaconResponse;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.RxUtil;
import eu.philcar.csg.OBC.service.DataManager;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Fulvio on 01/03/2018.
 */
@Singleton
public class SharengoBeaconRepository {

    private DataManager mDataManager;
    private SharengoBeaconDataSource mRemoteDataSource;

    private Disposable beaconDisposable;

    @Inject
    public SharengoBeaconRepository(DataManager mDataManager, SharengoBeaconDataSource mRemoteDataSource) {
        this.mDataManager = mDataManager;
        this.mRemoteDataSource = mRemoteDataSource;
    }



    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                            //
    //                                      BEACON                                                //
    //                                                                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    public void stopBeacon(){
        if (beaconDisposable != null)
            beaconDisposable.dispose();
    }


    public Observable<BeaconResponse> sendBeacon(String beacon){

        //if(!RxUtil.isRunning(beaconDisposable)) {
          return  mRemoteDataSource.sendBeacon(App.CarPlate,beacon)
                    //.doOnNext(n -> mDataManager.saveArea(n))
                  .doOnSubscribe(n -> {
                      //RxUtil.dispose(commandDisposable);
                      beaconDisposable = n;})
                  .doOnError(e -> {
                      DLog.E("Error insiede sendBeacon",((ErrorResponse)e).error);
                      RxUtil.dispose(beaconDisposable);})
                  .doOnComplete(() -> RxUtil.dispose(beaconDisposable));

        //}
    }





}
