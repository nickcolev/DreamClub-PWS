package com.vera5.httpd;

import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.nio.CharBuffer;

public class CGI {

  private static final String TAG = "PWS.CGI";

	public CGI() {
	}

	public static String exec(String cmd, byte[] data) {
Log.d(TAG, cmd+" l="+data.length);
		String out = "";	// cmd response
		try {
			String row;
			Process p = Runtime.getRuntime().exec(cmd);
			BufferedReader in = new BufferedReader(
				new InputStreamReader(p.getInputStream()),8192);
			while ((row = in.readLine()) != null) {
				out += row + "\r\n";
			}
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return out;
	}
}
