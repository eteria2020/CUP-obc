package eu.philcar.csg.OBC;

import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.controller.welcome.FMaintenance;
import eu.philcar.csg.OBC.controller.welcome.FWelcome;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.service.CarInfo;
import eu.philcar.csg.OBC.service.MessageFactory;
import eu.philcar.csg.OBC.service.ObcService;
import eu.philcar.csg.OBC.service.ServiceConnector;
import eu.philcar.csg.OBC.service.TripInfo;
import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Settings;

@SuppressWarnings("deprecation")
@SuppressLint({ "HandlerLeak", "Wakelock" })
public class AWelcome extends ABase {

	private DLog dlog = new DLog(this.getClass());
	
	private ServiceConnector serviceConnector;
	
	WakeLock screenLock;	

	private final int MSG_RELEASE_SCREEN = 10001;
	
	
	protected void setEngine(boolean status) {
		DLog.D("Request enable engine");
		sendMessage(MessageFactory.setEngine(status));
		sendMessage(MessageFactory.setEngine(status));
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		
		super.onCreate(savedInstanceState);
		//System.gc();
		dlog.d("AWelcome onCreate");
		setContentView(R.layout.a_base);
		
	    if (getIntent().getBooleanExtra("EXIT", false)) {
	        finish();
	    }
	    
	    App.userDrunk = false;
		App.Instance.persistUserDrunk();
		
		serviceConnector = new ServiceConnector(this, serviceHandler);
	
		screenLock = ((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(
			     PowerManager.ON_AFTER_RELEASE  | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, this.getClass().getSimpleName());
		
		if (savedInstanceState == null) {
			
			// Since this is the first fragment, we need to use the "add" method to show it to the user, and not the "replace"
			FragmentTransaction transaction = getFragmentManager().beginTransaction();
			
			transaction.add(R.id.awelPlaceholderFL, FWelcome.newInstance(), FWelcome.class.getName());
			transaction.addToBackStack(FWelcome.class.getName());
			
			transaction.commit();
		}
	}
	
	@Override
	protected void onPause() {
		
		super.onPause();
		App.setForegroundActivity(this.getClass().toString() +"Pause");

	}

	@Override
	protected void onDestroy() {
		
		super.onDestroy();
		
		serviceConnector.unregister();
		serviceConnector.disconnect();
	}
	
	public void sendMessage(Message msg) {
		serviceConnector.send(msg);
	}
	
	
	@Override
	protected void onResume() {
		
		super.onResume();
		
		App.setForegroundActivity(this);

		if (!serviceConnector.isConnected())
			serviceConnector.connect();
		
	}

	@Override
	protected int getPlaceholderResource() {
		return R.id.awelPlaceholderFL;
	}
	
	private  Handler serviceHandler = new Handler() {
		 @Override
		 public void handleMessage(Message msg) {
			
			 if (! App.isForegroundActivity(AWelcome.this)) {
				 //DLog.W(AWelcome.class.getName() + " MSG to non foreground activity. Ignoring");
				 return;
			 }
			 
			switch (msg.what) {
			
			case ObcService.MSG_CLIENT_REGISTER:
				DLog.D(AWelcome.class.getName() + ": MSG_CLIENT_REGISTER");
				break;
				
				
			case ObcService.MSG_IO_RFID:
				screenLock.acquire();
				Message lmsg = this.obtainMessage(MSG_RELEASE_SCREEN);
				this.sendMessageDelayed(lmsg, 2000);
			
				break;
				
			
			
			case ObcService.MSG_TRIP_BEGIN:
				DLog.D(AWelcome.class.getName() + "AWelcome: received ObcService.MSG_TRIP_BEGIN");
				TripInfo ti = (TripInfo)msg.obj;
				if (msg.arg1==0) {
					if (ti!=null && ti.customer !=  null) {
						Fragment fb = getFragmentManager().findFragmentById(R.id.awelPlaceholderFL);
						if (fb!=null && fb instanceof FWelcome) {
							FWelcome f = (FWelcome)fb;
							f.setName(ti.customer.name + " " + ti.customer.surname);
							f.setMaintenance(ti.isMaintenance);
							DLog.D(AWelcome.class.getName() + "AWelcome: visualizzazione bandiere e nome");
						} else {
							DLog.E(AWelcome.class.getName() + "Impossibile cast a FWelcome  :");
						}
					}
				} else {
					
					App.Instance.loadPinChecked();
					
					if (App.pinChecked) {
						startActivity(new Intent(AWelcome.this, AMainOBC.class));
						AWelcome.this.finish();
					} else {
						FWelcome fWelcome = (FWelcome)getFragmentManager().findFragmentByTag(FWelcome.class.getName());
						if (fWelcome != null) {
							fWelcome.setName(ti.customer.name + " " + ti.customer.surname);
							fWelcome.setMaintenance(ti.isMaintenance);
						}
					}
				}
				break;
				
			case ObcService.MSG_TRIP_END:
				try {
					popTillFragment(FWelcome.class.getName());
				}catch(Exception e){
					DLog.E(AWelcome.class.getName() + "AWelcome: Exception during popTillFragment",e);
				}

				
				FWelcome welcome = (FWelcome)getFragmentManager().findFragmentByTag(FWelcome.class.getName());
				if (welcome != null) {
					welcome.resetUI();
				}
		
				break;	
				
			case ObcService.MSG_CAR_INFO: 
				FWelcome fWelcome = (FWelcome)getFragmentManager().findFragmentByTag(FWelcome.class.getName());
				if (fWelcome != null) {
					fWelcome.setCarPlate(App.CarPlate);
				}	
				
				FMaintenance fmaintenance  = (FMaintenance)getFragmentManager().findFragmentByTag(FMaintenance.class.getName());
				if (fmaintenance != null) {
					
					if (msg.obj!=null &&  msg.obj instanceof CarInfo) {					
						fmaintenance.handleCarInfo((CarInfo)msg.obj);
					}
					
				}	
			
				
			case MSG_RELEASE_SCREEN:

				if (screenLock.isHeld())
					screenLock.release();
				break;
				
				
			default:
				super.handleMessage(msg);
			 }
		 }
	};
	
	
	@Override
	public int getActivityUID() {
		return App.AWELCOME_UID;
	}
}
