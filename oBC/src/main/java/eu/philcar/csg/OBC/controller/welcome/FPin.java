package eu.philcar.csg.OBC.controller.welcome;

import eu.philcar.csg.OBC.ABase;
import eu.philcar.csg.OBC.ASOS;
import eu.philcar.csg.OBC.AFAQ;
import eu.philcar.csg.OBC.AWelcome;
import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.controller.FBase;
import eu.philcar.csg.OBC.controller.map.DFProgressing;
import eu.philcar.csg.OBC.db.Customer;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.Debug;
import eu.philcar.csg.OBC.interfaces.OnTripCallback;
import eu.philcar.csg.OBC.service.MessageFactory;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class FPin extends FBase implements OnClickListener, OnTripCallback {

	public static FPin newInstance() {
		
		FPin fp = new FPin();
		return fp;
	}
	
	private DFProgressing progressDF;
	
	private Button b1,b2,b3,b4,b5,b6,b7,b8,b9,b0,sosB,faqB;
	private ImageButton deleteIB;
	private ImageView p1,p2,p3,p4;
	private TextView messageTV;
	private FrameLayout fpinRightFrame;
	private Handler localHandler = new Handler();
	private DLog dlog = new DLog(this.getClass());
	private final Runnable skipRemoteCheck = new Runnable() {
		@Override
		public void run() {
			progressDF.dismiss();

            if(getActivity()!=null)
			    ((AWelcome) getActivity()).sendMessage(MessageFactory.checkPin(pin));

			pinChecked(App.currentTripInfo.CheckPin(pin, true));
		}
	};
	
	private String pin;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		this.pin = "";
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


		
		View view = inflater.inflate(R.layout.f_pin, container, false);
		dlog.d("OnCreareView FPin");
		
		progressDF = DFProgressing.newInstance(R.string.checking_pin);
		
		b1 = (Button)view.findViewById(R.id.fpinOneB);
		b2 = (Button)view.findViewById(R.id.fpinTwoB);
		b3 = (Button)view.findViewById(R.id.fpinThreeB);
		b4 = (Button)view.findViewById(R.id.fpinFourB);
		b5 = (Button)view.findViewById(R.id.fpinFiveB);
		b6 = (Button)view.findViewById(R.id.fpinSixB);
		b7 = (Button)view.findViewById(R.id.fpinSevenB);
		b8 = (Button)view.findViewById(R.id.fpinEightB);
		b9 = (Button)view.findViewById(R.id.fpinNineB);
		b0 = (Button)view.findViewById(R.id.fpinZeroB);
		
		p1 = (ImageView)view.findViewById(R.id.fpinFirstIV);
		p2 = (ImageView)view.findViewById(R.id.fpinSecondIV);
		p3 = (ImageView)view.findViewById(R.id.fpinThirdIV);
		p4 = (ImageView)view.findViewById(R.id.fpinFourthIV);

		fpinRightFrame= (FrameLayout) view.findViewById(R.id.fpinRightFrame);

		messageTV = (TextView)view.findViewById(R.id.fpinMessageTV);
		
		Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "interstateregular.ttf");
		messageTV.setTypeface(font);
		((TextView)view.findViewById(R.id.fpin_pin_is_TV)).setTypeface(font);
		
		deleteIB = (ImageButton)view.findViewById(R.id.fpinDeleteIB);

		sosB = (Button)view.findViewById(R.id.fpinSOSB);
		faqB = (Button)view.findViewById(R.id.fpinFAQB);

		sosB.setOnClickListener(this);
		faqB.setOnClickListener(this);
		
		b1.setOnClickListener(this);
		b2.setOnClickListener(this);
		b3.setOnClickListener(this);
		b4.setOnClickListener(this);
		b5.setOnClickListener(this);
		b6.setOnClickListener(this);
		b7.setOnClickListener(this);
		b8.setOnClickListener(this);
		b9.setOnClickListener(this);
		b0.setOnClickListener(this);
		
		deleteIB.setEnabled(pin!=null&&pin.trim().length()==0);




			if (App.currentTripInfo!=null && App.currentTripInfo.isMaintenance) {
				fpinRightFrame.setBackgroundColor(getResources().getColor(R.color.background_red));

			} else {
				fpinRightFrame.setBackgroundColor(getResources().getColor(R.color.background_green));
			}



		return view;
	}

	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {
		case R.id.fpinOneB: addPin("1");
			break;
		case R.id.fpinTwoB: addPin("2");
			break;
		case R.id.fpinThreeB: addPin("3");
			break;
		case R.id.fpinFourB: addPin("4");
			break;
		case R.id.fpinFiveB: addPin("5");
			break;
		case R.id.fpinSixB: addPin("6");
			break;
		case R.id.fpinSevenB: addPin("7");
			break;
		case R.id.fpinEightB: addPin("8");
			break;
		case R.id.fpinNineB: addPin("9");
			break;
		case R.id.fpinZeroB: addPin("0");
			break;
		case R.id.fpinDeleteIB:
			deletePin();
			break;
		case R.id.fpinSOSB:
			startActivity(new Intent(getActivity(), ASOS.class));
			break;
		case R.id.fpinFAQB:
			startActivity(new Intent(getActivity(), AFAQ.class));
			break;
		}
	}
	
	private void addPin(String number) {
		
		pin += number;
		
		switch (pin.length()) {
		case 1: p1.setImageResource(R.drawable.img_pin_full);
			deleteIB.setEnabled(true);
			deleteIB.setOnClickListener(this);
			break;
		case 2: p2.setImageResource(R.drawable.img_pin_full);
			break;
		case 3: p3.setImageResource(R.drawable.img_pin_full);
			break;
		default: p4.setImageResource(R.drawable.img_pin_full);
			
			deleteIB.setEnabled(false);
			deleteIB.setOnClickListener(null);
			
			progressDF.show(getFragmentManager(), DFProgressing.class.getName());
			
			if (Debug.IGNORE_HARDWARE) {
				
				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						pinChecked(pin.equals("1234"));
					}
				}, 2000);
				
			} else {


				localHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						checkTripResult();
					}
				}, 400);
			}
			
			return;
		}
		
		messageTV.setText(R.string.pin_type);
	}
	
	private void deletePin() {
		
		pin = pin.substring(0, pin.length()-1);
		
		switch (pin.length()) {
		case 0: 
			p1.setImageResource(R.drawable.img_pin_empty);
			deleteIB.setEnabled(false);
			deleteIB.setOnClickListener(null);
			break;
		case 1:
			p2.setImageResource(R.drawable.img_pin_empty);
			break;
		case 2: 
			p3.setImageResource(R.drawable.img_pin_empty);
			break;
		case 3: 
			p4.setImageResource(R.drawable.img_pin_empty);
			break;
		}
		
		messageTV.setText(R.string.pin_type);
	}

	/*@Override
	public void onDetach() {
		super.onDetach();
		progressDF=null;
		localHandler =null;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		progressDF.dismiss();
		localHandler.removeCallbacksAndMessages(null);
	}*/

	private void pinChecked(boolean valid) {
		
		progressDF.dismiss();
		
		boolean result;
		//if (Debug.IGNORE_HARDWARE) {
			result = valid;
		/*} else {
			result = (App.currentTripInfo!=null?App.currentTripInfo.CheckPin(pin):false);
		}*/
		
		if (result) {
			
			disableUI();
			
			App.pinChecked = true;
			App.Instance.persistPinChecked();
			if(App.currentTripInfo.trip.n_pin!= Customer.N_COMPANY_PIN) {
				messageTV.setText(R.string.pin_right);
			}else{
				messageTV.setText(R.string.pin_business);
			}
			
			localHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					((ABase)getActivity()).pushFragmentnoBack(FInstructions.newInstance(true), FInstructions.class.getName(), true, FPin.this);
				}
			}, 1000);
			
		} else {
			
			disableUI();
			
			messageTV.setText(R.string.pin_wrong);
			
			pin = "";
			
			Handler h = new Handler();
			h.postDelayed(new Runnable() {
				@Override
				public void run() {
					p1.setImageResource(R.drawable.img_pin_empty);
					p2.setImageResource(R.drawable.img_pin_empty);
					p3.setImageResource(R.drawable.img_pin_empty);
					p4.setImageResource(R.drawable.img_pin_empty);
					
					messageTV.setText(R.string.pin_type);
					
					enableUI();
				}
			}, 1000);
		}
	}
	
	private void enableUI() {
		
		b1.setEnabled(true);
		b2.setEnabled(true);
		b3.setEnabled(true);
		b4.setEnabled(true);
		b5.setEnabled(true);
		b6.setEnabled(true);
		b7.setEnabled(true);
		b8.setEnabled(true);
		b9.setEnabled(true);
		b0.setEnabled(true);
	}
	
	private void disableUI() {
		
		b1.setEnabled(false);
		b2.setEnabled(false);
		b3.setEnabled(false);
		b4.setEnabled(false);
		b5.setEnabled(false);
		b6.setEnabled(false);
		b7.setEnabled(false);
		b8.setEnabled(false);
		b9.setEnabled(false);
		b0.setEnabled(false);
	}

	void checkTripResult(){
		if(App.currentTripInfo.trip.offline || App.currentTripInfo.trip.begin_sent){
			if(App.currentTripInfo.trip.recharge>=0 ) {

				try {

					progressDF.dismiss();
					if(getActivity()!=null)
						((AWelcome) getActivity()).sendMessage(MessageFactory.checkPin(pin));

					pinChecked(App.currentTripInfo.CheckPin(pin, true));
				}catch(Exception e){
					progressDF.dismiss();
					dlog.e("Exception while checking the pin resetting view",e);
					disableUI();

					messageTV.setText(R.string.pin_wrong);

					pin = "";

					localHandler.postDelayed(new Runnable() {
						@Override
						public void run() {
							p1.setImageResource(R.drawable.img_pin_empty);
							p2.setImageResource(R.drawable.img_pin_empty);
							p3.setImageResource(R.drawable.img_pin_empty);
							p4.setImageResource(R.drawable.img_pin_empty);

							messageTV.setText(R.string.pin_type);

							enableUI();
						}
					}, 1000);
				}
			}else{
				progressDF.dismiss();
				disableUI();
				switch (App.currentTripInfo.trip.recharge) {

					case -15://open trip

						messageTV.setText(R.string.other_trip_message);
						break;

					case -16://customers disabled

						messageTV.setText("SEI DISABILITATO");
						break;

					case -26://preauth fail
						messageTV.setTextSize(29);
						messageTV.setText("Inserire una carta valida nell'area personale per procedere con l'apertura della corsa.");
						break;
					case -27://preauth fail
						messageTV.setTextSize(29);
						messageTV.setText("Credito insufficiente sulla carta registrata per aprire la corsa, contattare il servizio clienti per ulteriori dettagli.");
						break;
					case -28:
					case -29:
						messageTV.setTextSize(29);
						messageTV.setText("Errore sulla preautorizzazione controlla i tuoi dati di pagamento o contatta il servizio clienti");
						break;


					default:

						messageTV.setText("ERRORE GENERICO CONTATTA IL SERVIZIO CLIENTI");
						//nuovi errori
				}


			}
		}else{
			localHandler.postDelayed(skipRemoteCheck,10000);
		}
	}

	@Override
	public void onTripResult(int response) {
		if(progressDF!=null && progressDF.isAdded())
		checkTripResult();
		/*localHandler.removeCallbacks(skipRemoteCheck);
		try {

			progressDF.dismiss();
			((AWelcome) getActivity()).sendMessage(MessageFactory.checkPin(pin));

			if (App.currentTripInfo.trip.recharge != -15) {
				pinChecked(App.currentTripInfo.CheckPin(pin, true));
			} else {
				messageTV.setText(R.string.other_trip_message);
				return;
			}
		}catch(Exception e){
			progressDF.dismiss();
			dlog.e("Exception while checking the pin resetting view",e);
			disableUI();

			messageTV.setText(R.string.pin_wrong);

			pin = "";

			localHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					p1.setImageResource(R.drawable.img_pin_empty);
					p2.setImageResource(R.drawable.img_pin_empty);
					p3.setImageResource(R.drawable.img_pin_empty);
					p4.setImageResource(R.drawable.img_pin_empty);

					messageTV.setText(R.string.pin_type);

					enableUI();
				}
			}, 1000);
		}*/
	}
}
