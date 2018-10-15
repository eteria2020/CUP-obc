package eu.philcar.csg.OBC.server;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.db.Trip;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.interfaces.OnTripCallback;
import eu.philcar.csg.OBC.service.TripInfo;

@Deprecated
public class TripsConnector implements RemoteEntityInterface {

	private DLog dlog = new DLog(this.getClass());

	private static boolean busy;

	public final TripInfo tripInfo;

	private final OnTripCallback callback;

	public TripsConnector(TripInfo tripInfo, OnTripCallback callback) {
		this.tripInfo = tripInfo;
		this.callback = callback;
	}

	public TripsConnector(TripInfo tripInfo) {
		this.tripInfo = tripInfo;
		this.callback = null;
	}

	public int MsgId() {
		if (tripInfo != null && tripInfo.trip != null && tripInfo.trip.offline)
			return Connectors.MSG_TRIPS_SENT_OFFLINE;
		else
			return Connectors.MSG_TRIPS_SENT_REALTIME;
	}

	public String GetRemoteUrl() {

		if (!App.hasNetworkConnection()) {
			dlog.w("Trips : No network");
			return null;
		}

		if (!busy) {
			busy = true;
			return App.URL_Corse;
		} else {
			dlog.w("Trips : busy");
			return null;
		}

	}

	public List<NameValuePair> GetParams() {

		ArrayList<NameValuePair> list = new ArrayList<NameValuePair>();

		Trip corsa = tripInfo.trip;

		if (corsa == null) {
			dlog.e("GetParams, corsa == null");
			return list;
		}

		try {
			if (!corsa.begin_sent) {
				dlog.d("GetParams, sending apertura tripInfo:" + tripInfo.toString());
				list.add(new BasicNameValuePair("cmd", "1"));

				list.add(new BasicNameValuePair("id_veicolo", corsa.plate + ""));
				list.add(new BasicNameValuePair("id_cliente", corsa.id_customer + ""));
				list.add(new BasicNameValuePair("ora", corsa.begin_timestamp + ""));
				list.add(new BasicNameValuePair("km", corsa.begin_km + ""));
				list.add(new BasicNameValuePair("carburante",
						corsa.begin_battery + ""));
				list.add(new BasicNameValuePair("lon", corsa.begin_lon + ""));
				list.add(new BasicNameValuePair("lat", corsa.begin_lat + ""));
				list.add(new BasicNameValuePair("warning", corsa.warning));
				list.add(new BasicNameValuePair("pulizia_int", corsa.int_cleanliness + ""));
				list.add(new BasicNameValuePair("pulizia_ext", corsa.ext_cleanliness + ""));
				list.add(new BasicNameValuePair("mac", App.MacAddress));
				list.add(new BasicNameValuePair("imei", App.IMEI));
				list.add(new BasicNameValuePair("n_pin", corsa.n_pin + ""));
				if (corsa.getId_parent() != 0)
					list.add(new BasicNameValuePair("id_parent", corsa.getId_parent() + ""));
			} else if (!corsa.end_sent && corsa.end_timestamp > 0) {

				dlog.d("GetParams, sending chiusura tripInfo:" + tripInfo.toString());
				list.add(new BasicNameValuePair("cmd", "2"));
				list.add(new BasicNameValuePair("id", corsa.remote_id + ""));
				list.add(new BasicNameValuePair("id_veicolo", corsa.plate + ""));
				list.add(new BasicNameValuePair("id_cliente", corsa.id_customer + ""));
				list.add(new BasicNameValuePair("ora", corsa.end_timestamp + ""));
				list.add(new BasicNameValuePair("km", corsa.end_km + ""));
				list.add(new BasicNameValuePair("carburante",
						corsa.end_battery + ""));
				list.add(new BasicNameValuePair("lon", corsa.end_lon + ""));
				list.add(new BasicNameValuePair("lat", corsa.end_lat + ""));
				list.add(new BasicNameValuePair("warning", corsa.warning));
				list.add(new BasicNameValuePair("pulizia_int", corsa.int_cleanliness + ""));
				list.add(new BasicNameValuePair("pulizia_ext", corsa.ext_cleanliness + ""));
				list.add(new BasicNameValuePair("park_seconds", corsa.park_seconds + ""));
				list.add(new BasicNameValuePair("n_pin", corsa.n_pin + ""));
				if (corsa.getId_parent() != 0)
					list.add(new BasicNameValuePair("id_parent", corsa.getId_parent() + ""));
			} else {
				dlog.e("Case not handled sending opening: " + corsa.toString());
				dlog.d("GetParams, sending apertura tripInfo:" + tripInfo.toString());
				tripInfo.trip.begin_sent = false;
				tripInfo.UpdateCorsa();
				list.add(new BasicNameValuePair("cmd", "1"));

				list.add(new BasicNameValuePair("id_veicolo", corsa.plate + ""));
				list.add(new BasicNameValuePair("id_cliente", corsa.id_customer + ""));
				list.add(new BasicNameValuePair("ora", corsa.begin_timestamp + ""));
				list.add(new BasicNameValuePair("km", corsa.begin_km + ""));
				list.add(new BasicNameValuePair("carburante",
						corsa.begin_battery + ""));
				list.add(new BasicNameValuePair("lon", corsa.begin_lon + ""));
				list.add(new BasicNameValuePair("lat", corsa.begin_lat + ""));
				list.add(new BasicNameValuePair("warning", corsa.warning));
				list.add(new BasicNameValuePair("pulizia_int", corsa.int_cleanliness + ""));
				list.add(new BasicNameValuePair("pulizia_ext", corsa.ext_cleanliness + ""));
				list.add(new BasicNameValuePair("mac", App.MacAddress));
				list.add(new BasicNameValuePair("imei", App.IMEI));
				list.add(new BasicNameValuePair("n_pin", corsa.n_pin + ""));
				if (corsa.getId_parent() != 0)
					list.add(new BasicNameValuePair("id_parent", corsa.getId_parent() + ""));
			}

			for (NameValuePair pair : list) {
				dlog.i(pair.getName() + ":" + pair.getValue());
			}
		} catch (Exception e) {
			dlog.e("Getparams, exception", e);
		}

		return list;

	}

