package eu.philcar.csg.OBC.service;

import android.app.IntentService;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.db.Customer;
import eu.philcar.csg.OBC.helpers.AndroidComponentUtil;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.RxUtil;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class SyncService extends Service {

    @Inject DataManager mDataManager;
    private Disposable mDisposable;
    private final DLog dlog = new DLog(this.getClass());

    public static Intent getStartIntent(Context context) {
        return new Intent(context, SyncService.class);
    }

    public static boolean isRunning(Context context) {
        return AndroidComponentUtil.isServiceRunning(context, SyncService.class);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        App.get(this).getComponent().inject(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        dlog.i("Starting sync...");

        if (!App.hasNetworkConnection()) {
            dlog.i("Sync canceled, connection not available");
            AndroidComponentUtil.toggleComponent(this, SyncOnConnectionAvailable.class, true);
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        RxUtil.dispose(mDisposable);

        Observable.interval(30, TimeUnit.SECONDS)
                .flatMap(n -> mDataManager.syncCustomer())
                .subscribeOn(Schedulers.io())
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
                        dlog.e( "Error syncing.",e);
                        stopSelf(startId);
                    }

                    @Override
                    public void onComplete() {
                        dlog.i("Synced successfully!");
                        stopSelf(startId);
                    }
                });


        /*mDataManager.syncRecipe().subscribeOn(Schedulers.io())
                .subscribe(new Observer<Recipe>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        mDisposable = d;
                    }

                    @Override
                    public void onNext(@NonNull Recipe ribot) {
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Timber.w(e, "Error syncing.");
                        stopSelf(startId);
                    }

                    @Override
                    public void onComplete() {
                        Timber.i("Synced successfully!");
                        stopSelf(startId);
                    }
                });;*/
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mDisposable != null) mDisposable.dispose();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static class SyncOnConnectionAvailable extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)
                    && App.hasNetworkConnection()) {
                DLog.I("Connection is now available, triggering sync...");
                AndroidComponentUtil.toggleComponent(context, this.getClass(), false);
                context.startService(getStartIntent(context));
            }
        }
    }


}
