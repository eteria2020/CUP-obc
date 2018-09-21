package eu.philcar.csg.OBC;

import java.util.Locale;

import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.controller.FBase;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.service.ServiceClient;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentManager.BackStackEntry;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.DisplayMetrics;

import com.skobbler.ngx.SKMaps;
import com.skobbler.ngx.SKMapsInitSettings;
import com.skobbler.ngx.map.SKMapViewStyle;
import com.skobbler.ngx.navigation.SKAdvisorSettings;
import com.skobbler.ngx.routing.SKRouteManager;

public abstract class ABase extends Activity implements ServiceClient{


	private DLog dlog = new DLog(this.getClass());
	protected App mApp;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		//dlog.d("ABase onCreate");
		
		// This has to be isSystem each time an activity starts ('cause we are fighting the standard way android deals with string localization)
		changeLanguage(getActivityLocale());

        mApp = (App)getApplicationContext();
		DLog.I("Lifecycle - onCreateActivity: " + this.getClass().getSimpleName());
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
        DLog.I("Lifecycle - onDestroyActivity: " + this.getClass().getSimpleName());
        
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
		SKMapsInitSettings initSettings = new SKMapsInitSettings();

		initSettings.setConnectivityMode(SKMaps.CONNECTIVITY_MODE_OFFLINE);
		initSettings.setMapsPath("/sdcard/SKMaps/");
		initSettings.setMapResourcesPaths("/sdcard/SKMaps/", new SKMapViewStyle("/sdcard/SKMaps/daystyle/","daystyle.json"));
		initSettings.setPreinstalledMapsPath("/sdcard/SKMaps/PreinstalledMaps/");


		initSettings.setMapDetailLevel(SKMapsInitSettings.SK_MAP_DETAIL_LIGHT);


		SKAdvisorSettings advisorSettings = new SKAdvisorSettings();
		advisorSettings.setLanguage(SKAdvisorSettings.SKAdvisorLanguage.LANGUAGE_EN);
		advisorSettings.setAdvisorConfigPath("/sdcard/SKMaps/Advisor");
		advisorSettings.setResourcePath("/sdcard/SKMaps/Advisor/Languages");
		advisorSettings.setAdvisorVoice("en");
		advisorSettings.setAdvisorType( SKAdvisorSettings.SKAdvisorType.TEXT_TO_SPEECH);
		initSettings.setAdvisorSettings(advisorSettings);

		SKMapViewStyle style = new SKMapViewStyle("/sdcard/skmaps/daystyle/","daystyle.json");
		initSettings.setCurrentMapViewStyle(style);


