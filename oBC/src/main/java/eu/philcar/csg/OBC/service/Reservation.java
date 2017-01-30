package eu.philcar.csg.OBC.service;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.util.JsonWriter;
import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.db.Customers;
import eu.philcar.csg.OBC.db.DbManager;
import eu.philcar.csg.OBC.helpers.DLog;

public class Reservation {
	
	private DLog dlog = new DLog(this.getClass());
	
	public int   id;
	public List<String> codes;
	public long timestamp;
	public long duration;
	private long lastDebugTrace;
	private boolean local=false;
	
	
	private boolean _timedOut = false;
	
	public Date time;
	
	public static Reservation createOutOfOrderReservation() {
		int id=-1;
		Customers customers = DbManager.getInstance(App.Instance).getClientiDao();
		
		if (customers==null)
			return null;
		
		List<String> codes = customers.getMaintainerCards();
		if (codes==null)
			return null;
		
		long now = new Date().getTime() /1000;
		int length = -1;
		
		return new Reservation(id,codes,now,length,true);
		
	}
	
	public static Reservation createFromString(String str) {
		if (str!=null && ! str.isEmpty()) {
			DLog.D("Received reservation: "+str);
			try {
				
				JSONArray jArray = new JSONArray(str);
			
				if (jArray.length() == 1) {
					JSONObject jobj = jArray.getJSONObject(0);
					
					int id = jobj.getInt("id");
					
					List<String> codes = new ArrayList<String>();
					
					if 	(jobj.has("card")) 
						codes.add(jobj.getString("card"));
					
					if (jobj.has("cards")) {
						JSONArray ja = new JSONArray(jobj.getString("cards"));
						for(int j=0; j<ja.length();j++) {
							codes.add(ja.getString(j));
						}
					}
						
					Double dtimestamp = jobj.getDouble("time");
					long timestamp = dtimestamp.longValue();
					long durata = jobj.getInt("length");
					boolean attiva = jobj.getBoolean("active");
					
					long now = new Date().getTime() /1000;
					if (attiva && (timestamp+durata>now || durata<=0))					
						return new Reservation(id,codes,timestamp,durata);
					else
						return null;
					
				} else {
					ArrayList<Reservation> Reservations = new ArrayList<Reservation>();
					Reservation Reservation;
					DLog.D("Reservation array length:" + jArray.length());
					for (int i = 0; i < jArray.length(); i++) {


						JSONObject jobj = jArray.getJSONObject(i);

						int id = jobj.getInt("id");

						List<String> codes = new ArrayList<String>();

						if (jobj.has("card"))
							codes.add(jobj.getString("card"));

						if (jobj.has("cards")) {
							JSONArray ja = new JSONArray(jobj.getString("cards"));
							for (int j = 0; j < ja.length(); j++) {
								codes.add(ja.getString(j));
							}
						}

						Double dtimestamp = jobj.getDouble("time");
						long timestamp = dtimestamp.longValue();
						long durata = jobj.getInt("length");
						boolean attiva = jobj.getBoolean("active");

						long now = new Date().getTime() / 1000;
						if (attiva && (timestamp + durata > now || durata <= 0)){
							Reservations.add( new Reservation(id, codes, timestamp, durata));
						}

					}

					//prima controllo se ci sono prenotazioni di manutenzione e nel caso imposto quella.
					for(int i=0;i<Reservations.size();i++){
						Reservation = Reservations.get(i);
						if(Reservation.duration<=0)
							return Reservation;

					}
					//ritorno la prima prenotazione attiva disponibile
					return Reservations.size()>0?Reservations.get(0):null;
				}
			
				
			} catch (Exception e) {
				DLog.E("Invalid json",e);
			}
		}
		return null;
	}
	
	
	public String toJson() {	

		JSONArray  jarr = new JSONArray();
		JSONObject jo = new JSONObject();
		
		try {
			
			jo.put("id", id);
			
			JSONArray ja = new JSONArray();
			for(String card : codes) {			
				ja.put(card);
			}
			jo.put("cards", ja);
			jo.put("time",timestamp);
			jo.put("length", duration);
			jo.put("active", true);
		} catch(Exception e) {
			dlog.e("Reservation JSON serialization",e);
		}
		jarr.put(jo);
		return jarr.toString();
		
	}
	
	public Reservation(int id, List<String> codes, long time, long duration) {
		this(id,codes,time,duration,false);
	}
	
	public Reservation(int id, List<String> codes, long time, long duration, boolean local) {
		this.id = id;
		this.codes = codes;
		this.timestamp =time;
		this.duration = duration;
		this.local=local;

		this.time = new Date(time*1000);

	}
	
	
	public boolean checkCode(String cardCode) {
		//Gestire card passpartout
		
		if (cardCode!=null && codes!=null) {
			for (String s : codes) {
				if (cardCode.equalsIgnoreCase(s))
					return true;
			}
		}
		return false;
	}
	
	public boolean isMaintenance() {
		return this.duration<=0;
	}
	
	public boolean  isLocal() {
		return this.local;
	}
	
	
	public boolean isTimedout() {
		long now = new Date().getTime()/1000;
		
		if (duration>0) {
			
			if (_timedOut)
				return true;
			
			if (timestamp+duration>now)  {
				if (now-lastDebugTrace>60) {
					dlog.d("Reservation : "+ (timestamp+duration-now));
					dlog.d("Reservation valid");
					lastDebugTrace = now;
				}
				return false;
			} else {
				dlog.d("Reservation timedout");
				return true;
			}
		} else  {
			if (now-lastDebugTrace>600) {
				dlog.d("Reservation infinite");
				lastDebugTrace = now;
			}
			return false;
		}
			
	}
	
    public boolean equals(Reservation r) {
    	
    	if (this.id != r.id)
    		return false;
    	
    	if (this.duration!= r.duration)
    		return false;
    	
    	if (this.time != r.time) 
    		return false;
    	
    	return true;
    				
    	
    }
	
	@Override
	public String toString() {
		return "Id:" + id + ", timestamp:" + time.toString() + ", card:" + codes.toString() + ",duration:" + duration + ",local:" + local;
	}

	
}
