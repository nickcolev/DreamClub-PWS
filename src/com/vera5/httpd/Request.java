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
  public byte[] data;			// PUT/POST
  public String log;			// For Logger
  public String err;			// Last error
  // Methods
  public int method = 0;
  private static final String[] aMethod = {"",
	"GET","HEAD","OPTIONS","TRACE","PUT","POST","DELETE"
  };
  private ArrayList<String> aHeader = new ArrayList<String>();
  public Config cfg;
  public PlainFile cache;


	public Request(ServerHandler parent) {
		this.cfg = parent.cfg;
	}

	public void get(Socket client) {
		long begin = System.currentTimeMillis();
		String s, a[], method = null;
		try {
			int i = 0, p;
			//BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()),8192);
			DataInputStream in = new DataInputStream(client.getInputStream());
			// The header
			while ((s = in.readLine()) != null) {
//Lib.dbg("REQ", s);
				if (s.length() == 0) break;
				this.aHeader.add(s);
				a = s.split(" ");
				if (method == null) {
					p = s.indexOf("HTTP/");
					if (p != -1) {
						//a = s.split(" ");
						method = a[0];
						this.url = a[1];
						this.version = a[2];
						parseUri(a[1]);
					}
				}
				/*
				// The first line is: method resourse HTTP/version (like 'GET / HTTP/1.0')
				if (a[2] != null)i == 0) {		// The first line
					method = a[0];
					this.url = a[1];
					this.version = a[2];
					parseUri(a[1]);
				}*/
				if (a[0].equals("Accept-Encoding:")) {
					this.AcceptEncoding = a[1];
				} else if (a[0].equals("Content-Type:")) {
					this.ContentType = a[1];
				} else if (a[0].equals("Content-Length:")) {
					this.ContentLength = Integer.parseInt(a[1]);
				} else if (a[0].equals("If-None-Match:")) {
					this.IfNoneMatch = a[1];
				}
				// Other headers parsing here
				i++;
			}
			if (method == null)
				Lib.dbg("***MER***", headers());
			if(this.ContentLength > 0) {	// PUT/POST data?
				getdata(in);
			}
			this.log = client.getInetAddress().toString() + " " + method + " ";
			for (i=1; i<this.aMethod.length; i++)
				if (this.aMethod[i].equals(method)) this.method = i;
		} catch (Exception e) {
			this.err = e.getMessage();
			Lib.dbg(TAG, this.err);
		}
		//Lib.dbg("***REQ*** ", this.uri+" complete in "+(System.currentTimeMillis() - begin)+"ms");
	}

	private void getdata(DataInputStream in) {
		long beg = System.currentTimeMillis();
//Lib.dbg("***PU0***", "about to read "+this.ContentLength+" bytes");
		try {
			this.data = new byte[this.ContentLength];
			in.readFully(this.data);
		} catch (IOException ie) {
			this.data = null;
			Log.e("***PUERR***", ie.getMessage());
			Lib.dbg(TAG, ie.getMessage());
		} catch (Exception e) {
			Log.e("***PUER2***", e.getMessage());
		}
//Lib.dbg("LEN", "Content-Length: "+this.ContentLength+", read "+this.data.length+" bytes in "+Lib.rtime(beg)+"ms");
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
