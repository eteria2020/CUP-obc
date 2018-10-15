package eu.philcar.csg.OBC.controller.welcome;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import eu.philcar.csg.OBC.ABase;
import eu.philcar.csg.OBC.AGoodbye;
import eu.philcar.csg.OBC.ASOS;
import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.controller.FBase;

public class FDrunk extends FBase implements OnClickListener {

	public static FDrunk newInstance() {

		FDrunk fd = new FDrunk();

		return fd;
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.f_drunk, container, false);

		Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "interstateregular.ttf");

		((TextView) view.findViewById(R.id.fdruMessageTV)).setTypeface(font);

		((Button) view.findViewById(R.id.fdruYesB)).setTypeface(font);
		view.findViewById(R.id.fdruYesB).setOnClickListener(this);

		((Button) view.findViewById(R.id.fdruNoB)).setTypeface(font);
		view.findViewById(R.id.fdruNoB).setOnClickListener(this);

		((Button) view.findViewById(R.id.fdruSOSB)).setTypeface(font);
		view.findViewById(R.id.fdruSOSB).setOnClickListener(this);

		return view;
	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {
			case R.id.fdruYesB:

				App.userDrunk = true;
				App.Instance.persistUserDrunk();

				Intent i = new Intent(getActivity(), AGoodbye.class);
				i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				i.putExtra(AGoodbye.JUMP_TO_END, true);
				startActivity(i);
				getActivity().finish();
				break;
			case R.id.fdruNoB:

				App.userDrunk = false;
				App.Instance.persistUserDrunk();

				((ABase) getActivity()).pushFragment(FCleanliness.newInstance(), FCleanliness.class.getName(), true);
				break;
			case R.id.fdruSOSB:
				startActivity(new Intent(getActivity(), ASOS.class));
				break;
		}
	}
}
