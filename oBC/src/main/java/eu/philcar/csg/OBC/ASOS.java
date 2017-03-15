package eu.philcar.csg.OBC;

import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.controller.sos.FSOS;
import eu.philcar.csg.OBC.service.ObcService;
import eu.philcar.csg.OBC.service.ServiceConnector;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class ASOS extends ABase {

	private ServiceConnector serviceConnector;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.a_base);
		
		
		serviceConnector = new ServiceConnector(this, serviceHandler);
		
		if (savedInstanceState == null) {
			
			// Since this is the first fragment, we need to use the "add" method to show it to the user, and not the "replace"
			FragmentTransaction transaction = getFragmentManager().beginTransaction();
			
			transaction.add(R.id.awelPlaceholderFL, FSOS.newInstance(), FSOS.class.getName());
			transaction.addToBackStack(FSOS.class.getName());
			
			transaction.commit();
		}
	}

	@Override
	protected int getPlaceholderResource() {
		return R.id.awelPlaceholderFL;
	}
	
	
	@Override
	protected void onResume() {		
		super.onResume();
		
		App.setForegroundActivity(this);
		
		serviceConnector.connect();
	}
	
	public void sendMessage(Message msg) {
		serviceConnector.send(msg);
	}
	
	
	
	@Override
	protected void onPause() {
		
		super.onPause();

		App.setForegroundActivity(this.getClass().toString() +"Pause");
		serviceConnector.unregister();
		serviceConnector.disconnect();
	}
	
	
	private  Handler serviceHandler = new Handler() {
		 @Override
		 public void handleMessage(Message msg) {
			
			switch (msg.what) {
			
		
			case ObcService.MSG_TRIP_END:
				Intent i = new Intent(ASOS.this, AWelcome.class);
				i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(i);
				ASOS.this.finish();
	
				break;					
				
			default:
				super.handleMessage(msg);
			 }
		 }
	};
	
	@Override
	public int getActivityUID() {
		return App.ASOS_UID;
	}
}
