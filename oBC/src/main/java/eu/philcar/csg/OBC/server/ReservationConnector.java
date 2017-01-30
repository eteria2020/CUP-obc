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

public class ReservationConnector implements RemoteEntityInterface {

	private DLog dlog = new DLog(this.getClass());
	
	public static  ReservationConnector GetDownloadConnector() {
		return new ReservationConnector();
	}
	
		
		
		
		private String targa = null;
		private int consumed = 0;
		public Reservation reservation;
		

		
		public void setTarga(String targa) {
			this.targa = targa;	
		}
		
		public void setConsumed(int id) {
			consumed = id;
		}
		
		public int MsgId() {
			return ObcService.MSG_SERVER_RESERVATION;
		}
		
		public Reservation getReservation() {
			return reservation;
		}
		public String GetRemoteUrl() {
			return App.URL_Reservations;
		}
	
		public int DecodeJson(String responseBody) {
			
			if (responseBody==null || responseBody.isEmpty())
				return 0;
			
			reservation = Reservation.createFromString(responseBody);
			
			if (reservation!=null)
				dlog.d("Received reservation: " + reservation.toString());
			else
				dlog.d("Received null reservation");
			
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
			
			if (targa!=null) {
				list = new ArrayList<NameValuePair>();
				list.add(new BasicNameValuePair("car_plate", targa));
			} 
						
			if (consumed>0) {
				list = new ArrayList<NameValuePair>();
				list.add(new BasicNameValuePair("consumed", "" + consumed));				
			}
			
			return list;
		}
	

	
	
}
