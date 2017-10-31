package eu.philcar.csg.OBC.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.service.Reservation;

public class ServerCommand {

	public String comandoRaw;
	
	public long id;
	public String command;
	public int    intarg1;
	public int    intarg2;
	public String    txtarg1;
	public String    txtarg2;
	public long queued;
	public int ttl;
	public String payload;
	
	
	public static List<ServerCommand> createFromString(String str) {
		ArrayList<ServerCommand> list = new ArrayList<ServerCommand>();
		
		if (str!=null && ! str.isEmpty()) {
			DLog.D("Received command: "+str);
			try {				
				JSONArray jArray = new JSONArray(str);
			
				for(int i =0; i<jArray.length(); i++) {
					ServerCommand cmd = new ServerCommand();
					
					JSONObject jobj = jArray.getJSONObject(i);
					
					if (jobj.has("id"))
						cmd.id = jobj.getInt("id");
					
					cmd.command = jobj.getString("command");
					
					if (jobj.has("intarg1"))
						cmd.intarg1 = jobj.getInt("intarg1");
					
					if (jobj.has("intarg2"))
						cmd.intarg2 = jobj.getInt("intarg2");
					
					if (jobj.has("txtarg1"))
						cmd.txtarg1 = jobj.getString("txtarg1");
					
					if (jobj.has("txtarg2"))
						cmd.txtarg2 = jobj.getString("txtarg2");
					
					if (jobj.has("queued")) {
						Double stamp = jobj.getDouble("queued");
						cmd.queued = stamp.longValue();
					}
					
					if (jobj.has("ttl"))
						cmd.ttl = jobj.getInt("ttl");
					
					if (jobj.has("payload"))
						cmd.payload = jobj.getString("payload");
					
					long now = new Date().getTime() /1000;
					if ((cmd.ttl<=0 || cmd.queued+cmd.ttl>now) || cmd.command.equalsIgnoreCase("CLOSE_TRIP"))
						list.add(cmd);
					else
						DLog.D("Command timeout :");					
				} 
			
				
			} catch (Exception e) {
				DLog.E("Invalid json",e);
			}
		}
		return list;
	}
	
	
	
}
