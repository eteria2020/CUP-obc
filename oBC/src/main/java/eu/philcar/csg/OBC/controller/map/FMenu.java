package eu.philcar.csg.OBC.controller.map;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
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

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import eu.philcar.csg.OBC.ABase;
import eu.philcar.csg.OBC.AGoodbye;
import eu.philcar.csg.OBC.AMainOBC;
import eu.philcar.csg.OBC.ASOS;
import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.controller.FBase;
import eu.philcar.csg.OBC.controller.welcome.FDamages;
import eu.philcar.csg.OBC.data.datasources.repositories.EventRepository;
import eu.philcar.csg.OBC.devices.LowLevelInterface;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.Debug;
import eu.philcar.csg.OBC.service.CarInfo;
import eu.philcar.csg.OBC.service.MessageFactory;
import eu.philcar.csg.OBC.service.ParkMode;

public class FMenu extends FBase implements OnClickListener {

	@BindView(R.id.endRentLL)
	protected LinearLayout endRentLL;

	@BindView(R.id.pauseRentLL)
	protected LinearLayout pauseRentLL;

	@BindView(R.id.cancelActionLL)
	protected LinearLayout cancelLL;

	@Inject
	EventRepository eventRepository;

	public static final int MSG_CLOSE_TRIP = 1;
	public static final int CLOSE_TRIP_DELAY = 15;

	public static final String REQUEST_PARK = "PARK";
	public static final String REQUEST_END_RENT = "END_RENT";

	private DLog dlog = new DLog(this.getClass());
	private ImageButton endRentIB, pauseRentIB, cancelIB, backIB;
	private ImageView ivDamages;
	private TextView endRentTV, pauseRentTV, cancelTV;
	private Button sosB;
	private View rootView;
	private FrameLayout fmen_right_FL;
	private boolean Checkkey = App.checkKeyOff;
	public boolean changed = true;
	public static final String CLICKED_BUTTON = "button";
	private boolean actionTaken = false;

	public String clickedButton; // to determinate which button is clicked

	public static FMenu newInstance() {
		FMenu fMenu = new FMenu();
		Bundle arguments = new Bundle();
		arguments.putString(CLICKED_BUTTON, "");
		fMenu.setArguments(arguments);
		return fMenu;

	}

	public static FMenu newInstance(String button) {
		FMenu fMenu = new FMenu();
		Bundle arguments = new Bundle();
		arguments.putString(CLICKED_BUTTON, button);
		fMenu.setArguments(arguments);
		return fMenu;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		App.get(getActivity()).getComponent().inject(this);
		Bundle arguments = getArguments();
		if (arguments != null && arguments.containsKey(CLICKED_BUTTON))
			clickedButton = arguments.getString(CLICKED_BUTTON);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		dlog.d("onCreateView FMenu " + clickedButton + " key is " + CarInfo.getKeyStatus() + " checkKey is " + App.checkKeyOff);

		// No more model logic, trip/car status in this method. It should only load UI items

		View view = inflater.inflate(R.layout.f_menu, container, false);
		rootView = view;
		unbinder = ButterKnife.bind(this, rootView);

		Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "interstateregular.ttf");

		(view.findViewById(R.id.cancelActionLL)).setVisibility(View.GONE);
		(view.findViewById(R.id.llSelfClose)).setVisibility(View.INVISIBLE);

		endRentIB = (ImageButton) view.findViewById(R.id.fmenEndRentIB);
		pauseRentIB = (ImageButton) view.findViewById(R.id.fmenPauseRentIB);
		cancelIB = (ImageButton) view.findViewById(R.id.cancelActionIB);
		backIB = (ImageButton) view.findViewById(R.id.fmenBackIB);
		fmen_right_FL = (FrameLayout) view.findViewById(R.id.fmen_right_FL);
		ivDamages = (ImageView) view.findViewById(R.id.ivDamages);

		endRentTV = (TextView) view.findViewById(R.id.fmenEndRentTV);
		pauseRentTV = (TextView) view.findViewById(R.id.fmenPauseRentTV);
		cancelTV = (TextView) view.findViewById(R.id.cancelActionTV);

		sosB = (Button) view.findViewById(R.id.fmenSOSB);

//		endRentTV.setTypeface(font);
//		pauseRentTV.setTypeface(font);
//		cancelTV.setTypeface(font);
//		sosB.setTypeface(font);


