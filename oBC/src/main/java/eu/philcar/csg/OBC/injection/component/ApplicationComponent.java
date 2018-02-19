package eu.philcar.csg.OBC.injection.component;

import android.app.Application;
import android.content.Context;

import javax.inject.Singleton;

import dagger.Component;
import eu.philcar.csg.OBC.controller.map.FHome;
import eu.philcar.csg.OBC.data.datasources.DataSourceModule;
import eu.philcar.csg.OBC.data.datasources.api.ApiModule;
import eu.philcar.csg.OBC.injection.ApplicationContext;
import eu.philcar.csg.OBC.injection.module.ApplicationModule;
import eu.philcar.csg.OBC.data.datasources.api.SharengoService;
import eu.philcar.csg.OBC.service.ObcService;
import eu.philcar.csg.OBC.service.SyncService;
import eu.philcar.csg.OBC.service.TripInfo;

@Singleton
@Component(
        modules = {
                ApplicationModule.class,
                DataSourceModule.class,
                ApiModule.class
        }
)
public interface ApplicationComponent {

    void inject(SyncService syncService);
    void inject(TripInfo tripInfo);
    void inject(FHome fHome);
    void inject(ObcService obcService);

    @ApplicationContext Context context();
    Application application();
    SharengoService sharengoService();

}
