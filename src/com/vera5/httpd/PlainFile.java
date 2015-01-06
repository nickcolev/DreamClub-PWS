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
	public String ETag;
	public boolean exists = false;
	private String fname;
	private File f;

	public PlainFile(String fname) {
		this.fname = fname;
		f = new File(fname);
		if (f.exists()) {
			SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
			this.time = sdf.format(f.lastModified());
			this.ETag = ETag(f);
			this.length = (int)f.length();
			this.type = guessContentType(fname);
			this.exists = true;
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
				Log.e("PWS.PlainFile", e.getMessage());
			}
		}
	}

	private String ETag (File f) {
		// usually MD5, but RFC2616 doesn't say it -- we just use file size and last modified
		return "" + f.length() + f.lastModified();
	}

	private String guessContentType (String fname) {
		FileNameMap map = URLConnection.getFileNameMap();
		String type = map.getContentTypeFor(fname);
		if (null == type) type = "application/octet-stream";
		// My env doesn't recognize SVG
		if(fname.endsWith(".svg")) type = "text/xml";
		return type;
	}

}
