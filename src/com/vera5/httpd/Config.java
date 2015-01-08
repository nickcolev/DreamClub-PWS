package com.vera5.httpd;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

public class Config {

	static final String TAG = "PWS.Config";
	public static int port;
	public static String root;
	public static String index;
	public static CharSequence defaultIndex;
	public static String version;
	public static String footer;

	public void configure(SharedPreferences p) {
		try {
			String s = p.getString("port", "8080");
			this.port = Integer.parseInt(s);
			this.root = p.getString("root", defaultDocRoot());
			this.index = p.getString("index", null);	// FIXME Can't we get it from 'strings'?
			this.footer = p.getString("footer", "");
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		}
	}

	private String defaultDocRoot() {
		return Environment.getExternalStorageDirectory().getAbsolutePath() + "/htdocs";
	}

}
