package eu.philcar.csg.OBC.injection.component;

import dagger.Subcomponent;
import it.bomby.mycookbook.injection.PerActivity;
import it.bomby.mycookbook.injection.module.ActivityModule;
import it.bomby.mycookbook.ui.base.BaseActivity;
import it.bomby.mycookbook.ui.camera.CameraActivity;
import it.bomby.mycookbook.ui.main.MainActivity;
import it.bomby.mycookbook.ui.notification.NoticifationActivity;
import it.bomby.mycookbook.ui.recipe.RecipeActivity;
import it.bomby.mycookbook.ui.settings.SettingsActivity;
import it.bomby.mycookbook.ui.user.UserActivity;

/**
 * This component inject dependencies to all Activities across the application
 */
@PerActivity
@Subcomponent(modules = ActivityModule.class)
public interface ActivityComponent {

    void inject(BaseActivity baseActivity);
    void inject(MainActivity mainActivity);
    void inject(RecipeActivity homeActivity);
    void inject(SettingsActivity settingsActivity);
    void inject(UserActivity userActivity);
    void inject(NoticifationActivity noticifationActivity);
    void inject(CameraActivity cameraActivity);

}
