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
		// Keep-it-simple: Just shell scripts
		if (!isShell(request)) return null;
		String cmd = "/system/bin/sh "+request.cfg.root+request.uri;
Log.d("***CGI***", "cmd="+cmd);
		String[] aEnv = {
			"REQUEST_METHOD="+request.getMethod(),
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
				output += row + "\n";
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

	private static boolean isShell(final Request request) {
		if (request.cache.content == null)	// no cache
			return false;
		// Get the first line
		String s = new String(request.cache.content);
		int p = s.indexOf("\n");
		if (p != -1) s = s.substring(0, p);
		// Check if it's a shell script
		if (!s.startsWith("#!"))	// Should start with '#!'
			return false;
		if (!s.endsWith("sh"))		// and should end with 'sh' ('#!/bin/sh' or '#!/bin/bash')
			return false;
		return true;
	}
}
