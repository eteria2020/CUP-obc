package eu.philcar.csg.OBC.data.datasources.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

import eu.philcar.csg.OBC.data.common.SerializationExclusionStrategy;
import eu.philcar.csg.OBC.db.Customer;
import io.reactivex.Observable;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;

/**
 * Created by Fulvio on 09/02/2018.
 */

public interface SharengoService {
    String ENDPOINT = "http://corestage.sharengo.it/api/";

    @GET("test.txt")
    Observable<List<Customer>> getCustomer();

    /******** Helper class that sets up a new services *******/
    class Creator {

        public static SharengoService newSharengoService() {
            Gson gson = new GsonBuilder()
                    .addSerializationExclusionStrategy(new SerializationExclusionStrategy())
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    .create();
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(SharengoService.ENDPOINT)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build();
            return retrofit.create(SharengoService.class);
        }
    }
}
