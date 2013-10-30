package jp.deadend.noname.skk;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnKeyListener;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SetKeyPreference extends DialogPreference implements OnKeyListener {
	private int mKeyCode;
	private TextView mTextView;
	private static final int DEFAULT_VALUE = KeyEvent.KEYCODE_UNKNOWN;
	private static final String FORMAT = "  Key: %s\n  Code: %d";
	private final int FP = ViewGroup.LayoutParams.FILL_PARENT;
	private final int WC = ViewGroup.LayoutParams.WRAP_CONTENT;

	public SetKeyPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SetKeyPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	int getValue(int defaultValue) {
		return getPersistedInt(defaultValue);
	}

	@Override
	protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
		if (restorePersistedValue) {
			mKeyCode = this.getPersistedInt(DEFAULT_VALUE);
		} else {
			mKeyCode = (Integer)defaultValue;
		}
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getInteger(index, DEFAULT_VALUE);
	}

	@Override
	protected View onCreateDialogView() {
		Context ctx = this.getContext();
		LinearLayout linearLayout = new LinearLayout(ctx);
		linearLayout.setOrientation(LinearLayout.HORIZONTAL);

		mTextView = new TextView(ctx);
		mTextView.setTextSize(25);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, WC);
		lp.weight=1;
		linearLayout.addView(mTextView, lp);

		ImageButton button = new ImageButton(ctx);
		button.setImageResource(android.R.drawable.ic_delete);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mKeyCode = DEFAULT_VALUE;
				mTextView.setText(String.format(FORMAT, getKeyName(mKeyCode), mKeyCode));
			}
		});
		button.setFocusable(false);
		linearLayout.addView(button, new LinearLayout.LayoutParams(WC, FP));

		return linearLayout;
	}

	@Override
	protected void showDialog(Bundle state) {
		super.showDialog(state);
		getDialog().setOnKeyListener(this);
		getDialog().takeKeyEvents(true);
		mKeyCode = this.getPersistedInt(DEFAULT_VALUE);
		mTextView.setText(String.format(FORMAT, getKeyName(mKeyCode), mKeyCode));
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);

		if (positiveResult) {
			if (callChangeListener(mKeyCode)) {
				persistInt(mKeyCode);
			}
		}
	}

	@Override
	public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
		SKKUtils.dlog("keycode " + keyCode);
		SKKUtils.dlog("e");
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
		case KeyEvent.KEYCODE_ENTER:
			return false;
		case KeyEvent.KEYCODE_HOME:
			return true;
		default:
			mKeyCode = keyCode;
			if (event.isCtrlPressed()) {
				mKeyCode |= 0x100000;
			}
			mTextView.setText(String.format(FORMAT, getKeyName(mKeyCode), mKeyCode));
			return true;
		}
	}

	static String getKeyName(int keyCode) {
		StringBuffer sb = new StringBuffer("");
		if ((keyCode & 0x100000) == 0x100000 ) { 
			sb.append("Ctrl + ");
			keyCode &= ~0x100000;
		}
		return sb.append(getCharName(keyCode)).toString();
	}
	
	static String getCharName(int CharCode) {
		

		
		switch (CharCode) {
		case KeyEvent.KEYCODE_A: return "A";
		case KeyEvent.KEYCODE_B: return "B";
		case KeyEvent.KEYCODE_C: return "C";
		case KeyEvent.KEYCODE_D: return "D";
		case KeyEvent.KEYCODE_E: return "E";
		case KeyEvent.KEYCODE_F: return "F";
		case KeyEvent.KEYCODE_G: return "G";
		case KeyEvent.KEYCODE_H: return "H";
		case KeyEvent.KEYCODE_I: return "I";
		case KeyEvent.KEYCODE_J: return "J";
		case KeyEvent.KEYCODE_K: return "K";
		case KeyEvent.KEYCODE_L: return "L";
		case KeyEvent.KEYCODE_M: return "M";
		case KeyEvent.KEYCODE_N: return "N";
		case KeyEvent.KEYCODE_O: return "O";
		case KeyEvent.KEYCODE_P: return "P";
		case KeyEvent.KEYCODE_Q: return "Q";
		case KeyEvent.KEYCODE_R: return "R";
		case KeyEvent.KEYCODE_S: return "S";
		case KeyEvent.KEYCODE_T: return "T";
		case KeyEvent.KEYCODE_U: return "U";
		case KeyEvent.KEYCODE_V: return "V";
		case KeyEvent.KEYCODE_W: return "W";
		case KeyEvent.KEYCODE_X: return "X";
		case KeyEvent.KEYCODE_Y: return "Y";
		case KeyEvent.KEYCODE_Z: return "Z";

		case KeyEvent.KEYCODE_0: return "0";
		case KeyEvent.KEYCODE_1: return "1";
		case KeyEvent.KEYCODE_2: return "2";
		case KeyEvent.KEYCODE_3: return "3";
		case KeyEvent.KEYCODE_4: return "4";
		case KeyEvent.KEYCODE_5: return "5";
		case KeyEvent.KEYCODE_6: return "6";
		case KeyEvent.KEYCODE_7: return "7";
		case KeyEvent.KEYCODE_8: return "8";
		case KeyEvent.KEYCODE_9: return "9";

		case KeyEvent.KEYCODE_ALT_LEFT:		return "ALT (left)";
		case KeyEvent.KEYCODE_ALT_RIGHT:	return "ALT (right)";
		case KeyEvent.KEYCODE_SHIFT_LEFT:	return "SHIFT (left)";
		case KeyEvent.KEYCODE_SHIFT_RIGHT:	return "SHIFT (right)";
		case KeyEvent.KEYCODE_NUM:			return "NUM";
		case KeyEvent.KEYCODE_SYM:			return "SYM";
		case KeyEvent.KEYCODE_SPACE:		return "SPACE";
		case KeyEvent.KEYCODE_DEL:			return "DEL";
		case KeyEvent.KEYCODE_ENTER:		return "ENTER";
		case KeyEvent.KEYCODE_TAB:			return "TAB";
		case KeyEvent.KEYCODE_AT:			return "@";
		case KeyEvent.KEYCODE_PERIOD:		return ".";
		case KeyEvent.KEYCODE_COMMA:		return ",";
		case KeyEvent.KEYCODE_APOSTROPHE:	return "'";
		case KeyEvent.KEYCODE_EQUALS:		return "=";
		case KeyEvent.KEYCODE_GRAVE:		return "`";
		case KeyEvent.KEYCODE_MINUS:		return "-";
		case KeyEvent.KEYCODE_PLUS:			return "+";
		case KeyEvent.KEYCODE_SEMICOLON:	return ";";
		case KeyEvent.KEYCODE_SLASH:		return "/";
		case KeyEvent.KEYCODE_STAR:			return "*";

		case KeyEvent.KEYCODE_DPAD_CENTER:	return "DPAD CENTER";
		case KeyEvent.KEYCODE_DPAD_DOWN:	return "DPAD DOWN";
		case KeyEvent.KEYCODE_DPAD_LEFT:	return "DPAD LEFT";
		case KeyEvent.KEYCODE_DPAD_RIGHT:	return "DPAD RIGHT";
		case KeyEvent.KEYCODE_DPAD_UP:		return "DPAD UP";
		case KeyEvent.KEYCODE_MENU:			return "MENU";
		case KeyEvent.KEYCODE_BACK:			return "BACK";
		case KeyEvent.KEYCODE_CALL:			return "CALL";
		case KeyEvent.KEYCODE_ENDCALL:		return "ENDCALL";
		case KeyEvent.KEYCODE_CAMERA:		return "CAMERA";
		case KeyEvent.KEYCODE_FOCUS:		return "FOCUS";
		case KeyEvent.KEYCODE_SEARCH:		return "SEARCH";
		case KeyEvent.KEYCODE_VOLUME_UP:	return "Volume UP";
		case KeyEvent.KEYCODE_VOLUME_DOWN:	return "Volume DOWN";

		case KeyEvent.KEYCODE_UNKNOWN: return "Not set";
		default: return "Unknown";
		}
	}
}
