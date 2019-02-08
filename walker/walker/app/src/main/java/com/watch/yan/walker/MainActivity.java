package com.watch.yan.walker;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends WearableActivity {
    private Button startButton;
    private Button stopButton;
    private TextView status;
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
             String message = intent.getStringExtra("message");
//             if(message.equals("start")){
//                 status.setText("netService start...");
//             }
//             if(message.equals("start0")){
//                 status.setText("SensorService start...");
//                 startButton.setEnabled(false);
//                 stopButton.setEnabled(true);
//             }
        }
    };

    private BroadcastReceiver mListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("sig");
            if(message.equals("start")){
                status.setText("start");
            }
            if(message.equals("stop")){
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
                        toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,150);
                        status.setText("stop");
                    }
                },100);
            }
            if(message.equals("connected")){
                Log.e("BroadCastReceiver","received "+message);
                status.setText("connected!");
            }
            if(message.equals("disconnected")){
                Log.e("BroadCastReceiver","received "+message);
                status.setText("disconnected!");
            }
        }
    };

    private BroadcastReceiver caliListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("showCali");
            status.setText(message);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startButton = findViewById(R.id.button5);
        stopButton = findViewById(R.id.button6);
        status = findViewById(R.id.textView);
        status.setText("welcome...");
        // Enables Always-on
        startService(new Intent(getApplicationContext(),netService.class));
        //200ms after connection stable
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startService(new Intent(getApplicationContext(), SensorService.class));
            }
        }, 200);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getApplicationContext());
                Intent intent = new Intent("sendStartStopToServer");
                intent.putExtra("cmd","s");
                lbm.sendBroadcast(intent);
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
            }
        });
        stopButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getApplicationContext());
                Intent intent = new Intent("sendStartStopToServer");
                intent.putExtra("cmd","e");
                lbm.sendBroadcast(intent);
//                try {
//                    stopService(new Intent(getApplicationContext(), SensorService.class));
//                    Thread.sleep(200);
//                    stopService(new Intent(getApplicationContext(), netService.class));
                    //status.setText("SensorService & netService Stop...");
                    startButton.setEnabled(true);
                    stopButton.setEnabled(false);
//                }catch (InterruptedException e){
//                    e.printStackTrace();
//                }
            }
        });
        stopButton.setEnabled(false);
        setAmbientEnabled();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON );
    }
    @Override
    public void onDestroy() {
        stopService(new Intent(getApplicationContext(), netService.class));
        stopService(new Intent(getApplicationContext(), SensorService.class));
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mListener);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(caliListener);
    }
    @Override
    public void onResume(){
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,new IntentFilter("NoticeMainActivity"));
        LocalBroadcastManager.getInstance(this).registerReceiver(mListener,new IntentFilter("tcpc"));
        LocalBroadcastManager.getInstance(this).registerReceiver(caliListener,new IntentFilter("sensorService"));
    }
}
