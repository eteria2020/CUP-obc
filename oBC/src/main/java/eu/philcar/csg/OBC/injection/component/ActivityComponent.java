package eu.philcar.csg.OBC.injection.component;

import dagger.Subcomponent;
import eu.philcar.csg.OBC.injection.PerActivity;
import eu.philcar.csg.OBC.injection.module.ActivityModule;

/**
 * This component inject dependencies to all Activities across the application
 */
@PerActivity
@Subcomponent(modules = ActivityModule.class)
public interface ActivityComponent {



}
