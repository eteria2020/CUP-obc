package eu.philcar.csg.OBC.server;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.helpers.DLog;

public class NoRedButton implements RemoteEntityInterface {
	private SharedPreferences preferences;
	private Context context;

	public NoRedButton(Context c) {
		this.context = c;
	}

	@Override
	public int MsgId() {
		return 0;
	}

	@Override
	public String GetRemoteUrl() {
		return App.URL_NRD;
	}

	@Override
	public List<NameValuePair> GetParams() {
		ArrayList<NameValuePair> list = null;
		list = new ArrayList<NameValuePair>();
		list.add(new BasicNameValuePair("plate", App.CarPlate));
		return list;
	}

	@Override
	public int DecodeJson(String response) {

		preferences = context.getSharedPreferences(App.COMMON_PREFERENCES, Context.MODE_PRIVATE);
		Editor edit = preferences.edit();
		edit.putString("NRD", response);
		edit.apply();

		DLog.I("NRD :" + response);
		return 0;
	}

	@Override
	public String EncodeJson() {
		return null;
	}

	@Override
	public eDirection getDirection() {
		return eDirection.DOWNLOAD;
	}

}



