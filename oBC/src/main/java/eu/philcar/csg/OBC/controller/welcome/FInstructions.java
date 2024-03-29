package eu.philcar.csg.OBC.controller.welcome;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import eu.philcar.csg.OBC.ABase;
import eu.philcar.csg.OBC.AGoodbye;
import eu.philcar.csg.OBC.AMainOBC;
import eu.philcar.csg.OBC.ASOS;
import eu.philcar.csg.OBC.AWelcome;
import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.controller.FBase;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.server.HttpsConnector;
import eu.philcar.csg.OBC.server.NoRedButton;
import eu.philcar.csg.OBC.service.MessageFactory;

public class FInstructions extends FBase {

	private DLog dlog = new DLog(this.getClass());
	private RelativeLayout fins_right_FL;
	private final static int MSG_CLOSE_FRAGMENT = 1;
//	private SharedPreferences preferences;

	public static FInstructions newInstance(boolean login) {

		FInstructions fi = new FInstructions();

		fi.login = login;

		return fi;
	}

	private Handler localHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
/*
			switch (msg.what) {
				case MSG_CLOSE_FRAGMENT:
					try {
						dlog.d("FInstruction timeout ");
						if (App.BannerName.getBundle("START") == null) {
							Intent i = new Intent(getActivity(), AMainOBC.class);
							i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							startActivity(i);
							getActivity().finish();
						} else {
							((ABase) getActivity()).pushFragment(FDriveMessage_new.newInstance(true), FDriveMessage_new.class.getName(), true);
						}
					} catch (Exception e) {
						dlog.e("FInstruction : MSG_CLOSE_FRAGMENT Exception", e);
					}
					break;
			}
*/
			try {
				if(msg.what==MSG_CLOSE_FRAGMENT) {
					dlog.d("FInstruction.handleMessage();timeout ");
					if (App.BannerName.getBundle("START") == null) {
						Intent i = new Intent(getActivity(), AMainOBC.class);
						i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						startActivity(i);
						getActivity().finish();
					} else {
						((ABase) getActivity()).pushFragment(FDriveMessage_new.newInstance(true), FDriveMessage_new.class.getName(), true);
					}
				}
			} catch(Exception e) {
				dlog.e("FInstruction.handleMessage();MSG_CLOSE_FRAGMENT Exception", e);
			}
		}
	};

	private boolean login;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		final View view = inflater.inflate(R.layout.f_instructions, container, false);
		dlog.d("FInstruction.onCreateView();");
		// no red button init
		SharedPreferences preferences = this.getActivity().getSharedPreferences(App.COMMON_PREFERENCES, Context.MODE_PRIVATE);

		App.NRD = preferences.getString("NRD", "-1");
		if (App.NRD.equals("-1") || App.NRD.equals("")) {
			NoRedButton rd = new NoRedButton(this.getActivity());
			HttpsConnector http = new HttpsConnector(this.getActivity());
			http.Execute(rd);
		}

		view.findViewById(R.id.llSelfClose).setVisibility(View.INVISIBLE);

		view.findViewById(R.id.finsNextIB).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dlog.d("FInstruction.onCreateView();finsNextIB click : " + login);
				if (login) {
					if (App.BannerName.getBundle("START") == null) {
						Intent i = new Intent(getActivity(), AMainOBC.class);
						i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						startActivity(i);
						getActivity().finish();
					} else
						((ABase) getActivity()).pushFragmentNoBack(FDriveMessage_new.newInstance(true), FDriveMessage_new.class.getName(), true, FInstructions.this);
				} else {
					((ABase) getActivity()).pushFragment(FGoodbye.newInstance(), FGoodbye.class.getName(), true);
				}
			}
		});

		view.findViewById(R.id.finsSOSB).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(getActivity(), ASOS.class));
			}
		});

		localHandler.removeMessages(MSG_CLOSE_FRAGMENT);
		localHandler.sendEmptyMessageDelayed(MSG_CLOSE_FRAGMENT, 60000);

		Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "interstateregular.ttf");

//		((TextView) view.findViewById(R.id.fins_message_TV)).setTypeface(font);

		fins_right_FL = (RelativeLayout) view.findViewById(R.id.fins_right_FL);

