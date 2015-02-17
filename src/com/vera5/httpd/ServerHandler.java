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

	public ServerHandler(Socket client, Handler handler, Config cfg) {
		this.toClient = client;
		this.handler = handler;
		this.cfg = cfg;
	}

	public void run() {

		long begin = System.currentTimeMillis();

		request = new Request(this);
		request.get(toClient);
		Response response = new Response(this);
		if (request.uri == null) {		// FIXME Investigate why/when this happens
			Lib.logE("(null) requested");
			response.plainResponse("400", "Bad request");
			return;
		}

		ServerService.log.request(request);		// FIXME Best place?!
		switch(request.method) {
			case 1:		// GET
			case 2:		// HEAD
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
				Lib.logE(this.err);
		}
		//if (request.uri.endsWith(".ttf") | request.uri.equals("/nick.css") | request.uri.endsWith(".html"))
			Lib.dbg("PERF", request.uri+" served in "+Lib.rtime(begin)+"ms");
	}

}
