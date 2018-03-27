package eu.philcar.csg.OBC;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import eu.philcar.csg.OBC.data.datasources.repositories.EventRepository;
import eu.philcar.csg.OBC.db.Events;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.service.ObcService;
import eu.philcar.csg.OBC.service.TripInfo;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;

import javax.inject.Inject;

public class SystemControl {
	
	private static DLog dlog = new DLog(SystemControl.class);
	
	public static int InsertAPN(Context ctx, String name){

	    //Set the URIs and variables
	    int id = -1;
	    boolean existing = false;
	    final Uri APN_TABLE_URI = Uri.parse("content://telephony/carriers");
	    final Uri PREFERRED_APN_URI = Uri.parse("content://telephony/carriers/preferapn");

	    //Check if the specified APN is already in the APN table, if so skip the insertion                      
	    Cursor parser = ctx.getContentResolver().query(APN_TABLE_URI, null, null, null, null);
	    parser.moveToLast();            
	    while (parser.isBeforeFirst() == false){
	        int index = parser.getColumnIndex("name");
	        String n = parser.getString(index);   
	        if (n.equals(name)){
	            existing = true;   
	            //Toast.makeText(getApplicationContext(), "APN already configured.",Toast.LENGTH_SHORT).show();
	            break;
	        }            
	        parser.moveToPrevious();
	    }       

	    //if the entry doesn't already exist, insert it into the APN table      
	    if (!existing){         

	           //Initialize the Content Resolver and Content Provider
	           ContentResolver resolver = ctx.getContentResolver();
	           ContentValues values = new ContentValues();

	           //Capture all the existing field values excluding name
	           Cursor apu = ctx.getContentResolver().query(PREFERRED_APN_URI, null, null, null, null);
	           apu.moveToFirst();                   
	           int index;       
	           
	           index = apu.getColumnIndex("apn");
	           String apn = apu.getString(index);               
	           index = apu.getColumnIndex("type");
	           String type = apu.getString(index);              
	           index = apu.getColumnIndex("proxy");
	           String proxy = apu.getString(index);             
	           index = apu.getColumnIndex("port");
	           String port = apu.getString(index);               
	           index = apu.getColumnIndex("user");
	           String user = apu.getString(index);              
	           index = apu.getColumnIndex("password");
	           String password = apu.getString(index);              
	           index = apu.getColumnIndex("server");
	           String server = apu.getString(index);                
	           index = apu.getColumnIndex("mmsc");
	           String mmsc = apu.getString(index);             
	           index = apu.getColumnIndex("mmsproxy");
	           String mmsproxy = apu.getString(index);              
	           index = apu.getColumnIndex("mmsport");
	           String mmsport = apu.getString(index);               
	           index = apu.getColumnIndex("mcc");
	           String mcc = apu.getString(index);               
	           index = apu.getColumnIndex("mnc");
	           String mnc = apu.getString(index);               
	           index = apu.getColumnIndex("numeric");
	           String numeric = apu.getString(index);

	           //Assign them to the ContentValue object
	           values.put("name", name); //the method parameter
	           values.put("apn", "web.omnitel.it");
	           values.put("type", "default,supl");
	           values.put("proxy", "");
	           values.put("port", "");
	           values.put("user", "*");
	           values.put("password", "*");
	           values.put("server", "*");
	           values.put("mmsc", "");
	           values.put("mmsproxy", "");
	           values.put("mmsport", "");             
	           values.put("mcc", "222");
	           values.put("mnc", "10");
	           //values.put("numeric", numeric);             

	           //Actual insertion into table
	           Cursor c = null;
	           try{
	               Uri newRow = resolver.insert(APN_TABLE_URI, values);

	               if(newRow != null){
	                   c = resolver.query(newRow, null, null, null, null);
	                    int idindex = c.getColumnIndex("_id");
	                    c.moveToFirst();
	                    id = c.getShort(idindex);                       
	               }
	           }
	           catch(SQLException e){}
	           if(c !=null ) c.close();          
	    }

	    return id;
	}

	//Takes the ID of the new record generated in InsertAPN and sets that particular record the default preferred APN configuration
	public static boolean SetPreferredAPN(Context ctx, int id){

	    //If the id is -1, that means the record was found in the APN table before insertion, thus, no action required
	    if (id == -1){
	        return false;
	    }

	    Uri.parse("content://telephony/carriers");
	    final Uri PREFERRED_APN_URI = Uri.parse("content://telephony/carriers/preferapn");

	    boolean res = false;
	    ContentResolver resolver = ctx.getContentResolver();
	    ContentValues values = new ContentValues();

	    values.put("apn_id", id); 
	    try{
	        resolver.update(PREFERRED_APN_URI, values, null, null);
	        Cursor c = resolver.query(PREFERRED_APN_URI, new String[]{"name", "apn"}, "_id="+id, null, null);
	        if(c != null){
	            res = true;
	            c.close();
	        }
	    }
	    catch (SQLException e){}
	     return res;
	}	
	
	
	public static void  Reset3G(Context ctx)  {

		Thread th = new Thread(new Restart3G());
		th.start();			
	}

	
	public static void ResycNTP() {
		Thread th = new Thread(new RestartNTP());
		th.start();				
	}
	
	
	private static int countFailedTests=0;
	public static boolean hasNetworkConnection(Context ctx, EventRepository eventRepository) {
		if (ctx==null)
			return false;
	    ConnectivityManager connectivityManager  = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    
	    boolean ok= (activeNetworkInfo != null && App.isNetworkStable());
	    
	    if (ok) {
	    	if (countFailedTests>0) {
		    	eventRepository.Restart3G(countFailedTests, activeNetworkInfo.getTypeName());
		    	countFailedTests=0;
	    	} 
	    } else {
	    	countFailedTests++;
	    }
	    return ok;
	}
	
	
	public static void TestActiveNetworkConnection( Message msg, Context ctx, Handler hnd) {
		Thread th = new Thread(new TestConnection(msg,ctx,hnd));		
		th.start();
	}
	
