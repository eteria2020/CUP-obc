package eu.philcar.csg.OBC.data.datasources;

import java.net.URLEncoder;

import eu.philcar.csg.OBC.data.datasources.api.SharengoBeaconApi;
import eu.philcar.csg.OBC.data.datasources.base.BaseRetrofitDataSource;
import eu.philcar.csg.OBC.data.model.BeaconResponse;
import io.reactivex.Observable;

/**
 * Created by Fulvio on 01/03/2018.
 */

public class SharengoBeaconRetrofitDataSource extends BaseRetrofitDataSource implements SharengoBeaconDataSource {

    private final SharengoBeaconApi mSharengoBeaconApi;

    public SharengoBeaconRetrofitDataSource(SharengoBeaconApi mSharengoBeaconApi) {
        this.mSharengoBeaconApi = mSharengoBeaconApi;
    }

    @Override
    public Observable<BeaconResponse> sendBeacon(String plate, String beacon) {

        try{
            beacon= URLEncoder.encode(beacon,"UTF-8");
        }catch (Exception e){

        }

        return  mSharengoBeaconApi.sendBeacon(plate,beacon)
                .compose(this.handleRetrofitRequest());
    }
}
