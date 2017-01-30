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

public class CommandsConnector implements RemoteEntityInterface {

	private DLog dlog = new DLog(this.getClass());
	
	public static  CommandsConnector GetDownloadConnector() {
		return new CommandsConnector();
	}
	
		
		
		
		private String car_plate = null;
		private List<ServerCommand> commands;


		public void setTarga(String car_plate) {
			this.car_plate = car_plate;	
		}
		

		
		public int MsgId() {
			return ObcService.MSG_SERVER_COMMAND;
		}
		
		public List<ServerCommand> getComandoServer() {
			return commands;
		}
		public String GetRemoteUrl() {
			return App.URL_Commands;
		}
	
		public int DecodeJson(String responseBody) {
			
			commands = ServerCommand.createFromString(responseBody);
			
			if (commands!=null)
				dlog.d("Received command: " + commands.toString());
			else
				dlog.d("Received null command");
			
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
			
			if (car_plate!=null) {
				list = new ArrayList<NameValuePair>();
				list.add(new BasicNameValuePair("car_plate", car_plate));
			} 
						
			
			return list;
		}
	

	
	
}
