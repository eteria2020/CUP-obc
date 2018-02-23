package eu.philcar.csg.OBC.data.datasources;

import java.util.List;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.data.datasources.api.SharengoPhpApi;
import eu.philcar.csg.OBC.data.datasources.base.BaseRetrofitDataSource;
import eu.philcar.csg.OBC.data.model.AreaResponse;
import eu.philcar.csg.OBC.data.model.CommandResponse;
import eu.philcar.csg.OBC.data.model.TripResponse;
import eu.philcar.csg.OBC.db.Trip;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.server.ServerCommand;
import eu.philcar.csg.OBC.service.DataManager;
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

    @Override
    public Observable<List<AreaResponse>> getArea(String plate, String md5) {
        return  mSharengoPhpApi.getArea(plate,md5)
                .compose(this.handleRetrofitRequest());
    }

    @Override
    public Observable<List<ServerCommand>> getCommands(String plate) {
        return  mSharengoPhpApi.getCommands(plate)
                .compose(this.handleRetrofitRequest());
    }

    @Override
    public Observable<TripResponse> openTrip(final Trip trip, final DataManager dataManager) {
        if(trip.id_parent==0) {
            return mSharengoPhpApi.openTrip(1, trip.plate, trip.id_customer, trip.begin_timestamp, trip.begin_km, trip.begin_battery, trip.begin_lon, trip.begin_lat, trip.warning, trip.int_cleanliness, trip.ext_cleanliness, App.MacAddress, App.IMEI, trip.n_pin)
                    .compose(this.handleRetrofitRequest())
                    .doOnError(e -> {
                        trip.recharge = 0; //server result
                        trip.offline = true;
                        dataManager.updateTripSetOffline(trip);
                    })
                    .doOnNext(t -> {
                        trip.recharge = t.getResult();
                        if (t.getResult() > 0) {

                            trip.remote_id = t.getResult();
                            trip.begin_sent = true;
                            dataManager.updateBeginSentDone(trip);


                        } else {
                            switch (t.getResult()) {

                                case -15:
                                    trip.warning = "OPEN TRIP";
                                    trip.begin_sent = true;
                                    break;

                                case -16:
                                    trip.warning = "FORBIDDEN";
                                    trip.begin_sent = true;
                                    break;

                                case -26:
                                case -27:
                                case -28:
                                case -29:
                                    trip.warning = "PREAUTH";
                                    trip.begin_sent = true;
                                    break;


                                default:
                                    trip.warning = "FAIL";
                                    trip.begin_sent = false;
                            }
                            if (t.getExtra() != null && !t.getExtra().isEmpty()) {
                                try {
                                    trip.remote_id = Integer.parseInt(t.getExtra());
                                } catch (Exception e) {
                                    dlog.e("Exception while extracting extra", e);
                                }
                            }
                            dataManager.updateBeginSentDone(trip);
                        }
                    });
        }else{
            return mSharengoPhpApi.openTrip(1, trip.plate, trip.id_customer, trip.begin_timestamp, trip.begin_km, trip.begin_battery, trip.begin_lon, trip.begin_lat, trip.warning, trip.int_cleanliness, trip.ext_cleanliness, App.MacAddress, App.IMEI, trip.n_pin, trip.id_parent)
                    .compose(this.handleRetrofitRequest())
                    .doOnError(e -> {
                        trip.recharge = 0; //server result
                        trip.offline = true;
                        dataManager.updateTripSetOffline(trip);
                    })
                    .doOnNext(t -> {
                        trip.recharge = t.getResult();
                        if (t.getResult() > 0) {

                            trip.remote_id = t.getResult();
                            trip.begin_sent = true;
                            dataManager.updateBeginSentDone(trip);


                        } else {
                            switch (t.getResult()) {

                                case -15:
                                    trip.warning = "OPEN TRIP";
                                    trip.begin_sent = true;
                                    break;

                                case -16:
                                    trip.warning = "FORBIDDEN";
                                    trip.begin_sent = true;
                                    break;

                                case -26:
                                case -27:
                                case -28:
                                case -29:
                                    trip.warning = "PREAUTH";
                                    trip.begin_sent = true;
                                    break;


                                default:
                                    trip.warning = "FAIL";
                                    trip.begin_sent = false;
                            }
                            if (t.getExtra() != null && !t.getExtra().isEmpty()) {
                                try {
                                    trip.remote_id = Integer.parseInt(t.getExtra());
                                } catch (Exception e) {
                                    dlog.e("Exception while extracting extra", e);
                                }
                            }
                            dataManager.updateBeginSentDone(trip);
                        }
                    });
        }
    }


    @Override
    public Observable<TripResponse> updateTrip(final Trip trip) {
        if(trip.id_parent==0) {
            return mSharengoPhpApi.openTrip(1, trip.plate, trip.id_customer, trip.begin_timestamp, trip.begin_km, trip.begin_battery, trip.begin_lon, trip.begin_lat, trip.warning, trip.int_cleanliness, trip.ext_cleanliness, App.MacAddress, App.IMEI, trip.n_pin)
                    .compose(this.handleRetrofitRequest());
        }else{
            return mSharengoPhpApi.openTrip(1, trip.plate, trip.id_customer, trip.begin_timestamp, trip.begin_km, trip.begin_battery, trip.begin_lon, trip.begin_lat, trip.warning, trip.int_cleanliness, trip.ext_cleanliness, App.MacAddress, App.IMEI, trip.n_pin, trip.id_parent)
                    .compose(this.handleRetrofitRequest());
        }
    }



    @Override
    public Observable<TripResponse> closeTrip(final Trip trip,final DataManager dataManager) {
        if(trip.id_parent!=0) {
            return mSharengoPhpApi.closeTrip(2,trip.remote_id, trip.plate, trip.id_customer, trip.end_timestamp, trip.end_km, trip.end_battery, trip.end_lon, trip.end_lat, trip.warning, trip.int_cleanliness, trip.ext_cleanliness, trip.park_seconds, trip.n_pin, trip.id_parent)
                    .compose(this.handleRetrofitRequest())
                    .doOnError(e ->{
                        trip.offline=true;
                        dataManager.updateTripSetOffline(trip);
                    })
                    .doOnNext(t -> {
                        trip.recharge = t.getResult();
                        if (t.getResult() > 0) {

                            trip.end_sent = true;
                            dataManager.updateEndSentDone(trip);



                        } else {
                            switch (t.getResult()) {

                                case -3:
                                    trip.warning="NO_MATCH";
                                    trip.begin_sent = false;
                                    trip.end_sent = false;
                                    dataManager.updateBeginSentDone(trip);
                                    break;


                                default:
                                    trip.warning="FAIL";
                                    trip.end_sent = false;
                            }

                            dataManager.updateEndSentDone(trip);
                        }
                    });

        }else {
            return mSharengoPhpApi.closeTrip(2,trip.remote_id, trip.plate, trip.id_customer, trip.end_timestamp, trip.end_km, trip.end_battery, trip.end_lon, trip.end_lat, trip.warning, trip.int_cleanliness, trip.ext_cleanliness, trip.park_seconds, trip.n_pin)
                    .compose(this.handleRetrofitRequest())
                    .doOnError(e ->{
                        trip.offline=true;
                        dataManager.updateTripSetOffline(trip);
                    })
                    .doOnNext(t -> {
                        trip.recharge = t.getResult();
                        if (t.getResult() > 0) {

                            trip.end_sent = true;
                            dataManager.updateEndSentDone(trip);



                        } else {
                            switch (t.getResult()) {

                                case -3:
                                    trip.warning="NO_MATCH";
                                    trip.begin_sent = false;
                                    trip.end_sent = false;
                                    dataManager.updateBeginSentDone(trip);
                                    break;


                                default:
                                    trip.warning="FAIL";
                                    trip.end_sent = false;
                            }

                            dataManager.updateEndSentDone(trip);
                        }
                    });
        }
    }
}
