package eu.philcar.csg.OBC.injection.component;

import dagger.Subcomponent;
import it.bomby.mycookbook.injection.PerActivity;
import it.bomby.mycookbook.injection.module.FragmentModule;
import it.bomby.mycookbook.ui.recipe.fragment.details.RecipeDetailsFragment;
import it.bomby.mycookbook.ui.recipe.fragment.newRecipe.NewRecipeFragment;
import it.bomby.mycookbook.ui.recipe.fragment.tabs.tab1.Tab1Fragment;
import it.bomby.mycookbook.ui.recipe.fragment.tabs.tab2.Tab2Fragment;
import it.bomby.mycookbook.ui.recipe.fragment.tabs.tab3.Tab3Fragment;

/**
 * This component inject dependencies to all MvpFragments across the application
 */
@PerActivity
@Subcomponent(modules = FragmentModule.class )
public interface FragmentComponent {

    void inject(Tab1Fragment fragment);
    void inject(Tab2Fragment fragment);
    void inject(Tab3Fragment fragment);
    void inject(RecipeDetailsFragment fragment);
    void inject(NewRecipeFragment fragment);

}
