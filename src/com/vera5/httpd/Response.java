package com.vera5.httpd;

import java.io.DataOutputStream;
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
  private DataOutputStream out;

	public Response(ServerHandler parent) {
		this.cfg = parent.cfg;
		this.client = parent.toClient;
		this.request = parent.request;
		try {
			out = new DataOutputStream(client.getOutputStream());
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}
	}

	private void cgiResponse(Request request) {
		if (request.ContentType == null)
			request.ContentType = "application/octet-stream";
		String output = CGI.exec(request);
		String err = "Internal Server Error";
		if (output == null)
			plainResponse("500", err);
		else {
			if (output.startsWith("Content-Type:"))
				reply((baseHeader("200")+output).getBytes());
			else
				plainResponse("500", err);
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
		PlainFile doc = new PlainFile(request);
		if (doc.f.exists()) {
			if (doc.f.isDirectory()) {
				PlainFile index = new PlainFile(request, Lib.addIndex(request.uri, request.cfg.index));
				if (index.f.exists()) {
					fileResponse(index);
				} else {
					if (request.uri.equals("/")) {
						plainResponse("200", this.cfg.defaultIndex.toString());
					} else {
						if (this.cfg.dir_list)
							ls(request);
						else
							Forbidden();
					}
				}
			} else {	// file exists
				if (doc.isCGI)
					cgiResponse(request);
				else
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
			PlainFile doc = new PlainFile(request);
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
		PlainFile doc = new PlainFile(request);
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
		Lib.logI(msg);
		try {
			FileOutputStream fd = new FileOutputStream(this.cfg.root+request.uri, false);
			fd.write(request.data);
			fd.close();
			ok = true;
		} catch (IOException e) {
			Lib.logE(e.getMessage());
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
			case 'd':
			case 'e':
			case 'i':
			case 's':
			case '?':
				String slog = ServerService.log.get(Character.toUpperCase(c));
				int len = slog.length();
				if (request.AcceptEncoding != null) {	// FIXME duplicated code
					if (request.AcceptEncoding.contains("gzip")) {
						byte[] zlog = Lib.gzip(slog.getBytes());
						String[] aHeader = {
							"Content-Type: text/plain",
							"Content-Length: " + zlog.length,
							"Content-Encoding: gzip"
						};
						String header = header("200", aHeader);
						byte[] buf = Lib.join(header, zlog);
						reply(buf);
					}
				} else
					plainResponse("200 OK", slog);
				break;
			default:
				isLog = false;
		}
		return isLog;
	}

	public boolean fileResponse(PlainFile doc) {
		// Caching
		boolean isHTML = doc.type.equals("text/html");
		int fl = ServerService.footer == null ? 0 : ServerService.footer.length;
		if (!isHTML) fl = 0;
		String ETag = doc.ETag + (isHTML ? "F" : "");
		if (request.IfNoneMatch != null)
			if (request.IfNoneMatch.equals(ETag))
				return NotModified(doc.type);
		if (this.request.method == 1) {
			doc.get();
		}
		String header = header("200 OK", doc.type, fl + doc.content.length)
			+ "\nETag: " + ETag
			+ "\nModified: " + doc.time
			+ (doc.status == 2 ? "\nContent-Encoding: gzip" : "")
			+ "\n\n";
Lib.dbg("***FR***", this.request.uri+" length()="+doc.f.length()+", data length="+doc.content.length);
		boolean r = true;
		try {
			out.write(header.getBytes());
			if(request.method == 1) {		// GET
				out.write(doc.content, 0, doc.content.length);
				// Footer -- maybe better to insert it before </body>
				if (fl > 0 && isHTML)
					out.write(ServerService.footer, 0, ServerService.footer.length);
			}
			out.flush();
			out.close();
			Lib.logI(this.request.log + this.request.uri);
		} catch (Exception e) {
			String err = e.getMessage();
			Lib.logV(err);
			Lib.logE(err);
			this.err = err;
			r = false;
		}
		return r;
	}

	public boolean plainResponse(String code, String msg) {
		String ContentType = msg.length() == 0 ? "" :
			"text/" + (msg.startsWith("<") ? "html" : "plain");
		String response = header(code, ContentType, msg.length())
			+ "\n\n" + msg;
		return reply(response.getBytes());
	}

	public boolean hOut(String code) {	// Header-only Out
		return reply(header(code, "", 0)+"\n\n");
	}

	public boolean hOut(String code, String[] a) {	// Overloaded
		return reply(header(code, a));
	}

	public boolean hOut(String code, byte[] b) {	// Overloaded
		String s = "";
		for (int i=0; i<b.length; i++) s += (char)b[i];
		return plainResponse("200", s);
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
		return baseHeader(code) + Lib.a2h(header) + "\n\n";
	}

	public boolean notExists(PlainFile doc) {
		this.err = this.request.log + this.request.uri + "--not found";
		Lib.logE(this.err);
		Lib.logV(this.err);
		return plainResponse("404", this.request.uri+" not found");
	}

	// Aliases & Helpers
	public boolean Forbidden() { return plainResponse("403", "Forbidden"); }
	public boolean NotModified(String ContentType) {
		return reply(header("304", ContentType, 0)+"\nDate: "+Lib.now()+"\n\n");
	}
}
