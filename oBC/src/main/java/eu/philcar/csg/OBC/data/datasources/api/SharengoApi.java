package eu.philcar.csg.OBC.data.datasources.api;

import java.util.List;

import eu.philcar.csg.OBC.data.model.ResponseCustomer;
import eu.philcar.csg.OBC.db.Customer;
import io.reactivex.Observable;
import retrofit2.adapter.rxjava2.Result;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by Fulvio on 15/02/2018.
 */

public interface SharengoApi {

    @GET("whitelist2")
    Observable<Result<List<Customer>>> getCustomer(@Query("lastupdate") long lastupdate);
}
