package eu.philcar.csg.OBC.helpers;

/**
 * Created by Fulvio on 18/10/2016.
 */
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class RunOnStartup extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            DLog.D(this.getClass().toString()+" Ricevuto boot del dispositivo, non è un crash");
            Intent i = new Intent(context, StubActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }

}