package com.vera5.httpd;

import java.io.*;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.SimpleDateFormat;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

class ServerHandler extends Thread {

  private static final String TAG = "PWS.Handler";
  private BufferedReader in;
  private PrintWriter out;
  private Socket toClient;
  private String documentRoot;
  private Context context;
  
	public ServerHandler(Socket s) {
		this.toClient = s;
	}

	public void run() {

	String s, a[];
	int i = 0;
	Request request = new Request();

	try {
		in = new BufferedReader(new InputStreamReader(toClient.getInputStream()), 8192);

		// Receive data
		while (true) {
			s = in.readLine().trim();
        	if (s.equals("")) break;
			a = s.split(" ");
			// The first line is the request method, resourse, and version (like 'GET /index.html HTTP/1.0')
			if (i == 0) {	// The first line
				request.method = a[0];
				request.uri = a[1].replace("/~", "/usr/");
				request.version = a[2];
			} else if (a[0].equals("Accept-Encoding:")) {
				request.AcceptEncoding = a[1];
			} else if (a[0].equals("Content-Type:")) {
				request.ContentType = a[1];
			} else if (a[0].equals("If-Modified-Since:")) {
				request.IfModifiedSince = a[1];
			} else if (a[0].equals("If-None-Match:")) {
				request.IfNoneMatch = a[1];
			}
			i++;
		}
	} catch (Exception e) {
    	Server.remove(toClient);
    	try {
    		toClient.close();
		}
    	catch (Exception ex){}
    }
    process(request);
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
	return s;
  }

  private String guessContentType (String dokument) {

	FileNameMap map = URLConnection.getFileNameMap();
	String type = map.getContentTypeFor(dokument);
	if (null == type) type = "application/octet-stream";
	// FIXME My env doesn't recognize SVG
	if(dokument.endsWith(".svg")) type = "text/xml";
	return type;
  }

  private void process(Request request) {		// TODO Implement HEAD/POST/PUT/DELETE

	String dokument = documentRoot + getDokument(request.uri);
	dokument = "/sdcard/htdocs" + request.uri;	// FIXME
Log.d("***CP33***", request.uri+" => "+dokument);

	// Folder? -- add 'index.html'
	try {
		File f = new File(dokument);
		if (f.exists()) {
			if (f.isDirectory())
				dokument += (dokument.endsWith("/") ? "" : "/")	+ "index.html";
		}
	} catch (Exception e) {}

    Log.d("Webserver", "Serving " + dokument);

	try {
		File f = new File(dokument);
		if (f.exists()) {
			// Caching
			if (null != request.IfNoneMatch) {
				if (request.IfNoneMatch.equals(ETag(f))) {
					response(304, "Not Modified");
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
		} else {
			Log.e(TAG, dokument+" not found");
			response(404, request.uri + " not found");
		}
	} catch (Exception e) {}
  }

  private void response (int code, String msg) {

	try {
		out = new PrintWriter (toClient.getOutputStream(), true);
		out.print (getHeader(code, "text/plain", msg));
		out.print (msg);
		out.flush ();
	} catch (Exception e) {}
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
}
