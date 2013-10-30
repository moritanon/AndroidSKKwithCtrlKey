package jp.deadend.noname.skk;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class FileChooser extends ListActivity {
	public static final String KEY_DIRNAME	= "FileChooserDirName";
	public static final String KEY_FILENAME	= "FileChooserFileName";
	public static final String KEY_FILEPATH	= "FileChooserFilePath";
	public static final String KEY_FONTSIZE	= "FileChooserFontSize";
	private static final int DEFAULT_FONTSIZE = 16;
	public static final String KEY_MODE		= "FileChooserMode";
	public static final String MODE_OPEN	= "ModeOPEN";
	public static final String MODE_SAVE	= "ModeSAVE";
	public static final String MODE_DIR		= "ModeDIR";

	private File mCurrentDir = null;
	private int mFontSize = DEFAULT_FONTSIZE;
	private String mMode = null;
	private Toast mSearchToast = null;
	private StringBuilder mSearchString = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.filechooser);

		Bundle extras = getIntent().getExtras();
		if (extras == null) {
			finish();
		} else {
			String m = extras.getString(KEY_MODE);
			if (m == null) {
				finish();
			} else if (m.equals(MODE_SAVE)) {
				setTitle(getString(R.string.label_filechooser_save_as));
				mMode = MODE_SAVE;
			} else if (m.equals(MODE_OPEN)) {
				setTitle(getString(R.string.label_filechooser_open));
				mMode = MODE_OPEN;
			} else if (m.equals(MODE_DIR)) {
				setTitle(getString(R.string.label_filechooser_opendir));
				findViewById(R.id.EditTextFileName).setVisibility(View.GONE);
				mMode = MODE_DIR;
			} else {
				finish();
			}

			mFontSize = extras.getInt(KEY_FONTSIZE, DEFAULT_FONTSIZE);

			String dirName = extras.getString(KEY_DIRNAME);
			String defaultDir = Environment.getExternalStorageDirectory().getPath();
			if (dirName == null) {
				mCurrentDir = new File(defaultDir);
			} else {
				mCurrentDir = new File(dirName);
				if (!(mCurrentDir.isDirectory() && mCurrentDir.canRead())) {
					mCurrentDir = new File(defaultDir);
				}
			}
			fillList();
		}

		findViewById(R.id.ButtonOK).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();

				String dirName = mCurrentDir.getAbsolutePath() + "/";
				if (mMode.equals(MODE_DIR)) {
					intent.putExtra(KEY_FILENAME, "");
					intent.putExtra(KEY_DIRNAME, dirName);
					intent.putExtra(KEY_FILEPATH, dirName);
					setResult(RESULT_OK, intent);
				} else {
					String fileName = ((EditText)findViewById(R.id.EditTextFileName)).getText().toString();
					if (fileName.length() == 0) {
						setResult(RESULT_CANCELED, intent);
					} else {
						intent.putExtra(KEY_FILENAME, fileName);
						intent.putExtra(KEY_DIRNAME, dirName);

						String filePath = dirName + fileName;
						intent.putExtra(KEY_FILEPATH, filePath);
						setResult(RESULT_OK, intent);
					}
				}
				finish();
			}
		});

		((Button)findViewById(R.id.ButtonCancel)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				setResult(RESULT_CANCELED, intent);
				finish();
			}
		});

		// quick search
		mSearchString = new StringBuilder();
		mSearchToast = Toast.makeText(FileChooser.this, "", Toast.LENGTH_SHORT);
		mSearchToast.setGravity(Gravity.BOTTOM | Gravity.RIGHT, 10, 10);
		getListView().setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN) {
					char label = event.getDisplayLabel();
					if ((label >= 65 && label <= 90) || (label >= 97 && label <= 122)) {
						char keyChar = Character.toLowerCase(label);
						mSearchString.append(keyChar);
						String str = new String(mSearchString);
						mSearchToast.setText(str);
						mSearchToast.show();

						ListView lv = (ListView)v;
						int startIndex = lv.getSelectedItem() == null ? 0 : lv.getSelectedItemPosition();
						for (int i=startIndex; i < lv.getCount(); i++) {
							if(((String)lv.getItemAtPosition(i)).toLowerCase(Locale.US).startsWith(str)) {
								lv.setSelection(i);
								return true;
							}
						}
						if (startIndex > 0) { // restart from the top
							for (int i=0; i < startIndex-1; i++) {
								if(((String)lv.getItemAtPosition(i)).toLowerCase(Locale.US).startsWith(str)) {
									lv.setSelection(i);
									return true;
								}
							}
						}
						return true;
					} else if (event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
						mSearchString.deleteCharAt(mSearchString.length()-1);
						mSearchToast.setText(new String(mSearchString));
						mSearchToast.show();
						return true;
					} else {
						mSearchString = new StringBuilder();
					}
				}

				return false;
			}
		});
	}

	@Override
	protected void onListItemClick(ListView list, View view, int position, long id) {
		super.onListItemClick(list, view, position, id);
		String item = (String)getListAdapter().getItem(position);
		EditText editText = (EditText)findViewById(R.id.EditTextFileName);

		if (item.equals("..")) {
			checkAndChangeDir(mCurrentDir.getParentFile());
			fillList();
			editText.setText("");
		} else if (item.substring(item.length() - 1).equals("/")) {
			checkAndChangeDir(new File(mCurrentDir, item));
			fillList();
			editText.setText("");
		} else {
			if (mMode.equals(MODE_OPEN)) {
				editText.setText(item);
				findViewById(R.id.ButtonOK).requestFocus();
			} else if (mMode.equals(MODE_SAVE)) {
				editText.setText(item);
				editText.requestFocus();
			}
		}
	}

	private void checkAndChangeDir(File newDir) {
		if (newDir.isDirectory() && newDir.canRead()) {
			mCurrentDir = newDir;
		} else {
			Toast.makeText(FileChooser.this, getString(R.string.error_access_failed, newDir.getAbsolutePath()), Toast.LENGTH_SHORT).show();
		}
	}

	private void fillList() {
		File[] filesArray = mCurrentDir.listFiles();
		if (filesArray == null) {
			Toast.makeText(FileChooser.this, getString(R.string.error_access_failed, mCurrentDir.getAbsolutePath()), Toast.LENGTH_SHORT).show();
			return;
		}

		TextView tv = (TextView)findViewById(R.id.TextDirName);
		tv.setText(mCurrentDir.getAbsolutePath());
		tv.setTextSize(mFontSize+2);

		List<String> dirs = new ArrayList<String>();
		List<String> files = new ArrayList<String>();
		for (File file : filesArray) {
			if (file.isDirectory()) {
				dirs.add(file.getName() + "/");
			} else {
				files.add(file.getName());
			}
		}
		Collections.sort(dirs);
		Collections.sort(files);
		if (!mCurrentDir.getAbsolutePath().equals("/")) {
			dirs.add(0, "..");
		}

		List<String> items = new ArrayList<String>();
		items.addAll(dirs);
		items.addAll(files);

		ArrayAdapter<String> fileList;
		if (mMode.equals(MODE_DIR)) {
			fileList = new ArrayAdapter<String>(this, R.layout.filechooser_row, items) {
				@Override
				public View getView(int position, View convertView, ViewGroup parent) {
					TextView view = (TextView)super.getView(position, convertView, parent);
					view.setTextSize(mFontSize);

					String text = view.getText().toString();
					if (!(text.equals("..") || text.substring(text.length() - 1).equals("/"))) {
						// not a directory
						view.setTextColor(0xFF808080);
					}
					return view;
				}
			};
		} else {
			fileList = new ArrayAdapter<String>(this, R.layout.filechooser_row, items) {
				@Override
				public View getView(int position, View convertView, ViewGroup parent) {
					TextView view = (TextView)super.getView(position, convertView, parent);
					view.setTextSize(mFontSize);
					return view;
				}
			};
		}
		setListAdapter(fileList);
	}
}