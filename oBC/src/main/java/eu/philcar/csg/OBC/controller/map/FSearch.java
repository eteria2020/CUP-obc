package eu.philcar.csg.OBC.controller.map;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.skobbler.ngx.SKCategories.SKPOICategory;
import com.skobbler.ngx.search.SKSearchListener;
import com.skobbler.ngx.search.SKSearchResult;
import com.skobbler.ngx.search.SKSearchResult.SKSearchResultType;

import java.util.ArrayList;
import java.util.List;

import eu.philcar.csg.OBC.ABase;
import eu.philcar.csg.OBC.AMainOBC;
import eu.philcar.csg.OBC.ASOS;
import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.controller.FBase;
import eu.philcar.csg.OBC.controller.map.CustomKeyboard.CustomKeyboardDelegate;
import eu.philcar.csg.OBC.controller.map.adapter.ALVSearchResults;
import eu.philcar.csg.OBC.controller.map.adapter.ALVSearchResults.ALVSearchResultsDelegate;
import eu.philcar.csg.OBC.controller.map.asynctask.ATGeocodingRequest;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.SkobblerSearch;

public class FSearch extends FBase implements OnClickListener, ALVSearchResultsDelegate, CustomKeyboardDelegate, SKSearchListener {

    public static FSearch newInstance() {

        FSearch fs = new FSearch();
        return fs;
    }

    private DLog dlog = new DLog(this.getClass());

    private DFProgressing progressDF;
    private TextView resultsTV, gotoTV;
    private EditText addressET, cityET, numberET;
    private Button sosB, searchB, btnSearch1, btnSearch2, btnSearch3, btnGo;
    private LinearLayout llRow1, llRow2, llRow3;
    private RelativeLayout resultsRL;
    private ImageButton backIB;
    private ListView resultsLV;
    private CustomKeyboard mCustomKeyboard;
    private SkobblerSearch sks;
    private RelativeLayout fpca_right_RL;

    private ATGeocodingRequest atRequest;
    private ALVSearchResults adapter;

    private SearchSteps searchSteps;

