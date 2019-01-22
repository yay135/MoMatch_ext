package com.watch.yan.watchc;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.watchface.Gles2WatchFaceService;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class netService extends Service {
    //android id
    private String android_id;
    //Binder Usage
    private final IBinder mBinder = new LocalBinder();
    private SensorService mService;
    // define a tcp member
    private TCPc mTCP;
    //trigger sensor
    public static boolean trigger =false;
    //create a new thread for tcp
    public static void CreateNewThread(final Runnable runnable) {
        final Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } finally {}
            }
        };
        t.start();
    }

    //Binder Usage
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    @Override
    public void onCreate() {
        this.android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);
        final LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        final ConnectivityManager cm =
                (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        final WifiManager fm = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        mTCP = new TCPc(lbm,fm,cm,new TCPc.OnMessageReceived() {
            @Override
            public void messageReceived(String message) {
                if (message.equals("canStart")){
                    Intent intent = new Intent("beep");
                    lbm.sendBroadcast(intent);
                }
                if (message.equals("TYPE")) {
                    String t = "SWT_"+android_id;
                    sendMSG(t);
                }
                if (message.equals("time")) {
                        for (int i=0; i<10; i++) {
                            sendMSG(Long.toString(System.currentTimeMillis()) + "t");
                            try {
                                Thread.sleep(5);
                            } catch(InterruptedException e) {
                                System.out.println("got interrupted!");
                            }
                        }
                    sendMSG("q");
                }
                if (message.equals("cali")){
                    Intent comm = new Intent("TimeCali");
                    comm.putExtra("cali","cal");
                    lbm.sendBroadcast(comm);
                }
            }
        });
    }

    private BroadcastReceiver mListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("sig");
            if(message.equals("start")){
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sendMSG("start");
                    }
                }).start();
            }
            else if(message.equals("stop")){
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sendMSG("stop");
                    }
                }).start();
            }
        }
    };
    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mListener);
        mTCP.stopClient();
        trigger = false;
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        CreateNewThread(mTCP);
        LocalBroadcastManager.getInstance(this).registerReceiver(mListener,new IntentFilter("cmdServer"));
        return super.onStartCommand(intent, flags, startId);
    }

    public void sendMSG(String msg) {
        mTCP.sendMessage(msg);
    }

    public String ObjectToJson(Object m) {
        Gson gson = new Gson();
        String json = gson.toJson(m);
        return json;
    }

    public boolean getTrigger(){
      return trigger;
    }

    public void send(Object m) {
        sendMSG(ObjectToJson(m));
    }

    //Binder Usage
    public class LocalBinder extends Binder {
        netService getService() {
            return netService.this;
        }
    }

}

