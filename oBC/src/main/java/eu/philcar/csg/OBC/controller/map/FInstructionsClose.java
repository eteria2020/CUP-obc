package eu.philcar.csg.OBC.controller.map;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Date;

import eu.philcar.csg.OBC.ABase;
import eu.philcar.csg.OBC.AGoodbye;
import eu.philcar.csg.OBC.AMainOBC;
import eu.philcar.csg.OBC.ASOS;
import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.controller.FBase;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.service.MessageFactory;

public class FInstructionsClose extends FBase implements OnClickListener {
	
	private DLog dlog = new DLog(this.getClass());

	private View rootView;
	private TextView tvOutside;
	private Button End;
	private RelativeLayout fins_right_FL;

	
	public static FInstructionsClose newInstance() {
		
		FInstructionsClose fm = new FInstructionsClose();
		return fm;
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		final View view = inflater.inflate(R.layout.f_instructions_close, container, false);
		rootView = view;
		dlog.d("OnCreareViewFInstructionClose");
		
		((ImageButton)view.findViewById(R.id.fmenBackIB)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				((ABase)getActivity()).popFragment();
			}
			
		});
		

		
		
		((Button)view.findViewById(R.id.finsSOSB)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(getActivity(), ASOS.class));
			}
		});
		
		Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "interstateregular.ttf");
		
		((TextView)view.findViewById(R.id.fins_message_TV)).setTypeface(font);
		tvOutside = (TextView)view.findViewById(R.id.tvOutside);
		((TextView)view.findViewById(R.id.finsInstructions1TV)).setTypeface(font);
		((TextView)view.findViewById(R.id.finsInstructions2TV)).setTypeface(font);
		((TextView)view.findViewById(R.id.finsInstructions3TV)).setTypeface(font);
		((TextView)view.findViewById(R.id.finsInstructions4TV)).setTypeface(font);
		((TextView)view.findViewById(R.id.finsInstructions5TV)).setTypeface(font);
		
	
		((AMainOBC)getActivity()).sendMessage(MessageFactory.RadioVolume(0));

		fins_right_FL=(RelativeLayout)view.findViewById(R.id.fins_right_FL);
		
		
		((View)view.findViewById(R.id.ivDamages)).setOnClickListener(this);
		((Button)view.findViewById(R.id.fmenPauseRentIB)).setOnClickListener(this);
		End=(Button)view.findViewById(R.id.fmenEndRentIB);
		((Button)view.findViewById(R.id.fmenEndRentIB)).setOnClickListener(this);

		if (App.currentTripInfo.isMaintenance) {
			fins_right_FL.setBackgroundColor(getResources().getColor(R.color.background_red));

		} else {
			fins_right_FL.setBackgroundColor(getResources().getColor(R.color.background_green));
		}
		End.setVisibility(View.VISIBLE);
		tvOutside.setVisibility(View.GONE);
		return view;
	}

	@Override
	public boolean handleBackButton() {
			return true;
		
	}

	@Override
	public void onClick(View v) {

	switch (v.getId()) {

		case R.id.ivDamages:
			Intent intent = new Intent(getActivity(), AGoodbye.class);
			intent.putExtra("fragment", "damages");
			startActivity(intent);
			break;


		case R.id.fmenEndRentIB:
			//if((new Date().getTime()-App.AppStartupTime.getTime())>90000) {
				if (((AMainOBC) getActivity()).isInsideParkArea()) {

					((AMainOBC) getActivity()).sendMessage(MessageFactory.setEngine(false));
					startActivity(new Intent(getActivity(), AGoodbye.class));
					getActivity().finish();
				} else {
					End.setVisibility(View.GONE);
					tvOutside.setVisibility(View.VISIBLE);
				}
			// else{

			//	End.setVisibility(View.GONE);
			//	tvOutside.setVisibility(View.VISIBLE);

			//}
			break;
			
		case R.id.fmenPauseRentIB:
			boolean startParkingMode = (App.getParkModeStarted() == null);
			((AMainOBC)getActivity()).setParkModeStarted(startParkingMode );
			
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
	
			}
			
			((ABase)getActivity()).pushFragment(FPark.newInstance(), FPark.class.getName(), true);
			
			/*
			if (startParkingMode) {
				startSelfClose(rootView);
				((View)rootView.findViewById(R.id.llCancel)).animate().alpha(0.25f);
				((TextView)rootView.findViewById(R.id.tvPushToCancel)).setVisibility(View.VISIBLE);
			} else {
				stopSelfClose(rootView);
				//((ABase)getActivity()).popFragment();
				((ABase)getActivity()).popTillFragment(FMap.class.getName());
			}
			*/
			
			break;
			
		case R.id.fmenSOSB:
			startActivity(new Intent(getActivity(), ASOS.class));
			break;
			
		case R.id.fmenBackIB:
			//((ABase)getActivity()).popFragment();
			((ABase)getActivity()).popTillFragment(FHome.class.getName());
			
			break;			
			
	}
		
	}
	
	

}
