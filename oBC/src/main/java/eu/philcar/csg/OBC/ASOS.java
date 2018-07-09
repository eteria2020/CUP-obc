package eu.philcar.csg.OBC;

import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.controller.sos.FSOS;
import eu.philcar.csg.OBC.helpers.Clients;
import eu.philcar.csg.OBC.service.ObcService;
import eu.philcar.csg.OBC.service.ServiceConnector;

import android.annotation.SuppressLint;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.media.session.MediaSessionCompat;

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
		
		serviceConnector.connect(Clients.SOS);
	}

	@Override
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
	
	
	@SuppressLint("HandlerLeak")
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

			case ObcService.MSG_FAILED_SOS:
				FSOS fSos = (FSOS) getFragmentManager().findFragmentByTag(FSOS.class.getName());

				fSos.failedSos();

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
