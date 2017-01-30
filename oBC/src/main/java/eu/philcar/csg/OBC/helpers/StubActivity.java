package eu.philcar.csg.OBC.helpers;

import eu.philcar.csg.OBC.AWelcome;
import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.AMainOBC;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;




/**
 * @author massimobelluz
 *
 * Stub startup activity, used only for redirecting to actual initial activity based on debug/release/testing/... conditions.
 * */
public class StubActivity extends Activity {


	@Override
	public void onCreate(Bundle b){
	    super.onCreate(b);
		DLog.D("StubActivity onCreate");
	    
	    if (Debug.ON || App.Instance.isConfigMode()) { 	    
	    	startActivity(new Intent(this, ServiceTestActivity.class));
	    } else {
			DLog.D("AWelcome onCreate");
	    	startActivity(new Intent(this,  AWelcome.class));
	    }
	    	
	    		
	    finish();
	}
	
	
}
