package eu.philcar.csg.OBC.data.datasources;

import java.util.List;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.data.datasources.api.SharengoPhpApi;
import eu.philcar.csg.OBC.data.datasources.base.BaseResponse;
import eu.philcar.csg.OBC.data.datasources.base.BaseRetrofitDataSource;
import eu.philcar.csg.OBC.data.model.AreaResponse;
import eu.philcar.csg.OBC.data.model.EventResponse;
import eu.philcar.csg.OBC.data.model.TripResponse;
import eu.philcar.csg.OBC.db.DbRecord;
import eu.philcar.csg.OBC.db.Event;
import eu.philcar.csg.OBC.db.Poi;
import eu.philcar.csg.OBC.db.Trip;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.server.ServerCommand;
import eu.philcar.csg.OBC.service.DataManager;
import eu.philcar.csg.OBC.service.Reservation;
import io.reactivex.Observable;

/**
 * Created by Fulvio on 16/02/2018.
 */

public class SharengoPhpRetrofitDataSource extends BaseRetrofitDataSource implements SharengoPhpDataSource {

    private final SharengoPhpApi mSharengoPhpApi;
    private DLog dlog = new DLog(this.getClass());

    public SharengoPhpRetrofitDataSource(SharengoPhpApi mSharengoPhpApi) {
        this.mSharengoPhpApi = mSharengoPhpApi;
    }

    /***
     *
     * @param plate
     * @param md5
     * @return all the area downloaded to be interpretated
     */
    @Override
    public Observable<List<AreaResponse>> getArea(String plate, String md5) {
        return  mSharengoPhpApi.getArea(plate,md5)
                .compose(this.handleRetrofitRequest())
                .doOnError(this::handleErorResponse);
    }

    /**
     *
     * @param plate
     * @return a list of command to be excecuted
     */
    @Override
    public Observable<List<ServerCommand>> getCommands(String plate) {
        return  mSharengoPhpApi.getCommands(plate)
                .compose(this.handleRetrofitRequest())
                .doOnError(this::handleErorResponse);
    }

    /**
     * open trip and handle all the database operation to save the result of the opening
     * @param trip
     * @param dataManager
     * @return
     */
    @Override
    public Observable<TripResponse> openTrip(final Trip trip, final DataManager dataManager) {
        if(trip.id_parent==0) {
            return mSharengoPhpApi.openTrip(1, trip.plate, trip.id_customer, trip.begin_timestamp, trip.begin_km, trip.begin_battery, trip.begin_lon, trip.begin_lat, trip.warning, trip.int_cleanliness, trip.ext_cleanliness, App.MacAddress, App.IMEI, trip.n_pin)
                    .compose(this.handleRetrofitRequest())
                    .doOnError(this::handleErorResponse)
                    .doOnError(e -> {
                        trip.recharge = 0; //server result
                        trip.offline = true;
                        dataManager.updateTripSetOffline(trip);
                    })
                    .concatMap(t -> {
                        this.handleResponsePersistance(trip, t, dataManager, Trip.OPEN_TRIP);
                        return Observable.just(t);
                    });
        }else{
            return mSharengoPhpApi.openTrip(1, trip.plate, trip.id_customer, trip.begin_timestamp, trip.begin_km, trip.begin_battery, trip.begin_lon, trip.begin_lat, trip.warning, trip.int_cleanliness, trip.ext_cleanliness, App.MacAddress, App.IMEI, trip.n_pin, trip.id_parent)
                    .compose(this.handleRetrofitRequest())
                    .doOnError(this::handleErorResponse)
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
                .concatMap(tripResponse -> Observable.just(trip))
                .doOnError(this::handleErorResponse);
    }

    /**
     * update Trip for Pin Result
     * @param trip
     * @return
     */
    @Override
    public Observable<TripResponse> updateTrip(final Trip trip) {
        if(trip.id_parent==0) {
            return mSharengoPhpApi.openTrip(1, trip.plate, trip.id_customer, trip.begin_timestamp, trip.begin_km, trip.begin_battery, trip.begin_lon, trip.begin_lat, trip.warning, trip.int_cleanliness, trip.ext_cleanliness, App.MacAddress, App.IMEI, trip.n_pin)
                    .compose(this.handleRetrofitRequest())
                    .doOnError(this::handleErorResponse);
        }else{
            return mSharengoPhpApi.openTrip(1, trip.plate, trip.id_customer, trip.begin_timestamp, trip.begin_km, trip.begin_battery, trip.begin_lon, trip.begin_lat, trip.warning, trip.int_cleanliness, trip.ext_cleanliness, App.MacAddress, App.IMEI, trip.n_pin, trip.id_parent)
                    .compose(this.handleRetrofitRequest())
                    .doOnError(this::handleErorResponse);
        }
    }



    @Override
    public Observable<TripResponse> closeTrip(final Trip trip,final DataManager dataManager) {
        if(trip.id_parent!=0) {
            return mSharengoPhpApi.closeTrip(2,trip.remote_id, trip.plate, trip.id_customer, trip.end_timestamp, trip.end_km, trip.end_battery, trip.end_lon, trip.end_lat, trip.warning, trip.int_cleanliness, trip.ext_cleanliness, trip.park_seconds, trip.n_pin, trip.id_parent)
                    .compose(this.handleRetrofitRequest())
                    .doOnError(this::handleErorResponse)
                    .doOnError(e ->{
                        trip.offline=true;
                        dataManager.updateTripSetOffline(trip);
                    })
                    .concatMap(tripResponse -> {
                        trip.handleResponse(tripResponse,dataManager,Trip.CLOSE_TRIP);
                        return Observable.just(tripResponse);
                    });

        }else {
            return mSharengoPhpApi.closeTrip(2,trip.remote_id, trip.plate, trip.id_customer, trip.end_timestamp, trip.end_km, trip.end_battery, trip.end_lon, trip.end_lat, trip.warning, trip.int_cleanliness, trip.ext_cleanliness, trip.park_seconds, trip.n_pin)
                    .compose(this.handleRetrofitRequest())
                    .doOnError(this::handleErorResponse)
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

    @Override
    public Observable<EventResponse> sendEvent(Event event, DataManager dataManager) {
        return mSharengoPhpApi.sendEvent(event.event,event.label, App.CarPlate, event.id_customer, event.id_trip, event.timestamp, event.intval, event.txtval, event.lon, event.lat, event.km, event.battery, App.IMEI, event.json_data)
                .compose(this.handleRetrofitRequest())
                .doOnError(this::handleErorResponse)
                .concatMap(n ->{this.handleResponsePersistance(event,n,dataManager,0);
                            return Observable.just(n);})
                .doOnError(e ->{
                    event.sending_error=true;
                    event.sent=false;
                    dataManager.updateEventSendingResponse(event);
                });

    }

    @Override
    public Observable<List<Reservation>> getReservation(String plate) {
        return  mSharengoPhpApi.getReservation(plate)
                .compose(this.handleRetrofitRequest())
                .doOnError(this::handleErorResponse);
    }

    @Override
    public Observable<Void> consumeReservation(int reservation_id) {
        return  mSharengoPhpApi.consumeReservation(reservation_id)
                .compose(this.handleRetrofitRequest())
                .doOnError(this::handleErorResponse);
    }

    @Override
    public Observable<List<Poi>> getPois(long lastupdate) {
        return mSharengoPhpApi.getPois(lastupdate)
                .compose(this.handleRetrofitRequest())
                .doOnError(this::handleErorResponse);
    }
}
