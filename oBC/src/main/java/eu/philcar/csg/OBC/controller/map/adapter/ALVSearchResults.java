package eu.philcar.csg.OBC.controller.map.adapter;

import java.util.ArrayList;
import java.util.List;

import com.skobbler.ngx.SKCategories.SKPOICategory;
import com.skobbler.ngx.search.SKSearchResult;
import com.skobbler.ngx.search.SKSearchResultParent;

import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.AMainOBC.GeocodedLocation;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class ALVSearchResults extends BaseAdapter {

	public interface ALVSearchResultsDelegate {
		void onRowSelected(SKSearchResult location);
	}
	
	private Context context;
	private List<SKSearchResult> locations;
	private ALVSearchResultsDelegate delegate;
	
	public ALVSearchResults(Context context, List<SKSearchResult> locations, ALVSearchResultsDelegate delegate) {
		this.context = context;
		this.locations = locations;
		this.delegate = delegate;
	}
	
	public void setLocations(List<SKSearchResult> locations) {
		this.locations = locations;
	}
	
	private int getIcon(SKPOICategory category) {
		
		switch (category) {
		case SKPOI_CATEGORY_BUS_STATION:
		case SKPOI_CATEGORY_BUS_STOP:
			return  R.drawable.ico_bus;
	
		
		case SKPOI_CATEGORY_SUBWAY_STATION:
		case SKPOI_CATEGORY_STATION:
		case SKPOI_CATEGORY_TRAIN_STATION:
			return R.drawable.ico_rail;
				
		case SKPOI_CATEGORY_UNKNOWN:
		    return R.drawable.ico_road;
		}
		
		return 0;
		
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
		
		TextView addressTV = (TextView)convertView.findViewById(R.id.alvgeoAddressTV);
		TextView parentsTV = (TextView)convertView.findViewById(R.id.alvgeoParents);
		ImageView ivIcon = (ImageView)convertView.findViewById(R.id.ivIcon);
		ImageButton disclosureIndicatorIB = (ImageButton)convertView.findViewById(R.id.alvgeoDisclosureIndicatorIB);
		
		
		final SKSearchResult location = locations.get(position);

		List<SKSearchResultParent> parents = location.getParentsList();
		StringBuilder sb = new StringBuilder();
		if (parents!=null && parents.size()>0) {
			for(SKSearchResultParent p : parents) {
				if (sb.length()>0) sb.append(",");
				sb.append(p.getParentName());
			}
		}
		
		addressTV.setText(location.getName());
		parentsTV.setText(sb.toString());
		int id = getIcon(location.getCategory());
		if (id==0)
			return null;
		ivIcon.setImageResource(id);

		
		OnClickListener clickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (delegate != null) {
					delegate.onRowSelected(location);
				}
			}
		};
		
		addressTV.setClickable(true);
		disclosureIndicatorIB.setFocusable(false);
		disclosureIndicatorIB.setFocusableInTouchMode(false);
		
		addressTV.setOnClickListener(clickListener);
		disclosureIndicatorIB.setOnClickListener(clickListener);
		
		return convertView;
	}
}
