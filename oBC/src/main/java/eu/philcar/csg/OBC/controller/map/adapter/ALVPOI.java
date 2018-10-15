package eu.philcar.csg.OBC.controller.map.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v4.util.LongSparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import eu.philcar.csg.OBC.AMainOBC.POI;
import eu.philcar.csg.OBC.R;

public class ALVPOI extends BaseAdapter {

	public static final int ADDITIONAL_ITEMS_PER_LIST = 3;

	public interface ALVFuelStationsDelegate {
		void onRowSelected(int position);
	}

	private Context context;
	private LongSparseArray<?> pois;
	private ALVFuelStationsDelegate delegate;

	public ALVPOI(Context context, LongSparseArray<?> pois, ALVFuelStationsDelegate delegate) {
		this.context = context;
		this.pois = pois;
		this.delegate = delegate;
	}

	@Override
	public int getCount() {

		return pois.size() + ADDITIONAL_ITEMS_PER_LIST;
	}

	@Override
	public Object getItem(int position) {

		if (position < pois.size()) {
			return pois.valueAt(position);
		}

		return null;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {

		if (convertView == null) {
			convertView = LayoutInflater.from(context).inflate(R.layout.alv_fuel_stations, parent, false);
		}

		Typeface font = Typeface.createFromAsset(context.getAssets(), "interstateregular.ttf");

		TextView titleTV = (TextView) convertView.findViewById(R.id.alvfsTitleTV);
		TextView addressTV = (TextView) convertView.findViewById(R.id.alvfsAddressTV);
		TextView distanceTV = (TextView) convertView.findViewById(R.id.alvfsDistanceTV);
		TextView timeTV = (TextView) convertView.findViewById(R.id.alvfsTimeTV);

		ImageButton disclosureIndicatorIB = (ImageButton) convertView.findViewById(R.id.alvfsDisclosureIndicatorIB);

		titleTV.setTypeface(font);
		addressTV.setTypeface(font);
		distanceTV.setTypeface(font);
		timeTV.setTypeface(font);

		if (position < pois.size()) {

			POI poi = (POI) pois.valueAt(position);

			int distance = poi.distance;
			int kilometers = distance / 1000;
			int meters = distance % 1000;

			String theDistance = (kilometers > 0 ? String.valueOf(kilometers) + "," : "") +
					(kilometers > 0 ? String.valueOf(meters / 100) : String.valueOf(meters)) + // (meters >= 100 ? String.valueOf(meters) : (meters > 10 ? "0" + String.valueOf(meters) : "00" + String.valueOf(meters)))
					" " + (kilometers > 0 ? context.getResources().getString(R.string.mu_distance_kilometers) : context.getResources().getString(R.string.mu_distance_meters));

			titleTV.setText(poi.title);
			addressTV.setText(poi.address);
			distanceTV.setText(theDistance);
			timeTV.setText(poi.time > 0 ? String.valueOf(poi.time) : "" + " " + context.getResources().getString(R.string.mu_distance_time));

			disclosureIndicatorIB.setVisibility(View.VISIBLE);

			disclosureIndicatorIB.setFocusable(false);
			disclosureIndicatorIB.setFocusableInTouchMode(false);

			disclosureIndicatorIB.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (delegate != null) {
						delegate.onRowSelected(position);
					}
				}
			});

		} else {

			titleTV.setText("");
			addressTV.setText("");
			distanceTV.setText("");
			timeTV.setText("");
			disclosureIndicatorIB.setVisibility(View.INVISIBLE);
		}

		return convertView;
	}

}
