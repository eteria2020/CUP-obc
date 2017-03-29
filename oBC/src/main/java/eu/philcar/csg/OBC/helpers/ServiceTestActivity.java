package eu.philcar.csg.OBC.helpers;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.Hik.Mercury.SDK.Manager.CANManager;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.db.Events;
import eu.philcar.csg.OBC.devices.UsbSerialConnection;
import eu.philcar.csg.OBC.service.CarInfo;
import eu.philcar.csg.OBC.service.MessageFactory;
import eu.philcar.csg.OBC.service.ObcService;
import eu.philcar.csg.OBC.service.ServiceConnector;
import eu.philcar.csg.OBC.service.TripInfo;


public class ServiceTestActivity extends Activity {

	private DLog dlog = new DLog(this.getClass());
	
	//Riferimento alla classe helper che si occuper� di connettersi al servizio 	
	private ServiceConnector serviceConnector =  null;
	private EditText etPlate;

	private UsbSerialConnection usc;
	private CarInfo carInfo;
	private CANManager CanManager;
	
	private boolean paused = false;
	
	WakeLock screenLock;
	WakeLock screenLockTrip;

	private final int MSG_RELEASE_SCREEN = 10001;

	int countRX=0;
	//Handler locale che ricever� i messaggi inviati dal servizio 
	
	
	
	private Handler serviceHandler = new Handler() {
		 @Override
		 public void handleMessage(Message msg) {
			Bundle b;
			

			if (! App.isForegroundActivity(ServiceTestActivity.this))
				return;

			
			switch (msg.what) {
			
			case ObcService.MSG_CLIENT_REGISTER:				
			
				Toast.makeText(ServiceTestActivity.this, "CONNECTED", Toast.LENGTH_SHORT).show();
				break;

			case ObcService.MSG_CAR_CAN_UPDATE:
				/*b = msg.getData();

				if (msg.obj!=null &&  msg.obj instanceof CarInfo) {
					updateCANData((CarInfo) msg.obj);
				}*/
				break;

			case ObcService.MSG_CAR_START_CHARGING:

				/*

				if (msg.obj!=null &&  msg.obj instanceof CarInfo) {
					updateCANData((CarInfo) msg.obj);
				}*/
				break;
				
			case ObcService.MSG_CLIENT_UNREGISTER:				
				serviceConnector.disconnect();
				ServiceTestActivity.this.finish();
				break;
				
			case ObcService.MSG_CMD_TIMEOUT:
				Toast.makeText(ServiceTestActivity.this, "TIMEOUT", Toast.LENGTH_SHORT).show();
				break;
			
			case ObcService.MSG_PING:
				Toast.makeText(ServiceTestActivity.this, "PING OK", Toast.LENGTH_SHORT).show();
				break;
				
			case ObcService.MSG_IO_RFID:
				screenLock.acquire();
				Message lmsg = this.obtainMessage(MSG_RELEASE_SCREEN);
				this.sendMessageDelayed(lmsg, 2000);
				
				
				b =  msg.getData();
				String id=null;
				String event=null;
				
				if (b!=null) {
					id = b.getString("id");
					event = b.getString("event");
				}
				
				id = (id!=null)?id:"";
				event = (event!=null)?event:"";

				Toast.makeText(ServiceTestActivity.this, "RFID : " + id +"\n" + event, Toast.LENGTH_SHORT).show();
				handleCard(id,event);
									
				
				break;
				
			case ObcService.MSG_CAR_UPDATE:
				Message rmsg = MessageFactory.requestCarInfo();
				serviceConnector.send(rmsg);
				break;
				
			case ObcService.MSG_CAR_INFO:
				b = msg.getData();				
				countRX++;
				((TextView)findViewById(R.id.tvNmsg)).setText(""+countRX);
				
				if (msg.obj!=null &&  msg.obj instanceof CarInfo) {					
					handleCarInfo((CarInfo)msg.obj);
				}
				break;
				
			case ObcService.MSG_TRIP_BEGIN:
				TripInfo ti = (TripInfo)msg.obj;
				if (ti!=null && ti.customer !=  null) {
					//((TextView)findViewById(R.id.tvStatus)).setText("Corsa aperta da " + ti.cliente.nome + " "+ ti.cliente.cognome);
				}
				BeginTimestamp = System.currentTimeMillis();
				break;
				
			case ObcService.MSG_TRIP_END:

				//((TextView)findViewById(R.id.tvStatus)).setText("Corsa chiusa");
				BeginTimestamp=0;
				break;		
				
			case ObcService.MSG_CUSTOMER_CHECKPIN:

				Toast.makeText(ServiceTestActivity.this, "PIN : " + msg.arg1, Toast.LENGTH_SHORT).show();

				break;	
				
			case MSG_RELEASE_SCREEN:
				Toast.makeText(ServiceTestActivity.this, "Lock released", Toast.LENGTH_SHORT).show();
				if (screenLock.isHeld())
					screenLock.release();
				break;
				
			default:
				super.handleMessage(msg);
			 }
		 }
	};
	
	
	long  BeginTimestamp = 0;
	
