package eu.philcar.csg.OBC.data.datasources.api;

import java.util.List;

import eu.philcar.csg.OBC.data.model.ConfigResponse;
import eu.philcar.csg.OBC.data.model.ModelResponse;
import eu.philcar.csg.OBC.db.BusinessEmployee;
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

    @GET("business-employees")
    Observable<Result<List<BusinessEmployee>>> getBusinessEmployees();


    @GET("configs")
    Observable<Result<ConfigResponse>> getConfigs(@Query("car_plate") String car_plate);


    @GET("carmodel")
    Observable<Result<List<ModelResponse>>> getModel(@Query("plate") String car_plate);
}
