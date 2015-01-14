package com.vera5.httpd;

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
	private Handler handler = null;
	private String path;
	private boolean enable = false;	// (future development) set to 'false' to disable file logging

	public Logger(String path) {
		this.path = path;
	}

	public void clean() {
		String log = this.path + "/log.txt";
		File f = new File(log);
		try {
			f.delete();
			f.createNewFile();
		} catch (IOException e) {
		}
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

	public CharSequence get(Socket socket) {	// We save timestamp in the log (to save space).
								// For display, format it properly.
		//String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss zzz").format(new Date());	// GMT
		String log = this.path + "/log.txt";
		File f = new File(log);
		String msg = "log to be here... (under development)";
		try {
			PrintWriter out = new PrintWriter (socket.getOutputStream(), true);
			out.print("HTTP/1.1 200\nContent-Type: text/plain\nConnection: close\n\n");
			BufferedReader in = new BufferedReader(new FileReader(f), 8192);
			String line;
			while ((line = in.readLine()) != null) {
				out.println(line);
			}
			in.close();
			out.flush();
			out.close();
		} catch (IOException e) {
		}
		return "log to be here... (under development)";
	}

}