	private void handleCard(String id, String event) {
//		Message  msg;
//		if (event.equalsIgnoreCase("OPEN")) {
//			msg = MessageFactory.setLed(MessageFactory.LED_GREEN, 0);
//			serviceConnector.send(msg);				
//			msg = MessageFactory.setLed(MessageFactory.LED_RED, 1);
//			serviceConnector.send(msg);
//			//((TextView)findViewById(R.id.tvStatus)).setText("Corsa aperta da Mario Rossi");
//			//BeginTimestamp = System.currentTimeMillis();
//					
//		} else if (event.equalsIgnoreCase("CLOSE")) {
//			msg = MessageFactory.setLed(MessageFactory.LED_RED, 0);
//			serviceConnector.send(msg);			
//			msg = MessageFactory.setLed(MessageFactory.LED_GREEN, 1);
//			serviceConnector.send(msg);	
//			//((TextView)findViewById(R.id.tvStatus)).setText("Corsa chiusa");
//			//BeginTimestamp=0;
//		}
//		
	}
	
	private void handleCarInfo(CarInfo carInfo) {
		
		((TextView)findViewById(R.id.tvCarInfo)).setText(carInfo.getJson_GPRS(true));
		float currVolt=0;
		String cellsVoltage="";
		for(int i =0;i<carInfo.cellVoltageValue.length;i++){
			cellsVoltage=cellsVoltage.concat(" " + carInfo.cellVoltageValue[i]);		//battery cell voltages
			if ((i+1) % 8 == 0) {

				cellsVoltage=cellsVoltage.concat("\n");
			}
		}

		cellsVoltage=cellsVoltage.concat("Battery type: "+ carInfo.batteryType+"\n");
		cellsVoltage=cellsVoltage.concat("Low voltage cells : "+ carInfo.isCellLowVoltage +" ("+ carInfo.lowCells +")\n");
		cellsVoltage=cellsVoltage.concat("V100%: "+ App.getMax_voltage()+"V\n");
		cellsVoltage=cellsVoltage.concat("VBATT: "+ carInfo.currVoltage+"V\n");



		cellsVoltage=cellsVoltage.concat("SOC: "+ carInfo.bmsSOC +"%\n");
		cellsVoltage=cellsVoltage.concat("SOC_GPRS: "+ carInfo.bmsSOC_GPRS +"%\n");
		cellsVoltage=cellsVoltage.concat("SOC2: "+ carInfo.virtualSOC +"%\n");
		cellsVoltage=cellsVoltage.concat("SOCR : "+ carInfo.SOCR +"\n");
		cellsVoltage=cellsVoltage.concat("SOCAdmin: "+ carInfo.batteryLevel +"%\n");
		cellsVoltage=cellsVoltage.concat("outAmp: "+ carInfo.outAmp +"A\n");
		//cellsVoltage=cellsVoltage.concat("CurrentValue: "+ carInfo.current +"A\n");

		
		//((TextView)findViewById(R.id.tvRpm)).setText(""+carInfo.rpm

		//((TextView)findViewById(R.id.tvCellsInfo)).setText(cellsVoltage);
		((TextView)findViewById(R.id.tvCellsInfo)).setText(cellsVoltage);

		((TextView)findViewById(R.id.tvSpeed)).setText(""+carInfo.speed);
		((TextView)findViewById(R.id.tvFuelLevel)).setText(""+carInfo.bmsSOC);
		((TextView)findViewById(R.id.tvTarga)).setText(""+carInfo.id);
		cellsVoltage=cellsVoltage.concat("outAmp: "+ carInfo.outAmp +"A\n");
		((TextView)findViewById(R.id.tvQuadro)).setText((carInfo.isKeyOn>0)?"ON":"OFF");
		((TextView)findViewById(R.id.tvFW)).setText(carInfo.fw_version);
		//((TextView)findViewById(R.id.tvAvolt)).setText(""+carInfo.analogVoltage);
		((TextView)findViewById(R.id.tvMvolt)).setText(""+carInfo.voltage);
		((TextView)findViewById(R.id.tvKm)).setText(""+carInfo.km);
	
		
		
		/*
		if (BeginTimestamp>0) {
			long sec = (System.currentTimeMillis()-BeginTimestamp)/1000;
			((TextView)findViewById(R.id.tvTime)).setText(""+sec);
		} else {
			((TextView)findViewById(R.id.tvTime)).setText("");
		}
		*/
		
		
	}