		SKRouteManager.getInstance().setAudioAdvisorSettings(advisorSettings);
	}
	
	/**
	 * This method changes the current activity language to italian
	 */
	public void setItalianLanguage() {
		saveLanguage("it");
		changeLanguage("it");
		SKMapsInitSettings initSettings = new SKMapsInitSettings();

		initSettings.setConnectivityMode(SKMaps.CONNECTIVITY_MODE_OFFLINE);
		initSettings.setMapsPath("/sdcard/SKMaps/");
		initSettings.setMapResourcesPaths("/sdcard/SKMaps/", new SKMapViewStyle("/sdcard/SKMaps/daystyle/","daystyle.json"));
		initSettings.setPreinstalledMapsPath("/sdcard/SKMaps/PreinstalledMaps/");


		initSettings.setMapDetailLevel(SKMapsInitSettings.SK_MAP_DETAIL_LIGHT);


		SKAdvisorSettings advisorSettings = new SKAdvisorSettings();
		advisorSettings.setLanguage(SKAdvisorSettings.SKAdvisorLanguage.LANGUAGE_IT);
		advisorSettings.setAdvisorConfigPath("/sdcard/SKMaps/Advisor");
		advisorSettings.setResourcePath("/sdcard/SKMaps/Advisor/Languages");
		advisorSettings.setAdvisorVoice("it");
		advisorSettings.setAdvisorType( SKAdvisorSettings.SKAdvisorType.TEXT_TO_SPEECH);
		initSettings.setAdvisorSettings(advisorSettings);

		SKMapViewStyle style = new SKMapViewStyle("/sdcard/skmaps/daystyle/","daystyle.json");
		initSettings.setCurrentMapViewStyle(style);


		SKRouteManager.getInstance().setAudioAdvisorSettings(advisorSettings);
	}

	public void setFrenchLanguage() {
		saveLanguage("fr");
		changeLanguage("fr");
		SKMapsInitSettings initSettings = new SKMapsInitSettings();

		initSettings.setConnectivityMode(SKMaps.CONNECTIVITY_MODE_OFFLINE);
		initSettings.setMapsPath("/sdcard/SKMaps/");
		initSettings.setMapResourcesPaths("/sdcard/SKMaps/", new SKMapViewStyle("/sdcard/SKMaps/daystyle/","daystyle.json"));
		initSettings.setPreinstalledMapsPath("/sdcard/SKMaps/PreinstalledMaps/");


		initSettings.setMapDetailLevel(SKMapsInitSettings.SK_MAP_DETAIL_LIGHT);


		SKAdvisorSettings advisorSettings = new SKAdvisorSettings();
		advisorSettings.setLanguage(SKAdvisorSettings.SKAdvisorLanguage.LANGUAGE_FR);
		advisorSettings.setAdvisorConfigPath("/sdcard/SKMaps/Advisor");
		advisorSettings.setResourcePath("/sdcard/SKMaps/Advisor/Languages");
		advisorSettings.setAdvisorVoice("fr");
		advisorSettings.setAdvisorType( SKAdvisorSettings.SKAdvisorType.TEXT_TO_SPEECH);
		initSettings.setAdvisorSettings(advisorSettings);

		SKMapViewStyle style = new SKMapViewStyle("/sdcard/skmaps/daystyle/","daystyle.json");
		initSettings.setCurrentMapViewStyle(style);


		SKRouteManager.getInstance().setAudioAdvisorSettings(advisorSettings);
	}

	public void setChinseseLanguage() {
		saveLanguage("zh");
		changeLanguage("zh");
		SKMapsInitSettings initSettings = new SKMapsInitSettings();

		initSettings.setConnectivityMode(SKMaps.CONNECTIVITY_MODE_OFFLINE);
		initSettings.setMapsPath("/sdcard/SKMaps/");
		initSettings.setMapResourcesPaths("/sdcard/SKMaps/", new SKMapViewStyle("/sdcard/SKMaps/daystyle/","daystyle.json"));
		initSettings.setPreinstalledMapsPath("/sdcard/SKMaps/PreinstalledMaps/");


		initSettings.setMapDetailLevel(SKMapsInitSettings.SK_MAP_DETAIL_LIGHT);


		SKAdvisorSettings advisorSettings = new SKAdvisorSettings();
		advisorSettings.setLanguage(SKAdvisorSettings.SKAdvisorLanguage.LANGUAGE_EN);
		advisorSettings.setAdvisorConfigPath("/sdcard/SKMaps/Advisor");
		advisorSettings.setResourcePath("/sdcard/SKMaps/Advisor/Languages");
		advisorSettings.setAdvisorVoice("en");
		advisorSettings.setAdvisorType( SKAdvisorSettings.SKAdvisorType.TEXT_TO_SPEECH);
		initSettings.setAdvisorSettings(advisorSettings);

		SKMapViewStyle style = new SKMapViewStyle("/sdcard/skmaps/daystyle/","daystyle.json");
		initSettings.setCurrentMapViewStyle(style);


		SKRouteManager.getInstance().setAudioAdvisorSettings(advisorSettings);
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
	 * Use this method to push (and display) a new fragment (i.e., like [UINavigationController pushViewController:animated]).
	 * Note that "fragment" must inherits from FBase.
	 *
	 * @param fragment		A fragment (which inherits from FBase) that will be displayed
	 * @param fragmentName The name associated with the transaction (e.g., [YourClassHere].class.getName()). If the back button won't be used, then this parameter will be removed before release but until then you should stick with it.
	 * @param animated		Self-explanatory
	 */
	public void pushFragmentNoBack(FBase fragment, String fragmentName, boolean animated, FBase closingFragment) {

		//getFragmentManager().popBackStack();
		FragmentTransaction transaction = getFragmentManager().beginTransaction();


		if (animated) {
			transaction.setCustomAnimations(R.animator.slide_in_right, R.animator.slide_out_left, R.animator.slide_in_left, R.animator.slide_out_right);
		}
		transaction.remove(closingFragment);
		transaction.add(getPlaceholderResource(), fragment, fragmentName);
		transaction.disallowAddToBackStack();
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
	public class PopTillException extends Exception {
		public PopTillException(String message) {
			super(message);
		}
	}
	public void popTillFragment(String fragmentName) throws PopTillException{
		
		try {
		getFragmentManager().popBackStackImmediate(fragmentName, 0);
		} catch (Exception e) {
			DLog.E("popTillFragment :",e);
			throw new PopTillException("Reset activity, unable to pop back stack");
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
		edit.apply();
	}
	
	private void changeLanguage(String locale) {
		
		DisplayMetrics dm = getResources().getDisplayMetrics();
	    android.content.res.Configuration conf = getResources().getConfiguration();
	    conf.locale = new Locale(locale);
	    getResources().updateConfiguration(conf, dm);
	}
}
