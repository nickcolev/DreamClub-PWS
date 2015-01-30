package com.vera5.httpd;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import android.util.Log;

public class Response {

  private final Config cfg;
  private final Socket client;
  private final Request request;
  private static final String TAG = "PWS.Response";
  private String err;
  private OutputStream out;

	public Response(ServerHandler parent) {
		this.cfg = parent.cfg;
		this.client = parent.toClient;
		this.request = parent.request;
		try {
			out = client.getOutputStream();
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}
	}

	public void delete(Request request) {
		File f = new File(cfg.root+request.uri);
		if (!f.exists())
			hOut("404 Not Found");
		else if (!f.canWrite())
			hOut("403 Forbidden");
		else if (f.delete())
			hOut("200 OK");
		else
			hOut("405 Not Allowed");
	}

	public void options(Request request) {
		if (request.uri.endsWith("*")) {	// General, a.k.a. ping
			hOut("200", new String[]{
				"Allow: "+request.getMethods()
			});
		} else {	// Particular resource
			PlainFile doc = new PlainFile(this.cfg.root+request.uri);
			if (doc.exists) {
				hOut("200", new String[]{
					"Allow: GET"
				});
			} else {
				hOut("200", new String[]{
					"Allow: None"
				});
			}
		}
	}

	public void post(Request request) {
		String uri = Lib.getDokument(request.uri);
		String path = cfg.root + uri;
		PlainFile doc = new PlainFile(path);
		if (doc.f.exists()) {
			String output = CGI.exec(request,this.cfg.root);
			if (output == null)
				plainResponse("501", "Internal Server Error");
			else
				reply("HTTP/1.1 200 OK\r\n"+output);
		} else
			plainResponse("404", request.uri+" not found");
	}

	public boolean put(Request request) {
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

	public boolean putLog(Request request) {
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

	public boolean fileResponse(PlainFile doc) {
		// Caching
		boolean isHTML = doc.type.equals("text/html");
		String ETag = doc.ETag + (isHTML ? ServerService.footer.ETag : "");
		if (null != request.IfNoneMatch) {
			if (request.IfNoneMatch.equals(ETag)) {
				reply(header("304", doc.type, 0)
					+ "\nDate: "+Lib.now()+"\n\n");
				return true;
			}
		}
		int l = isHTML ? ServerService.footer.length : 0;
		String header = header("200 OK", doc.type, l + doc.length)
			+ "\nETag: " + ETag
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

	public void plainResponse(String code, String msg) {
		String ContentType = msg.length() == 0 ? "" :
			"text/" + (msg.startsWith("<") ? "html" : "plain");
		String response = header(code, ContentType, msg.length())
			+ "\n\n" + msg;
		reply(response);
	}

	public boolean hOut(String code) {	// Header-only Out
		return reply(header(code, "", 0)+"\n\n");
	}

	public boolean hOut(String code, String[] a) {	// Overloaded
		return reply(header(code, a));
	}

	public boolean reply(String data) {
		boolean ok = false;
Log.d(TAG, data);
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
			+ "\nAccess-Control-Allow-Methods: " + request.getMethods()
			+ "\nConnection: close";
	}

	private String header(String code, String[] header) {	// Overloaded
		return header(code, "", 0) + Lib.a2h(header) + "\n\n";
	}

	public boolean notExists(PlainFile doc) {
		logV(this.request.uri+" not found");
		logE(this.request.log + this.request.uri + "--not found");
		if (this.request.uri.equals("/")) {
			plainResponse("200", cfg.defaultIndex.toString());
		} else {
			plainResponse("404", this.request.uri+" not found");
		}
		this.err = "not exists";
		return false;
	}

	// Aliases
	private void logE(String s) { ServerService.log.e(s); }
	private void logI(String s) { ServerService.log.i(s); }
	private void logS(String s) { ServerService.log.s(s); }
	private void logV(String s) { ServerService.log.v(s); }
}
