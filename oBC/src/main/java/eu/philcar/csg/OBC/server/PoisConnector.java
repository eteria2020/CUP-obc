package eu.philcar.csg.OBC.server;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.db.DbManager;
import eu.philcar.csg.OBC.db.Poi;
import eu.philcar.csg.OBC.db.Pois;
import eu.philcar.csg.OBC.helpers.DLog;

@Deprecated
public class PoisConnector implements RemoteEntityInterface {

    private DLog dlog = new DLog(this.getClass());

    public static PoisConnector GetDownloadConnector() {
        return new PoisConnector();
    }

    private long lastUpdate;

    public void setLastUpdate(long v) {
        this.lastUpdate = v;
    }

    public int MsgId() {
        return Connectors.MSG_DN_POIS;
    }

    public String GetRemoteUrl() {
        return App.URL_Pois;
    }

    public int DecodeJson(String responseBody) {

        DbManager dbm = App.Instance.getDbManager();

        Pois pois = dbm.getPoisDao();

        if (responseBody == null || responseBody.isEmpty()) {
            dlog.e("Empty json");
            return MsgId();
        }

        JSONArray jArray;
        try {

            jArray = new JSONArray(responseBody);
        } catch (JSONException e) {
            dlog.e("Errore estraendo array json", e);
            return MsgId();
        }

        int n = jArray.length();

        dlog.d("Downloaded " + n + " records");

        for (int i = 0; i < n; i++) {

            try {
                JSONObject jobj = jArray.getJSONObject(i);

                int id = jobj.getInt("id");

                // if (!clienti.isPresent(id, tms)) {
                Poi c = new Poi();
                c.id = id;
                c.type = jobj.getString("type");
                c.brand = "";
                c.code = jobj.getString("code");
                //c.type_group = jobj.getString("type_group");
                c.address = jobj.getString("address");
                c.town = jobj.getString("town").toLowerCase();
                c.type_group = "";
                c.zip = jobj.getString("zip_code");
                c.province = jobj.getString("province");
                c.lon = jobj.getDouble("lon");
                c.lat = jobj.getDouble("lat");
                c.update = jobj.getLong("update");
                c.attivo = true;

                try {
                    pois.createOrUpdate(c);
                } catch (SQLException e) {
                    dlog.e("Insert or update:", e);

                }
                // }
            } catch (JSONException e) {
                dlog.e("Errore estraendo array json", e);

            }

        }

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

        ArrayList<NameValuePair> list = null;

        if (lastUpdate > 0) {
            list = new ArrayList<NameValuePair>();
            list.add(new BasicNameValuePair("lastupdate", "" + lastUpdate));
        }

        return list;
    }

}
