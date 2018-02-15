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
import eu.philcar.csg.OBC.service.SyncService;

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
    void inject(FHome fHome);

    @ApplicationContext Context context();
    Application application();
    SharengoService sharengoService();

}
