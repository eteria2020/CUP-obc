package eu.philcar.csg.OBC.devices;

public class ObcString {
	public enum StringType {
		RESPONSE,
		INFO
	}

	public String rawString;
	public String tag;
	public String args;
	public StringType type;

}
