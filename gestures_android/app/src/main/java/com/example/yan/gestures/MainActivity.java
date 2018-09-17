package com.example.yan.gestures;

import android.content.Intent;
import android.gesture.GestureOverlayView;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;

import com.google.gson.Gson;

import java.util.ArrayList;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;

public class MainActivity extends AppCompatActivity {
    private LocalBroadcastManager lbm;
    private ArrayList<String[]> gesData = new ArrayList<>();
    private VelocityTracker mVelocityTracker;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        startService(new Intent(getApplicationContext(), netService.class));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        GestureOverlayView geov = findViewById(R.id.gestures);
        geov.setGestureVisible(true);
        geov.addOnGestureListener(new GestureOverlayView.OnGestureListener() {
            @Override
            public void onGestureStarted(GestureOverlayView gestureOverlayView,
                                         MotionEvent motionEvent) {
                Log.e("gesture","started");
                Intent cmd=new Intent("MainActivity");
                cmd.putExtra("data","start");
                lbm.sendBroadcast(cmd);
                if(mVelocityTracker==null){
                    mVelocityTracker = VelocityTracker.obtain();
                }else{
                    mVelocityTracker.clear();
                }
                gatherSamples(motionEvent);
            }
            @Override
            public void onGesture(GestureOverlayView gestureOverlayView,
                                  MotionEvent motionEvent) {
                Log.e("gesture","ongoing");
                gatherSamples(motionEvent);
            }
            @Override
            public void onGestureEnded(GestureOverlayView gestureOverlayView,
                                       MotionEvent motionEvent) {
                Log.e("gesture","ended");
                gatherSamples(motionEvent);
                if(sendSamples()) {
                    Intent cmd = new Intent("MainActivity");
                    cmd.putExtra("data", "stop");
                    lbm.sendBroadcast(cmd);
                }
            }
            @Override
            public void onGestureCancelled(GestureOverlayView gestureOverlayView,
                                           MotionEvent motionEvent) {
                Log.e("gesture","canceled");
                mVelocityTracker.recycle();
            }
        });
        this.lbm = LocalBroadcastManager.getInstance(this);
    }
    @Override
    public boolean onTouchEvent(MotionEvent event){
        int action = event.getActionMasked();
        if(action==ACTION_MOVE){
            Log.e("action",String.valueOf(action));
            Log.e("ACTION_MOVE",String.valueOf(ACTION_MOVE));
            Log.e("ACTION_DOWN",String.valueOf(ACTION_DOWN));
            Log.e("ACTION_UP",String.valueOf(ACTION_UP));
            gatherSamples(event);
            sendSamples();
        }
        return true;
    }
    public void gatherSamples(MotionEvent ev) {
        final int historySize = ev.getHistorySize();
        final int pointerCount = ev.getPointerCount();
        this.mVelocityTracker.addMovement(ev);
        this.mVelocityTracker.computeCurrentVelocity(1000);
        for (int h = 0; h < historySize; h++) {
            for (int p = 0; p < pointerCount; p++) {
                String[] aGes = new String[8];
                aGes[0] = String.valueOf(ev.getHistoricalEventTime(h));
                aGes[1] = String.valueOf(ev.getPointerId(p));
                aGes[2] = String.valueOf(ev.getHistoricalX(p,h));
                aGes[3] = String.valueOf(ev.getHistoricalY(p,h));
                aGes[4] = String.valueOf(ev.getHistoricalPressure(p,h));
                aGes[5] = String.valueOf(ev.getHistoricalSize(p,h));
                aGes[6] = "0.0";
                aGes[7] = "0.0";
                this.gesData.add(aGes);
            }
        }
        for (int p = 0; p < pointerCount; p++) {
            String[] aGes = new String[8];
            aGes[0] = String.valueOf(ev.getEventTime());
            aGes[1] = String.valueOf(ev.getPointerId(p));
            aGes[2] = String.valueOf(ev.getX(p));
            aGes[3] = String.valueOf(ev.getY(p));
            aGes[4] = String.valueOf(ev.getPressure());
            aGes[5] = String.valueOf(ev.getSize());
            aGes[6] = String.valueOf(this.mVelocityTracker.getXVelocity(p));
            aGes[7] = String.valueOf(this.mVelocityTracker.getYVelocity(p));
            this.gesData.add(aGes);
        }
    }
    public boolean sendSamples(){
        Intent data=new Intent("MainActivity");
        data.putExtra("data",this.ObjectToJson(this.gesData));
        this.gesData.clear();
        this.lbm.sendBroadcastSync(data);
        return true;
    }
    public String ObjectToJson(Object m) {
        Gson gson = new Gson();
        String json = gson.toJson(m);
        return json;
    }

    @Override
    public void onDestroy() {
        stopService(new Intent(getApplicationContext(), netService.class));
        super.onDestroy();
    }
}
