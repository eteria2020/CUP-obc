package eu.philcar.csg.OBC.controller.map;

import eu.philcar.csg.OBC.ABase;
import eu.philcar.csg.OBC.AGoodbye;
import eu.philcar.csg.OBC.ASOS;
import eu.philcar.csg.OBC.AWelcome;
import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.AMainOBC;
import eu.philcar.csg.OBC.controller.FBase;
import eu.philcar.csg.OBC.controller.welcome.FDamages;
import eu.philcar.csg.OBC.devices.LowLevelInterface;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.Debug;
import eu.philcar.csg.OBC.service.MessageFactory;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
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

public class FMenu extends FBase implements OnClickListener {

	private DLog dlog = new DLog(this.getClass());
	
	public static FMenu newInstance() {
		

		return new FMenu();
	}
	
	private ImageButton endRentIB, pauseRentIB, refuelIB, backIB;
	private ImageView ivDamages;
	private TextView endRentTV, pauseRentTV, refuelTV;
	private Button sosB;
	private View rootView;
	private FrameLayout fmen_right_FL;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		dlog.d("onCreateView FMenu");
		
		// No more model logic, trip/car status in this method. It should only load UI items
		
		View view = inflater.inflate(R.layout.f_menu, container, false);
		rootView = view;
		
		Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "interstateregular.ttf");
		
		(view.findViewById(R.id.tvPushToCancel)).setVisibility(View.INVISIBLE);
		(view.findViewById(R.id.llSelfClose)).setVisibility(View.INVISIBLE);
		
		endRentIB = (ImageButton)view.findViewById(R.id.fmenEndRentIB);
		pauseRentIB = (ImageButton)view.findViewById(R.id.fmenPauseRentIB);
		refuelIB = (ImageButton)view.findViewById(R.id.fmenRefuelIB);
		backIB = (ImageButton)view.findViewById(R.id.fmenBackIB);
		fmen_right_FL =(FrameLayout)view.findViewById(R.id.fmen_right_FL);
		ivDamages = (ImageView) view.findViewById(R.id.ivDamages);
		
		endRentTV = (TextView)view.findViewById(R.id.fmenEndRentTV);
		pauseRentTV = (TextView)view.findViewById(R.id.fmenPauseRentTV);
		refuelTV = (TextView)view.findViewById(R.id.fmenRefuelTV);
		
		sosB = (Button)view.findViewById(R.id.fmenSOSB);
		
		endRentTV.setTypeface(font);
		pauseRentTV.setTypeface(font);
		refuelTV.setTypeface(font);
		
		sosB.setTypeface(font);
				
		sosB.setOnClickListener(this);
		ivDamages.setOnClickListener(this);
		ivDamages.setVisibility(View.VISIBLE);

		if (App.currentTripInfo!=null && App.currentTripInfo.isMaintenance) {
			fmen_right_FL.setBackgroundColor(getResources().getColor(R.color.background_red));

		} else {
			fmen_right_FL.setBackgroundColor(getResources().getColor(R.color.background_green));
		}
		
		return view;
	}
	
	@Override
	public void onResume() {
		
		super.onResume();
		
		updateUIUsingAppValues();
	}

	@Override
	public void onPause() {
		
		super.onPause();
		

	}
	@Override
	public void onClick(View v) {
		
		// VERY IMPORTANT: there is NO check upon the values that are passed/used after a click has happened.
		// Due to the nature of the refactoring isSystem on this class on 2014/07/02, you MUST prevent the
		// listener to be fired up when incorrect (tuples/triples of) values exist. This should be isSystem
		// in the updateUIUsingAppValues() method. 
		// Also note that when I'm talking about "incorrect" (tuples/triples of) values, I'm not referring
		// to errors but to car/trip states that prevent some other states to be reachable, e.g., if the 
		// engine is actually on, the UI must NOT allow the user to stop rent or suspend trip.
		
		switch (v.getId()) {
		case R.id.fmenEndRentIB:
		case R.id.fmenEndRentTV:

			FMap.timer_2min.cancel();
			FMap.timer_5sec.cancel();
			dlog.d("Banner: end rent stopping update, start countdown");
			FMap.firstRun=true;
			((AMainOBC)getActivity()).sendMessage(MessageFactory.setEngine(false));

			Intent i = new Intent(getActivity(), AGoodbye.class);
			i.putExtra(AGoodbye.EUTHANASIA, false);

			startActivity(i);
			((AMainOBC)this.getActivity()).sendMessage(MessageFactory.scheduleSelfCloseTrip(40));
			getActivity().finish();
			break;
			
		case R.id.fmenPauseRentIB:
		case R.id.fmenPauseRentTV:

			FMap.timer_2min.cancel();
			FMap.timer_5sec.cancel();
			FMap.firstRun=true;
			((AMainOBC)getActivity()).sendMessage(MessageFactory.AudioChannel(LowLevelInterface.AUDIO_NONE));
			dlog.d("Banner: pause rent stopping update");
			boolean startParkingMode = (App.getParkModeStarted() == null);
			((AMainOBC)getActivity()).setParkModeStarted( startParkingMode );
			
			if (startParkingMode) {
				startSelfClose(rootView);
				(rootView.findViewById(R.id.llCancel)).animate().alpha(0.25f);
				(rootView.findViewById(R.id.tvPushToCancel)).setVisibility(View.VISIBLE);
			} else {
				stopSelfClose(rootView);
				//((ABase)getActivity()).popFragment();
				((ABase)getActivity()).popTillFragment(FMap.class.getName());
			}
			
			break;
			
		case R.id.fmenRefuelIB:
		case R.id.fmenRefuelTV:
			((ABase)getActivity()).pushFragment(FRefuel.newInstance(App.fuel_level <= 9), FRefuel.class.getName(), true);
			break;
			
		case R.id.fmenSOSB:
			startActivity(new Intent(getActivity(), ASOS.class));
			break;
			
		case R.id.fmenBackIB:
			//((ABase)getActivity()).popFragment();
			((ABase)getActivity()).popTillFragment(FMap.class.getName());
			stopSelfClose(rootView);
			break;

		case R.id.ivDamages:
			((ABase)getActivity()).pushFragment(FDamages.newInstance(false), FDamages.class.getName(), true);
			break;
		}
	}
	
	// This is (and should remain) the only method to call to update the UI
	public void updateUIUsingAppValues() {
		
		// WARNING!!! Some of the states below should never be triggered. They are marked as WTF (What-a-Terrible-Failure)
		// and leave a trace in the DLog (error level) so, if one of them is triggered, the reason why should be
		// discovered and corrected ASAP, as not to happen again. 
		// Anyhow, I wrote an implementation for them so as to provide some UI state that may allow the user to return into 
		// a valid state (the only one that remains critical is (FALSE, NULL, STARTED) but again, this should never happen 
		// so the right approach is to find out why it's happened and not to provide a valid UI for the user).
		
		AMainOBC activity = (AMainOBC)getActivity();
		if (activity == null) {
			return;				// Method called before fragment's on the stack? Then its variables are not defined and it must return. 
		}						// It will be automatically called later when MSG_CAR_UPDATE message is received
		
		//DLog.I("(EngineOn:" + App.motoreAvviato + ",ParkModeStarted" + (App.getParkModeStarted() != null) + ",ParkMode" + App.parkMode.toString() + ")");
		
		if (Debug.IGNORE_HARDWARE) {
			
			UIHelper(R.drawable.sel_button_cancel, R.string.menu_rent_end, this, 
					 R.drawable.sel_button_rent_pause, R.string.menu_park_mode_suspend, this, 
					 R.drawable.sel_button_fuel_station_small, R.string.menu_refuel, this, 
					 R.drawable.sel_button_back, this);
			
			return;
		}
		
		if (App.motoreAvviato) {
			
			if (App.getParkModeStarted() != null) {
				
				switch (App.parkMode) {
				case PARK_OFF: 		// (TRUE, DATE, OFF) - WTF STATE
					UIHelper(R.drawable.sel_button_cancel_small, R.string.menu_rent_end_shutdown_engine, null, 
							 R.drawable.button_rent_pause_pushed, R.string.menu_park_mode_suspend_shutdown_engine, null, 
							 R.drawable.button_fuel_station_small_pushed, R.string.menu_refuel_shutdown_engine, null, 
							 R.drawable.button_back_pushed, null);
					DLog.E("FMENU: illegal state reached on updateUIUsingAppValues: (Engine On, ParkModeStarted On, Park_OFF)");
					break;
				case PARK_STARTED:	// (TRUE, DATE, STARTED) - WTF STATE
					UIHelper(R.drawable.sel_button_cancel_small, R.string.menu_rent_end_shutdown_engine, null, 
							 R.drawable.button_rent_pause_pushed, R.string.menu_park_mode_suspend_shutdown_engine, null, 
							 R.drawable.button_fuel_station_small_pushed, R.string.menu_refuel_shutdown_engine, null, 
							 R.drawable.button_back_pushed, null);
					DLog.E("FMENU: illegal state reached on updateUIUsingAppValues: (Engine On, ParkModeStarted On, Park_STARTED)");
					break;
				case PARK_ENDED:	// (TRUE, DATE, ENDED) - WTF STATE
					UIHelper(R.drawable.sel_button_cancel_small, R.string.menu_rent_end_shutdown_engine, null, 
							 R.drawable.button_rent_pause_pushed, R.string.menu_park_mode_suspend_shutdown_engine, null, 
							 R.drawable.button_fuel_station_small_pushed, R.string.menu_refuel_shutdown_engine, null, 
							 R.drawable.button_back_pushed, null);
					DLog.E("FMENU: illegal state reached on updateUIUsingAppValues: (Engine On, ParkModeStarted On, Park_ENDED)");
					break;
				}
				
			} else {
				
				switch (App.parkMode) {
				case PARK_OFF: 		// (TRUE, NULL, OFF)
					UIHelper(R.drawable.sel_button_cancel_small, R.string.menu_rent_end_shutdown_engine, null, 
							 R.drawable.button_rent_pause_pushed, R.string.menu_park_mode_suspend_shutdown_engine, null, 
							 R.drawable.button_fuel_station_small_pushed, R.string.menu_refuel_shutdown_engine, null, 
							 R.drawable.sel_button_back, this);
					break;
				case PARK_STARTED:	// (TRUE, NULL, STARTED) - WTF STATE
					UIHelper(R.drawable.sel_button_cancel_small, R.string.menu_rent_end_shutdown_engine, null, 
							 R.drawable.button_rent_pause_pushed, R.string.menu_park_mode_suspend_shutdown_engine, null, 
							 R.drawable.button_fuel_station_small_pushed, R.string.menu_refuel_shutdown_engine, null, 
							 R.drawable.button_back_pushed, null);
					DLog.E("FMENU: illegal state reached on updateUIUsingAppValues: (Engine On, ParkMode NOT started, Park_STARTED)");
					break;
				case PARK_ENDED:	// (TRUE, NULL, ENDED) - WTF STATE
					UIHelper(R.drawable.sel_button_cancel_small, R.string.menu_rent_end_shutdown_engine, null, 
							 R.drawable.button_rent_pause_pushed, R.string.menu_park_mode_suspend_shutdown_engine, null, 
							 R.drawable.button_fuel_station_small_pushed, R.string.menu_refuel_shutdown_engine, null, 
							 R.drawable.button_back_pushed, null);
					DLog.E("FMENU: illegal state reached on updateUIUsingAppValues: (Engine On, ParkMode NOT started, Park_ENDED)");
					break;
				}
			}
			
		} else {
			
			if (App.getParkModeStarted() != null) {
				
				switch (App.parkMode) {
				case PARK_OFF: 		// (FALSE, DATE, OFF)
					UIHelper(R.drawable.sel_button_cancel_small, R.string.menu_rent_end_off, null, 
							 R.drawable.sel_button_rent_pause, R.string.menu_park_mode_suspend_instructions, this, 
							 R.drawable.button_fuel_station_small_pushed, R.string.menu_refuel_off, null, 
							 R.drawable.button_back_pushed, null);
					break;
				case PARK_STARTED:	// (FALSE, DATE, STARTED)
					UIHelper(R.drawable.sel_button_cancel_small, R.string.menu_rent_end_off, null, 
							 R.drawable.button_rent_resume_pushed, R.string.menu_park_mode_started, null,
							 R.drawable.button_fuel_station_small_pushed, R.string.menu_refuel_off, null, 
							 R.drawable.button_back_pushed, null);
					break;
				case PARK_ENDED:	// (FALSE, DATE, ENDED)
					UIHelper(R.drawable.sel_button_cancel_small, R.string.menu_rent_end_off, null, 
							 R.drawable.sel_button_rent_resume, R.string.menu_park_mode_resume, this, 
							 R.drawable.button_fuel_station_small_pushed, R.string.menu_refuel_off, null, 
							 R.drawable.button_back_pushed, null);
					break;
				}
				
			} else {
								
				switch (App.parkMode) {
				case PARK_OFF: 		// (FALSE, NULL, OFF)
					if (activity.isInsideParkArea()) {
						UIHelper(R.drawable.sel_button_cancel_small, R.string.menu_rent_end, this, 
								 R.drawable.sel_button_rent_pause, R.string.menu_park_mode_suspend, this, 
								 R.drawable.sel_button_fuel_station_small, R.string.menu_refuel, this, 
								 R.drawable.sel_button_back, this);
					} else {
						UIHelper(R.drawable.sel_button_cancel_small, R.string.menu_rent_end_outside_park_area, null, 
								 R.drawable.sel_button_rent_pause, R.string.menu_park_mode_suspend, this, 
								 R.drawable.sel_button_fuel_station_small, R.string.menu_refuel, this, 
								 R.drawable.sel_button_back, this);
					}
					break;
				case PARK_STARTED:	// (FALSE, NULL, STARTED) - WTF STATE
					if (activity.isInsideParkArea()) {
						UIHelper(R.drawable.sel_button_cancel_small, R.string.menu_rent_end, this, 
								 R.drawable.button_rent_pause_pushed, R.string.menu_park_mode_suspend_off, null, 
								 R.drawable.sel_button_fuel_station_small, R.string.menu_refuel, this, 
								 R.drawable.sel_button_back, this);
					} else {
						UIHelper(R.drawable.sel_button_cancel_small, R.string.menu_rent_end_outside_park_area, null, 
								 R.drawable.button_rent_pause_pushed, R.string.menu_park_mode_suspend_off, null, 
								 R.drawable.sel_button_fuel_station_small, R.string.menu_refuel, this, 
								 R.drawable.sel_button_back, this);
					}
					DLog.E("FMENU: illegal state reached on updateUIUsingAppValues: (Engine OFF, ParkMode NOT started, Park_STARTED)");
					break;
				case PARK_ENDED:	// (FALSE, NULL, ENDED) - WTF STATE
					if (activity.isInsideParkArea()) {
						UIHelper(R.drawable.sel_button_cancel_small, R.string.menu_rent_end, this, 
								 R.drawable.sel_button_rent_pause, R.string.menu_park_mode_suspend, this, 
								 R.drawable.sel_button_fuel_station_small, R.string.menu_refuel, this, 
								 R.drawable.sel_button_back, this);
					} else {
						UIHelper(R.drawable.sel_button_cancel_small, R.string.menu_rent_end_outside_park_area, null, 
								 R.drawable.sel_button_rent_pause, R.string.menu_park_mode_suspend, this, 
								 R.drawable.sel_button_fuel_station_small, R.string.menu_refuel, this, 
								 R.drawable.sel_button_back, this);
					}
					DLog.E("FMENU: illegal state reached on updateUIUsingAppValues: (Engine OFF, ParkMode NOT started, Park_ENDED)");
					break;
				}
			}
		}
	}
	
	// Convenient method to update all the UI elements
	@SuppressWarnings("unused")
	private void UIHelper( int rentImage, int rentText, OnClickListener rentListener, 
			int parkImage, int parkText, OnClickListener parkListener, 
			int fuelImage, int fuelText, OnClickListener fuelListener, 
			int backImage, OnClickListener backListener) {
		
		endRentIB.setOnClickListener(rentListener);
		endRentIB.setImageResource(rentImage);
		endRentTV.setOnClickListener(rentListener);
		endRentTV.setText(rentText);
		
		pauseRentIB.setOnClickListener(parkListener);
		pauseRentIB.setImageResource(parkImage);
		pauseRentTV.setOnClickListener(parkListener);
		pauseRentTV.setText(parkText);
		
		// Overwrite refuel settings if fuel card pin is not defined
		if (!Debug.IGNORE_HARDWARE && 
			(App.FuelCard_PIN == null || App.FuelCard_PIN.length() <= 0 || App.FuelCard_PIN.equalsIgnoreCase("null"))) {
			
			refuelTV.setText(R.string.menu_refuel_off);
			refuelIB.setImageResource(R.drawable.button_fuel_station_small_pushed);
			refuelIB.setOnClickListener(null);
			refuelTV.setOnClickListener(null);
			
		} else {
			
			refuelIB.setOnClickListener(fuelListener);
			refuelIB.setImageResource(fuelImage);
			refuelTV.setOnClickListener(fuelListener);
			refuelTV.setText(fuelText);
		}
		
		backIB.setOnClickListener(backListener);
		backIB.setImageResource(backImage);
	}
	
	CountDownTimer timer;
	private void startSelfClose(final View root) {
		int durata = 40;
		
		if (durata<=0)
			return;
		
		((AMainOBC)this.getActivity()).sendMessage(MessageFactory.scheduleSelfCloseTrip(durata));
		(root.findViewById(R.id.llSelfClose)).setVisibility(View.VISIBLE);

		(root.findViewById(R.id.ivDamages)).setVisibility(View.INVISIBLE);
		
		timer = new CountDownTimer((durata+1)*1000,1000) {
			@Override
		     public void onTick(long millisUntilFinished) {
		    	 ((TextView)root.findViewById(R.id.tvCountdown)).setText((millisUntilFinished/1000)+ " s");
		     }

			@Override
			public void onFinish() {
				dlog.d("FMenu: finish countdown");
				(root.findViewById(R.id.llSelfClose)).setVisibility(View.INVISIBLE);

			}

		};
		timer.start();
		dlog.d("FMenu: start countdown");
		
	}
	
	private void stopSelfClose(final View root) {
		((AMainOBC)this.getActivity()).sendMessage(MessageFactory.scheduleSelfCloseTrip(0));
		(root.findViewById(R.id.llSelfClose)).setVisibility(View.INVISIBLE);

		
		if (timer!=null)
			timer.cancel();
	}
	

}
