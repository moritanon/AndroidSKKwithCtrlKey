package jp.deadend.noname.skk;

import android.content.Context;
import android.content.res.Resources;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager.LayoutParams;
import android.util.FloatMath;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static jp.deadend.noname.skk.InputMode.*;

public class FlickJPKeyboardView extends KeyboardView implements KeyboardView.OnKeyboardActionListener {
	private static final int KEYCODE_FLICK_JP_CHAR_A	= -201;
	private static final int KEYCODE_FLICK_JP_CHAR_KA	= -202;
	private static final int KEYCODE_FLICK_JP_CHAR_SA	= -203;
	private static final int KEYCODE_FLICK_JP_CHAR_TA	= -204;
	private static final int KEYCODE_FLICK_JP_CHAR_NA	= -205;
	private static final int KEYCODE_FLICK_JP_CHAR_HA	= -206;
	private static final int KEYCODE_FLICK_JP_CHAR_MA	= -207;
	private static final int KEYCODE_FLICK_JP_CHAR_YA	= -208;
	private static final int KEYCODE_FLICK_JP_CHAR_RA	= -209;
	private static final int KEYCODE_FLICK_JP_CHAR_WA	= -210;
	private static final int KEYCODE_FLICK_JP_CHAR_TEN	= -211;
	private static final int KEYCODE_FLICK_JP_NONE		= -1000;
	private static final int KEYCODE_FLICK_JP_LEFT		= -1001;
	private static final int KEYCODE_FLICK_JP_RIGHT		= -1002;
	private static final int KEYCODE_FLICK_JP_TOQWERTY	= -1003;
	private static final int KEYCODE_FLICK_JP_SPACE		= -1004;
	private static final int KEYCODE_FLICK_JP_MOJI		= -1005;
	private static final int KEYCODE_FLICK_JP_KOMOJI	= -1006;
	private static final int KEYCODE_FLICK_JP_ENTER		= -1007;
	private static final int KEYCODE_FLICK_JP_SEARCH	= -1008;
	private static final int FLICK_DIRECTION_NONE		= 0;
	private static final int FLICK_DIRECTION_LEFT		= 1;
	private static final int FLICK_DIRECTION_UP			= 2;
	private static final int FLICK_DIRECTION_RIGHT		= 3;
	private static final int FLICK_DIRECTION_DOWN		= 4;
	private static final int FLICK_CURVE_NONE			= 0;
	private static final int FLICK_CURVE_LEFT			= 1;
	private static final int FLICK_CURVE_RIGHT			= 2;

	private SKKEngine mService;

	private GestureDetector mDetector;
	private int mFlickSensitivity = 10;
	private int mLastPressedKey = KEYCODE_FLICK_JP_NONE;
	private int mLastFlickDir = FLICK_DIRECTION_NONE;
	private int mLastCurveFlickDir = FLICK_CURVE_NONE;
	private float mFirstMoveEndX = -1;
	private float mFirstMoveEndY = -1;

	private SKKKeyboard mHiraganaKeyboard;
	private SKKKeyboard mKatakanaKeyboard;

