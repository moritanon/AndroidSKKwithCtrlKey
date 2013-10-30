package jp.deadend.noname.skk;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.KeyboardView;
import android.os.Bundle;
import android.os.Handler;
import android.content.ClipboardManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import static jp.deadend.noname.skk.InputMode.*;

public class SKKEngine extends InputMethodService {
	private CandidateViewContainer mCandidateViewContainer;
	private CandidateView mCandidateView;
	private KeyboardView mInputView;
	private KeyboardView mFlickJPInputView;
	private KeyboardView mQwertyInputView;

	public int mChoosedIndex;
	private List<String> mCandidateList;
	private List<String> mSuggestions;

	private InputMode mInputMode = HIRAKANA;

	private boolean isSKKOn = true;

	// ひらがなや英単語などの入力途中
	private StringBuilder mComposing = new StringBuilder();
	// 漢字変換のキー 送りありの場合最後がアルファベット 変換中は不変
	private StringBuilder mKanji = new StringBuilder();
	// 送りがな 「っ」や「ん」が含まれる場合だけ二文字になる
	private String mOkurigana = null;

	// 単語登録のキー 登録作業中は不変
	private String mRegKey = null;
	// 単語登録する内容
	private StringBuilder mRegEntry = new StringBuilder();
	private boolean isRegistering = false;

	private boolean isCandidatesViewShown = false;
	private int mCandidatesOpCount = 0;
	private int mOpCount = 0;

	private static final String DICTIONARY = "skk_dict_btree";
	private static final String USER_DICT = "skk_userdict";
	private SKKDictionary mDict;
	private SKKUserDictionary mUserDict;

	private SKKMetaKey mMetaKey = new SKKMetaKey(this);

	private int mKanaKey = 0;

	private boolean mUseSoftKeyboard = false;

	private BroadcastReceiver mMushroomReceiver;
	private String mMushroomWord = null;
	private Handler hMushroom = new Handler();
	private Runnable rMushroom = new Runnable() {
		public void run() {
			if (mMushroomWord != null) {
				if (mMushroomWord.length() > 0) {
					getCurrentInputConnection().commitText(mMushroomWord, 1);
					mMushroomWord = null;
					keyDownUp(KeyEvent.KEYCODE_DPAD_CENTER);
				}
			}
		}
	};

	// ローマ字辞書
	private Map<String, String> mRomajiMap = new HashMap<String, String>();
	{
		Map<String, String> m = mRomajiMap;
		m.put("a", "あ");m.put("i", "い");m.put("u", "う");m.put("e", "え");m.put("o", "お");
		m.put("ka", "か");m.put("ki", "き");m.put("ku", "く");m.put("ke", "け");m.put("ko", "こ");
		m.put("sa", "さ");m.put("si", "し");m.put("su", "す");m.put("se", "せ");m.put("so", "そ");
		m.put("ta", "た");m.put("ti", "ち");m.put("tu", "つ");m.put("te", "て");m.put("to", "と");
		m.put("na", "な");m.put("ni", "に");m.put("nu", "ぬ");m.put("ne", "ね");m.put("no", "の");
		m.put("ha", "は");m.put("hi", "ひ");m.put("hu", "ふ");m.put("he", "へ");m.put("ho", "ほ");
		m.put("ma", "ま");m.put("mi", "み");m.put("mu", "む");m.put("me", "め");m.put("mo", "も");
		m.put("ya", "や");m.put("yi", "い");m.put("yu", "ゆ");m.put("ye", "いぇ");m.put("yo", "よ");
		m.put("ra", "ら");m.put("ri", "り");m.put("ru", "る");m.put("re", "れ");m.put("ro", "ろ");
		m.put("wa", "わ");m.put("wi", "うぃ");m.put("we", "うぇ");m.put("wo", "を");m.put("nn", "ん");
		m.put("ga", "が");m.put("gi", "ぎ");m.put("gu", "ぐ");m.put("ge", "げ");m.put("go", "ご");
		m.put("za", "ざ");m.put("zi", "じ");m.put("zu", "ず");m.put("ze", "ぜ");m.put("zo", "ぞ");
		m.put("da", "だ");m.put("di", "ぢ");m.put("du", "づ");m.put("de", "で");m.put("do", "ど");
		m.put("ba", "ば");m.put("bi", "び");m.put("bu", "ぶ");m.put("be", "べ");m.put("bo", "ぼ");
		m.put("pa", "ぱ");m.put("pi", "ぴ");m.put("pu", "ぷ");m.put("pe", "ぺ");m.put("po", "ぽ");
		m.put("va", "う゛ぁ");m.put("vi", "う゛ぃ");m.put("vu", "う゛");m.put("ve", "う゛ぇ");m.put("vo", "う゛ぉ");

		m.put("xa", "ぁ");m.put("xi", "ぃ");m.put("xu", "ぅ");m.put("xe", "ぇ");m.put("xo", "ぉ");
		m.put("xtu", "っ");m.put("xke", "ヶ");
		m.put("cha", "ちゃ");m.put("chi", "ち");m.put("chu", "ちゅ");m.put("che", "ちぇ");m.put("cho", "ちょ");
		m.put("fa", "ふぁ");m.put("fi", "ふぃ");m.put("fu", "ふ");m.put("fe", "ふぇ");m.put("fo", "ふぉ");

		m.put("xya", "ゃ");                 m.put("xyu", "ゅ");                 m.put("xyo", "ょ");
		m.put("kya", "きゃ");               m.put("kyu", "きゅ");               m.put("kyo", "きょ");
		m.put("gya", "ぎゃ");               m.put("gyu", "ぎゅ");               m.put("gyo", "ぎょ");
		m.put("sya", "しゃ");               m.put("syu", "しゅ");               m.put("syo", "しょ");
		m.put("sha", "しゃ");m.put("shi", "し");m.put("shu", "しゅ");m.put("she", "しぇ");m.put("sho", "しょ");
		m.put("ja",  "じゃ");m.put("ji",  "じ");m.put("ju", "じゅ");m.put("je", "じぇ");m.put("jo", "じょ");
		m.put("cha", "ちゃ");m.put("chi", "ち");m.put("chu", "ちゅ");m.put("che", "ちぇ");m.put("cho", "ちょ");
		m.put("tya", "ちゃ");               m.put("tyu", "ちゅ");m.put("tye", "ちぇ");m.put("tyo", "ちょ");
		m.put("tha", "てゃ");m.put("thi", "てぃ");m.put("thu", "てゅ");m.put("the", "てぇ");m.put("tho", "てょ");
		m.put("dha", "でゃ");m.put("dhi", "でぃ");m.put("dhu", "でゅ");m.put("dhe", "でぇ");m.put("dho", "でょ");
		m.put("dya", "ぢゃ");m.put("dyi", "ぢぃ");m.put("dyu", "ぢゅ");m.put("dye", "ぢぇ");m.put("dyo", "ぢょ");
		m.put("nya", "にゃ");               m.put("nyu", "にゅ");               m.put("nyo", "にょ");
		m.put("hya", "ひゃ");               m.put("hyu", "ひゅ");               m.put("hyo", "ひょ");
		m.put("pya", "ぴゃ");               m.put("pyu", "ぴゅ");               m.put("pyo", "ぴょ");
		m.put("bya", "びゃ");               m.put("byu", "びゅ");               m.put("byo", "びょ");
		m.put("mya", "みゃ");               m.put("myu", "みゅ");               m.put("myo", "みょ");
		m.put("rya", "りゃ");               m.put("ryu", "りゅ");m.put("rye", "りぇ");m.put("ryo", "りょ");
		m.put("z,", "‥");m.put("z-", "〜");m.put("z.", "…");m.put("z/", "・");m.put("z[", "『");m.put("z]", "』");m.put("zh", "←");m.put("zj", "↓");m.put("zk", "↑");m.put("zl", "→");
	}
	// 全角で入力する記号リスト
	private Map<String, String> mZenkakuSeparatorMap = new HashMap<String, String>();
	{
		Map<String, String> m = mZenkakuSeparatorMap;
		m.put("-", "ー");m.put("!", "！");m.put("?", "？");m.put("~", "〜");m.put("[", "「");m.put("]", "」");
		m.put(".", "。");m.put(",","、");
	}

