package eu.philcar.csg.OBC.injection.component;

import android.app.Application;
import android.content.Context;

import javax.inject.Singleton;

import dagger.Component;
import eu.philcar.csg.OBC.injection.ApplicationContext;
import eu.philcar.csg.OBC.injection.module.ApplicationModule;
import eu.philcar.csg.OBC.service.SharengoService;
import eu.philcar.csg.OBC.service.SyncService;

@Singleton
@Component(modules = ApplicationModule.class)
public interface ApplicationComponent {

    void inject(SyncService syncService);

    @ApplicationContext Context context();
    Application application();
    SharengoService sharengoService();

}
