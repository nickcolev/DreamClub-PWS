package com.vera5.httpd;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

public class Config {

  static final String TAG = "PWS.Config";
  private static Context context;
  public static String root;
  public static String index;
  public static CharSequence defaultIndex;
  public static String version;
  public static String footer;
  public static boolean dir_list;
  public static boolean wake_lock;
  public static boolean wifi_lock;

	public Config(ServerService parent) {
		this.context = parent.context;
	}

	public void configure(SharedPreferences p) {
		try {
			this.root = Lib.sanify(p.getString("root", defaultDocRoot()));
			this.index = Lib.sanify(p.getString("index", null));
			this.footer = Lib.sanify(p.getString("footer", ""));
			this.dir_list = p.getBoolean("dir_list", false);
			this.wifi_lock = p.getBoolean("wifi_lock", false);
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		}
	}

	private String defaultDocRoot() {
		return Environment.getExternalStorageDirectory().getAbsolutePath() + "/htdocs";
	}

}
