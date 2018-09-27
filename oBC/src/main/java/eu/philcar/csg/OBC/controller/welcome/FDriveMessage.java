package eu.philcar.csg.OBC.controller.welcome;

import eu.philcar.csg.OBC.ABase;
import eu.philcar.csg.OBC.AGoodbye;
import eu.philcar.csg.OBC.ASOS;
import eu.philcar.csg.OBC.AWelcome;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FDriveMessage extends FBase {
	
	private DLog dlog = new DLog(this.getClass());
	private FrameLayout fchn_right_FL;

	public static FDriveMessage newInstance(boolean login) {
		
		FDriveMessage fi = new FDriveMessage();
		
		fi.login = login;
		
		return fi;
	}
	
	private boolean login;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		final View view = inflater.inflate(R.layout.f_lanes, container, false);
		
		view.findViewById(R.id.btnNext).setVisibility(View.INVISIBLE);
		view.findViewById(R.id.tvCountdown).setVisibility(View.VISIBLE);

		fchn_right_FL=(FrameLayout)view.findViewById(R.id.fchn_right_FL);
		
		view.findViewById(R.id.btnNext).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dlog.d("FDriveMessage btnNext click : " + login);
					Intent i = new Intent(getActivity(), AMainOBC.class);
					i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(i);
					getActivity().finish();

			}
		});
		
		view.findViewById(R.id.btnSOS).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(getActivity(), ASOS.class));
			}
		});
		
		Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "interstateregular.ttf");
		
		((TextView)view.findViewById(R.id.tvPageTitle)).setTypeface(font);
		((TextView)view.findViewById(R.id.tvMessageTitle)).setTypeface(font);
		((TextView)view.findViewById(R.id.tvMessage)).setTypeface(font);
			
		new CountDownTimer(4000,1000) {
				@Override
			     public void onTick(long millisUntilFinished) {
			    	 ((TextView)view.findViewById(R.id.tvCountdown)).setText((millisUntilFinished/1000)+ " s");
			     }

				@Override
				public void onFinish() {
					view.findViewById(R.id.tvCountdown).setVisibility(View.INVISIBLE);
					view.findViewById(R.id.btnNext).setVisibility(View.VISIBLE);
				}

		}.start();

		if (App.currentTripInfo!=null && App.currentTripInfo.isMaintenance) {
			fchn_right_FL.setBackgroundColor(getResources().getColor(R.color.background_red));

		} else {
			fchn_right_FL.setBackgroundColor(getResources().getColor(R.color.background_green));
		}
		
		return view;
	}


}
