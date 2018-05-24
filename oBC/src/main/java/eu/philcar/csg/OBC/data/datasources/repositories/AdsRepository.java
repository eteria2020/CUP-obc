package eu.philcar.csg.OBC.data.datasources.repositories;

import android.graphics.Bitmap;

import javax.inject.Inject;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.data.common.ErrorResponse;
import eu.philcar.csg.OBC.data.datasources.SharengoAdsDataSource;
import eu.philcar.csg.OBC.data.datasources.base.BaseRepository;
import eu.philcar.csg.OBC.data.model.AdsResponse;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.service.DataManager;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Fulvio on 17/05/2018.
 */

public class AdsRepository extends BaseRepository {

    private DataManager mDataManager;
    private SharengoAdsDataSource mRemoteDataSource;


    private CompositeDisposable mSubscriptions;

    @Inject
    public AdsRepository(DataManager mDataManager, SharengoAdsDataSource mRemoteDataSource) {
        this.mDataManager = mDataManager;
        this.mRemoteDataSource = mRemoteDataSource;
    }



    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                            //
    //                                      ADS                                                //
    //                                                                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////


    public void updateBannerStart(){
        String id = "1";
        String lat = "0.0";
        String lon = "0.0";
        String id_fleet = "1";
        String plate = "XH123KM";
        String index = null;
        String end = null;
        try{
            id = String.valueOf(App.currentTripInfo.customer.id);
            lat = String.valueOf(App.lastLocation!=null?App.lastLocation.getLatitude():0.0);
            lon = String.valueOf(App.lastLocation!=null?App.lastLocation.getLongitude():0.0);
            id_fleet = String.valueOf(App.FleetId);
            plate = App.CarPlate;
            index = mDataManager.getImageStart()!=null?mDataManager.getImageStart().getLastIndex():null;
            end = mDataManager.getImageStart()!=null? mDataManager.getImageStart().getLastEnd():null;
        }catch (Exception e){
            DLog.E("Exception while extracting params",e);
        }
         mRemoteDataSource.getBannerStart(id, lat, lon, id_fleet, plate, index, end)
                 .concatMap(adsResponse ->  mRemoteDataSource.updateImages(adsResponse))
                .subscribe(new Observer<AdsResponse>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(AdsResponse adsResponse) {

                mDataManager.saveBannerStart(adsResponse);
            }

            @Override
            public void onError(Throwable e) {
                DLog.E("Error insiede getBannerStart",((ErrorResponse)e).error);
            }

            @Override
            public void onComplete() {

            }
        });

    }


    public Observable<AdsResponse> updateBannerCars(){
        String id = "1";
        String lat = "0.0";
        String lon = "0.0";
        String id_fleet = "1";
        String plate = "XH123KM";
        String index = null;
        String end = null;
        try{
            id = String.valueOf(App.currentTripInfo.customer.id);
            lat = String.valueOf(App.lastLocation!=null?App.lastLocation.getLatitude():0.0);
            lon = String.valueOf(App.lastLocation!=null?App.lastLocation.getLongitude():0.0);
            id_fleet = String.valueOf(App.FleetId);
            plate = App.CarPlate;
            index = mDataManager.getImageCar()!=null?mDataManager.getImageCar().getLastIndex():null;
            end = mDataManager.getImageCar()!=null? mDataManager.getImageCar().getLastEnd():null;
        }catch (Exception e){
            DLog.E("Exception while extracting params",e);
        }
        //if(!RxUtil.isRunning(beaconDisposable)) {
         return mRemoteDataSource.getBannerCars(id, lat, lon, id_fleet, plate, index, end)
                 .concatMap(adsResponse ->  mRemoteDataSource.updateImages(adsResponse))
                 .doOnError(e->DLog.E("Error insiede getBannerCars",((ErrorResponse)e).error))
                 .doOnNext(adsResponse ->  mDataManager.saveBannerCar(adsResponse));

        //}
    }


    public void updateBannerEnd(){
        String id = "1";
        String lat = "0.0";
        String lon = "0.0";
        String id_fleet = "1";
        String plate = "XH123KM";
        String index = null;
        String end = null;
        try{
            id = String.valueOf(App.currentTripInfo.customer.id);
            lat = String.valueOf(App.lastLocation!=null?App.lastLocation.getLatitude():0.0);
            lon = String.valueOf(App.lastLocation!=null?App.lastLocation.getLongitude():0.0);
            id_fleet = String.valueOf(App.FleetId);
            plate = App.CarPlate;
            index = mDataManager.getImageEnd()!=null?mDataManager.getImageEnd().getLastIndex():null;
            end = mDataManager.getImageEnd()!=null? mDataManager.getImageEnd().getLastEnd():null;
        }catch (Exception e){
            DLog.E("Exception while extracting params",e);
        }

        //if(!RxUtil.isRunning(beaconDisposable)) {
         mRemoteDataSource.getBannerEnd(id, lat, lon, id_fleet, plate, index, end)
                 .concatMap(adsResponse ->  mRemoteDataSource.updateImages(adsResponse))
                .subscribe(new Observer<AdsResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(AdsResponse adsResponse) {
                        mDataManager.saveBannerEnd(adsResponse);
                    }

                    @Override
                    public void onError(Throwable e) {
                        DLog.E("Error insiede getBannerEnd",((ErrorResponse)e).error);
                    }

                    @Override
                    public void onComplete() {

                    }
                });

        //}
    }

    public Observable<Bitmap> getBannerStart() {
        return mDataManager.getBitmapStart()
                .observeOn(Schedulers.io());
    }

    public Observable<Bitmap> getBannerCar() {
        return mDataManager.getBitmapCar()
                .observeOn(Schedulers.io());
    }
    public Observable<Bitmap> getBannerEnd() {
        return mDataManager.getBitmapEnd()
                .observeOn(Schedulers.io());
    }
}
