package eu.philcar.csg.OBC.data.datasources;

import java.util.List;

import eu.philcar.csg.OBC.data.datasources.api.SharengoApi;
import eu.philcar.csg.OBC.data.datasources.base.BaseRetrofitDataSource;
import eu.philcar.csg.OBC.data.model.Area;
import eu.philcar.csg.OBC.data.model.Config;
import eu.philcar.csg.OBC.data.model.ModelResponse;
import eu.philcar.csg.OBC.data.model.ReservationResponse;
import eu.philcar.csg.OBC.data.model.SharengoResponse;
import eu.philcar.csg.OBC.db.BusinessEmployee;
import eu.philcar.csg.OBC.db.Customer;
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
                .compose(this.handleRetrofitRequest())
                .doOnSubscribe(this::addDisposable)
                .doOnError(this::handleErorResponse);
    }

    @Override
    public Observable<List<BusinessEmployee>> getBusinessEmployees() {
        return  mSharengoApi.getBusinessEmployees()
                .compose(this.handleRetrofitRequest())
                .doOnSubscribe(this::addDisposable)
                .doOnError(this::handleErorResponse);
    }

    @Override
    public Observable<Config> getConfig(String car_plate) {
        return  mSharengoApi.getConfigs(car_plate)
                .compose(this.handleRetrofitRequest())
                .concatMap(this::extractResponse)
                .doOnSubscribe(this::addDisposable)
                .doOnError(this::handleErorResponse);
    }


    @Override
    public Observable<List<Reservation>> getReservation(String car_plate) {
        return  mSharengoApi.getReservation(car_plate)
                .compose(this.handleRetrofitRequest())
                .concatMap(this::extractResponse)
                .doOnSubscribe(this::addDisposable)
                .doOnError(this::handleErorResponse);
    }

    @Override
    public Observable<Reservation> consumeReservation(int reservation_id) {
        return mSharengoApi.consumeReservation(reservation_id)
                .compose(this.handleRetrofitRequest())
                .concatMap(this::extractResponse)
                .doOnSubscribe(this::addDisposable)
                .doOnError(this::handleErorResponse);
    }

    @Override
    public Observable<List<Area>> getArea(String md5) {
        return  mSharengoApi.getArea(md5)
                .compose(this.handleRetrofitRequest())
                .concatMap(this::extractResponse)
                .doOnSubscribe(this::addDisposable)
                .doOnError(this::handleErorResponse)
                .doOnComplete(this::hanldeCompletation);
    }

        @Override
    public Observable<List<ModelResponse>> getModel(String plate) {
        return  mSharengoApi.getModel(plate)
                .compose(this.handleRetrofitRequest())
                .doOnSubscribe(this::addDisposable)
                .doOnError(this::handleErorResponse);
    }
}
