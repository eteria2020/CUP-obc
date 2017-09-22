package eu.philcar.csg.OBC.server;



import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.db.Customer;
import eu.philcar.csg.OBC.db.Customers;
import eu.philcar.csg.OBC.db.DbManager;
import eu.philcar.csg.OBC.helpers.DLog;
import android.os.Debug;
import android.text.format.DateFormat;

public class CustomersConnector implements RemoteEntityInterface {

	private DLog dlog = new DLog(this.getClass());
	
	public static  CustomersConnector GetDownloadConnector() {
		return new CustomersConnector();
	}
	
		
		
		private String cardCode = null;
		private long lastUpdate;
		private int  receivedRecords;

		private static boolean busy;
		
		public void setLastUpdate(long v) {
			this.lastUpdate = v;
		}
		
		public void setCardCodeQuery(String cardCode) {
			this.cardCode = cardCode;	
		}
		
		public int getReceivedRecords() {
			return receivedRecords;
		}
		
		public int MsgId() {
			return Connectors.MSG_DN_CLIENTI;
		}
		
		public String GetRemoteUrl() {
			if (!App.hasNetworkConnection) {
				dlog.w("Customers : No network");
				return null;
			}

			if (!busy) {
				busy=true;
				return App.URL_Clienti;
			} else {
				dlog.w("Customers : busy");
				return null;
			}
		}
	
	public int DecodeJson(String responseBody) {


		if (responseBody == null || responseBody.isEmpty()) {
			dlog.e("Empty response");
			return MsgId();
		}
			
		
		DbManager dbm = App.Instance.getDbManager();

		final Customers customers = dbm.getClientiDao();


		
		final JSONArray  jArray;
		try {
			jArray = new JSONArray(responseBody);
		} catch (JSONException e) {
			dlog.e("Errore estraendo array json", e);
			return MsgId();
		}
		
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

		
		
		dlog.d("Downloaded " +jArray.length() + " records");
		long ts_begin = System.currentTimeMillis();
		try {
			customers.callBatchTasks(new Callable<Void>() {

				@Override
				public Void call() throws Exception {
					String abilitato;
					int n = jArray.length();
					for (int i = 0; i < n; i++) {

						try {
							JSONObject jobj = jArray.getJSONObject(i);

							int id = jobj.getInt("id");
							long tms = jobj.getLong("tms");
							JSONArray ojarray = new JSONArray();

							// if (!clienti.isPresent(id, tms)) {
							Customer c = new Customer();
							c.id = id;
							c.name = jobj.getString("nome");
							c.surname = jobj.getString("cognome");
							c.language = jobj.getString("lingua");
							c.mobile = jobj.getString("cellulare");
							
							abilitato = jobj.getString("abilitato");
							if (abilitato!=null)
								c.enabled = abilitato.equalsIgnoreCase("TRUE");
							else
								c.enabled = false;
							
							c.info_display = jobj.getString("info_display");
							
							c.card_code = jobj.getString("codice_card");
							
							ojarray.put(jobj.getString("pin"));
							if (jobj.has("pin2")) {
							    String pin2 = jobj.getString("pin2");
							    if (pin2!=null && !pin2.isEmpty()) {
							    	ojarray.put(pin2);
							    }
							}

							c.pin = ojarray.toString();
							
							if (jobj.has("pins")) {
							    String pins = jobj.getString("pins");
							    if (pins!=null && !pins.isEmpty()) {
							    	c.pin = pins;
							    }
							}
							
							c.update_timestamp = tms;

							c.encrypt();
							
							try {
								customers.createOrUpdate(c);
							} catch (SQLException e) {
								dlog.e("Insert or update:", e);

							}
							// }
						} catch (JSONException e) {
							dlog.e("Errore estraendo array json", e);

						}

						
					}
					return null;
				}
				
			});
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	
		long time = System.currentTimeMillis() - ts_begin; 
		receivedRecords = jArray.length();
		dlog.d("WHITELIST PARSE: " + time +"ms for " + receivedRecords );

		App.whiteListSize = customers.getSize();

		busy = false;

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
			
			if (cardCode!=null) {
				list = new ArrayList<NameValuePair>();
				list.add(new BasicNameValuePair("cardcode", cardCode));
			} 
			
			if (lastUpdate > 0) {
				list = new ArrayList<NameValuePair>();
				list.add(new BasicNameValuePair("lastupdate", "" + lastUpdate));
			}
			
			return list;
		}
	

	
	
}
