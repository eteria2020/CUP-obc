package eu.philcar.csg.OBC.server;



import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.db.Customer;
import eu.philcar.csg.OBC.db.Customers;
import eu.philcar.csg.OBC.db.DbManager;
import eu.philcar.csg.OBC.db.Poi;
import eu.philcar.csg.OBC.db.Pois;
import eu.philcar.csg.OBC.helpers.DLog;
import android.os.Debug;
import android.text.format.DateFormat;

public class PoisConnector implements RemoteEntityInterface {

	private DLog dlog = new DLog(this.getClass());
	
	public static  PoisConnector GetDownloadConnector() {
		return new PoisConnector();
	}
	
		
			
		
		
		

		private long lastUpdate;
		
		public void setLastUpdate(long v) {
			this.lastUpdate = v;
		}
		
		
		public int MsgId() {
			return Connectors.MSG_DN_POIS;
		}
		
		public String GetRemoteUrl() {
			return App.URL_Pois;
		}
	
	public int DecodeJson(String responseBody) {

		DbManager dbm = App.Instance.getDbManager();

		Pois pois = dbm.getPoisDao();

		if (responseBody==null || responseBody.isEmpty()) {
			dlog.e("Empty json");
			return  MsgId();
		}
		
		JSONArray jArray;
		try {

			jArray = new JSONArray(responseBody);
		} catch (JSONException e) {
			dlog.e("Errore estraendo array json", e);
			return MsgId();
		}
		

		int n = jArray.length();
		
		dlog.d("Downloaded " +n + " records");
		
		for (int i = 0; i < n; i++) {

			try {
				JSONObject jobj = jArray.getJSONObject(i);

				int id = jobj.getInt("id");


				// if (!clienti.isPresent(id, tms)) {
				Poi c = new Poi();
				c.id = id;
				c.tipo = jobj.getString("type");
				c.codice= jobj.getString("code");
				c.via = jobj.getString("address");
				c.citta = jobj.getString("town");
				c.cap = jobj.getString("zip_code");
				c.provincia =  jobj.getString("province");
				c.attivo = true;
				c.lon = jobj.getDouble("lon");
				c.lat = jobj.getDouble("lat");
				c.aggiornamento = jobj.getLong("update");
				
				try {
					pois.createOrUpdate(c);
				} catch (SQLException e) {
					dlog.e("Insert or update:", e);

				}
				// }
			} catch (JSONException e) {
				dlog.e("Errore estraendo array json", e);

			}

			
		}

		return MsgId();
	}

		public eDirection getDirection() {
			return eDirection.DOWNLOAD;
		}

		public String EncodeJson() {
			
			return null;
		}

		@Override
		public List<NameValuePair> GetParams() {
			
			ArrayList<NameValuePair> list = null;

			
			if (lastUpdate > 0) {
				list = new ArrayList<NameValuePair>();
				list.add(new BasicNameValuePair("aggiornamento", "" + lastUpdate));
			}
			
			return list;
		}
	

	
	
}
