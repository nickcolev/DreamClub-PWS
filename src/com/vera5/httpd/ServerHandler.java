package com.vera5.httpd;

import android.os.Handler;
import android.util.Log;
import java.io.IOException;
import java.net.Socket;

class ServerHandler extends Thread {

  public Socket toClient;
  public Config cfg;
  public PlainFile cache;
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
		* For Timer()/TimerTask() implementation, see
		* http://www.java2s.com/Code/Java/Development-Class/UsejavautilTimertoscheduleatasktoexecuteonce5secondshavepassed.htm
		* http://docs.oracle.com/javase/7/docs/api/java/util/Timer.html
		* http://www.gamedev.net/topic/303695-setting-up-an-ontimer-/
		* http://stackoverflow.com/questions/4044726/how-to-set-a-timer-in-java
		if (request.header("Connection").equals("close")) {
			try { toClient.close(); }
			catch (IOException e) { }
		}
		*/
	}

	// Aliases
	private void logE(String s) { ServerService.log.e(s); }
	private void logI(String s) { ServerService.log.i(s); }
	private void logS(String s) { ServerService.log.s(s); }
	private void logV(String s) { ServerService.log.v(s); }
}
