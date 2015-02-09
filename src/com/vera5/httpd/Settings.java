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
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;	///
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import java.util.Map;

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
		for(Map.Entry<String,?> entry : keys.entrySet()) {
			Preference p = findPreference(entry.getKey());
			if (p instanceof EditTextPreference)
				setSummary(prefs, entry.getKey());
			else if (p instanceof CheckBoxPreference)
				setBoolean(prefs, entry.getKey());
		}
		//hide("footer"); FIXME Some hidden preferences
		listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
			public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
				Preference p = findPreference(key);
				if (p instanceof EditTextPreference)
					setSummary(sp, key);
				else
					setBoolean(sp, key);
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

	private void hide(String key) {
		EditTextPreference pref = (EditTextPreference) findPreference(key);
		PreferenceScreen ps = getPreferenceScreen();
		ps.removePreference(pref);
	}

	private void setBoolean(SharedPreferences sp, String key) {
		try {
			CheckBoxPreference cbp = (CheckBoxPreference) findPreference(key);
			if (mBoundService != null) {
				if (key.equals("dir_list"))
					mBoundService.cfg.dir_list = sp.getBoolean(key, false);
			}
		} catch (Exception e) {
		}
	}

	private void setSummary(SharedPreferences sp, String key) {
		try {
			EditTextPreference pref = (EditTextPreference) findPreference(key);
			pref.setSummary(sp.getString(key, ""));
			if (mBoundService != null) {
				// Not necessary to restart but for port change
				if (key.equals("port")) {
					mBoundService.ReStart();
				}
				if (key.equals("index")) {
					mBoundService.cfg.index = sp.getString(key, "");
				}
				if (key.equals("footer")) {
					mBoundService.getFooter();
				}
			}
		} catch (Exception e) {
			Log.e("httpd.setSummary()", e.getMessage());
		}	
	}

	private ServiceConnection mConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className, IBinder service) {
			mBoundService = ((ServerService.LocalBinder)service).getService();
	    }
		public void onServiceDisconnected(ComponentName className) {
			mBoundService = null;
		}
	};

}
