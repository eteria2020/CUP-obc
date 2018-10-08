package eu.philcar.csg.OBC.controller.map;

import android.app.Activity;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.text.Editable;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import eu.philcar.csg.OBC.R;

public class CustomKeyboard {

    public interface CustomKeyboardDelegate {
        void onEnterPushed();
    }

    private KeyboardView mKeyboardView;
    private Activity mHostActivity;
    private CustomKeyboardDelegate delegate;
    private static boolean caps = false;
    private static boolean num = false;

    public CustomKeyboard(Activity host, View view, int viewId, int layoutId, CustomKeyboardDelegate delegate) {

        this.delegate = delegate;

        mHostActivity = host;

        Keyboard mKeyboard = new Keyboard(host, layoutId);

        // Lookup the KeyboardView
        mKeyboardView = (KeyboardView) view.findViewById(viewId);

        // Attach the keyboard to the view
        mKeyboardView.setKeyboard(mKeyboard);

        // Install the key handler
        mKeyboardView.setOnKeyboardActionListener(mOnKeyboardActionListener);

        // Hide the standard keyboard initially
        host.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    public boolean isCustomKeyboardVisible() {
        return mKeyboardView.getVisibility() == View.VISIBLE;
    }

    public void showCustomKeyboard(View view) {
        mKeyboardView.setVisibility(View.VISIBLE);
        mKeyboardView.setEnabled(true);
        if (view != null)
            ((InputMethodManager) mHostActivity.getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void hideCustomKeyboard() {
        mKeyboardView.setVisibility(View.GONE);
        mKeyboardView.setEnabled(false);
    }

    public void registerEditText(EditText editText) {

        // Make the custom keyboard appear
        editText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) showCustomKeyboard(v);
                else hideCustomKeyboard();
            }
        });
        editText.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showCustomKeyboard(v);
            }
        });
        // Disable standard keyboard hard way
        editText.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                EditText edittext = (EditText) v;
                int inType = edittext.getInputType();       // Backup the input type
                edittext.setInputType(InputType.TYPE_NULL); // Disable standard keyboard
                edittext.onTouchEvent(event);               // Call native handler
                edittext.setInputType(inType);              // Restore input type
                return true; // Consume touch event
            }
        });
        // Disable spell check (hex strings look like words to Android)
        editText.setInputType(editText.getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    }

    private OnKeyboardActionListener mOnKeyboardActionListener = new OnKeyboardActionListener() {

        @Override
        public void onKey(int primaryCode, int[] keyCodes) {

            View focusCurrent = mHostActivity.getWindow().getCurrentFocus();

            if (focusCurrent == null || focusCurrent.getClass() != EditText.class) return;

            EditText edittext = (EditText) focusCurrent;
            Editable editable = edittext.getText();

            int start = edittext.getSelectionStart();
            switch (primaryCode) {
                case Keyboard.KEYCODE_DELETE:
                    if (editable != null && start > 0) editable.delete(start - 1, start);
                    break;
                case Keyboard.KEYCODE_MODE_CHANGE:
                    num = !num;
                    if (num) {
                        Keyboard mKeyboard = new Keyboard(mHostActivity, R.xml.hexkbd_num);
                        mKeyboardView.setKeyboard(mKeyboard);
                        mKeyboardView.invalidateAllKeys();
                    } else {
                        Keyboard mKeyboard = new Keyboard(mHostActivity, R.xml.hexkbd);
                        mKeyboardView.setKeyboard(mKeyboard);
                        mKeyboardView.invalidateAllKeys();
                    }

                    break;
                case Keyboard.KEYCODE_SHIFT:
                    caps = !caps;
                    mKeyboardView.getKeyboard().setShifted(caps);
                    mKeyboardView.invalidateAllKeys();
                    break;
                case Keyboard.KEYCODE_DONE:
                    hideCustomKeyboard();
                    if (delegate != null) {
                        delegate.onEnterPushed();
                    }
                    break;
                default:
                    char code = (char) primaryCode;
                    if (Character.isLetter(code) && caps) {
                        code = Character.toUpperCase(code);
                    }
                    editable.insert(start, String.valueOf(code));
            }/*
            if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_enter)) {
		    	hideCustomKeyboard();
		    	if (delegate != null) {
		    		delegate.onEnterPushed();
		    	}
		    } else if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_backspace)) {
		    	if( editable!=null && start>0 ) editable.delete(start - 1, start);
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_close)) {
		    	hideCustomKeyboard();
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_space)) {
		    	editable.insert(start, " ");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_slash)) {
		    	editable.insert(start, "\\");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_one)) {
		    	editable.insert(start, "1");
		    } else if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_two)) {
		    	editable.insert(start, "2");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_three)) {
		    	editable.insert(start, "3");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_four)) {
		    	editable.insert(start, "4");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_five)) {
		    	editable.insert(start, "5");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_six)) {
		    	editable.insert(start, "6");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_seven)) {
		    	editable.insert(start, "7");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_eight)) {
		    	editable.insert(start, "8");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_nine)) {
		    	editable.insert(start, "9");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_zero)) {
		    	editable.insert(start, "0");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_apostrophe)) {
		    	editable.insert(start, "'");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_i_acute)) {
		    	editable.insert(start, "ì");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_q)) {
		    	editable.insert(start, "q");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_w)) {
		    	editable.insert(start, "w");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_e)) {
		    	editable.insert(start, "e");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_r)) {
		    	editable.insert(start, "r");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_t)) {
		    	editable.insert(start, "t");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_y)) {
		    	editable.insert(start, "y");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_u)) {
		    	editable.insert(start, "u");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_i)) {
		    	editable.insert(start, "i");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_o)) {
		    	editable.insert(start, "o");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_p)) {
		    	editable.insert(start, "p");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_e_acute_a)) {
		    	editable.insert(start, "è");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_e_acute_b)) {
		    	editable.insert(start, "é");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_a)) {
		    	editable.insert(start, "a");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_s)) {
		    	editable.insert(start, "s");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_d)) {
		    	editable.insert(start, "d");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_f)) {
		    	editable.insert(start, "f");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_g)) {
		    	editable.insert(start, "g");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_h)) {
		    	editable.insert(start, "h");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_j)) {
		    	editable.insert(start, "j");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_k)) {
		    	editable.insert(start, "k");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_l)) {
		    	editable.insert(start, "l");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_o_acute)) {
		    	editable.insert(start, "ò");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_a_acute)) {
		    	editable.insert(start, "à");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_u_acute)) {
		    	editable.insert(start, "ù");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_z)) {
		    	editable.insert(start, "z");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_x)) {
		    	editable.insert(start, "x");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_c)) {
		    	editable.insert(start, "c");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_v)) {
		    	editable.insert(start, "v");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_b)) {
		    	editable.insert(start, "b");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_n)) {
		    	editable.insert(start, "n");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_m)) {
		    	editable.insert(start, "m");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_coma)) {
		    	editable.insert(start, ",");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_dot)) {
		    	editable.insert(start, ".");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_minus)) {
		    	editable.insert(start, "-");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_less_than)) {
		    	editable.insert(start, "<");
		    } else  if (primaryCode == mHostActivity.getResources().getInteger(R.integer.key_greater_than)) {
		    	editable.insert(start, ">");
		    } else  if (primaryCode == -1) {
				mKeyboardView.getKeyboard().setShifted(caps);
				mKeyboardView.invalidateAllKeys();

			}/*else if( primaryCode == CodeClear ) {
		        if( editable!=null ) editable.clear();
		    } */
        }

        @Override
        public void onPress(int primaryCode) {
        }

        @Override
        public void onRelease(int primaryCode) {
        }

        @Override
        public void onText(CharSequence text) {
        }

        @Override
        public void swipeLeft() {
        }

        @Override
        public void swipeRight() {
        }

        @Override
        public void swipeDown() {
        }

        @Override
        public void swipeUp() {
        }
    };

}
