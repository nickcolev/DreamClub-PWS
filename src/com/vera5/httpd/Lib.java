package com.vera5.httpd;

import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.NoSuchMethodError;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

class Lib {

	public static long rtime(long begin) {
		return (System.currentTimeMillis() - begin);
	}

	public static void dbg(String tag, String msg) {
		Log.d(tag, msg);
		ServerService.log.d(tag+" "+msg);
	}

	public static String a2h(String[] a) {
		String s = "";
		for(int i=0; i<a.length; i++) s += "\n" + a[i];
		return s;
	}

	public static byte[] gzip(byte[] in) {
		long begin = System.currentTimeMillis();
		byte[] gzip;
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream(in.length);
			GZIPOutputStream gos = new GZIPOutputStream(os);
			gos.write(in);
			gos.close();
			gzip = os.toByteArray();
			os.close();
			dbg("GZIP", ""+in.length+" bytes gzipped to "+gzip.length+" bytes in "+(System.currentTimeMillis()-begin)+"ms");
		} catch (IOException e) {
			gzip = null;
			Log.e("GZIP", e.getMessage());
		}
		return gzip;
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

	// Aliases
	public static void logE(String s) { ServerService.log.e(s); }
	public static void logI(String s) { ServerService.log.i(s); }
	public static void logS(String s) { ServerService.log.s(s); }
	public static void logV(String s) { ServerService.log.v(s); }

}