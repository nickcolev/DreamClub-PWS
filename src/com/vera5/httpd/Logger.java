package com.vera5.httpd;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

  static final String TAG = "PWS.Logger";
  private final Context context;
  private Handler handler;
  private Socket client;
  private String path;
  private boolean enable = true;	// Logging enabled by default

	public Logger(ServerService parent) {
		this.context = parent.context;
		this.path = this.context.getFilesDir().getPath();
	}

	public void clean() {
		String log = fname();
		File f = new File(log);
		try {
			f.delete();
			f.createNewFile();
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}
	}

	public void request(Request request) {
		i(clientIP(request.client)+" "+request.getMethod()+" "+request.uri);
	}

	private void put(String tag, String s) {
		if (!enable) return;
		// FIXME Optimization -- write at separate thread. How about sqlite?
		String now = "" + new Date().getTime();		// write timestamp to save space (GMT)
		String log = fname();
		File f = new File(log);
		try {
			if (!f.exists()) f.createNewFile();
			BufferedWriter b = new BufferedWriter(new FileWriter(log, true), 8192);
			b.append(now + "\t" + tag + (s.startsWith("/") ? "" : "/") +
				s.replaceAll("\n", "\\\\n") + "\n");
			b.flush();
			b.close();
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
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
	private String fname() { return this.path + "/log.txt"; }

	public void v(String s) {	// To a TextView
		if (this.handler != null) {
			Message msg = new Message();
			Bundle b = new Bundle();
			b.putString("msg", s);
			msg.setData(b);
			handler.sendMessage(msg);
		}
	}

	public String get(char c) {
						// We save timestamp in the log (to save space).
						// For display, format it properly.
		String ts, a[], line, log = fname(),
			m = c + "/", result = "", none = "Nothing to display";
		File f = new File(log);
		if (f.length() == 0) return none;
		try {
			BufferedReader in = new BufferedReader(new FileReader(f), 8192);
			while ((line = in.readLine()) != null) {
				a = line.split("\t");
				if (a[1] == null) {
					result += "\t" + line + "\n";
					continue;
				}
				if (c != '?')	// Filter
					if (!a[1].startsWith(m)) continue;
				ts = a[0] == null ? "" : formatTime(a[0]);
				result += ts + "\t" + a[1] + "\n";
			}
			in.close();
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
			result = e.getMessage();
		}
		return (result.length() == 0 ? none : result);
	}

	private String formatTime(String timestamp) {
		String sdf;
		try {
			sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(Long.valueOf(timestamp)));
		} catch (Exception e) {
			sdf = "";
			Log.e(TAG, e.getMessage());
		}
		return sdf;
	}

}
