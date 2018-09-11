package eu.philcar.csg.OBC.data.datasources.api;


import eu.philcar.csg.OBC.data.model.BeaconResponse;
import io.reactivex.Observable;
import retrofit2.Response;
import retrofit2.adapter.rxjava2.Result;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

/**
 * Created by Fulvio on 01/03/2018.
 */

public interface SharengoBeaconApi {

    //http://core.sharengo.it:7600/notifies?

    @Headers("Connection: close")
    @GET("notifies")
    Observable<Result<BeaconResponse>> sendBeacon(@Query("plate") String plate, @Query(value = "beaconText", encoded=true) String beacon);
}
