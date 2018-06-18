package eu.philcar.csg.OBC.data.datasources;

import java.util.List;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.data.datasources.api.SharengoApi;
import eu.philcar.csg.OBC.data.datasources.base.BaseRetrofitDataSource;
import eu.philcar.csg.OBC.data.model.Area;
import eu.philcar.csg.OBC.data.model.Config;
import eu.philcar.csg.OBC.data.model.EventResponse;
import eu.philcar.csg.OBC.data.model.ModelResponse;
import eu.philcar.csg.OBC.data.model.TripResponse;
import eu.philcar.csg.OBC.db.BusinessEmployee;
import eu.philcar.csg.OBC.db.Customer;
import eu.philcar.csg.OBC.db.Event;
import eu.philcar.csg.OBC.db.Events;
import eu.philcar.csg.OBC.db.Poi;
import eu.philcar.csg.OBC.db.Trip;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.server.ServerCommand;
import eu.philcar.csg.OBC.service.DataManager;
import eu.philcar.csg.OBC.service.Reservation;
import io.reactivex.Observable;

/**
 * Created by Fulvio on 15/02/2018.
 */

public class SharengoRetrofitDataSource extends BaseRetrofitDataSource implements SharengoDataSource {

    private final SharengoApi mSharengoApi;


    public SharengoRetrofitDataSource(SharengoApi mSharengoApi) {
        this.mSharengoApi = mSharengoApi;
    }

    @Override
    public Observable<List<Customer>> getCustomer(long lastupdate) {
        return  mSharengoApi.getCustomer(lastupdate)
                .compose(this.handleRetrofitRequest());
    }

    @Override
    public Observable<List<BusinessEmployee>> getBusinessEmployees() {
        return  mSharengoApi.getBusinessEmployees()
                .compose(this.handleRetrofitRequest());
    }

    @Override
    public Observable<Config> getConfig(String car_plate) {
        return  mSharengoApi.getConfigs(car_plate)
                .compose(this.handleRetrofitRequest());
    }


    @Override
    public Observable<List<Reservation>> getReservation(String car_plate) {
        return  mSharengoApi.getReservation(car_plate)
                .compose(this.handleSharengoRetrofitRequest());
    }

    @Override
    public Observable<Reservation> consumeReservation(int reservation_id) {
        return mSharengoApi.consumeReservation(reservation_id)
                .compose(this.handleSharengoRetrofitRequest());
    }

    @Override
    public Observable<List<Area>> getArea(String md5) {
        return  mSharengoApi.getArea(md5)
                .compose(this.handleSharengoRetrofitRequest());
    }

    @Override
    public Observable<List<ServerCommand>> getCommands(String plate) {
        return  mSharengoApi.getCommands(plate)
                .compose(this.handleSharengoRetrofitRequest());
    }

        @Override
    public Observable<List<ModelResponse>> getModel(String plate) {
        return  mSharengoApi.getModel(plate)
                .compose(this.handleRetrofitRequest());
    }

    @Override
    public Observable<List<Poi>> getPois(long lastupdate) {
        return mSharengoApi.getPois(String.valueOf(lastupdate))
                .compose(this.handleSharengoRetrofitRequest());
    }

    @Override
    public Observable<EventResponse> sendEvent(Event event, DataManager dataManager) {
        DLog.D("HttpLogger: sending Event " + event.toString());
        return mSharengoApi.sendEvent(event.event,event.label, App.CarPlate, event.id_customer, event.id_trip, event.timestamp, event.intval, event.txtval, event.lon, event.lat, event.km, event.battery, App.IMEI, event.json_data)
                .compose(this.handleSharengoRetrofitRequest())
                .concatMap(n ->{this.handleResponsePersistance(event,n,dataManager,0);
                    return Observable.just(n);})
                .doOnError(e ->{
                    event.sending_error=true;
                    event.sent=false;
                    if(event.id == Events.EVT_SOS){
                        event.intval = 3;
                    }
                    dataManager.updateEventSendingResponse(event);
                });

    }

    /**
     * open trip and handle all the database operation to save the result of the opening
     * @param trip
     * @param dataManager
     * @return
     */
    @Override
    public Observable<TripResponse> openTrip(final Trip trip, final DataManager dataManager) {

        return mSharengoApi.openTrip(1, trip.plate, trip.id_customer, trip.begin_timestamp, trip.begin_km, trip.begin_battery, trip.begin_lon, trip.begin_lat,
                trip.warning==null?"":trip.warning, trip.int_cleanliness, trip.ext_cleanliness, App.MacAddress==null?"":App.MacAddress, App.IMEI, trip.n_pin, trip.id_parent==0?null:String.valueOf(trip.id_parent))
                .compose(this.handleSharengoRetrofitRequest())
                .doOnError(e -> {
                    trip.recharge = 0; //server result
                    trip.offline = true;
                    dataManager.updateTripSetOffline(trip);
                })
                .concatMap(t -> {
                    this.handleResponsePersistance(trip, t, dataManager, Trip.OPEN_TRIP);
                    return Observable.just(t);
                });
    }

    /**
     * open trip wothout blocking the chain, it return the Trip not the TripResponse
     * @param trip
     * @param dataManager
     * @return
     */
    @Override
    public Observable<Trip> openTripPassive(final Trip trip, final DataManager dataManager) {
        return openTrip(trip,dataManager)
                .concatMap(tripResponse -> Observable.just(trip));
    }


    /**
     * open trip wothout blocking the chain, it return the Trip not the TripResponse
     * @param trip
     * @param dataManager
     * @return
     */
    @Override
    public Observable<Trip> closeTripPassive(final Trip trip, final DataManager dataManager) {
        return closeTrip(trip,dataManager)
                .concatMap(tripResponse -> Observable.just(trip));
    }

    /**
     * update Trip for Pin Result
     * @param trip
     * @return
     */
    @Override
    public Observable<TripResponse> updateTrip(final Trip trip) {

        return mSharengoApi.openTrip(1, trip.plate, trip.id_customer, trip.begin_timestamp, trip.begin_km, trip.begin_battery, trip.begin_lon, trip.begin_lat,
                trip.warning==null?"":trip.warning, trip.int_cleanliness, trip.ext_cleanliness, App.MacAddress, App.IMEI, trip.n_pin,trip.id_parent==0?null:String.valueOf(trip.id_parent))
                .compose(this.handleSharengoRetrofitRequest());

    }

    @Override
    public Observable<TripResponse> closeTrip(final Trip trip,final DataManager dataManager) {
        return mSharengoApi.closeTrip(2,trip.remote_id, trip.plate, trip.id_customer, trip.end_timestamp, trip.end_km, trip.end_battery, trip.end_lon, trip.end_lat,
                trip.warning==null?"":trip.warning, trip.int_cleanliness, trip.ext_cleanliness, trip.park_seconds, trip.n_pin, trip.id_parent==0?null:String.valueOf(trip.id_parent))
                .compose(this.handleSharengoRetrofitRequest())
                .doOnError(e ->{
                    trip.offline=true;
                    dataManager.updateTripSetOffline(trip);
                })
                .concatMap(tripResponse -> {
                    trip.handleResponse(tripResponse,dataManager,Trip.CLOSE_TRIP);
                    return Observable.just(tripResponse);
                });
    }
}
