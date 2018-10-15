package eu.philcar.csg.OBC.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Fulvio on 07/05/2018.
 */

public class SharengoResponse<T> {
	public int status;
	public String reason;
	public T data;
	@SerializedName("time")
	public long timestamp;
}
