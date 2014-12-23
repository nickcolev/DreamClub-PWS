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

public class ServerService extends Service {

	private static final String TAG = "PWS.Service";
    private int NOTIFICATION_ID = 4711;
    private NotificationManager mNM;
    private String message;
    private Notification notification;
    private Server server;
    private boolean isRunning = false;
    
    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		showNotification();
    }
    
    private void showNotification() {
    	updateNotifiction("");
		startForeground(NOTIFICATION_ID, notification);
    }
    
    public void startServer(Handler handler, String documentRoot, int port) {
    	try {
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
	        String note = "running on " + ipAddress + ":" + port;
	        // Display to the client log	        
	    	Message msg = new Message();
	    	Bundle b = new Bundle();
	    	b.putString("msg", note);
	    	msg.setData(b);
	    	handler.sendMessage(msg);
	    	updateNotifiction(note);
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
			updateNotifiction("Server stopped");
			isRunning = false;
    	}
    }
    
    public void updateNotifiction(String message) {
        CharSequence text = message;
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, StartActivity.class), 0);
        // FIXME Optimization possible below
        if (notification == null) {
	        notification = new Notification(R.drawable.ic_launcher, text, System.currentTimeMillis());
	        notification.setLatestEventInfo(this, getString(R.string.app_name), text, contentIntent);
	        mNM.notify(NOTIFICATION_ID, notification);
        } else {
        	notification.setLatestEventInfo(this, getString(R.string.app_name), text, contentIntent);
        	mNM.notify(NOTIFICATION_ID, notification);
        }
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
