package com.vera5.httpd;

import android.util.Log;
import java.io.File;

class DocumentCache extends Thread {

  private final Request request;
  private final Config cfg;
  private String uri;

	public DocumentCache(Request request) {
		this.request = request;
		this.cfg = request.cfg;
		// Add default index to directories
		File f = new File(request.cfg.root + request.uri);
		if (f.exists()) {
			this.uri = request.uri;
			if (f.isDirectory()) this.uri +=
				(request.uri.endsWith("/") ? "" : "/") +
				request.cfg.index;
		}
	}

	public void run() {
		if (this.request.uri != null) {
			this.request.parent.cache = new PlainFile(this.request, this.uri);
			if (this.request.parent.cache.f.exists())
				if (this.request.parent.cache.f.isFile())
					this.request.parent.cache.get();
		}
		this.interrupt();
	}

}