		sosB.setOnClickListener(this);
		ivDamages.setOnClickListener(this);
		ivDamages.setVisibility(View.VISIBLE);

		if (App.currentTripInfo != null && App.currentTripInfo.isMaintenance) {
			fmen_right_FL.setBackgroundColor(getResources().getColor(R.color.background_red));

		} else {
			fmen_right_FL.setBackgroundColor(getResources().getColor(R.color.background_green));
		}

		if (clickedButton.equalsIgnoreCase(REQUEST_END_RENT)) {
			endRentLL.setVisibility(View.VISIBLE);
			pauseRentLL.setVisibility(View.GONE);
		} else if (clickedButton.equalsIgnoreCase(REQUEST_PARK)) {
			endRentLL.setVisibility(View.GONE);
			pauseRentLL.setVisibility(View.VISIBLE);
		} else {
			endRentLL.setVisibility(View.VISIBLE);
			pauseRentLL.setVisibility(View.VISIBLE);
		}

		return view;
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
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

	@SuppressLint("HandlerLeak")
	private Handler localHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MSG_CLOSE_TRIP:
					closeTrip();
					break;

			}
		}
	};

	/**
	 * Quando si chiude la corsa con la nuova logica dovremo:
	 * visualizzare la pagina fGoodby quindi con la sua activity però con la possibilità di annullare e tornare indietro.
	 * spegnere il motore solamente alla fine dei 40s di FGoodbye
	 * Al click annulla dovremo controllare di annullare perfettamente tutto il resto per tornare in uno stato coerente con la logica interna
	 */
	private void closeTrip() {
		AMainOBC activity = (AMainOBC) getActivity();
		if (activity != null && activity.checkisInsideParkingArea()) {
			eventRepository.menuclick("END RENT");
			resetBanner();
			dlog.d("Banner: end rent stopping update, start countdown");
			//((AMainOBC)getActivity()).sendMessage(MessageFactory.setEngine(false));

			Intent i = new Intent(getActivity(), AGoodbye.class);
			i.putExtra(AGoodbye.EUTHANASIA, false);

			startActivity(i);
			((AMainOBC) this.getActivity()).sendMessage(MessageFactory.scheduleSelfCloseTrip(39));
			getActivity().finish();
		}
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

		AMainOBC activity = (AMainOBC) getActivity();

		if (v != null)
			switch (v.getId()) {
				case R.id.fmenEndRentIB:
				case R.id.fmenEndRentTV:
					resetBanner();
					closeTrip();//chiudo immediatamente la pagina
//			updateUIUsingAppValues();
					break;

				case R.id.fmenPauseRentIB:
				case R.id.fmenPauseRentTV:
					resetBanner();
					((AMainOBC) getActivity()).sendMessage(MessageFactory.AudioChannel(LowLevelInterface.AUDIO_NONE, -1));
					dlog.d("Banner: click on pauseRent starting park countdown");
					boolean startParkingMode = (App.getParkModeStarted() == null);
					((AMainOBC) getActivity()).setParkModeStarted(startParkingMode);

					if (!App.motoreAvviato && App.getParkModeStarted() != null && App.parkMode == ParkMode.PARK_ENDED) {

						((AMainOBC) getActivity()).sendMessage(MessageFactory.setEngine(true));

					}

					if (startParkingMode) {
						startSelfClose(rootView);
						//(rootView.findViewById(R.id.endRentLL)).animate().alpha(0.25f);
						(rootView.findViewById(R.id.cancelActionLL)).setVisibility(View.VISIBLE);
						eventRepository.menuclick("PAUSE RENT");
					} else {
						eventRepository.menuclick("RESUME RENT");
						stopSelfClose(rootView);
						//((ABase)getActivity()).popFragment();
						try {
							((ABase) getActivity()).popTillFragment(FHome.class.getName());
						} catch (Exception e) {
							dlog.e("Exception while popping fragment", e);
						}
					}

					break;
				case R.id.fmenSOSB:
					startActivity(new Intent(getActivity(), ASOS.class));
					break;
				case R.id.cancelActionIB:
				case R.id.cancelActionTV:
				case R.id.fmenBackIB:
				case R.id.llSelfClose:
					if (App.getParkModeStarted() != null)
						((AMainOBC) getActivity()).setParkModeStarted(false);
					eventRepository.menuclick("CANCEL " + clickedButton);
					stopSelfClose(rootView);
					//((ABase)getActivity()).popFragment();
					try {
						((ABase) getActivity()).popTillFragment(FHome.class.getName());
					} catch (Exception e) {
						dlog.d("Exception while popping fragment");
					}
					//stopSelfClose(rootView);
					break;

				case R.id.ivDamages:
					((ABase) getActivity()).pushFragment(FDamages.newInstance(false), FDamages.class.getName(), true);
					break;
			}
	}

	private void resetBanner() {
		try {
			FHome.timer_2min.cancel();
			FHome.timer_5sec.cancel();
		} catch (Exception e) {
			dlog.e("Exeption while park", e);
		}
		FHome.firstRun = true;
	}

	// This is (and should remain) the only method to call to update the UI
	public void updateUIUsingAppValues() {

		// WARNING!!! Some of the states below should never be triggered. They are marked as WTF (What-a-Terrible-Failure)
		// and leave a trace in the DLog (error level) so, if one of them is triggered, the reason why should be
		// discovered and corrected ASAP, as not to happen again.
		// Anyhow, I wrote an implementation for them so as to provide some UI state that may allow the user to return into
		// a valid state (the only one that remains critical is (FALSE, NULL, STARTED) but again, this should never happen
		// so the right approach is to contains out why it's happened and not to provide a valid UI for the user).

		AMainOBC activity = (AMainOBC) getActivity();
		if (activity == null) {
			return;                // Method called before fragment's on the stack? Then its variables are not defined and it must return.
		}                        // It will be automatically called later when MSG_CAR_UPDATE message is received

		//DLog.I("(EngineOn:" + App.motoreAvviato + ",ParkModeStarted" + (App.getParkModeStarted() != null) + ",ParkMode" + App.parkMode.toString() + ")");

		if (Debug.IGNORE_HARDWARE) {

			UIHelper(R.drawable.ic_letter_x_red, R.string.menu_rent_end, this,
					R.drawable.ic_letter_p, R.string.menu_park_mode_suspend, this,
					R.drawable.ic_letter_x_red, R.string.menu_cancel_action, null,
					R.drawable.ic_arrow_left, this);

			return;
		}

		if (App.motoreAvviato) { //SEMPRE FALSE

			if (App.getParkModeStarted() != null) {

				switch (App.parkMode) {
					case PARK_OFF:        // (TRUE, DATE, OFF) - WTF STATE
						UIHelper(R.drawable.ic_letter_x_red, R.string.menu_rent_end_shutdown_engine, null,
								R.drawable.ic_letter_p, R.string.menu_park_mode_suspend_shutdown_engine, null,
								R.drawable.ic_arrow_left, R.string.menu_cancel_action, null,
								R.drawable.ic_arrow_left, null);
						DLog.E("FMENU: illegal state reached on updateUIUsingAppValues: (Engine On, ParkModeStarted On, Park_OFF)");
						break;
					case PARK_STARTED:    // (TRUE, DATE, STARTED) - WTF STATE
						UIHelper(R.drawable.ic_letter_x_red, R.string.menu_rent_end_shutdown_engine, null,
								R.drawable.ic_letter_p, R.string.menu_park_mode_suspend_shutdown_engine, null,
								R.drawable.ic_arrow_left, R.string.menu_cancel_action, null,
								R.drawable.ic_arrow_left, null);
						DLog.E("FMENU: illegal state reached on updateUIUsingAppValues: (Engine On, ParkModeStarted On, Park_STARTED)");
						break;
					case PARK_ENDED:    // (TRUE, DATE, ENDED) - WTF STATE
						UIHelper(R.drawable.ic_letter_x_red, R.string.menu_rent_end_shutdown_engine, null,
								R.drawable.ic_letter_p, R.string.menu_park_mode_suspend_shutdown_engine, null,
								R.drawable.ic_arrow_left, R.string.menu_cancel_action, null,
								R.drawable.ic_arrow_left, null);
						DLog.E("FMENU: illegal state reached on updateUIUsingAppValues: (Engine On, ParkModeStarted On, Park_ENDED)");
						break;
				}

			} else {

				switch (App.parkMode) {
					case PARK_OFF:        // (TRUE, NULL, OFF) //
						UIHelper(R.drawable.ic_letter_x_red, R.string.menu_rent_end_shutdown_engine, null,
								R.drawable.ic_letter_p, R.string.menu_park_mode_suspend_shutdown_engine, null,
								R.drawable.ic_arrow_left, R.string.menu_cancel_action, null,
								R.drawable.ic_arrow_left, this);
						break;
					case PARK_STARTED:    // (TRUE, NULL, STARTED) - WTF STATE
						UIHelper(R.drawable.ic_letter_x_red, R.string.menu_rent_end_shutdown_engine, null,
								R.drawable.ic_letter_p, R.string.menu_park_mode_suspend_shutdown_engine, null,
								R.drawable.ic_arrow_left, R.string.menu_refuel_shutdown_engine, null,
								R.drawable.ic_arrow_left, null);
						DLog.E("FMENU: illegal state reached on updateUIUsingAppValues: (Engine On, ParkMode NOT started, Park_STARTED)");
						break;
					case PARK_ENDED:    // (TRUE, NULL, ENDED) - WTF STATE
						UIHelper(R.drawable.ic_letter_x_red, R.string.menu_rent_end_shutdown_engine, null,
								R.drawable.ic_letter_p, R.string.menu_park_mode_suspend_shutdown_engine, null,
								R.drawable.ic_arrow_left, R.string.menu_cancel_action, null,
								R.drawable.ic_arrow_left, null);
						DLog.E("FMENU: illegal state reached on updateUIUsingAppValues: (Engine On, ParkMode NOT started, Park_ENDED)");
						break;
				}
			}

		} else {

			if (App.getParkModeStarted() != null) {

				switch (App.parkMode) {
					case PARK_OFF:        // (FALSE, DATE, OFF) //
						UIHelper(R.drawable.ic_letter_x_red, R.string.menu_rent_end_off, null,
								R.drawable.ic_letter_p, R.string.menu_park_mode_suspend_instructions, null,
								R.drawable.ic_arrow_left, R.string.menu_cancel_action, this,
								R.drawable.ic_arrow_left, this);
						break;
					case PARK_STARTED:    // (FALSE, DATE, STARTED)
						UIHelper(R.drawable.ic_letter_x_red, R.string.menu_rent_end_off, null,
								R.drawable.ic_letter_p, R.string.menu_park_mode_started, null,
								R.drawable.ic_arrow_left, R.string.menu_cancel_action, null,
								R.drawable.ic_arrow_left, null);
						stopSelfClose(rootView);
						if (endRentLL != null)
							endRentLL.setVisibility(View.GONE);
						break;
					case PARK_ENDED:    // (FALSE, DATE, ENDED)
						UIHelper(R.drawable.ic_letter_x_red, R.string.menu_rent_end_off, null,
								R.drawable.ic_letter_p, R.string.menu_park_mode_resume, this,
								R.drawable.ic_arrow_left, R.string.menu_cancel_action, null,
								R.drawable.ic_arrow_left, null);
						if (endRentLL != null)
							endRentLL.setVisibility(View.GONE);
						break;
				}

			} else {

				switch (App.parkMode) {
					case PARK_OFF:        // (FALSE, NULL, OFF)
						if (CarInfo.getKeyStatus() != null && !CarInfo.getKeyStatus().equalsIgnoreCase("OFF") && App.checkKeyOff) {
							dlog.d("updateUI: display key error " + CarInfo.getKeyStatus() + " " + App.checkKeyOff);
							UIHelper(R.drawable.ic_key_red, R.string.menu_rent_end_key_on, null,
									R.drawable.ic_key_green, R.string.menu_park_mode_suspend_key_on, null,
									R.drawable.ic_arrow_left, R.string.menu_cancel_action, null,
									R.drawable.ic_arrow_left, this);

						} else {
							if (activity.checkisInsideParkingArea()) {
								if (!actionTaken) {
									UIHelper(R.drawable.ic_letter_x_red, R.string.menu_rent_end, this,
											R.drawable.ic_letter_p, R.string.menu_park_mode_suspend, this,
											R.drawable.ic_arrow_left, R.string.menu_cancel_action, null,
											R.drawable.ic_arrow_left, this);

									if (clickedButton.equalsIgnoreCase(REQUEST_PARK) && App.checkKeyOff) {

										dlog.cr(" auto click on Park");
										onClick(pauseRentIB);
										actionTaken = true;
										clickedButton = "";
									} else if (clickedButton.equalsIgnoreCase(REQUEST_END_RENT) && App.checkKeyOff) {
										dlog.cr(" auto click on End Rent");
										onClick(endRentIB);
										actionTaken = true;
										clickedButton = "";
									}
								} else {
									if (clickedButton.equalsIgnoreCase(REQUEST_END_RENT))
										UIHelper(R.drawable.ic_letter_x_red, R.string.menu_rent_end_instruction, null,
												R.drawable.ic_letter_p, R.string.menu_park_mode_resume, null,
												R.drawable.ic_arrow_left, R.string.menu_cancel_action, this,
												R.drawable.ic_arrow_left, this);
								}

							} else {
								dlog.d("updateUI: display area error");
								UIHelper(R.drawable.ic_letter_x_red, R.string.menu_rent_end_outside_park_area, null,
										R.drawable.ic_letter_p, R.string.menu_park_mode_suspend, this,
										R.drawable.ic_arrow_left, R.string.menu_cancel_action, null,
										R.drawable.ic_arrow_left, this);
								if (!actionTaken && clickedButton.equalsIgnoreCase(REQUEST_PARK) && App.checkKeyOff) {
									onClick(pauseRentIB);
									actionTaken = true;
								}
							}
						}
						break;
					case PARK_STARTED:    // (FALSE, NULL, STARTED) - WTF STATE
						if (activity.checkisInsideParkingArea()) {
							UIHelper(R.drawable.ic_letter_x_red, R.string.menu_rent_end, this,
									R.drawable.ic_letter_p, R.string.menu_park_mode_suspend_off, null,
									R.drawable.ic_arrow_left, R.string.menu_refuel, this,
									R.drawable.ic_arrow_left, this);
						} else {
							UIHelper(R.drawable.ic_letter_x_red, R.string.menu_rent_end_outside_park_area, null,
									R.drawable.ic_letter_p, R.string.menu_park_mode_suspend_off, null,
									R.drawable.ic_arrow_left, R.string.menu_cancel_action, this,
									R.drawable.ic_arrow_left, this);
						}
						DLog.E("FMENU: illegal state reached on updateUIUsingAppValues: (Engine OFF, ParkMode NOT started, Park_STARTED)");
						break;
					case PARK_ENDED:    // (FALSE, NULL, ENDED) - WTF STATE
						if (activity.checkisInsideParkingArea()) {
							UIHelper(R.drawable.ic_letter_x_red, R.string.menu_rent_end, this,
									R.drawable.ic_letter_p, R.string.menu_park_mode_suspend, this,
									R.drawable.ic_arrow_left, R.string.menu_cancel_action, null,
									R.drawable.ic_arrow_left, this);
						} else {
							UIHelper(R.drawable.ic_letter_x_red, R.string.menu_rent_end_outside_park_area, null,
									R.drawable.ic_letter_p, R.string.menu_park_mode_suspend, this,
									R.drawable.ic_arrow_left, R.string.menu_cancel_action, null,
									R.drawable.ic_arrow_left, this);
						}
						DLog.E("FMENU: illegal state reached on updateUIUsingAppValues: (Engine OFF, ParkMode NOT started, Park_ENDED)");
						break;
				}
			}
		}

	}

	// Convenient method to update all the UI elements
	@SuppressWarnings("unused")
	private void UIHelper(int rentImage, int rentText, OnClickListener rentListener,
						  int parkImage, int parkText, OnClickListener parkListener,
						  int fuelImage, int fuelText, OnClickListener fuelListener,
						  int backImage, OnClickListener backListener) {

		if (rentListener != null) {
			endRentIB.setEnabled(true);
			endRentIB.animate().alpha(1f);
		} else {
			endRentIB.setEnabled(false);
			if (!(CarInfo.getKeyStatus() != null && !CarInfo.getKeyStatus().equalsIgnoreCase("OFF") && App.checkKeyOff))
				endRentIB.animate().alpha(0.33f);
		}

		endRentIB.setOnClickListener(rentListener);
		endRentIB.setImageResource(rentImage);
		endRentTV.setOnClickListener(rentListener);
		endRentTV.setText(rentText);

		if (parkListener != null) {
			pauseRentIB.setEnabled(true);
			pauseRentIB.animate().alpha(1f);
		} else {
			pauseRentIB.setEnabled(false);
			if (!(CarInfo.getKeyStatus() != null && !CarInfo.getKeyStatus().equalsIgnoreCase("OFF") && App.checkKeyOff))
				pauseRentIB.animate().alpha(0.33f);
		}
		pauseRentIB.setOnClickListener(parkListener);
		pauseRentIB.setImageResource(parkImage);
		pauseRentTV.setOnClickListener(parkListener);
		pauseRentTV.setText(parkText);

		if (cancelLL != null) {
			if (fuelListener != null)
				cancelLL.setVisibility(View.VISIBLE);
			else
				cancelLL.setVisibility(View.GONE);
		}

		cancelIB.setImageResource(fuelImage);
		if (fuelListener != null) {
			cancelIB.setEnabled(true);
			cancelIB.setOnClickListener(fuelListener);
			cancelTV.setOnClickListener(fuelListener);
		} else {
			cancelIB.setEnabled(false);
			cancelIB.setOnClickListener(fuelListener);
			cancelTV.setOnClickListener(fuelListener);
		}
		cancelTV.setText(fuelText);

		if (backListener != null) {
			backIB.setEnabled(true);
			backIB.setOnClickListener(backListener);
			backIB.animate().alpha(1f);
		} else {

			backIB.setEnabled(true);
			backIB.setOnClickListener(backListener);
			backIB.animate().alpha(0.33f);
		}
		backIB.setImageResource(backImage);
	}

	CountDownTimer timer;

	private void startSelfCloseTrip(final View root, int duration) {
		int durata = duration;

		if (durata <= 0)
			return;

		//((AMainOBC)this.getActivity()).sendMessage(MessageFactory.scheduleSelfCloseTrip(durata));
		(root.findViewById(R.id.llSelfClose)).setVisibility(View.VISIBLE);
		(root.findViewById(R.id.llSelfClose)).setOnClickListener(this);

		(root.findViewById(R.id.ivDamages)).setVisibility(View.INVISIBLE);

		timer = new CountDownTimer((durata + 1) * 1000, 1000) {
			@Override
			public void onTick(long millisUntilFinished) {
				((TextView) root.findViewById(R.id.tvCountdown)).setText((millisUntilFinished / 1000) + " s");
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

	private void startSelfClose(View root) {
		startSelfClose(root, 39);

	}

	private void startSelfClose(final View root, int duration) {
		int durata = duration;

		if (durata <= 0)
			return;

		((AMainOBC) this.getActivity()).sendMessage(MessageFactory.scheduleSelfCloseTrip(durata));
		(root.findViewById(R.id.llSelfClose)).setVisibility(View.VISIBLE);
		(root.findViewById(R.id.llSelfClose)).setOnClickListener(this);

		(root.findViewById(R.id.ivDamages)).setVisibility(View.INVISIBLE);

		timer = new CountDownTimer((durata + 1) * 1000, 1000) {
			@Override
			public void onTick(long millisUntilFinished) {
				((TextView) root.findViewById(R.id.tvCountdown)).setText((millisUntilFinished / 1000) + " s");
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

/*	public void onActivityCreated(Bundle savedInstanceState) {

		if(Checkkey != true)
			return;
		final Handler h = new Handler();
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				try {
					KeyOffCheck();
				}catch (Exception e)
				{
					dlog.e("keyoffceck",e);
				}

				h.postDelayed(this, 1000);
			}
		};


		h.postDelayed(runnable, 1000);

		super.onActivityCreated(savedInstanceState);

	}
 */

	private void stopSelfClose(final View root) {
		try {
			if (getActivity() != null)
				((AMainOBC) this.getActivity()).sendMessage(MessageFactory.scheduleSelfCloseTrip(0));

			if (App.isIsCloseable())
				App.setIsCloseable(false);

			(root.findViewById(R.id.llSelfClose)).setVisibility(View.INVISIBLE);
			(root.findViewById(R.id.llSelfClose)).setOnClickListener(null);

			localHandler.removeMessages(MSG_CLOSE_TRIP);

			if (timer != null)
				timer.cancel();
		} catch (Exception e) {
			dlog.e("Exception while stopping timer", e);
		}
	}

	@Override
	public void onDestroy() {
		cancelIB = null;
		ivDamages = null;
		endRentIB = null;
		endRentIB = null;
		pauseRentIB = null;
		pauseRentTV = null;
		super.onDestroy();
	}

}
