package eu.philcar.csg.OBC.controller.map;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import eu.philcar.csg.OBC.ABase;
import eu.philcar.csg.OBC.AMainOBC;
import eu.philcar.csg.OBC.ASOS;
import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.controller.FBase;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.Debug;

public class FPinCode extends FBase implements OnClickListener {

    public static FPinCode newInstance(String code) {

        FPinCode fpc = new FPinCode();

        if (Debug.IGNORE_HARDWARE) {
            fpc.code = "1234";
        } else {
            fpc.code = code;
        }

        return fpc;
    }

    private TextView oneTV, twoTV, threeTV, fourTV, messageTV, savedMinutesTV, minutesTV;
    private ImageButton nextIB, backIB;
    private Button sosB;
    private LinearLayout cardLL, savedMinutesLL;
    private RelativeLayout fpca_right_RL;
    private ImageView fuelStatusIV;
    private DLog dlog = new DLog(this.getClass());

    private String code;
    private byte uiStatus;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.f_pin_code, container, false);
        dlog.d("OnCreareView FPinCode");

        Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "interstateregular.ttf");

        oneTV = (TextView) view.findViewById(R.id.fpcaPinCodeOneTV);
        twoTV = (TextView) view.findViewById(R.id.fpcaPinCodeTwoTV);
        threeTV = (TextView) view.findViewById(R.id.fpcaPinCodeThreeTV);
        fourTV = (TextView) view.findViewById(R.id.fpcaPinCodeFourTV);
        fpca_right_RL = (RelativeLayout) view.findViewById(R.id.fpca_right_RL);

        cardLL = (LinearLayout) view.findViewById(R.id.fpcaCardLL);
        fuelStatusIV = (ImageView) view.findViewById(R.id.fpcaFuelFullIV);
        savedMinutesLL = (LinearLayout) view.findViewById(R.id.fpcaSavedMinutesLL);
        minutesTV = (TextView) view.findViewById(R.id.fpcaMinutesTV);

        messageTV = (TextView) view.findViewById(R.id.fpcaMessageTV);

        savedMinutesTV = (TextView) view.findViewById(R.id.fpcaSavedMinutesTV);

        backIB = (ImageButton) view.findViewById(R.id.fpcaBackIB);
        sosB = (Button) view.findViewById(R.id.fpcaSOSB);
        nextIB = (ImageButton) view.findViewById(R.id.fpcaNextIB);

        ((TextView) view.findViewById(R.id.fpcaPinTV)).setTypeface(font);
        oneTV.setTypeface(font);
        twoTV.setTypeface(font);
        threeTV.setTypeface(font);
        fourTV.setTypeface(font);
        sosB.setTypeface(font);

        messageTV.setTypeface(font);
        savedMinutesTV.setTypeface(font);
        minutesTV.setTypeface(font);

        updateUI();

        backIB.setOnClickListener(this);
        sosB.setOnClickListener(this);
        nextIB.setOnClickListener(this);

        if (App.currentTripInfo != null && App.currentTripInfo.isMaintenance) {
            fpca_right_RL.setBackgroundColor(getResources().getColor(R.color.background_red));

        } else {
            fpca_right_RL.setBackgroundColor(getResources().getColor(R.color.background_green));
        }

        return view;
    }

    private void updateUI() {

        switch (uiStatus) {

            case 0:

                cardLL.setVisibility(View.VISIBLE);
                fuelStatusIV.setVisibility(View.INVISIBLE);
                savedMinutesLL.setVisibility(View.INVISIBLE);

                oneTV.setText(code.substring(0, 1));
                twoTV.setText(code.substring(1, 2));
                threeTV.setText(code.substring(2, 3));
                fourTV.setText(code.substring(3, 4));

                messageTV.setText(R.string.fuel_status_pin_card);

                break;

            case 1:

                backIB.setVisibility(View.GONE);

                cardLL.setVisibility(View.INVISIBLE);
                fuelStatusIV.setVisibility(View.VISIBLE);
                savedMinutesLL.setVisibility(View.INVISIBLE);

                messageTV.setText(R.string.refueling_done);

                break;

            case 2:

                cardLL.setVisibility(View.INVISIBLE);
                fuelStatusIV.setVisibility(View.INVISIBLE);
                savedMinutesLL.setVisibility(View.VISIBLE);

                messageTV.setText(R.string.back_to_navigation);

                // TODO: replace with real data
                String minutes = "20";
                if (minutes.length() < 2) {
                    minutes = "0" + minutes;
                }
                // END

                minutesTV.setText(minutes);

                break;

        }
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.fpcaBackIB:
                ((AMainOBC) getActivity()).popFragment();
                break;

            case R.id.fpcaSOSB:
                startActivity(new Intent(getActivity(), ASOS.class));
                break;

            case R.id.fpcaNextIB:

                if (uiStatus < 2) {

                    uiStatus++;
                    updateUI();

                } else {

                    ((AMainOBC) getActivity()).setFuelStation(null);
                    ((AMainOBC) getActivity()).setEndingPosition(null);
                    ((AMainOBC) getActivity()).setCurrentRouting(null);

                    if (App.isNavigatorEnabled) {
                        try {
                            ((ABase) getActivity()).popTillFragment(FMap.class.getName());
                        } catch (Exception e) {
                            dlog.d("Exception while popping fragment");
                        }
                    } else {
                        try {
                            ((ABase) getActivity()).popTillFragment(FDriving.class.getName());
                        } catch (Exception e) {
                            dlog.d("Exception while popping fragment");
                        }
                        return;
                    }
                }

                break;

        }
    }

    // TODO: remove this method
    @Override
    public boolean handleBackButton() {

        if (0 < uiStatus && uiStatus <= 2) {
            uiStatus--;
            updateUI();
            return true;
        }

        return super.handleBackButton();
    }
}
