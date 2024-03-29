package eu.philcar.csg.OBC;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import eu.philcar.csg.OBC.data.datasources.repositories.EventRepository;
import eu.philcar.csg.OBC.helpers.DLog;
//import eu.philcar.csg.OBC.service.ObcService;

public class SystemControl {

	public enum RebootCause {
		NO_3G(3 * 60 * 60 * 1000),
		AMP(45 * 60 * 1000),
		ADMIN(2 * 60 * 1000),
		DAILY(24 * 60 * 60 * 1000, "Reboot giornaliero");

		int timeout;
		String label;

		RebootCause(int timeout, String name) {
			this.timeout = timeout;
			this.label = name;
		}
		RebootCause(int timeout) {
			this.timeout = timeout;
		}
	}

	private static Map<RebootCause, Thread> rebootMap = new HashMap<>();


	private static DLog dlog = new DLog(SystemControl.class);

/*	public static int InsertAPN(Context ctx, String name) {

		//Set the URIs and variables
		int id = -1;
		boolean existing = false;
		final Uri APN_TABLE_URI = Uri.parse("content://telephony/carriers");
		final Uri PREFERRED_APN_URI = Uri.parse("content://telephony/carriers/preferapn");

		//Check if the specified APN is already in the APN table, if so skip the insertion
		Cursor parser = ctx.getContentResolver().query(APN_TABLE_URI, null, null, null, null);
		parser.moveToLast();
		while (parser.isBeforeFirst() == false) {
			int index = parser.getColumnIndex("name");
			String n = parser.getString(index);
			if (n.equals(name)) {
				existing = true;
				//Toast.makeText(getApplicationContext(), "APN already configured.",Toast.LENGTH_SHORT).show();
				break;
			}
			parser.moveToPrevious();
		}

		//if the entry doesn't already exist, insert it into the APN table
		if (!existing) {

			//Initialize the Content Resolver and Content Provider
			ContentResolver resolver = ctx.getContentResolver();
			ContentValues values = new ContentValues();

			//Capture all the existing field values excluding name
			Cursor apu = ctx.getContentResolver().query(PREFERRED_APN_URI, null, null, null, null);
			apu.moveToFirst();
			int index;

			index = apu.getColumnIndex("apn");
			String apn = apu.getString(index);
			index = apu.getColumnIndex("type");
			String type = apu.getString(index);
			index = apu.getColumnIndex("proxy");
			String proxy = apu.getString(index);
			index = apu.getColumnIndex("port");
			String port = apu.getString(index);
			index = apu.getColumnIndex("user");
			String user = apu.getString(index);
			index = apu.getColumnIndex("password");
			String password = apu.getString(index);
			index = apu.getColumnIndex("server");
			String server = apu.getString(index);
			index = apu.getColumnIndex("mmsc");
			String mmsc = apu.getString(index);
			index = apu.getColumnIndex("mmsproxy");
			String mmsproxy = apu.getString(index);
			index = apu.getColumnIndex("mmsport");
			String mmsport = apu.getString(index);
			index = apu.getColumnIndex("mcc");
			String mcc = apu.getString(index);
			index = apu.getColumnIndex("mnc");
			String mnc = apu.getString(index);
			index = apu.getColumnIndex("numeric");
			String numeric = apu.getString(index);

			//Assign them to the ContentValue object
			values.put("name", name); //the method parameter
			values.put("apn", "web.omnitel.it");
			values.put("type", "default,supl");
			values.put("proxy", "");
			values.put("port", "");
			values.put("user", "*");
			values.put("password", "*");
			values.put("server", "*");
			values.put("mmsc", "");
			values.put("mmsproxy", "");
			values.put("mmsport", "");
			values.put("mcc", "222");
			values.put("mnc", "10");
			//values.put("numeric", numeric);

			//Actual insertion into table
			Cursor c = null;
			try {
				Uri newRow = resolver.insert(APN_TABLE_URI, values);

				if (newRow != null) {
					c = resolver.query(newRow, null, null, null, null);
					int idindex = c.getColumnIndex("_id");
					c.moveToFirst();
					id = c.getShort(idindex);
				}
			} catch (SQLException e) {
			}
			if (c != null) c.close();
		}

		return id;
	}*/

	//Takes the ID of the new record generated in InsertAPN and sets that particular record the default preferred APN configuration
/*
	public static boolean SetPreferredAPN(Context ctx, int id) {

		//If the id is -1, that means the record was found in the APN table before insertion, thus, no action required
		if (id == -1) {
			return false;
		}

		Uri.parse("content://telephony/carriers");
		final Uri PREFERRED_APN_URI = Uri.parse("content://telephony/carriers/preferapn");

		boolean res = false;
		ContentResolver resolver = ctx.getContentResolver();
		ContentValues values = new ContentValues();

		values.put("apn_id", id);
		try {
			resolver.update(PREFERRED_APN_URI, values, null, null);
			Cursor c = resolver.query(PREFERRED_APN_URI, new String[]{"name", "apn"}, "_id=" + id, null, null);
			if (c != null) {
				res = true;
				c.close();
			}
		} catch (SQLException e) {
		}
		return res;
	}
*/

