package eu.philcar.csg.OBC.server;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.service.TripInfo;

@Deprecated
public class CallCenterConnector implements RemoteEntityInterface {

	private TripInfo tripInfo;
	private String mobile;

	public static CallCenterConnector GetDownloadConnector() {
		return new CallCenterConnector();
	}

	public void setTripInfo(TripInfo ti) {
		tripInfo = ti;
	}

	public void setMobileNumber(String number) {
		mobile = number;
	}

	public int MsgId() {
		return Connectors.MSG_CALL_CENTER;
	}

	public String GetRemoteUrl() {
		return App.URL_Callcenter;
	}

	public int DecodeJson(String responseBody) {

		if (responseBody.contains("OK. DATI RICEVUTI"))
			return MsgId();
		else
			return 0;
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

		if (tripInfo != null && tripInfo.customer != null) {
			mobile = mobile.replace(" ", "");
			list = new ArrayList<NameValuePair>();
			list.add(new BasicNameValuePair("cognome", tripInfo.customer.surname));
			list.add(new BasicNameValuePair("nome", tripInfo.customer.name));
			list.add(new BasicNameValuePair("targa", App.CarPlate));
			list.add(new BasicNameValuePair("numero", mobile));
		}

		return list;
	}

}

