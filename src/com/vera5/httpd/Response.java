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
  private DataOutputStream out;
  public String err = "";
  public long started;

	public Response(ServerHandler parent) {
		this.cfg = parent.cfg;
		this.client = parent.toClient;
		this.request = parent.request;
		this.started = System.currentTimeMillis();
		try {
			out = new DataOutputStream(client.getOutputStream());
		} catch (IOException e) {
			this.err = e.getMessage();
			Lib.errlog(TAG, this.err);
		}
	}

	private void cgiResponse(Request request) {
		if (request.ContentType == null)
			request.ContentType = "application/octet-stream";
		String output = CGI.exec(request);
		this.err = "Internal Server Error";
		if (output == null)
			plainResponse("500", this.err);
		else {
			if (output.startsWith("Content-Type:")) {
				reply((baseHeader("200")+output).getBytes());
				this.err = "";
			} else
				plainResponse("500", this.err);
		}
		try { out.close(); } catch (IOException e) {}
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

	public boolean fileResponse(PlainFile doc) {
		// Caching
		boolean isHTML = doc.type.equals("text/html");
		String ETag = doc.ETag;
		if (isHTML && ServerService.footer != null)
			ETag = doc.ETag(ServerService.footer.length);
		if (request.IfNoneMatch != null)
			if (!doc.isCGI)
				if (request.IfNoneMatch.equals(ETag))
					return NotModified(doc.type);
		int len = 0;
		if (this.request.method == 1) {
			doc.get();
			len = doc.content.length;
		}
		String header = header("200 OK", doc.type, len)
			+ "\nETag: " + ETag
			+ "\nModified: " + doc.time
			+ (doc.status == 2 ? "\nContent-Encoding: gzip" : "")
			+ "\n\n";
		boolean r = true;
		try {
			out.write(header.getBytes());
			out.flush();
			if(request.method == 1)		// GET
				out.write(doc.content, 0, doc.content.length);
			out.flush();
			out.close();
		} catch (Exception e) {
			this.err = e.getMessage();
			Lib.logV(this.err);
			Lib.logE(this.err);
			r = false;
		}
		return r;
	}

	public void get(Request request) {
		if (pws(request)) return;	// PWS command (internal processing)
		PlainFile doc = new PlainFile(request);
		if (doc.f.exists()) {
			if (doc.f.isDirectory()) {
				PlainFile index = new PlainFile(request, Lib.addIndex(request.uri, request.cfg.index));
				if (index.f.exists()) {
					fileResponse(index);
				} else {
					if (request.uri.equals("/")) {
						if (request.method == 1)
							plainResponse("200", this.cfg.defaultIndex.toString());
						else
							hOut("404");
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
		String s = "<html><head><title>"+request.uri+"</title><style>td { font-family: monospace; padding-left:5px;}</style></head><body><p>Content of "+request.uri+"</p><table>";
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

	public boolean notExists(PlainFile doc) {
		this.err = " not found";
		Lib.logV(this.request.uri+this.err);
		return plainResponse("404", this.request.uri+this.err);
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
			if (output == null) {
				this.err = "Internal Server Error";
				plainResponse("501", this.err);
			} else
				reply("HTTP/1.1 200 OK\n"+output);
		} else
			plainResponse("404", request.uri+" not found");
	}

	public boolean put(Request request) {
		boolean ok = false;
		try {
			FileOutputStream fd = new FileOutputStream(this.cfg.root+request.uri, false);
			fd.write(request.data);
			fd.close();
			ok = true;
		} catch (IOException e) {
			this.err = e.getMessage();
		}
		return ok;
	}

	private boolean pws(Request request) {
		// ?something
		if (request.uri.equals("/") && request.args != null) {
			// ?log[=key[:{*|c|d|e|i|t}]]
			if (request.args.startsWith("log")) {
				String slog = ServerService.log.get(request.args.substring(3));
				int len = slog.length();
				if (request.gzipAccepted()) {
					byte[] zlog = Lib.gzip(slog.getBytes());
					String[] aHeader = {
						"Content-Type: text/plain",
						"Content-Length: " + zlog.length,
						"Content-Encoding: gzip"
					};
					String header = header("200", aHeader);
					byte[] buf = Lib.join(header, zlog);
					return reply(buf);
				} else
					return plainResponse("200", slog);
			}
			if (request.args.equals("config")) {
				// web config -- config[=on&wifi=1...
				return plainResponse("200", request.cfg.setupPage());
			}
		}
		return false;
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
			if (!request.KeepAlive) out.close();
			ok = true;
		} catch (IOException e) {
			this.err = e.getMessage();
			Lib.errlog(TAG, this.err);
		}
		return ok;
	}

	public boolean reply(String data) {		// Overloaded
		return reply(data.getBytes());
	}

	private String baseHeader(String code) {
		// cross-domain requests (CORS)
		String cors = this.cfg.CORS ?
			"\nAccess-Control-Allow-Origin: *\nAccess-Control-Allow-Methods: " + this.request.getMethods()
			: "";
		return "HTTP/1.1 " + code
			+ "\nServer: PWS/" + this.cfg.version + " @Android "+android.os.Build.VERSION.RELEASE
			+ cors
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

	// Aliases & Helpers
	public boolean Forbidden() { return plainResponse("403", "Forbidden"); }
	public boolean NotModified(String ContentType) {
		return reply(header("304", ContentType, 0)+"\nDate: "+Lib.now()+"\n\n");
	}
}
