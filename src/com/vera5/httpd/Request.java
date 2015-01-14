package com.vera5.httpd;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class Request {

	public String method;
	public String uri;
	public String version;
	public String ContentType;
	public String AcceptEncoding;
	public String IfModifiedSince;
	public String IfNoneMatch;
	public byte[] data;			// PUT/POST
	public String log;			// For Logger

	public void get(Socket client) {
		String s, a[];
		int i = 0;
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()), 8192);
			// Receive data
			while (true) {
				s = in.readLine().trim();
				if (s.equals("")) break;
				a = s.split(" ");
				// The first line is the request method, resourse, and version (like 'GET / HTTP/1.0')
				if (i == 0) {	// The first line
					this.method = a[0];
					this.uri = a[1].replace("/~", "/usr/");
					this.version = a[2];
				} else if (a[0].equals("Accept-Encoding:")) {
					this.AcceptEncoding = a[1];
				} else if (a[0].equals("Content-Type:")) {
					this.ContentType = a[1];
				} else if (a[0].equals("If-Modified-Since:")) {
					this.IfModifiedSince = a[1];
				} else if (a[0].equals("If-None-Match:")) {
					this.IfNoneMatch = a[1];
				}
				i++;
				// Note: 'this.' is not necessary (mandatory). Used for clarity.
			}
			this.log = client.getInetAddress().toString() +
				" " + this.method + " ";
		} catch (Exception e) {
		}
	}
}
