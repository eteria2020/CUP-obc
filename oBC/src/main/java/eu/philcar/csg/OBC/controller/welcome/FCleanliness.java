package eu.philcar.csg.OBC.controller.welcome;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import javax.inject.Inject;

import eu.philcar.csg.OBC.ABase;
import eu.philcar.csg.OBC.ASOS;
import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.controller.FBase;
import eu.philcar.csg.OBC.data.datasources.repositories.EventRepository;
import eu.philcar.csg.OBC.helpers.DLog;

public class FCleanliness extends FBase implements OnClickListener {

    public static FCleanliness newInstance() {

        FCleanliness fc = new FCleanliness();
        return fc;
    }

    @Inject
    EventRepository eventRepository;

    private ImageButton insideGreenLedIB, insideYellowLedIB, insideRedLedIB, outsideGreenLedIB, outsideYellowLedIB, outsideRedLedIB;
    private Button sosB;
    private FrameLayout fcle_right_FL;
    private DLog dlog = new DLog(this.getClass());

    private int insideState, outsideState;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.get(getActivity()).getComponent().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.f_cleanliness, container, false);
        dlog.d("OnCreareView FCleanliness");

        Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "interstateregular.ttf");

        ((TextView) view.findViewById(R.id.fcleMessageInsideTV)).setTypeface(font);
        ((TextView) view.findViewById(R.id.fcleMessageOutsideTV)).setTypeface(font);
        ((TextView) view.findViewById(R.id.fcleMessageTV)).setTypeface(font);
        fcle_right_FL = (FrameLayout) view.findViewById(R.id.fcle_right_FL);
        insideGreenLedIB = (ImageButton) view.findViewById(R.id.fcleInsideGreenLedIB);
        insideYellowLedIB = (ImageButton) view.findViewById(R.id.fcleInsideYellowLedIB);
        insideRedLedIB = (ImageButton) view.findViewById(R.id.fcleInsideRedLedIB);

        outsideGreenLedIB = (ImageButton) view.findViewById(R.id.fcleOutsideGreenLedIB);
        outsideYellowLedIB = (ImageButton) view.findViewById(R.id.fcleOutsideYellowLedIB);
        outsideRedLedIB = (ImageButton) view.findViewById(R.id.fcleOutsideRedLedIB);

        sosB = (Button) view.findViewById(R.id.fcleSOSB);

        enableUI();

        if (App.currentTripInfo != null && App.currentTripInfo.isMaintenance) {
            fcle_right_FL.setBackgroundColor(getResources().getColor(R.color.background_red));

        } else {
            fcle_right_FL.setBackgroundColor(getResources().getColor(R.color.background_green));
        }

        return view;
    }

    private void setInsideCleanliness(int v) {
        if (App.currentTripInfo != null && App.currentTripInfo.trip != null) {
            App.currentTripInfo.trip.int_cleanliness = v;
        }
    }

    private void setOutsideCleanliness(int v) {
        if (App.currentTripInfo != null && App.currentTripInfo.trip != null) {
            App.currentTripInfo.trip.ext_cleanliness = v;
        }
    }

    @Override
    public void onClick(View v) {

        disableUI();

        switch (v.getId()) {
            case R.id.fcleInsideGreenLedIB:
                insideState = 3;
                setInsideCleanliness(0);
                break;
            case R.id.fcleInsideYellowLedIB:
                insideState = 2;
                setInsideCleanliness(1);
                break;
            case R.id.fcleInsideRedLedIB:
                insideState = 1;
                setInsideCleanliness(2);
                break;
            case R.id.fcleOutsideGreenLedIB:
                outsideState = 3;
                setOutsideCleanliness(0);
                break;
            case R.id.fcleOutsideYellowLedIB:
                outsideState = 2;
                setOutsideCleanliness(1);
                break;
            case R.id.fcleOutsideRedLedIB:
                outsideState = 1;
                setOutsideCleanliness(2);
                break;
            case R.id.fcleSOSB:
                startActivity(new Intent(getActivity(), ASOS.class));
                break;
        }

        if (updateUI()) {
            enableUI();
        }
    }

    private boolean updateUI() {

        insideRedLedIB.setSelected(false);
        insideYellowLedIB.setSelected(false);
        insideGreenLedIB.setSelected(false);

        switch (insideState) {
            case 1:
                insideRedLedIB.setSelected(true);
                break;
            case 2:
                insideYellowLedIB.setSelected(true);
                break;
            case 3:
                insideGreenLedIB.setSelected(true);
                break;
        }

        outsideRedLedIB.setSelected(false);
        outsideYellowLedIB.setSelected(false);
        outsideGreenLedIB.setSelected(false);

        switch (outsideState) {
            case 1:
                outsideRedLedIB.setSelected(true);
                break;
            case 2:
                outsideYellowLedIB.setSelected(true);
                break;
            case 3:
                outsideGreenLedIB.setSelected(true);
                break;
        }

        if (insideState > 0 && outsideState > 0) {

            if (App.currentTripInfo != null && App.currentTripInfo.trip != null) {
                App.CounterCleanlines = 0;
                App.Instance.persistCounterCleanlines();
                eventRepository.eventCleanliness(App.currentTripInfo.trip.int_cleanliness, App.currentTripInfo.trip.ext_cleanliness);
                App.currentTripInfo.UpdateCorsa();

            }

            ((ABase) getActivity()).pushFragment(FInstructions.newInstance(true), FInstructions.class.getName(), true);

            return false;
        }

        return true;
    }

    private void disableUI() {

        insideGreenLedIB.setOnClickListener(null);
        insideYellowLedIB.setOnClickListener(null);
        insideRedLedIB.setOnClickListener(null);
        outsideGreenLedIB.setOnClickListener(null);
        outsideYellowLedIB.setOnClickListener(null);
        outsideRedLedIB.setOnClickListener(null);
        sosB.setOnClickListener(null);
    }

    private void enableUI() {

        insideGreenLedIB.setOnClickListener(this);
        insideYellowLedIB.setOnClickListener(this);
        insideRedLedIB.setOnClickListener(this);
        outsideGreenLedIB.setOnClickListener(this);
        outsideYellowLedIB.setOnClickListener(this);
        outsideRedLedIB.setOnClickListener(this);
        sosB.setOnClickListener(this);
    }
}