	private boolean mUsePopup = true;
	private boolean mFixedPopup = false;
	private PopupWindow[] mPopupFlickGuide = new PopupWindow[7];
	private TextView[] mPopupTextView = new TextView[7];
	private int[] mFlickGuideOffset = null;
	private int mPopupSize = -1;
	private int mPopupCenterX = -1;
	private int mPopupCenterY = -1;
	//フリックガイドTextView用
	private SparseArray<String[]> mFlickGuideLabelList = new SparseArray<String[]>();
	{
		SparseArray<String[]> a = mFlickGuideLabelList;
		a.append(KEYCODE_FLICK_JP_CHAR_A,	new String[]{"あ", "い", "う", "え", "お", "小", ""});
		a.append(KEYCODE_FLICK_JP_CHAR_KA,	new String[]{"か", "き", "く", "け", "こ", "",   "゛"});
		a.append(KEYCODE_FLICK_JP_CHAR_SA,	new String[]{"さ", "し", "す", "せ", "そ", "",   "゛"});
		a.append(KEYCODE_FLICK_JP_CHAR_TA,	new String[]{"た", "ち", "つ", "て", "と", "",   "゛"});
		a.append(KEYCODE_FLICK_JP_CHAR_NA,	new String[]{"な", "に", "ぬ", "ね", "の", "",   ""});
		a.append(KEYCODE_FLICK_JP_CHAR_HA,	new String[]{"は", "ひ", "ふ", "へ", "ほ", "゜", "゛"});
		a.append(KEYCODE_FLICK_JP_CHAR_MA,	new String[]{"ま", "み", "む", "め", "も", "",   ""});
		a.append(KEYCODE_FLICK_JP_CHAR_YA,	new String[]{"や", "",   "ゆ", "",   "よ", "小", ""});
		a.append(KEYCODE_FLICK_JP_CHAR_RA,	new String[]{"ら", "り", "る", "れ", "ろ", "",   ""});
		a.append(KEYCODE_FLICK_JP_CHAR_WA,	new String[]{"わ", "を", "ん", "ー", "「", "",   ""});
		a.append(KEYCODE_FLICK_JP_CHAR_TEN,	new String[]{"、", "。", "？", "！", "」", "",   ""});
		a.append(KEYCODE_FLICK_JP_KOMOJI,	new String[]{"小", "゛", "",   "゜", "",   "",   ""});
	}

	// かな小文字変換用
	private Map<String, String> mSmallKanaMap = new HashMap<String, String>();
	{
		Map<String, String> m = mSmallKanaMap;
		m.put("あ", "ぁ");m.put("い", "ぃ");m.put("う", "ぅ");m.put("え", "ぇ");m.put("お", "ぉ");
		m.put("ぁ", "あ");m.put("ぃ", "い");m.put("ぅ", "う");m.put("ぇ", "え");m.put("ぉ", "お");
		m.put("や", "ゃ");m.put("ゆ", "ゅ");m.put("よ", "ょ");m.put("つ", "っ");
		m.put("ゃ", "や");m.put("ゅ", "ゆ");m.put("ょ", "よ");m.put("っ", "つ");
		m.put("ア", "ァ");m.put("イ", "ィ");m.put("ウ", "ゥ");m.put("エ", "ェ");m.put("オ", "ォ");
		m.put("ァ", "ア");m.put("ィ", "イ");m.put("ゥ", "ウ");m.put("ェ", "エ");m.put("ォ", "オ");
		m.put("ヤ", "ャ");m.put("ユ", "ュ");m.put("ヨ", "ョ");m.put("ツ", "ッ");
		m.put("ャ", "ヤ");m.put("ュ", "ユ");m.put("ョ", "ヨ");m.put("ッ", "ツ");
	}
	// 濁音変換用
	private Map<String, String> mDakutenMap = new HashMap<String, String>();
	{
		Map<String, String> m = mDakutenMap;
		m.put("か", "が");m.put("き", "ぎ");m.put("く", "ぐ");m.put("け", "げ");m.put("こ", "ご");
		m.put("が", "か");m.put("ぎ", "き");m.put("ぐ", "く");m.put("げ", "け");m.put("ご", "こ");
		m.put("さ", "ざ");m.put("し", "じ");m.put("す", "ず");m.put("せ", "ぜ");m.put("そ", "ぞ");
		m.put("ざ", "さ");m.put("じ", "し");m.put("ず", "す");m.put("ぜ", "せ");m.put("ぞ", "そ");
		m.put("た", "だ");m.put("ち", "ぢ");m.put("つ", "づ");m.put("て", "で");m.put("と", "ど");
		m.put("だ", "た");m.put("ぢ", "ち");m.put("づ", "つ");m.put("で", "て");m.put("ど", "と");
		m.put("は", "ば");m.put("ひ", "び");m.put("ふ", "ぶ");m.put("へ", "べ");m.put("ほ", "ぼ");
		m.put("ば", "は");m.put("び", "ひ");m.put("ぶ", "ふ");m.put("べ", "へ");m.put("ぼ", "ほ");
		m.put("カ", "ガ");m.put("キ", "ギ");m.put("ク", "グ");m.put("ケ", "ゲ");m.put("コ", "ゴ");
		m.put("ガ", "カ");m.put("ギ", "キ");m.put("グ", "ク");m.put("ゲ", "ケ");m.put("ゴ", "コ");
		m.put("サ", "ザ");m.put("シ", "ジ");m.put("ス", "ズ");m.put("セ", "セ");m.put("ソ", "ゾ");
		m.put("ザ", "サ");m.put("ジ", "シ");m.put("ズ", "ス");m.put("ゼ", "ゼ");m.put("ゾ", "ソ");
		m.put("タ", "ダ");m.put("チ", "ヂ");m.put("ツ", "ヅ");m.put("テ", "デ");m.put("ト", "ド");
		m.put("ダ", "タ");m.put("ヂ", "チ");m.put("ヅ", "ツ");m.put("デ", "テ");m.put("ド", "ト");
		m.put("ハ", "バ");m.put("ヒ", "ビ");m.put("フ", "ブ");m.put("ヘ", "ベ");m.put("ホ", "ボ");
		m.put("バ", "ハ");m.put("ビ", "ヒ");m.put("ブ", "フ");m.put("ベ", "ヘ");m.put("ボ", "ホ");
		m.put("ウ", "ヴ");m.put("ヴ", "ウ");
	}
	// 濁音変換用
	private Map<String, String> mHanDakutenMap = new HashMap<String, String>();
	{
		Map<String, String> m = mHanDakutenMap;
		m.put("は", "ぱ");m.put("ひ", "ぴ");m.put("ふ", "ぷ");m.put("へ", "ぺ");m.put("ほ", "ぽ");
		m.put("ぱ", "は");m.put("ぴ", "ひ");m.put("ぷ", "ふ");m.put("ぺ", "へ");m.put("ぽ", "ほ");
		m.put("ハ", "パ");m.put("ヒ", "ピ");m.put("フ", "プ");m.put("ヘ", "ペ");m.put("ホ", "ポ");
		m.put("パ", "ハ");m.put("ピ", "ヒ");m.put("プ", "フ");m.put("ペ", "ヘ");m.put("ポ", "ホ");
	}

