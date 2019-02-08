package com.watch.yan.rotary;


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
    private TextView status;
    private boolean buttonSate = true;

    private BroadcastReceiver mListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("sig");
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

    private BroadcastReceiver beepListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            beep();
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startButton = findViewById(R.id.button5);
        status = findViewById(R.id.textView);
        status.setText("welcome...");
        final LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);

        startService(new Intent(getApplicationContext(),netService.class));
        //200ms after connection stable
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startService(new Intent(getApplicationContext(), SensorService.class));
            }
        }, 200);
        // Enables Always-on
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                startButton.setEnabled(false);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startButton.setEnabled(true);
                    }
                }, 1000);
                if(buttonSate) {
                    startButton.setText("Stop");
                    Intent intent0 = new Intent("cmdServer");
                    intent0.putExtra("sig", "start");
                    lbm.sendBroadcastSync(intent0);
                    Intent intent1 = new Intent("cmdSensorService");
                    intent1.putExtra("sig", "start");
                    lbm.sendBroadcastSync(intent1);
                    buttonSate = false;
                    status.setText("started");
                }else{
                    startButton.setText("Start");
                    Intent intent0 = new Intent("cmdServer");
                    intent0.putExtra("sig", "stop");
                    lbm.sendBroadcastSync(intent0);
                    Intent intent1 = new Intent("cmdSensorService");
                    intent1.putExtra("sig", "stop");
                    lbm.sendBroadcastSync(intent1);
                    buttonSate = true;
                }
            }
        });
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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(beepListener);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mListener);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(caliListener);
    }
    @Override
    public void onResume(){
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(beepListener,new IntentFilter("beep"));
        LocalBroadcastManager.getInstance(this).registerReceiver(mListener,new IntentFilter("tcpc"));
        LocalBroadcastManager.getInstance(this).registerReceiver(caliListener,new IntentFilter("sensorService"));
    }

    public void beep(){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
                toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,150);
                status.setText("stopped");
            }
        },100);
    }
}
