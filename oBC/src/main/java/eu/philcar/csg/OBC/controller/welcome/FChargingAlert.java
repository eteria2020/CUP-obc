package eu.philcar.csg.OBC.controller.welcome;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.ButterKnife;
import butterknife.OnClick;
import eu.philcar.csg.OBC.ABase;
import eu.philcar.csg.OBC.ASOS;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.controller.FBase;
//import eu.philcar.csg.OBC.helpers.DLog;

public class FChargingAlert extends FBase {

//	private DLog dlog = new DLog(this.getClass());

	public static FChargingAlert newInstance() {

//		FChargingAlert fp = new FChargingAlert();
		return new FChargingAlert();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.f_charging_alert, container, false);
		unbinder = ButterKnife.bind(this, view);

		return view;
	}

	@OnClick(R.id.fChargingAlertBtnNext)
	protected void nextPage(View v) {
		((ABase) getActivity()).pushFragment(FPin.newInstance(), FPin.class.getName(), true);
	}

	@OnClick(R.id.fChargingAlertBtnSOS)
	protected void sosPage(View view) {
		startActivity(new Intent(getActivity(), ASOS.class));
	}

}
