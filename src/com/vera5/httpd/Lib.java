package com.vera5.httpd;

import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;

class Lib {

	public static String getDokument (String fname) {
		String s = fname.replaceFirst ("[\\?#](.*)","");	// Strip after ? or #
		try {
			s = URLDecoder.decode (s);
		} catch (Exception e) {
			s = fname;
		}
		return s;
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

}
