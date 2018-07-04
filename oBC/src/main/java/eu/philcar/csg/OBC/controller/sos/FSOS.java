package eu.philcar.csg.OBC.controller.sos;

import eu.philcar.csg.OBC.ABase;
import eu.philcar.csg.OBC.ASOS;
import eu.philcar.csg.OBC.AWelcome;
import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.controller.FBase;
import eu.philcar.csg.OBC.controller.FNumber;
import eu.philcar.csg.OBC.data.datasources.repositories.EventRepository;
import eu.philcar.csg.OBC.db.Events;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.ServiceTestActivity;
import eu.philcar.csg.OBC.service.MessageFactory;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;

public class FSOS extends FBase implements OnClickListener {

	public static FSOS newInstance() {
		
		FSOS fsos = new FSOS();
		return fsos;
	}

	@Inject
	EventRepository eventRepository;
	
	private TextView messageTV, numberTV,tvCarPlate;
	private ImageButton cancelIB;
	private FrameLayout fsos_right_FL;
	private Button changeNumberB, dialCallB;
	private int logoTaps =0;
	private DLog dlog = new DLog(this.getClass());
	
	private String customerCenterNumber;


	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		App.get(getActivity()).getComponent().inject(this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		View view = inflater.inflate(R.layout.f_sos, container, false);

		dlog.d("OnCreareView FSOS");
		
		Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "interstateregular.ttf");
				
		cancelIB = (ImageButton)view.findViewById(R.id.fsosCancelIB);
		
		messageTV = (TextView)view.findViewById(R.id.fsosMessageTV);
		numberTV = (TextView)view.findViewById(R.id.fsosNumberTV);
		tvCarPlate = (TextView)view.findViewById(R.id.tvCarPlate);
		fsos_right_FL=(FrameLayout)view.findViewById(R.id.fsos_right_FL);
		
		numberTV.setTypeface(font);
		
		messageTV.setTypeface(font);

		tvCarPlate.setTypeface(font);

		if (App.CarPlate.equalsIgnoreCase("XH123KM"))
			tvCarPlate.setText("CONFIGURARE TARGA");
		else
			tvCarPlate.setText(App.CarPlate);
		
		dialCallB = (Button)view.findViewById(R.id.fsosDialCallB);
		changeNumberB = (Button)view.findViewById(R.id.fsosChangeNumberB);
		
		messageTV.setText(R.string.call_book);
		((ImageView)view.findViewById(R.id.fsosAlertIV)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				logoTaps++;
			}
		});

		// Verify long press after min 5 taps for show service login dialog
		((ImageView)view.findViewById(R.id.fsosAlertIV)).setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				if (logoTaps>=5) {
					logoTaps=0;
					askAdminPassword();
				}
				return true;
			}
		});
		
		dialCallB.setOnClickListener(this);
		changeNumberB.setOnClickListener(this);
		cancelIB.setOnClickListener(this);

		if (App.currentTripInfo!=null && App.currentTripInfo.isMaintenance) {
			fsos_right_FL.setBackgroundColor(getResources().getColor(R.color.background_red));

		} else {
			fsos_right_FL.setBackgroundColor(getResources().getColor(R.color.background_green));
		}
		return view;
	}

	@Override
	public void onResume() {
		
		super.onResume();
		
		if (App.currentTripInfo!=null && App.currentTripInfo.customer!=null) {
			customerCenterNumber = App.currentTripInfo.customer.mobile;
		} else {
			customerCenterNumber = "";
		}
		
		numberTV.setText(customerCenterNumber);
	}
	
	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {
		
		case R.id.fsosCancelIB:
			((ABase)getActivity()).popFragment();
			break;
			
		case R.id.fsosChangeNumberB:
			dlog.d("Click on fsosChangeNumberB");
			((ABase)getActivity()).pushFragment(FNumber.newInstance(), FNumber.class.getName(), true);
			break;
			
		case R.id.fsosDialCallB:
			
			((ASOS)getActivity()).sendMessage(MessageFactory.requestCallCenterCallSOS(customerCenterNumber));
			
			changeNumberB.setVisibility(View.INVISIBLE);
			dialCallB.setVisibility(View.INVISIBLE);
			messageTV.setText(R.string.call_dealt);
			break;
		}
	}

	private void askAdminPassword() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());

		((ASOS)this.getActivity()).sendMessage(MessageFactory.setDisplayStatus(true));

		builder.setTitle("Accesso manutenzione");

		// Add edit text for password input
		final EditText input = new EditText(this.getActivity());
		input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		builder.setView(input);

		// Set up the buttons
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String pwd =  input.getText().toString();
				App.isAdmin=0;
				//TODO: use external config for password in hashed form
				if (pwd.equals("Roger18")) App.isAdmin=1;
				if (pwd.equals("redrum18"))  App.isAdmin=2;

				if (App.isAdmin>0) {
					eventRepository.DiagnosticPage(App.isAdmin);
					Intent intent = new Intent(FSOS.this.getActivity(), ServiceTestActivity.class);
					FSOS.this.getActivity().startActivity(intent);
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


}
