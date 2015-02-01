package com.vera5.httpd;

import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;

public class CGI {

  private static final String TAG = "PWS.CGI";

	public static String exec(final Request request) {
		String s = "";
		if (request.ContentLength == 0) {	// GET
			s = request.args == null ? "" : request.args;
		} else {
			for(int i=0; i<request.data.length; i++)
				s += (char)request.data[i];
		}
		int len = request.ContentLength == 0 ? s.length() : request.ContentLength;
		String cmd = "/system/bin/sh "+request.cfg.root+request.uri;
		String[] aEnv = {
			"REQUEST_METHOD="+(request.method == 0 ? "GET" : request.getMethod()),
			"CONTENT_LENGTH="+len,
			"CONTENT_TYPE="+request.ContentType,
			"CONTENT="+s
		};	// FIXME utilize stdin?! (as CGI requires)
		int err = 0;
		String output = "";			// cmd response
		try {
			String row;
			Process p = Runtime.getRuntime().exec(cmd, aEnv);
			err = p.waitFor();
			BufferedReader in = new BufferedReader(
				new InputStreamReader(p.getInputStream()),8192);
			while ((row = in.readLine()) != null) {
				output += row + "\r\n";
			}
			in.close();
		} catch (IOException e) {
			output = null;
			e.printStackTrace();
		} catch (InterruptedException ie) {
			output = null;
			ie.printStackTrace();
		}
		if (err != 0) output = null;
		return output;
	}
}
