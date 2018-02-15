package eu.philcar.csg.OBC.data.datasources;

import java.util.List;

import eu.philcar.csg.OBC.data.model.ResponseCustomer;
import eu.philcar.csg.OBC.db.Customer;
import io.reactivex.Observable;

/**
 * Created by Fulvio on 15/02/2018.
 */

public interface SharengoDataSource {

    Observable<List<Customer>> getCustomer(long lastupdate);
}
