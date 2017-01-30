package eu.philcar.csg.OBC.controller.welcome;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.NetworkOnMainThreadException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import eu.philcar.csg.OBC.ABase;
import eu.philcar.csg.OBC.AGoodbye;
import eu.philcar.csg.OBC.ASOS;
import eu.philcar.csg.OBC.AWelcome;
import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.controller.FBase;
import eu.philcar.csg.OBC.controller.FNumber;
import eu.philcar.csg.OBC.controller.welcome.adapter.LADamages;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.service.MessageFactory;

public class FDamagesNew extends FBase implements OnClickListener, OnSeekBarChangeListener, OnScrollListener {

    private DLog dlog = new DLog(this.getClass());

    public static FDamagesNew newInstance(boolean login) {

        FDamagesNew fd = new FDamagesNew();

        fd.login = login;

        return fd;
    }

    private LinearLayout firstLL;

    private TextView damagesObservedTV, messageTV, newmessageTV, newdamagesObservedTV, numberTV;
    private ImageButton nextIB;
    private Button changeNumberB, sosB, dialCallB;
    private LinearLayout questionLL, newDamagesLL, callDealtLL, fnewDamageLL;


    private VerticalSeekBar damagesVSB, newdamagesVSB;
    private LADamages damagesA, newdamagesA;
    private ListView damagesLV, newdamagesLV;

    private boolean  login;
    private static boolean editMode;
    private String customerCenterNumber;
    public String jsonStr= "";
    private boolean JsonEND=false;

    private ArrayList<String> damages, newdamages;