	public int DecodeJson(String responseBody) {

		try {

			if (responseBody == null || responseBody.isEmpty()) {
				tripInfo.trip.recharge = 0; //server result
				tripInfo.trip.offline = true;
				tripInfo.setTxOffline();
				tripInfo.UpdateCorsa();
				dlog.w("No response from server, keeping info off-line");
				busy = false;
				return MsgId();
			}

			dlog.i("DecodeJson: " + responseBody);
			JSONObject jobj = new JSONObject(responseBody);

			int result = jobj.getInt("result");
			String caption = jobj.getString("message");

			dlog.i("DecodeJson: result " + result + " from trip: " + tripInfo.trip.toString());
			dlog.i("DecodeJson: caption " + (caption != null ? caption : "NULL"));

			tripInfo.trip.recharge = result; //server result
			tripInfo.serverMessage = caption;

			if (result > 0) {

				if (!tripInfo.trip.begin_sent) {
					tripInfo.trip.remote_id = result;
					tripInfo.trip.begin_sent = true;
					tripInfo.setTxApertura();
				} else {
					tripInfo.trip.end_sent = true;
					tripInfo.setTxChiusura();
				}

				tripInfo.UpdateCorsa();

			} else {
				switch (result) {

					case -15:
						tripInfo.trip.warning = "OPEN TRIP";
						tripInfo.setWarning("OPEN TRIP");
						if (jobj.has("extra")) {
							tripInfo.trip.remote_id = jobj.getInt("extra");
							tripInfo.trip.begin_sent = true;
							tripInfo.setTxApertura();
						}

//					tripInfo.isOpen =false;
//					tripInfo.corsa.ora_fine = tripInfo.corsa.ora_inizio;
//					tripInfo.corsa.timestamp_fine = tripInfo.corsa.timestamp_inizio;
//					tripInfo.corsa.km_fine = tripInfo.corsa.km_inizio;
						break;

					case -16:
						tripInfo.trip.warning = "FORBIDDEN";
						tripInfo.setWarning("FORBIDDEN");
						if (jobj.has("extra")) {
							tripInfo.trip.remote_id = jobj.getInt("extra");
							tripInfo.trip.begin_sent = true;
							tripInfo.setTxApertura();
						}
						break;

					case -26:
					case -27:
					case -28:
					case -29:
						tripInfo.trip.warning = "PREAUTH";
						tripInfo.setWarning("PREAUTH");
						if (jobj.has("extra")) {
							tripInfo.trip.remote_id = jobj.getInt("extra");
							tripInfo.trip.begin_sent = true;
							tripInfo.setTxApertura();
						}
						break;

					default:
						tripInfo.trip.warning = "FAIL";
						tripInfo.setWarning("FAIL");
						tripInfo.trip.begin_sent = false;
				}

				tripInfo.UpdateCorsa();
				if (callback != null)
					callback.onTripResult(tripInfo);
			}
		} catch (JSONException e) {
			dlog.e("DecodeJson, JSON exception", e);
		} catch (Exception e) {
			dlog.e("DecodeJson, exception", e);
		}
		busy = false;
		return MsgId();
	}

	public eDirection getDirection() {
		return eDirection.DOWNLOAD;
	}

	public String EncodeJson() {

		return null;
	}
}

