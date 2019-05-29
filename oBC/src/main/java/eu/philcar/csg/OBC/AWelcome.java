package eu.philcar.csg.OBC;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import java.lang.ref.WeakReference;

import eu.philcar.csg.OBC.controller.welcome.FMaintenance;
import eu.philcar.csg.OBC.controller.welcome.FWelcome;
import eu.philcar.csg.OBC.helpers.Clients;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.ServiceTestActivity;
import eu.philcar.csg.OBC.interfaces.OnTripCallback;
import eu.philcar.csg.OBC.service.CarInfo;
import eu.philcar.csg.OBC.service.MessageFactory;
import eu.philcar.csg.OBC.service.ObcService;
import eu.philcar.csg.OBC.service.ServiceConnector;
import eu.philcar.csg.OBC.service.TripInfo;

@SuppressWarnings("deprecation")
@SuppressLint({"Wakelock"})
public class AWelcome extends ABase {

	public static final int MSG_RELEASE_SCREEN = 10001;
	WakeLock screenLock;
	private DLog dlog = new DLog(this.getClass());
	private ServiceConnector serviceConnector;
	private CarInfo localCarInfo = null;
	private WelcomeServiceHandler serviceHandler = new WelcomeServiceHandler(new WeakReference<AWelcome>(this));

	protected void setEngine(boolean status) {
		DLog.D("Request enable engine");
		sendMessage(MessageFactory.setEngine(status));
		sendMessage(MessageFactory.setEngine(status));
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		//System.gc();
		dlog.d("AWelcome onCreate");
		setContentView(R.layout.a_base);

		if (getIntent().getBooleanExtra("EXIT", false)) {
			finish();
		}

		App.userDrunk = false;
		App.Instance.persistUserDrunk();
		if (App.currentTripInfo == null) {
			App.askClose.clear();
		}

		serviceConnector = new ServiceConnector(this, serviceHandler);

		screenLock = ((PowerManager) getSystemService(POWER_SERVICE)).newWakeLock(
				PowerManager.ON_AFTER_RELEASE | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, this.getClass().getSimpleName());

		if (savedInstanceState == null) {

			// Since this is the first fragment, we need to use the "add" method to show it to the user, and not the "replace"
			FragmentTransaction transaction = getFragmentManager().beginTransaction();

			transaction.add(R.id.awelPlaceholderFL, FWelcome.newInstance(), FWelcome.class.getName());
			transaction.addToBackStack(FWelcome.class.getName());

			transaction.commit();
		}
	}

	@Override
	protected void onPause() {

		super.onPause();
		//App.setForegroundActivity(this.getClass().toString() +"Pause");

	}

	@Override
	protected void onDestroy() {

		super.onDestroy();

		serviceConnector.unregister();
		serviceConnector.disconnect();
	}

	@Override
	public void sendMessage(Message msg) {
		serviceConnector.send(msg);
	}

	@Override
	protected void onResume() {

		super.onResume();

		App.setForegroundActivity(this);

		if (!serviceConnector.isConnected())
			serviceConnector.connect(Clients.Welcome);

	}

	@Override
	protected int getPlaceholderResource() {
		return R.id.awelPlaceholderFL;
	}

	public CarInfo getLocalCarInfo() {
		return localCarInfo;
	}

	@Override
	public int getActivityUID() {
		return App.AWELCOME_UID;
	}

	static class WelcomeServiceHandler extends Handler {

		final WeakReference<AWelcome> welcomeWeakReference;

		public WelcomeServiceHandler(WeakReference<AWelcome> welcomeWeakReference) {
			this.welcomeWeakReference = welcomeWeakReference;
		}

