package com.vera5.httpd;

import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.text.SimpleDateFormat;

public class PlainFile {

	public int length = 0;
	public String time;		// last updated
	public byte[] content;
	public String type;
	public String ETag = "";
	public String fname;
	public boolean exists = false;
	public boolean isDir = false;
	public String err;
	private File f;

	public PlainFile(String fname) {
		f = new File(fname);
		if (f.exists()) {
			this.exists = true;
			this.fname = fname;
			if (f.isFile()) {
				SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
				this.time = sdf.format(f.lastModified());
				this.ETag = ETag(f);
				this.length = (int)f.length();
				this.type = guessContentType(fname);
			} else if (f.isDirectory()) {
				this.type = "text/directory";
				this.isDir = true;
			}
		}
	}

	public void get() {
		if (this.exists) {
			try {
				FileInputStream in = new FileInputStream(this.f);
				this.content = new byte[this.length];
				this.length = in.read(this.content, 0, this.length);
			} catch (IOException e) {
				this.length = 0;
				this.content = null;
				this.err = e.getMessage();
				Log.e("PWS.PlainFile", e.getMessage());
			}
		}
	}

	private String ETag (File f) {
		// usually MD5, but RFC2616 doesn't say it -- we just use file size and last modified
		return "" + f.length() + f.lastModified();
	}

	private String guessContentType (final String fname) {
		final FileNameMap map = URLConnection.getFileNameMap();
		String type = map.getContentTypeFor(fname);
		if (null == type) type = "application/octet-stream";
		// My env doesn't recognize SVG
		if(fname.endsWith(".svg")) type = "text/xml";
		return type;
	}

	public boolean canRead() { return this.f.canRead(); }
	public boolean canWrite() { return this.f.canWrite(); }
	public boolean isDir() { return this.f.isDirectory(); }
}
