package eu.philcar.csg.OBC.data.datasources;

import android.graphics.Bitmap;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.data.datasources.api.SharengoAdsApi;
import eu.philcar.csg.OBC.data.datasources.base.BaseRetrofitDataSource;
import eu.philcar.csg.OBC.data.model.AdsImage;
import eu.philcar.csg.OBC.data.model.AdsResponse;
import eu.philcar.csg.OBC.helpers.DLog;
import io.reactivex.Observable;

/**
 * Created by Fulvio on 17/05/2018.
 */

public class SharengoAdsRetrofitDataSource extends BaseRetrofitDataSource implements SharengoAdsDataSource {

    private final SharengoAdsApi mSharengoAdsApi;

    public SharengoAdsRetrofitDataSource(SharengoAdsApi mSharengoBeaconApi) {
        this.mSharengoAdsApi = mSharengoBeaconApi;
    }

    @Override
    public Observable<AdsResponse> getBannerCars(String id, String lat, String lon, String id_fleet, String plate, String index, String end) {
        return  mSharengoAdsApi.getBannerCars(id, lat, lon, id_fleet, plate, index, end)
                .compose(this.handleRetrofitRequest());
    }

    @Override
    public Observable<AdsResponse> getBannerStart(String id, String lat, String lon, String id_fleet, String plate, String index, String end) {
        return  mSharengoAdsApi.getBannerStart(id, lat, lon, id_fleet, plate, index, end)
                .compose(this.handleRetrofitRequest());
    }

    @Override
    public Observable<AdsResponse> getBannerEnd(String id, String lat, String lon, String id_fleet, String plate, String index, String end) {
        return  mSharengoAdsApi.getBannerEnd(id, lat, lon, id_fleet, plate, index, end)
                .compose(this.handleRetrofitRequest());
    }

    @Override
    public void shouldDownload(AdsImage response) {

        try {
            File outDir = new File(App.getBannerImagesFolder());
            if (!outDir.isDirectory()) {
                outDir.mkdir();
            }
            URL urlImg = new URL(response.imageUrl);
            String extension = urlImg.getFile().substring(urlImg.getFile().lastIndexOf('.') + 1);
            String filename = String.valueOf(response.id).concat(".").concat(extension);

            //download imagine se non esiste
            File file = new File(outDir, filename);

            if(file.exists()){
                return;
            }
            FileOutputStream fileOutput = new FileOutputStream(file);

            Picasso.get().load(response.imageUrl).get().compress(Bitmap.CompressFormat.PNG,100,fileOutput);
        } catch (Exception e) {
            DLog.E("Exception while downloading image",e);
        }
    }

    @Override
    public Observable<AdsResponse> updateImages(AdsResponse response) {
        for(AdsImage image: response.getImages())
            shouldDownload(image);
        return Observable.just(response);
    }


    public Observable<Bitmap> bannerClick(AdsImage image){
        return
    }
}
