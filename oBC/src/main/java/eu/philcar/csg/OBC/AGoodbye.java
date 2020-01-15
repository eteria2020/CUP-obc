package eu.philcar.csg.OBC;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

import eu.philcar.csg.OBC.controller.map.FPark;
import eu.philcar.csg.OBC.controller.welcome.FDamages;
import eu.philcar.csg.OBC.controller.welcome.FGoodbye;
import eu.philcar.csg.OBC.helpers.Clients;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.StubActivity;
import eu.philcar.csg.OBC.service.MessageFactory;
import eu.philcar.csg.OBC.service.ObcService;
import eu.philcar.csg.OBC.service.ServiceConnector;

public class AGoodbye extends ABase {

	public static final String JUMP_TO_END = "JUMP_TO_END";
	public static final String EUTHANASIA = "I_WANT_TO_DIE";
	public final boolean SKIP_DAMAGES = true;
	private DLog dlog = new DLog(this.getClass());
	private ServiceConnector serviceConnector;
	private GoodbyServiceHandler serviceHandler = new GoodbyServiceHandler(new WeakReference<>(this));

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		if (getIntent().getBooleanExtra(EUTHANASIA, true)) {
			Intent i = new Intent(this, StubActivity.class);
			startActivity(i);
			finish();
		}

		//System.gc();
		dlog.d("AGoodbye.onCreate();extra: " + getIntent().getBooleanExtra(EUTHANASIA, true));

		setContentView(R.layout.a_base);

		serviceConnector = new ServiceConnector(this, serviceHandler);


		/*if (savedInstanceState == null) {


		}*/
	}

	@Override
	protected void onResume() {
		super.onResume();
		App.setForegroundActivity(this);
		if (!serviceConnector.isConnected())
			serviceConnector.connect(Clients.Goodbye);

	}

	@Override
	protected void onPause() {

		super.onPause();

		App.setForegroundActivity("Pause");
		if (serviceConnector.isConnected()) {
			serviceConnector.unregister();
			serviceConnector.disconnect();
		}
	}

	@Override
	protected int getPlaceholderResource() {
		return R.id.awelPlaceholderFL;
	}

	@Override
	public void sendMessage(Message msg) {
		serviceConnector.send(msg);
	}

	public void setAudioSystem(int mode, int volume) {
		this.sendMessage(MessageFactory.AudioChannel(mode, volume));
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (serviceConnector.isConnected()) {
			serviceConnector.unregister();
			serviceConnector.disconnect();
		}
		serviceConnector = null;
		if(serviceHandler!=null){
			serviceHandler.removeCallbacksAndMessages(null);
		}
		serviceHandler = null;
		App.isClosing = false;
	}

	@Override
	public int getActivityUID() {
		return App.AGOODBYE_UID;
	}

	static class GoodbyServiceHandler extends Handler {

		final WeakReference<AGoodbye> goodbyeWeakReference;

		GoodbyServiceHandler(WeakReference<AGoodbye> goodbyeWeakReference) {
			this.goodbyeWeakReference = goodbyeWeakReference;
		}

		@Override
		public void handleMessage(Message msg) {
			try {
				FPark fPark;
				if (msg.what != ObcService.MSG_TRIP_END) {

					if (!App.isForegroundActivity(goodbyeWeakReference.get())) {
						DLog.W("AGoodbye.handleMessage();MSG to non foreground activity. ignoring");
						if (App.currentTripInfo == null) {
							DLog.W("AGoodbye.handleMessage();no trip found wrong foreground activity restarting AWelcome");
							if (!App.isForegroundActivity(AWelcome.class.getName())) {
								Intent i = new Intent(goodbyeWeakReference.get(), AWelcome.class);
								i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
								goodbyeWeakReference.get().startActivity(i);
							}

							goodbyeWeakReference.get().finish();
						}
						return;
					}
					if (App.currentTripInfo == null) {

						DLog.W(AGoodbye.class.getName() + " no trip found restarting AWelcome");
						Intent i = new Intent(goodbyeWeakReference.get(), AWelcome.class);
						i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						goodbyeWeakReference.get().startActivity(i);
						goodbyeWeakReference.get().finish();
						return;
					}
				}
				switch (msg.what) {

					case ObcService.MSG_CLIENT_REGISTER:
						DLog.I("AGoodbye.handleMessage();MSG_CLIENT_REGISTER");

						// Since this is the first fragment, we need to use the "add" method to show it to the user, and not the "replace"
						FragmentTransaction transaction = goodbyeWeakReference.get().getFragmentManager().beginTransaction();
						String fragmentName = goodbyeWeakReference.get().getIntent().getStringExtra("fragment");

						if (fragmentName != null && fragmentName.equals("damages")) {
							transaction.add(R.id.awelPlaceholderFL, FDamages.newInstance(false), FDamages.class.getName());
							transaction.addToBackStack(FDamages.class.getName());
						} else if (goodbyeWeakReference.get().SKIP_DAMAGES || (goodbyeWeakReference.get().getIntent() != null && goodbyeWeakReference.get().getIntent().getBooleanExtra(AGoodbye.JUMP_TO_END, false))) {
							DLog.D("AGoodbye.handleMessage();JUMP_TO_END");//Create fragment and add bundle to indicate the explicit request to close trip

							Fragment f = FGoodbye.newInstance();
							Bundle b = new Bundle();
							b.putBoolean("CLOSE", true);
							f.setArguments(b);

							transaction.add(R.id.awelPlaceholderFL, f, FGoodbye.class.getName());
							transaction.addToBackStack(FGoodbye.class.getName());
						} else {
							transaction.add(R.id.awelPlaceholderFL, FDamages.newInstance(true), FDamages.class.getName());
							transaction.addToBackStack(FDamages.class.getName());
						}

						transaction.commit();
						break;

					case ObcService.MSG_CMD_TIMEOUT:
						DLog.D("AGoodbye.handleMessage();MSG_CMD_TIMEOUT");
						break;

					case ObcService.MSG_PING:
						DLog.D("AGoodbye.handleMessage();MSG_PING");
						break;

					case ObcService.MSG_IO_RFID:

						DLog.D("AGoodbye.handleMessage();MSG_IO_RFID");
						break;

					case ObcService.MSG_TRIP_END:

						App.userDrunk = false;
						App.Instance.persistUserDrunk();

						Intent i = new Intent(goodbyeWeakReference.get(), AWelcome.class);
						i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						goodbyeWeakReference.get().startActivity(i);
						goodbyeWeakReference.get().finish();

						break;

					case ObcService.MSG_TRIP_BEGIN:

						//NON PRENDERE COME VALIDO QUESTO EVENTO VIENE GENERATO ANCHE A CORSA GIà APERTA

						break;

					case ObcService.MSG_CUSTOMER_INFO:

						break;

					case ObcService.MSG_TRIP_PARK_CARD_BEGIN:
						fPark = (FPark) goodbyeWeakReference.get().getFragmentManager().findFragmentByTag(FPark.class.getName());

						if (fPark != null) {
							fPark.showBeginPark();
						}
						break;

					case ObcService.MSG_TRIP_PARK_CARD_END:
						fPark = (FPark) goodbyeWeakReference.get().getFragmentManager().findFragmentByTag(FPark.class.getName());

						if (fPark != null) {
							fPark.showEndPark();
						}
						break;

					default:
						super.handleMessage(msg);
				}
			} catch (Exception e) {
				DLog.E("AGoodbye.handleMessage();Exception while handling message", e);
			}
		}
	}
}
