package com.vera5.httpd;

import android.os.Handler;
import android.util.Log;
import java.io.IOException;
import java.net.Socket;

class ServerHandler extends Thread {

  public Socket toClient;
  public Config cfg;
  public Request request;
  public Response response;
  private Handler handler;
  private String err;

	public ServerHandler(Socket client, Handler handler, Config cfg) {
		this.toClient = client;
		this.handler = handler;
		this.cfg = cfg;
	}

	public void run() {

		request = new Request(this);
		request.get(toClient);
		response = new Response(this);
		if (request.uri == null) {		// Net/IO error
			response.plainResponse("400", "Bad request");
			return;
		}

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
					Lib.logV("PUT failed with "+response.err);
					response.plainResponse("500", response.err);
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

		ServerService.log.request(this);

	}

}
