package eu.philcar.csg.OBC.data.datasources.repositories;

import javax.inject.Inject;
import javax.inject.Singleton;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.data.common.ErrorResponse;
import eu.philcar.csg.OBC.data.datasources.SharengoBeaconDataSource;
import eu.philcar.csg.OBC.data.datasources.base.BaseRepository;
import eu.philcar.csg.OBC.data.model.BeaconResponse;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.service.DataManager;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;

/**
 * Created by Fulvio on 01/03/2018.
 */
@Singleton
public class SharengoBeaconRepository extends BaseRepository {

    private DataManager mDataManager;
    private SharengoBeaconDataSource mRemoteDataSource;

    private CompositeDisposable mSubscriptions;

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


    public Observable<BeaconResponse> sendBeacon(String beacon){

        //if(!RxUtil.isRunning(beaconDisposable)) {
          return  mRemoteDataSource.sendBeacon(App.CarPlate,beacon)
                    //.doOnNext(n -> mDataManager.saveArea(n))
                  .doOnError(e -> {
                      DLog.E("Error insiede sendBeacon",((ErrorResponse)e).error);
                      //RxUtil.dispose(beaconDisposable);
                  })
                  .doOnComplete(() -> {
                      //RxUtil.dispose(beaconDisposable);

                  });

        //}
    }





}
