package com.vera5.httpd;

import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;

public class CGI {

  private static final String TAG = "PWS.CGI";

	public static String exec(final Request request) {
		// Keep-it-simple: Just shell scripts
		if (!isShell(request)) return null;
		String cmd = "/system/bin/sh "+request.cfg.root+request.uri;
		String sEnv = "REQUEST_METHOD="+request.getMethod()
			+ "\nCONTENT_LENGTH="+request.ContentLength
			+ "\nCONTENT_TYPE="+request.ContentType
			+ "\nDOCUMENT_ROOT="+request.cfg.root
			+ "\nREMOTE_ADDR="+request.client.getInetAddress().getHostAddress();
		if (request.method == 1)
			sEnv += "\nQUERY_STRING="+(request.args == null ? "" : request.args);
		File root = new File(request.cfg.root);
		int err = 0;
		String output = "";			// cmd response
		try {
			String row;
			Process p = Runtime.getRuntime().exec(cmd, sEnv.split("\n"), root);
			if (request.method == 6) {	// POST
				stdin(p, request.data);
			}
			err = p.waitFor();
			BufferedReader in = new BufferedReader(
				new InputStreamReader(p.getInputStream()),8192);
			while ((row = in.readLine()) != null) {
				output += row + "\n";
			}
			in.close();
			return output;
		} catch (IOException e) {
			output = e.getMessage();
			//e.printStackTrace();
		} catch (InterruptedException ie) {
			output = ie.getMessage();
			//ie.printStackTrace();
		}
		if (err != 0) output = "err: "+err;
		return output;
	}

	private static void stdin(Process p, byte[] data) throws IOException {
		OutputStream stdin = p.getOutputStream();
		stdin.write(data);
		stdin.flush();
		stdin.close();
	}

	private static boolean isShell(final Request request) {
		File f = new File(request.cfg.root+request.uri);
		FileInputStream in;
		try {
			in = new FileInputStream(f);
		} catch (FileNotFoundException e) {
			Lib.errlog(TAG, e.getMessage());
			return false;
		}
		int cnt = 35;
		byte[] buf = new byte[cnt];
		try {
			cnt = in.read(buf, 0, cnt);
		} catch (IOException e) {
			Lib.errlog(TAG, e.getMessage());
			return false;
		}
		String s = new String(buf);
		// Get the first line
		int p = s.indexOf("\n");
		if (p == -1) return false;
		s = s.substring(0, p);
		// Check if it's a shell script
		if (!s.startsWith("#!"))	// Should start with '#!'
			return false;
		if (!s.endsWith("sh"))		// and should end with 'sh' ('#!/bin/sh' or '#!/bin/bash')
			return false;
		return true;
	}
}
