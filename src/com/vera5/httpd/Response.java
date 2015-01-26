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
import java.text.SimpleDateFormat;
import java.util.Date;
import android.util.Log;

public class Response {

  private final Socket client;
  private static final String TAG = "PWS.Response";
  private String err;
  private OutputStream out;
  private Config cfg;
  private Request request;

	public Response(Config cfg, Socket client) {
		this.cfg = cfg;
		this.client = client;
		try {
			out = client.getOutputStream();
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}
	}

	public void send(Request request) {
		if (request.uri == null) {		// Investigate why/when this happens
			logE("(null) requested");
			return;
		}
		this.request = request;
		String dokument = getDokument(request.uri);
		String path = cfg.root + dokument;
		switch(request.method) {
			case 1:		// GET
			case 2:		// HEAD
				// log, loge, logi, logs
				if (request.uri.startsWith("/log") && request.uri.length() < 6)
					if (putLog(request)) return;
				PlainFile doc = new PlainFile(path);
				if (doc.exists)
					fileResponse(doc);
				else
					notExists(doc);
				break;
			case 3:		// OPTIONS
				options(request);
				break;
			case 4:		// TRACE
				// see https://www.owasp.org/index.php/Test_HTTP_Methods_(OTG-CONFIG-006)
				plainResponse("200", request.headers());
				break;
			case 5:		// PUT
				if (put(request)) {
					plainResponse("201", request.uri+" OK");
				} else {
					logV("PUT failed with "+this.err);
					logE("PUT failed with "+this.err);
					plainResponse("500", this.err);
				}
				break;
			// other methods
			default:
				this.err = request.getMethod()+" Not Implemented";
				plainResponse("501", this.err);
				logS(this.err);
		}
	}

	private void options(Request request) {
		if (request.uri.endsWith("*")) {	// General, a.k.a. ping
			reply(header("200", "", 0)+"\nAllow: "+request.getMethods()+"\n\n");
		} else {	// Particular resource
			PlainFile doc = new PlainFile(cfg.root+request.uri);
			if (doc.exists) {
				reply(header("200", doc.type, 0)+"\nAllow: GET\n\n");
			} else {
				reply(header("200", "", 0)+"\nAllow: None\n\n");
			}
		}
	}

	private boolean put(Request request) {
		boolean ok = false;
		String msg = "PUT "+request.uri+", len="+request.ContentLength+", data="+request.data;
		logI(msg);
		try {
			FileOutputStream fd = new FileOutputStream(this.cfg.root+request.uri, false);
			fd.write(request.data);
			fd.close();
			ok = true;
		} catch (IOException e) {
			logE(e.getMessage());
			this.err = e.getMessage();
		}
		return ok;
	}

	private boolean putLog(Request request) {
		boolean isLog = true;
		char c = request.uri.length() == 4 ? '?' : request.uri.charAt(4);
		switch(c) {
			case 'c':
				ServerService.log.clean();
				plainResponse("200 OK", "log reset!");
				break;
			case 'e':
			case 'i':
			case 's':
			case '?':
				plainResponse("200 OK",
					ServerService.log.get(Character.toUpperCase(c)));
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
				reply(header("304", doc.type, 0)
					+ "\nDate: "+now()+"\n\n");
				//plainResponse("304", "");
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
		reply(response);
	}

	private boolean reply(String data) {
		boolean ok = false;
		try {
			out.write(data.getBytes(), 0, data.length());
			out.flush();
			out.close();
			ok = true;
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}
		return ok;
	}

	private String header(String code, String ContentType, int length) {
		return	"HTTP/1.1 " + code
			+ (ContentType.equals("") ? "" : "\nContent-Type: "+ContentType)
			+ (length == -1 ? "" : "\nContent-Length: "+length)
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
		} else if (doc.isDir) {
			plainResponse("403", "Forbidden");
		} else {
			plainResponse("404", this.request.uri+" not found");
		}
		this.err = "not exists";
		return false;
	}

	private String now() {
		SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
		return sdf.format(new Date());
	}

	// Aliases
	private void logE(String s) { ServerService.log.e(s); }
	private void logI(String s) { ServerService.log.i(s); }
	private void logS(String s) { ServerService.log.s(s); }
	private void logV(String s) { ServerService.log.v(s); }
}
