package eu.philcar.csg.OBC.server;

import java.util.List;

import org.apache.http.NameValuePair;

public interface RemoteEntityInterface {

	enum eDirection  {UPLOAD, DOWNLOAD}

    int  MsgId();
	String GetRemoteUrl();
	
	List<NameValuePair> GetParams();
	
	int DecodeJson(String response);
	String  EncodeJson();
	
	eDirection getDirection();
	

	
}
