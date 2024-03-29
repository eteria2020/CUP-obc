package eu.philcar.csg.OBC.data.datasources.base;

import com.google.gson.Gson;

import eu.philcar.csg.OBC.data.model.ServerResponse;

/**
 * Created by Fulvio on 16/02/2018.
 */

public abstract class BaseResponse implements ServerResponse {

	/**
	 * Use String type property for optional value, they will be discarded if null when the Json get built
	 *
	 * @return the Json built from the Object property
	 */
	public String getJson() {
		Gson gson = new Gson();
		return gson.toJson(this);
	}
}
