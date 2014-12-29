package com.vera5.httpd;

import android.app.AlertDialog;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class ServerService extends Service {

	private static final String TAG = "PWS.Service";
	private static final int SERVERPORT = 8088;
    private int NOTIFICATION_ID = 4711;
    private NotificationManager mNM;
	private SharedPreferences prefs;
    private String message;
    private Notification notification;
    private Server server;
	private boolean isRunning = false;
    private Thread serviceThread = null;
	private ServerSocket serverSocket;
	private Intent intent;
	public Handler handler;


    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		notification = new Notification(R.drawable.ic_launcher, "Starting...", System.currentTimeMillis());
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		showNotification();
    }

	private void showNotification() {
		updateNotifiction("");
		startForeground(NOTIFICATION_ID, notification);
	}

	public void init(Handler handler) {
		this.handler = handler;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		this.intent = intent;
		final int currentId = startId;
		Runnable r = new Runnable() {
			public void run() {
				Socket client = null;
				try {
					serverSocket = new ServerSocket(SERVERPORT);
				} catch (IOException e) {
					updateNotifiction(e.getMessage());
				}
				try {
					String s = "Waiting for connections on " + SERVERPORT;
					updateNotifiction(s);
					log(s);
					while (!Thread.currentThread().isInterrupted()) {
						client = serverSocket.accept();
						Log.i(TAG, "request  from " + client.getInetAddress().toString());
						//ServerHandler h = new ServerHandler(client);
						//new Thread(h).start();
					}
					serverSocket.close();
					stopSelf();
Log.d(TAG, "*********");
					updateNotifiction("Service stopped");
				} catch (Exception ie) {
					Log.d(TAG, "Thread shutting down...");
				} finally {
Log.d(TAG, "***finally***");
					serviceThread = null;
				}
			}
		};

		serviceThread = new Thread(r);
		serviceThread.start();
		return Service.START_STICKY;
	}

	// FIXME DELME
    public void startServer(Handler handler, String documentRoot, int port) {
		this.handler = handler;
    	try {
String s = prefs.getString("port", "");
Toast.makeText(this, "Port: "+s, Toast.LENGTH_SHORT).show();

			isRunning = true;
    		WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
    		WifiInfo wifiInfo = wifiManager.getConnectionInfo(); 		
    		String ipAddress = intToIp(wifiInfo.getIpAddress());
    		// Work around when WiFi not connected
    		if(ipAddress.equals("0.0.0.0")) ipAddress = "localhost";
			server = new Server(handler, documentRoot, ipAddress, port, getApplicationContext());
			server.start();
	        Intent i = new Intent(this, StartActivity.class);
	        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i, 0);
	    	updateNotifiction("running on " + ipAddress + ":" + port);
    	} catch (Exception e) {
    		isRunning = false;
    		Log.e(TAG, e.getMessage());
	        updateNotifiction("Error: " + e.getMessage());
    	}
    }

    public static String intToIp(int i) {
        return ((i       ) & 0xFF) + "." +
               ((i >>  8 ) & 0xFF) + "." +
               ((i >> 16 ) & 0xFF) + "." +
               ( i >> 24   & 0xFF);
    }
    
    public void stopServer() {
    	if(null != server) {
			server.stopServer();
			server.interrupt();
			isRunning = false;
    	}
    }
    
	public void updateNotifiction(String message) {
		CharSequence text = message;
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, StartActivity.class), 0);
		notification.setLatestEventInfo(this, getString(R.string.app_name), text, contentIntent);
		mNM.notify(NOTIFICATION_ID, notification);
	}

	public void log(String s) {
		Message msg = new Message();
		Bundle b = new Bundle();
		b.putString("msg", s);
		msg.setData(b);
		handler.sendMessage(msg);
	}

    @Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
    	ServerService getService() {
            return ServerService.this;
        }
    }
    
    public boolean isRunning() {
    	return isRunning;
    }
}
