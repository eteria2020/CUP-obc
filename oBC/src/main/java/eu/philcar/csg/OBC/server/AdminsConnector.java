package eu.philcar.csg.OBC.server;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.helpers.DLog;

@Deprecated
public class AdminsConnector implements RemoteEntityInterface {

	private DLog dlog = new DLog(this.getClass());

	public List<String> AdminsList = new ArrayList<>();

	public static AdminsConnector GetDownloadConnector() {
		return new AdminsConnector();
	}

	private String carPlate = null;

/*	public void setCarPlate(String plate) {
		this.carPlate = plate;
	}*/

	public int MsgId() {
		return Connectors.MSG_DN_ADMINS;
	}

	public String GetRemoteUrl() {
		return App.URL_Admins;
	}

	public int DecodeJson(String responseBody) {

		if (responseBody == null || responseBody.isEmpty()) {
			dlog.e("Empty response");
			return MsgId();
		}

		JSONArray jArray;
		try {

			jArray = new JSONArray(responseBody);
		} catch (JSONException e) {
			dlog.e("Errore estraendo array json", e);
			return MsgId();
		}

		int n = jArray.length();
		AdminsList.clear();

		dlog.d("Downloaded " + n + " records");

		for (int i = 0; i < n; i++) {

			try {
				JSONObject jobj = jArray.getJSONObject(i);
//				int id = jobj.getInt("id");
				String card = jobj.getString("card");
				AdminsList.add(card);

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

		if (carPlate != null) {
			list = new ArrayList<>();
			list.add(new BasicNameValuePair("car_plate", carPlate));
		}

		return list;
	}

}
