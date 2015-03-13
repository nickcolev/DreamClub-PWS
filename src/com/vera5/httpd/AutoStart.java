package com.vera5.httpd;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

public class AutoStart extends BroadcastReceiver {

	public void onReceive(Context arg0, Intent arg1) {
		if (PreferenceManager.getDefaultSharedPreferences(arg0).getBoolean("auto_start", false)) {
			Intent intent = new Intent(arg0, ServerService.class);
			arg0.startService(intent);
		}
	}
}
