package eu.philcar.csg.OBC.devices;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import eu.philcar.csg.OBC.helpers.DLog;

public class RadioSetup {
	
	public class RadioChannel {

		public String name;
		public String band; 
		public double frequency;
		public int volume;
		
	}
	
	private List<RadioChannel> channels;
	
	public RadioSetup() {
		channels = new ArrayList<RadioChannel>();
	}
	
	public static RadioSetup fromJson(String json) {
		
		RadioSetup radioSetup = new RadioSetup();
		
		JSONArray ja;
		try {
			ja = new JSONArray(json);
	
			if (ja!=null && ja.length()>0) {
				for(int i=0; i<ja.length();i++) {
					JSONObject jo = ja.optJSONObject(i);
					if (jo!=null) {
						RadioChannel rc = radioSetup.new RadioChannel();
						rc.band = jo.optString("band", "FM");
						rc.name = jo.optString("name", "");
						rc.frequency = jo.optDouble("frequency", 0);
						rc.volume = jo.optInt("volume", 75);
						if (rc.frequency>0) 
							radioSetup.channels.add(rc);					
					}
				}
				return radioSetup;
			}
		} catch (JSONException e) {
			DLog.E("Parsing RadioSetup JSON",e);
		}
		
		return null;
	}
	
	public RadioChannel getChannel(int index) {
		if (index>=0 && index<channels.size()) {
			return channels.get(index);
		}
		return null;
	}
	
	public void addChannel(String band, double frequency, String name) {
		RadioChannel rc = new RadioChannel();
		rc.band = band;
		rc.frequency = frequency;
		rc.name = name;
		channels.add(rc);
	}
	
	public String toJson() {
		JSONArray ja = new JSONArray();
		for(RadioChannel ch : channels) {
			JSONObject jo = new JSONObject();
			try {
				jo.put("band", ch.band);
				jo.put("frequency", ch.frequency);
				jo.put("name",ch.name);
				jo.put("volume", ch.volume);				
			} catch (JSONException e) {
			}
			ja.put(jo);
		}
		return ja.toString();
	
	}

}
