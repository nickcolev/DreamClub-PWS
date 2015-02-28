package com.vera5.httpd;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.lang.Thread;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.ArrayList;
import android.util.Log;

public class Request {

  private static final String TAG = "PWS.Request";
  public String uri;			// Normalized url
  public String url;
  public String args;			// after ? in the url
  public String version;
  public String ContentType;
  public int ContentLength = 0;
  public String AcceptEncoding;
  public String IfNoneMatch;
  public String Host;
  public String remote_addr = "";
  public byte[] data;			// PUT/POST
  public String err = "";		// Last error
  public long started;
  // Methods
  public short method = 0;
  private static final String[] aMethod = {"",
	"GET","HEAD","OPTIONS","TRACE","PUT","POST","DELETE"
  };
  private ArrayList<String> aHeader = new ArrayList<String>();
  public Config cfg;
  public final Socket client;


	public Request(ServerHandler parent) {
		this.cfg = parent.cfg;
		this.client = parent.toClient;
		this.remote_addr = Lib.clientIP(this.client);
	}

	public void get(Socket client) {
		this.started = System.currentTimeMillis();
		String s, a[], method = null;
		int i = 0, p;
		try {
			DataInputStream in = new DataInputStream(client.getInputStream());
			// The header
			while ((s = in.readLine()) != null) {
				if (s.length() == 0) break;
				this.aHeader.add(s);
				a = s.split(" ");
				if (method == null) {
					p = s.indexOf("HTTP/");
					if (p != -1) {
						method = a[0].trim();
						this.url = a[1];
						this.version = a[2];
						parseUri(a[1]);
					}
				}
				if (a[0].equals("Accept-Encoding:")) {
					this.AcceptEncoding = a[1];
				} else if (a[0].equals("Content-Type:")) {
					this.ContentType = a[1];
				} else if (a[0].equals("Content-Length:")) {
					this.ContentLength = Integer.parseInt(a[1]);
				} else if (a[0].equals("If-None-Match:")) {
					this.IfNoneMatch = a[1];
				} else if (a[0].equals("Host:")) {
					this.Host = a[1];
				}
				// Other headers parsing here
				i++;
			}
			if (method == null) return;
			if(this.ContentLength > 0)	// PUT/POST data?
				getdata(in);
			setMethod(method);
		} catch (Exception e) {
			this.err = e.getMessage();
			Lib.errlog(TAG, this.err);
		}
	}

	private void setMethod(String method) {
		for (short i=1; i<this.aMethod.length; i++)
			if (this.aMethod[i].equals(method)) {
				this.method = i;
				break;
			}
	}

	private void getdata(DataInputStream in) {
		long beg = System.currentTimeMillis();
		try {
			this.data = new byte[this.ContentLength];
			in.readFully(this.data);
		} catch (IOException ie) {
			this.data = null;
			Lib.errlog(TAG, ie.getMessage());
		} catch (Exception e) {
			Lib.errlog(TAG, e.getMessage());
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
	public boolean gzipAccepted() {
		if (this.AcceptEncoding == null) return false;
		return this.AcceptEncoding.contains("gzip");
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
	private void parseUri(String uri) {
		String s = uri;
		try {
			s = URLDecoder.decode(s);
		} catch (Exception e) {
		}
		int p = s.indexOf('?');
		if (p == -1)
			this.uri = s;
		else {
			this.uri = s.substring(0, p);
			this.args = s.substring(++p);
		}
	}

}