	private static class Restart3G implements Runnable {

		@Override
		public void run() {
			dlog.d("Begin restart 3G");
			Runtime rt = Runtime.getRuntime();
			try {
				rt.exec(new String[]{"/system/xbin/su","-c", "settings put global airplane_mode_on 1"});
				Thread.sleep(2000);
				rt.exec(new String[]{"/system/xbin/su","-c", "am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true"});
				Thread.sleep(5000);
				dlog.d("...Disabled 3G");
				rt.exec(new String[]{"/system/xbin/su","-c", "settings put global airplane_mode_on 0"});
				Thread.sleep(2000);
				rt.exec(new String[]{"/system/xbin/su","-c", "am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true"});
				dlog.d("...Enabled 3G");
			} catch (IOException | InterruptedException e) {
				dlog.e("Restarting 3G",e);				
			}
			
			
		}
		
	}
	
	
	private static class RestartNTP implements Runnable {

		@Override
		public void run() {
			dlog.d("Begin restarting NTP");
			Runtime rt = Runtime.getRuntime();
			try {
				rt.exec(new String[]{"/system/xbin/su","-c", "settings put global auto_time 0"});
				Thread.sleep(2000);
				rt.exec(new String[]{"/system/xbin/su","-c", "settings put global auto_time 1"});
				dlog.d("...Restarted NTP");
			} catch (IOException | InterruptedException e) {
				dlog.e("Restarting NTP",e);				
			}
			
			
		}
		
	}
	
	
	public static class TestConnection implements Runnable {

		private Message msg;
		private Context ctx;
		private Handler hnd;
		@Inject EventRepository eventRepository;
		
		public TestConnection(Message msg, Context ctx, Handler hnd) {
			App.get(ctx).getComponent().inject(this);
		   this.msg = msg;	
		   this.ctx = ctx;
		   this.hnd = hnd;
		}
		
		@Override
		public void run() {
			
			if (msg==null)
				return;
			
		    if (hasNetworkConnection(ctx,eventRepository)) {
		        try {
		            HttpURLConnection urlc = (HttpURLConnection) (new URL("http://www.google.com").openConnection());
		            urlc.setRequestProperty("User-Agent", "Test");
		            urlc.setRequestProperty("Connection", "close");
		            urlc.setConnectTimeout(1500); 
		            urlc.connect();
		            msg.arg1=1;
					urlc.disconnect();
		        } catch (IOException e) {
		        	msg.arg1=0;		            
		        }
		    } else {
		    	msg.arg1=0;	
		    }
		    if (msg!=null &&  hnd!=null)
		    	hnd.sendMessage(msg);
		}
		
	}
	
	private static long shutdownInProgress=0;
	
	public static void doShutdown() {
		//If there is another shutdown in progress not older than 60 sec : ignore
		if (System.currentTimeMillis() - shutdownInProgress>60000) {
			Thread th = new Thread(new Shutdown(App.Instance.getApplicationContext()));
			th.start();			
		} else {
			dlog.d("Shutdown already in progress");
		}
	}
	
	public static class Shutdown implements Runnable {

		@Inject
		EventRepository eventRepository;

		public Shutdown(Context ctx) {
			App.get(ctx).getComponent().inject(this);
		}

		@Override
		public void run() {
			DLog.D(SystemControl.class.toString()+" Begin shutdown ");
			shutdownInProgress = System.currentTimeMillis();
			eventRepository.Shutdown();
			Runtime rt = Runtime.getRuntime();
			try {
				Thread.sleep(60000);
				rt.exec(new String[]{"/system/xbin/su","-c", "reboot -p"});
			} catch (IOException | InterruptedException e) {
				dlog.e("Shutdown",e);				
			}
		
		
		}
	}
	public static long rebootInProgress=0;
	public static void ForceReboot(){
		Runtime rt = Runtime.getRuntime();
		try {
			rt.exec(new String[]{"/system/xbin/su", "-c", "reboot"});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static void doReboot() {
		//If there is another reboot in progress not older than 6 hour : ignore

		if (System.currentTimeMillis() - rebootInProgress>21600000) {

			//Events.Reboot("No 3G Reboot");
			Thread th = new Thread(new Reboot());
			th.start();
		} else {
			if(System.currentTimeMillis() - rebootInProgress<0 && System.currentTimeMillis()-App.AppStartupTime.getTime()>3600000) { //if time is 01/01/2000 reboot every hour

				//Events.Reboot("No 3G Reboot");
				Thread th = new Thread(new Reboot());
				th.start();
			}
			else
			DLog.D(SystemControl.class.toString()+" Last Reboot within 6 hour, wait");
		}
	}
	private static class Reboot implements Runnable {
		@Override
		public void run() {
			try {

				DLog.D(SystemControl.class.toString()+" begin reboot");

				Thread.sleep(50000);
				if(App.currentTripInfo==null || !App.currentTripInfo.isOpen) {
				//	Events.Reboot();
					Runtime rt = Runtime.getRuntime();
					rebootInProgress = System.currentTimeMillis();
					App.Instance.persistRebootTime();
					rt.exec(new String[]{"/system/xbin/su", "-c", "reboot"});
				}else{

					DLog.D(SystemControl.class.toString()+" Abort reboot: Trip open!!! ");
				}
			} catch (IOException | InterruptedException e) {
				DLog.E(SystemControl.class.toString()+" Reboot: ",e);
			}


		}
	}
	
	
}
