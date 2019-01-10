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
import android.os.SystemClock;
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
                } finally {

                }
            }
        };
        //start the thread.
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
                Log.d("TCP:", message);
                if (message.equals("s")) {
                    System.out.println(System.currentTimeMillis()+"_received start");
                    //netService.trigger=true;
                    Intent comm = new Intent("tcpc");
                    comm.putExtra("sig","start");
                    lbm.sendBroadcast(comm);
                    Log.e("comm",String.valueOf(System.currentTimeMillis()));
                }
                if (message.equals("e")) {
                    //netService.trigger=false;
                    Intent comm = new Intent("tcpc");
                    comm.putExtra("sig","stop");
                    lbm.sendBroadcast(comm);
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

    private BroadcastReceiver startStopListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String message = intent.getStringExtra("cmd");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    sendMSG(message);
                    Log.e("sendToServer",message);
                }
            }).start();
        }
    };
    @Override
    public void onDestroy() {
        mTCP.stopClient();
        trigger = false;
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("netService", "service Start");
        CreateNewThread(mTCP);
        // sendBroadcast to mainActivity
        Intent startNotice = new Intent("NoticeMainActivity");
        startNotice.putExtra("message","start");
        LocalBroadcastManager.getInstance(this).sendBroadcast(startNotice);
        LocalBroadcastManager.getInstance(this).registerReceiver(startStopListener,new IntentFilter("sendStartStopToServer"));
        return super.onStartCommand(intent, flags, startId);
    }

    public void sendMSG(String msg) {
        Log.e("messages",msg);
        mTCP.sendMessage(msg);
    }

    public String ObjectToJson(Object m) {
        Gson gson = new Gson();
        String json = gson.toJson(m);
        return json;
        //return json.substring(1,json.length()-1);
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

