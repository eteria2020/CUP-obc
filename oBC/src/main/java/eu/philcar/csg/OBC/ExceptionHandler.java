package eu.philcar.csg.OBC;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.crashlytics.android.Crashlytics;

import org.acra.ACRA;

import java.lang.Thread.UncaughtExceptionHandler;

import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.StubActivity;

public class ExceptionHandler implements UncaughtExceptionHandler {

    private final static int RESTART_TIME_MILLIS = 100;

    private App mApp;

    public ExceptionHandler(App mApp) {
        this.mApp = mApp;
    }

    public void uncaughtException(Thread t, Throwable e) {

        // This code restarts the last activity
//	    PendingIntent pendingIntent = PendingIntent.getActivity(
//	    		mApp.getBaseContext(), 
//	    		0, 
//	    		new Intent(mApp.getCurrentActivity().getIntent()), 
//	    		mApp.getCurrentActivity().getIntent().getFlags());

        // This code restarts the ObcService

        PendingIntent pendingIntent = PendingIntent.getActivity(mApp.getBaseContext(), 0, new Intent(mApp.getApplicationContext(), StubActivity.class), 0);
        DLog.CR("Crash di OBC a causa di un errore in:");
        DLog.E("Uncaught Exception ", e);
        e.printStackTrace();
        Crashlytics.logException(e);
        ACRA.getErrorReporter().handleSilentException(e);

        AlarmManager mgr = (AlarmManager) mApp.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + RESTART_TIME_MILLIS, pendingIntent);

        System.exit(2);
    }
}