		@Override
		public void handleMessage(Message msg) {
			try {

				if (!App.isForegroundActivity(welcomeWeakReference.get()) && !App.isForegroundActivity(ServiceTestActivity.class.getName())) {
					DLog.W(AWelcome.class.getName() + " MSG to non foreground activity. ");
					//AWelcome.this.finish();
					return;
				}

				switch (msg.what) {

					case ObcService.MSG_CLIENT_REGISTER:
						DLog.D(AWelcome.class.getName() + ": MSG_CLIENT_REGISTER");
						break;

					case ObcService.MSG_IO_RFID:
						welcomeWeakReference.get().screenLock.acquire();
						Message lmsg = this.obtainMessage(welcomeWeakReference.get().MSG_RELEASE_SCREEN);
						this.sendMessageDelayed(lmsg, 2000);

						break;

					case ObcService.MSG_API_TRIP_CALLBACK:
						Fragment frag = welcomeWeakReference.get().getActiveFragment();// getFragmentManager().findFragmentById(R.id.awelPlaceholderFL);
						if (frag != null && frag instanceof OnTripCallback && msg.obj instanceof TripInfo) {

							((OnTripCallback) frag).onTripResult((TripInfo) msg.obj);

						} else {
							DLog.I(AWelcome.class.getName() + " Impossible cast a OnTripCallback  :" + frag);
						}
						break;

					case ObcService.MSG_TRIP_BEGIN:
						DLog.D("perf: received MSG_TRIP_BEGIN");
						TripInfo ti = (TripInfo) msg.obj;
						if (msg.arg1 == 0) {
							if (ti != null && ti.customer != null) {
								Fragment fb = welcomeWeakReference.get().getActiveFragment();// getFragmentManager().findFragmentById(R.id.awelPlaceholderFL);
								if (fb != null && fb instanceof FWelcome) {
									welcomeWeakReference.get().runOnUiThread(() -> {
										FWelcome f = (FWelcome) fb;
										f.setName(ti.customer.name + " " + ti.customer.surname);
										f.setMaintenance(ti.isMaintenance);
										DLog.D("perf: show flag");
									});

								} else {
									DLog.E(AWelcome.class.getName() + "Impossibile cast a FWelcome  :");
								}
							}
						} else {

							App.Instance.loadPinChecked();

							if (App.pinChecked) {
								welcomeWeakReference.get().startActivity(new Intent(welcomeWeakReference.get(), AMainOBC.class));
								welcomeWeakReference.get().finish();
							} else {
								FWelcome fWelcome = (FWelcome) welcomeWeakReference.get().getFragmentManager().findFragmentByTag(FWelcome.class.getName());
								if (fWelcome != null) {
									fWelcome.setName(ti.customer.name + " " + ti.customer.surname);
									fWelcome.setMaintenance(ti.isMaintenance);
									DLog.D(AWelcome.class.getName() + "AWelcome: visualizzazione bandiere e nome corsa già aperta");
								}
							}
						}
						break;

					case ObcService.MSG_TRIP_END:
						DLog.D("perf: received MSG_TRIP_END");
						try {
							welcomeWeakReference.get().popTillFragment(FWelcome.class.getName());
						} catch (PopTillException ex) {
							DLog.E("AWelcome: Exception during popTillFragment refreshing activity ", ex);
							welcomeWeakReference.get().finish();
						} catch (Exception e) {
							DLog.E(AWelcome.class.getName() + "AWelcome: Exception during popTillFragment", e);
						}

						final FWelcome welcome = (FWelcome) welcomeWeakReference.get().getFragmentManager().findFragmentByTag(FWelcome.class.getName());
						if (welcome != null) {
							welcomeWeakReference.get().runOnUiThread(new Runnable() {
								@Override
								public void run() {
									DLog.D("perf: reset ui");
									welcome.resetUI();
								}
							});
						}

						break;

					case ObcService.MSG_CUSTOMER_CHECKPIN:

						String pin = (String) msg.obj;
						if (App.currentTripInfo.CheckPin(pin, false)) {
							welcomeWeakReference.get().startActivity(new Intent(welcomeWeakReference.get(), AMainOBC.class));
							welcomeWeakReference.get().finish();
						}
						break;
					case ObcService.MSG_CAR_INFO:
						if (msg.obj != null && msg.obj instanceof CarInfo) {
							welcomeWeakReference.get().localCarInfo = (CarInfo) msg.obj;
						}
						FWelcome fWelcome = (FWelcome) welcomeWeakReference.get().getFragmentManager().findFragmentByTag(FWelcome.class.getName());
						if (fWelcome != null && fWelcome.isVisible()) {
							fWelcome.setCarPlate(App.CarPlate);
						}

						FMaintenance fmaintenance = (FMaintenance) welcomeWeakReference.get().getFragmentManager().findFragmentByTag(FMaintenance.class.getName());
						if (fmaintenance != null) {

							if (msg.obj != null && msg.obj instanceof CarInfo) {
								fmaintenance.handleCarInfo((CarInfo) msg.obj);
							}

						}

					case AWelcome.MSG_RELEASE_SCREEN:

						if (welcomeWeakReference.get().screenLock.isHeld())
							welcomeWeakReference.get().screenLock.release();
						break;

					default:
						super.handleMessage(msg);
				}
			} catch (Exception e) {
				DLog.E("Excepton handling aWelcome Message", e);
			}
		}
	}
}