	public FlickJPKeyboardView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setup(context);
	}

	public FlickJPKeyboardView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setup(context);
	}

	private void setup(Context context) {
		mFlickSensitivity = SKKPrefs.getFlickSensitivity(context);
		int keyHeight = SKKPrefs.getKeyHeight(context);
		mHiraganaKeyboard = new SKKKeyboard(context, R.xml.keys_flick_jp, keyHeight);
		mKatakanaKeyboard = new SKKKeyboard(context, R.xml.keys_flick_jp_katakana, keyHeight);
		setKeyboard(mHiraganaKeyboard);
		setOnKeyboardActionListener(this);
		mDetector = new GestureDetector(context, new FlickGestureListener());
		setPreviewEnabled(false);
		mUsePopup = SKKPrefs.getUsePopup(context);
		if (mUsePopup) {
			mFixedPopup = SKKPrefs.getFixedPopup(context);
			for (int i=0; i<7; i++) {
				mPopupFlickGuide[i] = createPopupGuide(context);
				mPopupTextView[i] = (TextView)mPopupFlickGuide[i].getContentView().findViewById(R.id.Label);
			}
			String kutouten = SKKPrefs.getPrefKutoutenType(context);
			if (kutouten.equals("en")) {
				mFlickGuideLabelList.put(KEYCODE_FLICK_JP_CHAR_TEN,	new String[]{"，", "．", "？", "！", "」", "",   ""});
			} else if (kutouten.equals("jp_en")) {
				mFlickGuideLabelList.put(KEYCODE_FLICK_JP_CHAR_TEN,	new String[]{"，", "。", "？", "！", "」", "",   ""});
			}
		}
	}

	public void setService(SKKEngine listener) {
		mService = listener;
	}

	void setHiraganaMode() {
		setKeyboard(mHiraganaKeyboard);
	}

	void setKatakanaMode() {
		setKeyboard(mKatakanaKeyboard);
	}

	private PopupWindow createPopupGuide(Context context) {
		View view = ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.popup_flickguide, null);

		PopupWindow popup = new PopupWindow(context);
		//~ popup.setWindowLayoutMode(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		popup.setWindowLayoutMode(0, 0);
		popup.setContentView(view);
		popup.setAnimationStyle(0);
		popup.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.popup_frame));

		return popup;
	}

	private void calculatePopupGeometry() {
		Keyboard.Key key = mHiraganaKeyboard.getKeys().get(0);
		int size = Math.min(key.width, key.height);
		for (int i=0; i<5; i++) {
			mPopupFlickGuide[i].setWidth(size);
			mPopupFlickGuide[i].setHeight(size);
			mPopupTextView[i].setTextSize(TypedValue.COMPLEX_UNIT_PX, size-20);
		}
		mPopupFlickGuide[5].setWidth(size/2);
		mPopupFlickGuide[5].setHeight(size/2);
		mPopupTextView[5].setTextSize(TypedValue.COMPLEX_UNIT_PX, size/2-8);
		mPopupFlickGuide[6].setWidth(size/2);
		mPopupFlickGuide[6].setHeight(size/2);
		mPopupTextView[6].setTextSize(TypedValue.COMPLEX_UNIT_PX, size/2-8);
		mPopupSize = size;

		int[] offsetInWindow = new int[2];
		getLocationInWindow(offsetInWindow);
		int[] windowLocation = new int[2];
		getLocationOnScreen(windowLocation);
		mFlickGuideOffset = new int[2];
		mFlickGuideOffset[0] = -mPopupSize/2;
		mFlickGuideOffset[1] = -windowLocation[1] + offsetInWindow[1] - mPopupSize/2;

		if (mFixedPopup) {
			mPopupCenterX = this.getWidth()/2;
			mPopupCenterY = windowLocation[1] - mPopupSize*2;
		}
	}

	private void highlightPopup(int n) {
		for (int i=0; i<7; i++) {
			mPopupTextView[i].setBackgroundResource(R.drawable.popup_label);
		}
		mPopupTextView[n].setBackgroundResource(R.drawable.popup_label_highlighted);
	}

	private void procesFirstFlick(float theta) {
		String[] labelList = mFlickGuideLabelList.get(mLastPressedKey);
		if (labelList == null) return;
		boolean hasLeftCurve =  (labelList[5].length() != 0);
		boolean hasRightCurve = (labelList[6].length() != 0);

		if ((theta >= 45.0f) && (theta < 112.5f)) {
			mLastFlickDir = FLICK_DIRECTION_RIGHT;
		} else if ((theta >= 112.5f) && (theta < 157.5f)) {
			if (!hasRightCurve) return;
			mLastCurveFlickDir = FLICK_CURVE_RIGHT;
		} else if ((theta >= 157.5f) && (theta < 202.5f)) {
			mLastFlickDir = FLICK_DIRECTION_UP;
		} else if ((theta >= 202.5f) && (theta < 247.5f)) {
			if (!hasLeftCurve) return;
			mLastCurveFlickDir = FLICK_CURVE_LEFT;
		} else if ((theta >= 247.5f) && (theta < 315.0f)) {
			mLastFlickDir = FLICK_DIRECTION_LEFT;
		} else {
			mLastFlickDir = FLICK_DIRECTION_DOWN;
		}

		if (!mUsePopup) return;

		if (mLastCurveFlickDir == FLICK_CURVE_LEFT) {
			highlightPopup(5);
			return;
		} else if (mLastCurveFlickDir == FLICK_CURVE_RIGHT) {
			highlightPopup(6);
			return;
		}

		int posX, posY;
		switch (mLastFlickDir) {
		case FLICK_DIRECTION_LEFT:
			if (mPopupFlickGuide[1].isShowing()) {
				highlightPopup(1);
				posX = mPopupCenterX + mFlickGuideOffset[0] - mPopupSize - mPopupSize/2;
				posY = mPopupCenterY + mFlickGuideOffset[1] + mPopupSize;
				mPopupFlickGuide[5].update(posX, posY, -1, -1);
				posY = mPopupCenterY + mFlickGuideOffset[1] - mPopupSize/2;
				mPopupFlickGuide[6].update(posX, posY, -1, -1);
			}
			break;
		case FLICK_DIRECTION_UP:
			if (mPopupFlickGuide[2].isShowing()) {
				highlightPopup(2);
				posX = mPopupCenterX + mFlickGuideOffset[0] - mPopupSize/2;
				posY = mPopupCenterY + mFlickGuideOffset[1] - mPopupSize - mPopupSize/2;
				if (mLastPressedKey == KEYCODE_FLICK_JP_CHAR_TA) {
					// 例外：小さい「っ」
					mPopupTextView[5].setText("小");
					mPopupFlickGuide[5].showAtLocation(FlickJPKeyboardView.this, android.view.Gravity.NO_GRAVITY, posX, posY);
				} else {
					mPopupFlickGuide[5].update(posX, posY, -1, -1);
				}
				posX = mPopupCenterX + mFlickGuideOffset[0] + mPopupSize;
				mPopupFlickGuide[6].update(posX, posY, -1, -1);
			}
			break;
		case FLICK_DIRECTION_RIGHT:
			if (mPopupFlickGuide[3].isShowing()) {
				highlightPopup(3);
				posX = mPopupCenterX + mFlickGuideOffset[0] + 2*mPopupSize;
				posY = mPopupCenterY + mFlickGuideOffset[1] - mPopupSize/2;
				mPopupFlickGuide[5].update(posX, posY, -1, -1);
				posY = mPopupCenterY + mFlickGuideOffset[1] + mPopupSize;
				mPopupFlickGuide[6].update(posX, posY, -1, -1);
			}
			break;
		case FLICK_DIRECTION_DOWN:
			if (mPopupFlickGuide[4].isShowing()) {
				highlightPopup(4);
				posX = mPopupCenterX + mFlickGuideOffset[0] + mPopupSize;
				posY = mPopupCenterY + mFlickGuideOffset[1] + 2*mPopupSize;
				mPopupFlickGuide[5].update(posX, posY, -1, -1);
				posX = mPopupCenterX + mFlickGuideOffset[0] - mPopupSize/2;
				mPopupFlickGuide[6].update(posX, posY, -1, -1);
			}
			break;
		}
	}

	private void processCurveFlick(int lastFlickDir, float theta) {
		String[] labelList = mFlickGuideLabelList.get(mLastPressedKey);
		if (labelList == null) return;
		boolean hasLeftCurve =  (labelList[5].length() != 0);
		boolean hasRightCurve = (labelList[6].length() != 0);
		if (!hasLeftCurve && !hasRightCurve) return;

		SKKUtils.dlog("processCurveFlick: dir=" + lastFlickDir + " theta=" + theta);
		switch (lastFlickDir) {
		case FLICK_DIRECTION_LEFT:
			if (((theta >= 292.5f) && (theta < 360.0f)) || ((theta >= 0.0f) && (theta < 22.5f))) {
				mLastCurveFlickDir = FLICK_CURVE_LEFT;
			} else if ((theta >= 157.5f) && (theta < 247.5f)) {
				mLastCurveFlickDir = FLICK_CURVE_RIGHT;
			}
			break;
		case FLICK_DIRECTION_DOWN:
			if ((theta >= 22.5f) && (theta < 112.5f)) {
				mLastCurveFlickDir = FLICK_CURVE_LEFT;
			} else if ((theta >= 247.5f) && (theta < 337.5f)) {
				mLastCurveFlickDir = FLICK_CURVE_RIGHT;
			}
			break;
		case FLICK_DIRECTION_RIGHT:
			if ((theta >= 112.5f) && (theta < 202.5f)) {
				mLastCurveFlickDir = FLICK_CURVE_LEFT;
			} else if (((theta >= 0.0f) && (theta < 67.5f)) || ((theta >= 337.5f) && (theta < 360.0f))) {
				mLastCurveFlickDir = FLICK_CURVE_RIGHT;
			}
			break;
		case FLICK_DIRECTION_UP:
			if ((theta >= 202.5f) && (theta < 292.5f)) {
				mLastCurveFlickDir = FLICK_CURVE_LEFT;
			} else if ((theta >= 67.5f) && (theta < 157.5f)) {
				mLastCurveFlickDir = FLICK_CURVE_RIGHT;
			}
			break;
		}

		if (mLastCurveFlickDir == FLICK_CURVE_LEFT) {
			//ここも小さい「っ」は特別処理
			if (!hasLeftCurve && (mLastPressedKey != KEYCODE_FLICK_JP_CHAR_TA || lastFlickDir != FLICK_DIRECTION_UP)) {
				mLastCurveFlickDir = FLICK_CURVE_NONE;
				return;
			}
		}
		if (mLastCurveFlickDir == FLICK_CURVE_RIGHT && !hasRightCurve) {
			mLastCurveFlickDir = FLICK_CURVE_NONE;
			return;
		}

		if (mLastCurveFlickDir == FLICK_CURVE_NONE || !mUsePopup) return;

		if (mLastCurveFlickDir == FLICK_CURVE_LEFT && mPopupFlickGuide[5].isShowing()) {
			highlightPopup(5);
		} else if (mLastCurveFlickDir == FLICK_CURVE_RIGHT && mPopupFlickGuide[6].isShowing()) {
			highlightPopup(6);
		}
	}

	private void processFlickForLetter(int keyCode, int direction, int curve_dir, boolean isShifted) {
		int vowel ='a';
		switch (direction) {
		case FLICK_DIRECTION_LEFT:
			vowel = 'i';
			break;
		case FLICK_DIRECTION_UP:
			vowel = 'u';
			break;
		case FLICK_DIRECTION_RIGHT:
			vowel = 'e';
			break;
		case FLICK_DIRECTION_DOWN:
			vowel = 'o';
			break;
		}

		int consonant = -1;
		switch (keyCode) {
		case KEYCODE_FLICK_JP_CHAR_A:
			if (curve_dir == FLICK_CURVE_LEFT) {
				mService.processKey('x');
				mService.processKey(vowel);
			} else if (isShifted) {
				mService.processKey(Character.toUpperCase(vowel));
			} else {
				mService.processKey(vowel);
			}
			return;
		case KEYCODE_FLICK_JP_CHAR_KA:
			if (curve_dir == FLICK_CURVE_RIGHT) {
				consonant = 'g';
			} else {
				consonant = 'k';
			}
			break;
		case KEYCODE_FLICK_JP_CHAR_SA:
			if (curve_dir == FLICK_CURVE_RIGHT) {
				consonant = 'z';
			} else {
				consonant = 's';
			}
			break;
		case KEYCODE_FLICK_JP_CHAR_TA:
			if (curve_dir == FLICK_CURVE_RIGHT) {
				consonant = 'd';
			} else {
				consonant = 't';
			}
			break;
		case KEYCODE_FLICK_JP_CHAR_NA:
			consonant = 'n';
			break;
		case KEYCODE_FLICK_JP_CHAR_HA:
			if (curve_dir == FLICK_CURVE_RIGHT) {
				consonant = 'b';
			} else if (curve_dir == FLICK_CURVE_LEFT) {
				consonant = 'p';
			} else {
				consonant = 'h';
			}
			break;
		case KEYCODE_FLICK_JP_CHAR_MA:
			consonant = 'm';
			break;
		case KEYCODE_FLICK_JP_CHAR_YA:
			consonant = 'y';
			break;
		case KEYCODE_FLICK_JP_CHAR_RA:
			consonant = 'r';
			break;
		case KEYCODE_FLICK_JP_CHAR_WA:
			switch (direction) {
			case FLICK_DIRECTION_NONE:
				if (isShifted) {
					mService.processKey('W');
				} else {
					mService.processKey('w');
				}
				mService.processKey('a');
				break;
			case FLICK_DIRECTION_LEFT:
				mService.processKey('w');
				mService.processKey('o');
				break;
			case FLICK_DIRECTION_UP:
				if (isShifted) {
					mService.processKey('N');
				} else {
					mService.processKey('n');
				}
				mService.processKey('n');
				break;
			case FLICK_DIRECTION_RIGHT:
				mService.processKey('-');
				break;
			case FLICK_DIRECTION_DOWN:
				mService.processKey('[');
				break;
			}
			return;
		case KEYCODE_FLICK_JP_CHAR_TEN:
			switch (direction) {
			case FLICK_DIRECTION_NONE:
				mService.processKey(',');
				break;
			case FLICK_DIRECTION_LEFT:
				mService.processKey('.');
				break;
			case FLICK_DIRECTION_UP:
				mService.processKey('?');
				break;
			case FLICK_DIRECTION_RIGHT:
				mService.processKey('!');
				break;
			case FLICK_DIRECTION_DOWN:
				mService.processKey(']');
				break;
			}
			return;
		default:
			return;
		}

		if (isShifted) {
			mService.processKey(Character.toUpperCase(consonant));
		} else {
			mService.processKey(consonant);
		}
		mService.processKey(vowel);

		if (curve_dir == FLICK_CURVE_LEFT) {
			if ((consonant == 't' && vowel == 'u') || (consonant == 'y' && (vowel == 'a' || vowel == 'u' || vowel == 'o'))) {
				mService.changeLastChar(mSmallKanaMap);
			}
		}
	}

	@Override
	protected boolean onLongPress(Keyboard.Key key) {
		int code = key.codes[0];
		if (code == KEYCODE_FLICK_JP_ENTER) {
			mService.keyDownUp(KeyEvent.KEYCODE_SEARCH);
			return true;
		} else if (code == KEYCODE_FLICK_JP_SPACE) {
			mService.sendToMushroom();
			return true;
		}

		return super.onLongPress(key);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		mDetector.onTouchEvent(event);
		return super.onTouchEvent(event);
	}

	@Override
	public void onPress(int primaryCode) {
		if (mLastFlickDir == FLICK_DIRECTION_NONE) {
			mLastPressedKey = primaryCode;
		}

		if (mUsePopup) {
			if (mFlickGuideOffset == null) {
				calculatePopupGeometry();
			}

			String[] labelList = mFlickGuideLabelList.get(primaryCode);
			if (labelList == null) return;

			highlightPopup(0);
			for (int i=0; i<7; i++) {
				if (labelList[i].length() == 0) continue;
				int posX = mPopupCenterX + mFlickGuideOffset[0];
				int posY = mPopupCenterY + mFlickGuideOffset[1];
				switch (i) {
				case 1:
					posX -= mPopupSize;
					break;
				case 2:
					posY -= mPopupSize;
					break;
				case 3:
					posX += mPopupSize;
					break;
				case 4:
					if (primaryCode == KEYCODE_FLICK_JP_CHAR_WA || primaryCode == KEYCODE_FLICK_JP_CHAR_TEN || primaryCode == KEYCODE_FLICK_JP_KOMOJI) {
						continue;
					}
					posY += mPopupSize;
					break;
				case 5:
					posX -= mPopupSize/2;
					posY -= mPopupSize/2;
					break;
				case 6:
					posX += mPopupSize;
					posY -= mPopupSize/2;
					break;
				}

				String label;
				if (getKeyboard() == mHiraganaKeyboard) {
					label = labelList[i];
				} else {
					label = SKKUtils.hirakana2katakana(labelList[i]);
				}
				mPopupTextView[i].setText(label);
				mPopupFlickGuide[i].showAtLocation(this, android.view.Gravity.NO_GRAVITY, posX, posY);
			}
		}
	}

	@Override
    public void onRelease(int primaryCode) {
		SKKUtils.dlog("onRelease: primaryCode="+primaryCode+"  mLastPressedKey="+mLastPressedKey+"  mLastFlickDir="+mLastFlickDir);

		switch (mLastPressedKey) {
		case Keyboard.KEYCODE_SHIFT:
			setShifted(!isShifted());
			break;
		case Keyboard.KEYCODE_DELETE:
			mService.handleCancel(true);
			break;
		case KEYCODE_FLICK_JP_LEFT:
			mService.handleLeftKey();
			break;
		case KEYCODE_FLICK_JP_RIGHT:
			mService.handleRightKey();
			break;
		case KEYCODE_FLICK_JP_SPACE:
			mService.processKey(' ');
			break;
		case KEYCODE_FLICK_JP_ENTER:
			mService.handleEnter();
			break;
		case KEYCODE_FLICK_JP_KOMOJI:
			if (mLastFlickDir == FLICK_DIRECTION_NONE) {
				mService.changeLastChar(mSmallKanaMap);
			} else if (mLastFlickDir == FLICK_DIRECTION_LEFT) {
				mService.changeLastChar(mDakutenMap);
			} else if (mLastFlickDir == FLICK_DIRECTION_RIGHT) {
				mService.changeLastChar(mHanDakutenMap);
			}
			break;
		case KEYCODE_FLICK_JP_MOJI:
			mService.processKey('q');
			break;
		case KEYCODE_FLICK_JP_TOQWERTY:
			mService.toggleSKK();
			break;
		case KEYCODE_FLICK_JP_CHAR_A:
		case KEYCODE_FLICK_JP_CHAR_KA:
		case KEYCODE_FLICK_JP_CHAR_SA:
		case KEYCODE_FLICK_JP_CHAR_TA:
		case KEYCODE_FLICK_JP_CHAR_NA:
		case KEYCODE_FLICK_JP_CHAR_HA:
		case KEYCODE_FLICK_JP_CHAR_MA:
		case KEYCODE_FLICK_JP_CHAR_YA:
		case KEYCODE_FLICK_JP_CHAR_RA:
		case KEYCODE_FLICK_JP_CHAR_WA:
		case KEYCODE_FLICK_JP_CHAR_TEN:
			processFlickForLetter(mLastPressedKey, mLastFlickDir, mLastCurveFlickDir, isShifted());
			break;
		}

		if (mLastPressedKey != Keyboard.KEYCODE_SHIFT) {
			setShifted(false);
		}

		mLastPressedKey = KEYCODE_FLICK_JP_NONE;
		mLastFlickDir = FLICK_DIRECTION_NONE;
		mLastCurveFlickDir = FLICK_CURVE_NONE;
		mFirstMoveEndX = -1;
		mFirstMoveEndY = -1;
		if (mUsePopup) {
			for (int i=0; i<7; i++) {
				if (mPopupFlickGuide[i].isShowing()) {
					mPopupFlickGuide[i].dismiss();
				}
			}
		}
	}

	private class FlickGestureListener extends GestureDetector.SimpleOnGestureListener {
		@Override
		public boolean onDown(final MotionEvent ev) {
			if (mUsePopup && !mFixedPopup) {
				mPopupCenterX = (int)ev.getRawX();
				mPopupCenterY = (int)ev.getRawY();
			}
			return true;
		}

		@Override
		public boolean onScroll(final MotionEvent ev1, final MotionEvent ev2, final float distX, final float distY) {
			if (mLastCurveFlickDir != FLICK_CURVE_NONE) {
				return true;
			}

			SKKUtils.dlog("onScroll: ev1 X=" + ev1.getX() + " rawX=" + ev1.getRawX() + "  Y=" + ev1.getY() + " rawY=" + ev1.getRawY());
			SKKUtils.dlog("onScroll: ev2 X=" + ev2.getX() + " rawX=" + ev2.getRawX() + "  Y=" + ev2.getY() + " rawY=" + ev2.getRawY());
			SKKUtils.dlog("onScroll: sensitivity=" + mFlickSensitivity + "  mLastFlickDir=" + mLastFlickDir);

			if (mLastFlickDir == FLICK_DIRECTION_NONE) {
				// First move
				float dx = ev2.getRawX() - ev1.getRawX();
				float dy = ev2.getRawY() - ev1.getRawY();
				float length = FloatMath.sqrt(dx*dx + dy*dy);
				if (length < mFlickSensitivity) {
					return true;
				}
				mFirstMoveEndX = ev2.getRawX();
				mFirstMoveEndY = ev2.getRawY();

				float theta = (float)(Math.atan2(dx, dy)*180.0/Math.PI);
				if (theta < 0.0f) {
					theta = 360.0f+theta;
				}

				procesFirstFlick(theta);

				SKKUtils.dlog("onScroll() first move: length=" + length + " theta=" + theta);
				SKKUtils.dlog("onScroll() first move: dir=" + mLastFlickDir + " curve=" + mLastCurveFlickDir);
			} else {
				// Second move
				float dx = ev2.getRawX() - mFirstMoveEndX;
				float dy = ev2.getRawY() - mFirstMoveEndY;
				float length = FloatMath.sqrt(dx*dx + dy*dy);
				if (length < mFlickSensitivity) {
					return true;
				}

				float theta = (float)(Math.atan2(dx, dy)*180.0/Math.PI);
				if (theta < 0.0f) {
					theta = 360.0f+theta;
				}

				processCurveFlick(mLastFlickDir, theta);

				SKKUtils.dlog("onScroll() second move: length=" + length + " theta=" + theta);
				SKKUtils.dlog("onScroll() second move: dir=" + mLastFlickDir + " curve=" + mLastCurveFlickDir);
			}

			return true;
		}
	}

    public void onKey(int primaryCode, int[] keyCodes) {
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