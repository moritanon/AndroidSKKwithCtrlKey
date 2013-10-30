package jp.deadend.noname.skk;

import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.view.KeyEvent;
import android.util.AttributeSet;

import static jp.deadend.noname.skk.InputMode.*;

public class QwertyKeyboardView extends KeyboardView implements KeyboardView.OnKeyboardActionListener {
	private static final int KEYCODE_QWERTY_TOJP	= -1008;
	private static final int KEYCODE_QWERTY_TOSYM	= -1009;
	private static final int KEYCODE_QWERTY_TOLATIN	= -1010;
	private static final int KEYCODE_QWERTY_ENTER	= -1011;

	private SKKEngine mService;

	private Keyboard mLatinKeyboard;
	private Keyboard mSymbolsKeyboard;
	private Keyboard mSymbolsShiftedKeyboard;

	public QwertyKeyboardView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setup(context);
	}

	public QwertyKeyboardView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setup(context);
	}

	private void setup(Context context) {
		int keyHeight = SKKPrefs.getKeyHeight(context);
		mLatinKeyboard = new SKKKeyboard(context, R.xml.qwerty, keyHeight);
		mSymbolsKeyboard = new SKKKeyboard(context, R.xml.symbols, keyHeight);
		mSymbolsShiftedKeyboard = new SKKKeyboard(context, R.xml.symbols_shift, keyHeight);
		setKeyboard(mLatinKeyboard);
		setOnKeyboardActionListener(this);
	}

	public void setService(SKKEngine listener) {
		mService = listener;
	}

	@Override
	protected boolean onLongPress (Keyboard.Key key) {
		if (key.codes[0] == KEYCODE_QWERTY_ENTER) {
			mService.keyDownUp(KeyEvent.KEYCODE_SEARCH);
			return true;
		}

		return super.onLongPress(key);
	}

	@Override
	public void onKey(int primaryCode, int[] keyCodes) {
		if (primaryCode == Keyboard.KEYCODE_DELETE) {
			mService.handleCancel(true);
        } else if (primaryCode == Keyboard.KEYCODE_SHIFT) {
			setShifted(!isShifted());
			Keyboard cur_keyboard = getKeyboard();
			if (cur_keyboard == mSymbolsKeyboard) {
				mSymbolsKeyboard.setShifted(true);
				setKeyboard(mSymbolsShiftedKeyboard);
				mSymbolsShiftedKeyboard.setShifted(true);
			} else if (cur_keyboard == mSymbolsShiftedKeyboard) {
				mSymbolsShiftedKeyboard.setShifted(false);
				setKeyboard(mSymbolsKeyboard);
				mSymbolsKeyboard.setShifted(false);
			}
		} else if (primaryCode == KEYCODE_QWERTY_ENTER) {
			mService.keyDownUp(KeyEvent.KEYCODE_ENTER);
		} else if (primaryCode == KEYCODE_QWERTY_TOJP) {
			mService.toggleSKK();
		} else if (primaryCode == KEYCODE_QWERTY_TOSYM) {
			setKeyboard(mSymbolsKeyboard);
		} else if (primaryCode == KEYCODE_QWERTY_TOLATIN) {
			setKeyboard(mLatinKeyboard);
		} else {
			handleCharacter(primaryCode, keyCodes);
		}
	}

	private void handleCharacter(int primaryCode, int[] keyCodes) {
		if (isShifted()) {
			primaryCode = Character.toUpperCase(primaryCode);
		}

		mService.sendText(String.valueOf((char) primaryCode));
    }

	public void onPress(int primaryCode) {
	}

    public void onRelease(int primaryCode) {
	}

	public void onText(CharSequence text) {
	}

	public void swipeRight() {
	}

	public void swipeLeft() {
	}

	public void swipeDown() {
	}

	public void swipeUp() {
	}

}