	private void updateCANData(CarInfo carInfo) {

		((TextView)findViewById(R.id.tvCarInfo)).setText(carInfo.getJson_GPRS(true));
		float currVolt=0;
		String cellsVoltage="";
		for(int i =0;i<carInfo.cellVoltageValue.length;i++){
			cellsVoltage=cellsVoltage.concat(" " + carInfo.cellVoltageValue[i]);		//battery cell voltages
			if ((i+1) % 8 == 0) {

				cellsVoltage=cellsVoltage.concat("\n");
			}
		}

		cellsVoltage=cellsVoltage.concat("Battery type: "+ carInfo.batteryType+"\n");
		cellsVoltage=cellsVoltage.concat("V100%: "+ App.getMax_voltage()+"V\n");
		cellsVoltage=cellsVoltage.concat("VBATT: "+ carInfo.currVoltage+"V\n");



		//cellsVoltage=cellsVoltage.concat("SOC: "+ carInfo.bmsSOC +"%\n");
		cellsVoltage=cellsVoltage.concat("SOC_GPRS: "+ carInfo.bmsSOC_GPRS +"%\n");
		cellsVoltage=cellsVoltage.concat("SOC2: "+ carInfo.virtualSOC +"%\n");
		cellsVoltage=cellsVoltage.concat("Low voltage cells : "+ carInfo.isCellLowVoltage +" ("+ carInfo.lowCells +")\n");
		cellsVoltage=cellsVoltage.concat("SOCR : "+ carInfo.SOCR +"\n");
		cellsVoltage=cellsVoltage.concat("SOCAdmin: "+ carInfo.batteryLevel +"%\n");
		cellsVoltage=cellsVoltage.concat("outAmp: "+ carInfo.outAmp +"A\n");
		cellsVoltage=cellsVoltage.concat("CurrentAmp: "+ carInfo.currentAmpere +"A\n");
		cellsVoltage=cellsVoltage.concat("ChargingAmp: "+ carInfo.chargingAmpere +"A\n");
		cellsVoltage=cellsVoltage.concat("maxAmp: "+ carInfo.maxAmpere +"A\n");


		//((TextView)findViewById(R.id.tvRpm)).setText(""+carInfo.rpm

		//((TextView)findViewById(R.id.tvCellsInfo)).setText(cellsVoltage);
		((TextView)findViewById(R.id.tvCellsInfo)).setText(cellsVoltage);



	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		CanManager = CANManager.get(this);

		Events.DiagnosticPage(0);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		getWindow().addFlags(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

		setContentView(R.layout.activity_service);

		((TextView) findViewById(R.id.tvVer)).setText(App.sw_Version);

		screenLock = ((PowerManager) getSystemService(POWER_SERVICE)).newWakeLock(
				PowerManager.ON_AFTER_RELEASE | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "TAG");


		serviceConnector = new ServiceConnector(this, serviceHandler);

		//serviceConnector.startService();

		serviceConnector.connect();


		etPlate = (EditText) findViewById(R.id.etPlate);
		etPlate.setText(App.CarPlate);
		etPlate.setFilters(new InputFilter[]{new InputFilter.AllCaps(), new InputFilter.LengthFilter(10)});


		((ToggleButton) findViewById(R.id.tbLed1)).setOnCheckedChangeListener(ledToggleListener);
		((ToggleButton) findViewById(R.id.tbLed2)).setOnCheckedChangeListener(ledToggleListener);
		((ToggleButton) findViewById(R.id.tbLed3)).setOnCheckedChangeListener(ledToggleListener);
		((ToggleButton) findViewById(R.id.tbLed4)).setOnCheckedChangeListener(ledToggleListener);


		if (App.isAdmin == 1) {                    //MASSIMI PRIVILEGI ATTENZIONE!!!


			findViewById(R.id.DoorsLL).setVisibility(View.VISIBLE);
			findViewById(R.id.MotorLL).setVisibility(View.VISIBLE);
			findViewById(R.id.ServerLL).setVisibility(View.VISIBLE);
			findViewById(R.id.LogLL).setVisibility(View.VISIBLE);


			((Button) findViewById(R.id.btnOpen)).setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					Message msg = MessageFactory.openDoors();
					serviceConnector.send(msg);
				}

			});


