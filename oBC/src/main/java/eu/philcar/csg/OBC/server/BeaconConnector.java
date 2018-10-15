package eu.philcar.csg.OBC.server;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.helpers.DLog;

@Deprecated
public class BeaconConnector implements RemoteEntityInterface {

	private DLog dlog = new DLog(this.getClass());

	public String beaconText;
	public String targa = "NA";

	public int MsgId() {
		return Connectors.MSG_UP_BEACON;
	}

	public String GetRemoteUrl() {

		return App.URL_Beacon;

	}

	public List<NameValuePair> GetParams() {

		ArrayList<NameValuePair> list = new ArrayList<NameValuePair>();

		if (targa != null)
			list.add(new BasicNameValuePair("plate", targa));

		if (beaconText != null)
			list.add(new BasicNameValuePair("beaconText", beaconText));

		return list;

	}

	public int DecodeJson(String responseBody) {

		return MsgId();
	}

	public eDirection getDirection() {
		return eDirection.DOWNLOAD;
	}

	public String EncodeJson() {

		return beaconText;
	}
}

