package com.vera5.httpd;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class ServerService extends Service {

	private static final String TAG = "PWS.Service";
	private static String version;
    private int NOTIFICATION_ID = 4711;
    private NotificationManager mNM;
    private Notification notification;
	private boolean isRunning = false;
    private Thread serviceThread = null;
	private ServerSocket serverSocket;
	private Intent intent;
	private SharedPreferences prefs;
	public Config cfg;
	public Handler handler;
	public static byte[] footer;
	public static Logger log;
	private static final int sockBufSize = 4096;


    @Override
    public void onCreate() {
		log = new Logger(getFilesDir().getPath());
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		notification = new Notification(R.drawable.icon24, "Starting", System.currentTimeMillis());
		updateNotifiction("");
		startForeground(NOTIFICATION_ID, notification);
		cfg = new Config();
		configure();
    }

	@Override
	public void onDestroy() {
		mNM.cancel(NOTIFICATION_ID);
		super.onDestroy();
	}

	public void configure() {
		cfg.configure(this.prefs);
		cfg.version = version();
		cfg.defaultIndex = getResources().getText(R.string.defaultIndex);
		getFooter();
	}

	public void ReStart() {
		Intent intent = new Intent(getApplicationContext(), ServerService.class);
		closeSocket(null);
		stopService(intent);
		startService(intent);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		this.intent = intent;
		final int currentId = startId;
		final String ip = getIP();
		final int port = getPort();
		log.setHandler(this.handler);
		log.s("Start at "+ip+":"+port+", Root "+this.cfg.root);
		Runnable r = new Runnable() {
			public void run() {
				Socket client = null;
				try {
					InetAddress localhost = InetAddress.getByName(ip);;
					serverSocket = new ServerSocket(port, 0, localhost);
				} catch (IOException e) {
					updateNotifiction(e.getMessage());
					log.e(e.getMessage());
					stopSelf();
					return;
				}
				String s = ip + ":" + port;
				updateNotifiction(s);
				log.v("Waiting for connections on " + s);
				try {
					WifiLock(true);
					while (!Thread.currentThread().isInterrupted()) {
						client = serverSocket.accept();
						s = "request  from " + client.getInetAddress().toString();
						log.v(s);
						ServerHandler h = new ServerHandler(client, handler, cfg);
						new Thread(h).start();
					}
				} catch (Exception e) {
					log.s("Shutdown");
					WifiLock(false);
					closeSocket(client);
					serviceThread = null;
					stopSelf();
				}
			}
		};

		serviceThread = new Thread(r);
		serviceThread.start();
		return Service.START_STICKY;
	}

	public void getFooter() {
		String fname = prefs.getString("footer", "");
		fname = Lib.sanify(prefs.getString("root", "/sdcard/htdocs")) + Lib.sanify(fname);
		File f = new File(fname);
		if (f.exists() && f.isFile())
			try {
				FileInputStream in = new FileInputStream(f);
				int l = (int)f.length();
				this.footer = new byte[l];
				l = in.read(this.footer, 0, l);
				in.close();
				// FIXME What if it is not a text/html?!
				Tooltip("Setting "+fname);
			} catch (IOException e) {
			}
	}

	private int getPort() {
		return Integer.parseInt(prefs.getString("port", "8080"));
	}

	public void closeSocket(Socket client) {
		try {
			serverSocket.close();
			if (client != null) client.close();
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}
	}

	private void Tooltip(String s) {
		Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
	}

	public void updateNotifiction(String message) {
		CharSequence text = message;
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, StartActivity.class), 0);
		notification.setLatestEventInfo(this, getString(R.string.app_name), text, contentIntent);
		mNM.notify(NOTIFICATION_ID, notification);
	}

	private String getIP() {
		String IP;
		try {
			final WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			IP = Lib.intToIp(wifiInfo.getIpAddress());
			if (IP.equals("0.0.0.0")) IP = "localhost";
		} catch (Exception e) {
			IP = "localhost";
		}
		return IP;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		try {
			this.serverSocket.close();
			stopSelf();
			isRunning = false;
		} catch (IOException e) {
			log.e(e.getMessage());
			Log.e(TAG, e.getMessage());
		}
        return true;	// allow rebind
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

	private String version() {
		try {
			PackageManager packageManager = getPackageManager();
			PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(),0);
			return packageInfo.versionName;
		} catch (PackageManager.NameNotFoundException e) {
			Log.e(TAG, "Error while fetching app version", e);
			return "?";
		}
	}
	
	private void WifiLock(boolean on) {
		final WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
		final WifiLock wifiLock = wm.createWifiLock("PWS");
		if (on) {
			wifiLock.acquire();
		} else {
			if (wifiLock.isHeld())
				wifiLock.release();
		}
	}

}
