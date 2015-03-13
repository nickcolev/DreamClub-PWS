package com.vera5.httpd;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AutoStart extends BroadcastReceiver {

	public void onReceive(Context arg0, Intent arg1) {
		// FIXME if auto_start is false don't auto start
		Intent intent = new Intent(arg0, ServerService.class);
		arg0.startService(intent);
	}
}
