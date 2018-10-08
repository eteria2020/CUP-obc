package eu.philcar.csg.OBC.controller.welcome;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import javax.inject.Inject;

import eu.philcar.csg.OBC.ABase;
import eu.philcar.csg.OBC.ASOS;
import eu.philcar.csg.OBC.AWelcome;
import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.controller.FBase;
import eu.philcar.csg.OBC.data.datasources.repositories.EventRepository;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.service.CarInfo;
import eu.philcar.csg.OBC.service.MessageFactory;

public class FMaintenance extends FBase {

    private DLog dlog = new DLog(this.getClass());
    @Inject
    EventRepository eventRepository;

    public static FMaintenance Instance;
    private Handler handler = new Handler();

    public static FMaintenance newInstance() {
        return new FMaintenance();
    }

    public FMaintenance() {
        Instance = this;

    }

    private View view;
    private AlertDialog dialog;

    private void askPin() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());

        ((AWelcome) this.getActivity()).sendMessage(MessageFactory.setDisplayStatus(true));

        builder.setTitle("Inserire PIN");
        // Set up the input
        final EditText input = new EditText(this.getActivity());
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", null);

        builder.setNegativeButton("Salta pagina", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                ((ABase) getActivity()).pushFragment(FPin.newInstance(), FPin.class.getName(), true);
                dlog.d("Skipped FMaintenance");
                eventRepository.Maintenance("Skip");
            }
        });

        builder.setCancelable(false);
        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnShowListener(new OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {
                Button b = FMaintenance.this.dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                final DialogInterface d = dialog;
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String pwd = input.getText().toString();
                        int npwd = 0;

                        if (App.currentTripInfo != null && App.currentTripInfo.customer != null) {
                            npwd = App.currentTripInfo.customer.checkPin(pwd);
                        }

                        if (npwd > 0) {
                            eventRepository.Maintenance("Enter");
                            dlog.d("Entering FMaintenance");
                            d.dismiss();
                        } else {
                            input.setText("");
                            Toast.makeText(FMaintenance.this.getActivity(), "PIN errato", Toast.LENGTH_SHORT).show();
                            dlog.d("Wrong pin");
                            eventRepository.Maintenance("Wrong pin");
                        }
                    }
                });
            }

        });
        dialog.show();
        dlog.d("FMaintenance asking pin");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        App.get(getActivity()).getComponent().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //App.Charging = true;
        eventRepository.Maintenance("Show");
        askPin();
        ((AWelcome) getActivity()).sendMessage(MessageFactory.requestCarInfo());
        view = inflater.inflate(R.layout.f_maintenance, container, false);

        view.findViewById(R.id.tvCountdown).setVisibility(View.GONE);

        view.findViewById(R.id.btnEndCharging).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dlog.d("Pushed EndCharging");
                ((AWelcome) getActivity()).sendMessage(MessageFactory.sendEndCharging());
                eventRepository.Maintenance("EndCharging");
                ((TextView) view.findViewById(R.id.tvChargingStatus)).setText(R.string.maintenance_status_wait);

            }
        });

        view.findViewById(R.id.fmaintSOSB).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), ASOS.class));
            }
        });

        view.findViewById(R.id.btnCarUpdate).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dlog.d("Pushed CarUpdate");
                try {
                    update(((AWelcome) getActivity()).getLocalCarInfo());
                } catch (Exception e) {
                    dlog.e("Exception while updating LocalCarInfo", e);
                }

            }
        });

        view.findViewById(R.id.btnEndCharging).setEnabled(false);
        ((TextView) view.findViewById(R.id.tvChargingStatus)).setText(R.string.maintenance_status_wait);

		/*new CountDownTimer(4000,1000) {
            @Override
			public void onTick(long millisUntilFinished) {
				((TextView)view.findViewById(R.id.tvCountdown)).setText((millisUntilFinished/1000)+ " s");
			}

			@Override
			public void onFinish() {
				((View)view.findViewById(R.id.tvCountdown)).setVisibility(View.INVISIBLE);


			}

		}.start();*/

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    update(((AWelcome) getActivity()).getLocalCarInfo());
                } catch (Exception e) {
                    dlog.e("Exception while updating LocalCarInfo", e);
                }
            }
        }, 500);
        return view;
    }

    public void update(CarInfo carinfo) {
        if (!FMaintenance.this.isVisible()) {
            return;
        }
        dlog.d("update App.Charging: " + App.isCharging() + " chargingPlug: " + carinfo.isChargingPlug());
        if (App.isCharging() && !carinfo.isChargingPlug()) {
            view.findViewById(R.id.btnEndCharging).setEnabled(true);
            ((TextView) view.findViewById(R.id.tvChargingStatus)).setText(R.string.maintenance_status_done);
        } else {
            view.findViewById(R.id.btnEndCharging).setEnabled(false);
            if (carinfo.isChargingPlug())
                ((TextView) view.findViewById(R.id.tvChargingStatus)).setText(R.string.maintenance_status_plug_insert);
            else
                ((ABase) getActivity()).pushFragment(FPin.newInstance(), FPin.class.getName(), true);
        }
    }

    @Override
    public boolean handleBackButton() {
        return false;
    }

    public void handleCarInfo(CarInfo carinfo) {

        update(carinfo);
    }

    @Override
    public void onPause() {
        super.onPause();
        Instance = null;
        //dialog.dismiss();
    }

    @Override
    public void onDestroy() {
        Instance = null;
        //dialog.dismiss();
        super.onDestroy();
    }
}
