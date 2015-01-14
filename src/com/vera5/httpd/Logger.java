package com.vera5.httpd;

import android.util.Log;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

	static final String TAG = "PWS.Logger";
	public static String logFile;

	public Logger(String path) {
		this.logFile = path + "/log.txt";;
	}

	public void put(String s) {
		String now = "" + new Date().getTime();		// write timestamp to save space (GMT)
		File f = new File(logFile);
		try {
			if (!f.exists()) f.createNewFile();
			BufferedWriter b = new BufferedWriter(new FileWriter(logFile, true));
			b.append(now + "\t" + s + "\n");
			b.flush();
			b.close();
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}
	}

	public void get() {	// We save timestamp in the log (to save space).
						// For display, format it properly.
		//String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss zzz").format(new Date());	// GMT
	}

}
