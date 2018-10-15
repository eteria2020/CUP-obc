package eu.philcar.csg.OBC.helpers;

import android.os.Handler;
import android.os.Message;

import com.skobbler.ngx.SKCategories.SKPOICategory;
import com.skobbler.ngx.SKMaps.SKLanguage;
import com.skobbler.ngx.packages.SKPackage;
import com.skobbler.ngx.search.SKMultiStepSearchSettings;
import com.skobbler.ngx.search.SKSearchListener;
import com.skobbler.ngx.search.SKSearchManager;
import com.skobbler.ngx.search.SKSearchManager.SKListLevel;
import com.skobbler.ngx.search.SKSearchResult;
import com.skobbler.ngx.search.SKSearchResult.SKSearchResultType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkobblerSearch implements SKSearchListener {

	private final String DefaultPackage = "IT";
	private List<SKPackage> packages;
	private Map<Short, List<SKSearchResult>> resultsPerLevel = new HashMap<Short, List<SKSearchResult>>();
	private SKSearchManager searchManager;
	private int currentListLevel;
	private String[] preselections;
	private Handler handler;
	private boolean rawResults = false;

	private ArrayList<SKPOICategory> categoryFilterList = null;

	public final static int MSG_FOUND_CITY = 1;
	public final static int MSG_FOUND_STREET = 2;
	public final static int MSG_FOUND_HOUSENUMBER = 3;
	public final static int MSG_RAW = 4;

	public SkobblerSearch() {
		searchManager = new SKSearchManager(this);
	}

	public void setRawResult(boolean value) {
		rawResults = value;
	}

	public void resetCategoryFilter() {
		categoryFilterList = null;
	}

	public void addCategoryFilter(SKPOICategory category) {

		if (categoryFilterList == null)
			categoryFilterList = new ArrayList<SKPOICategory>();

		if (!categoryFilterList.contains(category))
			categoryFilterList.add(category);
	}

	public void removeCategory(SKPOICategory category) {
		if (categoryFilterList != null && categoryFilterList.contains(category)) {
			categoryFilterList.remove(category);
		}
	}

	private void changeLevel(int newLevel, long parentId) {
		changeLevel(newLevel, parentId, "");
	}

	private void changeLevel(int newLevel, long parentId, String term) {
		if (newLevel == 0 || newLevel < currentListLevel) {
			// for new list level 0 or smaller than previous one just change the
			// level and update the adapter
			currentListLevel = newLevel;
		} else if (newLevel > currentListLevel && newLevel > 0) {
			// for new list level greater than previous one execute an offline
			// address search

			// get a search object
			SKMultiStepSearchSettings searchObject = new SKMultiStepSearchSettings();
			// set the maximum number of results to be returned
			searchObject.setMaxSearchResultsNumber(25);
			// set the country code
			searchObject.setOfflinePackageCode(DefaultPackage);
			// set the search term
			searchObject.setSearchTerm(term);
			// set the id of the parent node in which to search
			searchObject.setParentIndex(parentId);
			// set the list level
			searchObject.setListLevel(SKListLevel.forInt(newLevel));
			// change the list level to the new one
			currentListLevel = newLevel;
			// initiate the search
			searchManager.multistepSearch(searchObject);
		}
	}

	public void preselect(String[] selections, Handler handler) {

		if (selections == null || selections.length == 0)
			return;

		preselections = selections;
		changeLevel(0, -1);
		changeLevel(2, -1, selections[0]);

	}

	private SKMultiStepSearchSettings getSearchSettings() {
		SKMultiStepSearchSettings searchObject = new SKMultiStepSearchSettings();
		// set the maximum number of results to be returned
		searchObject.setMaxSearchResultsNumber(25);
		// set the country code
		searchObject.setOfflinePackageCode(DefaultPackage);
		searchObject.setSearchLanguage(SKLanguage.LANGUAGE_IT);

		return searchObject;
	}

	public void setHandler(Handler handler) {
		this.handler = handler;
	}

	public void SearchCity(String city) {
		SKMultiStepSearchSettings searchObj = getSearchSettings();
		searchObj.setListLevel(SKListLevel.SK_LIST_LEVEL_CITY);
		searchObj.setParentIndex(-1);
		searchObj.setSearchTerm(city);

		searchManager.multistepSearch(searchObj);
	}

	public void SearchStreet(String street, long parent) {
		SKMultiStepSearchSettings searchObj = getSearchSettings();
		searchObj.setListLevel(SKListLevel.SK_LIST_LEVEL_STREET);
		searchObj.setParentIndex(parent);
		searchObj.setSearchTerm(street);

		searchManager.multistepSearch(searchObj);
	}

	public void SearchHouseNumber(String HouseNumber, long parent) {
		SKMultiStepSearchSettings searchObj = getSearchSettings();
		searchObj.setListLevel(SKListLevel.SK_LIST_LEVEL_HOUSENUMBER);
		searchObj.setParentIndex(parent);
		searchObj.setSearchTerm(HouseNumber);

		searchManager.multistepSearch(searchObj);
	}

	public boolean applyCategoryFilter(SKSearchResult result) {

		if (categoryFilterList == null || categoryFilterList.isEmpty())
			return true;

		return categoryFilterList.contains(result.getCategory());

	}

	@Override
	public void onReceivedSearchResults(List<SKSearchResult> list) {
		int i = 0;
		Message msg = new Message();
		List<SKSearchResult> results = new ArrayList<SKSearchResult>();

		if (rawResults) {
			if (list != null && !list.isEmpty()) {
				while (i < list.size()) {
					if (applyCategoryFilter(list.get(i)))
						results.add(list.get(i));
					i++;
				}
			}
			msg.what = MSG_RAW;
			msg.obj = results;
		} else {

			if (list != null && !list.isEmpty()) {
				while (i < list.size()) {
					SKSearchResult r = list.get(i);

					SKSearchResultType rt = r.getType();
					if (rt.equals(SKSearchResultType.CITY) ||
							rt.equals(SKSearchResultType.CITY_SECTOR)) {
						msg.what = MSG_FOUND_CITY;
						results.add(r);
						msg.obj = results;
						break;
					} else if (rt.equals(SKSearchResultType.STREET)) {
						msg.what = MSG_FOUND_STREET;
						results.add(r);
						msg.obj = results;

						break;
					} else if (rt.equals(SKSearchResultType.HOUSE_NUMBER) ||
							(rt.equals(SKSearchResultType.POINT) && r.getCategory().equals(SKPOICategory.SKPOI_CATEGORY_UNKNOWN))) {
						msg.what = MSG_FOUND_HOUSENUMBER;
						results.add(r);
						msg.obj = results;

						break;
					}
					i++;
				}
			}
		}

		if (handler != null)
			handler.sendMessage(msg);

	
		/*
		if (preselections!=null && currentListLevel<=preselections.length ) {
			String term = preselections[currentListLevel-1];
			changeLevel(currentListLevel+1, list.get(0).getId(),term);
		} else {
			if (handler!=null) {
				Message msg = handler.obtainMessage();
				msg.obj = list;
				handler.sendMessage(msg);
			}
		}
		*/

	}

}
