package eu.philcar.csg.OBC.controller.map;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import eu.philcar.csg.OBC.ABase;
import eu.philcar.csg.OBC.AMainOBC;
import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.controller.FBase;
import eu.philcar.csg.OBC.devices.LowLevelInterface;
import eu.philcar.csg.OBC.devices.RadioSetup;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.service.MessageFactory;
import eu.philcar.csg.OBC.service.ObcService;

public class FRadio  extends FBase {
	
	public static FRadio newInstance() {
		return new FRadio();
	}
	
	public static Bundle savedInstance;
	
	private DLog dlog = new DLog(this.getClass());
	
	
	private static final String BND_BAND = "band";
	private static final String BND_FREQ = "freq";
	private static final String BND_VOLUME = "volume";
	
	View[] RadioButton = new View[4];
	TextView tvVolume, tvFrequency, tvBand, tvChannelName;
	private View rootView;
	
	
	private int volume=0;
	private int savedVolume =60;
	private double fmFreq = 0;
	private double amFreq = 0;
	private final int MSG_CLOSE = 1;
	
	private enum Bands { AM , FM };
	private Bands band = Bands.FM;
	
	public static void setVolume(int volume) {
		if (savedInstance==null)
			savedInstance = new Bundle();
		
		savedInstance.putInt(BND_VOLUME, volume);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		
		View view = inflater.inflate(R.layout.f_radio, container, false);
		dlog.d("OnCreareView FRadio");
		rootView = view;
		
		
		Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "interstateregular.ttf");

		RadioButton[0] =  view.findViewById(R.id.lRadioButton1);
		RadioButton[1] =  view.findViewById(R.id.lRadioButton2);
		RadioButton[2] =  view.findViewById(R.id.lRadioButton3);
		RadioButton[3] =  view.findViewById(R.id.lRadioButton4);
		
		tvVolume = (TextView)view.findViewById(R.id.tvVolume);
		tvFrequency = (TextView)view.findViewById(R.id.tvFrequency);
		tvBand = (TextView)view.findViewById(R.id.tvBand);
		tvChannelName = (TextView)view.findViewById(R.id.tvChannelName);
		
		
		
