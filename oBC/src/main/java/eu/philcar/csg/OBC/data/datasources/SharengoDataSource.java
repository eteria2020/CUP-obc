package eu.philcar.csg.OBC.data.datasources;

import java.util.List;

import eu.philcar.csg.OBC.data.model.ConfigResponse;
import eu.philcar.csg.OBC.data.model.ModelResponse;
import eu.philcar.csg.OBC.db.BusinessEmployee;
import eu.philcar.csg.OBC.db.Customer;
import io.reactivex.Observable;

/**
 * Created by Fulvio on 15/02/2018.
 */

public interface SharengoDataSource {

    Observable<List<Customer>> getCustomer(long lastupdate);

    Observable<List<BusinessEmployee>> getBusinessEmployees();

    Observable<ConfigResponse> getConfig(String car_plate);


    Observable<List<ModelResponse>> getModel(String plate);
}
