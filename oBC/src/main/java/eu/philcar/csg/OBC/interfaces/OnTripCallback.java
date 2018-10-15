package eu.philcar.csg.OBC.interfaces;

import eu.philcar.csg.OBC.service.TripInfo;

/**
 * Created by Fulvio on 24/10/2017.
 */
public interface OnTripCallback {

	void onTripResult(TripInfo response);
}
