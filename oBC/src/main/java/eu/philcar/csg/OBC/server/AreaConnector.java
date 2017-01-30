package eu.philcar.csg.OBC.server;



import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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

public class AreaConnector implements RemoteEntityInterface {

	private DLog dlog = new DLog(this.getClass());
	
	public static  AreaConnector GetDownloadConnector() {
		return new AreaConnector();
	}
	
		
			
		
		
		
		
		public int MsgId() {
			return Connectors.MSG_DN_AREAS;
		}
		
		public String GetRemoteUrl() {
			return App.URL_Area;
		}
	
		public int DecodeJson(String responseBody) {
	
			if (responseBody==null || responseBody.isEmpty() || responseBody.trim().isEmpty()) 
				return MsgId();
			
			File file = new File(App.APP_DATA_PATH,"area.json");
			
			try {
				OutputStream os = new FileOutputStream(file);
				os.write(responseBody.getBytes());
				os.close();
			} catch (IOException e) {
				dlog.e("File output error area.json",e);
				return MsgId();
			}
			
			App.Instance.initAreaPolygon();
	
	
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
			

			ArrayList<NameValuePair>  list = new ArrayList<NameValuePair>();
			list.add(new BasicNameValuePair("targa", "" + App.CarPlate));
			list.add(new BasicNameValuePair("md5", "" + App.AreaPolygonMD5));
			

			return list;
		}
	

	
	
}
