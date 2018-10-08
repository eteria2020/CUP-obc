package eu.philcar.csg.OBC.server;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.db.Event;
import eu.philcar.csg.OBC.helpers.DLog;

@Deprecated
public class EventsConnector implements RemoteEntityInterface {

    private DLog dlog = new DLog(this.getClass());

    public Event event;
    public String json_data;
    public int returnMessageId;

    public int MsgId() {
        return returnMessageId;
    }

    public String GetRemoteUrl() {

        if (App.hasNetworkConnection())
            return App.URL_Eventi;
        else
            return null;

    }

    public List<NameValuePair> GetParams() {

        ArrayList<NameValuePair> list = new ArrayList<NameValuePair>();

        if (event == null) {
            dlog.e("GetParams, evento == null");
            return list;
        }
        if (event.lat > 90 || event.lat < -90 || event.lon > 180 || event.lon < -180) {
            event.lat = 0;
            event.lon = 0;
        }

        try {
            if (!event.sent) {
                dlog.d("GetParams, sending evento");
                list.add(new BasicNameValuePair("event_id", event.event + ""));
                list.add(new BasicNameValuePair("label", event.label));
                list.add(new BasicNameValuePair("car_plate", App.CarPlate));
                list.add(new BasicNameValuePair("customer_id", event.id_customer + ""));
                list.add(new BasicNameValuePair("trip_id", event.id_trip + ""));
                list.add(new BasicNameValuePair("event_time", event.timestamp + ""));
                list.add(new BasicNameValuePair("intval", event.intval + ""));
                list.add(new BasicNameValuePair("txtval", event.txtval + ""));
                list.add(new BasicNameValuePair("lon", event.lon + ""));
                list.add(new BasicNameValuePair("lat", event.lat + ""));
                list.add(new BasicNameValuePair("km", event.km + ""));
                list.add(new BasicNameValuePair("battery", event.battery + ""));
                list.add(new BasicNameValuePair("imei", App.IMEI));
                list.add(new BasicNameValuePair("json_data", event.json_data));
            }

            for (NameValuePair pair : list) {
                dlog.i(pair.getName() + ":" + pair.getValue());
            }
        } catch (Exception e) {
            dlog.e("Getparams, exception", e);
        }

        return list;

    }

    public int DecodeJson(String responseBody) {

        try {

            if (responseBody == null || responseBody.isEmpty()) {
                event.sending_error = true;
                event.Update();
                dlog.w("No response from server, keeping info off-line");
                return MsgId();
            }

            dlog.i("DecodeJson: " + responseBody);
            JSONObject jobj = new JSONObject(responseBody);

            int result = jobj.getInt("result");
            String caption = jobj.getString("message");

            dlog.i("DecodeJson: result " + result);
            dlog.i("DecodeJson: caption " + (caption != null ? caption : "NULL"));

            if (result > 0) {
                event.sent = true;
            } else {
                event.sending_error = true;
            }
            event.Update();
        } catch (JSONException e) {

            event.sent = true;
            event.Update();
            dlog.e("DecodeJson, JSON exception", e);
        } catch (Exception e) {
            dlog.e("DecodeJson, exception", e);
        }

        return MsgId();
    }

    public eDirection getDirection() {
        return eDirection.DOWNLOAD;
    }

    public String EncodeJson() {

        return null;
    }
}

