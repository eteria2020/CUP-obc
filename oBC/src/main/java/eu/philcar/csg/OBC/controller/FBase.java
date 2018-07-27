package eu.philcar.csg.OBC.controller;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import butterknife.Unbinder;
import eu.philcar.csg.OBC.helpers.DLog;

public abstract class FBase extends Fragment {

	protected Unbinder unbinder;

	/**
	 * When required, this method allows fragments to properly handle the back button navigation. 
	 * 
	 * @return	A boolean indicating to the main activity when the fragment has handled the back-action and there's no need for further action (true) 
	 * 			or when the fragment doesn't need any special action and the main activity should handle the back-action (false)
	 */
	public boolean handleBackButton() {
		return true;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if(unbinder!=null){
			//unbinder.unbind();
		}
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		DLog.D("OnViewCreated " + this.getClass());
	}
}
