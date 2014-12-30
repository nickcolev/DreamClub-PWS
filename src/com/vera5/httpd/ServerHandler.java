package com.vera5.httpd;

import java.io.*;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.SimpleDateFormat;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

class ServerHandler extends Thread {

  private static final String TAG = "PWS.Handler";
  private BufferedReader in;
  private PrintWriter out;
  private Socket toClient;
  private String documentRoot;
  private Context context;
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

	String dokument = getDokument(request.uri);

	// Folder? -- add 'index.html'
	try {
		File f = new File(dokument);
		if (f.exists()) {
			if (f.isDirectory())
				dokument += (dokument.endsWith("/") ? "" : "/")	+ "index.html";
		}
	} catch (Exception e) {}

    Log.d(TAG, "Serving " + dokument);

	try {
		File f = new File(dokument);
		if (f.exists()) {
			// Caching
			if (null != request.IfNoneMatch) {
				if (request.IfNoneMatch.equals(ETag(f))) {
					plainResponse(304, "Not Modified");
					closeConnection();
					return;
				}
			}
			FileInputStream in = new FileInputStream(dokument);
			OutputStream out = toClient.getOutputStream();
			String header = getHeader (200, guessContentType(dokument), f);
			out.write(header.getBytes());
			if(!request.method.equals("HEAD")) {
				byte[] buf = new byte[8192];
				int count = 0;
				while((count = in.read(buf)) != -1) {
					out.write(buf, 0, count);
				}
			}
			out.flush();
			log(dokument);
		} else {
			log(dokument+" not found");
			plainResponse(404, request.uri + " not found");
		}
	} catch (Exception e) {}
  }

  private void plainResponse (int code, String msg) {

	try {
		out = new PrintWriter (toClient.getOutputStream(), true);
		out.print (getHeader(code, "text/plain", msg));
		out.print (msg);
		out.flush ();
	} catch (Exception e) {}
  }

  private void closeConnection () {
  	try {
		Server.remove(toClient);
		toClient.close();
	} catch (Exception e) {}
  }

  private String ETag (File f) {

	// usually MD5, but RFC2616 doesn't say it -- we just use file size and last modified
	return "" + f.length() + f.lastModified();
  }

  private String getDokument (String fname) {
	String s = "";
	try {
		s = fname.replaceFirst ("\\?(.*)","");
		s = URLDecoder.decode (s);
	} catch (Exception e) { s = fname; }
	return cfg.DocumentRoot + s;
  }

  private String guessContentType (String dokument) {

	FileNameMap map = URLConnection.getFileNameMap();
	String type = map.getContentTypeFor(dokument);
	if (null == type) type = "application/octet-stream";
	// FIXME My env doesn't recognize SVG
	if(dokument.endsWith(".svg")) type = "text/xml";
	return type;
  }

  private String getHeaderBase (int code, String type) {
	return	"HTTP/1.1 " + code
		+ "\nContent-Type: " + type
		+ "\nServer: AndroidWebserver/" + Server.version
		+ "\nConnection: close";
   }

  private String getHeader (int code, String type, File f) {
	SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
	return	getHeaderBase (code, type)
		+ "\nContent-Length: " + f.length()
		+ "\nETag: " + ETag(f)
		+ "\nLast-Modified: " + sdf.format(f.lastModified())
		+ "\n\n";
  }

  // Overloaded
  private String getHeader (int code, String type, String msg) {
	return	getHeaderBase (code, type)
		+ "\nContent-Length: " + msg.length()
		+ "\n\n";
  }

	// FIXME Same in the service
	private void log(String s) {
		Message msg = new Message();
		Bundle b = new Bundle();
		b.putString("msg", s);
		msg.setData(b);
		handler.sendMessage(msg);
	}

}
