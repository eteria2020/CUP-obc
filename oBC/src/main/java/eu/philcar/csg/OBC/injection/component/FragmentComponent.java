package eu.philcar.csg.OBC.injection.component;

import dagger.Subcomponent;
import eu.philcar.csg.OBC.injection.PerActivity;
import eu.philcar.csg.OBC.injection.module.FragmentModule;

/**
 * This component inject dependencies to all MvpFragments across the application
 */
@PerActivity
@Subcomponent(modules = FragmentModule.class )
public interface FragmentComponent {



}