    private String defaultCity = "Milano";
    private View rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_search, container, false);
        dlog.d("OnCreareView FSerch");
        //System.gc();

        setupUI(view);

        progressDF = new DFProgressing();
        rootView = view;

        cityET = (EditText) view.findViewById(R.id.fseaCityED);
        addressET = (EditText) view.findViewById(R.id.fseaAddressED);
        numberET = (EditText) view.findViewById(R.id.fseaHouseNumberED);

        llRow1 = (LinearLayout) view.findViewById(R.id.llRow1);
        llRow2 = (LinearLayout) view.findViewById(R.id.llRow2);
        llRow3 = (LinearLayout) view.findViewById(R.id.llRow3);

        btnSearch1 = (Button) view.findViewById(R.id.btnSearch1);
        btnSearch2 = (Button) view.findViewById(R.id.btnSearch2);
        btnSearch3 = (Button) view.findViewById(R.id.btnSearch3);

        fpca_right_RL = (RelativeLayout) view.findViewById(R.id.fpca_right_RL);

        btnSearch1.setOnClickListener(this);
        btnSearch2.setOnClickListener(this);
        btnSearch3.setOnClickListener(this);

        view.findViewById(R.id.fseaGotoBTN).setOnClickListener(this);

        cityET.setRawInputType(InputType.TYPE_CLASS_TEXT);
        cityET.setTextIsSelectable(true);
        addressET.setRawInputType(InputType.TYPE_CLASS_TEXT);
        addressET.setTextIsSelectable(true);
        numberET.setRawInputType(InputType.TYPE_CLASS_TEXT);
        numberET.setTextIsSelectable(true);

        cityET.setText(App.Instance.loadDefaultCity());

        cityET.addTextChangedListener(new SearchFieldsWatcher(R.id.fseaCityED, this, R.id.btnSearch1, 1));
        addressET.addTextChangedListener(new SearchFieldsWatcher(R.id.fseaAddressED, this, R.id.btnSearch2, 2));
        numberET.addTextChangedListener(new SearchFieldsWatcher(R.id.fseaHouseNumberED, this, R.id.btnSearch3, 3));

        mCustomKeyboard = new CustomKeyboard(getActivity(), view, R.id.fseaKeyboardKV, R.xml.hexkbd, this);
        mCustomKeyboard.registerEditText(cityET);
        mCustomKeyboard.registerEditText(addressET);
        mCustomKeyboard.registerEditText(numberET);

        resultsLV = (ListView) view.findViewById(R.id.fseaResultsLV);
        resultsTV = (TextView) view.findViewById(R.id.fseaResultsTV);
        resultsRL = (RelativeLayout) view.findViewById(R.id.fseaResultsRL);
        gotoTV = (TextView) view.findViewById(R.id.fseaGotoTV);

        searchB = (Button) view.findViewById(R.id.fseaSearchB);

        sosB = (Button) view.findViewById(R.id.fseaSOSB);
        backIB = (ImageButton) view.findViewById(R.id.fseaBackIB);

        btnGo = (Button) view.findViewById(R.id.btnGo);

        adapter = new ALVSearchResults(getActivity(), new ArrayList<SKSearchResult>(), this);
        resultsLV.setAdapter(adapter);

        Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "interstateregular.ttf");

        sosB.setTypeface(font);

        searchB.setOnClickListener(this);
        sosB.setOnClickListener(this);
        backIB.setOnClickListener(this);
        btnGo.setOnClickListener(this);

        searchSteps = new SearchSteps();

        sks = new SkobblerSearch();
        sks.setRawResult(true);
        sks.addCategoryFilter(SKPOICategory.SKPOI_CATEGORY_BUS_STATION);
        sks.addCategoryFilter(SKPOICategory.SKPOI_CATEGORY_BUS_STOP);
        sks.addCategoryFilter(SKPOICategory.SKPOI_CATEGORY_SUBWAY_STATION);
        sks.addCategoryFilter(SKPOICategory.SKPOI_CATEGORY_STATION);
        sks.addCategoryFilter(SKPOICategory.SKPOI_CATEGORY_TRAIN_STATION);
        sks.addCategoryFilter(SKPOICategory.SKPOI_CATEGORY_UNKNOWN);

        sks.setHandler(this.searchHandler);

        updateUI(null);

        defaultCity = App.DefaultCity;

        sks.SearchCity(defaultCity);

        if (App.currentTripInfo != null && App.currentTripInfo.isMaintenance) {
            fpca_right_RL.setBackgroundColor(getResources().getColor(R.color.background_red));

        } else {
            fpca_right_RL.setBackgroundColor(getResources().getColor(R.color.background_green));
        }

        return view;
    }

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        try {
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setupUI(View view) {

        // Set up touch listener for non-text box views to hide keyboard.
        if (!(view instanceof EditText) && !(view instanceof android.inputmethodservice.KeyboardView)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    if (mCustomKeyboard.isCustomKeyboardVisible()) {
                        mCustomKeyboard.hideCustomKeyboard();
                    }
                    return false;
                }
            });
        }

        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setupUI(innerView);
            }
        }
    }

	/*public void runOneLineSearch() {
        // get the search manager and set the search result listener
		SKSearchManager searchManager = new SKSearchManager(this);
		// set the search topic
		String searchTerm = "Restaurant";
		SKOnelineSearchSettings onelineSearchSettings = new SKOnelineSearchSettings(searchTerm, SKSearchManager.SKSearchMode.OFFLINE);
		// set the position around which to do the search
		SKCoordinate pos = new SKCoordinate(37.756479, -122.432871);
		onelineSearchSettings.setGpsCoordinates(pos);
		// set the geocoder
		//onelineSearchSettings.setOnlineGeocoder(SKOnelineSearchSettings.SKGeocoderType.MAP_SEARCH_OSM);
		// set the maximum number of results
		onelineSearchSettings.setSearchResultsNumber(20);
		// the status value indicates if the search could be performed
		SKSearchStatus searchStatus = searchManager.onelineSearch(onelineSearchSettings);
	}*/

    @Override
    public void onReceivedSearchResults(List<SKSearchResult> list) {

    }

    private void prepareAndStartResearch() {

        // If atRequest != null then we're still processing previous request (dialog should automatically block UI, but we double check)
        //if (atRequest == null && validate()) {
        //navigateTo(searchSteps.getParent());

        //}

        if (mCustomKeyboard.isCustomKeyboardVisible()) {
            mCustomKeyboard.hideCustomKeyboard();
        }
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.fseaSearchB:

                if (mCustomKeyboard.isCustomKeyboardVisible()) {
                    mCustomKeyboard.hideCustomKeyboard();
                }

                prepareAndStartResearch();

                break;

            case R.id.fseaSOSB:
                startActivity(new Intent(getActivity(), ASOS.class));
                break;

            case R.id.fseaBackIB:
                ((ABase) getActivity()).popFragment();
                break;

            case R.id.btnSearch1:
                mCustomKeyboard.hideCustomKeyboard();
                String city = cityET.getText().toString();
                sks.SearchCity(city);
                break;

            case R.id.btnSearch2:
                String street = addressET.getText().toString();
                mCustomKeyboard.hideCustomKeyboard();
                sks.SearchStreet(street, searchSteps.getParentId());
                break;

            case R.id.btnSearch3:
                String number = numberET.getText().toString();
                mCustomKeyboard.hideCustomKeyboard();
                sks.SearchHouseNumber(number, searchSteps.getParentId());
                break;

            case R.id.fseaGotoBTN:
            case R.id.btnGo:
                //int i =3/0; crash test
                navigateTo(searchSteps.getParent());
                //SystemControl.doReboot(); DEVELOP ONLY!!!!
                break;

        }
    }

    private boolean validate() {

        if (addressET.getText().toString().trim().length() <= 0) {
            Toast.makeText(getActivity(), R.string.error_empty_address, Toast.LENGTH_LONG).show();
            return false;
        }

        if (cityET.getText().toString().trim().length() <= 0) {
            Toast.makeText(getActivity(), R.string.error_empty_city, Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    private void updateUI(String term) {
        int step = searchSteps.get();

        resultsRL.setVisibility(View.INVISIBLE);
        resultsLV.setVisibility(View.INVISIBLE);

        switch (step) {

            case SearchSteps.BEGIN:
            case SearchSteps.CITY:
                if (term != null) cityET.setText(term);

                llRow1.setVisibility(View.VISIBLE);
                llRow2.setVisibility(View.GONE);
                llRow3.setVisibility(View.GONE);

                btnSearch1.setVisibility(View.VISIBLE);
                btnSearch2.setVisibility(View.INVISIBLE);
                btnSearch3.setVisibility(View.INVISIBLE);
                break;

            case SearchSteps.STREET:
                if (term != null) addressET.setText(term);
                llRow1.setVisibility(View.VISIBLE);
                llRow2.setVisibility(View.VISIBLE);
                llRow3.setVisibility(View.GONE);

                btnSearch1.setVisibility(View.INVISIBLE);
                btnSearch2.setVisibility(View.VISIBLE);
                btnSearch3.setVisibility(View.INVISIBLE);
                break;

            case SearchSteps.HOUSE:

                llRow1.setVisibility(View.VISIBLE);
                llRow2.setVisibility(View.VISIBLE);
                llRow3.setVisibility(View.VISIBLE);

                btnSearch1.setVisibility(View.INVISIBLE);
                btnSearch2.setVisibility(View.INVISIBLE);
                btnSearch3.setVisibility(View.VISIBLE);
                break;

            case SearchSteps.DONE:
                llRow1.setVisibility(View.VISIBLE);
                llRow2.setVisibility(View.VISIBLE);
                llRow3.setVisibility(View.VISIBLE);

                btnSearch1.setVisibility(View.INVISIBLE);
                btnSearch2.setVisibility(View.INVISIBLE);
                btnSearch3.setVisibility(View.INVISIBLE);
                break;

        }
    }

    @Override
    public void onRowSelected(SKSearchResult location) {

        if (location.getType() == SKSearchResultType.POINT) {
            navigateTo(location);
        } else {
            updateUI(location.getName());
            searchSteps.next();
            searchSteps.setParent(location);
            updateUI(null);
        }
//		((ABase)getActivity()).popFragment();
    }

    @Override
    public synchronized void onEnterPushed() {
        prepareAndStartResearch();
    }

    public void onEditTextChange(long fieldId) {

        if (fieldId == R.id.fseaCityED) {
            if (searchSteps.get() != SearchSteps.CITY) {
                searchSteps.set(SearchSteps.CITY);
                updateUI(null);
            }

        } else if (fieldId == R.id.fseaAddressED) {
            if (searchSteps.get() != SearchSteps.STREET) {
                searchSteps.set(SearchSteps.STREET);
                updateUI(null);
            }
        } else if (fieldId == R.id.fseaHouseNumberED) {
            if (searchSteps.get() != SearchSteps.HOUSE) {
                searchSteps.set(SearchSteps.HOUSE);
                updateUI(null);
            }
        }

    }

    private void showAdapter(List<SKSearchResult> list) {
        if (list != null && list.size() > 0) {
            resultsRL.setVisibility(View.INVISIBLE);
            resultsLV.setVisibility(View.VISIBLE);
            adapter.setLocations(list);
            adapter.notifyDataSetChanged();
        } else {

            resultsLV.setVisibility(View.INVISIBLE);
            resultsRL.setVisibility(View.VISIBLE);
            switch (searchSteps.get()) {
                case SearchSteps.STREET:
                    gotoTV.setText(getResources().getText(R.string.gotoCity));
                    break;
                case SearchSteps.HOUSE:
                    gotoTV.setText(getResources().getText(R.string.gotoStreet));
                    break;
            }

        }

    }

    private void handleResult(List<SKSearchResult> list) {

        if (list == null || list.size() == 0)
            return;

        if (searchSteps.get() == SearchSteps.BEGIN) {
            updateUI(list.get(0).getName());
            searchSteps.set(SearchSteps.STREET);
            searchSteps.setParent(list.get(0));
            updateUI(null);
        } else {
            showAdapter(list);
        }

    }

    private Handler searchHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            List<SKSearchResult> list = (List<SKSearchResult>) msg.obj;

            long parent;

            switch (msg.what) {
                case 0:
                    sks.setHandler(this);
                    sks.SearchCity("Milano");
                    break;
                case SkobblerSearch.MSG_FOUND_CITY:
/*				 parent = list.get(0).getId();
				 cityET.setText(item.getName());
				 searchSteps.setParent(parent);
				 searchSteps.set(SearchSteps.STREET); 
				 updateUI();*/
                    showAdapter(list);
                    break;
                case SkobblerSearch.MSG_FOUND_STREET:
/*				 parent = list.get(0).getId();
				 addressET.setText(item.getName());
				 searchSteps.setParent(parent);
				 searchSteps.set(SearchSteps.HOUSE);
				 updateUI();*/
                    showAdapter(list);
                    break;
                case SkobblerSearch.MSG_FOUND_HOUSENUMBER:
                    SKSearchResult item = list.get(0);
                    numberET.setText(item.getName());
                    dlog.d("" + list.get(0).describeContents());
                    searchSteps.set(SearchSteps.DONE);
                    updateUI(null);
                    break;

                case SkobblerSearch.MSG_RAW:
                    handleResult(list);
                    break;

            }

        }
    };

    private void navigateTo(SKSearchResult result) {

        if (result != null) {
            dlog.d("navigateTo: set destination to " + result.toString());
            Location point = new Location("");
            point.setLatitude(result.getLocation().getLatitude());
            point.setLongitude(result.getLocation().getLongitude());
            ((AMainOBC) getActivity()).navigateTo(point);
            ((ABase) getActivity()).popFragment();
        }
    }

    private class SearchFieldsWatcher implements TextWatcher {

        private long fieldId;
        private FSearch parent;
        private long buttonId;
        private int type;

        public SearchFieldsWatcher(long fieldId, FSearch parent, long buttonId, int type) {
            this.fieldId = fieldId;
            this.parent = parent;
            this.buttonId = buttonId;
            this.type = type;

        }

        @Override
        public void afterTextChanged(Editable s) {
            parent.onEditTextChange(fieldId);
            if (mCustomKeyboard.isCustomKeyboardVisible()) {
                //rootView.findViewById((int) buttonId).performClick();
                EditText street = (EditText) rootView.findViewById((int) fieldId);
                //mCustomKeyboard.hideCustomKeyboard();
                switch (type) {
                    case 1:
                        sks.SearchCity(street.getText().toString());
                        break;
                    case 2:
                        sks.SearchStreet(street.getText().toString(), searchSteps.getParentId());
                        break;
                    case 3:
                        sks.SearchHouseNumber(street.getText().toString(), searchSteps.getParentId());
                        break;
                }
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

    }

    private class SearchSteps {

        public static final int BEGIN = 0;
        public static final int CITY = 1;
        public static final int STREET = 2;
        public static final int HOUSE = 3;
        public static final int DONE = 4;

        private int current = BEGIN;
        private SKSearchResult lastParent = null;
        private SKSearchResult[] parents = new SKSearchResult[4];

        public void set(int v) {
            if (v >= 0 && v <= 4) {
                current = v;
                lastParent = parents[v];
            }
        }

        public int get() {
            return current;
        }

        public SearchSteps next() {
            set(current + 1);
            return this;
        }

        public void setParent(SKSearchResult parent) {
            lastParent = parent;
            parents[current] = parent;
        }

        public SKSearchResult getParent() {
            return lastParent;
        }

        public long getParentId() {
            if (lastParent != null)
                return lastParent.getId();
            else
                return -1;
        }

        public void search(String term) {
            search(term, lastParent.getId());
        }

        public void search(String term, long parent) {
            switch (current) {
                case BEGIN:
                case CITY:
                    sks.SearchCity(term);
                    break;
                case STREET:
                    sks.SearchStreet(term, parent);
                    break;
                case HOUSE:
                    sks.SearchHouseNumber(term, parent);
                    break;
            }
        }

    }

    @Override
    public void onDestroy() {
        resultsLV = null;
        rootView = null;
        adapter = null;
        backIB = null;
        mCustomKeyboard = null;
        sosB = null;
        fpca_right_RL = null;
        searchB = null;
        gotoTV = null;
        btnGo = null;
        btnSearch1 = null;
        btnSearch1 = null;
        btnSearch3 = null;
        llRow1 = null;
        llRow2 = null;
        llRow3 = null;
        resultsRL = null;
        resultsTV = null;
        sks = null;
        searchHandler = null;
        super.onDestroy();
    }
}
