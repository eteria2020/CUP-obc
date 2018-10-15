package eu.philcar.csg.OBC.controller.map.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;

import eu.philcar.csg.OBC.AMainOBC.GeocodedLocation;
import eu.philcar.csg.OBC.R;

public class ALVGeocoding extends BaseAdapter {

	public interface ALVGeocodingDelegate {
		void onRowSelected(GeocodedLocation location);
	}

	private Context context;
	private ArrayList<GeocodedLocation> locations;
	private ALVGeocodingDelegate delegate;

	public ALVGeocoding(Context context, ArrayList<GeocodedLocation> locations, ALVGeocodingDelegate delegate) {
		this.context = context;
		this.locations = locations;
		this.delegate = delegate;
	}

	public void setLocations(ArrayList<GeocodedLocation> locations) {
		this.locations = locations;
	}

	@Override
	public int getCount() {

		return locations.size();
	}

	@Override
	public Object getItem(int position) {

		return locations.get(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {

		if (convertView == null) {
			convertView = LayoutInflater.from(context).inflate(R.layout.alv_geocoding, parent, false);
		}

		TextView addressTV = (TextView) convertView.findViewById(R.id.alvgeoAddressTV);

		ImageButton disclosureIndicatorIB = (ImageButton) convertView.findViewById(R.id.alvgeoDisclosureIndicatorIB);

		final GeocodedLocation location = locations.get(position);

		addressTV.setText(location.address);

		disclosureIndicatorIB.setFocusable(false);
		disclosureIndicatorIB.setFocusableInTouchMode(false);

		disclosureIndicatorIB.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (delegate != null) {
					delegate.onRowSelected(location);
				}
			}
		});

		return convertView;
	}
}
