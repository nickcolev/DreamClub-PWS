package com.vera5.httpd;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import java.util.Map;
import android.widget.Toast;

public class Settings extends PreferenceActivity {

  private ServerService mBoundService;
  OnSharedPreferenceChangeListener listener;

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.edit().clear();
		PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
		Map<String,?> keys = prefs.getAll();
		for(Map.Entry<String,?> entry : keys.entrySet())
			setSummary(prefs, entry.getKey());
		listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
			public void onSharedPreferenceChanged(SharedPreferences p, String key) {
				setSummary(p, key);
			}
		};
	}

	@Override
	protected void onStart() {
		super.onStart();
		Intent intent = new Intent(Settings.this, ServerService.class);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
		p.registerOnSharedPreferenceChangeListener(listener);
	}

	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listener);
		unbindService(mConnection);
	}

	private void setSummary(SharedPreferences p, String key) {
		try {
			EditTextPreference pref = (EditTextPreference) findPreference(key);
			pref.setSummary(p.getString(key, ""));
			if (mBoundService != null) {
				// Not necessary to restart but for port change
				if (key.equals("port")) {
					mBoundService.ReStart();
				}
				if (key.equals("index")) {
					mBoundService.cfg.index = p.getString(key, "");
				}
				if (key.equals("footer")) {
					mBoundService.getFooter();
				}
			}
		} catch (Exception e) {
			Log.e("httpd.setSummary()", e.getMessage());
		}	
	}

	private void Tooltip(String s) {
		Toast.makeText(Settings.this, s, Toast.LENGTH_SHORT).show();
	}

	private ServiceConnection mConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className, IBinder service) {
			mBoundService = ((ServerService.LocalBinder)service).getService();
			Tooltip("Service connected");
	    }
		public void onServiceDisconnected(ComponentName className) {
			mBoundService = null;
		}
	};

}
