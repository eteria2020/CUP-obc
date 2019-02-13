package eu.philcar.csg.OBC.controller.welcome;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import eu.philcar.csg.OBC.ABase;
import eu.philcar.csg.OBC.AWelcome;
import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.BuildConfig;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.controller.FBase;
import eu.philcar.csg.OBC.data.datasources.repositories.EventRepository;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.Debug;
import eu.philcar.csg.OBC.helpers.ServiceTestActivity;
import eu.philcar.csg.OBC.service.MessageFactory;
import eu.philcar.csg.OBC.service.Reservation;

public class FWelcome extends FBase {

	public static FWelcome newInstance() {

		FWelcome fw = new FWelcome();
		return fw;
	}

	@Inject
	EventRepository eventRepository;

	@BindView(R.id.fwcm_whole_RL)
	protected RelativeLayout fwcm_whole_RL;
	private DLog dlog = new DLog(this.getClass());
	@BindView(R.id.fwelWelcomeLL)
	protected LinearLayout welcomeLL;
	@BindView(R.id.fwelBannerLL)
	protected LinearLayout bannerLL;
	@BindView(R.id.fwelLanguageLL)
	protected LinearLayout flagsLL;
	@BindView(R.id.fwel_name_TV)
	protected TextView nameTV;
	@BindView(R.id.tvCarPlate)
	protected TextView tvCarPlate;
	@BindView(R.id.tvDateTime)
	protected TextView tvDateTime;
	@BindView(R.id.tvFleet)
	protected TextView tvFleet;
	@BindView(R.id.fwelItalianIB)
	protected ImageButton fwelItalianIB;
	@BindView(R.id.fwelEnglishIB)
	protected ImageButton fwelEnglishIB;
	@BindView(R.id.fwelFrenchIB)
	protected ImageButton fwelFrenchIB;
	@BindView(R.id.fwelchineseIB)
	protected ImageButton fwelChineseIB;
	@BindView(R.id.fwelSlovakiaIB)
	protected ImageButton fwelSlovakiaIB;
	private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.getDefault());

	private int logoTaps = 0;
	private boolean isMaintenance = false;
	public static FWelcome Instance;

	public FWelcome() {
		Instance = this;

	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		App.get(getActivity()).getComponent().inject(this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.f_welcome, container, false);

		unbinder = ButterKnife.bind(this, view);

		dlog.d("OnCreateView FWelcome");

		view.findViewById(R.id.fwelItalianIB).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				((ABase) getActivity()).setItalianLanguage();
				nextPage();

			}
		});
		view.findViewById(R.id.fwelFrenchIB).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				((ABase) getActivity()).setFrenchLanguage();
				nextPage();

			}
		});
		view.findViewById(R.id.fwelEnglishIB).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				((ABase) getActivity()).setEnglishLanguage();
				nextPage();
			}
		});
		view.findViewById(R.id.fwelchineseIB).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				((ABase) getActivity()).setChinseseLanguage();
				nextPage();

			}
		});

		// Count  taps for service login dialog
		view.findViewById(R.id.fwelLogoIV).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				logoTaps++;
			}
		});

		// Verify long press after min 5 taps for show service login dialog
		view.findViewById(R.id.fwelLogoIV).setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				if (logoTaps >= 5) {
					logoTaps = 0;
					askAdminPassword();
				}
				return true;
			}
		});

		Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "interstateregular.ttf");
		((TextView) view.findViewById(R.id.fwel_welcome_TV)).setTypeface(font);

		nameTV.setTypeface(font);
		tvCarPlate.setTypeface(font);
		tvFleet.setTypeface(font);
		tvDateTime.setTypeface(font);

		tvFleet.setText(App.DefaultCity);
		setCarPlate(App.CarPlate);

		((TextView) view.findViewById(R.id.tvVersion)).setText(App.Versions.AppName);

		resetUI();

		if (Debug.IGNORE_HARDWARE) {
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					setName("Nome Cognome");
				}
			}, 2000);
		}

		//Start clock update
		localHandler.sendEmptyMessage(0);

		if (App.reservation != null) {
			if (App.reservation.isMaintenance()) {
				fwcm_whole_RL.setBackgroundColor(getResources().getColor(R.color.background_red));

			} else {
				fwcm_whole_RL.setBackgroundColor(getResources().getColor(R.color.background_green));
			}
		} else {
			fwcm_whole_RL.setBackgroundColor(getResources().getColor(R.color.background_green));
		}

		return view;
	}

	//Go to next UI page
	private void nextPage() {
		// if the charge mode is active go to Maintenance page for resetting it.
		// We assume that if the car is in charging mode no one but the admins can get in
		if (App.isCharging()) {
			((ABase) getActivity()).pushFragment(FMaintenance.newInstance(), FMaintenance.class.getName(), true);
			DLog.D("Request FMaintenance");
		} else
			((ABase) getActivity()).pushFragment(FPin.newInstance(), FPin.class.getName(), true);
	}

	//Show dialog for service page
	private void askAdminPassword() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());

		((AWelcome) this.getActivity()).sendMessage(MessageFactory.setDisplayStatus(true));

		builder.setTitle("Accesso manutenzione");

		// Add edit text for password input
		final EditText input = new EditText(this.getActivity());
		input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		builder.setView(input);

		// Set up the buttons
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String pwd = input.getText().toString();
				App.isAdmin = 0;
				//TODO: use external config for password in hashed form
				if (pwd.equals("Roger18")) App.isAdmin = 1;
				if (pwd.equals("redrum18")) App.isAdmin = 2;

				if (App.isAdmin > 0) {
					eventRepository.DiagnosticPage(App.isAdmin);
					Intent intent = new Intent(FWelcome.this.getActivity(), ServiceTestActivity.class);
					FWelcome.this.getActivity().startActivity(intent);
				} else {
					eventRepository.DiagnosticPageFail(pwd);
				}
			}
		});
		builder.setNegativeButton("Annulla", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});

		builder.show();
	}

	public void maintenanceBackground(Reservation r) {

		if (r != null && r.isMaintenance()) {
			fwcm_whole_RL.setBackgroundColor(getResources().getColor(R.color.background_red));

		} else {
			fwcm_whole_RL.setBackgroundColor(getResources().getColor(R.color.background_green));
		}

	}

	public void setName(String name) {

		nameTV.setText(name);

		fwelItalianIB.setEnabled(true);
		fwelEnglishIB.setEnabled(true);
		fwelFrenchIB.setEnabled(true);

		welcomeLL.setVisibility(View.VISIBLE);
		flagsLL.setVisibility(View.VISIBLE);
//		fwelItalianIB.setVisibility(View.VISIBLE);
		fwelEnglishIB.setVisibility(View.VISIBLE);
		//fwelFrenchIB.setVisibility(View.VISIBLE);

		switch ()

		bannerLL.setVisibility(View.INVISIBLE);
		flagsLL.invalidate();

		welcomeLL.invalidate();
		if (App.reservation != null) {
			if (App.reservation.isMaintenance()) {
				fwcm_whole_RL.setBackgroundColor(getResources().getColor(R.color.background_red));

			} else {
				fwcm_whole_RL.setBackgroundColor(getResources().getColor(R.color.background_green));
			}
		}

	}

	public void setCarPlate(String carPlate) {
		//If the current plate is the default value don't show it but a warning
		if (carPlate.equalsIgnoreCase("XH123KM"))
			tvCarPlate.setText("CONFIGURARE TARGA");
		else
			tvCarPlate.setText(carPlate);
	}

	public void setMaintenance(boolean v) {
		isMaintenance = v;
	}

	public void resetUI() {

		nameTV.setText("");
		welcomeLL.setVisibility(View.INVISIBLE);

		//fwelItalianIB.setEnabled(false);
		//fwelItalianIB.setVisibility(View.INVISIBLE);

		//fwelEnglishIB.setEnabled(false);
		//fwelEnglishIB.setVisibility(View.INVISIBLE);

		//fwelFrenchIB.setEnabled(false);
		//fwelFrenchIB.setVisibility(View.INVISIBLE);

		bannerLL.setVisibility(View.VISIBLE);
		bannerLL.invalidate();
		flagsLL.setVisibility(View.GONE);
		flagsLL.invalidate();

	}

	private Handler localHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case 0:
					if (tvDateTime != null) tvDateTime.setText(sdf.format(new Date()));
					this.sendEmptyMessageDelayed(0, 1000);
					break;

			}
		}
	};

	@Override
	public void onDestroy() {
		bannerLL = null;
		flagsLL = null;
		fwelEnglishIB = null;
		fwelFrenchIB = null;
		fwelItalianIB = null;
		super.onDestroy();
	}
}
