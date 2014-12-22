// TODO settings could be in activity preferences -- see http://stackoverflow.com/questions/3570690/whats-the-best-way-to-do-application-settings-in-android
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
import android.preference.*;	// Preferences
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
//import android.widget.ToggleButton;
// Preferences
import android.content.SharedPreferences;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;


public class StartActivity extends Activity {

	private static final String TAG = "PWS";
    private static ScrollView mScroll;
    private static TextView mLog;
    private String lastMessage = "";
	private ServerService mBoundService;
	private SharedPreferences prefs;

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
Log.d("***CP10***", "onCreate()");
		setContentView(R.layout.main);
		// Settings
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		// Initialize members with default values for a new instance
		if (savedInstanceState == null) {	// Initialize
			mLog = (TextView) findViewById(R.id.log);
			mScroll = (ScrollView) findViewById(R.id.ScrollView01);
		}

		String documentRoot = setupDocRoot();
        if (documentRoot != null) doBindService();
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
    	mLog.append("\n" + s);
    	mScroll.fullScroll(ScrollView.FOCUS_DOWN);
    }
    
    private void startServer(Handler handler, String documentRoot, int port) {
    	if (mBoundService == null) {
	        Toast.makeText(StartActivity.this, "Service not connected", Toast.LENGTH_SHORT).show();
		} else {
			mBoundService.startServer(handler, documentRoot, port);
		}
    }
    
    private void stopServer() { 
    	if (mBoundService == null) {
	        Toast.makeText(StartActivity.this, "Service not connected", Toast.LENGTH_SHORT).show();
		} else {
			mBoundService.stopServer();
		}
    }
    
	private ServiceConnection mConnection = new ServiceConnection() {

	    public void onServiceConnected(ComponentName className, IBinder service) {
			mBoundService = ((ServerService.LocalBinder)service).getService();
			Toast.makeText(StartActivity.this, "Service connected", Toast.LENGTH_SHORT).show();
			mBoundService.updateNotifiction(lastMessage);
			if(!mBoundService.isRunning()) {
				try {
					int port = Integer.parseInt(prefs.getString("port", ""));
					mBoundService.startServer(mHandler, getDocRoot(), port);
				} catch (Exception e) {
					Log.e(TAG, e.getMessage());
				}
			}
	    }

		public void onServiceDisconnected(ComponentName className) {
			mBoundService = null;
			Toast.makeText(StartActivity.this, "Service disconnected", Toast.LENGTH_SHORT).show();
		}
	};

	private void doUnbindService() {
    	if (mBoundService != null) {
	        unbindService(mConnection);
	    }
	}
	
	private void doBindService() {
		// http://developer.android.com/guide/components/services.html
	    bindService(new Intent(StartActivity.this, ServerService.class), mConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onDestroy() {
	    super.onDestroy();
Log.d("***CP19***", "onDestroy()");
	    doUnbindService();
	    File f = new File(getDocRoot()+"/index.html");
	    // DEBUG try { f.delete(); } catch (Exception e) { }
	}
/*
	@Override
	public void onBackPressed() {
		moveTaskToBack(true);
	}
*/
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
				stopServer();
				this.finish();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public static String getDefaultDocRoot() {
		return Environment.getExternalStorageDirectory().getAbsolutePath() + "/htdocs";
	}

	private String getDocRoot() {		// Warning: no trailing '/'
		String s = prefs.getString("doc_root", "").replaceAll("/$", "");	// Remove trailing slash
		return ((s.startsWith("/") ? "" : "/") + s);					// Add leading slash if ommited
/*
		// TODO Check if exists
		String path;
		if (prefs.getString("doc_root", "").equals(""))
			path = getDefaultDocRoot();
		else {
			String s = prefs.getString("doc_root", "").replaceAll("/$", "");	// Remove trailing slash
			path = (s.startsWith("/") ? "" : "/") + s;					// Add leading slash if ommited
		}
		return path;
*/
	}
}
