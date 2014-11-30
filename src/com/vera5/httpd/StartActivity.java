// TODO settings could be in activity preferences -- see http://stackoverflow.com/questions/3570690/whats-the-best-way-to-do-application-settings-in-android
package com.vera5.httpd;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class StartActivity extends Activity {

    private ToggleButton mToggleButton;
    private EditText port;
    private static TextView mLog;
    private static ScrollView mScroll;
    private String documentRoot;
    private String lastMessage = "";
	private ServerService mBoundService;

    final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Bundle b = msg.getData();
			log(b.getString("msg"));
		}
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mToggleButton = (ToggleButton) findViewById(R.id.toggle);
        port = (EditText) findViewById(R.id.port);
        mLog = (TextView) findViewById(R.id.log);
        mScroll = (ScrollView) findViewById(R.id.ScrollView01);
        documentRoot = getDocRoot();

        if(null != documentRoot) {
	        try {
		        if (!(new File(documentRoot)).exists()) {
		        	(new File(documentRoot)).mkdir();
		        	Log.i("***", "Created " + documentRoot);
		         	BufferedWriter bout = new BufferedWriter(new FileWriter(documentRoot + "/index.html"));
		         	bout.write(
		         		"<html><head><title>Android Webserver</title>"+
		         		"</head>"+
		         		"<body>Willkommen auf dem Android Webserver."+
		         		"<br><br>Die HTML-Dateien liegen in " + documentRoot + ", der Sourcecode dieser App auf "+
		         		"<a href=\"https://github.com/bodeme/androidwebserver\">Github</a>"+
		         		"</body></html>"
		         	);
		         	bout.flush();
		         	bout.close();
		        	Log.i("*** Webserver", "Created index.html");
		        }
	        } catch (Exception e) {
	        	Log.v("ERROR",e.getMessage());
	        }
	        log("\nPlease mail suggestions to fef9560@b0d3.de\nDocument-Root: " + documentRoot);
        } else {
            log("Error: Document-Root could not be found.");
        }
        
        mToggleButton.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				if(mToggleButton.isChecked()) {
					startServer(mHandler, documentRoot, new Integer(port.getText().toString()));
				} else {
					stopServer();
				}
			}
		});

        doBindService();
        // TODO Start server on Activity start
    }

    public static void log( String s ) {
    	mLog.append(s + "\n");
    	mScroll.fullScroll(ScrollView.FOCUS_DOWN);
    }
    
    private void startServer(Handler handler, String documentRoot, int port) {
    	if (mBoundService == null) {
	        Toast.makeText(StartActivity.this, "Service not connected", Toast.LENGTH_SHORT).show();
		} else {
			mBoundService.startServer(handler, documentRoot, port);
		}
    }
    
    private void stopServer() { 
    	if (mBoundService == null) {
	        Toast.makeText(StartActivity.this, "Service not connected", Toast.LENGTH_SHORT).show();
		} else {
			mBoundService.stopServer();
		}
    }
    
    private ServiceConnection mConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className, IBinder service) {
	        mBoundService = ((ServerService.LocalBinder)service).getService();
	        Toast.makeText(StartActivity.this, "Service connected", Toast.LENGTH_SHORT).show();
	        mBoundService.updateNotifiction(lastMessage);
	        
	        mToggleButton.setChecked(mBoundService.isRunning());
	    }

	    public void onServiceDisconnected(ComponentName className) {
	        mBoundService = null;
	        Toast.makeText(StartActivity.this, "Service disconnected", Toast.LENGTH_SHORT).show();
	    }
	};

	private void doUnbindService() {
    	if (mBoundService != null) {
	        unbindService(mConnection);
	    }
	}
	
	private void doBindService() {
	    bindService(new Intent(StartActivity.this, ServerService.class), mConnection, Context.BIND_AUTO_CREATE);
	}


	@Override
	protected void onDestroy() {
	    super.onDestroy();
	    doUnbindService();
	}
	

	@Override
	protected void onResume() {
		super.onResume();
	}
	
	private String getDocRoot() {		// Warning: no trailing '/'
		return Environment.getExternalStorageDirectory().getAbsolutePath() + "/htdocs";
	}
}
