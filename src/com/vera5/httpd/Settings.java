package com.vera5.httpd;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

public class Settings extends PreferenceActivity {

  OnSharedPreferenceChangeListener listener;

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
		listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
			public void onSharedPreferenceChanged(SharedPreferences p, String key) {
				setSummary(p, key);
				// TODO Restart server if port changed
			}
		};
	}

	@Override
	protected void onStart() {
		super.onStart();
		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
		p.registerOnSharedPreferenceChangeListener(listener);
	}

	@Override
	protected void onPause() {
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listener);
		super.onPause();
	}

	private void setSummary(SharedPreferences p, String key) {
		try {
			EditTextPreference pref = (EditTextPreference) findPreference(key);
			pref.setSummary(p.getString(key, ""));
		} catch (Exception e) {
			Log.e("httpd.setSummary()", e.getMessage());
		}	
	}

}
