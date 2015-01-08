package com.vera5.httpd;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import java.util.Map;

public class Settings extends PreferenceActivity {

  private boolean changed;
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
				setResult(StartActivity.SETTINGS_CHANGED);
				// TODO Restart server if port changed
			}
		};
	}

	@Override
	protected void onStart() {
		super.onStart();
		this.changed = false;
		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
		p.registerOnSharedPreferenceChangeListener(listener);
	}

	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listener);
	}

	private void setSummary(SharedPreferences p, String key) {
		try {
			EditTextPreference pref = (EditTextPreference) findPreference(key);
			pref.setSummary(p.getString(key, ""));
			this.changed = true;
		} catch (Exception e) {
			Log.e("httpd.setSummary()", e.getMessage());
		}	
	}

}
