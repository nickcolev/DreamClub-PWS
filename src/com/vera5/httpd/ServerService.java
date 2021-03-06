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
import android.os.PowerManager;
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
  private static final int NOTIFICATION_ID = 4711;
  private static final int sockBufSize = 4096;
  private static String version;
  private NotificationManager mNM;
  private Notification notification;
  private boolean isRunning = false;
  private Thread serviceThread = null;
  private ServerSocket serverSocket;
  private int lastClient;		// Hash
  private long lastTimestamp;
  private Intent intent;
  private SharedPreferences prefs;
  public static Context context;
  public Config cfg;
  public Handler handler;
  public static byte[] footer;
  public static Logger log;
  //
  private WifiManager wifiMan;
  private WifiLock wifiLock;
  private PowerManager pwrMan;
  private PowerManager.WakeLock wakeLock;
 

    @Override
    public void onCreate() {
		this.context = getApplicationContext();
		log = new Logger(this);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		notification = new Notification(R.drawable.ic_launcher, "Starting", System.currentTimeMillis());
		updateNotifiction("");
		startForeground(NOTIFICATION_ID, notification);
		cfg = new Config(this);
		configure();
		if (!mkRoot()) Tooltip("Failed to create document root "+cfg.root);
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
		this.wifiMan = (WifiManager) getSystemService(WIFI_SERVICE);
		this.wifiLock = this.wifiMan.createWifiLock("PWS");
		this.pwrMan = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = this.pwrMan.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PWS");
	}

	public void ReStart() {
		closeSocket(null);
		stopService(intent);
		startService(intent);
	}

	private boolean setServerSocket(String ip, int port) {
		boolean ok = true;
		try {
			InetAddress localhost = InetAddress.getByName(ip);
			serverSocket = new ServerSocket(port, 0, localhost);
		} catch (IOException e) {
			updateNotifiction(e.getMessage());
			log.e(e.getMessage());
			log.v(e.getMessage());
			stopSelf();
			ok = false;
		}
		return ok;
	}

	private void tuneClient(Socket s) {
		try {
			s.setSendBufferSize(4096);
			s.setTrafficClass(8);
			s.setTcpNoDelay(true);
			s.setKeepAlive(true);
		} catch (SocketException e) {
			Lib.logE(e.getMessage());
			Lib.logV(e.getMessage());
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		this.intent = intent;
		final int currentId = startId;
		final String ip = getIP();
		final int port = getPort();
		log.setHandler(this.handler);
		log.s("Start at "+ip+":"+port+", Root "+this.cfg.root);
		log.v("Personal Web Server "+this.cfg.version);
		Runnable r = new Runnable() {
			public void run() {
				if (serverSocket == null)
					if (!setServerSocket(ip, port)) return;
				if (cfg.wake_lock) WakeLock(true);
				if (cfg.wifi_lock) WifiLock(true);
				Socket client = null;
				String s = ip + ":" + port;
				updateNotifiction(s);
				log.v("Waiting for connections on " + s);
				// FIXME
				// 1. Can we move some logic out of the run()?! like serverSocket creation, etc.
				// 3. Minimize log(s) in the cycle (can't we put it in the thread?)
				try {
					while (!Thread.currentThread().isInterrupted()) {
						client = serverSocket.accept();
						tuneClient(client);
						log.setClient(client);
						if (!sameClient(client))
							log.v("request  from " + Lib.clientIP(client));
						ServerHandler h = new ServerHandler(client, handler, cfg);
						new Thread(h).start();
					}
				} catch (Exception e) {
					log.s("Shutdown");
					WifiLock(false);
					WakeLock(false);
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

	protected boolean sameClient(Socket s) {
		int currClient = s.getInetAddress().hashCode();
		long ms = System.currentTimeMillis();
		if (lastClient == 0) {
			lastClient = currClient;
			lastTimestamp = ms;
			return false;
		}
		// Criteria: same IP & request within 2500 ms (FIXME Good enough?!)
		boolean same = currClient == lastClient & ((ms - lastTimestamp) < 2500);
//Log.d("***CMP***", (same ? "" : "NOT ")+"same client");
		lastClient = currClient;
		lastTimestamp = ms;
		return same;
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
			if (serverSocket != null) {
				serverSocket.close();
				serverSocket = null;
			}
			if (client != null) {
				client.close();
				client = null;
			}
		} catch (IOException e) {
			Lib.errlog(TAG, e.getMessage());
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
			Lib.errlog(TAG, e.getMessage());
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

	private boolean mkRoot() {
		File f = new File(this.cfg.root);
		if (f.exists())
			return true;
		else
			return f.mkdir();
	}

	private String version() {
		try {
			PackageManager packageManager = getPackageManager();
			PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(),0);
			return packageInfo.versionName;
		} catch (PackageManager.NameNotFoundException e) {
			Lib.errlog(TAG, "Get app version failed with "+e.getMessage());
			return "?";
		}
	}

	public void WakeLock(boolean on) {
		if (on) {
			this.wakeLock.acquire();
		} else {
			if (this.wakeLock.isHeld())
				this.wakeLock.release();
		}
	}

	public void WifiLock(boolean on) {
		if (on && this.wifiMan.isWifiEnabled()) {
			this.wifiLock.acquire();
		} else {
			if (this.wifiLock.isHeld()) {
				this.wifiLock.release();
			}
		}
	}

}
