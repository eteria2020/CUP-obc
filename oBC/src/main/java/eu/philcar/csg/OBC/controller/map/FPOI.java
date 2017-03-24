package eu.philcar.csg.OBC.controller.map;

import eu.philcar.csg.OBC.ABase;
import eu.philcar.csg.OBC.ASOS;
import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.AMainOBC;
import eu.philcar.csg.OBC.AMainOBC.FuelStation;
import eu.philcar.csg.OBC.AMainOBC.POI;
import eu.philcar.csg.OBC.controller.FBase;
import eu.philcar.csg.OBC.controller.map.adapter.ALVPOI;
import eu.philcar.csg.OBC.controller.map.adapter.ALVPOI.ALVFuelStationsDelegate;
import eu.philcar.csg.OBC.controller.welcome.VerticalSeekBar;
import eu.philcar.csg.OBC.controller.welcome.adapter.LADamages;
import eu.philcar.csg.OBC.helpers.DLog;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class FPOI extends FBase implements OnClickListener, ALVFuelStationsDelegate, OnItemClickListener, OnSeekBarChangeListener, OnScrollListener  {

	public static FPOI newInstance(boolean poi) {
		
		FPOI fpoi = new FPOI();
		
		fpoi.poi = poi;
		
		return fpoi;
	}
	
	private ALVPOI adapter;
	private ListView fuelStationsLV;
	private VerticalSeekBar barVSB;
	
	private Button sosB;
	private ImageButton backIB, disclosureIndicatorIB;
	private ImageView descriptionIV;
	private TextView messageTV, titleTV, addressTV, distanceTV, timeTV, detailTV;
	private RelativeLayout descriptionRL;
	private FrameLayout fpoi_right_FL;
	private DLog dlog = new DLog(this.getClass());
	
	private boolean poi;
	
	private int oldFirstItem = 0;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		View view = inflater.inflate(R.layout.f_poi, container, false);

		dlog.d("OnCreareView FPOI");
		
		int count = 0;
		if (poi) {
			adapter = new ALVPOI(getActivity(), ((AMainOBC)getActivity()).getPOIs(), this);
			count = ((AMainOBC)getActivity()).getPOIs().size();
		} else {
			adapter = new ALVPOI(getActivity(), ((AMainOBC)getActivity()).computeFuelStations(), this);
			count = ((AMainOBC)getActivity()).computeFuelStations().size();
		}
		
		Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "interstateregular.ttf");
		
		titleTV = (TextView)view.findViewById(R.id.fpoiTitleTV);
		titleTV.setTypeface(font);
		addressTV = (TextView)view.findViewById(R.id.fpoiAddressTV);
		addressTV.setTypeface(font);
		distanceTV = (TextView)view.findViewById(R.id.fpoiDistanceTV);
		distanceTV.setTypeface(font);
		timeTV = (TextView)view.findViewById(R.id.fpoiTimeTV);
		timeTV.setTypeface(font);
		descriptionIV = (ImageView)view.findViewById(R.id.fpoiDescriptionImageIV);
		detailTV = (TextView)view.findViewById(R.id.fpoiDescriptionDetailsTV);
		detailTV.setTypeface(font);
		
		disclosureIndicatorIB = (ImageButton)view.findViewById(R.id.fpoiDisclosureIndicatorIB);

		fpoi_right_FL=(FrameLayout)view.findViewById(R.id.fpoi_right_FL);
		
		descriptionRL = (RelativeLayout)view.findViewById(R.id.fpoiDescriptionRL);
		
		messageTV = (TextView)view.findViewById(R.id.fpoiMessageTV);
		messageTV.setTypeface(font);
		
		fuelStationsLV = (ListView)view.findViewById(R.id.fpoiFuelStationsLV);
		barVSB = (VerticalSeekBar)view.findViewById(R.id.fpoiBarSB);
		
		barVSB.setMax(count-1 > 0 ? count-1 : 1);
		barVSB.setProgress(count-1 > 0 ? count-1 : 1);
		
		if (count-1 > 0) {
			fuelStationsLV.setOnScrollListener(this);
			barVSB.setEnabled(true);
			barVSB.setOnSeekBarChangeListener(this);
		} else {
			fuelStationsLV.setOnScrollListener(null);
			barVSB.setEnabled(false);
			barVSB.setOnSeekBarChangeListener(null);
		}
		
		sosB = (Button)view.findViewById(R.id.fpoiSOSB);
		backIB = (ImageButton)view.findViewById(R.id.fpoiBackIB);
		
		fuelStationsLV.setAdapter(adapter);
		
		updateUI();
		
		fuelStationsLV.setOnItemClickListener(this);
		
		disclosureIndicatorIB.setOnClickListener(this);
		
		sosB.setOnClickListener(this);
		backIB.setOnClickListener(this);

		if (App.currentTripInfo!=null && App.currentTripInfo.isMaintenance) {
			fpoi_right_FL.setBackgroundColor(getResources().getColor(R.color.background_red));

		} else {
			fpoi_right_FL.setBackgroundColor(getResources().getColor(R.color.background_green));
		}
		
		return view;
	}
	
	private void updateUI() {
		
		if (((AMainOBC)getActivity()).getFuelStation() != null) {
			fuelStationsLV.setVisibility(View.INVISIBLE);
			descriptionRL.setVisibility(View.VISIBLE);
		} else {
			fuelStationsLV.setVisibility(View.VISIBLE);
			descriptionRL.setVisibility(View.INVISIBLE);
		}
		
		if (((AMainOBC)getActivity()).getPOI() != null) {
			messageTV.setText(R.string.go_to_map_or_back);
		} else {
			if (poi) {
				messageTV.setText(R.string.select_a_destination);
			} else {
				messageTV.setText(R.string.select_fuel_station);
			}
		}
	}

	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {
		
		case R.id.fpoiBackIB:
			
			if (((AMainOBC)getActivity()).getPOI() != null) {
				((AMainOBC)getActivity()).setPOI(null);
				((AMainOBC)getActivity()).setEndingPosition(null);
				((AMainOBC)getActivity()).setCurrentRouting(null);
				updateUI();
			} else {
				((ABase)getActivity()).popFragment();
			}
			
			break;
		
		case R.id.fpoiSOSB:
			startActivity(new Intent(getActivity(), ASOS.class));
			break;
		
		case R.id.fpoiDisclosureIndicatorIB:

			try {
				((ABase)getActivity()).popTillFragment(FMap.class.getName());
			}  catch (Exception e) {
				dlog.d("Exception while popping fragment");
			}
			break;
			
		}
	}

	@Override
	public void onRowSelected(int position) {
		
		if (!App.isNavigatorEnabled) {
			try {
				((ABase)getActivity()).popTillFragment(FDriving.class.getName());
			} catch (Exception e) {
				dlog.d("Exception while popping fragment");
			}
			return;
		}
		
		if (poi) {
			selectPOI(((AMainOBC)getActivity()).getPOIs().valueAt(position));
			updateUI();
		} else {
			FuelStation fs = (FuelStation)adapter.getItem(position);//((AMainOBC)getActivity()).fuelStationValueAt(position);
			((AMainOBC)getActivity()).setFuelStation(fs);
			((AMainOBC)getActivity()).setEndingPosition(fs.location);
			((AMainOBC)getActivity()).setCurrentRouting(null);

			try {
				((ABase)getActivity()).popTillFragment(FMap.class.getName());
			} catch (Exception e) {
				dlog.d("Exception while popping fragment");
			}
		}
	}


	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
		
		if (!App.isNavigatorEnabled) {
			try {
				((ABase)getActivity()).popTillFragment(FDriving.class.getName());
			} catch (Exception e) {
				dlog.d("Exception while popping fragment");
			}
			return;
		}
		
		if (poi) {
			selectPOI(((AMainOBC)getActivity()).getPOIs().valueAt(position));
			updateUI();
		} else {
			FuelStation fs = (FuelStation)adapter.getItem(position);//((AMainOBC)getActivity()).fuelStationValueAt(position);
			((AMainOBC)getActivity()).setFuelStation(fs);
			((AMainOBC)getActivity()).setEndingPosition(fs.location);
			((AMainOBC)getActivity()).setCurrentRouting(null);

			try {
				((ABase)getActivity()).popTillFragment(FMap.class.getName());
			} catch (Exception e) {
				dlog.d("Exception while popping fragment");
			}
		}
	}
	
	private void selectPOI(POI poi) {
		
		((AMainOBC)getActivity()).setPOI(poi);
		((AMainOBC)getActivity()).setEndingPosition(poi.location);
		((AMainOBC)getActivity()).setCurrentRouting(null);
		
		int distance = poi.distance;
		int kilometers = distance/1000;
		int meters = distance % 1000;
		
		titleTV.setText(poi.title);
		addressTV.setText(poi.address);
		distanceTV.setText((kilometers > 0 ? String.valueOf(kilometers)+"," : "") + 
				(kilometers > 0 ? (meters >= 100 ? String.valueOf(meters) : (meters > 10 ? "0" + String.valueOf(meters) : "00" + String.valueOf(meters))) : String.valueOf(meters)) +  
				" " + (kilometers > 0 ? getResources().getString(R.string.mu_distance_kilometers) : getResources().getString(R.string.mu_distance_meters)) );
		timeTV.setText(String.valueOf(poi.time) + " " + getResources().getString(R.string.mu_distance_time));
		
		detailTV.setText(poi.description);
		
		// TODO: set the real image resource
		descriptionIV.setImageResource(R.drawable.img_placeholder);
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		if (oldFirstItem != firstVisibleItem) {
			oldFirstItem = firstVisibleItem;
			barVSB.setProgress(adapter.getCount()- ALVPOI.ADDITIONAL_ITEMS_PER_LIST - 1 - firstVisibleItem);
			barVSB.updateThumb();
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		int listPosition = adapter.getCount() - LADamages.ADDITIONAL_ITEMS_PER_LIST - progress - 1;
		fuelStationsLV.setSelection(listPosition);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {}
}
