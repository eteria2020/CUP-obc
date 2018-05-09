package eu.philcar.csg.OBC.data.datasources.api;

import java.util.List;

import eu.philcar.csg.OBC.data.model.Area;
import eu.philcar.csg.OBC.data.model.Config;
import eu.philcar.csg.OBC.data.model.EventResponse;
import eu.philcar.csg.OBC.data.model.ModelResponse;
import eu.philcar.csg.OBC.data.model.ReservationResponse;
import eu.philcar.csg.OBC.data.model.SharengoResponse;
import eu.philcar.csg.OBC.db.BusinessEmployee;
import eu.philcar.csg.OBC.db.Customer;
import eu.philcar.csg.OBC.db.Poi;
import eu.philcar.csg.OBC.service.Reservation;
import io.reactivex.Observable;
import retrofit2.adapter.rxjava2.Result;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Created by Fulvio on 15/02/2018.
 */

public interface SharengoApi {

    @GET("whitelist2")
    Observable<Result<List<Customer>>> getCustomer(@Query("lastupdate") long lastupdate);

    @GET("business-employees")
    Observable<Result<List<BusinessEmployee>>> getBusinessEmployees();


    @GET("area")
    Observable<Result<SharengoResponse<List<Area>>>> getArea(@Query("md5") String car_plate);

    @GET("commands")
    Observable<Result<Config>> getCommands(@Query("car_plate") String car_plate);

    @GET("reservation")
    Observable<Result<SharengoResponse<List<Reservation>>>> getReservation(@Query("car_plate") String car_plate);

    @GET("reservation")
    Observable<Result<SharengoResponse<Reservation>>> consumeReservation(@Query("consumed") int reservation_id);

    @GET("pois")
    Observable<SharengoResponse<List<Poi>>> getPois(@Query("lastupdate") String lastupdate);

    @GET("configs")
    Observable<Result<SharengoResponse<Config>>> getConfigs(@Query("car_plate") String car_plate);

    @POST("events")
    Observable<Result<SharengoResponse<EventResponse>>> sendEvent(@Query("event_id") int event_id, @Query("label") String label, @Query("car_plate") String car_plate, @Query("customer_id") int customer_id,
                                                                  @Query("trip_id") int trip_id, @Query("event_time") long event_time, @Query("intval") int intval, @Query("txtval") String txtval,
                                                                  @Query("lon") double lon, @Query("lat") double lat, @Query("km") int km, @Query("battery") int battery, @Query("imei") String imei,
                                                                  @Query("json_data") String json_data);

    @POST("trips")
    Observable<Result<Config>> postTrips(@Query("car_plate") String car_plate);


    @GET("carmodel")
    Observable<Result<List<ModelResponse>>> getModel(@Query("plate") String car_plate);

}
