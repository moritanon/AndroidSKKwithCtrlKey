package jp.deadend.noname.skk;

import android.util.Log;

class SKKUtils {
	//~ private static final boolean IS_DEBUG_MODE = true;
	private static final boolean IS_DEBUG_MODE = false;

	// 半角から全角 (UNICODE)
	static int hankaku2zenkaku(int pcode) {
		if (pcode == 0x20) { // スペースだけ、特別
			return 0x3000;
		}
		return pcode - 0x20 + 0xFF00;
	}

	/**
	* 文字列・改
	*
	* @author 佐藤 雅俊さん <okome@siisise.net> http://siisise.net/java/lang/ のコードを改変
	*/
	/**
	* ひらがなを全角カタカナにする
	*/
	static String hirakana2katakana(String str) {
		if (str == null) return null;

		StringBuilder str2 = new StringBuilder();

		for (int i=0; i<str.length(); i++) {
			char ch = str.charAt(i);

			if (ch >= 0x3040 && ch <= 0x309A) {
				ch += 0x60;
			}
			str2.append(ch);
		}

		int idx = str2.indexOf("ウ゛");
		if (idx != -1) str2.replace(idx, idx+2, "ヴ");

		return str2.toString();
	}

	static boolean isAlphabet(int code) {
		return ((code >= 0x41 && code <= 0x5A) || (code >= 0x61 && code <= 0x7A)) ? true : false;
	}

	static boolean isVowel(int code) {
		switch (code) {
		case 'a':
		case 'i':
		case 'u':
		case 'e':
		case 'o':
			return true;
		default:
			return false;
		}
	}

	static String removeAnnotation(String text) {
		int i = text.indexOf(';'); // セミコロンで解説が始まる
		return ((i == -1) ? text : text.substring(0, i));
	}

	// debug log
	static void dlog(String msg) {
		if (IS_DEBUG_MODE) {
			Log.d("SKK", msg);
		}
	}
}