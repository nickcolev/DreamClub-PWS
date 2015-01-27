package com.vera5.httpd;

import android.os.Handler;
import android.util.Log;
import java.io.IOException;
import java.net.Socket;

class ServerHandler extends Thread {

  private Socket toClient;
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
		Response response = new Response(cfg, toClient);
		response.send(request);
		if (request.header("Connection").equals("close")) {
			try { toClient.close(); }
			catch (IOException e) { }
		}
	}

}
