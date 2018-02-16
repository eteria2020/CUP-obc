package eu.philcar.csg.OBC.data.datasources.repositories;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Singleton;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.data.datasources.SharengoDataSource;
import eu.philcar.csg.OBC.data.model.ConfigResponse;
import eu.philcar.csg.OBC.db.BusinessEmployee;
import eu.philcar.csg.OBC.db.Customer;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.RxUtil;
import eu.philcar.csg.OBC.service.DataManager;
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

    private Disposable customerDisposable;
    private Disposable employeeDisposable;
    private Disposable configDisposable;

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


    public void getCustomer(){

        if(!RxUtil.isRunning(customerDisposable)) {
            mRemoteDataSource.getCustomer(mDataManager.getMaxLastupdate())
                    .concatMap(n -> mDataManager.saveCustomer(n))
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Observer<Customer>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {
                            customerDisposable = d;
                        }

                        @Override
                        public void onNext(@NonNull Customer ribot) {
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            DLog.E("Error syncing.", e);
                            RxUtil.dispose(customerDisposable);
                        }

                        @Override
                        public void onComplete() {
                            DLog.I("Synced successfully!");
                            RxUtil.dispose(customerDisposable);
                        }
                    });
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
                    .subscribeOn(Schedulers.io())
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
                            DLog.E("Error syncing.", e);
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
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Observer<ConfigResponse>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {
                            configDisposable = d;
                        }

                        @Override
                        public void onNext(@NonNull ConfigResponse ribot) {
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            DLog.E("Error syncing.", e);
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

}
