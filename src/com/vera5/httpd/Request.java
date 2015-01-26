package com.vera5.httpd;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import android.util.Log;

public class Request {

  private static final String TAG = "PWS.Request";
	public String uri;
	public String version;
	public String ContentType;
	public int ContentLength = 0;
	public String AcceptEncoding;
	public String IfModifiedSince;
	public String IfNoneMatch;
	public byte[] data;			// PUT/POST
	public String log;			// For Logger
	public String err;			// Last error
	// Methods
	public int method = 0;
	private static final String[] aMethod = {"",
		"GET","HEAD","OPTIONS","TRACE","PUT"
		//"POST", "DELETE"
	};
	private ArrayList<String> aHeader = new ArrayList<String>();

	private String readLine(DataInputStream in) {
		int c = -1, i=0;
		char[] buf = new char[512];
		try {
			while ((c = in.read()) != -1) {
				if (c == 10) continue;
				if (c == 13) break;
				buf[i++] = (char)c;
			}
		} catch (IOException e) {
			logV(e.getMessage());
		}
		if (c == -1 || i == 0) return null;
		String s = new String(buf);
		return s.substring(0, i);
	}

	public void get(Socket client) {
		String s, a[], method = "GET";
		int i = 0, len = 0;
		try {
			DataInputStream in = new DataInputStream(client.getInputStream());
			// The header
			while ((s = readLine(in)) != null) {
				this.aHeader.add(s);
				a = s.split(" ");
				// The first line is the request method, resourse, and version (like 'GET / HTTP/1.0')
				if (i == 0) {		// The first line
					method = a[0];
					this.uri = a[1].replace("/~", "/usr/");
					this.version = a[2];
				} else if (a[0].equals("Accept-Encoding:")) {
					this.AcceptEncoding = a[1];
				} else if (a[0].equals("Content-Type:")) {
					this.ContentType = a[1];
				} else if (a[0].equals("Content-Length:")) {
					len = Integer.parseInt(a[1]);
				} else if (a[0].equals("If-Modified-Since:")) {
					this.IfModifiedSince = a[1];
				} else if (a[0].equals("If-None-Match:")) {
					this.IfNoneMatch = a[1];
				}
				// Other headers parsing here
				i++;
				// Note: 'this.' is not necessary (mandatory). Used for clarity.
			}
			if (len > 0) {		// PUT/POST data?
				in.read();		// left-over LF (10) from the header
				this.data = new byte[len];
				int c;
				i = 0;
				while ((c = in.read()) != -1)
					this.data[i++] = (byte)c;
				this.ContentLength = i;
			}
			this.log = client.getInetAddress().toString() + " " + method + " ";
			for (i=1; i<this.aMethod.length; i++)
				if (aMethod[i].equals(method)) this.method = i;
		} catch (Exception e) {
			err = e.getMessage();
		}
	}

	// Helpers below
	public String getMethod() { return this.aMethod[this.method]; }
	public String getMethods() {
		String s = "";
		for (int i=1; i<this.aMethod.length; i++)
			s += (i==1 ? "" : ",") + this.aMethod[i];
		return s;
	}
	public String header(String key) {
		String[] a = new String[4];
		for (String row : this.aHeader) {
			a = row.split(":");
			if (a[0].equals(key))
				return a[1].trim();
		}
		return null;
	}
	public String headers() {
		String s = "";
		for (String h : this.aHeader) s += h + "\n";
		return s;
	}
	private void logV(String s) { ServerService.log.v(s); }
	private void logS(String s) { ServerService.log.s(s); }
}
