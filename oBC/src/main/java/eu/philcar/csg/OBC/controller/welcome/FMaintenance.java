package eu.philcar.csg.OBC.controller.welcome;

import eu.philcar.csg.OBC.ABase;
import eu.philcar.csg.OBC.AGoodbye;
import eu.philcar.csg.OBC.ASOS;
import eu.philcar.csg.OBC.AWelcome;
import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.AMainOBC;
import eu.philcar.csg.OBC.controller.FBase;
import eu.philcar.csg.OBC.controller.map.FMap;
import eu.philcar.csg.OBC.db.Events;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.ServiceTestActivity;
import eu.philcar.csg.OBC.service.CarInfo;
import eu.philcar.csg.OBC.service.MessageFactory;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Html;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class FMaintenance extends FBase {

	private DLog dlog = new DLog(this.getClass());
	public static FMaintenance Instance;

	public static FMaintenance newInstance() {
		return new FMaintenance();
	}

	public FMaintenance(){
		Instance = this;
	}

	private  View view;



	private void askPin() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());

		((AWelcome)this.getActivity()).sendMessage(MessageFactory.setDisplayStatus(true));

		builder.setTitle("Inserire PIN");
		// Set up the input
		final EditText input = new EditText(this.getActivity());
		// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
		input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
		builder.setView(input);

		// Set up the buttons
		builder.setPositiveButton("OK",null);


		builder.setNegativeButton("Salta pagina", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				((ABase)getActivity()).pushFragment(FPin.newInstance(), FPin.class.getName(), true);
				dlog.d("Skipped FMaintenance");
				Events.Maintenance("Skip");
			}
		});

		builder.setCancelable(false);
		final AlertDialog  d = builder.create();
		d.setCanceledOnTouchOutside(false);
		d.setOnShowListener(new OnShowListener() {

			@Override
			public void onShow(DialogInterface dialog) {
				Button b = d.getButton(AlertDialog.BUTTON_POSITIVE);
				final DialogInterface d = dialog;
				b.setOnClickListener( new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						String pwd =  input.getText().toString();
						int npwd=0;

						if (App.currentTripInfo!=null && App.currentTripInfo.customer!=null) {
							npwd = App.currentTripInfo.customer.checkPin(pwd);
						}

						if (npwd>0) {
							Events.Maintenance("Enter");
							dlog.d("Entering FMaintenance");
							d.dismiss();
						} else {
							input.setText("");
							Toast.makeText(FMaintenance.this.getActivity(), "PIN errato", Toast.LENGTH_SHORT).show();;
							dlog.d("Wrong pin");
							Events.Maintenance("Wrong pin");
						}
					}
				});
			}

		});
		d.show();
		dlog.d("FMaintenance asking pin");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Events.Maintenance("Show");
		askPin();

		view = inflater.inflate(R.layout.f_maintenance, container, false);

		((View)view.findViewById(R.id.finsNextIB)).setVisibility(View.INVISIBLE);
		((View)view.findViewById(R.id.tvCountdown)).setVisibility(View.VISIBLE);

		((Button)view.findViewById(R.id.finsNextIB)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				((ABase)getActivity()).pushFragment(FPin.newInstance(), FPin.class.getName(), true);((ABase)getActivity()).pushFragment(FPin.newInstance(), FPin.class.getName(), true);

				if(App.Charging) {
					dlog.d("Skipped FMaintenance");
					Events.Maintenance("Skip");
				}
				dlog.d("Closing FMaintenance");
			}
		});


		((Button)view.findViewById(R.id.btnEndCharging)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dlog.d("Pushed EndCharging");
				((AWelcome)getActivity()).sendMessage(MessageFactory.sendEndCharging());
				Events.Maintenance("EndCharging");
				((TextView)view.findViewById(R.id.tvEndCharging)).setText("Attedere prego stiamo caricando le informazioni della macchina per effettuare la procedura di stacco.");

			}
		});


		((Button)view.findViewById(R.id.btnEndCharging)).setEnabled(false);
		((TextView)view.findViewById(R.id.tvEndCharging)).setText("Attedere prego stiamo caricando le informazioni della macchina per effettuare la procedura di stacco.");

		new CountDownTimer(4000,1000) {
			@Override
			public void onTick(long millisUntilFinished) {
				((TextView)view.findViewById(R.id.tvCountdown)).setText((millisUntilFinished/1000)+ " s");
			}

			@Override
			public void onFinish() {
				((View)view.findViewById(R.id.tvCountdown)).setVisibility(View.INVISIBLE);
				((View)view.findViewById(R.id.finsNextIB)).setVisibility(View.VISIBLE);


			}

		}.start();

		return view;
	}


	public void update(CarInfo carinfo) {
		if(!FMaintenance.this.isVisible()){
			return;
		}
		dlog.d("update App.Charging: "+App.Charging+" chargingPlug: "+carinfo.chargingPlug);
		if (App.Charging && carinfo.chargingPlug==false) {
			((Button)view.findViewById(R.id.btnEndCharging)).setEnabled(true);
			((TextView)view.findViewById(R.id.tvEndCharging)).setText("TERMINA RICARICA: L'auto sar� nuovamente disponible al noleggio a meno di altre condizioni di allarme");
		} else {
			((Button)view.findViewById(R.id.btnEndCharging)).setEnabled(false);
			if (carinfo.chargingPlug)
				((TextView)view.findViewById(R.id.tvEndCharging)).setText("IMPOSSIBILE TERMINARE RICARCA: spina ancora inserita");
			else
				((TextView)view.findViewById(R.id.tvEndCharging)).setText("Auto non in ricarica");
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Instance=null;
	}

	@Override
	public boolean handleBackButton() {
		return false;
	}

	public void handleCarInfo(CarInfo carinfo) {
		update(carinfo);
	}
}
