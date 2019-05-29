package eu.philcar.csg.OBC.task;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import eu.philcar.csg.OBC.ABase;
import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.controller.FBase;
import eu.philcar.csg.OBC.controller.map.FHome;
import eu.philcar.csg.OBC.helpers.DLog;

/**
 * Created by Momo on 2/14/2018.
 */

/**
 * debug purpose
 */
public class OptimizeDistanceCalc extends FBase implements View.OnClickListener {
	// private static DLog dlog =
	private DLog dlog = new DLog(this.getClass());
	static boolean run = false;
	public SharedPreferences sp;
	public static Context context;

	public static Date prevTime;
	public static Date currentTime;

	static int Odo_start = 0;
	static int Odo_lastValidValue = 0;
	static int DistancebyODO = 0;
	static List<Integer> odo_array = new ArrayList<Integer>();
	public static Location prevLoc = null;
	public static Location prevLoc2 = null;
	public static Location changedGPS;
	public static int changedodo;
	public static OptimizeDistanceCalc ODC = new OptimizeDistanceCalc();

	public static double totalDistance = 0;
	public static double tripDistanceValue = -1;
	public static float totalDistance2 = 0;
	public static boolean Odo_firstInit = true;

	/* for debug */
	private static TextView KMbygps, KMbyODO, KMbygps2;
	private ImageButton Backb;
	Handler handler = new Handler();

	public static OptimizeDistanceCalc newInstance() {
		OptimizeDistanceCalc fm = new OptimizeDistanceCalc();
		return fm;
	}

	/****************/

