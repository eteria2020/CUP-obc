package eu.philcar.csg.OBC.controller;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import eu.philcar.csg.OBC.ABase;
import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.helpers.DLog;

public class FNumber extends FBase implements OnClickListener {

	public static FNumber newInstance() {

		FNumber fn = new FNumber();
		return fn;
	}

	private Button b1, b2, b3, b4, b5, b6, b7, b8, b9, b0, bp;
	private DLog dlog = new DLog(this.getClass());
	private FrameLayout fchn_right_FL;
	private TextView numberTV;
	private int MAX_NUM = 11;

	private String phoneNumber;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		if (App.currentTripInfo != null && App.currentTripInfo.customer != null)
			phoneNumber = App.currentTripInfo.customer.mobile;
		else
			phoneNumber = "";

		if (phoneNumber.equalsIgnoreCase("null"))
			phoneNumber = "";
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.f_number, container, false);

		b1 = (Button) view.findViewById(R.id.fchnOneB);
		b2 = (Button) view.findViewById(R.id.fchnTwoB);
		b3 = (Button) view.findViewById(R.id.fchnThreeB);
		b4 = (Button) view.findViewById(R.id.fchnFourB);
		b5 = (Button) view.findViewById(R.id.fchnFiveB);
		b6 = (Button) view.findViewById(R.id.fchnSixB);
		b7 = (Button) view.findViewById(R.id.fchnSevenB);
		b8 = (Button) view.findViewById(R.id.fchnEightB);
		b9 = (Button) view.findViewById(R.id.fchnNineB);
		b0 = (Button) view.findViewById(R.id.fchnZeroB);
		bp = (Button) view.findViewById(R.id.fchnPlus);

		numberTV = (TextView) view.findViewById(R.id.fchnNumberTV);
		fchn_right_FL = (FrameLayout) view.findViewById(R.id.fchn_right_FL);

		Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "interstateregular.ttf");
		numberTV.setTypeface(font);

		numberTV.setText(phoneNumber);

		((TextView) view.findViewById(R.id.fchnMessageTV)).setTypeface(font);

		view.findViewById(R.id.fchnDeleteIB).setOnClickListener(this);
		view.findViewById(R.id.fchnDeleteIB).setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				phoneNumber = "";
				MAX_NUM = 11;
				updateUI();
				return true;
			}
		});
		view.findViewById(R.id.fchnNextIB).setOnClickListener(this);

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
		bp.setOnClickListener(this);

		if (App.currentTripInfo != null && App.currentTripInfo.isMaintenance) {
			fchn_right_FL.setBackgroundColor(getResources().getColor(R.color.background_red));

		} else {
			fchn_right_FL.setBackgroundColor(getResources().getColor(R.color.background_green));
		}

		return view;
	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {
			case R.id.fchnOneB:
				addNumber("1");
				break;
			case R.id.fchnTwoB:
				addNumber("2");
				break;
			case R.id.fchnThreeB:
				addNumber("3");
				break;
			case R.id.fchnFourB:
				addNumber("4");
				break;
			case R.id.fchnFiveB:
				addNumber("5");
				break;
			case R.id.fchnSixB:
				addNumber("6");
				break;
			case R.id.fchnSevenB:
				addNumber("7");
				break;
			case R.id.fchnEightB:
				addNumber("8");
				break;
			case R.id.fchnNineB:
				addNumber("9");
				break;
			case R.id.fchnZeroB:
				addNumber("0");
				break;
			case R.id.fchnPlus:
				addNumber("+");
				break;
			case R.id.fchnDeleteIB:
				removeNumber();
				break;
			case R.id.fchnNextIB:

				if (phoneNumber.length() < 9) {
					Toast.makeText(getActivity(), R.string.number_invalid, Toast.LENGTH_SHORT).show();
					return;
				}

				if (App.currentTripInfo != null && App.currentTripInfo.customer != null) {
					dlog.d("User Change phone number from " + App.currentTripInfo.customer.mobile + " to " + phoneNumber);
					App.currentTripInfo.customer.mobile = phoneNumber;
				}
				((ABase) getActivity()).popFragment();
				return;
		}

		updateUI();
	}

	private void addNumber(String number) {

		if (number.equalsIgnoreCase("+"))
			if (phoneNumber.length() != 0)
				return;
			else
				MAX_NUM = 16;

		if (phoneNumber.length() >= MAX_NUM) {
			return;
		}

//		if (phoneNumber.length() == 3) {
//			phoneNumber += " ";
//		}

		phoneNumber += number;
	}

	private void removeNumber() {

		if (phoneNumber.length() > 0) {
			phoneNumber = phoneNumber.substring(0, phoneNumber.length() - 1);

			if (phoneNumber.length() == 4) {
				phoneNumber = phoneNumber.substring(0, phoneNumber.length() - 1);
			}
		}
		if (phoneNumber.length() == 0)
			MAX_NUM = 11;
	}

	private void updateUI() {
		numberTV.setText(phoneNumber);
	}
}
