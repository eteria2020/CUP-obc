package eu.philcar.csg.OBC;

import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.controller.map.FMenu;
import eu.philcar.csg.OBC.controller.map.FPark;
import eu.philcar.csg.OBC.controller.welcome.FDamages;
import eu.philcar.csg.OBC.controller.welcome.FDamagesNew;
import eu.philcar.csg.OBC.controller.welcome.FGoodbye;
import eu.philcar.csg.OBC.helpers.AudioPlayer;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.StubActivity;
import eu.philcar.csg.OBC.service.MessageFactory;
import eu.philcar.csg.OBC.service.ObcService;
import eu.philcar.csg.OBC.service.ServiceConnector;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class AGoodbye extends ABase {

	private DLog dlog = new DLog(this.getClass());
	private final boolean SKIP_DAMAGES = true;
	
	public static final String JUMP_TO_END = "JUMP_TO_END";

	public static final String EUTHANASIA = "I_WANT_TO_DIE";
	public AudioPlayer player;
	
	
	private ServiceConnector serviceConnector;
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		if(getIntent().getBooleanExtra(EUTHANASIA,true)) {
			Intent i = new Intent(this, StubActivity.class);
			startActivity(i);
			finish();
		}

		//System.gc();
		dlog.d("AGoodbye: onCreate");

		setContentView(R.layout.a_base);
		player = new AudioPlayer(this);
		player.inizializePlayer();
		
		
		serviceConnector = new ServiceConnector(this, serviceHandler);
		
		
		/*if (savedInstanceState == null) {
			

		}*/
	}
	
	@Override
	protected void onResume() {				
		super.onResume();
		
		serviceConnector.connect();
		
	}
		
	
	
	@Override
	protected void onPause() {
		
		super.onPause();
		
		serviceConnector.unregister();
		serviceConnector.disconnect();
	}


	@Override
	protected int getPlaceholderResource() {
		return R.id.awelPlaceholderFL;
	}
	
	public void sendMessage(Message msg) {
		serviceConnector.send(msg);
	}

	public void setAudioSystem(int mode){
		this.sendMessage(MessageFactory.AudioChannel(mode));
	}
	
	private  Handler serviceHandler = new Handler() {
		 @Override
		 public void handleMessage(Message msg) {
			FPark fPark;
			switch (msg.what) {
			
			case ObcService.MSG_CLIENT_REGISTER:
				DLog.E(AGoodbye.class.getName() + ": MSG_CLIENT_REGISTER");
				
				// Since this is the first fragment, we need to use the "add" method to show it to the user, and not the "replace"
				FragmentTransaction transaction = getFragmentManager().beginTransaction();
				String fragmentName = getIntent().getStringExtra("fragment");
				
				if (fragmentName!=null && fragmentName.equals("damages")) {
					transaction.add(R.id.awelPlaceholderFL, FDamages.newInstance(false), FDamages.class.getName());
					transaction.addToBackStack(FDamages.class.getName());
				} else if (SKIP_DAMAGES || (getIntent() != null && getIntent().getBooleanExtra(AGoodbye.JUMP_TO_END, false))) {
					dlog.d("Agoodbye JUMP_TO_END");//Create fragment and add bundle to indicate the explicit request to close trip

					Fragment f = FGoodbye.newInstance();
					Bundle b = new Bundle();
					b.putBoolean("CLOSE", true);
					f.setArguments(b);

					transaction.add(R.id.awelPlaceholderFL, f , FGoodbye.class.getName());
					transaction.addToBackStack(FGoodbye.class.getName());
				} else {
					transaction.add(R.id.awelPlaceholderFL, FDamages.newInstance(true), FDamages.class.getName());
					transaction.addToBackStack(FDamages.class.getName());
				}

				transaction.commit();				
				break;
				
			case ObcService.MSG_CMD_TIMEOUT:
				DLog.E(AMainOBC.class.getName() + ": MSG_CMD_TIMEOUT");
				break;
			
			case ObcService.MSG_PING:
				DLog.E(AMainOBC.class.getName() + ": MSG_PING");
				break;
				
			case ObcService.MSG_IO_RFID:
				
				DLog.E(AMainOBC.class.getName() + ": MSG_IO_RFID");
				break;
				
			
			case ObcService.MSG_TRIP_END:
				
				App.userDrunk = false;
				App.Instance.persistUserDrunk();
				
				Intent i = new Intent(AGoodbye.this, AWelcome.class);
				i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(i);
				AGoodbye.this.finish();
	
				break;					
				
			case ObcService.MSG_CUSTOMER_INFO:
				

				break;		
				
			case ObcService.MSG_TRIP_PARK_CARD_BEGIN:
				fPark = (FPark)getFragmentManager().findFragmentByTag(FPark.class.getName());
				
				if (fPark != null) {
					fPark.showBeginPark();
				}
				break;				
				
			case ObcService.MSG_TRIP_PARK_CARD_END:
				fPark = (FPark)getFragmentManager().findFragmentByTag(FPark.class.getName());
				
				if (fPark != null) {
					fPark.showEndPark();
				}
				break;
				
				
			default:
				super.handleMessage(msg);
			 }
		 }
	};

	@Override
	protected void onDestroy() {
		super.onDestroy();
		App.isClosing=false;
	}

	@Override
	public int getActivityUID() {
		return App.AGOODBYE_UID;
	}
}
