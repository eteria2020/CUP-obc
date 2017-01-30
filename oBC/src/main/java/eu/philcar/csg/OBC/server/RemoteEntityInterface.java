package eu.philcar.csg.OBC.server;

import java.util.List;

import org.apache.http.NameValuePair;

public interface RemoteEntityInterface {

	public enum eDirection  {UPLOAD, DOWNLOAD};
	
	public int  MsgId();	
	public String GetRemoteUrl();	
	
	public List<NameValuePair> GetParams();
	
	public int DecodeJson(String response);
	public String  EncodeJson();
	
	public eDirection getDirection();
	

	
}
