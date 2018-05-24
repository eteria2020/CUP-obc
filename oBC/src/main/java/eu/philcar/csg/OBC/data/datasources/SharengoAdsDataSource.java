package eu.philcar.csg.OBC.data.datasources;

import eu.philcar.csg.OBC.data.model.AdsImage;
import eu.philcar.csg.OBC.data.model.AdsResponse;
import io.reactivex.Observable;
import retrofit2.http.Query;

/**
 * Created by Fulvio on 17/05/2018.
 */

public interface SharengoAdsDataSource {


    Observable<AdsResponse> getBannerCars(String id, String lat, String lon, String id_fleet,
                                                String plate, String index, String end);

    Observable<AdsResponse> getBannerStart(String id, String lat, String lon, String id_fleet,
                                                String plate, String index, String end);

    Observable<AdsResponse> getBannerEnd(String id, String lat, String lon, String id_fleet,
                                                String plate, String index, String end);

    void shouldDownload(AdsImage image);

    Observable<AdsResponse> updateImages(AdsResponse response);

}
