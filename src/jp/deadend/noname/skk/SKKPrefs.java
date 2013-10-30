package jp.deadend.noname.skk;

import android.os.Bundle;
import android.os.Environment;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.KeyEvent;

public class SKKPrefs extends PreferenceActivity {
	private static final String PREFKEY_KUTOUTEN_TYPE = "PrefKeyKutoutenType";
	private static final String PREFKEY_KANA_KEY = "PrefKanaKey";
	private static final String PREFKEY_CANDIDATES_OP_COUNT = "PrefCandidatesOpCount";
	private static final String PREFKEY_FLICK_SENSITIVITY = "PrefKeyFlickSensitivity";
	private static final String PREFKEY_USE_SOFTKEY = "PrefKeyUseSoftKey";
	private static final String PREFKEY_USE_POPUP = "PrefKeyUsePopup";
	private static final String PREFKEY_FIXED_POPUP = "PrefKeyFixedPopup";
	private static final String PREFKEY_KEY_HEIGHT = "PrefKeyKeyHeight";

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		addPreferencesFromResource(R.xml.prefs);
	}

	static String getPrefKutoutenType(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(PREFKEY_KUTOUTEN_TYPE, "en");
	}

	//~ static String getPrefDefaultMode(Context context) {
		//~ return PreferenceManager.getDefaultSharedPreferences(context).getString(PREFKEY_DEFAULT_MODE, "");
	//~ }

	static int getCandidatesOpCount(Context context) {
		return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString(PREFKEY_CANDIDATES_OP_COUNT, "2"));
	}

	static int getPrefKanaKey(Context context) {
		int key = PreferenceManager.getDefaultSharedPreferences(context).getInt(PREFKEY_KANA_KEY, 93);
		if (key == KeyEvent.KEYCODE_UNKNOWN) {key = 93;}
		return key;
	}

	static int getFlickSensitivity(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getInt(PREFKEY_FLICK_SENSITIVITY, 10);
	}

	static String getPrefUseSoftKey(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(PREFKEY_USE_SOFTKEY, "auto");
	}

	static boolean getUsePopup(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREFKEY_USE_POPUP, true);
	}

	static boolean getFixedPopup(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREFKEY_FIXED_POPUP, false);
	}

	static int getKeyHeight(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getInt(PREFKEY_KEY_HEIGHT, 80);
	}
}
