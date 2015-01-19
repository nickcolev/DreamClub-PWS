package com.vera5.httpd;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import android.util.Log;

public class Request {

	public String uri;
	public String version;
	public String ContentType;
	public int ContentLength;
	public String AcceptEncoding;
	public String IfModifiedSince;
	public String IfNoneMatch;
	public static byte[] data;			// PUT/POST
	public String log;					// For Logger
	public String err;
	// Methods
	public String[] aMethod = { "", "GET", "HEAD", "OPTIONS", "TRACE", "POST", "PUT", "DELETE" };
	public int method;

	public void get(Socket client) {
		String s, a[], method = "GET";
		int i = 0, len = 0;
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()), 8192);
			// Receive the header
			while ((s = in.readLine()) != null) {
Log.d("***Req***", " "+s);
				if (s.equals("")) break;
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
				i++;
				// Note: 'this.' is not necessary (mandatory). Used for clarity.
			}
			// Get content (if any -- PUT/POST)
			if (len > 0) {
				this.ContentLength = len;	// FIXME Potential bug -- what if actual length < Content-Length? (Heartbleed bug)
				int c;
				this.data = new byte[len];
				i = 0;
				while(len-- > 0) {
					c = in.read();
					if (c < 0) break;
					this.data[i++] = (byte)(c & 0xFF);
				}
			}
			// Setup properties
			this.log = client.getInetAddress().toString() +
				" " + method + " ";
			for (int m=1; m<this.aMethod.length; m++)
				if (method.equals(this.aMethod[m])) {
					this.method = m;
				}
		} catch (Exception e) {
			err = e.getMessage();
		}
	}
}
