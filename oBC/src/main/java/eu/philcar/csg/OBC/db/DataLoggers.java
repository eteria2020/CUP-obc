package eu.philcar.csg.OBC.db;

import android.util.Log;

import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.support.ConnectionSource;

import org.json.JSONObject;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static eu.philcar.csg.OBC.service.ObcService.Data.AMPER;
import static eu.philcar.csg.OBC.service.ObcService.Data.KM;
import static eu.philcar.csg.OBC.service.ObcService.Data.KM_FROM_TRIP_BEG;
import static eu.philcar.csg.OBC.service.ObcService.Data.SOC;
import static eu.philcar.csg.OBC.service.ObcService.Data.TIME;
import static eu.philcar.csg.OBC.service.ObcService.Data.V_BATTERY;
import static eu.philcar.csg.OBC.service.ObcService.Data.V_MAX_CELL;
import static eu.philcar.csg.OBC.service.ObcService.Data.V_MIN_CELL;

/**
 * Created by Fulvio on 01/10/2018.
 */

public class DataLoggers extends DbTable<DataLogger, Integer> {
	private static final String TAG = DataLoggers.class.getSimpleName();

	public DataLoggers(ConnectionSource connectionSource, Class dataClass)
			throws SQLException {
		super(connectionSource, dataClass);
		// TODO Auto-generated constructor stub
	}

	public static Class GetRecordClass() {
		return DataLogger.class;
	}

	public DataLogger saveLog(JSONObject json) {

		DataLogger log = buildLogFromJson(json);
		try {
			create(log);

		} catch (Exception e) {
			Log.e(TAG, "saveLog: Exception", e);
		}
		return log;
	}

	private DataLogger buildLogFromJson(JSONObject json) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
		DataLogger log = new DataLogger();
		try {
			log.ampere = Integer.parseInt(json.optString(AMPER.toString(), "0").replace(",", "."));
			log.km = (long) Float.parseFloat(json.optString(KM.toString(), "0").replace(",", "."));
			log.km_from_trip_beg = (long) Float.parseFloat(json.optString(KM_FROM_TRIP_BEG.toString(), "0").replace(",", "."));
			log.soc = Integer.parseInt(json.optString(SOC.toString(), "0"));
			log.time = sdf.parse(json.optString(TIME.toString(), "").replaceAll("T", " "));
			log.v_battery = Float.parseFloat(json.optString(V_BATTERY.toString(), "0.0").replace(",", "."));
			log.v_max_cell = Float.parseFloat(json.optString(V_MAX_CELL.toString(), "0.0").replace(",", "."));
			log.v_min_cell = Float.parseFloat(json.optString(V_MIN_CELL.toString(), "0.0").replace(",", "."));
		} catch (Exception e) {
			Log.e(TAG, "saveLog: Exception", e);
		}
		return log;
	}

	public void removeOldLog(Date date) {
		try {
			DeleteBuilder<DataLogger, Integer> deleteBuilder = deleteBuilder();
			deleteBuilder.where().le("time", date);
			delete(deleteBuilder.prepare());
		} catch (Exception e) {
			Log.e(TAG, "removeOldLog: Exception", e);
		}
	}
}
