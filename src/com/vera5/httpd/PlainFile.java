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
		fname = request.cfg.root+uri.replaceFirst("^/~", "/usr/");
		f = new File(fname);
		if (f.exists()) {
			this.exists = true;
			this.fname = fname;
			if (f.isFile()) {
				SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
				this.time = sdf.format(f.lastModified());
				this.ETag = ETag(0);
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
				byte[] content = getFileBytes(this.f);
				// Add the footer
				if (ServerService.footer != null)
					if (this.type.equals("text/html")) {
						// Better to insert it before </body>
						// binarySearch()?
						//int p = indexOfEndBody(content.toByteArray());
Log.d("***PF***", this.f.getName()+" size: "+content.length);
						content = join(content, ServerService.footer);
Log.d("***PF***", "new size: "+content.length);
						// Can we move the '</body>' to the end afterwards?!
					}
				this.status = 1;
				// gzip (if client supports it)
				if (this.request.gzipAccepted()) {
					content = Lib.gzip(content);
Log.d("***PF***", "gzip size: "+content.length);
					this.status = 2;
				}
				this.content = content;
				if (!this.isCGI) this.isCGI = isCGI();
			} catch (IOException e) {
				this.length = 0;
				this.content = null;
				this.err = e.getMessage();
				Lib.logE(TAG+": "+this.request.uri+" "+e.getMessage());
			}
		}
	}

	private byte[] join(byte[] b1, byte[] b2) {
		byte[] b = new byte[b1.length+b2.length];
		for (int i=0; i<b1.length; i++) b[i] = b1[i];
		for (int j=0; j<b2.length; j++) b[j+b1.length] = b2[j];
		return b;
	}

	private byte[] getFileBytes(File f) throws IOException {
		FileInputStream in = new FileInputStream(f);
		int l = (int)f.length();
		byte[] b = new byte[l];
		in.read(b, 0, l);
		in.close();
		return b;
	}

	private int indexOfEndBody(byte[] b) {
		int i;
		try {
			// FIXME As the '</body>' is at the end, better scan backwards
			for (i=0; i<b.length; i++)
				if (b[i] == '<' && b[i+1]=='/'	// Start of closing HTML tag
					&& lcase(b[i+2])=='b'
					&& lcase(b[i+3])=='o'
					&& lcase(b[i+4])=='d'
					&& lcase(b[i+5])=='y')
Log.d("***PFIO***", "i="+i+" "+new String(b, i, 7));
					return i;
		} catch (Exception e) {}
		return b.length;
	}

	private char lcase(byte b) {
		return Character.toLowerCase((char)b);
	}

	private boolean isCGI() {
		String s = "";
		int len = this.content.length > 80 ? 80 : this.content.length;
		for (int i=0; i<len; i++)
			if (this.content[i] == 13 || this.content[i] == 10) break;
			else s += (char)this.content[i];
		return s.startsWith("#!/");
	}

	public String ETag (int salt) {
		return Lib.md5("" + (this.f.length() + this.f.lastModified() + salt));
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
				final String[] aExt = { "css", "gif", "jpg", "jpeg", "js", "png", "sh", "svg" };
				final String[] aType = { "text/css", "image/gif", "image/jpeg", "image/jpeg", "application/javascript", "image/png", "text/x-shellscript", "image/svg+xml" };
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
