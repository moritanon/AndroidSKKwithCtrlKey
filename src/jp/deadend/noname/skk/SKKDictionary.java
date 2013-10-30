package jp.deadend.noname.skk;

import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.btree.BTree;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;

class SKKDictionary {
	static final String BTREE_NAME = "skk_dict";
	String mDicFile;

	RecordManager mRecMan;
	long          mRecID;
	protected BTree mBTree;
	protected boolean isValid;

	SKKDictionary() {
	}

	SKKDictionary(String dic) {
		isValid = true;
		mDicFile = dic;

		try {
			mRecMan = RecordManagerFactory.createRecordManager(mDicFile);
			mRecID = mRecMan.getNamedObject(BTREE_NAME);

			if (mRecID == 0) {
				Log.e("SKK", "Dictionary not found: " + mDicFile);
				isValid = false;
			}

			mBTree = BTree.load(mRecMan, mRecID);
		} catch (Exception e) {
			Log.e("SKK", "Error in opening the dictionary: " + e.toString());
			isValid = false;
		}
	}

	boolean isValid() {
		return isValid;
	}

	List<String> getCandidates(String key) {
		List<String> list = new ArrayList<String>();
		String[] va_array;

		SKKUtils.dlog("findValue(): key = " + key);

		try {
			String value = (String)mBTree.find(key);
			if (value == null) return null;

			va_array = value.split("/");
			SKKUtils.dlog("dic: " + mDicFile + " " + value);
			SKKUtils.dlog("length = " + va_array.length);

			if (va_array.length <= 0) {
				Log.e("SKK", "Invalid value found: Key=" + key + " value=" + value);
				return null;
			}

			// va_array[0]は常に空文字列なので1から始める
			for (int i=1; i<va_array.length; i++) {
				list.add(va_array[i]);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return list;
	}

	void findKeys(String key, boolean isKanji, List<String> list) {
		Tuple         tuple = new Tuple();
		TupleBrowser  browser;

		try {
			browser = mBTree.browse(key);
			if (browser.getNext(tuple) == false) return;
			// 最初の一つがkeyと同じ場合listに追加しない
			String first = (String)tuple.getKey();
			if (!first.equals(key)) list.add(first);

			if (!isKanji) {
				for (int i=0; i<5; i++) {
					if (browser.getNext(tuple) == false) break;
					list.add((String)tuple.getKey());
				}
			} else {
				int klen = key.length();
				String str = null;
				for (int i=0; i<5; i++) {
					if (browser.getNext(tuple) == false) break;
					str = (String)tuple.getKey();
					if ((str.length() == klen+1) && SKKUtils.isAlphabet(str.charAt(klen))) continue;
					list.add(str);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}