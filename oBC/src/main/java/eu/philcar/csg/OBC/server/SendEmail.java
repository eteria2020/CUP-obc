package eu.philcar.csg.OBC.server;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.helpers.DLog;

public class SendEmail implements RemoteEntityInterface {

    private String email;
    private Handler handler;

    @Override
    public int MsgId() {
        return Connectors.MSG_SEND_EMAIL;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    @Override
    public String GetRemoteUrl() {
        return App.URL_SENDEMAIL;
    }

    @Override
    public List<NameValuePair> GetParams() {
        ArrayList<NameValuePair> list = null;

        list = new ArrayList<NameValuePair>();
        list.add(new BasicNameValuePair("PLATE", App.CarPlate));
        list.add(new BasicNameValuePair("EMAIL", this.email));

        return list;

    }

    @Override
    public int DecodeJson(String response) {
        DLog.I("SendEmail: " + response);
        Message m = Message.obtain(); //get null message
        Bundle b = new Bundle();
        b.putString("response", response);
        m.setData(b);
        this.handler.sendMessage(m);

        return 0;
    }

    @Override
    public String EncodeJson() {
        return null;
    }

    @Override
    public eDirection getDirection() {
        return RemoteEntityInterface.eDirection.DOWNLOAD;
    }

    public void setEmail(String em) {
        email = em;
    }

}
