package com.vera5.httpd;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.net.Socket;

public class Logger {

  static final String TAG = "PWS.Logger";
  private final Context context;
  private Handler handler;
  private Socket client;
  private boolean enable = true;	// Logging enabled by default

	public Logger(ServerService parent) {
		this.context = parent.context;
	}

	public void request(Request request) {
		i(clientIP(request.client)+" "+request.getMethod()+" "+request.uri);
	}

	private void put(String tag, String s) {
		if (enable) {
			// Actual logging in a separate thread
			LoggerThread lt = new LoggerThread(this.context, tag, s);
			lt.run();
		}
	}

	// Helpers
	public String clientIP(Socket client) { return client.getInetAddress().toString().substring(1); }
	public void enable() { enable = true; }
	public void disable() { enable = false; }
	public void setClient(Socket client) { this.client = client; }
	public void setHandler(Handler handler) { this.handler = handler; }
	public void d(String s) { put("D", s); }
	public void e(String s) { put("E", s); }
	public void i(String s) { put("I", s); }
	public void s(String s) { put("S", s); }

	public void v(String s) {	// To a TextView
		if (this.handler != null) {
			Message msg = new Message();
			Bundle b = new Bundle();
			b.putString("msg", s);
			msg.setData(b);
			handler.sendMessage(msg);
		}
	}

	public String get(String key) {
		Database db = new Database(this.context);
		if (key.startsWith("=")) key = key.substring(1);
		String[] a = key.split(":");
		String tag = null, where = null;
		switch (a.length) {
			case 1:
				where = a[0];
				break;
			case 2:
				where = a[0];
				tag = a[1].substring(0,1);
				if (tag.startsWith("*")) tag = null;
				break;
		}
		switch (where.length()) {
			case 0:
				where = null;
				break;
			case 1:				// like, log=e
				tag = where;
				where = null;
				break;
		}
		return db.getLog(tag, where);
	}


	class LoggerThread extends Thread {
	  private String tag, msg;
	  private final Context context;
		public LoggerThread(Context context, String tag, String msg) {
			this.context = context;
			this.tag = tag;
			this.msg = msg;
		}
		public void run() {
			Database db = new Database(this.context);
			try {
				db.putLog(tag, msg);
			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
			}
		}
	}
}
