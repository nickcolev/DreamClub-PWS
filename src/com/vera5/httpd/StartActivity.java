package com.vera5.httpd;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;


public class StartActivity extends Activity {

	private static final String TAG = "PWS";
    private static ScrollView mScroll;
    private static TextView mLog;
	private ServerService mBoundService;
	private SharedPreferences prefs;
	private Intent intent;
	private Config cfg;

    final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Bundle b = msg.getData();
			log(b.getString("msg"));
		}
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mLog = (TextView) findViewById(R.id.log);
		mScroll = (ScrollView) findViewById(R.id.ScrollView01);
		intent = new Intent(this, ServerService.class);
		// Settings
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		// Configuration
		cfg = new Config();
		try {
			String s = prefs.getString("port", "8080");
			cfg.port = Integer.parseInt(s);
			cfg.root = prefs.getString("root", defaultDocRoot());
			cfg.index = prefs.getString("index", null);	// FIXME Can't we get it from 'strings'?
			cfg.header = prefs.getString("header", "");
			cfg.footer = prefs.getString("footer", "");
			cfg.version = version();
			cfg.defaultIndex = getText(R.string.defaultIndex);
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		}
		log("Version "+cfg.version);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

	@Override
	public void onDestroy() {
		if (mBoundService != null) {
			unbindService(mConnection);
			mConnection = null;
		}
		super.onDestroy();
	}

    public static void log( String s ) {
    	mLog.append(s + "\n");
    	mScroll.fullScroll(ScrollView.FOCUS_DOWN);
    }

	private void Tooltip(String s) {
		Toast.makeText(StartActivity.this, s, Toast.LENGTH_SHORT).show();
	}

	private ServiceConnection mConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className, IBinder service) {
			mBoundService = ((ServerService.LocalBinder)service).getService();
			if(!mBoundService.isRunning()) {
				try {
					mBoundService.init(mHandler, cfg);
					startService(intent);
				} catch (Exception e) {
					Log.e(TAG, e.getMessage());
				}
			}
	    }
		public void onServiceDisconnected(ComponentName className) {
			mBoundService = null;
		}
	};

	@Override
	public void onBackPressed() {
		moveTaskToBack(true);
	}

	// Menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.settings:
				try {
					startActivity(new Intent(".Settings"));
				} catch (Exception e) {
					Log.e(TAG, e.getMessage());
				}
				return true;
			case R.id.exit:
				this.finish();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public static String defaultDocRoot() {
		return Environment.getExternalStorageDirectory().getAbsolutePath() + "/htdocs";
	}

	private String version() {
		try {
			PackageManager packageManager = getPackageManager();
			PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(),0);
			return packageInfo.versionName;
		} catch (PackageManager.NameNotFoundException e) {
			Log.e(TAG, "Error while fetching app version", e);
			return "?";
		}
	}

}
