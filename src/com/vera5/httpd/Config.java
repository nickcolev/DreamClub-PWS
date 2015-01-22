package com.vera5.httpd;

import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

public class Config {

	static final String TAG = "PWS.Config";
	public static String root;
	public static String index;
	public static CharSequence defaultIndex;
	public static String version;
	public static String footer;

	public void configure(SharedPreferences p) {
		try {
			this.root = sanify(p.getString("root", defaultDocRoot()));
			this.index = p.getString("index", null);
			this.footer = sanify(p.getString("footer", ""));
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		}
	}

	private String defaultDocRoot() {
		return Environment.getExternalStorageDirectory().getAbsolutePath() + "/htdocs";
	}

	public String sanify(String path) {
		// Remove trailing slash
		if (path.endsWith("/"))
			path = path.substring(0, path.length() - 1);
		// Add leading slash on return
		return (path.startsWith("/") ? "" : "/") + path;
	}

}
