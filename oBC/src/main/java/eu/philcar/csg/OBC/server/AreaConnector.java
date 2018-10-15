package eu.philcar.csg.OBC.server;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.helpers.DLog;

@Deprecated
public class AreaConnector implements RemoteEntityInterface {

	private DLog dlog = new DLog(this.getClass());

	public static AreaConnector GetDownloadConnector() {
		return new AreaConnector();
	}

	public int MsgId() {
		return Connectors.MSG_DN_AREAS;
	}

	public String GetRemoteUrl() {
		return App.URL_Area;
	}

	public int DecodeJson(String responseBody) {

		if (responseBody == null || responseBody.isEmpty() || responseBody.trim().isEmpty())
			return MsgId();

		File file = new File(App.getAppDataPath(), "area.json");

		try {
			OutputStream os = new FileOutputStream(file);
			os.write(responseBody.getBytes());
			os.close();
		} catch (IOException e) {
			dlog.e("File output error area.json", e);
			return MsgId();
		}

		App.Instance.initAreaPolygon();

		return MsgId();
	}

	public eDirection getDirection() {
		return eDirection.DOWNLOAD;
	}

	public String EncodeJson() {

		return null;
	}

	@Override
	public List<NameValuePair> GetParams() {

		ArrayList<NameValuePair> list = new ArrayList<NameValuePair>();
		list.add(new BasicNameValuePair("targa", "" + App.CarPlate));
		list.add(new BasicNameValuePair("md5", "" + App.AreaPolygonMD5));

		return list;
	}

}
