package eu.philcar.csg.OBC.devices;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.service.ObcService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class ObcParser extends Handler {
	public final int MSG_NEWSTRING = 1;
	
	private DLog dlog = new DLog(this.getClass());

	private ObcService obcService;
	
	public ObcParser(ObcService service) {
		obcService = service;
	}
	
	
	@Override
	public void handleMessage(Message msg) {
	
		if (msg.what == MSG_NEWSTRING) {
			if (msg.obj!=null) {
				String str = (String) msg.obj;
				ObcString obcstr = parse(str);
				execute(obcstr);
			}
			
		} else {
			super.handleMessage(msg);
		}			
		
	}
	
	public ObcString parse(String raw) {
		ObcString  str = new ObcString();
		
		str.rawString = raw;
		if (raw.startsWith("!")) {
			str.type = ObcString.StringType.INFO;
			raw = raw.substring(1);
		} else if (raw.startsWith("#")) {
			str.type = ObcString.StringType.RESPONSE;
			raw = raw.substring(1);
		} else {
			str.type = ObcString.StringType.INFO;
		}
		
		int i = raw.indexOf(' ');
		if (i<=0) {
			str.tag=raw.trim();
		} else {
			str.tag = raw.substring(0,i).trim();
			str.args = raw.substring(i).trim();
		}
		
		return str;
	}
	
	public void execute(ObcString str) {

		//Verifica prima se ci sono dei client registrati per ricevere la notifica del comando
		Iterator it = listeners.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<ObcCommand, OnObcReceivedListener> pair = (Map.Entry<ObcCommand, OnObcReceivedListener>) it.next();
			if (pair.getKey().MatchesResponse(str.tag)) {
				pair.getValue().newString(pair.getKey(),str);
				it.remove();
				break;
			}
		}
		
		
		if (str.tag.equalsIgnoreCase("RFID")) {	
			String p[] = str.args.split(",");
			String id = p[0];
			String event = (p.length>=2?p[1]:"");
			boolean handled = (p.length>=3?true:false);
			dlog.d("OBC_IO read RFID :" + id +" event: " + event);
			obcService.notifyCard(id,event,handled);
			return;
		}
		

		
		
		if (str.type==ObcString.StringType.INFO && str.tag.equalsIgnoreCase("CAR1")) {
			String p[] = str.args.split(",");
			Bundle b = new Bundle();
			for (String s : p) {
				String sp[] =s.split("=");
				if (sp.length==2) {
					b.putString(sp[0], sp[1]);					
				}				
			}
			obcService.notifyCarInfo(b);
		}
		
		
		if (str.type==ObcString.StringType.INFO && str.tag.equalsIgnoreCase("STARTED")) {
			obcService.notifyObcIoBoot();
		}
		
		if (str.type==ObcString.StringType.INFO && str.tag.equalsIgnoreCase("BATTERY")) {
			String p[] = str.args.split(",");
			Bundle b = new Bundle();
			for (String s : p) {
				String sp[] =s.split("=");
				if (sp.length==2) {
					b.putString(sp[0], sp[1]);					
				}				
			}
			obcService.notifyBatteryInfo(b);
		}
		
	}
	
	private Map<ObcCommand, OnObcReceivedListener> listeners = new Hashtable<ObcCommand, OnObcReceivedListener>();
	
	
	public void registerReplyListener(ObcCommand cmd,  OnObcReceivedListener listener) {
	   	listeners.put(cmd, listener);
	}
	
	public interface OnObcReceivedListener {
		public void newString(ObcCommand cmd, ObcString str) ;
	}
	
	

}
