package com.vera5.httpd;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import android.util.Log;

public class Response {

  private final ServerHandler parent;
  private final Config cfg;
  private final Socket client;
  private final Request request;
  private static final String TAG = "PWS.Response";
  private String err;
  private OutputStream out;

	public Response(ServerHandler parent) {
		this.parent = parent;
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
		File f = new File(this.cfg.root+request.uri);
		if (!f.exists())
			hOut("404 Not Found");
		else if (!f.canWrite())
			hOut("403 Forbidden");
		else if (f.delete())
			hOut("200 OK");
		else
			hOut("405 Not Allowed");
	}

	public void get(Request request) {
		String path = this.cfg.root + request.uri;
		PlainFile doc = new PlainFile(path);
		if (doc.f.exists()) {
			if (doc.f.isDirectory()) {
				PlainFile index = new PlainFile(Lib.addIndex(path, this.cfg.index));
				if (index.f.exists()) {
					fileResponse(index);
				} else {
					if (request.uri.equals("/"))
						plainResponse("200", this.cfg.defaultIndex.toString());
					else
						ls(request);	// FIXME Is allowed
						//Forbidden();	// FIXME Implement preference
				}
			} else {
				fileResponse(doc);
			}
		} else {
			notExists(doc);
		}
	}

	public void ls(Request request) {
		File dir = new File(this.cfg.root+request.uri);
		String s = "<html><head><title>"+request.uri+"</title></head><body><p>Content of "+request.uri+"</p><table>";
		if (dir.isDirectory()) {
			long bytes = 0;
			File[] files = dir.listFiles();
			for (File f : files) {
				bytes += f.length();
				s += "<tr>" +
					"<td align=\"right\">" + Lib.fileAttr(f) + "</td>" +
					"<td align=\"right\">" + f.length() + "</td>" +
					"<td>" + f.getName() + "</td>" +
					"</tr>";
			}
			s += "</table><p><small>"+bytes+" bytes in "+files.length+" files</small></p></body></html>";
			plainResponse("200", s);
		} else
			plainResponse("403", "Forbidden");
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
		PlainFile doc = new PlainFile(this.cfg.root+request.uri);
		if (doc.f.exists()) {
			String output = CGI.exec(request);
			if (output == null)
				plainResponse("501", "Internal Server Error");
			else
				reply("HTTP/1.1 200 OK\r\n"+output);
		} else
			plainResponse("404", request.uri+" not found");
	}

	public boolean put(Request request) {
		boolean ok = false;
		String msg = "PUT "+request.uri+", len="+request.ContentLength;
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
		if (this.parent.cache.fname != null)
			if (this.parent.cache.isCGI) {
				String output = CGI.exec(this.request);
				if (output == null)
					plainResponse("500", "Internal Server Error");
				else {
					if (!output.startsWith("HTTP"))
						output = baseHeader("200") + output;
					return reply(output);
				}
			}
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
				doc.get(this.request);
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

	public void hOut(String code, byte[] b) {	// Overloaded
		String s = "";
		for (int i=0; i<b.length; i++) s += (char)b[i];
		plainResponse("200", s);
	}

	public boolean reply(byte[] data) {
		boolean ok = false;
		try {
			out.write(data, 0, data.length);
			out.flush();
			out.close();
			ok = true;
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}
		return ok;
	}

	public boolean reply(String data) {		// Overloaded
		return reply(data.getBytes());
	}

	private String baseHeader(String code) {
		return "HTTP/1.1 " + code
			+ "\nServer: PWS/" + this.cfg.version
			+ "\nAccess-Control-Allow-Origin: *"	// FIXME restrict cross-domain requests
			+ "\nAccess-Control-Allow-Methods: " + this.request.getMethods()
			+ "\nConnection: close";			
	}

	private String header(String code, String ContentType, int length) {
		return baseHeader(code)
			+ (ContentType.length()==0 ? "" : "\nContent-Type: "+ContentType)
			+ (length == -1 ? "" : "\nContent-Length: "+length);
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
			hOut("404 Not Found");
			//plainResponse("404", Lib.baseuri(this.request.url)+" not found");
		}
		this.err = "not exists";
		return false;
	}

	// Aliases & Helpers
	public void Forbidden() { plainResponse("403", "Forbidden"); }
	private void logE(String s) { ServerService.log.e(s); }
	private void logI(String s) { ServerService.log.i(s); }
	private void logS(String s) { ServerService.log.s(s); }
	private void logV(String s) { ServerService.log.v(s); }
}