	public static void Reset3G(Context ctx) {

		Thread th = new Thread(new Restart3G());
		th.start();
	}

/*	public static void emfileException(Throwable e) {

//		doReboot("EMFILE");

	}*/

/*	public static void ResycNTP() {
		Thread th = new Thread(new RestartNTP());
		th.start();
	}*/

	private static int countFailedTests = 0;

	public static boolean hasNetworkConnection(Context ctx, EventRepository eventRepository) {
		if (ctx == null)
			return false;
		ConnectivityManager connectivityManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

		boolean ok = (activeNetworkInfo != null && App.isNetworkStable());

		if (ok) {
			if (countFailedTests > 0) {
				eventRepository.Restart3G(countFailedTests, activeNetworkInfo.getTypeName());
				countFailedTests = 0;
			}
		} else {
			countFailedTests++;
		}
		return ok;
	}

/*	public static void TestActiveNetworkConnection(Message msg, Context ctx, Handler hnd) {
		Thread th = new Thread(new TestConnection(msg, ctx, hnd));
		th.start();
	}*/

	private static class Restart3G implements Runnable {

		@Override
		public void run() {
			dlog.d("SystemControl.Restart3G();Begin restart 3G");
			dlog.cr("SystemControl.Restart3G();Inizio restart No3g mod aereo");
			Runtime rt = Runtime.getRuntime();
			try {
				rt.exec(new String[]{"/system/xbin/su", "-c", "settings put global airplane_mode_on 1"});
				Thread.sleep(2000);
				rt.exec(new String[]{"/system/xbin/su", "-c", "am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true"});
				dlog.d("SystemControl.Restart3G();Disabled 3G");
				Thread.sleep(15000);
				rt.exec(new String[]{"/system/xbin/su", "-c", "settings put global airplane_mode_on 0"});
				Thread.sleep(2000);
				rt.exec(new String[]{"/system/xbin/su", "-c", "am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true"});
				dlog.d("SystemControl.Restart3G();Enabled 3G");
				dlog.cr("SystemControl.Restart3G();Fine restart No3g mod aereo");
			} catch (IOException | InterruptedException e) {
				dlog.e("SystemControl.Restart3G();Restarting 3G", e);
			}

		}

	}

/*	private static class RestartNTP implements Runnable {

		@Override
		public void run() {
			dlog.d("SystemControl.RestartNTP();Begin restarting NTP");
			Runtime rt = Runtime.getRuntime();
			try {
				rt.exec(new String[]{"/system/xbin/su", "-c", "settings put global auto_time 0"});
				Thread.sleep(2000);
				rt.exec(new String[]{"/system/xbin/su", "-c", "settings put global auto_time 1"});
				dlog.d("SystemControl.RestartNTP();Restarted NTP");
			} catch (IOException | InterruptedException e) {
				dlog.e("SystemControl.RestartNTP();Restarting NTP", e);
			}

		}

	}*/

	public static class TestConnection implements Runnable {

		private Message msg;
		private Context ctx;
		private Handler hnd;
		@Inject
		EventRepository eventRepository;

		public TestConnection(Message msg, Context ctx, Handler hnd) {
			App.get(ctx).getComponent().inject(this);
			this.msg = msg;
			this.ctx = ctx;
			this.hnd = hnd;
		}

		@Override
		public void run() {

			if (msg == null)
				return;

			if (hasNetworkConnection(ctx, eventRepository)) {
				try {
					HttpURLConnection urlc = (HttpURLConnection) (new URL("http://www.google.com").openConnection());
					urlc.setRequestProperty("User-Agent", "Test");
					urlc.setRequestProperty("Connection", "close");
					urlc.setConnectTimeout(1500);
					urlc.connect();
					msg.arg1 = 1;
					urlc.disconnect();
				} catch (IOException e) {
					msg.arg1 = 0;
				}
			} else {
				msg.arg1 = 0;
			}
			if (msg != null && hnd != null)
				hnd.sendMessage(msg);
		}

	}

	private static long shutdownInProgress = 0;

	public static void doShutdown(long time) {
		//If there is another shutdown in progress not older than 60 sec : ignore
		if (System.currentTimeMillis() - shutdownInProgress > 60000) {
			Thread th = new Thread(new Shutdown(App.Instance.getApplicationContext(), time));
			th.start();
		} else {
			dlog.d("SystemControl.doShutdown();Shutdown already in progress");
		}
	}

	public static class Shutdown implements Runnable {

		@Inject
		EventRepository eventRepository;
		long time;

		public Shutdown(Context ctx, long time) {
			App.get(ctx).getComponent().inject(this);
			this.time = time;
		}

