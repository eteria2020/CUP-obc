package eu.philcar.csg.OBC.data.datasources;

import java.util.List;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.data.datasources.api.SharengoPhpApi;
import eu.philcar.csg.OBC.data.datasources.base.BaseRetrofitDataSource;
import eu.philcar.csg.OBC.data.model.AreaResponse;
import eu.philcar.csg.OBC.data.model.CommandResponse;
import eu.philcar.csg.OBC.data.model.TripResponse;
import eu.philcar.csg.OBC.db.Trip;
import eu.philcar.csg.OBC.server.ServerCommand;
import io.reactivex.Observable;

/**
 * Created by Fulvio on 16/02/2018.
 */

public class SharengoPhpRetrofitDataSource extends BaseRetrofitDataSource implements SharengoPhpDataSource {

    private final SharengoPhpApi mSharengoPhpApi;

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
    public Observable<TripResponse> openTrip(Trip trip) {
        return  mSharengoPhpApi.openTrip(1, trip.plate, trip.id_customer, trip.begin_timestamp, trip.begin_km, trip.begin_battery, trip.begin_lon, trip.begin_lat, trip.warning, trip.int_cleanliness, trip.ext_cleanliness, App.MacAddress, App.IMEI, trip.n_pin)
                .compose(this.handleRetrofitRequest());
    }
}