 /*   @Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OptimizeDistanceCalc.context = getApplicationContext();
    }
    public static Context getAppContext() {
        return OptimizeDistanceCalc.context;
    } */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	  /*  super.onCreate(savedInstanceState);
        setContentView(R.layout.a_odc);

        Backb = (ImageButton) findViewById(R.id.Backb);
        KMbygps = (TextView) findViewById(R.id.KMbyGPS);
        KMbyODO = (TextView) findViewById(R.id.KMbyODO);

        Backb.setOnClickListener(this);
        KMbygps.setText(String.format("%.02f", totalDistance));
        KMbygps.setText((int) DistancebyODO); */

	}

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.a_odc, container, false);

		//  Backb = (ImageButton) view.findViewById(R.id.Backb);
		KMbygps = (TextView) view.findViewById(R.id.KMbyGPS);
		KMbygps2 = (TextView) view.findViewById(R.id.KMbyGPS2);
		KMbyODO = (TextView) view.findViewById(R.id.KMbyODO);

		view.findViewById(R.id.Backb).setOnClickListener(this);

		KMbygps.setText(String.format("%.02f", totalDistance));
		KMbyODO.setText(String.valueOf(DistancebyODO));
		KMbygps2.setText(String.valueOf(totalDistance2));

		return view;
	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {

			case R.id.Backb:

				try {
					((ABase) getActivity()).popTillFragment(FHome.class.getName());
				} catch (Exception e) {
					dlog.d("Exception while popping fragment");
				}

				break;

		}

	}

	public static void Controller(int status, Location loc) {
		changedGPS = loc;
		Controller(status);
	}

	public static void Controller(int status, int odo) {
		changedodo = odo;
		Controller(status);
	}

	public static void Controller(int status) {
		if (run == false)
			return;

		switch (status) {

			case OdoController.RUNonChangGps:
				onChangeGPS();
				break;

			case OdoController.RUNonChangeOdo:
				onChangeOdo();
				break;

			case OdoController.PAUSE:
				pause();
				break;

			case OdoController.RESUME:
				resume();
				break;

			case OdoController.STOP:

				stop();
				break;

			case OdoController.DESTROY:
				ODC.destroy();
				break;

		}
	}

	public static void init() {
		//check if all variables are inizialized
		totalDistance = ODC.getDouble(0);
		tripDistanceValue = 0;
		// totalDistance = 0;
		totalDistance2 = 0;
		prevTime = new Date();
		run = true;
	}

	public static void pause() {
		prevTime = null;
		run = false;

	}

	public static void resume() {
		prevTime = new Date();
		currentTime = null;
		//if(totalDistance == 0)
		totalDistance = ODC.getDouble(0);
		run = true;
	}

	public static final void stop() {
		tripDistanceValue = -1;
		prevTime = null;
		run = false;
		//   totalDistance = 0;
		Controller(OdoController.DESTROY);
	}

	public static void pauseCalc() {
		run = false;
	}

	public void destroy() {
		Savetofile();
	}

	public static void onChangeOdo() {
		odo_array.add(changedodo);

		if (changedodo <= 0) // case odo not valid
			return;

		if (Odo_firstInit) { // case first time in the rent
			Odo_start = changedodo;
			Odo_lastValidValue = Odo_start;
		} else if (Odo_lastValidValue > changedodo + 3) {            // case odo has already inizialized
			//  dlog.i("optimizeDistance:odo value problem- relived value:" + odo + " - lastValidValue: ");
			odoCorrection(changedodo);
		} else {
			Odo_lastValidValue = changedodo;
			//   log(odo);

		}
		ODC.updateUIUsingAppValues();
	}

	public static void odoCorrection(int value) {
		// log value to debug it

	}

	public static final void onChangeGPS() {
		if (changedGPS == null)
			return;
		if (prevLoc2 == null)
			prevLoc2 = new Location(changedGPS);

		if (prevLoc == null) {
			prevLoc = new Location(changedGPS);

		} else {
			double mdistance = meterDistanceBetweenPoints((float) changedGPS.getLatitude(), (float) changedGPS.getLongitude(), (float) prevLoc.getLatitude(), (float) prevLoc.getLongitude());
			float distance2 = prevLoc2.distanceTo(changedGPS);
			currentTime = new Date();
			long diffSec = (currentTime.getTime() - prevTime.getTime()) / 1000;

			ODC.putDouble(totalDistance);
			if (mdistance > 30 && mdistance / diffSec < 25) { //&& mdistance/diffSec < 25
				prevLoc = new Location(changedGPS);
				totalDistance = totalDistance + mdistance;
				tripDistanceValue = tripDistanceValue + mdistance;
				prevTime = new Date(currentTime.getTime());
				ODC.putDouble(totalDistance / 1000);
			}
			if (distance2 > 30 && distance2 / diffSec < 25) {
				totalDistance2 = totalDistance2 + distance2;
				prevLoc2 = new Location(changedGPS);
				prevTime = new Date(currentTime.getTime());
			}
			ODC.updateUIUsingAppValues();
		}
		if (changedGPS != null) {
			//   log();
			//   log(location);
		}

	}

	public void setStartOdoValue(int odo) {
		if (odo <= 0)
			return;
		Odo_start = odo;
		sp = context.getSharedPreferences(App.COMMON_PREFERENCES, Context.MODE_PRIVATE);
		Editor edit = sp.edit();
		edit.putInt(SPVarNames.odoValue, Odo_start);
		edit.apply();
	}

	public void log() {

	}

	public void log(Location location) {

	}

	public void log(int Odo) {

	}

	public final void Savetofile() {
		try {
			Writer output = null;

			File file = new File(Environment.getExternalStorageDirectory() + "/prova/" + "pr.txt");
			file.getParentFile().mkdirs();
			file.createNewFile();
			output = new BufferedWriter(new FileWriter(file));

			output.write("distanza totale-var:" + totalDistance);
			sp = context.getSharedPreferences(App.COMMON_PREFERENCES, Context.MODE_PRIVATE);
			output.write(sp.getAll().toString());
			output.close();
			// Toast.makeText(getApplicationContext(), "Composition saved", Toast.LENGTH_LONG).show();

		} catch (Exception e) {
			// Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	private static double meterDistanceBetweenPoints(float lat_a, float lng_a, float lat_b, float lng_b) {
		float pk = (float) (180.f / Math.PI);

		float a1 = lat_a / pk;
		float a2 = lng_a / pk;
		float b1 = lat_b / pk;
		float b2 = lng_b / pk;

		double t1 = Math.cos(a1) * Math.cos(a2) * Math.cos(b1) * Math.cos(b2);
		double t2 = Math.cos(a1) * Math.sin(a2) * Math.cos(b1) * Math.sin(b2);
		double t3 = Math.sin(a1) * Math.sin(b1);
		if (t1 + t2 + t3 == 1)
			return 0;
		double tt = Math.acos(t1 + t2 + t3);

		return (6366000 * tt);
		//return tt;
	}

	public final void putDouble(final double value) {
		sp = context.getSharedPreferences(App.COMMON_PREFERENCES, Context.MODE_PRIVATE);
		Editor edit = sp.edit();
		edit.putLong("distanceByGPSCalculation", Double.doubleToRawLongBits(value));
		edit.apply();
		updateUIUsingAppValues();
	}

	double getDouble(final double defaultValue) {
		sp = context.getSharedPreferences(App.COMMON_PREFERENCES, Context.MODE_PRIVATE);
		return Double.longBitsToDouble(sp.getLong("distanceByGPSCalculation", Double.doubleToLongBits(defaultValue)));
	}

	public void updateUIUsingAppValues() {
		if (KMbygps != null && totalDistance != 0) {
			double a = totalDistance / 1000;
			KMbygps.setText(String.valueOf(a));
		}
		if (KMbyODO != null)
			try {
				KMbyODO.setText(String.valueOf(changedodo));
			} catch (Exception e) {
			}

		if (KMbygps2 != null && tripDistanceValue > 0) {
			float b = (float) (tripDistanceValue / 1000);
			KMbygps2.setText(String.valueOf(b));
		}
	}

  /*  public boolean GPSValidation(Location loc, Location prevLoc){
        if(loc == null || prevLoc == null)
            return false;
        if(loc.distanceTo(prevLoc) < 50)
    }
*/

}

class SPVarNames {
	public static final String odoValue = "startodovalue";
}


