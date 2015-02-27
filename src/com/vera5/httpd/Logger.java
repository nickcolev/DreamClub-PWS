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
  private ServerHandler thread;
  private Request request;
  private Response response;
  private boolean enable = true;	// Logging enabled by default

	public Logger(ServerService parent) {
		this.context = parent.context;
	}

	public void request(ServerHandler thread) {
		this.thread = thread;
		i(Lib.clientIP(thread.request.client)+" "+thread.request.getMethod()+" "+thread.request.uri);
	}

	private void put(String tag, String s) {
		if (enable) {
			// Actual logging in a separate thread
			LoggerThread lt = new LoggerThread(this.context, this.thread, tag, s);
			lt.run();
		}
	}

	// Helpers
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
	  private final ServerHandler thread;
		public LoggerThread(Context context, ServerHandler thread, String tag, String msg) {
			this.context = context;
			this.thread = thread;
			this.tag = tag;
			this.msg = msg;
		}
		public void run() {
			Database db = new Database(this.context);
			String who = "";
			long ms = 0;
			if (this.thread != null) {
				who = this.thread.request.remote_addr;
				ms = Lib.rtime(this.thread.request.started);
				if (this.thread.response.err.length() > 0) {
					tag = "E";
					msg += this.thread.response.err;
				}
			}
			try {
				db.putLog(tag, who, msg, ms);
			} catch (Exception e) {
				Lib.errlog(TAG, e.getMessage());
			}
		}
	}
}
