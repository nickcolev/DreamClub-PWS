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
						content = addFooter(content);
					}
				this.status = 1;
				// gzip (if client supports it)
				if (this.request.gzipAccepted()) {
					content = Lib.gzip(content);
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

	private byte[] addFooter(byte[] content) {
		long begin = System.currentTimeMillis();
		int p = indexOfEndBody(content);
		byte[] b = new byte[content.length+ServerService.footer.length];
		// Copy contents (up to '</body...')
		// arraycopy(Obj src,int srcPos,Obj dst,int dstPos,int len)
		System.arraycopy(content,0,b,0,p);
		// Copy the footer
		System.arraycopy(ServerService.footer,0,b,p,ServerService.footer.length);
		// Add skipped '</body...'
		System.arraycopy(content,p,b,p+ServerService.footer.length,content.length-p);
Log.d("***PF***", "run "+Lib.rtime(begin)+"ms");
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
					&& lcase(b[i+5])=='y') {
//Log.d("***PFIO***", "i="+i+" "+new String(b, i, 7));
					return i;
				}
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
