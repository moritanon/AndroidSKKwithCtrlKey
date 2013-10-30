package jp.deadend.noname.skk;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SKKMushroom extends ListActivity {
	private class AppInfo implements Comparable<AppInfo> {
		Drawable icon;
		String appName;
		String packageName;
		String className;

		public AppInfo(Drawable ic, String na, String pn, String cn) {
			icon = ic;
			appName = na;
			packageName = pn;
			className = cn;
		}

		public int compareTo(AppInfo other) {
			return this.appName.compareTo(other.appName);
		}
	}

	public static final String ACTION_SIMEJI_MUSHROOM = "com.adamrocker.android.simeji.ACTION_INTERCEPT";
	public static final String CATEGORY_SIMEJI_MUSHROOM = "com.adamrocker.android.simeji.REPLACE";

	public static final String ACTION_BROADCAST = "jp.deadend.noname.skk.MUSHROOM_RESULT";
	public static final String CATEGORY_BROADCAST = "jp.deadend.noname.skk.MUSHROOM_VALUE";

	public static final String REPLACE_KEY = "replace_key";

	private String mStr;

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		mStr = getIntent().getExtras().getString(REPLACE_KEY);
		setListAdapter(new AppListAdapter(this, loadMushroomAppList()));
	}

	@Override
	protected void onListItemClick (ListView l, View v, int position, long id) {
		AppInfo info = (AppInfo)l.getItemAtPosition(position);

		Intent intent = new Intent(ACTION_SIMEJI_MUSHROOM);
		intent.addCategory(CATEGORY_SIMEJI_MUSHROOM);
		intent.setClassName(info.packageName, info.className);
		intent.putExtra(REPLACE_KEY, mStr);
		// intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivityForResult(intent, 0);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			Bundle extras = data.getExtras();
			String s = extras.getString(REPLACE_KEY);

			SKKUtils.dlog("sent back from a mushroom: " + s);

			Intent retIntent = new Intent(ACTION_BROADCAST);
			retIntent.addCategory(CATEGORY_BROADCAST);
			retIntent.putExtra(REPLACE_KEY, s);
			sendBroadcast(retIntent);
		}
		finish();
	}

	private List<AppInfo> loadMushroomAppList() {
		PackageManager pm = getPackageManager();
		Intent intent = new Intent(ACTION_SIMEJI_MUSHROOM);
		intent.addCategory(CATEGORY_SIMEJI_MUSHROOM);
		List<ResolveInfo> appList = pm.queryIntentActivities(intent, 0);
		if (appList.size() == 0) {
			Toast.makeText(this, getString(R.string.error_no_mushroom), Toast.LENGTH_LONG).show();
			finish();
		}

		List<AppInfo> result = new ArrayList<AppInfo>(appList.size());
		for (ResolveInfo ri : appList) {
			ActivityInfo ai = ri.activityInfo;
			Drawable icon = ri.loadIcon(pm);
			icon.setBounds(0, 0, 48, 48);
			result.add(new AppInfo(icon, ri.loadLabel(pm).toString(), ai.packageName, ai.name));
		}
		Collections.sort(result);

		return result;
	}

	private class AppListAdapter extends ArrayAdapter<AppInfo> {
		public AppListAdapter(Context context, List<AppInfo> items) {
			super(context, 0, items);
    	}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView tv = (TextView)convertView;

			if (tv == null) {
				tv = new TextView(SKKMushroom.this);
				tv.setTextSize(20);
				tv.setGravity(android.view.Gravity.CENTER_VERTICAL);
				tv.setPadding(4, 4, 4, 4);
				tv.setCompoundDrawablePadding(8);
			}

    		AppInfo item = getItem(position);
			tv.setCompoundDrawables(item.icon, null, null, null);
    		tv.setText(item.appName);

    		return tv;
    	}
    }
}
