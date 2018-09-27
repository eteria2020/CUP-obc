package eu.philcar.csg.OBC.controller.map;

import eu.philcar.csg.OBC.ABase;
import eu.philcar.csg.OBC.AGoodbye;
import eu.philcar.csg.OBC.ASOS;
import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.AMainOBC;
import eu.philcar.csg.OBC.controller.FBase;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.service.MessageFactory;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class FPark extends FBase implements OnClickListener {
	
	private DLog dlog = new DLog(this.getClass());

	private View rootView;
	private RelativeLayout fins_right_FL;

	
	public static FPark newInstance() {
		
		FPark fm = new FPark();
		return fm;
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		final View view = inflater.inflate(R.layout.f_park, container, false);
		rootView = view;
		dlog.d("OnCreareView FPark");

		startSelfClose(rootView);
		//((View)rootView.findViewById(R.id.llCancel)).animate().alpha(0.25f);

		view.findViewById(R.id.finsSOSB).setOnClickListener(this);
		view.findViewById(R.id.fmenBackIB).setOnClickListener(this);
		view.findViewById(R.id.ivPark).setOnClickListener(this);
		fins_right_FL=(RelativeLayout)view.findViewById(R.id.fins_right_FL);

		rootView.findViewById(R.id.ivPark).setClickable(true);
		rootView.findViewById(R.id.ivPark).setBackgroundResource(R.drawable.sel_button_rent_resume);

		if (App.currentTripInfo!=null && App.currentTripInfo.isMaintenance) {
			fins_right_FL.setBackgroundColor(getResources().getColor(R.color.background_red));

		} else {
			fins_right_FL.setBackgroundColor(getResources().getColor(R.color.background_green));
		}
		

		return view;
	}

	@Override
	public boolean handleBackButton() {
			return true;
		
	}

	@Override
	public void onClick(View v) {
		
	switch (v.getId()) {

			
		case R.id.finsSOSB:
			startActivity(new Intent(getActivity(), ASOS.class));
			break;

		case R.id.ivPark:
		case R.id.fmenBackIB:
			//((ABase)getActivity()).popFragment();
			((AMainOBC)getActivity()).setParkModeStarted(false);
			try {
				((ABase)getActivity()).popTillFragment(FMap.class.getName());
			} catch (Exception e) {
				dlog.d("Exception while popping fragment");
			}
			((ABase)getActivity()).pushFragment(FMap.newInstance(), FMap.class.getName(), true);
			stopSelfClose(rootView);
			break;			
			
	}
		
	}
	
	public void showBeginPark() {
		rootView.findViewById(R.id.fmenBackIB).setVisibility(View.GONE);
		((TextView)rootView.findViewById(R.id.tvRow1)).setText(R.string.menu_park_mode_resume);

		rootView.findViewById(R.id.ivPark).setClickable(false);
		rootView.findViewById(R.id.ivPark).setBackgroundResource(R.drawable.sel_button_rent_pause);
	}
	
	public void showEndPark() {

		rootView.findViewById(R.id.ivPark).setClickable(true);
		rootView.findViewById(R.id.ivPark).setBackgroundResource(R.drawable.sel_button_rent_resume);
		((TextView)rootView.findViewById(R.id.tvRow1)).setText(R.string.menu_park_mode_resume);
	}
	

	
	
	CountDownTimer timer;
	private void startSelfClose(final View root) {
		int durata = 40;
		
		if (durata<=0)
			return;
		
		((AMainOBC)this.getActivity()).sendMessage(MessageFactory.scheduleSelfCloseTrip(durata));
		root.findViewById(R.id.llSelfClose).setVisibility(View.VISIBLE);
		
		timer = new CountDownTimer((durata+1)*1000,1000) {
			@Override
		     public void onTick(long millisUntilFinished) {
		    	 ((TextView)root.findViewById(R.id.tvCountdown)).setText((millisUntilFinished/1000)+ " s");
		     }

			@Override
			public void onFinish() {
				dlog.d("FMenu: finish countdown");
				root.findViewById(R.id.llSelfClose).setVisibility(View.INVISIBLE);
			}

		};
		timer.start();
		dlog.d("FMenu: start countdown");
		
	}
	
	private void stopSelfClose(final View root) {
		((AMainOBC)this.getActivity()).sendMessage(MessageFactory.scheduleSelfCloseTrip(0));
		root.findViewById(R.id.llSelfClose).setVisibility(View.INVISIBLE);
		
		if (timer!=null)
			timer.cancel();
	}
}
