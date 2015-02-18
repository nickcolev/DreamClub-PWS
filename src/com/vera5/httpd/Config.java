package com.vera5.httpd;

import android.content.Context;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.util.Log;
import java.io.InputStream;
import java.io.IOException;
import java.util.Map;

public class Config {

  static final String TAG = "PWS.Config";
  private static Context context;
  private static SharedPreferences preferences;
  public static String root;
  public static String index;
  public static CharSequence defaultIndex;
  public static String version;
  public static String footer;
  public static boolean dir_list;
  public static boolean wake_lock;
  public static boolean wifi_lock;
  public static boolean CORS;

	public Config(ServerService parent) {
		this.context = parent.context;
	}

	public void configure(SharedPreferences p) {
		this.preferences = p;
		try {
			this.root = Lib.sanify(p.getString("root", defaultDocRoot()));
			this.index = Lib.sanify(p.getString("index", null));
			this.footer = Lib.sanify(p.getString("footer", ""));
			this.dir_list = p.getBoolean("dir_list", false);
			this.wake_lock = p.getBoolean("wake_lock", false);
			this.wifi_lock = p.getBoolean("wifi_lock", false);
			this.CORS = p.getBoolean("cors", false);
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		}
	}

	private String defaultDocRoot() {
		return Environment.getExternalStorageDirectory().getAbsolutePath() + "/htdocs";
	}

	public String setupPage() {
		int len = 0;
		String html = "";
		try {
			Resources res = this.context.getResources();
			InputStream in = res.openRawResource(R.raw.websetup);
			len = in.available();
			byte[] buffer = new byte[len];
			len = in.read(buffer);
			html = new String(buffer);
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}
		Map<String,?> keys = this.preferences.getAll();
		for(Map.Entry<String,?> entry : keys.entrySet()) {
				html = setValue(html, entry);
		}
		return html;
	}

	private String setValue(String html, Map.Entry<String,?> entry) {
		String v = "";
		if (entry.getValue() instanceof java.lang.Boolean) {
			v = entry.getValue().toString() == "true" ? "checked" : "";
		} else if (entry.getValue() instanceof java.lang.String) {
			v = "value=\"" + entry.getValue() + "\"";
		}
		String p = "\""+entry.getKey()+"\"";
		String r = p + " " + v;
		return html.replaceFirst(p, r);
	}

}