		@Override
		public void run() {
			DLog.D("SystemControl.Shutdown.run();Begin shutdown ");
			shutdownInProgress = System.currentTimeMillis();
			eventRepository.Shutdown();
			Runtime rt = Runtime.getRuntime();
			try {
				Thread.sleep(time);
				if(App.spegnimentoEnabled && !App.spegnimentoDisabled && (App.currentTripInfo==null || !App.currentTripInfo.isOpen) && (App.reservation== null || App.reservation.isMaintenance()) && !BuildConfig.BUILD_TYPE.equalsIgnoreCase("debug")) {

					DLog.D("SystemControl.Shutdown.run(); Shutting down ");
					rt.exec(new String[]{"/system/xbin/su", "-c", "reboot -p"});
				}
				else {
					shutdownInProgress = 0;
					eventRepository.ShutdownAbort();
					DLog.D("SystemControl.Shutdown.run();Spegnimento Aborted");
				}
			} catch (Exception  e) {
				dlog.e("SystemControl.Shutdown.run();Spegnimento Exception", e);
			}

		}
	}

	public static long rebootInProgress = 0;

	public static void ForceReboot() {
		Runtime rt = Runtime.getRuntime();
		try {
			rt.exec(new String[]{"/system/xbin/su", "-c", "reboot"});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Deprecated
	public static void doReboot(String label) {
		//If there is another reboot in progress not older than 6 hour : ignore
		if(BuildConfig.BUILD_TYPE.equalsIgnoreCase("debug")){
			return;
		}

		if (System.currentTimeMillis() - rebootInProgress > 21600000) {

			dlog.cr("SystemControl.Shutdown.doReboot();" + label);
			//Events.Reboot("No 3G Reboot");
			Thread th = new Thread(new Reboot());
			th.start();
		} else {
			if (System.currentTimeMillis() - rebootInProgress < 0 && System.currentTimeMillis() - App.AppStartupTime.getTime() > 3600000) { //if time is 01/01/2000 reboot every hour

				//Events.Reboot("No 3G Reboot");
				dlog.cr("SystemControl.Shutdown.doReboot();Eseguo reboot per " + label);
				Thread th = new Thread(new Reboot());
				th.start();
			} else{
				DLog.D("SystemControl.Shutdown.doReboot();Last Reboot within 6 hour, wait");
			}

		}
	}

	public static void doReboot(RebootCause label) {
		//check for last reboot time for label
		if( BuildConfig.BUILD_TYPE.equalsIgnoreCase("debug") || (rebootMap!= null && rebootMap.containsKey(label))){
			return;
		}

		if(System.currentTimeMillis() - App.Instance.getRebootTimeForLabel(label.name()) >0) {// se maggiore di 0 l'ultimo reboot non è nel futuro posso procedere nel reboot
			if(System.currentTimeMillis() - App.Instance.getRebootTimeForLabel(label.name())> label.timeout){
				dlog.cr("SystemControl.Shutdown.doReboot();Eseguo reboot per " + label.label);
				//Events.Reboot("No 3G Reboot");
				Thread th = new Thread(new Reboot(2 * 60 * 1000, label.name()));
				th.start();
				try {
					rebootMap.put(label, th);
				}catch (Exception e){
					dlog.e("SystemControl.Shutdown.doReboot();Exception while saving reboot Thread",e);
				}
			}
		}else { //sono nel passato procedo aspettando 5 min
			dlog.cr("SystemControl.Shutdown.doReboot();Eseguo reboot per " + label.label);
			Thread th = new Thread(new Reboot(5*60*1000, label.name()));
			th.start();
		}
	}

	public static void cancelRebootCause(RebootCause cause){
		try{
			if(rebootMap != null && rebootMap.containsKey(cause)){
				rebootMap.get(cause).interrupt();
				rebootMap.remove(cause);
			}

		}catch (Exception e) {
		    dlog.e("SystemControl.cancelRebootCause();Exception", e);
		}
	}

	private static class Reboot implements Runnable {
		long sleep;
		String label;

		private Reboot(long sleepMillis, String label) {
			this.sleep = sleepMillis;
			this.label = label;
		}

		private Reboot() {
			this(50000, null);
		}

		@Override
		public void run() {
			try {

				DLog.D("SystemControl.Reboot.run();begin reboot");

				Thread.sleep(sleep);
				if ((App.currentTripInfo == null || !App.currentTripInfo.isOpen) && (App.reservation == null || App.reservation.isMaintenance())) {
					//	Events.Reboot();
					Runtime rt = Runtime.getRuntime();
					rebootInProgress = System.currentTimeMillis();
					App.Instance.persistRebootTime();
					App.Instance.setRebootTimeForLabel(label);
					rt.exec(new String[]{"/system/xbin/su", "-c", "reboot"});
				} else {

					DLog.D("SystemControl.Reboot.run();Abort reboot: Trip open!!! ");
				}
			} catch (IOException | InterruptedException e) {
				dlog.e("SystemControl.Reboot.run();", e);
			}
		}
	}
}