		((Button)view.findViewById(R.id.btnMax)).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {				
				volume = Math.min(volume+5, 100);
				requestVolume(volume);
				rescheduleClose();
			}
			
		});;
		
		
		((Button)view.findViewById(R.id.btnMin)).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {				
				volume = Math.max(volume-5, 0);
				requestVolume(volume);
				rescheduleClose();
			}
			
		});;
		
		((Button)view.findViewById(R.id.btnZero)).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				
				if (v.getTag()==null) {
					requestVolume(0);
					v.setTag(1);
					v.setBackgroundResource(R.drawable.button_pin_pushed);
					tvVolume.setText("Mute");
					rescheduleClose();
				} else {
					requestVolume(-1);
					v.setTag(null);
					v.setBackgroundResource(R.drawable.sel_button_pin);
					tvVolume.setText(volume+ "%");
					rescheduleClose();
				}
			}
			
		});;
		
		((Button)view.findViewById(R.id.btnSeekSx)).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {	
				Message msg = MessageFactory.RadioSeek(-1, false);		
				((AMainOBC) getActivity()).sendMessage(msg);
				rescheduleClose();
			}
			
		});;
		
		((Button)view.findViewById(R.id.btnSeekDx)).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {	
				Message msg = MessageFactory.RadioSeek(1, false);		
				((AMainOBC) getActivity()).sendMessage(msg);
				rescheduleClose();
			}
			
		});;
		
		View.OnTouchListener btnChannelTouchListener =  new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				
				case MotionEvent.ACTION_DOWN:
					v.setBackgroundResource(R.drawable.sha_roundedbox_selected);
					RadioSetup.RadioChannel ch = (RadioSetup.RadioChannel) v.getTag();
					if (ch!=null) {
						requestChannel(ch.band, ch.frequency);	
					}
					break;
				case MotionEvent.ACTION_UP:
					v.setBackgroundResource(R.drawable.sha_roundedbox);
					break;
				}					
				rescheduleClose();
				return true;
			}
			
		};

		
		View.OnTouchListener btnAmFmlTouchListener =  new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				
				case MotionEvent.ACTION_DOWN:
					v.setBackgroundResource(R.drawable.button_pin_pushed);
					if (band==Bands.FM)
						band=Bands.AM;
					else 
						band=Bands.FM;
					
					selectBand(true);
	
					break;
				case MotionEvent.ACTION_UP:
					v.setBackgroundResource(R.drawable.button_pin);
					break;
				}
				rescheduleClose();
				return true;
			}
			
		};
		

		for(int i=0;i<RadioButton.length;i++) {
			RadioSetup.RadioChannel ch = App.radioSetup.getChannel(i);
			
		
			
			RadioButton[i].setTag(ch);
			if (ch!=null) {
				((TextView)RadioButton[i].findViewById(R.id.tvBtnFrequency)).setText(ch.frequency+" MHz");
				((TextView)RadioButton[i].findViewById(R.id.tvBtnBand)).setText(ch.band);
				((TextView)RadioButton[i].findViewById(R.id.tvBtnName)).setText(ch.name);
			}
			RadioButton[i].setOnTouchListener(btnChannelTouchListener);
		}
		
		((View)view.findViewById(R.id.rlAmFm)).setOnTouchListener(btnAmFmlTouchListener);
		
		if (savedInstance!=null && savedInstance.containsKey(BND_BAND) && savedInstance.containsKey(BND_FREQ) ) {			
			requestChannel(savedInstance.getString(BND_BAND), savedInstance.getDouble(BND_FREQ));
			volume = savedInstance.getInt(BND_VOLUME);
			requestVolume(volume);
			selectChannel(savedInstance.getString(BND_BAND), savedInstance.getDouble(BND_FREQ));
		} else {
			RadioSetup.RadioChannel ch = App.radioSetup.getChannel(0);
			if (ch!=null) {
				requestChannel(ch.band, ch.frequency);
				selectChannel(ch.band,ch.frequency);
				if (savedInstance!=null && savedInstance.containsKey(BND_VOLUME)) 
						volume = savedInstance.getInt(BND_VOLUME);
				else
					volume=50;
				requestVolume(volume);
				
			}
		}
		
		tvVolume.setText(volume+ "%");
		selectBand(false);
		
		dlog.d("Created view");
		
		return view;
	}
	
	

	@Override
	public void onResume() {		
		super.onResume();
		dlog.d("Resume");
		rescheduleClose();
	}
		
	@Override
	public void onPause() {
		dlog.d("Pause");
		super.onPause();
		localHandler.removeMessages(MSG_CLOSE);
		
		savedInstance = new Bundle();
		savedInstance.putString(BND_BAND, band.equals(Bands.FM)?"FM":"AM");
		savedInstance.putDouble(BND_FREQ, (band.equals(Bands.FM)?fmFreq:amFreq));
		savedInstance.putInt(BND_VOLUME, volume);
	}
	

	
	private void rescheduleClose() {
		localHandler.removeMessages(MSG_CLOSE);
		localHandler.sendEmptyMessageDelayed(MSG_CLOSE, 6000);
	}
	
	private void requestVolume(int volume) {
		((AMainOBC)getActivity()).sendMessage(MessageFactory.AudioChannel(LowLevelInterface.AUDIO_RADIO,volume));
		Message msg = MessageFactory.RadioVolume(volume);	
		((AMainOBC) getActivity()).sendMessage(msg);
	}
	
	private void requestChannel(String band, double freq) {
		Message msg = MessageFactory.RadioChannel(band, freq);
		((AMainOBC) getActivity()).sendMessage(msg);
		
	}
	
	private void selectBand(boolean changeChannel) {

		
		if (band==Bands.FM) {
			((TextView)rootView.findViewById(R.id.tvBtnFM)).setBackgroundColor(Color.rgb(0x18,0x7B,0x21));
			((TextView)rootView.findViewById(R.id.tvBtnFM)).setTextColor(Color.rgb(0xff, 0xff, 0xff));
			
			((TextView)rootView.findViewById(R.id.tvBtnAM)).setBackgroundColor(Color.TRANSPARENT);
			((TextView)rootView.findViewById(R.id.tvBtnAM)).setTextColor(Color.rgb(0x30, 0x30, 0x30));			
			if (changeChannel) requestChannel("FM", fmFreq);
			
		} else {
			((TextView)rootView.findViewById(R.id.tvBtnAM)).setBackgroundColor(Color.rgb(0x18,0x7B,0x21));
			((TextView)rootView.findViewById(R.id.tvBtnAM)).setTextColor(Color.rgb(0xff, 0xff, 0xff));
			
			((TextView)rootView.findViewById(R.id.tvBtnFM)).setBackgroundColor(Color.TRANSPARENT);
			((TextView)rootView.findViewById(R.id.tvBtnFM)).setTextColor(Color.rgb(0x30, 0x30, 0x30));		
			if (changeChannel) requestChannel("AM", amFreq);
			
		}
		
	}
	
	private void selectChannel(String band, double freq) {
		
		dlog.d("Radio: Select channel  band= " + band +  "  freq=" + freq);
		
		this.band = (band.equalsIgnoreCase("AM")? Bands.AM: Bands.FM);
		
		tvChannelName.setText("");
		for(int i=0;i<RadioButton.length; i++) {
			RadioSetup.RadioChannel ch = (RadioSetup.RadioChannel) RadioButton[i].getTag();
			if (ch!=null) dlog.d("Radio: Select channel button  : " + ch.frequency +  " " + freq + " +/- " + Math.abs(ch.frequency- freq));
			if (ch!=null && ch.band.equalsIgnoreCase(band) && Math.abs(ch.frequency- freq)< 0.05d) {
				RadioButton[i].setBackgroundResource(R.drawable.sha_roundedbox_selected);
				tvChannelName.setText(ch.name);
			} else {
				RadioButton[i].setBackgroundResource(R.drawable.sha_roundedbox);
			}
		}
		
		if (band.equalsIgnoreCase("FM"))
			fmFreq=freq;
		else if (band.equalsIgnoreCase("AM"))
			amFreq=freq;
	}
	
	private String formatFrequency(double f) {
		if (f<10) 
			return (f*1000 + " KHz");
		else
			return(f+ " MHz");
				
	}
	
	public void notifyRadioMsg(Message msg) {
		
		if (msg==null)
			return;
		double freq;
		String band;
		switch (msg.what) {
		
		case ObcService.MSG_RADIO_VOLUME_INFO:
			int volume = msg.arg1;
			tvVolume.setText(volume+ "%");
			View v = rootView.findViewById(R.id.btnZero);
			if (volume>0 && v.getTag()!=null) {
				v.setBackgroundResource(R.drawable.sel_button_pin);
				v.setTag(null);
			} else if (volume==0 &&  v.getTag()!=null ) {
				tvVolume.setText("Mute");
			}
			
			break;
		case ObcService.MSG_RADIO_CURPLAY_INFO :
			freq = msg.arg1/1000d;
			band = (String) msg.obj;
			tvFrequency.setText(formatFrequency(freq));
			tvBand.setText(band);
			selectChannel(band,freq);
			selectBand(false);
			dlog.d("CurPlayInfo: " + band +" " + freq);
			break;
		case ObcService.MSG_RADIO_SEEK_INFO:
			freq = msg.arg1/1000d;
			band = (String) msg.obj;
			tvFrequency.setText(formatFrequency(freq));
			break;
		case ObcService.MSG_RADIO_SEEK_VALID_INFO:
			freq = msg.arg1/1000d;
			band = (String) msg.obj;
			tvFrequency.setText(formatFrequency(freq));
			break;
		case ObcService.MSG_RADIO_SEEK_STATUS:
			dlog.d("RadioSeekStatus: " + msg.arg1);			
			break;
			
		}
		
		msg.recycle();
	}
	
	Handler localHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			
			switch (msg.what) {
			case MSG_CLOSE:
				try {
					((ABase)getActivity()).popTillFragment(FHome.class.getName());
				} catch (Exception e) {
					dlog.d("Exception while popping fragment");
				}
				break;
			}
		}
	};

}
