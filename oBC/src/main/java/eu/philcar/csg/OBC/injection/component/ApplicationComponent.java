package eu.philcar.csg.OBC.injection.component;

import android.app.Application;
import android.content.Context;

import javax.inject.Singleton;

import dagger.Component;
import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.SystemControl;
import eu.philcar.csg.OBC.controller.FBase;
import eu.philcar.csg.OBC.controller.map.FHome;
import eu.philcar.csg.OBC.controller.map.FMap;
import eu.philcar.csg.OBC.controller.map.FMenu;
import eu.philcar.csg.OBC.controller.sos.FSOS;
import eu.philcar.csg.OBC.controller.welcome.FCleanliness;
import eu.philcar.csg.OBC.controller.welcome.FDriveMessage_new;
import eu.philcar.csg.OBC.controller.welcome.FGoodbye;
import eu.philcar.csg.OBC.controller.welcome.FMaintenance;
import eu.philcar.csg.OBC.controller.welcome.FWelcome;
import eu.philcar.csg.OBC.data.datasources.DataSourceModule;
import eu.philcar.csg.OBC.data.datasources.api.ApiModule;
import eu.philcar.csg.OBC.data.datasources.repositories.SharengoPhpRepository;
import eu.philcar.csg.OBC.devices.Hik_io;
import eu.philcar.csg.OBC.helpers.ServiceTestActivity;
import eu.philcar.csg.OBC.injection.ApplicationContext;
import eu.philcar.csg.OBC.injection.module.ApplicationModule;
import eu.philcar.csg.OBC.data.datasources.api.SharengoService;
import eu.philcar.csg.OBC.service.CarInfo;
import eu.philcar.csg.OBC.service.ObcService;
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

    void inject(SystemControl.TestConnection testConnection);
    void inject(App app);
    void inject(SystemControl.Shutdown shutdown);
    void inject(CarInfo carInfo);
    void inject(FSOS fsos);
    void inject(FMap fMap);
    void inject(FWelcome fWelcome);
    void inject(Hik_io hik_io);
    void inject(ServiceTestActivity serviceTestActivity);
    void inject(FMenu fMenu);
    void inject(FCleanliness fCleanliness);
    void inject(FMaintenance fMaintenance);
    void inject(TripInfo tripInfo);
    void inject(FHome fHome);
    void inject(FDriveMessage_new fDriveMessage_new);
    void inject(FGoodbye fGoodbye);
    void inject(ObcService obcService);

    @ApplicationContext Context context();
    Application application();
    SharengoService sharengoService();
    SharengoPhpRepository sharengoPhpRepository();

}
