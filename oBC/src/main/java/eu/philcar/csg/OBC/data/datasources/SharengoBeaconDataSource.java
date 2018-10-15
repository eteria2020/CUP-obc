package eu.philcar.csg.OBC.data.datasources;

import eu.philcar.csg.OBC.data.model.BeaconResponse;
import io.reactivex.Observable;

/**
 * Created by Fulvio on 01/03/2018.
 */

public interface SharengoBeaconDataSource {

	Observable<BeaconResponse> sendBeacon(String plate, String beacon);
}
