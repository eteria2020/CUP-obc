package eu.philcar.csg.OBC.controller.welcome;

import java.util.ArrayList;

import eu.philcar.csg.OBC.ABase;
import eu.philcar.csg.OBC.AGoodbye;
import eu.philcar.csg.OBC.ASOS;
import eu.philcar.csg.OBC.AWelcome;
import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.controller.FBase;
import eu.philcar.csg.OBC.controller.FNumber;
import eu.philcar.csg.OBC.controller.map.FMenu;
import eu.philcar.csg.OBC.controller.welcome.adapter.LADamages;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.service.MessageFactory;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class FDamages extends FBase implements OnClickListener, OnSeekBarChangeListener, OnScrollListener {

	private DLog dlog = new DLog(this.getClass());
	
	public static FDamages newInstance(boolean login) {
		
		FDamages fd = new FDamages();
		
		fd.login = login;
		
		return fd;
	}
	
	private LinearLayout firstLL, secondLL;
	
	private TextView damagesObservedTV, messageTV, numberTV;
	private ImageButton nextIB;
	private Button changeNumberB, sosB, dialCallB;
	private LinearLayout questionLL, newDamagesLL, callDealtLL;
	
	private VerticalSeekBar damagesVSB;
	private LADamages damagesA;
	private ListView damagesLV;
	
	private boolean editMode, login;
	private String customerCenterNumber;
	
	private int oldFirstItem = 0;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		View view = inflater.inflate(R.layout.f_damages, container, false);
		dlog.d("OnCreareView FDamages");
		
		firstLL = (LinearLayout)view.findViewById(R.id.fdam_first_LL);
		secondLL = (LinearLayout)view.findViewById(R.id.fdam_second_LL);
		
		firstLL.setVisibility(login ? View.VISIBLE : View.INVISIBLE);
		secondLL.setVisibility(login ? View.INVISIBLE : View.VISIBLE);
		
		callDealtLL = (LinearLayout)view.findViewById(R.id.fdam_call_reserved_LL);
		newDamagesLL = (LinearLayout)view.findViewById(R.id.fdam_number_LL);
		questionLL = (LinearLayout)view.findViewById(R.id.fdam_question_LL);
		
		Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "interstateregular.ttf");
				
		nextIB = (ImageButton)view.findViewById(R.id.fdamCloseIB);
		
		((Button)view.findViewById(R.id.fdamYesB)).setTypeface(font);
		((Button)view.findViewById(R.id.fdamYesB)).setOnClickListener(this);
		
		((Button)view.findViewById(R.id.fdamNoB)).setTypeface(font);
		((Button)view.findViewById(R.id.fdamNoB)).setOnClickListener(this);
		
		((Button)view.findViewById(R.id.fdamAlternativeYesB)).setTypeface(font);
		((Button)view.findViewById(R.id.fdamAlternativeYesB)).setOnClickListener(this);
		
		((Button)view.findViewById(R.id.fdamAlternativeNoB)).setTypeface(font);
		((Button)view.findViewById(R.id.fdamAlternativeNoB)).setOnClickListener(this);
		
		sosB = ((Button)view.findViewById(R.id.fdamSOSB));
		sosB.setOnClickListener(this);
		
		damagesObservedTV = (TextView)view.findViewById(R.id.fdamDamagesObservedTV);
		messageTV = (TextView)view.findViewById(R.id.fdamMessageTV);
		numberTV = (TextView)view.findViewById(R.id.fdamNumberTV);
		
		((TextView)view.findViewById(R.id.fdam_question_TV)).setTypeface(font);
		((TextView)view.findViewById(R.id.fdam_call_message_TV)).setTypeface(font);
		((TextView)view.findViewById(R.id.fdamNumberCallTV)).setTypeface(font);
		((TextView)view.findViewById(R.id.fdam_call_reserved_message_TV)).setTypeface(font);
				
		damagesObservedTV.setTypeface(font);
		messageTV.setTypeface(font);
		numberTV.setTypeface(font);
		
		dialCallB = (Button)view.findViewById(R.id.fdamCallB);
		changeNumberB = (Button)view.findViewById(R.id.fdamChangeNumberB);
		
		((Button)view.findViewById(R.id.fdam_call_reserved_close_B)).setTypeface(font);
		((Button)view.findViewById(R.id.fdam_call_reserved_close_B)).setOnClickListener(this);
		
		dialCallB.setTypeface(font);
		changeNumberB.setTypeface(font);
		
		dialCallB.setOnClickListener(this);
		changeNumberB.setOnClickListener(this);
		
		ArrayList<String> damages = App.Instance.getDamagesList();
		
		int damagesSize = damages.size();
		
		damagesA = new LADamages(getActivity(), damages);
		damagesLV = (ListView)view.findViewById(R.id.fdamDamagesLV); 
		damagesLV.setAdapter(damagesA);
		
		damagesVSB = (VerticalSeekBar)view.findViewById(R.id.fdamDamageListSB);
		
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
		
		numberTV.setText(customerCenterNumber);
		
		updateUI();
	}
	
	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {
		
		case R.id.fdamAlternativeNoB:
			dlog.d("FDamages fdamAlternativeNoB click : ");
			secondLL.setVisibility(View.INVISIBLE);
			//getActivity().finish();
			((ABase)getActivity()).pushFragment(FMenu.newInstance(), FMenu.class.getName(), true);
			
			break;
		
		case R.id.fdamNoB:
			
			questionLL.setVisibility(View.GONE);
			dlog.d("FDamages fdamNoB click : " + login);
			if (login) {
				((ABase)getActivity()).pushFragment(FInstructions.newInstance(true), FInstructions.class.getName(), true);
			} else {
				((ABase)getActivity()).pushFragment(FMenu.newInstance(), FMenu.class.getName(), true);
				//getActivity().finish();
			}
			
			break;
			
		case R.id.fdamAlternativeYesB:
			firstLL.setVisibility(View.VISIBLE);
			secondLL.setVisibility(View.INVISIBLE);
			editMode = true;
			updateUI();
			break;
			
		case R.id.fdamYesB:
			editMode = true;
			updateUI();
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
						((ABase)getActivity()).pushFragment(FMenu.newInstance(), FMenu.class.getName(), true);
					}
				} else {
					editMode = false;
					updateUI();
				}
			} else {
				dlog.d("FDamages fdamCloseIB click : " + login);
				if (login) {
					((ABase)getActivity()).pushFragment(FInstructions.newInstance(true), FInstructions.class.getName(), true);
				} else {
					((ABase)getActivity()).pushFragment(FMenu.newInstance(), FMenu.class.getName(), true);
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
			
			numberTV.setText(customerCenterNumber);
			
		} else {
			
			questionLL.setVisibility(View.VISIBLE);
			newDamagesLL.setVisibility(View.GONE);
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
		
		int listPosition = damagesA.getCount() - LADamages.ADDITIONAL_ITEMS_PER_LIST - progress - 1;
		damagesLV.setSelection(listPosition);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		
		if (oldFirstItem != firstVisibleItem) {
			oldFirstItem = firstVisibleItem;
			damagesVSB.setProgress(damagesA.getCount()- LADamages.ADDITIONAL_ITEMS_PER_LIST - 1 - firstVisibleItem);
			damagesVSB.updateThumb();
		}
	}
}
