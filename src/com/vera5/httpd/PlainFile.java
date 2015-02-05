package com.vera5.httpd;

import android.util.Log;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

public class PlainFile {

  private static final String TAG = "PWS.PlainFile";
  private final Request request;
  public int status = 0;	// 1=conent read, 2=content gzipped
  public int length = 0;
  public String time;		// last updated
  public byte[] content;
  public String type;
  public String ETag = "";
  public String fname;
  public String ext;
  public boolean exists = false;
  public boolean isCGI = false;
  public String err;
  public File f;

	public PlainFile(Request request, String uri) {
		this.request = request;
		fname = (request.cfg.root+uri).replaceFirst("^/~", "/usr/");
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
				this.isCGI = guessCGI(fname);
			} else if (f.isDirectory()) {
				this.type = "text/directory";
			}
		}
	}

	public PlainFile(Request request) {		// Overloaded
		this(request, request.uri);
	}

	public void get() {
		if (this.f.exists()) {
			try {
				FileInputStream in = new FileInputStream(this.f);
				this.content = new byte[this.length];
				this.length = in.read(this.content, 0, this.length);
				this.status = 1;
Lib.dbg("***CP38***", this.request.uri+" Accept-Encoding: "+this.request.AcceptEncoding);
				if (!this.isCGI) this.isCGI = isCGI();
				if (this.request.AcceptEncoding != null)	// Optimization -- gzip before send
					if (this.request.AcceptEncoding.contains("gzip"))
						gzip(this.content);
			} catch (IOException e) {
				this.length = 0;
				this.content = null;
				this.err = e.getMessage();
				//Log.e(TAG, e.getMessage());
				this.request.logE(TAG+": "+this.request.uri+" "+e.getMessage());
			}
		}
	}

	private boolean isCGI() {
		String s = "";
		int len = this.content.length > 80 ? 80 : this.content.length;
		for (int i=0; i<len; i++)
			if (this.content[i] == 13 || this.content[i] == 10) break;
			else s += (char)this.content[i];
		return s.startsWith("#!/");	// FIXME Enchance the logic
	}

	private String ETag (File f) {
		// usually MD5, but RFC2616 doesn't say it -- we just use file size and last modified
		return "" + f.length() + f.lastModified();
	}

	private boolean guessCGI(final String fname) {
		boolean isCGI = false;
		int p = fname.lastIndexOf('.');
		if (p != -1) {
			this.ext = fname.substring(p + 1);
			final String[] cgi = {
				"asp", "cfm", "cgi", "jsp", "php", "pl", "py", "sh", "vbs"
			};
			isCGI = Arrays.asList(cgi).contains(this.ext);
		}
		return isCGI;
	}

	private String guessContentType (final String fname) {
		final FileNameMap map = URLConnection.getFileNameMap();
		String type = map.getContentTypeFor(fname);
		// Discrepancy in Java/Android implementation?!
		// (extensions in 'ext' are not recognized properly on my devel. env. for some reason
		if (type == null) {
			int p = fname.lastIndexOf('.');
			if (p != -1) {
				String ext = fname.substring(p+1);
				final String[] aExt = { "css", "gif", "jpg", "jpeg", "js", "png", "svg" };
				final String[] aType = { "text/css", "image/gif", "image/jpeg", "image/jpeg", "application/javascript", "image/png", "image/svg+xml" };
				for (int i=0; i<aExt.length; i++)
					if (aExt[i].equals(ext)) {
						type = aType[i];
						break;
					}
			}
		}
		if (null == type) type = "application/octet-stream";
		return type;
	}

	private void gzip(byte[] in) {
		long begin = System.currentTimeMillis();
		int length = this.length;
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream(in.length);
			GZIPOutputStream gos = new GZIPOutputStream(os);
			gos.write(in);
			gos.close();
			byte[] gzip = os.toByteArray();
			os.close();
			this.content = gzip.clone();
			this.status = 2;
Lib.dbg("***gzip*** ", this.request.uri+" -> "+length+" bytes gzipped in "+(System.currentTimeMillis()-begin)+"ms");
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}
	}

}
