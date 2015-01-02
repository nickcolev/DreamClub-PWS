package com.vera5.httpd;

import android.app.AlertDialog;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
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
import java.net.InetAddress;
import java.net.Socket;
import java.net.ServerSocket;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class ServerService extends Service {

	private static final String TAG = "PWS.Service";
    private int NOTIFICATION_ID = 4711;
    private NotificationManager mNM;
    private Notification notification;
	private boolean isRunning = false;
    private Thread serviceThread = null;
	private ServerSocket serverSocket;
	private SharedPreferences prefs;
	private Intent intent;
	private Handler handler;
	private Config cfg;


    @Override
    public void onCreate() {
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		notification = new Notification(R.drawable.ic_launcher, "Starting...", System.currentTimeMillis());
		showNotification();
    }

	private void showNotification() {
		updateNotifiction("");
		startForeground(NOTIFICATION_ID, notification);
	}

	public void init(Handler handler, Config cfg) {
		this.handler = handler;
		this.cfg = cfg;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		try {
			this.serverSocket.close();
			stopSelf();
			isRunning = false;
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}
        return true;	// allow rebind
}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		this.intent = intent;
		final int currentId = startId;
		Runnable r = new Runnable() {
			public void run() {
				Log.i(TAG, "Starting...");
				Socket client = null;
				InetAddress localhost;
				try {
					localhost = InetAddress.getLocalHost();
					serverSocket = new ServerSocket(cfg.port, 0, localhost);
				} catch (IOException e) {
					updateNotifiction(e.getMessage());
					log(e.getMessage());
					return;
				}
				try {
					String s = localhost.getHostAddress() + ":" + cfg.port;
					updateNotifiction(s);
					log("Waiting for connections on " + s);
					while (!Thread.currentThread().isInterrupted()) {
						client = serverSocket.accept();
						log("request  from " + client.getInetAddress().toString());
						ServerHandler h = new ServerHandler(client, handler, cfg);
						new Thread(h).start();
					}
				} catch (Exception ie) {
					Log.i(TAG, "Shutting down...");
					serviceThread = null;
				}
			}
		};

		serviceThread = new Thread(r);
		serviceThread.start();
		return Service.START_STICKY;
	}

	private void Tooltip(String s) {
		Toast.makeText(ServerService.this, s, Toast.LENGTH_SHORT).show();
	}

	public void updateNotifiction(String message) {
		CharSequence text = message;
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, StartActivity.class), 0);
		notification.setLatestEventInfo(this, getString(R.string.app_name), text, contentIntent);
		mNM.notify(NOTIFICATION_ID, notification);
	}

	private void log(String s) {
		Message msg = new Message();
		Bundle b = new Bundle();
		b.putString("msg", s);
		msg.setData(b);
		handler.sendMessage(msg);
	}

	private String getIP() {
		WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		String IP = intToIp(wifiInfo.getIpAddress());
		return (IP.equals("0.0.0.0") ? "localhost" : IP);
	}

	public static String intToIp(int i) {
		return	((i       ) & 0xFF) + "." +
				((i >>  8 ) & 0xFF) + "." +
				((i >> 16 ) & 0xFF) + "." +
				( i >> 24   & 0xFF);
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
