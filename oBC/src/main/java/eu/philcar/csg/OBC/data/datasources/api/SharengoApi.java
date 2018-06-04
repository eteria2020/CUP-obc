package eu.philcar.csg.OBC.data.datasources.api;

import java.util.List;

import eu.philcar.csg.OBC.data.model.Area;
import eu.philcar.csg.OBC.data.model.Config;
import eu.philcar.csg.OBC.data.model.EventResponse;
import eu.philcar.csg.OBC.data.model.ModelResponse;
import eu.philcar.csg.OBC.data.model.ReservationResponse;
import eu.philcar.csg.OBC.data.model.SharengoResponse;
import eu.philcar.csg.OBC.data.model.TripResponse;
import eu.philcar.csg.OBC.db.BusinessEmployee;
import eu.philcar.csg.OBC.db.Customer;
import eu.philcar.csg.OBC.db.Poi;
import eu.philcar.csg.OBC.server.ServerCommand;
import eu.philcar.csg.OBC.service.Reservation;
import io.reactivex.Observable;
import retrofit2.adapter.rxjava2.Result;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
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


    @GET("v2/area")
    Observable<Result<SharengoResponse<List<Area>>>> getArea(@Query("md5") String md5);

    @GET("v2/commands")
    Observable<Result<SharengoResponse<List<ServerCommand>>>> getCommands(@Query("car_plate") String car_plate);

    @GET("v2/reservation")
    Observable<Result<SharengoResponse<List<Reservation>>>> getReservation(@Query("car_plate") String car_plate);

    @GET("v2/reservation")
    Observable<Result<SharengoResponse<Reservation>>> consumeReservation(@Query("consumed") int reservation_id);

    @GET("/v2pois")
    Observable<Result<SharengoResponse<List<Poi>>>> getPois(@Query("lastupdate") String lastupdate);

    @GET("configs")
    Observable<Result<Config>> getConfigs(@Query("car_plate") String car_plate);

    @FormUrlEncoded
    @POST("v2/events")
    Observable<Result<SharengoResponse<EventResponse>>> sendEvent(@Field(value = "event_id", encoded=true) int event_id, @Field("label") String label, @Field("car_plate") String car_plate, @Field("customer_id") int customer_id,
                                                                  @Field("trip_id") int trip_id, @Field("event_time") long event_time, @Field("intval") int intval, @Field("txtval") String txtval,
                                                                  @Field("lon") double lon, @Field("lat") double lat, @Field("km") int km, @Field("battery") int battery, @Field("imei") String imei,
                                                                  @Field("json_data") String json_data);

    @FormUrlEncoded
    @POST("v2/trips")
    Observable<Result<Config>> postTrips(@Query("car_plate") String car_plate);


    @GET("carmodel")
    Observable<Result<List<ModelResponse>>> getModel(@Query("plate") String car_plate);

    @FormUrlEncoded
    @POST("v2/trips")
    Observable<Result<SharengoResponse<TripResponse>>> openTrip(@Field("cmd") int cmd_type, @Field("id_veicolo") String plate, @Field("id_cliente") int customerId, @Field("ora") long beginTimestamp, @Field("km") int km, @Field("carburante") int soc,
                                              @Field("lon") double beginLon, @Field("lat") double beginLat, @Field("warning") String warning, @Field("pulizia_int") int intCleanliness, @Field("pulizia_ext") int extCleanliness,
                                              @Field("mac") String macAddress, @Field("imei") String imei, @Field("n_pin") int nPin, @Field("id_parent") String id_parent);

    @FormUrlEncoded
    @POST("v2/trips")
    Observable<Result<SharengoResponse<TripResponse>>> closeTrip(@Field("cmd") int cmd_type,@Field("id") int remote_id, @Field("id_veicolo") String plate, @Field("id_cliente") int customerId, @Field("ora") long endTimestamp, @Field("km") int km, @Field("carburante") int soc,
                                               @Field("lon") double endLon, @Field("lat") double endLat, @Field("warning") String warning, @Field("pulizia_int") int intCleanliness, @Field("pulizia_ext") int extCleanliness,
                                               @Field("park_seconds") int park_seconds, @Field("n_pin") int nPin, @Field("id_parent") String id_parent);


}