	// 濁音半濁音変換用
	private Map<String, String> mConsonantMap = new HashMap<String, String>();
	{
		Map<String, String> m = mConsonantMap;
		m.put("が", "g");m.put("ぎ", "g");m.put("ぐ", "g");m.put("げ", "g");m.put("ご", "g");
		m.put("か", "k");m.put("き", "k");m.put("く", "k");m.put("け", "k");m.put("こ", "k");
		m.put("ざ", "z");m.put("じ", "z");m.put("ず", "z");m.put("ぜ", "z");m.put("ぞ", "z");
		m.put("さ", "s");m.put("し", "s");m.put("す", "s");m.put("せ", "s");m.put("そ", "s");
		m.put("だ", "d");m.put("ぢ", "d");m.put("づ", "d");m.put("で", "d");m.put("ど", "d");
		m.put("た", "t");m.put("ち", "t");m.put("つ", "t");m.put("て", "t");m.put("と", "t");
		m.put("ば", "b");m.put("び", "b");m.put("ぶ", "b");m.put("べ", "b");m.put("ぼ", "b");
		m.put("ぱ", "p");m.put("ぴ", "p");m.put("ぷ", "p");m.put("ぺ", "p");m.put("ぽ", "p");
		m.put("は", "h");m.put("ひ", "h");m.put("ふ", "h");m.put("へ", "h");m.put("ほ", "h");
	}

