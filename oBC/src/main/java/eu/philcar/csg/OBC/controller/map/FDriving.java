package eu.philcar.csg.OBC.controller.map;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import eu.philcar.csg.OBC.ABase;
import eu.philcar.csg.OBC.AMainOBC;
import eu.philcar.csg.OBC.ASOS;
import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.controller.FBase;

public class FDriving extends FBase implements OnClickListener {

    public static FDriving newInstance() {
        FDriving fd = new FDriving();
        return fd;
    }

    private Button sosB;
    private ImageButton cancelIB;
    private ImageView parkingStatusIV, parkingDirectionIV, adIV;
    private TextView dayTV, timeTV;
    LinearLayout fdri_top_LL;

    Handler timerHandler = new Handler();
    @SuppressLint("SimpleDateFormat")
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {

            SimpleDateFormat day = new SimpleDateFormat("dd MMMM yyyy");
            SimpleDateFormat time = new SimpleDateFormat("HH:mm");

            if (dayTV != null) {
                dayTV.setText(day.format(new Date()));
            }
            if (timeTV != null) {
                timeTV.setText(time.format(new Date()));
            }

            if (timerHandler != null) {
                timerHandler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.f_driving, container, false);

        adIV = (ImageView) view.findViewById(R.id.fdriLeftBorderIV);

        sosB = (Button) view.findViewById(R.id.fdriSOSB);

        cancelIB = (ImageButton) view.findViewById(R.id.fdriCancelIB);
        fdri_top_LL = (LinearLayout) view.findViewById(R.id.fdri_top_LL);

        parkingStatusIV = (ImageView) view.findViewById(R.id.fdriParkingStatusIV);
        parkingDirectionIV = (ImageView) view.findViewById(R.id.fdriParkingDirectionIV);

        dayTV = (TextView) view.findViewById(R.id.fdri_date_TV);
        timeTV = (TextView) view.findViewById(R.id.fdri_hour_TV);

        AMainOBC activity = (AMainOBC) getActivity();
        if (activity != null) {
            updateParkAreaStatus(activity.isInsideParkArea(), activity.getRotationToParkAngle());
        }

        Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "interstateregular.ttf");

        sosB.setTypeface(font);

        sosB.setOnClickListener(this);
        cancelIB.setOnClickListener(this);

        App.setIsCloseable(false);

        if (App.currentTripInfo != null && App.currentTripInfo.isMaintenance) {
            fdri_top_LL.setBackgroundColor(getResources().getColor(R.color.background_red));

        } else {
            fdri_top_LL.setBackgroundColor(getResources().getColor(R.color.background_green));
        }

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        timerHandler.postDelayed(timerRunnable, 0);
    }

    public void updateParkAreaStatus(boolean isInside, float rotationAngle) {

        if (isInside) {
            parkingStatusIV.setImageResource(R.drawable.img_parking_p_green);
            parkingDirectionIV.setImageResource(R.drawable.img_parking_arrow_off);
        } else {
            parkingStatusIV.setImageResource(R.drawable.img_parking_p_red);
            parkingDirectionIV.setImageResource(R.drawable.img_parking_arrow);
        }

        parkingDirectionIV.setRotation(rotationAngle);
    }

    public void updateAd(File theFile) {

        if (theFile != null && theFile.exists()) {

            Bitmap myBitmap = null;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;

            boolean success = false;
            while (!success) {

                try {
                    myBitmap = BitmapFactory.decodeFile(theFile.getAbsolutePath(), options);
                    success = true;
                } catch (OutOfMemoryError err) {
                    options.inSampleSize *= 2;
                }
            }

            try {
                adIV.setImageBitmap(myBitmap);
            } catch (OutOfMemoryError err) {

            }
        }
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.fdriSOSB:
                startActivity(new Intent(getActivity(), ASOS.class));
                break;

            case R.id.fdriCancelIB:

                ((ABase) getActivity()).pushFragment(FMenu.newInstance(), FMenu.class.getName(), true);

                break;
        }
    }
}
