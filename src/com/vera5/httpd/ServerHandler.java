package com.vera5.httpd;

import android.os.Handler;
import android.util.Log;
import java.io.IOException;
import java.net.Socket;

class ServerHandler extends Thread {

  public Socket toClient;
  public Config cfg;
  public PlainFile cache;	// FIXME DELME
  public Request request;
  private Handler handler;
  private String err;

	public ServerHandler(Socket s, Handler handler, Config cfg) {
		this.toClient = s;
		this.handler = handler;
		this.cfg = cfg;
	}

	public void run() {

		request = new Request(this);
		request.get(toClient);
		if (request.uri == null) {		// FIXME Investigate why/when this happens
			Lib.logE("(null) requested");
			return;
		}
		Response response = new Response(this);
		switch(request.method) {
			case 1:		// GET
			case 2:		// HEAD
				// log, loge, logi, logs
				if (request.uri.startsWith("/log") && request.uri.length() < 6)
					response.putLog(request);
				else
					response.get(request);
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
					Lib.logV("PUT failed with "+this.err);
					Lib.logE("PUT failed with "+this.err);
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
				Lib.logS(this.err);
		}

	}

}
