package eu.philcar.csg.OBC.controller.map;

import eu.philcar.csg.OBC.ABase;
import eu.philcar.csg.OBC.ASOS;
import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.controller.FBase;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class FRefuel extends FBase implements OnClickListener {

	public static FRefuel newInstance(boolean canRefuel) {
		
		FRefuel frf = new FRefuel();
		
		frf.canRefuel = canRefuel;
		
		return frf;
	}
	
	private Button cardPINB, fuelStationListB, sosB;
	private ImageButton backIB;
	private ImageView fuelStatusIV;
	private TextView messageTV;
	private FrameLayout fref_right_FL;
	
	private boolean canRefuel;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		View view = inflater.inflate(R.layout.f_refuel, container, false);
		
		Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "interstateregular.ttf");
		
		cardPINB = (Button)view.findViewById(R.id.frefCardPinB);
		fuelStationListB = (Button)view.findViewById(R.id.frefFuelStationListB);
		sosB = (Button)view.findViewById(R.id.frefSOSB);

		fref_right_FL=(FrameLayout)view.findViewById(R.id.fref_right_FL);
		
		backIB = (ImageButton)view.findViewById(R.id.frefBackIB);
		
		fuelStatusIV = (ImageView)view.findViewById(R.id.frefFuelStatusIV);
		
		messageTV = (TextView)view.findViewById(R.id.frefMessageTV);
		
		cardPINB.setTypeface(font);
		fuelStationListB.setTypeface(font);
		sosB.setTypeface(font);
		
		messageTV.setTypeface(font);
		
		updateUI();

		if (App.currentTripInfo.isMaintenance) {
			fref_right_FL.setBackgroundColor(getResources().getColor(R.color.background_red));

		} else {
			fref_right_FL.setBackgroundColor(getResources().getColor(R.color.background_green));
		}
		
		return view;
	}
	
	private void updateUI() {
		
		if (canRefuel) {
			
			fuelStatusIV.setImageResource(R.drawable.img_fuel_empty);
			messageTV.setText(R.string.fuel_status_show_pin);
			
			cardPINB.setVisibility(View.VISIBLE);
			fuelStationListB.setVisibility(View.VISIBLE);
			
			cardPINB.setOnClickListener(this);
			fuelStationListB.setOnClickListener(this);
			
			sosB.setOnClickListener(this);
			backIB.setOnClickListener(this);
			
		} else {
			
			fuelStatusIV.setImageResource(R.drawable.img_fuel_one_quarter);
			messageTV.setText(R.string.fuel_status_one_quarter);
			
			cardPINB.setVisibility(View.INVISIBLE);
			fuelStationListB.setVisibility(View.INVISIBLE);
			
			cardPINB.setOnClickListener(null);
			fuelStationListB.setOnClickListener(null);
			
			sosB.setOnClickListener(this);
			backIB.setOnClickListener(this);
		}
	}

	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {

		case R.id.frefSOSB:
			startActivity(new Intent(getActivity(), ASOS.class));
			break;
			
		case R.id.frefBackIB:
			((ABase)getActivity()).popFragment();
			break;
			
		case R.id.frefCardPinB:
			if (App.FuelCard_PIN!=null && App.FuelCard_PIN.length() > 0 && !App.FuelCard_PIN.equalsIgnoreCase("null")) {
				((ABase)getActivity()).pushFragment(FPinCode.newInstance(App.FuelCard_PIN), FPinCode.class.getName(), true);
			}
			break;
			
		case R.id.frefFuelStationListB:
			
			((ABase)getActivity()).pushFragment(FPOI.newInstance(false), FPOI.class.getName(), true);
			break;
		
		}
	}
}
