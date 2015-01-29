package com.vera5.httpd;

import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;

public class CGI {

  private static final String TAG = "PWS.CGI";

	public static String exec(Request request, String root) {
		String s = "";
		for(int i=0; i<request.data.length; i++) s += (char)request.data[i];
		String cmd = "/system/bin/sh "+root+request.uri;
		String[] aEnv = {
			"REQUEST_METHOD=POST",
			"CONTENT_TYPE="+request.ContentType,
			"CONTENT_LENGTH="+request.ContentLength,
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
