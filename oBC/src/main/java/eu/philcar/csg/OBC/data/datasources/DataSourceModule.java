package eu.philcar.csg.OBC.data.datasources;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import eu.philcar.csg.OBC.data.datasources.api.SharengoApi;
import eu.philcar.csg.OBC.service.DataManager;

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



}
