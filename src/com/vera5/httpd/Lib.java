package com.vera5.httpd;

import java.io.File;
import java.lang.NoSuchMethodError;
import java.text.SimpleDateFormat;
import java.util.Date;

class Lib {

	public static void logE(String msg) {
		ServerService.log.e(msg);
	}

	public static String a2h(String[] a) {
		String s = "";
		for(int i=0; i<a.length; i++) s += "\n" + a[i];
		return s;
	}

	public static String now() {
		SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
		return sdf.format(new Date());
	}

	public static String intToIp(int i) {
		return	((i       ) & 0xFF) + "." +
				((i >>  8 ) & 0xFF) + "." +
				((i >> 16 ) & 0xFF) + "." +
				( i >> 24   & 0xFF);
	}

	public static String fileAttr(File f) {
		String exec = "?";
		try {
			exec = f.canExecute() ? "x" : " ";
		} catch (NoSuchMethodError e) {
			exec = "?";
		}
		return	(f.isDirectory() ? "d" : " ") +
			(f.canRead() ? "r" : "-") +
			(f.canWrite() ? "w" : "-") +
			exec;
	}

}
