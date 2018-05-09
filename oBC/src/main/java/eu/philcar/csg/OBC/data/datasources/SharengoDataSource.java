package eu.philcar.csg.OBC.data.datasources;

import java.util.List;

import eu.philcar.csg.OBC.data.model.Area;
import eu.philcar.csg.OBC.data.model.Config;
import eu.philcar.csg.OBC.data.model.ModelResponse;
import eu.philcar.csg.OBC.data.model.ReservationResponse;
import eu.philcar.csg.OBC.db.BusinessEmployee;
import eu.philcar.csg.OBC.db.Customer;
import eu.philcar.csg.OBC.service.Reservation;
import io.reactivex.Observable;

/**
 * Created by Fulvio on 15/02/2018.
 */

public interface SharengoDataSource {

    Observable<List<Customer>> getCustomer(long lastupdate);

    Observable<List<BusinessEmployee>> getBusinessEmployees();

    Observable<Config> getConfig(String car_plate);


    Observable<List<Reservation>> getReservation(String car_plate);

    Observable<Reservation> consumeReservation(int reservation_id);

    Observable<List<Area>> getArea(String md5);

    Observable<List<ModelResponse>> getModel(String plate);
}
