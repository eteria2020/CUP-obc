package eu.philcar.csg.OBC.helpers;

import android.util.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import eu.philcar.csg.OBC.App;

public class DLog {
	private static final String LOGTAG = "OBC";
	private static final Logger Log4j = LoggerFactory.getLogger("OBC");
	private static final Logger LogCr = LoggerFactory.getLogger("ControlRoom");

	private String logtag;
	private Logger log4j;

	/**
	 * Logging functions to generate ADB logcat messages.
	 */

	public DLog(Class<?> cls) {
		log4j = LoggerFactory.getLogger(cls);
		logtag = "OBC-" + cls.getSimpleName();
	}

	public final void e(String nMessage) {
		String msg = this.getNow() + ";" + nMessage;
		Log.e(logtag, msg);
		if (App.saveLog)
			log4j.error(msg);
	}

	public final void e(String nMessage, Throwable tr) {
		String msg = this.getNow() + ";" + nMessage;
		Log.e(logtag, msg, tr);
		if (App.saveLog)
			log4j.error(msg, tr);
	}

	public final void w(String nMessage) {
		String msg = this.getNow() + ";" + nMessage;
		Log.w(logtag, msg);
		if (App.saveLog)
			log4j.warn(msg);
	}

	public final void d(String nMessage) {
		String msg = this.getNow() + ";" + nMessage;
		Log.d(logtag, msg);
		if (App.saveLog)
			log4j.debug(msg);
	}

	public final void i(String nMessage) {
		String msg = this.getNow() + ";" + nMessage;
		Log.i(logtag, msg);
		if (App.saveLog)
			log4j.info(msg);
	}

	public final void cr(String nMessage) {
		String msg = this.getNow() + ";" + nMessage;
		Log.d("ControlRoom", msg);
		if (App.saveLog)
			LogCr.debug(msg);
	}

	private String getNow() {
		Calendar c = Calendar.getInstance();
		SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
		return df.format(c.getTime());
	}

	/* STATIC */

	public static void E(String nMessage) {
		String msg = DLog.GetNow() + ";" + nMessage;
		Log.e(LOGTAG, msg);
		if (App.saveLog)
			Log4j.error(msg);
	}

	public static void E(String nMessage, Throwable tr) {
		String msg = DLog.GetNow() + ";" + nMessage;
		Log.e(LOGTAG, msg, tr);
		if (App.saveLog)
			Log4j.error(msg, tr);
	}

	public static void W(String nMessage) {
		String msg = DLog.GetNow() + ";" + nMessage;
		Log.w(LOGTAG, msg);
		if (App.saveLog)
			Log4j.warn(msg);
	}

	public static void D(String nMessage) {
		String msg = DLog.GetNow() + ";" + nMessage;
		Log.d(LOGTAG, msg);
		if (App.saveLog)
			Log4j.debug(msg);
	}

	public static void I(String nMessage) {
		String msg = DLog.GetNow() + ";" + nMessage;
		Log.i(LOGTAG, msg);
		if (App.saveLog)
			Log4j.info(msg);
	}

	public static void CR(String nMessage) {
		String msg = DLog.GetNow() + ";" + nMessage;
		Log.d("ControlRoom", msg);
		if (App.saveLog)
			LogCr.debug(msg);
	}

	static private String GetNow() {
		Calendar c = Calendar.getInstance();
		SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
		return df.format(c.getTime());
	}
}
