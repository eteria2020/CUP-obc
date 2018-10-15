package eu.philcar.csg.OBC.db;

import android.content.Context;
import android.os.Handler;

import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;

import eu.philcar.csg.OBC.helpers.DLog;

public class Pois extends DbTable<Poi, Integer> {

	public Pois(ConnectionSource connectionSource, Class dataClass)
			throws SQLException {
		super(connectionSource, dataClass);
		// TODO Auto-generated constructor stub
	}

	public static Class GetRecordClass() {
		return Poi.class;
	}

	public void deleteAll() {
		try {
			this.executeRawNoArgs("DELETE  FROM poi");

		} catch (SQLException e) {
			DLog.E("deleteAll failed:", e);

		}
	}

	public boolean isPresent(int id, long update) {

		try {

			PreparedQuery<Poi> query = queryBuilder().setCountOf(true).where().eq("id", id).and().eq("update", update).prepare();
			long c = this.countOf(query);

			return c > 0;

		} catch (SQLException e) {
			DLog.E("isPresent fail:", e);

		}

		return false;
	}

	public List<Poi> getPois(String typeGroup) {

		try {

			PreparedQuery<Poi> query = queryBuilder().where().eq("type_group", typeGroup).prepare();

			return this.query(query);

		} catch (SQLException e) {
			DLog.E("getPois fail:", e);

		}

		return null;
	}

	public List<Poi> getCityPois(String city) {

		try {
			PreparedQuery<Poi> query;

			switch (city.toLowerCase()) {
				case "milano":
					query = queryBuilder().where().like("town", city.toLowerCase()).and().eq("attivo", true).and().like("type", "Isole Digitali").prepare();
					break;
				case "firenze":
					query = queryBuilder().where().like("town", city.toLowerCase()).and().eq("attivo", true).and().like("type", "Isole Digitali").prepare();
					break;
				/*case "roma":
					//query = queryBuilder().where().eq("citta", city.toLowerCase()).and().eq("attivo", true).and().ne("type", "Stazione ENEL Drive").prepare();
					break;*/
				default:
					return null;
				//query = queryBuilder().where().eq("citta", city.toLowerCase()).and().eq("attivo", true).prepare();
				//break;
			}

			return this.query(query);

		} catch (Exception e) {
			DLog.E("getPois fail:", e);

		}

		return null;
	}

	public long mostRecent() {
		long max = 0;
		try {

			max = this.queryRawValue("SELECT max(\"update\") FROM poi");

		} catch (SQLException e) {
			DLog.E("mostRecent fail:", e);

		}

		return max;
	}

	public long getSize() {
		long count = 0;
		try {
			count = this.queryRawValue("SELECT count(*) FROM poi");

		} catch (SQLException e) {
			DLog.E("getSize fail:", e);

		}

		return count;
	}

	@Deprecated
	public void startDownload(Context ctx, Handler handler) {
		Calendar calendar = Calendar.getInstance();
		int day = calendar.get(Calendar.DAY_OF_WEEK);
		if (day == Calendar.MONDAY) {
			DLog.D("Start pois download..");
//			PoisConnector cn = new PoisConnector();
//			cn.setLastUpdate(mostRecent());
//			HttpsConnector http = new HttpsConnector(ctx);
//			http.SetHandler(handler);
//			http.Execute(cn);
		}
	}

}