    private int oldFirstItem = 0, oldFirstItemNew = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.f_damagesalternative, container, false);
        dlog.d("OnCreareView FDamages_new");
        newdamages = new ArrayList<String>();

        new Thread(new Runnable() {
            public void run() {
                handleJson("http://stats.sharengo.it/maintenance/danni_macchina.php");
            }
        }).start();

        firstLL = (LinearLayout)view.findViewById(R.id.fdam_first_LL);

        firstLL.setVisibility(View.VISIBLE);// ? View.VISIBLE : View.INVISIBLE);

        callDealtLL = (LinearLayout)view.findViewById(R.id.fdam_call_reserved_LL);
        newDamagesLL = (LinearLayout)view.findViewById(R.id.fdam_number_LL);
        fnewDamageLL = (LinearLayout)view.findViewById(R.id.fnewdamDamagesLL);
        questionLL = (LinearLayout)view.findViewById(R.id.fdam_question_LL);

        Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "interstateregular.ttf");

        nextIB = (ImageButton)view.findViewById(R.id.fdamCloseIB);

        ((Button)view.findViewById(R.id.fdamYesB)).setTypeface(font);
        ((Button)view.findViewById(R.id.fdamYesB)).setOnClickListener(this);

        ((Button)view.findViewById(R.id.fdamNoB)).setTypeface(font);
        ((Button)view.findViewById(R.id.fdamNoB)).setOnClickListener(this);

        sosB = ((Button)view.findViewById(R.id.fdamSOSB));
        sosB.setOnClickListener(this);

        damagesObservedTV = (TextView)view.findViewById(R.id.fdamDamagesObservedTV);
        newdamagesObservedTV = (TextView)view.findViewById(R.id.fnewDamagesObservedTV);
        messageTV = (TextView)view.findViewById(R.id.fdamMessageTV);
        newmessageTV = (TextView)view.findViewById(R.id.fnewDamMessageTV);
        numberTV = (TextView)view.findViewById(R.id.fdamNumberTV);

        ((TextView)view.findViewById(R.id.fdam_question_TV)).setTypeface(font);
        ((TextView)view.findViewById(R.id.fdam_call_message_TV)).setTypeface(font);
        ((TextView)view.findViewById(R.id.fdamNumberCallTV)).setTypeface(font);
        ((TextView)view.findViewById(R.id.fdam_call_reserved_message_TV)).setTypeface(font);

        damagesObservedTV.setTypeface(font);
        newdamagesObservedTV.setTypeface(font);
        messageTV.setTypeface(font);
        newmessageTV.setTypeface(font);
        numberTV.setTypeface(font);

        dialCallB = (Button)view.findViewById(R.id.fdamCallB);
        changeNumberB = (Button)view.findViewById(R.id.fdamChangeNumberB);

        ((Button)view.findViewById(R.id.fdam_call_reserved_close_B)).setTypeface(font);
        ((Button)view.findViewById(R.id.fdam_call_reserved_close_B)).setOnClickListener(this);

        dialCallB.setTypeface(font);
        changeNumberB.setTypeface(font);

        dialCallB.setOnClickListener(this);
        changeNumberB.setOnClickListener(this);

         damages = App.Instance.getDamagesList();

        int damagesSize = damages.size();

        damagesA = new LADamages(getActivity(), damages);
        damagesLV = (ListView)view.findViewById(R.id.fdamDamagesLV);
        damagesLV.setAdapter(damagesA);

          newdamagesA = new LADamages(getActivity(), newdamages);
          newdamagesLV = (ListView)view.findViewById(R.id.fnewdamDamagesLV);
           newdamagesLV.setAdapter(newdamagesA);

        damagesVSB = (VerticalSeekBar)view.findViewById(R.id.fdamDamageListSB);
        newdamagesVSB = (VerticalSeekBar)view.findViewById(R.id.fnewDamageListSB);

        damagesVSB.setMax(damagesSize-1 > 0 ? damagesSize-1 : 1);
        damagesVSB.setProgress(damagesSize-1 > 0 ? damagesSize-1 : 1);

        if (damagesSize-1 > 0) {
            damagesLV.setOnScrollListener(this);
            damagesVSB.setEnabled(true);
            damagesVSB.setOnSeekBarChangeListener(this);
        } else {
            damagesLV.setOnScrollListener(null);
            damagesVSB.setEnabled(false);
            damagesVSB.setOnSeekBarChangeListener(null);
        }

        newdamagesLV.setOnScrollListener(this);
        newdamagesVSB.setEnabled(true);
        newdamagesVSB.setOnSeekBarChangeListener(this);

        newdamagesLV.setClickable(true);
        newdamagesLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                Object o = newdamagesLV.getItemAtPosition(position);
                int i=0;
                String str=(String)o;//As you are using Default String Adapter
                switch (str) {
                    case "Parabrezza":
                        i=99+22-i;
                        return;
                }
            }
        });




        damagesObservedTV.setTypeface(font);

        if (damagesSize == 1) {
            damagesObservedTV.setText(String.valueOf(damagesSize) + " " + getResources().getString(R.string.damage_observed));
        } else {
            damagesObservedTV.setText(String.valueOf(damagesSize) + " " + getResources().getString(R.string.damages_observed));
        }

        return view;




    }

    @Override
    public void onResume() {

        super.onResume();

        if (App.currentTripInfo!=null && App.currentTripInfo.customer!=null) {
            customerCenterNumber = App.currentTripInfo.customer.mobile;
        } else {
            customerCenterNumber = "";
        }


        updateUINew();
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.fdamNoB:

                questionLL.setVisibility(View.GONE);
                dlog.d("FDamagesNew fdamNoB click : " + login);
                if (login) {
                    ((ABase)getActivity()).pushFragment(FInstructions.newInstance(true), FInstructions.class.getName(), true);
                } else {
                    //((ABase)getActivity()).pushFragment(new FGoodbye(), FGoodbye.class.getName(), true);
                    getActivity().finish();
                }

                break;



            case R.id.fdamYesB:
                editMode = true;
                updateUINew();
                break;

            case R.id.fdamCloseIB:
                if (editMode) {
                    if (callDealtLL.getVisibility()==View.VISIBLE) {
                        questionLL.setVisibility(View.GONE);
                        newDamagesLL.setVisibility(View.GONE);
                        callDealtLL.setVisibility(View.GONE);

                        nextIB.setImageResource(R.drawable.sel_button_next);
                    } else if (newDamagesLL.getVisibility()==View.GONE) {
                        if (login) {
                            ((ABase)getActivity()).pushFragment(FInstructions.newInstance(true), FInstructions.class.getName(), true);
                        } else {
                            getActivity().finish();
                            //((ABase)getActivity()).pushFragment(FInstructions.newInstance(false), FInstructions.class.getName(), true);
                        }
                    } else {
                        editMode = false;
                        updateUINew();
                    }
                } else {
                    dlog.d("FDamagesNew fdamCloseIB click : " + login);
                    if (login) {
                        ((ABase)getActivity()).pushFragment(FInstructions.newInstance(true), FInstructions.class.getName(), true);
                    } else {
                        ((ABase)getActivity()).pushFragment(new FGoodbye(), FGoodbye.class.getName(), true);
                    }
                }
                break;

            case R.id.fdamSOSB:
                startActivity(new Intent(getActivity(), ASOS.class));
                break;

            case R.id.fdamChangeNumberB:
                ((ABase)getActivity()).pushFragment(FNumber.newInstance(), FNumber.class.getName(), true);
                break;

            case R.id.fdamCallB:

                if (login) {
                    ((AWelcome)getActivity()).sendMessage(MessageFactory.requestCallCenterCall(customerCenterNumber));
                } else {
                    ((AGoodbye)getActivity()).sendMessage(MessageFactory.requestCallCenterCall(customerCenterNumber));
                }

                questionLL.setVisibility(View.GONE);
                newDamagesLL.setVisibility(View.GONE);
                callDealtLL.setVisibility(View.VISIBLE);

                break;

            case R.id.fdam_call_reserved_close_B:

                questionLL.setVisibility(View.GONE);
                newDamagesLL.setVisibility(View.GONE);
                callDealtLL.setVisibility(View.GONE);

                nextIB.setImageResource(R.drawable.sel_button_next);

                break;
        }
    }

    private void updateUI() {

        if (editMode) {
            questionLL.setVisibility(View.GONE);
            newDamagesLL.setVisibility(View.VISIBLE);
            callDealtLL.setVisibility(View.GONE);
            fnewDamageLL.setVisibility(View.GONE);

            messageTV.setText(R.string.call_book);
            nextIB.setImageResource(R.drawable.sel_button_cancel);
            nextIB.setVisibility(View.VISIBLE);
            nextIB.setOnClickListener(this);
            sosB.setVisibility(View.GONE);

            if (App.currentTripInfo!=null && App.currentTripInfo.customer!=null) {
                customerCenterNumber = App.currentTripInfo.customer.mobile;
            } else {
                customerCenterNumber = "";
            }


        } else {

            questionLL.setVisibility(View.VISIBLE);
            newDamagesLL.setVisibility(View.GONE);
            callDealtLL.setVisibility(View.GONE);
            fnewDamageLL.setVisibility(View.GONE);

            messageTV.setText(R.string.new_damage);

            nextIB.setVisibility(View.GONE);
            sosB.setVisibility(View.VISIBLE);
        }
    }

    private void updateUINew() {

        if (editMode) {
            questionLL.setVisibility(View.GONE);
            newDamagesLL.setVisibility(View.GONE);
            callDealtLL.setVisibility(View.GONE);
            fnewDamageLL.setVisibility(View.VISIBLE);
            nextIB.setVisibility(View.VISIBLE);
            messageTV.setVisibility(View.INVISIBLE);
            nextIB.setOnClickListener(this);



        } else {

            questionLL.setVisibility(View.VISIBLE);
            newDamagesLL.setVisibility(View.GONE);
            fnewDamageLL.setVisibility(View.GONE);
            callDealtLL.setVisibility(View.GONE);

            messageTV.setText(R.string.new_damage);

            nextIB.setVisibility(View.GONE);
            sosB.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean handleBackButton() {

        return true;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        if(seekBar.getId() == R.id.fnewDamageListSB){
            int listPosition = newdamagesA.getCount() - LADamages.ADDITIONAL_ITEMS_PER_LIST - progress - 1;
            newdamagesLV.setSelection(listPosition);
        }
        else {
            int listPosition = damagesA.getCount() - LADamages.ADDITIONAL_ITEMS_PER_LIST - progress - 1;
            damagesLV.setSelection(listPosition);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {}

   @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

        switch (view.getId()){

        case  R.id.fnewdamDamagesLV:
            if (oldFirstItemNew != firstVisibleItem) {

                newdamagesVSB.setProgress(newdamagesA.getCount() - LADamages.ADDITIONAL_ITEMS_PER_LIST - 1 - firstVisibleItem);
                newdamagesVSB.updateThumb();
                oldFirstItemNew = firstVisibleItem;
            }
             break;

        case R.id.fdamDamagesLV:

            if (oldFirstItem != firstVisibleItem) {

                damagesVSB.setProgress(damagesA.getCount() - LADamages.ADDITIONAL_ITEMS_PER_LIST - 1 - firstVisibleItem);
                damagesVSB.updateThumb();
                oldFirstItem = firstVisibleItem;
            }
            break;
        }
    }




    private void handleJson(String urls) {

        if(newdamages.size()>1 || JsonEND)
            return;

        JsonEND=true;
        StringBuilder builder = new StringBuilder();
        HttpClient client = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(urls);
        try {
            HttpResponse response = client.execute(httpGet);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if (statusCode == 200) {
                HttpEntity entity = response.getEntity();
                InputStream content = entity.getContent();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(content));
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                reader.close();
                content.close();
            } else {
                JsonEND=false;
                Log.e(FDamagesNew.class.toString(), "Failed to download file");
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (NetworkOnMainThreadException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        jsonStr=builder.toString();
        parseJson();
        (getActivity()).runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // your stuff to update the UI
                try{
                    newdamagesA = new LADamages(getActivity(),newdamages);
                    newdamagesLV.setAdapter(newdamagesA);

                    newdamagesVSB.setEnabled(true);


                    newdamagesVSB.setMax(newdamages.size()-1 > 0 ? newdamages.size()-1 : 1);
                    newdamagesVSB.setProgress(newdamages.size()-1 > 0 ? newdamages.size()-1 : 1);
                    newdamagesLV.invalidate();

                }
                catch (Exception e){

                }
            }
        });



        return ;

    }



    private void parseJson() {

        try {
            JSONObject json = new JSONObject(jsonStr);
            Log.i(FDamagesNew.class.getName(), "creazione oggetto Json");
            //Get the instance of JSONArray that contains JSONObjects
           JSONArray jsonArray = json.optJSONArray("Danni");

            //Iterate the jsonArray and print the info of JSONObjects
            for(int k=0;k<jsonArray.length();k++) {
                JSONObject jsonObject = jsonArray.getJSONObject(k);

                newdamages.add(jsonObject.getString("name"));

            }
        } catch (JSONException e) {
            e.printStackTrace();}

    }



}
