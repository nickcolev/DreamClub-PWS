package com.vera5.httpd;

import android.os.Handler;
import android.util.Log;
import java.io.IOException;
import java.net.Socket;

class ServerHandler extends Thread {

  public Socket toClient;
  public Config cfg;
  public Request request;
  private Handler handler;
  private String err;

	public ServerHandler(Socket s, Handler handler, Config cfg) {
		this.toClient = s;
		this.handler = handler;
		this.cfg = cfg;
	}

	public void run() {

		request = new Request();
		request.get(toClient);
		if (request.uri == null) {		// FIXME Investigate why/when this happens
			logE("(null) requested");
			return;
		}
		Response response = new Response(this);
		switch(request.method) {
			case 1:		// GET
			case 2:		// HEAD
				// log, loge, logi, logs
				if (request.uri.startsWith("/log") && request.uri.length() < 6)
					if (response.putLog(request)) return;
				String uri = Lib.getDokument(request.uri);
				String path = cfg.root + uri;
				PlainFile doc = new PlainFile(path);
				if (doc.f.exists()) {
					if (doc.f.isDirectory()) {
						PlainFile index = new PlainFile(addIndex(path));
						if (index.f.exists()) {
							response.fileResponse(index);
						} else {
							if (request.uri.equals("/"))
								response.plainResponse("200", this.cfg.defaultIndex.toString());
							else
								//response.ls(request);	// FIXME Is allowed
								response.Forbidden();	// FIXME Implement preference
						}
					} else
						response.fileResponse(doc);
				} else
					response.notExists(doc);
				break;
			case 3:		// OPTIONS
				response.options(request);
				break;
			case 4:		// TRACE
				response.plainResponse("200", request.headers());
				break;
			case 5:		// PUT
				if (response.put(request)) {
					response.hOut("201");
				} else {
					logV("PUT failed with "+this.err);
					logE("PUT failed with "+this.err);
					response.plainResponse("500", this.err);
				}
				break;
			case 6:		// POST
				response.post(request);
				break;
			case 7:		// DELETE
				response.delete(request);
				break;
			default:
				this.err = request.getMethod()+" Not Implemented";
				response.plainResponse("501", this.err);
				logS(this.err);
		}
		
		/* FIXME Implement after 10" timeout, like Timer.schedule(test, 1000);
		if (request.header("Connection").equals("close")) {
			try { toClient.close(); }
			catch (IOException e) { }
		}
		*/
	}

	private String addIndex(String name) { return name + (name.endsWith("/") ? "" : "/") + cfg.index; }

	// Aliases
	private void logE(String s) { ServerService.log.e(s); }
	private void logI(String s) { ServerService.log.i(s); }
	private void logS(String s) { ServerService.log.s(s); }
	private void logV(String s) { ServerService.log.v(s); }
}
