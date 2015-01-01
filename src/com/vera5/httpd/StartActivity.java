package com.vera5.httpd;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import android.app.Activity;
import android.app.NotificationManager;
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
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
// Preferences
import android.content.SharedPreferences;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;

import android.content.res.Resources;


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
			cfg.root = prefs.getString("doc_root", defaultDocRoot());
			// more here
			cfg.defaultIndex = getText(R.string.index);
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		}
		///FIXME if (cfg.root.endsWith("/")) cfg.root = cfg.root.substr(0, ...
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

	@Override
	public void onDestroy() {
		if (mBoundService != null) {
			stopService(intent);
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
			///Tooltip("Service connected");
			if(!mBoundService.isRunning()) {
				try {
					mBoundService.init(mHandler, cfg);	// FIXME Better name?
					startService(intent);
				} catch (Exception e) {
					Log.e(TAG, e.getMessage());
				}
			}
	    }
		public void onServiceDisconnected(ComponentName className) {
			mBoundService = null;
			Tooltip("Service disconnected");
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

	private String getDocRoot() {
		String s = prefs.getString("doc_root", "");
		if (s.equals("")) {					// Default
			s = defaultDocRoot();
		} else {
			s = s.replaceAll("/$", "");		// Remove trailing slash
		}
		return ((s.startsWith("/") ? "" : "/") + s);					// Add leading slash if ommited
	}
}