//		((TextView) view.findViewById(R.id.finsInstructions1TV)).setTypeface(font);
//		((TextView) view.findViewById(R.id.finsInstructions2TV)).setTypeface(font);
//		((TextView) view.findViewById(R.id.finsInstructions3TV)).setTypeface(font);

		if (login) {

			((AWelcome) getActivity()).sendMessage(MessageFactory.setEngine(true));
			((AWelcome) getActivity()).sendMessage(MessageFactory.setEngine(true));

			//Set the NRB state for all the car in the fleet//
			switch (getString(R.string.def_lang)){
				case "nl":
					((ABase) getActivity()).setNRBState("1");
					break;
				default:
					((ABase) getActivity()).setNRBState("0");
					break;
			}

			((TextView) view.findViewById(R.id.fins_message_TV)).setText(R.string.instruction_title);
			try {
				if (Integer.parseInt(App.NRD) == 1) {
					((TextView) view.findViewById(R.id.finsInstructions1TV)).setText(R.string.instruction_start_2);
					((TextView) view.findViewById(R.id.finsInstructions2TV)).setText(R.string.instruction_start_3);
					((TextView) view.findViewById(R.id.finsInstructions3TV)).setText(Html.fromHtml(getString(R.string.instruction_start_4)));
					view.findViewById(R.id.finsInstructions4TV).setVisibility(View.GONE);
					view.findViewById(R.id.fbullet4TV).setVisibility(View.GONE);
				} else {
					((TextView) view.findViewById(R.id.finsInstructions1TV)).setText(R.string.instruction_start_1);
					((TextView) view.findViewById(R.id.finsInstructions2TV)).setText(R.string.instruction_start_2);
					((TextView) view.findViewById(R.id.finsInstructions3TV)).setText(Html.fromHtml(getString(R.string.instruction_start_3)));
					((TextView) view.findViewById(R.id.finsInstructions4TV)).setText(R.string.instruction_start_4);
				}
			} catch (Exception e) {
				((TextView) view.findViewById(R.id.finsInstructions1TV)).setText(R.string.instruction_start_1);
				((TextView) view.findViewById(R.id.finsInstructions2TV)).setText(R.string.instruction_start_2);
				((TextView) view.findViewById(R.id.finsInstructions3TV)).setText(Html.fromHtml(getString(R.string.instruction_start_3)));
				((TextView) view.findViewById(R.id.finsInstructions4TV)).setText(R.string.instruction_start_4);

			}
			view.findViewById(R.id.fins_message_bottom_TV).setVisibility(View.GONE);

			view.findViewById(R.id.ivDamages).setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					((ABase) getActivity()).pushFragment(FDamages.newInstance(true), FDamages.class.getName(), true);
				}

			});

			view.findViewById(R.id.ivDirty).setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					((ABase) getActivity()).pushFragment(FCleanliness.newInstance(), FCleanliness.class.getName(), true);
				}

			});

		} else {
			try {
				App.setIsCloseable(true);

				((AGoodbye) this.getActivity()).sendMessage(MessageFactory.sendBeacon());
				((AGoodbye) this.getActivity()).sendMessage(MessageFactory.scheduleSelfCloseTrip(40));
				view.findViewById(R.id.llSelfClose).setVisibility(View.VISIBLE);

				new CountDownTimer(41000, 1000) {
					@Override
					public void onTick(long millisUntilFinished) {
						((TextView) view.findViewById(R.id.tvCountdown)).setText((millisUntilFinished / 1000) + " s");
					}

					@Override
					public void onFinish() {
					}

				}.start();

				((TextView) view.findViewById(R.id.fins_message_TV)).setText(R.string.instruction_title_close);
				((TextView) view.findViewById(R.id.finsInstructions1TV)).setText(R.string.instruction_close_1);

				// Instruction N.2 is removed and the following shifted up

				((TextView) view.findViewById(R.id.finsInstructions2TV)).setText(R.string.instruction_close_3);
				((TextView) view.findViewById(R.id.finsInstructions3TV)).setText(R.string.instruction_close_4);

				view.findViewById(R.id.fins_fourth_LL).setVisibility(View.GONE);

				view.findViewById(R.id.fins_message_bottom_TV).setVisibility(View.VISIBLE);
			} catch (Exception e) {
				dlog.e("FInstruction.onCreateView();Exception wtf login is: " + login, e);
			}
		}

		if (App.currentTripInfo != null && App.currentTripInfo.isMaintenance) {
			fins_right_FL.setBackgroundColor(getResources().getColor(R.color.background_red));

		} else {
			fins_right_FL.setBackgroundColor(getResources().getColor(R.color.background_green));
		}

		return view;
	}

	@Override
	public boolean handleBackButton() {

		if (login) {
			return super.handleBackButton();
		} else {
			return true;
		}
	}

	@Override
	public void onDestroy() {
		fins_right_FL = null;

		super.onDestroy();
	}

	@Override
	public void onPause() {

		localHandler.removeCallbacksAndMessages(null);
		super.onPause();
	}
}
