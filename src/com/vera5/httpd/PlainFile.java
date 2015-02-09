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
			long begin = System.currentTimeMillis();
			try {
				FileInputStream in = new FileInputStream(this.f);
				ByteArrayOutputStream content = getFileBytes(this.f);
				// Add the footer
				if (ServerService.footer != null)
					if (this.type.equals("text/html"))
						content.write(ServerService.footer, 0, ServerService.footer.length);
				this.status = 1;
				// gzip (if client supports it)
				if (gzipAccept()) {
					byte[] cmp = Lib.gzip(content.toByteArray());
					content.reset();
					content.write(cmp, 0, cmp.length);
					this.status = 2;
				}
				this.content = content.toByteArray();
				if (!this.isCGI) this.isCGI = isCGI();
			} catch (IOException e) {
				this.length = 0;
				this.content = null;
				this.err = e.getMessage();
				Lib.logE(TAG+": "+this.request.uri+" "+e.getMessage());
			}
		}
	}

	private boolean gzipAccept() {
		if (this.request.AcceptEncoding == null) return false;
		return this.request.AcceptEncoding.contains("gzip");
	}

	private ByteArrayOutputStream getFileBytes(File f) throws IOException {
		long begin = System.currentTimeMillis();
		ByteArrayOutputStream ret = new ByteArrayOutputStream();
		FileInputStream in = new FileInputStream(f);
		byte[] buf = new byte[4096];
		int cnt = 0;
		while ((cnt = in.read(buf, 0, 4096)) != -1)
			ret.write(buf, 0, cnt);
		return ret;
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

}
