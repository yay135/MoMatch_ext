package com.example.yan.gestures;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class netService extends Service {
    private String android_id;
    LocalBroadcastManager lbm;
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("data");
            mTCP.sendMessage(message);
        }
    };
    private TCPc mTCP;
//    private PowerManager.WakeLock mWakeLock;
    public netService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        this.android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);
        super.onCreate();
        mTCP = new TCPc(new TCPc.OnMessageReceived() {
            @Override
            public void messageReceived(String message) {
                if(message.equals("TYPE")){
                    mTCP.sendMessage("SP_"+android_id);
                }
                if (message.equals("time")) {
                    for (int i=0; i<5; i++) {
                        mTCP.sendMessage(Long.toString(System.currentTimeMillis()) + "t");
                        try {
                            Thread.sleep(5);
                        } catch(InterruptedException e) {
                            System.out.println("got interrupted!");
                        }
                    }
                    mTCP.sendMessage("q");
                }
            }
        });
        this.lbm=LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(mReceiver,new IntentFilter("MainActivity"));
        PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);
        //this.mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "systemService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //this.mWakeLock.acquire();
        ExecutorService aThread = Executors.newSingleThreadExecutor();
        aThread.execute(mTCP);
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onDestroy() {
        mTCP.stopClient();
        super.onDestroy();
    }
}
