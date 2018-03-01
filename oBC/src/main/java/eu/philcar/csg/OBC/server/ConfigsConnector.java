package eu.philcar.csg.OBC.server;




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
import eu.philcar.csg.OBC.helpers.DLog;
import android.os.Debug;
import android.text.format.DateFormat;
@Deprecated
public class ConfigsConnector implements RemoteEntityInterface {

	private DLog dlog = new DLog(this.getClass());
	
	public String ConfigsString=null;
	
	public static  ConfigsConnector GetDownloadConnector() {
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
		
		if (carPlate!=null) {
			list = new ArrayList<NameValuePair>();
			list.add(new BasicNameValuePair("car_plate",  carPlate));
		} 
		
		
		return list;
	}
	

	
	
}
