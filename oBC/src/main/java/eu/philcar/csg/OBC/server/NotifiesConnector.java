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
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.service.ObcService;
import eu.philcar.csg.OBC.service.Reservation;
import android.os.Debug;
import android.text.format.DateFormat;
@Deprecated
public class NotifiesConnector implements RemoteEntityInterface {

	private DLog dlog = new DLog(this.getClass());
	
	public static  NotifiesConnector GetDownloadConnector() {
		return new NotifiesConnector();
	}
	
		
		
		
		private String targa = null;
		private String beacon = null;
		
		public String response;
		

		
		public void setTarga(String targa) {
			this.targa = targa;	
		}

		public void setBeacon(String beacon) {
			this.beacon = beacon;	
		}
		public int MsgId() {
			return ObcService.MSG_SERVER_HTTPNOTIFY;
		}
		
		public String GetRemoteUrl() {
 			return App.URL_Notifies;
		}
	
		public int DecodeJson(String responseBody) {
			this.response = responseBody;
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
			list = new ArrayList<NameValuePair>();
			
			if (targa!=null) {			
				list.add(new BasicNameValuePair("plate", targa));
			} 
			
			if (beacon!=null) {
				list.add(new BasicNameValuePair("beaconText", beacon));
			}
					
			return list;
		}
	

	
	
}
