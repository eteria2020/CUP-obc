package eu.philcar.csg.OBC.server;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.helpers.DLog;

@Deprecated
public class ConfigsConnector implements RemoteEntityInterface {

	private DLog dlog = new DLog(this.getClass());

	public String ConfigsString = null;

	public static ConfigsConnector GetDownloadConnector() {
		return new ConfigsConnector();
	}

	private String carPlate = null;

	public void setCarPlate(String plate) {
		this.carPlate = plate;
	}

	public int MsgId() {
		return Connectors.MSG_DN_CONFIGS;
	}

	public String GetRemoteUrl() {
		return App.URL_Configs;
	}

	public int DecodeJson(String responseBody) {
		if (responseBody == null || responseBody.isEmpty()) {
			dlog.e("Empty response");
		} else {
			dlog.d("ConfigsString: " + responseBody);
			ConfigsString = responseBody;
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
			list = new ArrayList<NameValuePair>();
			list.add(new BasicNameValuePair("car_plate", carPlate));
		}

		return list;
	}

}
