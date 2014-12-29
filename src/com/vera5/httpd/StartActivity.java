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


public class StartActivity extends Activity {

	private static final String TAG = "PWS";
	private static int Port = 8080;
    private static ScrollView mScroll;
    private static TextView mLog;
	private ServerService mBoundService;
	private SharedPreferences prefs;
	private Intent intent;

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
		intent = new Intent(this, ServerService.class);
		// Settings
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		// FIXME Separate Config() method
		String s = prefs.getString("port", "");
		if (!s.equals("")) {
			try {
				Port = Integer.parseInt(s);
			} catch (Exception e) {
				Port = 8080;	// Default
			}
		}
		mLog = (TextView) findViewById(R.id.log);
		mScroll = (ScrollView) findViewById(R.id.ScrollView01);

		String documentRoot = setupDocRoot();
        if (documentRoot != null) {
			bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		}
    }

	private String setDocRoot() {
		String path = getDocRoot();
		File f = new File(path);
		if (f.exists()) return path;
		try {
			f.mkdir();
			Log.i(TAG, "Created folder " + path);
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
			return null;
		}
		return path;
	}

	private void setDocIndex(String path) {
		File f = new File(path+"/index.html");
		if (f.exists()) return;
		try {
			// FIXME Can we put the index.html in 'res'?
			BufferedWriter bout = new BufferedWriter(new FileWriter(f));
			bout.write(
				"<html><head><title>"+TAG+"</title>"+
				"</head>"+
				"<body>\n<h1>It works!</h1>\n"+
				"<p>The web server software is running but no content has been added in <tt>"+path+"</tt>, yet.</p>\n"+
				"</body></html>"
			);
			bout.flush();
			bout.close();
			Log.i(TAG, "Created index.html");
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		}
	}

	private String setupDocRoot() {
		String path = setDocRoot();
		if (path != null) setDocIndex(path);
		return path;
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
			Tooltip("Service connected");
			if(!mBoundService.isRunning()) {
				try {
					mBoundService.init(mHandler);	// FIXME Better name?
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
				if (mBoundService != null) unbindService(mConnection);
				stopService(intent);
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
