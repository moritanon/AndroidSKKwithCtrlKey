package jp.deadend.noname.skk;

import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.util.Log;

public class SKKKeyboard extends Keyboard {
	private int mTotalHeight;

	public SKKKeyboard(Context context, int xmlLayoutResId, int height) {
		super(context, xmlLayoutResId);

		String kutouten = SKKPrefs.getPrefKutoutenType(context);

		List<Key> keys = this.getKeys();
		int max_y = 0;
		for (int i=0; i<keys.size(); i++) {
			//高さの設定
			Key key = keys.get(i);
			key.y *= height;
			key.height *= height;

			if (max_y < key.y) {
				max_y = key.y;
			}

			//句読点キーのラベル設定
			int[] codes = key.codes;
			if (codes[0] == -211) {
				if (kutouten.equals("en")) {
					key.label = "，．？！";
				} else if (kutouten.equals("jp_en")) {
					key.label = "，。？！";
				}
			}
		}

		mTotalHeight = max_y+height;
	}

	@Override
	public int getHeight() {
		return mTotalHeight;
	}
}
