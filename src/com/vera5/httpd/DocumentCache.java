package com.vera5.httpd;

class DocumentCache extends Thread {

  private final Request request;
  private final Config cfg;

	public DocumentCache(Request request) {
		this.request = request;
		this.cfg = request.cfg;
	}

	public void run() {
		if (this.request.uri != null) {
			String path  = this.cfg.root + this.request.uri;
			this.request.parent.cache = new PlainFile(
				Lib.addIndex(path, this.cfg.index));
			/* pre-fetch
			if (request.parent.cache.f.exists())
				request.parent.cache.get(this.request);
			*/
		}
		this.interrupt();
	}
}
