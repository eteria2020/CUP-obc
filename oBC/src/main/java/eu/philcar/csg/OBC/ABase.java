package eu.philcar.csg.OBC;

import java.util.Locale;

import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.controller.FBase;
import eu.philcar.csg.OBC.helpers.DLog;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.FragmentManager.BackStackEntry;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.DisplayMetrics;

public abstract class ABase extends Activity {


	private DLog dlog = new DLog(this.getClass());
	protected App mApp;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		dlog.d("ABase onCreate");
		
		// This has to be isSystem each time an activity starts ('cause we are fighting the standard way android deals with string localization)
		changeLanguage(getActivityLocale());

        mApp = (App)getApplicationContext();
	}

	@Override
	protected void onResume() {
		
		super.onResume();
		
        mApp.setCurrentActivity(this);
	}
	
	@Override
	protected void onPause() {

        clearReferences();
        
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		
        clearReferences();
        
        super.onDestroy();
    }
	
	public void clearReferences() {
		
		if (mApp != null) {
		
			ABase currActivity = mApp.getCurrentActivity();
			
			if (currActivity != null && currActivity.getActivityUID() == this.getActivityUID()) {
				mApp.setCurrentActivity(null);
			}
		}
    }
	
	public abstract int getActivityUID();
	
	/**
	 * This method must return the resource of the placeholder in the activity layout to which fragments will be attached.
	 * 
	 * @return The resource id of the layout placeholder
	 */
	protected abstract int getPlaceholderResource();
	
	/**
	 * This method changes the current activity language to english
	 */
	public void setEnglishLanguage() {
		saveLanguage("en");
		changeLanguage("en");
	}
	
	/**
	 * This method changes the current activity language to italian
	 */
	public void setItalianLanguage() {
		saveLanguage("it");
		changeLanguage("it");
	}

	public void setFrenchLanguage() {
		saveLanguage("fr");
		changeLanguage("fr");
	}
	
	/**
	 * Use this method to push (and display) a new fragment (i.e., like [UINavigationController pushViewController:animated]).
	 * Note that "fragment" must inherits from FBase.
	 * 
	 * @param fragment		A fragment (which inherits from FBase) that will be displayed
	 * @param fragmentName The name associated with the transaction (e.g., [YourClassHere].class.getName()). If the back button won't be used, then this parameter will be removed before release but until then you should stick with it.
	 * @param animated		Self-explanatory
	 */
	public void pushFragment(FBase fragment, String fragmentName, boolean animated) {
		
		FragmentTransaction transaction = getFragmentManager().beginTransaction();
		
		if (animated) {
			transaction.setCustomAnimations(R.animator.slide_in_right, R.animator.slide_out_left, R.animator.slide_in_left, R.animator.slide_out_right);
		}
		transaction.replace(getPlaceholderResource(), fragment, fragmentName);
		transaction.addToBackStack(fragmentName);	// If the back button won't be used, this line can be removed in sake of performance. It is planned to remove it before release but until then, you should stick with it and with its parameter
		
		transaction.commit();
	}

	/**
	 * Use this method to pop the fragment until the fragment name, in not present push (and display) a new fragment (i.e., like [UINavigationController pushViewController:animated]).
	 * Note that "fragment" must inherits from FBase.
	 *
	 * @param fragment		A fragment (which inherits from FBase) that will be displayed
	 * @param fragmentName The name associated with the transaction (e.g., [YourClassHere].class.getName()). If the back button won't be used, then this parameter will be removed before release but until then you should stick with it.
	 * @param animated		Self-explanatory
	 */
	public void pushBackFragment(FBase fragment, String fragmentName, boolean animated) {

		boolean success=false;
		try {
			success=getFragmentManager().popBackStackImmediate(fragmentName, 0);
		} catch (Exception e) {
			DLog.E("popTillFragment :",e);
		}
		if(success)
			return;
		else {

			FragmentTransaction transaction = getFragmentManager().beginTransaction();

			if (animated) {
				transaction.setCustomAnimations(R.animator.slide_in_right, R.animator.slide_out_left, R.animator.slide_in_left, R.animator.slide_out_right);
			}
			transaction.replace(getPlaceholderResource(), fragment, fragmentName);
			transaction.addToBackStack(fragmentName);    // If the back button won't be used, this line can be removed in sake of performance. It is planned to remove it before release but until then, you should stick with it and with its parameter

			transaction.commit();
		}
	}
	
	/**
	 * Use this method to pop the last pushed fragment.
	 */
	public void popFragment() {
		
		int entryCount = getFragmentManager().getBackStackEntryCount();
		
		if (entryCount <= 1) {
			finish();
			return;
		} else {			
			getFragmentManager().popBackStack();
		} 
	}
	
	/**
	 * Use this method to pop back until the custom fragment is reached.
	 */
	public void popTillFragment(String fragmentName) {
		
		try {
		getFragmentManager().popBackStackImmediate(fragmentName, 0);
		} catch (Exception e) {
			DLog.E("popTillFragment :",e);
		}
		
		int entryCount = getFragmentManager().getBackStackEntryCount();
		
		if (entryCount < 1) {
			finish();
			return;
		} 
	}
	
	@Override
	public void onBackPressed() {
		
		int entryCount = getFragmentManager().getBackStackEntryCount();
		
		BackStackEntry entry;
		if (entryCount>0) {
			 entry = getFragmentManager().getBackStackEntryAt(entryCount-1);
		} else {
			finish();
			return;
		}
		
		FBase currentFragment = (FBase)getFragmentManager().findFragmentByTag(entry.getName());
		
		if (!currentFragment.handleBackButton()) {
			
			if (entryCount == 1) {
				finish();
				return;
			} else {
				super.onBackPressed();
			}
		}
	}
	
	public FBase getActiveFragment() {
		
		int entryCount = getFragmentManager().getBackStackEntryCount();
		
		BackStackEntry entry = getFragmentManager().getBackStackEntryAt(entryCount-1);
		
		return (FBase)getFragmentManager().findFragmentByTag(entry.getName());
	}
	
	public String getActivityLocale() {
		
		// Since we are fighting the standard way Android handles string localization, user chosen locale must be saved in shared preferences and set for each activity, otherwise system locale will be chosen by the o.s. 
		SharedPreferences sp = getSharedPreferences(getResources().getString(R.string.app_name), Context.MODE_PRIVATE); 
		return sp.getString("language", "it");
	}
	
	private void saveLanguage(String locale) {
		
		SharedPreferences sp = getSharedPreferences(getResources().getString(R.string.app_name), Context.MODE_PRIVATE);
		Editor edit = sp.edit();
		edit.putString("language", locale);
		edit.commit();
	}
	
	private void changeLanguage(String locale) {
		
		DisplayMetrics dm = getResources().getDisplayMetrics();
	    android.content.res.Configuration conf = getResources().getConfiguration();
	    conf.locale = new Locale(locale);
	    getResources().updateConfiguration(conf, dm);
	}
}
