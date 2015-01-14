package com.vera5.httpd;

import java.io.*;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

class ServerHandler extends Thread {

  static final int M_VIEW = 1;
  static final int M_FILE = 2;
  
  private static final String TAG = "PWS.Handler";
  private BufferedReader in;
  private PrintWriter out;
  private Socket toClient;
  private Handler handler;
  private Config cfg;

	public ServerHandler(Socket s, Handler handler, Config cfg) {
		this.toClient = s;
		this.handler = handler;
		this.cfg = cfg;
	}

	public void run() {

		Request request = new Request();
		request.get(toClient);
		response(request);
	}

	private void response(Request request) {		// TODO Implement HEAD/POST/PUT/DELETE

		if (request.uri == null) {
			logE("(null) requested");
			return;
		}

		if (request.uri.equals("/log")) {
Log.d("***CP33***", "log request");
			plainResponse(200, "text/plain", ServerService.log.get());
			return;
		}

		String dokument = getDokument(request.uri);
		String path = cfg.root + dokument;

		// Process
		try {
			File f = new File(path);
			if (f.exists()) {
				PlainFile doc = new PlainFile(path);
				String ETag = doc.ETag + ServerService.footer.ETag;
// FIXME Combined file/footer ETag
Log.d("***CP36***", "ETag: "+ETag);
				// Caching
				if (null != request.IfNoneMatch) {
					if (request.IfNoneMatch.equals(doc.ETag)) {
						plainResponse(304, doc.type, "Not Modified");	// FIXME Do we need msg for 304?
						return;
					}
				}
				doc.get();
				boolean isHTML = doc.type.equals("text/html");
				int l = isHTML ? ServerService.footer.length : 0;
				OutputStream out = toClient.getOutputStream();
				String header = getHeader (doc, l);
				out.write(header.getBytes());
				if(!request.method.equals("HEAD")) {
					out.write(doc.content, 0, doc.length);
				}
				// Footer -- maybe better to insert it before </body>
				if (ServerService.footer.length > 0 && isHTML) {
					out.write(ServerService.footer.content, 0, ServerService.footer.length);
				}
				out.flush();
				logI(request.log + dokument);
			} else {
				logV(dokument+" not found");
				logE(request.log + dokument + "--not found");
				if (dokument.equals("/"+cfg.index)) {
					plainResponse(200, "text/html", cfg.defaultIndex);
				} else {
					plainResponse(403, "Forbidden");	// FIXME Rather "Not found". Forbidden when there's not default index.
				}
			}
		} catch (Exception e) {
			logV(e.getMessage());
			logE(e.getMessage());
		}
	}

	private void plainResponse (int code, String type, CharSequence msg) {
		try {
			// FIXME Use OutputStream as above?!
			out = new PrintWriter (toClient.getOutputStream(), true);
			out.print (getHeader(code, type, msg));
			out.print (msg);
			out.flush ();
		} catch (Exception e) {}
	}

	// Overload
	private void plainResponse (int code, String msg) {
		plainResponse(code, "text/plain", msg);
	}

	private String getDokument (String fname) {
		if (fname == null) return cfg.root;
		String s = fname.replaceFirst ("[\\?#](.*)","");	// Strip after ? or #
		try {
			s = URLDecoder.decode (s);
		} catch (Exception e) {
			s = fname;
		}
		// Folder? -- add default document index name
		try {
			File f = new File(cfg.root + s);
			if (f.exists())
				if (f.isDirectory())
					s += (s.endsWith("/") ? "" : "/")	+ cfg.index;
		} catch (Exception e) {}
		return s;
	}

	private String getHeaderBase (int code, String type, int len) {
		return	"HTTP/1.1 " + code
			+ "\nContent-Type: " + type
			+ "\nContent-Length: " + len
			+ "\nServer: PWS/" + cfg.version
			+ "\nConnection: close";
	}

	private String getHeader (PlainFile doc, int len) {
		// BUG: 'int' allows up to ~2GB file. It's OK for web content, but...?!
		return	getHeaderBase (200, doc.type, len + doc.length)
			+ "\nETag: " + doc.ETag
			+ "\nLast-Modified: " + doc.time
			+ "\n\n";
	}

	// Overloaded
	private String getHeader (int code, String type, CharSequence msg) {
		return	getHeaderBase (code, type, msg.length()) + "\n\n";
	}

	// Aliases
	private void logE(String s) { ServerService.log.e(s); }
	private void logI(String s) { ServerService.log.i(s); }
	private void logV(String s) { ServerService.log.v(s); }
	private void log(String s, int mask) {
		if ((mask & M_FILE) == M_FILE) ServerService.log.i(s);
		if ((mask & M_VIEW) == M_VIEW) ServerService.log.v(s);
	}

}
