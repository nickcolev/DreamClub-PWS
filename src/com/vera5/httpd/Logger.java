package com.vera5.httpd;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

	static final String TAG = "PWS.Logger";
	private Handler handler = null;
	private String path;
	private boolean enable = false;	// (future development) set to 'false' to disable file logging

	public Logger(String path) {
		this.path = path;
	}

	private void put(String tag, String s) {
		if (!enable) return;
		String now = "" + new Date().getTime();		// write timestamp to save space (GMT)
		String log = this.path + "/log.txt";
		File f = new File(log);
		try {
			if (!f.exists()) f.createNewFile();
			BufferedWriter b = new BufferedWriter(new FileWriter(log, true));
			b.append(now + "\t" + tag + "/" + s + "\n");
			b.flush();
			b.close();
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}
	}

	public void enable() { enable = true; }
	public void disable() { enable = false; }

	public void setHandler(Handler handler) { this.handler = handler; }

	public void i(String s) { put("I", s); }
	public void e(String s) { put("E", s); }

	public void v(String s) {	// To a TextView
		if (this.handler != null) {
			Message msg = new Message();
			Bundle b = new Bundle();
			b.putString("msg", s);
			msg.setData(b);
			handler.sendMessage(msg);
		}
	}

	public CharSequence get() {	// We save timestamp in the log (to save space).
								// For display, format it properly.
		//String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss zzz").format(new Date());	// GMT
		return "log to be here... (under development)";
	}

}