	@Override
	public void onCreate() {
		CustomUncaughtExceptionHandler customUncaughtExceptionHandler = new CustomUncaughtExceptionHandler(
                getApplicationContext());
		
        Thread.setDefaultUncaughtExceptionHandler(customUncaughtExceptionHandler);
		super.onCreate();

		Context bc = getBaseContext();

		String kutouten = SKKPrefs.getPrefKutoutenType(bc);
		SKKUtils.dlog("kutouten type: " + kutouten);
		/* if (kutouten.equals("en")) {
			mZenkakuSeparatorMap.put(".", "．");
			mZenkakuSeparatorMap.put(",", "，");
		} else if (kutouten.equals("jp")) {
			mZenkakuSeparatorMap.put(".", "。");
			mZenkakuSeparatorMap.put(",", "、");
		} else if (kutouten.equals("jp_en")) {
			mZenkakuSeparatorMap.put(".", "。");
			mZenkakuSeparatorMap.put(",", "，");
		} else {
			mZenkakuSeparatorMap.put(".", "．");
			mZenkakuSeparatorMap.put(",", "，");
		} */
		mZenkakuSeparatorMap.put(".", "。");
		mZenkakuSeparatorMap.put(",", "、");

		mKanaKey = SKKPrefs.getPrefKanaKey(bc);
		SKKUtils.dlog("Kana key code: " + mKanaKey);
		mCandidatesOpCount = SKKPrefs.getCandidatesOpCount(bc);
		SKKUtils.dlog("op count: " + mCandidatesOpCount);

		String dd = getFilesDir().getAbsolutePath();
		SKKUtils.dlog("dict dir: " + dd);
		mDict = new SKKDictionary(dd + "/" + DICTIONARY);
		if (!mDict.isValid()) {
			Toast.makeText(SKKEngine.this, getString(R.string.error_dic), Toast.LENGTH_LONG).show();
			stopSelf();
		}
		mUserDict = new SKKUserDictionary(dd + "/" + USER_DICT);
		if (!mUserDict.isValid()) {
			Toast.makeText(SKKEngine.this, getString(R.string.error_user_dic), Toast.LENGTH_LONG).show();
			stopSelf();
		}

		mMushroomReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				Bundle extras = intent.getExtras();
				if (extras != null) {
					mMushroomWord = extras.getString(SKKMushroom.REPLACE_KEY);
					SKKUtils.dlog("mMushroomWord: " + mMushroomWord);
				}
				hMushroom.postDelayed(rMushroom, 250);

				//~ if (mMushroomWord != null) {
					//~ ClipboardManager cm = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
					//~ cm.setText(mMushroomWord);
				//~ }
			}
		};

		IntentFilter filter = new IntentFilter(SKKMushroom.ACTION_BROADCAST);
		filter.addCategory(SKKMushroom.CATEGORY_BROADCAST);
		registerReceiver(mMushroomReceiver, filter);
	}

	/**
	* This is the point where you can do all of your UI initialization.  It
	* is called after creation and any configuration change.
	*/
	@Override public void onInitializeInterface() {
		
		Configuration config = getResources().getConfiguration();
		String use_softkey = SKKPrefs.getPrefUseSoftKey(getApplicationContext());

		if (config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
			SKKUtils.dlog("hardware keyboard found");
			if (use_softkey.equals("on")) {
				mUseSoftKeyboard = true;
				SKKUtils.dlog("software keyboard forced");
			} else {
				mUseSoftKeyboard = false;
				SKKUtils.dlog("software keyboard disabled");
			}
		} else if (config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES) {
			SKKUtils.dlog("hardware keyboard not found");
			if (use_softkey.equals("off")) {
				mUseSoftKeyboard = false;
				SKKUtils.dlog("software keyboard disabled");
			} else {
				mUseSoftKeyboard = true;
				SKKUtils.dlog("use software keyboard");
			}
		}

		if (mUseSoftKeyboard) {
			mFlickJPInputView = (FlickJPKeyboardView)getLayoutInflater().inflate(R.layout.input_flickjp, null);
			((FlickJPKeyboardView)mFlickJPInputView).setService(this);
			mQwertyInputView = (QwertyKeyboardView)getLayoutInflater().inflate(R.layout.input_qwerty, null);
			((QwertyKeyboardView)mQwertyInputView).setService(this);
		} else {
			mFlickJPInputView = null;
			mQwertyInputView = null;
		}
    }

	@Override
	public void onDestroy() {
		mUserDict.commitChanges();
		unregisterReceiver(mMushroomReceiver);

		super.onDestroy();
	}

	/**
	* This is the main point where we do our initialization of the
	* input method to begin operating on an application. At this
	* point we have been bound to the client, and are now receiving
	* all of the detailed information about the target of our edits.
	*/
	@Override
	public void onStartInput(EditorInfo attribute, boolean restarting) {
		super.onStartInput(attribute, restarting);
		mMetaKey.clearMetaKeyState();
		if (isSKKOn) changeMode(mInputMode, true);
	}

	@Override
	public View onCreateInputView() {
		if (mUseSoftKeyboard) {
			if (isSKKOn) {
				mInputView = mFlickJPInputView;
				if (mInputMode == KATAKANA) {
					((FlickJPKeyboardView)mInputView).setKatakanaMode();
				}
			} else {
				mInputView = mQwertyInputView;
			}
			return mInputView;
		}

		return null;
	}

	/**
	* Called by the framework when your view for showing candidates
	* needs to be generated, like {@link #onCreateInputView}.
	*/
	@Override
	public View onCreateCandidatesView() {
		mCandidateViewContainer = (CandidateViewContainer) getLayoutInflater().inflate(R.layout.candidates, null);
		mCandidateViewContainer.initViews();
		mCandidateView = (CandidateView) mCandidateViewContainer.findViewById(R.id.candidates);
		mCandidateView.setService(this);

		return mCandidateViewContainer;
	}

	@Override
	public void onStartCandidatesView(EditorInfo info, boolean restarting) {
		isCandidatesViewShown = true;
	}

	@Override
	public void onFinishCandidatesView(boolean finishingInput) {
		isCandidatesViewShown = false;
		super.onFinishCandidatesView(finishingInput);
	}

    /**
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    @Override public void onFinishInput() {
		super.onFinishInput();

		mComposing.setLength(0);
		//~ updateCandidates();

		setCandidatesViewShown(false);

		if (mInputView != null) {
			mInputView.closing();
		}
	}

	// never use fullscreen mode
	@Override
	public boolean onEvaluateFullscreenMode() {
		return false;
	}

	/**
	* Use this to monitor key events being delivered to the
	* application. We get first crack at them, and can either resume
	* them or let them continue to the app.
	*/
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (!isSKKOn) return super.onKeyUp(keyCode, event);

		switch (keyCode) {
		case KeyEvent.KEYCODE_SHIFT_LEFT:
		case KeyEvent.KEYCODE_SHIFT_RIGHT:
			mMetaKey.releaseMetaKey(SKKMetaKey.MetaKey.SHIFT_KEY);
			return true;
		case KeyEvent.KEYCODE_ALT_LEFT:
		case KeyEvent.KEYCODE_ALT_RIGHT:
			mMetaKey.releaseMetaKey(SKKMetaKey.MetaKey.ALT_KEY);
			return true;
		case KeyEvent.KEYCODE_ENTER:
			return true;
		default:
			break;
		}

		return super.onKeyUp(keyCode, event);
	}

	private boolean isKanaKey(int key, KeyEvent event) {
		SKKUtils.dlog("isKanaKey " + key + "  "  + event.isCtrlPressed());

		return (key == 38 && event.isCtrlPressed());
	}
	private boolean isCancelKey(int key, KeyEvent event) {
		return (key == 35 && event.isCtrlPressed());
	}
	/**
	* Use this to monitor key events being delivered to the
	* application. We get first crack at them, and can either resume
	* them or let them continue to the app.
	*/
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		SKKUtils.dlog("onKeyDown(): keyCode=" + keyCode + " mComp=" + mComposing + " mKanji=" + mKanji + " im=" + mInputMode);

		if (!isSKKOn) {
			if (isKanaKey(keyCode, event)) {
				toggleSKK();
				//changeMode(HIRAKANA, true);
				return true;
			} else {
				return super.onKeyDown(keyCode, event);
			}
		}

		if (isKanaKey(keyCode, event)) { // かなモードに移行するためのキー(設定可)
			switch (mInputMode) {
			case ZENKAKU:
			case KATAKANA:
				changeMode(HIRAKANA, true);
				break;
			case HIRAKANA:
				if (!isRegistering) toggleSKK();
				break;
			case ENG2JP:
			case CHOOSE:
				handleEnter();
				break;
			default:
				break;
			}
			return true;
		}
		
		if (isCancelKey(keyCode, event)) {
			handleCancel(false);
			return true;
		}
		
		if (event.isCtrlPressed()) {
			return true;
		}

		// Process special keys
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			if (!handleBackKey()) {
				return super.onKeyDown(keyCode, event);
			}
			break;
		case KeyEvent.KEYCODE_DEL:
			handleCancel(true);
			break;
		case KeyEvent.KEYCODE_ENTER:
			handleEnter();
			break;
		case KeyEvent.KEYCODE_SHIFT_LEFT:
		case KeyEvent.KEYCODE_SHIFT_RIGHT:
			mMetaKey.pressMetaKey(SKKMetaKey.MetaKey.SHIFT_KEY);
			break;
		case KeyEvent.KEYCODE_ALT_LEFT:
		case KeyEvent.KEYCODE_ALT_RIGHT:
			mMetaKey.pressMetaKey(SKKMetaKey.MetaKey.ALT_KEY);
			break;
		case KeyEvent.KEYCODE_DPAD_LEFT:
			handleLeftKey();
			break;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			handleRightKey();
			break;
		case KeyEvent.KEYCODE_DPAD_UP:
		case KeyEvent.KEYCODE_DPAD_DOWN:
			mMetaKey.useMetaState();
			if (!isRegistering && mComposing.length() == 0 && mKanji.length() == 0) return super.onKeyDown(keyCode, event);
			break;
		default:
			// For all other keys, if we want to do transformations on
			// text being entered with a hard keyboard, we need to
			// process it and do the appropriate action.
			if (translateKeyDown(keyCode, event)) {
				return true;
			} else {
				return super.onKeyDown(keyCode, event);
			}
		}

		return true;
	}

	/**
	* This translates incoming hard key events in to edit operations
	* on an InputConnection.
	*/
	private boolean translateKeyDown(int keyCode, KeyEvent event) {
		// Shift・Altキーの状態をチェック
		int metaBit = mMetaKey.useMetaState();

		int c = event.getUnicodeChar(metaBit);
		SKKUtils.dlog("key:" + keyCode + " translated:" + c);

		InputConnection ic = getCurrentInputConnection();
		if (c == 0 || ic == null) {
			SKKUtils.dlog("translateKeyDown(): didn't go processKey()");
			return false;
		}

		processKey(c);

		return true;
	}

	void processKey(int pcode) {
		switch (mInputMode) {
		case ZENKAKU:
			// 全角変換しcommitして終了
			pcode = SKKUtils.hankaku2zenkaku(pcode);
			commitTextSKK(String.valueOf((char) pcode), 1);
			break;
		case ENG2JP:
			// スペースで変換するかそのままComposingに積む
			if (pcode == ' ') {
				if (mComposing.length() != 0) {
					mKanji.setLength(0);
					mKanji.append(mComposing);
					conversionStart(mKanji);
				}
			} else {
				mComposing.append((char) pcode);
				setComposingTextSKK(mComposing, 1);
				if (mUseSoftKeyboard) {
					updateSuggestions();
				}
			}
			break;
		case CHOOSE:
			// 最初の候補より戻ると変換に戻る 最後の候補より進むと登録
			switch (pcode) {
			case ' ':
				if (mChoosedIndex == mCandidateList.size() - 1) {
					if (!isRegistering) registerStart(mKanji.toString());
				} else {
					chooseNextCandidate();
				}
				break;
			case '>':
				// 接尾辞入力
				pickCandidate(mChoosedIndex);
				changeMode(KANJI, false);
				mKanji.append('>');
				setComposingTextSKK(mKanji, 1);
				break;
			case 'x':
				if (mChoosedIndex == 0) {
					if (mComposing.length() != 0) {
						// KANJIモードに戻る
						if (mOkurigana != null) {
							mOkurigana = null;
							mKanji.deleteCharAt(mKanji.length() -1);
						}
						setComposingTextSKK(mKanji, 1);
						changeMode(KANJI, false);
					} else {
						mKanji.setLength(0);
						setComposingTextSKK(mComposing, 1);
						changeMode(ENG2JP, false);
					}
					if (mUseSoftKeyboard) {
						updateSuggestions();
					}
				} else {
					choosePreviousCandidate();
				}
				break;
			default:
				pickCandidate(mChoosedIndex);
				processKey(pcode);
				break;
			}
			break;
		case HIRAKANA:
		case KATAKANA:
		case KANJI:
		case OKURIGANA:
			// モード変更操作
			if (mInputMode == HIRAKANA || mInputMode == KATAKANA) {
				switch (pcode) {
				case 'q':
					if (mInputMode == HIRAKANA) {
						changeMode(KATAKANA, true);
					} else {
						changeMode(HIRAKANA, true);
					}
					return;
				case 'l':
					if (mComposing.length() != 1 || mComposing.charAt(0) != 'z') {
						if (!isRegistering) toggleSKK();
						return;
					} // 「→」を入力するための例外
					break;
				case 'L':
					changeMode(ZENKAKU, true);
					return;
				case '/':
					if (mComposing.length() != 1 || mComposing.charAt(0) != 'z') {
						changeMode(ENG2JP, true);
						return;
					} // 中黒を入力するための例外
					break;
				default:
					break;
				}
			}

			SKKUtils.dlog("doJapaneseConversion():: " + pcode + "(" + (char) pcode + ") mComp=" + mComposing + " mKanji=" + mKanji + " im=" + mInputMode);
			doJapaneseConversion(pcode);
			SKKUtils.dlog("End: mComp=" + mComposing + " mKanji=" + mKanji + " im=" + mInputMode);
			SKKUtils.dlog("--------------------------------------------------------------------------------");

			break;
		default:
			SKKUtils.dlog("Unknown mode!");
			break;
		}
	}

	// processKey()が長くて疲れるので，日本語変換関係だけ分けたもの
	private void doJapaneseConversion(int pcode) {
		String hchr = null; // かな1単位ぶん

		// シフトキーの状態をチェック
		boolean isUpper = Character.isUpperCase(pcode);
		if (isUpper) { // ローマ字変換のために小文字に戻す
			pcode = Character.toLowerCase(pcode);
		}

		switch (mInputMode) {
		case OKURIGANA:
			// 「ん」か「っ」を処理したらここで終わり
			if (handleNN(pcode)) return;
			SKKUtils.dlog("okuri: mOkuri=" + mOkurigana + " mComp=" + mComposing + " pcode=" + (char)pcode);
			// 送りがなが確定すれば変換，そうでなければComposingに積む
			mComposing.append((char) pcode);
			hchr = mRomajiMap.get(mComposing.toString());
			if (mOkurigana != null) { //「ん」か「っ」がある場合
				if (hchr != null) {
					mComposing.setLength(0);
					mOkurigana = mOkurigana + hchr;
					conversionStart(mKanji);
				} else {
					setComposingTextSKK(mKanji.toString().substring(0, mKanji.length()-1) + mOkurigana + mComposing.toString(), 1);
				}
			} else {
				if (hchr != null) {
					mComposing.setLength(0);
					mOkurigana = hchr;
					conversionStart(mKanji);
				} else {
					setComposingTextSKK(mKanji.toString().substring(0, mKanji.length()-1) + mComposing.toString(), 1);
				}
			}
			break;
		case HIRAKANA:
		case KATAKANA:
			handleNN(pcode);
			if (isUpper) {
				// 漢字変換候補入力の開始。KANJIへの移行
				if (mComposing.length() > 0) {
					commitTextSKK(mComposing, 1);
					mComposing.setLength(0);
				}
				changeMode(KANJI, false);
				doJapaneseConversion(pcode);
			} else {
				mComposing.append((char) pcode);
				// 全角にする記号ならば全角，そうでなければローマ字変換
				hchr = mZenkakuSeparatorMap.get(mComposing.toString());
				if (hchr == null) {
					hchr = mRomajiMap.get(mComposing.toString());
				}

				if (hchr != null) { // 確定できるものがあれば確定
					if (mInputMode == KATAKANA) {
						hchr = SKKUtils.hirakana2katakana(hchr);
					}
					mComposing.setLength(0);
					commitTextSKK(hchr, 1);
				} else { // アルファベットならComposingに積む
					if (SKKUtils.isAlphabet(pcode)) {
						setComposingTextSKK(mComposing, 1);
					} else {
						commitTextSKK(mComposing, 1);
						mComposing.setLength(0);
					}
				}
			}
			break;
		case KANJI:
			handleNN(pcode);
			if (pcode == 'q') {
				// カタカナ変換
				if (mKanji.length() > 0) {
					String str = SKKUtils.hirakana2katakana(mKanji.toString());
					commitTextSKK(str, 1);
				}
				changeMode(HIRAKANA, true);
			} else if (pcode == ' ' || pcode == '>') {
				// 変換開始
				// 最後に単体の'n'で終わっている場合、'ん'に変換
				if (mComposing.length() == 1 && mComposing.charAt(0) == 'n') {
					mKanji.append('ん');
					setComposingTextSKK(mKanji, 1);
				}
				if (pcode == '>') {
					// 接頭辞入力
					mKanji.append('>');
				}
				mComposing.setLength(0);
				conversionStart(mKanji);
			} else if (isUpper && mKanji.length() > 0) {
				// 送り仮名開始
				// 最初の平仮名はついシフトキーを押しっぱなしにしてしまうた
				// め、mKanjiの長さをチェックmKanjiの長さが0の時はシフトが
				// 押されていなかったことにして下方へ継続させる
				mKanji.append((char) pcode); //送りありの場合子音文字追加
				mComposing.setLength(0);
				if (SKKUtils.isVowel(pcode)) { // 母音なら送り仮名決定，変換
					mOkurigana  = mRomajiMap.get(String.valueOf((char) pcode));
					conversionStart(mKanji);
				} else { // それ以外は送り仮名モード
					mComposing.append((char) pcode);
					setComposingTextSKK(mKanji, 1);
					changeMode(OKURIGANA, false);
				}
			} else {
				// 未確定
				mComposing.append((char) pcode);
				hchr = mZenkakuSeparatorMap.get(mComposing.toString());
				if (hchr == null) {
					hchr = mRomajiMap.get(mComposing.toString());
				}

				if (hchr != null) {
					mComposing.setLength(0);
					mKanji.append(hchr);
					setComposingTextSKK(mKanji, 1);
				} else {
					setComposingTextSKK(mKanji.toString() + mComposing.toString(), 1);
				}
				if (mUseSoftKeyboard) {
					updateSuggestions();
				}
			}
			break;
		default:
			SKKUtils.dlog("Unknown mode!");
			break;
		}
	}

	// commitTextのラッパー 登録作業中なら登録内容に追加し，表示を更新
	private void commitTextSKK(CharSequence text, int newCursorPosition) {
		InputConnection ic = getCurrentInputConnection();
		if (ic == null) return;

		if (isRegistering) {
			mRegEntry.append(text);
			ic.setComposingText("▼" + mRegKey + "：" + mRegEntry, newCursorPosition);
		} else {
			ic.commitText(text, newCursorPosition);
		}
	}

	void sendText(CharSequence text) {
		getCurrentInputConnection().commitText(text, 1);
	}

	//setComposingTextのラッパー
	private void setComposingTextSKK(CharSequence text, int newCursorPosition) {
		InputConnection ic = getCurrentInputConnection();
		if (ic == null) return;

		StringBuilder ct = new StringBuilder();

		if (isRegistering) {
			ct.append("▼");
			ct.append(mRegKey);
			ct.append("：");
			ct.append(mRegEntry);
		}

		if (!text.equals("") || mInputMode == ENG2JP) {
			switch (mInputMode) {
			case KANJI:
			case ENG2JP:
			case OKURIGANA:
				ct.append("▽");
				break;
			case CHOOSE:
				ct.append("▼");
				break;
			default:
				break;
			}
		}
		ct.append(text);

		ic.setComposingText(ct, newCursorPosition);
	}

	// 変換スタート
	// composingに辞書のキー 送りありの場合最後はアルファベット
	// 送りありの場合は送りがな自体をmOkuriganaに入れておく
	private void conversionStart(StringBuilder composing) {
		String str = composing.toString();

		changeMode(CHOOSE, false);

		List<String> list = findKanji(str);
		if (list == null) {
			registerStart(str);
			return;
		}

		mChoosedIndex = 0;

		if (mOkurigana != null) {
			setComposingTextSKK(SKKUtils.removeAnnotation(list.get(0)).concat(mOkurigana), 1);
		} else {
			setComposingTextSKK(SKKUtils.removeAnnotation(list.get(0)), 1);
		}

		mCandidateList = list;
		mOpCount = 1;
		if (mCandidatesOpCount == 1 || mUseSoftKeyboard) {
			setCandidatesToCandidateView();
		}
	}

	private void registerStart(String str) {
		mRegKey = str;
		mRegEntry.setLength(0);
		isRegistering = true;
		changeMode(HIRAKANA, true);
		//setComposingTextSKK("", 1);
	}

	// "ん"と"っ"を取り扱う
	// 処理した場合はtrue
	private boolean handleNN(int pcode) {
		if (mComposing.length() != 1 || mOkurigana != null) return false;

		char first = mComposing.charAt(0);
		String str;
		if (first == 'n') {
			if (!SKKUtils.isVowel(pcode) && pcode != 'n' && pcode != 'y') {
				str = "ん";
			} else {
				return false;
			}
		} else if (first == pcode) {
			str = "っ";
		} else {
			return false;
		}
		if (mInputMode == KATAKANA) str = SKKUtils.hirakana2katakana(str);

		if (mInputMode == OKURIGANA) {
			mOkurigana = str;
			setComposingTextSKK(mKanji.toString().substring(0, mKanji.length()-1) + str + (char)pcode, 1);
			mComposing.setLength(0);
			mComposing.append((char)pcode);
		} else if (mInputMode == KANJI) {
			mKanji.append(str);
			setComposingTextSKK(mKanji, 1);
			mComposing.setLength(0);
		} else { // HIRAGANA, KATAKANA
			commitTextSKK(str, 1);
			mComposing.setLength(0);
		}

		return true;
	}

	private boolean handleBackKey() {
		switch (mInputMode) {
		case HIRAKANA:
		case KATAKANA:
		case ZENKAKU:
			if (isRegistering) {
				isRegistering = false;
				mRegKey = null;
				mRegEntry.setLength(0);
				reset();

				return true;
			}
			break;
		case KANJI:
		case CHOOSE:
		case ENG2JP:
		case OKURIGANA:
			if (isRegistering) {
				isRegistering = false;
				mRegKey = null;
				mRegEntry.setLength(0);
			}
			changeMode(HIRAKANA, true);

			return true;
		}

		return false;
	}

	void handleLeftKey() {
		// Shift・Altキーの状態を消費
		mMetaKey.useMetaState();
		if (mInputMode == CHOOSE) {
			choosePreviousCandidate();
		} else if (!isRegistering && mComposing.length() == 0 && mKanji.length() == 0) {
			keyDownUp(KeyEvent.KEYCODE_DPAD_LEFT);
		}
	}

	void handleRightKey() {
		mMetaKey.useMetaState();
		if (mInputMode == CHOOSE) {
			chooseNextCandidate();
		} else if (!isRegistering && mComposing.length() == 0 && mKanji.length() == 0) {
			keyDownUp(KeyEvent.KEYCODE_DPAD_RIGHT);
		}
	}

	void handleEnter() {
		switch (mInputMode) {
		case CHOOSE:
			pickCandidate(mChoosedIndex);
			break;
		case ENG2JP:
			if (mComposing.length() > 0) {
				commitTextSKK(mComposing, 1);
				mComposing.setLength(0);
			}
			changeMode(HIRAKANA, true);
			break;
		case KANJI:
		case OKURIGANA:
			commitTextSKK(mKanji, 1);
			mComposing.setLength(0);
			mKanji.setLength(0);
			changeMode(HIRAKANA, true);
			break;
		default:
			if (mComposing.length() == 0) {
				if (isRegistering) {
					// 単語登録終了
					isRegistering = false;
					if (mRegEntry.length() != 0) {
						mUserDict.addEntry(mRegKey, mRegEntry.toString());
						mUserDict.commitChanges();
						getCurrentInputConnection().commitText(mRegEntry.toString(), 1);
					}
					mRegKey = null;
					mRegEntry.setLength(0);
					reset();
				} else {
					keyDownUp(KeyEvent.KEYCODE_ENTER);
				}
			} else {
				commitTextSKK(mComposing, 1);
				mComposing.setLength(0);
			}
		}

		// Shift・Altキーの状態を消費
		mMetaKey.useMetaState();
	}


	void handleCancel(boolean withBackSpace) {
		int clen = mComposing.length();
		int klen = mKanji.length();
		SKKUtils.dlog("handleCancel(): clen=" + clen + " klen=" + klen + " mComp=" + mComposing + " mKanji=" + mKanji + " im=" + mInputMode);

		if (clen == 0 && klen == 0) {
			if (isRegistering) {
				int rlen = mRegEntry.length();
				if (rlen > 0) {
					mRegEntry.deleteCharAt(rlen-1);
					setComposingTextSKK("", 1);
				}
			} else if (mInputMode == ENG2JP) {
				changeMode(HIRAKANA, true);
			} else {
				keyDownUp(KeyEvent.KEYCODE_DEL);
			}
			// Shift・Altキーの状態を消費
			mMetaKey.useMetaState();
			return;
		}

		if (withBackSpace) {
			if (clen > 0) {
				mComposing.deleteCharAt(clen-1);
			} else if (klen > 0) {
				mKanji.deleteCharAt(klen-1);
			}
			clen = mComposing.length();
			klen = mKanji.length();
		}

		switch (mInputMode) {
		case HIRAKANA:
		case KATAKANA:
			setComposingTextSKK(mComposing, 1);
			break;
		case KANJI:
			if (klen == 0 && clen == 0) {
				changeMode(HIRAKANA, true);
			} else {
				setComposingTextSKK(mKanji.toString() + mComposing.toString(), 1);
				if (mUseSoftKeyboard) {
					updateSuggestions();
				}
			}
			break;
		case ENG2JP:
			if (clen == 0) {
				changeMode(HIRAKANA, true);
			} else {
				setComposingTextSKK(mComposing, 1);
				if (mUseSoftKeyboard) {
					updateSuggestions();
				}
			}
			break;
		case OKURIGANA:
			mComposing.setLength(0);
			mOkurigana = null;
			setComposingTextSKK(mKanji, 1);
			changeMode(KANJI, false);
			break;
		case CHOOSE:
			if (klen == 0) {
				changeMode(HIRAKANA, true);
			} else {
				if (clen > 0) { // 英語変換中
					changeMode(ENG2JP, false);
					setComposingTextSKK(mComposing, 1);
					if (mUseSoftKeyboard) {
						updateSuggestions();
					}
				} else { // 漢字変換中
					if (mOkurigana != null) {
						mOkurigana = null;
					}
					changeMode(KANJI, false);
					setComposingTextSKK(mKanji, 1);
					if (mUseSoftKeyboard) {
						updateSuggestions();
					}
				}
			}
			break;
		default:
			SKKUtils.dlog("handleBackspace() do nothing");
			break;
		}

		// Shift・Altキーの状態を消費
		mMetaKey.useMetaState();
		SKKUtils.dlog("handleBackspace() end: clen=" + clen + " klen=" + klen + " mComp=" + mComposing + " mKanji=" + mKanji + " im=" + mInputMode);
	}

	void sendToMushroom() {
		String str;
		if (mInputMode == KANJI || mInputMode == ENG2JP) {
			str = mKanji.toString();
		} else {
			ClipboardManager cm = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
			CharSequence cs = cm.getText();
			if (cs == null) {
				str = "";
			} else {
				str = cs.toString();
			}
		}

		if (mInputMode == HIRAKANA || mInputMode == KATAKANA || mInputMode == ZENKAKU) {
			reset();
		} else {
			changeMode(HIRAKANA, true);
		}

		try {
			Intent mushroom = new Intent(this, SKKMushroom.class);
			mushroom.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mushroom.putExtra(SKKMushroom.REPLACE_KEY, str);
			startActivity(mushroom);
		} catch (ActivityNotFoundException e) {
		}
	}

	// 小文字大文字変換，濁音，半濁音に使う
	void changeLastChar(Map<String, String> map) {
		if (mInputMode == KANJI && mComposing.length() == 0) {
			String s = mKanji.toString();
			int idx = s.length() - 1;
			String lastchar = s.substring(idx);
			String new_lastchar = map.get(lastchar);

			if (new_lastchar != null) {
				mKanji.deleteCharAt(idx);
				mKanji.append(new_lastchar);
				setComposingTextSKK(mKanji, 1);
				if (mUseSoftKeyboard) {
					updateSuggestions();
				}
			}
			return;
		}

		if (mInputMode == CHOOSE) {
			if (mOkurigana == null) return;
			String new_okuri = map.get(mOkurigana);

			if ((mOkurigana.equals("つ") && new_okuri.equals("っ")) || (mOkurigana.equals("っ") && new_okuri.equals("つ"))) {
				// 変換やりなおしをしなくてもいい例外
				// 送りがなが「っ」になる場合は，どのみち必ずt段の音なので
				mOkurigana = new_okuri;
				setComposingTextSKK(SKKUtils.removeAnnotation(mCandidateList.get(mChoosedIndex)).concat(mOkurigana), 1);
			} else if (new_okuri != null) {
				String new_okuri_consonant = mConsonantMap.get(new_okuri);
				mKanji.deleteCharAt(mKanji.length() - 1);
				mKanji.append(new_okuri_consonant);
				mOkurigana = new_okuri;
				conversionStart(mKanji); //変換やりなおし
			}
			return;
		}

		if (mComposing.length() == 0 && mKanji.length() == 0) {
			String lastchar = null;
			String new_lastchar = null;

			InputConnection ic = getCurrentInputConnection();
			if (ic != null) {
				CharSequence cs = ic.getTextBeforeCursor(1, 0);
				if (cs != null) {
					lastchar = cs.toString();
					new_lastchar = map.get(lastchar);

					if (new_lastchar != null) {
						ic.deleteSurroundingText(1, 0);
						ic.commitText(new_lastchar, 1);
					}
				}
			}
		}
	}

	private List<String> findKanji(String key) {
		SKKUtils.dlog("findKanji(): key=" + key);
		List<String> list1 = mDict.getCandidates(key);
		List<String> list2 = mUserDict.getCandidates(key);

		if (list1 == null && list2 == null) {
			SKKUtils.dlog("Dictoinary: Can't find Kanji for " + key);
			return null;
		}

		if (list1 == null) list1 = new ArrayList<String>();
		if (list2 != null) {
			int idx = 0;
			for (String s : list2) {
				//個人辞書の候補を先頭に追加
				list1.remove(s);
				list1.add(idx, s);
				idx++;
			}
		}

		return list1;
	}

	private void updateSuggestions() {
		mChoosedIndex = 0;
		int clen = mComposing.length();
		int klen = mKanji.length();

		if (clen == 0 && klen == 0) {
			setSuggestions(null);
			return;
		}

		String str = mComposing.toString();
		List<String> list = new ArrayList<String>();

		switch (mInputMode) {
		case ENG2JP:
			list.add(str);
			mDict.findKeys(str, false, list);
			break;
		case KANJI:
			if (clen == 0) {
				str = mKanji.toString();
				list.add(str);
			} else {
				//list.add(str);
				String tmp = str.concat("a");
				// ローマ字入力中はとりあえずア行に借り決めして検索。こうしないと英単語が出て使えない
				tmp = mRomajiMap.get(tmp);
				if (tmp != null) str = tmp;
				str = mKanji.toString().concat(str);
			}

			mDict.findKeys(str, true, list);
			break;
		default:
			SKKUtils.dlog("updateSuggestions(): " + mInputMode);
		}

		setSuggestions(list);
	}

	private void setSuggestions(List<String> suggestions) {
		if (suggestions != null && suggestions.size() > 0) {
			mSuggestions = suggestions;
			setCandidatesViewShown(true);
		} else if (isExtractViewShown()) {
			setCandidatesViewShown(true);
		}

		if (mCandidateView != null) {
			mCandidateView.setSuggestions(suggestions);
		}
	}

	private void choosePreviousCandidate() {
		if (mCandidateList == null) return;

		String cad;
		mChoosedIndex--;
		if (mChoosedIndex < 0) {
			mChoosedIndex = mCandidateList.size() - 1;
		}
		mOpCount++;
		SKKUtils.dlog("mOpCount: " + mOpCount + "  isCandidatesViewShown: " + isCandidatesViewShown);
		if (mCandidatesOpCount != 0 && !isCandidatesViewShown && mOpCount >= mCandidatesOpCount) {
			setCandidatesToCandidateView();
		}
		if (mCandidateView != null) {
			mCandidateView.choose(mChoosedIndex);
		}
		cad = SKKUtils.removeAnnotation(mCandidateList.get(mChoosedIndex));
		if (mOkurigana != null) {
			cad = cad.concat(mOkurigana);
		}
		setComposingTextSKK(cad, 1);

		return;
	}

	private void chooseNextCandidate() {
		if (mCandidateList == null) return;

		String cad;
		mChoosedIndex++;
		if (mChoosedIndex >= mCandidateList.size()) {
			mChoosedIndex = 0;
		}
		mOpCount++;
		SKKUtils.dlog("mOpCount: " + mOpCount + "  isCandidatesViewShown: " + isCandidatesViewShown);
		if (mCandidatesOpCount != 0 && !isCandidatesViewShown && mOpCount >= mCandidatesOpCount) {
			setCandidatesToCandidateView();
		}
		if (mCandidateView != null) {
			mCandidateView.choose(mChoosedIndex);
		}
		cad = SKKUtils.removeAnnotation(mCandidateList.get(mChoosedIndex));
		if (mOkurigana != null) {
			cad = cad.concat(mOkurigana);
		}
		setComposingTextSKK(cad, 1);

		return;
	}

	private void setCandidatesToCandidateView() {
		if (mCandidateList != null && mCandidateList.size() > 0) {
			setCandidatesViewShown(true);
		}
		if (mCandidateView != null) {
			mCandidateView.setSuggestions(mCandidateList);
		}
	}

	private void pickCandidate(int index) {
		if (mInputMode != CHOOSE) {
			return;
		}

		if (mCandidateList.size() > 0) {
			String s = mCandidateList.get(index);

			commitTextSKK(SKKUtils.removeAnnotation(s), 1);
			if (mOkurigana != null) commitTextSKK(mOkurigana, 1);
			SKKUtils.dlog("Fixed: s=" + s + " mKanji=" + mKanji + " mComp=" + mComposing + " mOkuri=" + mOkurigana);
			mUserDict.addEntry(mKanji.toString(), s);

			changeMode(HIRAKANA, true);
		}
	}

	public void pickCandidateViewManually(int index) {
		if (mInputMode == CHOOSE) {
			pickCandidate(index);
			return;
		}

		if (mSuggestions.size() > 0) {
			String s = mSuggestions.get(index);

			if (mInputMode == ENG2JP) {
				setComposingTextSKK(s, 1);
				mComposing.setLength(0);
				mComposing.append(s);
				conversionStart(mComposing);
			} else if (mInputMode == KANJI) {
				setComposingTextSKK(s, 1);
				int li = s.length() - 1;
				int last = s.codePointAt(li);
				if (SKKUtils.isAlphabet(last)) {
					mKanji.setLength(0);
					mKanji.append(s.substring(0, li));
					mComposing.setLength(0);
					processKey(Character.toUpperCase(last));
				} else {
					mKanji.setLength(0);
					mKanji.append(s);
					mComposing.setLength(0);
					conversionStart(mKanji);
				}
			}
		}
	}

	/**
	* Helper to send a key down / key up pair to the current editor.
	*/
	void keyDownUp(int keyEventCode) {
		InputConnection ic = getCurrentInputConnection();
		if (ic != null) {
			ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
			ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
		}
	}

	private void reset() {
		mComposing.setLength(0);
		mKanji.setLength(0);
		mOkurigana = null;
		mSuggestions = null;
		mCandidateList = null;

		//setCandidatesViewShown()ではComposingTextがflushされるので消す
		InputConnection ic = getCurrentInputConnection();
		if (ic != null) {
			ic.setComposingText("", 1);
		}
		setCandidatesViewShown(false);
//		mMetaKey.clearMetaKeyState();
	}

	void toggleSKK() {
		SKKUtils.dlog("toggleSKK");
		if (isSKKOn) {
			isSKKOn = false;
			hideStatusIcon();
			reset();
			if (mUseSoftKeyboard) {
				mInputView = mQwertyInputView;
				setInputView(mInputView);
			}
		} else {
			isSKKOn = true;
			if (mUseSoftKeyboard) {
				mInputView = mFlickJPInputView;
				setInputView(mInputView);
			}
			changeMode(HIRAKANA, true);
		}
		mMetaKey.clearMetaKeyState();
	}

	// change the mode and set the status icon
	private void changeMode(InputMode im, boolean doReset) {
		if (!isSKKOn) return;

		int icon = 0;

		if (doReset) reset();

		switch (im) {
		case HIRAKANA:
			mInputMode = HIRAKANA;
			icon = R.drawable.immodeic_hiragana;
			if (mUseSoftKeyboard) {
				if (mInputView != null) {
					((FlickJPKeyboardView)mInputView).setHiraganaMode();
				}
			}
			break;
		case KATAKANA:
			mInputMode = KATAKANA;
			icon = R.drawable.immodeic_katakana;
			if (mUseSoftKeyboard) {
				if (mInputView != null) {
					((FlickJPKeyboardView)mInputView).setKatakanaMode();
				}
			}
			break;
		case KANJI:
			mInputMode = KANJI;
			break;
		case CHOOSE:
			mInputMode = CHOOSE;
			break;
		case ZENKAKU:
			mInputMode = ZENKAKU;
			icon = R.drawable.immodeic_full_alphabet;
			break;
		case ENG2JP:
			mInputMode = ENG2JP;
			icon = R.drawable.immodeic_eng2jp;
			setComposingTextSKK("", 1);
			break;
		case OKURIGANA:
			mInputMode = OKURIGANA;
			break;
		default:
			break;
		}

		if (mUseSoftKeyboard) {
			hideStatusIcon();
		} else if (icon != 0) {
			showStatusIcon(icon);
		}
		if (isRegistering && doReset) {
			setComposingTextSKK("", 1);
		}
		// ComposingTextのflush回避のためreset()で一旦消してるので，登録中はここまで来てからComposingText復活
	}
}
