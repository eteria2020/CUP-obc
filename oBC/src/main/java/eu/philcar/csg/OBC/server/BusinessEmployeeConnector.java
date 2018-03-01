package eu.philcar.csg.OBC.server;


import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.db.BusinessEmployee;
import eu.philcar.csg.OBC.db.BusinessEmployees;
import eu.philcar.csg.OBC.db.DbManager;
import eu.philcar.csg.OBC.helpers.DLog;
@Deprecated
public class BusinessEmployeeConnector implements RemoteEntityInterface {

	private DLog dlog = new DLog(this.getClass());

	public static BusinessEmployeeConnector GetDownloadConnector() {
		return new BusinessEmployeeConnector();
	}
	

	public int MsgId() {
		return Connectors.MSG_DN_AZIENDE;
	}
	
	public String GetRemoteUrl() {
		return App.URL_Dipendenti;
	}
	
	public int DecodeJson(String responseBody) {
		if (responseBody == null || responseBody.isEmpty()) {
			dlog.e("Empty response");
			return MsgId();
		}

		DbManager dbm = App.Instance.getDbManager();

		final BusinessEmployees dipendentiDao = dbm.getDipendentiDao();

		final JSONArray jArray;
		try {
			jArray = new JSONArray(responseBody);
		} catch (JSONException e) {
			dlog.e("Errore estraendo array json", e);
			return MsgId();
		}

		dlog.d("Downloaded " + jArray.length() + " records");
		long ts_begin = System.currentTimeMillis();
		try {
			dipendentiDao.callBatchTasks(new Callable<Void>() {

				@Override
				public Void call() throws Exception {

					dipendentiDao.deleteAll(); //delete everything since I just received the new list of employees

					int n = jArray.length();
					for (int i = 0; i < n; i++) {
						try {
							JSONObject jobj = jArray.getJSONObject(i);
							BusinessEmployee b = new BusinessEmployee();
							b.id = jobj.getInt("id");
							b.businessCode = jobj.getString("codice_azienda");
							b.isBusinessEnabled = jobj.getBoolean("azienda_abilitata");
							b.timeLimits = jobj.getString("limite_orari");

							try {
								dipendentiDao.createOrUpdate(b);
							} catch (SQLException e) {
								dlog.e("Insert or update:", e);
							}
						} catch (JSONException e) {
							dlog.e("Errore estraendo array json", e);

						}
					}
					return null;
				}

			});
		} catch (SQLException e1) {
			dlog.e("SQLException",e1);
		}


		long time = System.currentTimeMillis() - ts_begin;
		int receivedRecords = jArray.length();
		dlog.d("BusinessEmployee list parse: " + time +"ms for " + receivedRecords );

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
		return new ArrayList<>();
	}
}
