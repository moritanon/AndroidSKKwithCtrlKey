package jp.deadend.noname.skk;

import android.inputmethodservice.InputMethodService;
import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;

import java.util.EnumMap;

class SKKMetaKey {
	enum MetaKey {SHIFT_KEY, ALT_KEY};
	private static final EnumMap<MetaKey, Integer> KEYCODES = new EnumMap<MetaKey, Integer>(MetaKey.class);

	private static final int MASK_SHIFT_STATES =
		KeyEvent.META_SHIFT_ON | KeyEvent.META_SHIFT_LEFT_ON | KeyEvent.META_SHIFT_RIGHT_ON;
	private static final int MASK_ALT_STATES =
		KeyEvent.META_ALT_ON | KeyEvent.META_ALT_LEFT_ON | KeyEvent.META_ALT_RIGHT_ON;
	private static final int MASK_ALL_STATES = MASK_SHIFT_STATES | MASK_ALT_STATES;

	private static final EnumMap<MetaKey, Integer> MASK_STATES = new EnumMap<MetaKey, Integer>(MetaKey.class);
	static {
		KEYCODES.put(MetaKey.SHIFT_KEY, KeyEvent.KEYCODE_SHIFT_LEFT);
		KEYCODES.put(MetaKey.ALT_KEY, KeyEvent.KEYCODE_ALT_LEFT);
		MASK_STATES.put(MetaKey.SHIFT_KEY, MASK_SHIFT_STATES);
		MASK_STATES.put(MetaKey.ALT_KEY, MASK_ALT_STATES);
	}

	private enum MetaKeyState {STATE_NONE, STATE_ON, STATE_LOCKED};

	private EnumMap<MetaKey, MetaKeyState> mState = new EnumMap<MetaKey, MetaKeyState>(MetaKey.class);
	private EnumMap<MetaKey, Boolean> isPressed = new EnumMap<MetaKey, Boolean>(MetaKey.class);
	private EnumMap<MetaKey, Boolean> isUsed = new EnumMap<MetaKey, Boolean>(MetaKey.class);

	private InputMethodService mIs;

	SKKMetaKey(InputMethodService s) {
		mIs = s;
		clearMetaKeyState();
	}

	void clearMetaKeyState() {
		InputConnection ic = mIs.getCurrentInputConnection();
		if (ic != null) ic.clearMetaKeyStates(MASK_ALL_STATES);

		for (MetaKey m : MetaKey.values()) {
			mState.put(m, MetaKeyState.STATE_NONE);
			isPressed.put(m, false);
			isUsed.put(m, false);
		}
	}

	void pressMetaKey(MetaKey key) {
		// send a key press event also to an editor
		InputConnection ic = mIs.getCurrentInputConnection();
		if (ic != null) ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KEYCODES.get(key)));

		isPressed.put(key, true);
		isUsed.put(key, false);
		switch (mState.get(key)) {
		case STATE_NONE:
			mState.put(key, MetaKeyState.STATE_ON);
			SKKUtils.dlog("metakey(" + key + "): off->on");
			break;
		case STATE_ON:
			mState.put(key, MetaKeyState.STATE_LOCKED);
			SKKUtils.dlog("metakey(" + key + "): on->locked");
			break;
		case STATE_LOCKED:
			mState.put(key, MetaKeyState.STATE_NONE);
			SKKUtils.dlog("metakey(" + key + "): locked->off");
			break;
		}
	}

	void releaseMetaKey(MetaKey key) {
		// send a key release event also to an editor
		InputConnection ic = mIs.getCurrentInputConnection();
		if (ic != null) ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KEYCODES.get(key)));

		isPressed.put(key, false);
		SKKUtils.dlog("metakey(" + key + ") released");
		if (mState.get(key) == MetaKeyState.STATE_ON && isUsed.get(key)) {
			mState.put(key, MetaKeyState.STATE_NONE);
			if (ic != null) ic.clearMetaKeyStates(MASK_STATES.get(key));
			SKKUtils.dlog("metakey(" + key + "): on->off");
		}
	}

	int useMetaState() {
		int meta = 0;

		if (useMetaKey(MetaKey.SHIFT_KEY)) meta |= KeyEvent.META_SHIFT_ON;
		if (useMetaKey(MetaKey.ALT_KEY)) meta |= KeyEvent.META_ALT_ON;

		return meta;
	}

	private boolean useMetaKey(MetaKey key) {
		switch (mState.get(key)) {
		case STATE_NONE:
			SKKUtils.dlog("metakey(" + key + "): off");
			return false;
		case STATE_ON:
			if (isPressed.get(key)) {
				SKKUtils.dlog("metakey(" + key + "): on");
				isUsed.put(key, true);
			} else {
				mState.put(key, MetaKeyState.STATE_NONE);
				InputConnection ic = mIs.getCurrentInputConnection();
				if (ic != null) ic.clearMetaKeyStates(MASK_STATES.get(key));
				SKKUtils.dlog("metakey(" + key + "): on->off");
			}
			return true;
		case STATE_LOCKED:
			SKKUtils.dlog("metakey(" + key + "): on");
			return true;
		}

		return false;
	}
}