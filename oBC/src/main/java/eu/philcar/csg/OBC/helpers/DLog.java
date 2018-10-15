package eu.philcar.csg.OBC.helpers;

import android.util.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.philcar.csg.OBC.App;

public class DLog {
    private static final String LOGTAG = "OBC";
    private static final Logger Log4j = LoggerFactory.getLogger("OBC");
    private static final Logger LogCr = LoggerFactory.getLogger("ControlRoom");

    /**
     * Logging functions to generate ADB logcat messages.
     */

    public static final void E(String nMessage) {
        Log.e(LOGTAG, nMessage);
        if (App.saveLog)
            Log4j.error(nMessage);
    }

    public static final void E(String nMessage, Throwable tr) {
        Log.e(LOGTAG, nMessage, tr);
        if (App.saveLog)
            Log4j.error(nMessage, tr);
    }

    public static final void W(String nMessage) {
        Log.w(LOGTAG, nMessage);
        if (App.saveLog)
            Log4j.warn(nMessage);
    }

    public static final void D(String nMessage) {
        Log.d(LOGTAG, nMessage);
        if (App.saveLog)
            Log4j.debug(nMessage);
    }

    public static final void I(String nMessage) {
        Log.i(LOGTAG, nMessage);
        if (App.saveLog)
            Log4j.info(nMessage);
    }

    public static final void CR(String nMessage) {
        Log.d("ControlRoom", nMessage);
        if (App.saveLog)
            LogCr.debug(nMessage);
    }

    private String logtag = "OBC";
    private Logger log4j;

    public DLog(Class<?> cls) {
        log4j = LoggerFactory.getLogger(cls);
        logtag = "OBC-" + cls.getSimpleName();
    }

    public final void e(String nMessage) {
        Log.e(logtag, nMessage);
        if (App.saveLog)
            log4j.error(nMessage);
    }

    public final void e(String nMessage, Throwable tr) {
        Log.e(logtag, nMessage, tr);
        if (App.saveLog)
            log4j.error(nMessage, tr);
    }

    public final void w(String nMessage) {
        Log.w(logtag, nMessage);
        if (App.saveLog)
            log4j.warn(nMessage);
    }

    public final void d(String nMessage) {
        Log.d(logtag, nMessage);
        if (App.saveLog)
            log4j.debug(nMessage);
    }

    public final void i(String nMessage) {
        Log.i(logtag, nMessage);
        if (App.saveLog)
            log4j.info(nMessage);
    }

    public final void cr(String nMessage) {
        Log.d("ControlRoom", nMessage);
        if (App.saveLog)
            LogCr.debug(nMessage);
    }

}
