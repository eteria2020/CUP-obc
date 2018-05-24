package eu.philcar.csg.OBC.data.datasources.api;

import eu.philcar.csg.OBC.data.model.AdsResponse;
import io.reactivex.Observable;
import retrofit2.adapter.rxjava2.Result;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by Fulvio on 17/05/2018.
 */

public interface SharengoAdsApi {

    @GET("banner2_offline.php")
    Observable<Result<AdsResponse>> getBannerCars(@Query("id") String id, @Query("lat") String lat,@Query("lon") String lon, @Query("id_fleet") String id_fleet,
                                                  @Query("carplate") String plate,@Query("index") String index,@Query("end") String end);

    @GET("banner4_offline.php")
    Observable<Result<AdsResponse>> getBannerStart(@Query("id") String id, @Query("lat") String lat,@Query("lon") String lon, @Query("id_fleet") String id_fleet,
                                                   @Query("carplate") String plate,@Query("index") String index,@Query("end") String end);

    @GET("banner5_offline.php")
    Observable<Result<AdsResponse>> getBannerEnd(@Query("id") String id, @Query("lat") String lat,@Query("lon") String lon, @Query("id_fleet") String id_fleet,
                                                 @Query("carplate") String plate,@Query("index") String index,@Query("end") String end);
}
