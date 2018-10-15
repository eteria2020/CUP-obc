package eu.philcar.csg.OBC.injection.module;

import android.content.Context;
import android.support.v4.app.Fragment;

import dagger.Module;
import dagger.Provides;
import eu.philcar.csg.OBC.injection.ActivityContext;

@Module
public class FragmentModule {

	private Fragment mFragment;

	public FragmentModule(Fragment fragment) {
		mFragment = fragment;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//
	//          SPECIFICARE DI SEGUITO TUTTI I PROVIDERE DI TUTTI I PRESENTER DELL'APP
	//
	//
	////////////////////////////////////////////////////////////////////////////////////////////////

	@Provides
	Fragment provideFragment() {
		return mFragment;
	}

	@Provides
	@ActivityContext
	Context providesContext() {
		return mFragment.getActivity();
	}

}
