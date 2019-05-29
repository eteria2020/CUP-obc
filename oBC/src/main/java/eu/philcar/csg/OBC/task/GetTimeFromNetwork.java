package eu.philcar.csg.OBC.task;

import android.app.AlarmManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.SystemClock;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.service.CarInfo;

public class GetTimeFromNetwork extends AsyncTask<CarInfo, Void, Void> { // The call to network has to be in syncTask

	final String TIME_SERVER = "time.google.com"; // change the server here to fit what you need
	final String TIME_SERVER_IP = "193.204.114.232";
	private DLog dlog = new DLog(GetTimeFromNetwork.class);
	private Context mContext;

	private final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd.HHmmss", Locale.getDefault());

	public GetTimeFromNetwork(Context mContext) {
		this.mContext = mContext;
	}

	@Override
	protected Void doInBackground(CarInfo... carinfo) { // This will print the network time
		try {
			printTimes(carinfo[0]);
		} catch (Exception e) {
			dlog.e("Exception in printTime", e);
		}
		return null;
	}

	private void printTimes(CarInfo carInfo) {

		Runtime rt = Runtime.getRuntime();
		Date time = null;
		try {
			NTPUDPClient timeClient = new NTPUDPClient();
			InetAddress inetAddress = InetAddress.getByName(TIME_SERVER_IP);
			TimeInfo timeInfo = timeClient.getTime(inetAddress);
			long returnTime = timeInfo.getMessage().getTransmitTimeStamp().getTime();   //server time
			time = new Date(returnTime);
			dlog.i("getCurrentNetworkTime: Time: " + TIME_SERVER_IP + ": " + time);

		} catch (Exception e) {
			dlog.e("Exception while retrieving NTP time", e);
			if (carInfo != null && carInfo.intGpsLocation.getTime() > 1234567890) {
				time = new Date(carInfo.intGpsLocation.getTime() + SystemClock.elapsedRealtime() - carInfo.intGpsLocation.getElapsedRealtimeNanos() / 1000000);
				dlog.i("getCurrentNetworkTime: Time: GPS " + time);
			}
		}

		try {
			if (time != null && !(SystemClock.elapsedRealtime() > 15 * 60 * 1000 && System.currentTimeMillis() < 1234567890)) {
				rt.exec(new String[]{"/system/xbin/su", "-c", "date -s " + sdf.format(time) + ";\n"}); //
				rt.exec(new String[]{"/system/xbin/su", "-c", "settings put global auto_time 0"});
			} else
				rt.exec(new String[]{"/system/xbin/su", "-c", "settings put global auto_time 1"});

			rt.exec(new String[]{"/system/xbin/su", "-c", "settings put global auto_time_zone 0"}); //date -s 20120423.130000
			AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
			am.setTimeZone(App.timeZone);
		} catch (Exception e) {

			try {
				rt.exec(new String[]{"/system/xbin/su", "-c", "settings put global auto_time 1"});
			} catch (IOException e1) {
				dlog.e("Deeper Exception while retrieving GPS time", e);
			}
			dlog.e("Exception while retrieving GPS time", e);
		}

	}
}