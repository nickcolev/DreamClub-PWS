package com.vera5.httpd;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URLDecoder;
import android.util.Log;

public class Response {

  private final Socket client;
  private static final String TAG = "PWS.Response";
  private String err;
  private OutputStream out;
  private Config cfg;
  private Request request;
  public String lastError;

	public Response(Config cfg, Socket client) {
		this.cfg = cfg;
		this.client = client;
		try {	// FIXME Wouldn't be better to do this at later stage?
			out = client.getOutputStream();
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}
	}

	public void send(Request request) {
		this.request = request;
		String dokument = getDokument(request.uri);
		String path = cfg.root + dokument;
		switch(request.method) {
			case 1:		// GET
			case 2:		// HEAD
				if (request.uri == null) {
					logE("(null) requested");
					return;
				}
				// log, loge, logi, logs
				if (request.uri.startsWith("/log") && request.uri.length() < 6)
					if (putLog(request)) return;
				// Process
				PlainFile doc = new PlainFile(path);
				if (doc.exists)
					fileResponse(doc);
				else
					notExists(doc);
				break;
			case 6:		// PUT
				if (put(request)) {
					plainResponse("201", request.uri+" OK");
				} else {
					logV("POST not implemented yet");
					plainResponse("500", this.lastError);
				}
				break;
			// other methods
			default:
				String err = request.aMethod[request.method]+" Not Implemented";
				plainResponse("501", err);
				logS(err);
		}
	}

	private boolean put(Request request) {
		boolean ok = false;
		try {
			FileOutputStream fd = new FileOutputStream(this.cfg.root+request.uri);
			fd.write(request.data, 0, request.ContentLength);
			fd.close();
			ok = true;
		} catch (IOException e) {
			this.lastError = e.getMessage();
		}
		return ok;
	}

	private boolean putLog(Request request) {
		boolean isLog = true;
		char c = request.uri.length() == 4 ? '?' : request.uri.charAt(4);
		switch(c) {
			case 'c':
				///ServerService.log.clean();
				plainResponse("200 OK", "log reset!");
				break;
			case 'd':	// Debug
				plainResponse("200 OK", "debug");
			case 'e':
			case 'i':
			case 's':
				ServerService.log.get(this.client, Character.toUpperCase(c));
				break;
			case '?':
				ServerService.log.get(this.client, c);
				break;
			default:
				isLog = false;
		}
		return isLog;
	}

	private boolean fileResponse(PlainFile doc) {
		// Caching
		if (null != request.IfNoneMatch) {
			if (request.IfNoneMatch.equals(doc.ETag)) {
				plainResponse("304", "");
				return true;
			}
		}
		String ETag = doc.ETag + ServerService.footer.ETag;
		boolean isHTML = doc.type.equals("text/html");
		int l = isHTML ? ServerService.footer.length : 0;
		String header = header("200 OK", doc.type, l + doc.length)
			+ "\nETag: " + doc.ETag
			+ "\nModified: " + doc.time
			+ "\n\n";
		boolean r = true;
		try {
			out.write(header.getBytes());
			if(request.method != 2) {		// HEAD
				doc.get();
				out.write(doc.content, 0, doc.length);
				// Footer -- maybe better to insert it before </body>
				if (ServerService.footer.length > 0 && isHTML)
					out.write(ServerService.footer.content, 0, ServerService.footer.length);
			}
			out.flush();
			logI(this.request.log + this.request.uri);
		} catch (Exception e) {
			String err = e.getMessage();
			logV(err);
			logE(err);
			this.err = err;
			r = false;
		}
		return r;
	}

	private void plainResponse(String code, String msg) {
		String ContentType = msg.length() == 0 ? "" :
			"text/" + (msg.startsWith("<") ? "html" : "plain");
		String response = header(code, ContentType, msg.length())
			+ "\n\n" + msg;
		try {
			out.write(response.getBytes(), 0, response.length());
			out.flush();
			out.close();
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}
	}

	private String header(String code, String ContentType, int length) {
		return	"HTTP/1.1 " + code
			+ (ContentType.equals("") ? "" : "\nContent-Type: "+ContentType)
			+ (length > 0 ? "\nContent-Length: "+length : "")
			+ "\nServer: PWS/" + cfg.version
			+ "\nAccess-Control-Allow-Origin: *"	// FIXME restrict cross-domain requests
			+ "\nConnection: close";
	}

	private String getDokument (String fname) {
		if (fname == null) fname = "/";
		if (fname.equals("/")) return "/"+cfg.index;
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

	private boolean notExists(PlainFile doc) {
		logV(this.request.uri+" not found");
		logE(this.request.log + this.request.uri + "--not found");
		if (this.request.uri.equals("/")) {
			plainResponse("200", cfg.defaultIndex.toString());
		} else if (isDir()) {
			plainResponse("403", "Forbidden");
		} else {
			plainResponse("404", this.request.uri+" not found");
		}
		this.err = "not exists";
		return false;
	}

	private boolean isDir() {
		File f = new File(cfg.root + this.request.uri);
		if (!f.exists()) return false;
		return f.isDirectory();
	}

	// Aliases
	private void logE(String s) { ServerService.log.e(s); }
	private void logI(String s) { ServerService.log.i(s); }
	private void logS(String s) { ServerService.log.s(s); }
	private void logV(String s) { ServerService.log.v(s); }
}