			((Button) findViewById(R.id.btnClose)).setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					Message msg = MessageFactory.closeDoors();
					serviceConnector.send(msg);

				}

			});


			((Button) findViewById(R.id.btnEngineOn)).setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					Message msg = MessageFactory.setEngine(true);
					serviceConnector.send(msg);

				}

			});

			((Button) findViewById(R.id.btnEngineOff)).setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					Message msg = MessageFactory.setEngine(false);
					serviceConnector.send(msg);
				}

			});

			((Button) findViewById(R.id.btnAltIP)).setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					Message msg = MessageFactory.useAlternativeIP();
					serviceConnector.send(msg);
				}

			});

			((Button) findViewById(R.id.btnDefIP)).setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					Message msg = MessageFactory.useDefaultIP();
					serviceConnector.send(msg);
				}

			});

			((Button) findViewById(R.id.btnLogY)).setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					Message msg = MessageFactory.enableLog();
					serviceConnector.send(msg);
				}

			});

			((Button) findViewById(R.id.btnlogN)).setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					Message msg = MessageFactory.disableLog();
					serviceConnector.send(msg);
				}

			});




		} else {

			findViewById(R.id.DoorsLL).setVisibility(View.GONE);
			findViewById(R.id.MotorLL).setVisibility(View.GONE);
			findViewById(R.id.ServerLL).setVisibility(View.GONE);
			findViewById(R.id.LogLL).setVisibility(View.GONE);
			findViewById(R.id.TestLeaseLL).setVisibility(View.VISIBLE);
		}

    	
    	/*


    	((Button)findViewById(R.id.btnResetAdmins)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Message  msg = MessageFactory.resetAdminCards();
				serviceConnector.send(msg);
			}

    	});
    	((Button)findViewById(R.id.btnFinish)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				Message  msg = MessageFactory.stopService();
				serviceConnector.send(msg);
				
			    Intent intent = new Intent(getApplicationContext(), AWelcome.class);
			    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			    intent.putExtra("EXIT", true);
			    startActivity(intent);
				
				ServiceTestActivity.this.finish();
			}
    		
    	});
    	
        
    	
    	*/

		((Button) findViewById(R.id.btnGPRS)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				findViewById(R.id.BodyLL).setVisibility(View.VISIBLE);
				findViewById(R.id.CellsLL).setVisibility(View.GONE);


			}

		});

		((Button) findViewById(R.id.btnCells)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				findViewById(R.id.BodyLL).setVisibility(View.GONE);
				findViewById(R.id.CellsLL).setVisibility(View.VISIBLE);


					/*String cellsVoltage="";
					for(int i =0;i<28;i++){
						cellsVoltage=cellsVoltage.concat(" " + CanManager.getCellVoltageValue(i));
					}
					((TextView)findViewById(R.id.tvCellsInfo)).setText(cellsVoltage);*/


			}

		});

		((Button) findViewById(R.id.btnSet)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!etPlate.isEnabled()) {
					etPlate.setEnabled(true);
					etPlate.setFocusable(true);
					etPlate.setFocusableInTouchMode(true);
					etPlate.requestFocus();
				} else {
					String plate = etPlate.getText().toString();
					Message msg = MessageFactory.setPlate(plate);
					serviceConnector.send(msg);
					Toast.makeText(ServiceTestActivity.this, "Set new plate id  to : '" + plate + "'", Toast.LENGTH_LONG).show();
					etPlate.setEnabled(false);
					etPlate.setFocusable(false);
					((Button) findViewById(R.id.btnGo)).requestFocus();
				}
			}
		});


		((Button) findViewById(R.id.btnSettings)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 0);
			}
		});

		((Button) findViewById(R.id.btnGo)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				serviceConnector.unregister();
				ServiceTestActivity.this.finish();
				//startActivity(new Intent(ServiceTestActivity.this, AWelcome.class));


			    /*
				if (Build.DEVICE.equalsIgnoreCase("tiny4412")) {
					  usc = new UsbSerialConnection();
					  usc.enumerate(ServiceTestActivity.this);
					  usc.startIntentFilter(ServiceTestActivity.this);
					  usc.setPort(1);
					  usc.connect(ServiceTestActivity.this);
				}
			    */


			}

		});


		((Button) findViewById(R.id.btnLcd)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Message msg = MessageFactory.setDisplayStatus(false);
				serviceConnector.send(msg);
				ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();

				worker.schedule(new Runnable() {

					@Override
					public void run() {
						Message msg = MessageFactory.setDisplayStatus(true);
						serviceConnector.send(msg);
					}

				}, 30, TimeUnit.SECONDS);
			}
		});

		((Button) findViewById(R.id.btnRadio)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
//				if (!SystemControl.hasNetworkConnection(getApplicationContext()))
//					SystemControl.Reset3G(null);

				//Message  msg = MessageFactory.zmqRestart();
				//serviceConnector.send(msg);

				Intent startMain = new Intent(Intent.ACTION_MAIN);
				startMain.addCategory(Intent.CATEGORY_HOME);
				startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(startMain);

			}
		});

		((Button) findViewById(R.id.btnTestLease)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				askLeaseCard();
			}

		});


		((Button) findViewById(R.id.btnGo)).requestFocus();
		etPlate.setFocusable(false);

