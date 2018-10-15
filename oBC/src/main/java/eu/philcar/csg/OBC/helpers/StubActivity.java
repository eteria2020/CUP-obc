package eu.philcar.csg.OBC.helpers;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import eu.philcar.csg.OBC.App;

/**
 * @author massimobelluz
 *         <p>
 *         Stub startup activity, used only for redirecting to actual initial activity based on debug/release/testing/... conditions.
 */
public class StubActivity extends Activity {

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        DLog.D("StubActivity onCreate");

        if (Debug.ON || App.Instance.isConfigMode()) {
            startActivity(new Intent(this, ServiceTestActivity.class));
        } else {
            DLog.D("AWelcome onCreate Stub");
            //startActivity(new Intent(this,  AWelcome.class));
        }

        finish();
    }

}
