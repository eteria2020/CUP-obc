package eu.philcar.csg.OBC.data.datasources.repositories;

import android.support.annotation.NonNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import eu.philcar.csg.OBC.data.datasources.SharengoDataSource;
import eu.philcar.csg.OBC.data.model.ResponseCustomer;
import eu.philcar.csg.OBC.db.Customer;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.RxUtil;
import eu.philcar.csg.OBC.service.DataManager;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Fulvio on 15/02/2018.
 */
@Singleton
public class SharengoRepository {
    private DataManager mDataManager;
    private SharengoDataSource mRemoteDataSource;

    private Disposable mDisposable;

    @Inject
    public SharengoRepository(DataManager mDataManager, SharengoDataSource mRemoteDataSource) {
        this.mDataManager = mDataManager;
        this.mRemoteDataSource = mRemoteDataSource;
    }

    public void stopRequest(){
        if (mDisposable != null)
            mDisposable.dispose();
    }


    public void getCustomer(){

        RxUtil.dispose(mDisposable);

        mRemoteDataSource.getCustomer(mDataManager.getMaxLastupdate())
                .concatMap(n-> mDataManager.saveCustomer(n)).subscribeOn(Schedulers.io())
                .subscribe(new Observer<Customer>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        mDisposable = d;
                    }

                    @Override
                    public void onNext(@NonNull Customer ribot) {
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        DLog.E( "Error syncing.",e);
                    }

                    @Override
                    public void onComplete() {
                        DLog.I("Synced successfully!");
                    }
                });
    }




}
