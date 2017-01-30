package eu.philcar.csg.OBC.controller.welcome.adapter;

import eu.philcar.csg.OBC.R;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class LADamages extends BaseAdapter {

	public static final int ADDITIONAL_ITEMS_PER_LIST = 3;
	
	private ArrayList<String> damages;
	private Context context;
	
	public LADamages(Context context, ArrayList<String> damages) {
		this.context = context;
		this.damages = damages;
	}
	
	@Override
	public int getCount() {
		return damages.size() + ADDITIONAL_ITEMS_PER_LIST;
	}

	@Override
	public Object getItem(int position) {
		
		if (position < damages.size()) {
			return damages.get(position);
		}
		
		return null;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		if (convertView == null) {
			convertView = LayoutInflater.from(context).inflate(R.layout.la_damages, parent, false);
		}
		
		TextView damageTV = (TextView)convertView.findViewById(R.id.ladamDamageTV);

		Typeface font = Typeface.createFromAsset(context.getAssets(), "interstateregular.ttf");
		damageTV.setTypeface(font);
		
		if (position < damages.size()) {
			damageTV.setText(damages.get(position));
		} else {
			damageTV.setText("");
		}
		
		return convertView;
	}

}
