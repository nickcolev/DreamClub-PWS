package com.vera5.httpd;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class Server extends Thread {

	private static final String TAG = "PWS.Server";
	private ServerSocket listener = null;
	private boolean running = true;
	private String documentRoot;
	private static Handler mHandler;
	private Context context;
	
	public static LinkedList<Socket> clientList = new LinkedList<Socket>();
	public static String version;

    public Server(Handler handler, String documentRoot, String ip, int port, Context context) throws IOException {
		super();
		this.documentRoot = documentRoot;
		this.context = context;
		Server.mHandler = handler;
		this.version = getVersion();
		InetAddress ipadr = InetAddress.getByName(ip);
		listener = new ServerSocket(port,0,ipadr);
	}

	@Override
	public void run() {
		while( running ) {
			try {
				send("Waiting for connections");
				Socket client = listener.accept();
				send("New connection from " + client.getInetAddress().toString());
				new ServerHandler(documentRoot, context, client).start();
				clientList.add(client);
			} catch (IOException e) {
				send(e.getMessage());
				Log.e("Webserver", e.getMessage());
			}
		}
	}

	public void stopServer() {
		running = false;
		try {
			listener.close();
		} catch (IOException e) {
			send(e.getMessage());
			Log.e("Webserver", e.getMessage());
		}
	}
	
	public synchronized static void remove(Socket s) {
	    send("Closing connection: " + s.getInetAddress().toString());
        clientList.remove(s);      
    }
	
	private String getVersion() {
		String ver = "0.0";
		try {
			PackageManager manager = this.context.getPackageManager();
			PackageInfo info = manager.getPackageInfo(this.context.getPackageName(), 0);
			ver = info.versionName;
		} catch (Exception e) {
			Log.e("Webserver", e.getMessage());
		}
		return ver;
	}

    public static void send(String s) {
    	if(s != null) {
	    	Message msg = new Message();
	    	Bundle b = new Bundle();
	    	b.putString("msg", s);
	    	msg.setData(b);
	    	mHandler.sendMessage(msg);
    	}
    }
}
