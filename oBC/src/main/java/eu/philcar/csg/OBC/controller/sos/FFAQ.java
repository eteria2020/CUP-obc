package eu.philcar.csg.OBC.controller.sos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import eu.philcar.csg.OBC.ABase;
import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.controller.FBase;
import eu.philcar.csg.OBC.helpers.DLog;

public class FFAQ extends FBase implements OnClickListener {

	public static FFAQ newInstance() {

		FFAQ ffaq = new FFAQ();
		return ffaq;
	}

	private ImageButton cancelIB;
	private FrameLayout ffaq_right_FL;
	private DLog dlog = new DLog(this.getClass());

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.f_faq, container, false);
		dlog.d("OnCreareView FFAQ");

		cancelIB = (ImageButton) view.findViewById(R.id.ffaqCancelIB);
		ffaq_right_FL = (FrameLayout) view.findViewById(R.id.ffaq_right_FL);

		cancelIB.setOnClickListener(this);

		if (App.currentTripInfo != null && App.currentTripInfo.isMaintenance) {
			ffaq_right_FL.setBackgroundColor(getResources().getColor(R.color.background_red));

		} else {
			ffaq_right_FL.setBackgroundColor(getResources().getColor(R.color.background_green));
		}

		return view;
	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {

			case R.id.ffaqCancelIB:
				((ABase) getActivity()).popFragment();
				break;

		}
	}
}