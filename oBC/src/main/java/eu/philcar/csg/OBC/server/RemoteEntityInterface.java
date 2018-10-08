package eu.philcar.csg.OBC.server;

import org.apache.http.NameValuePair;

import java.util.List;

public interface RemoteEntityInterface {

    enum eDirection {UPLOAD, DOWNLOAD}

    int MsgId();

    String GetRemoteUrl();

    List<NameValuePair> GetParams();

    int DecodeJson(String response);

    String EncodeJson();

    eDirection getDirection();

}