//	    if (Build.DEVICE.equalsIgnoreCase("tiny4412")) {
//			  UsbSerialConnection usc = new UsbSerialConnection();
//			  usc.enumerate(ServiceTestActivity.this);
//			  usc.startIntentFilter(ServiceTestActivity.this);
//	    }	  

	}


	private void askLeaseCard() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);



		builder.setTitle("Inserire hex della Card");

		// Add edit text for password input
		final EditText input = new EditText(this);
		input.setInputType(InputType.TYPE_CLASS_TEXT );
		builder.setView(input);

		// Set up the buttons
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String hex =  input.getText().toString();

				dlog.i("onLeaseReportCard : " + ", hex="+hex);
				serviceConnector.send(MessageFactory.sendDebugCard(hex));

			}
		});
		builder.setNegativeButton("Annulla", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});

		builder.show();
	}


	private OnCheckedChangeListener  ledToggleListener =	
			new OnCheckedChangeListener() {
		
				@Override
				public void onCheckedChanged(CompoundButton tb , boolean value) {				
					int led=0;
					switch (tb.getId()) {
					case R.id.tbLed1 :
						((ToggleButton)findViewById(R.id.tbLed2)).setChecked(false);
						((ToggleButton)findViewById(R.id.tbLed3)).setChecked(false);
						((ToggleButton)findViewById(R.id.tbLed4)).setChecked(false);
						led=MessageFactory.LED_GREEN;
						break;
					case R.id.tbLed2 :
						((ToggleButton)findViewById(R.id.tbLed1)).setChecked(false);
						((ToggleButton)findViewById(R.id.tbLed3)).setChecked(false);
						((ToggleButton)findViewById(R.id.tbLed4)).setChecked(false);
						
						led=MessageFactory.LED_YELLOW;
						break;
					case R.id.tbLed3 :
						((ToggleButton)findViewById(R.id.tbLed1)).setChecked(false);
						((ToggleButton)findViewById(R.id.tbLed2)).setChecked(false);
						((ToggleButton)findViewById(R.id.tbLed4)).setChecked(false);						
						led=MessageFactory.LED_BLUE;
						break;
					case R.id.tbLed4 :
						((ToggleButton)findViewById(R.id.tbLed1)).setChecked(false);
						((ToggleButton)findViewById(R.id.tbLed2)).setChecked(false);
						((ToggleButton)findViewById(R.id.tbLed3)).setChecked(false);						
						led=MessageFactory.LED_RED;
						break;
						
						
					}
					
					Message  msg = MessageFactory.setLed(led, value?MessageFactory.LED_ON:MessageFactory.LED_OFF);
					serviceConnector.send(msg);		
				}
				
			};
	
	
	@Override
	public void onResume() {
		super.onResume();
		screenLock.acquire();
		
		serviceConnector.send(MessageFactory.setDisplayStatus(true));
		
		App.setForegroundActivity(this);
		
        getWindow().addFlags(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(
                new OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                            getActionBar().show();
                        } else {
                            int mUIFlag = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LOW_PROFILE
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                            getWindow().getDecorView()
                                    .setSystemUiVisibility(mUIFlag);
                            getActionBar().hide();
                        }
                    }
                });
	}


	@Override
	protected void onPause() {
		super.onPause();

		App.setForegroundActivity(this.getClass().toString() +"Pause");

		if(screenLock.isHeld())
				screenLock.release();
		
		serviceConnector.send(MessageFactory.setDisplayStatus(false));
	}
	
	
	@Override 
	public void onDestroy() {
		super.onDestroy();
		App.isAdmin=0;
		serviceConnector.unregister();
		serviceConnector.disconnect();

		
		
	}
	


	

	
}
