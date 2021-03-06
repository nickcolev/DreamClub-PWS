package com.vera5.httpd;

import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.NoSuchMethodError;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

class Lib {

	public static String a2h(String[] a) {
		String s = "";
		for(int i=0; i<a.length; i++) s += "\n" + a[i];
		return s;
	}

	public static String addIndex(String path, String index) {
		return path+(path.endsWith("/") ? "" : "/")+index.substring(1);	// Config puts leadin '/' to the default index
	}

	public static String clientIP(Socket client) {
		return client.getInetAddress().toString().substring(1);
	}

	public static void dbg(String tag, String msg) {
		Log.d(tag, msg);
		ServerService.log.d(tag+" "+msg);
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
		} catch (IOException e) {
			gzip = null;
			errlog("GZIP", e.getMessage());
		}
		return gzip;
	}

	public static String intToIp(int i) {
		return	((i       ) & 0xFF) + "." +
				((i >>  8 ) & 0xFF) + "." +
				((i >> 16 ) & 0xFF) + "." +
				( i >> 24   & 0xFF);
	}

	public static byte[] join(byte[] header, byte[] body) {
		ByteArrayOutputStream os = new ByteArrayOutputStream(header.length);
		os.write(header, 0, header.length);
		os.write(body, 0, body.length);
		return os.toByteArray();
	}

	public static byte[] join(String header, byte[] body) {	// Overloaded
		return join(header.getBytes(), body);
	}

	public static String md5(String s) {
		try {
			// Create MD5 Hash
			MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
			digest.update(s.getBytes());
			byte messageDigest[] = digest.digest();
			
			// Create Hex String
			StringBuffer hexString = new StringBuffer();
			for (int i=0; i<messageDigest.length; i++)
				hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
			return hexString.toString();
			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return "";
	}

	public static String now() {
		SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
		return sdf.format(new Date());
	}

	public static long rtime(long begin) {
		return (System.currentTimeMillis() - begin);
	}

	public static String sanify(String path) {
		// Remove trailing slash
		if (path.endsWith("/"))
			path = path.substring(0, path.length() - 1);
		// Add leading slash on return
		return (path.startsWith("/") ? "" : "/") + path;
	}

	// Aliases
	public static void errlog(String tag, String msg) {
		Log.e(tag, msg);
		logE(msg);
	}
	public static void logE(String s) { ServerService.log.e(s); }
	public static void logI(String s) { ServerService.log.i(s); }
	public static void logS(String s) { ServerService.log.s(s); }
	public static void logV(String s) { ServerService.log.v(s); }

}
