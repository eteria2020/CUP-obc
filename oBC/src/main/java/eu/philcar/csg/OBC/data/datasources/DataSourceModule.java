package eu.philcar.csg.OBC.data.datasources;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import eu.philcar.csg.OBC.data.datasources.api.SharengoApi;
import eu.philcar.csg.OBC.data.datasources.api.SharengoBeaconApi;
import eu.philcar.csg.OBC.data.datasources.api.SharengoPhpApi;

/**
 * Created by Fulvio on 15/02/2018.
 */
@Module
public class DataSourceModule {

    @Provides
    @Singleton
    SharengoDataSource provideSharengoRemoteDataSource(SharengoApi api) {
        return new SharengoRetrofitDataSource(api);
    }

    @Provides
    @Singleton
    SharengoPhpDataSource provideSharengoPhpRemoteDataSource(SharengoPhpApi api) {
        return new SharengoPhpRetrofitDataSource(api);
    }

    @Provides
    @Singleton
    SharengoBeaconDataSource provideSharengoBeaconRemoteDataSource(SharengoBeaconApi api) {
        return new SharengoBeaconRetrofitDataSource(api);
    }

}
