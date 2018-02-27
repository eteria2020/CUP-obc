package eu.philcar.csg.OBC.data.datasources.api;

import java.util.List;

import eu.philcar.csg.OBC.data.model.AreaResponse;
import eu.philcar.csg.OBC.data.model.CommandResponse;
import eu.philcar.csg.OBC.data.model.EventResponse;
import eu.philcar.csg.OBC.data.model.TripResponse;
import eu.philcar.csg.OBC.db.Trip;
import eu.philcar.csg.OBC.server.ServerCommand;
import io.reactivex.Observable;
import retrofit2.adapter.rxjava2.Result;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by Fulvio on 16/02/2018.
 */

public interface SharengoPhpApi {

    @GET("zone/json.php")
    Observable<Result<List<AreaResponse>>> getArea(@Query("targa") String plate, @Query("md5") String md5);

    @GET("get_commands.php")
    Observable<Result<List<ServerCommand>>> getCommands(@Query("car_plate") String car_plate);


    @GET("pushcorsa-convenzioni.php")
    Observable<Result<TripResponse>> openTrip(@Query("cmd") int cmd_type, @Query("id_veicolo") String plate, @Query("id_cliente") int customerId, @Query("ora") long beginTimestamp, @Query("km") int km, @Query("carburante") int soc,
                                              @Query("lon") double beginLon, @Query("lat") double beginLat, @Query("warning") String warning, @Query("pulizia_int") int intCleanliness, @Query("pulizia_ext") int extCleanliness,
                                              @Query("mac") String macAddress, @Query("imei") String imei, @Query("n_pin") int nPin, @Query("id_parent") int id_parent);
    @GET("pushcorsa-convenzioni.php")
    Observable<Result<TripResponse>> openTrip(@Query("cmd") int cmd_type, @Query("id_veicolo") String plate, @Query("id_cliente") int customerId, @Query("ora") long beginTimestamp, @Query("km") int km, @Query("carburante") int soc,
                                             @Query("lon") double beginLon, @Query("lat") double beginLat, @Query("warning") String warning, @Query("pulizia_int") int intCleanliness, @Query("pulizia_ext") int extCleanliness,
                                             @Query("mac") String macAddress, @Query("imei") String imei, @Query("n_pin") int nPin);

    @GET("pushcorsa-convenzioni.php")
    Observable<Result<TripResponse>> closeTrip(@Query("cmd") int cmd_type,@Query("id") int remote_id, @Query("id_veicolo") String plate, @Query("id_cliente") int customerId, @Query("ora") long endTimestamp, @Query("km") int km, @Query("carburante") int soc,
                                              @Query("lon") double endLon, @Query("lat") double endLat, @Query("warning") String warning, @Query("pulizia_int") int intCleanliness, @Query("pulizia_ext") int extCleanliness,
                                              @Query("park_seconds") int park_seconds, @Query("n_pin") int nPin);


    @GET("pushcorsa-convenzioni.php")
    Observable<Result<TripResponse>> closeTrip(@Query("cmd") int cmd_type,@Query("id") int remote_id, @Query("id_veicolo") String plate, @Query("id_cliente") int customerId, @Query("ora") long endTimestamp, @Query("km") int km, @Query("carburante") int soc,
                                               @Query("lon") double endLon, @Query("lat") double endLat, @Query("warning") String warning, @Query("pulizia_int") int intCleanliness, @Query("pulizia_ext") int extCleanliness,
                                               @Query("park_seconds") int park_seconds, @Query("n_pin") int nPin, @Query("id_parent") int id_parent);

    @GET("pushevent.php")
    Observable<Result<EventResponse>> sendEvent(@Query("event_id") int event_id, @Query("label") String label, @Query("car_plate") String car_plate, @Query("customer_id") int customer_id,
                                                @Query("trip_id") int trip_id, @Query("event_time") long event_time, @Query("intval") int intval, @Query("txtval") String txtval,
                                                @Query("lon") double lon, @Query("lat") double lat, @Query("km") int km, @Query("battery") int battery, @Query("imei") String imei,
                                                @Query("json_data") String json_data);


}
