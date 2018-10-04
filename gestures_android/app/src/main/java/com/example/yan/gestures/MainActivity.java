package com.example.yan.gestures;

import android.content.Intent;
import android.gesture.GestureOverlayView;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;

public class MainActivity extends AppCompatActivity {
    private LocalBroadcastManager lbm;
    private ArrayList<String[]> gesData = new ArrayList<>();
    private VelocityTracker mVelocityTracker;
    private Map<Integer,String[]> keyDatas = new HashMap();
    private boolean sig = true;
    private boolean start = false;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d("pressed",String.valueOf(keyCode));
        boolean valid = false;
        String[] keyData = new String[4];
        String ss = String.valueOf(System.currentTimeMillis());
        keyData[0] = ss.substring(ss.length()-6); keyData[1] = "9";
        switch (keyCode){
            case KeyEvent.KEYCODE_NUMPAD_0:
                valid = true;
                keyData[2] = "0";
            case KeyEvent.KEYCODE_NUMPAD_1:
                valid = true;
                keyData[2] = "1";
            case KeyEvent.KEYCODE_NUMPAD_2:
                valid = true;
                keyData[2] = "2";
            case KeyEvent.KEYCODE_NUMPAD_3:
                valid = true;
                keyData[2] = "3";
            case KeyEvent.KEYCODE_NUMPAD_4:
                valid = true;
                keyData[2] = "4";
            case KeyEvent.KEYCODE_NUMPAD_5:
                valid = true;
                keyData[2] = "5";
            case KeyEvent.KEYCODE_NUMPAD_6:
                valid = true;
                keyData[2] = "6";
            case KeyEvent.KEYCODE_NUMPAD_7:
                valid = true;
                keyData[2] = "7";
            case KeyEvent.KEYCODE_NUMPAD_8:
                valid = true;
                keyData[2] = "8";
            case KeyEvent.KEYCODE_NUMPAD_9:
                valid = true;
                keyData[2] = "9";
            case KeyEvent.KEYCODE_ENTER:
                valid = true;
                keyData[2] = "Enter";
        }
        if(valid && !keyDatas.containsKey(keyCode)){
            keyDatas.put(keyCode,keyData);
        }
        if(sig){
            Intent cmd=new Intent("MainActivity");
            cmd.putExtra("data","start");
            lbm.sendBroadcast(cmd);
            sig = false;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d("key", "keyup");
        boolean sent = false;
        if (keyDatas.containsKey(keyCode)) {
            String ss = String.valueOf(System.currentTimeMillis());
            ss = ss.substring(ss.length() - 6);
            keyDatas.get(keyCode)[3] = String.valueOf(ss);
            Intent data = new Intent("MainActivity");
            ArrayList<String[]> tmp = new ArrayList<>();
            tmp.add(keyDatas.get(keyCode));
            data.putExtra("data", this.ObjectToJson(tmp));
            this.lbm.sendBroadcastSync(data);
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {}
            keyDatas.remove(keyCode);
            if (keyDatas.size() == 0) {
                Intent cmd = new Intent("MainActivity");
                cmd.putExtra("data", "stop");
                lbm.sendBroadcast(cmd);
                sig = true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        TextView tex = findViewById(R.id.logo);
        tex.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(start){
                    start = false;
                    Intent cmd=new Intent("logo");
                    cmd.putExtra("data","stop");
                    lbm.sendBroadcast(cmd);
                    return;
                }
                start = true;
                Intent cmd=new Intent("logo");
                cmd.putExtra("data","start");
                lbm.sendBroadcast(cmd);
                return;
            }
        });
        startService(new Intent(getApplicationContext(), netService.class));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        GestureOverlayView geov = findViewById(R.id.gestures);
        geov.setGestureVisible(true);
        geov.addOnGestureListener(new GestureOverlayView.OnGestureListener() {
            @Override
            public void onGestureStarted(GestureOverlayView gestureOverlayView,
                                         MotionEvent motionEvent) {
                //Log.e("gesture","started");
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
                //Log.e("gesture","ongoing");
                gatherSamples(motionEvent);
            }
            @Override
            public void onGestureEnded(GestureOverlayView gestureOverlayView,
                                       MotionEvent motionEvent) {
                //Log.e("gesture","ended");
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
                //Log.e("gesture","canceled");
                if(sendSamples()) {
                    Intent cmd = new Intent("MainActivity");
                    cmd.putExtra("data", "stop");
                    lbm.sendBroadcast(cmd);
                }
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
        long diff = System.currentTimeMillis() - SystemClock.uptimeMillis();
        final int historySize = ev.getHistorySize();
        final int pointerCount = ev.getPointerCount();
        this.mVelocityTracker.addMovement(ev);
        this.mVelocityTracker.computeCurrentVelocity(1000);
//        for (int h = 0; h < historySize; h++) {
//            for (int p = 0; p < pointerCount; p++) {
//                String[] aGes = new String[9];
//                String ss = String.valueOf(ev.getHistoricalEventTime(h));
//                aGes[0] = ss.substring(ss.length()-6);
//                aGes[1] = String.valueOf(ev.getPointerId(p));
//                aGes[2] = String.valueOf(ev.getHistoricalX(p,h));
//                aGes[3] = String.valueOf(ev.getHistoricalY(p,h));
//                aGes[4] = String.valueOf(ev.getHistoricalPressure(p,h));
//                aGes[5] = String.valueOf(ev.getHistoricalSize(p,h));
//                aGes[6] = "0.0";
//                aGes[7] = "0.0";
//                aGes[8] = String.valueOf(ev.getHistoricalOrientation(p,h));
//                this.gesData.add(aGes);
//            }
//        }
        for (int p = 0; p < pointerCount; p++) {
            String[] aGes = new String[9];
            String ss = String.valueOf(ev.getEventTime()+diff);
            aGes[0] = ss.substring(ss.length()-6);
            aGes[1] = String.valueOf(ev.getPointerId(p));
            aGes[2] = String.valueOf(ev.getX(p));
            aGes[3] = String.valueOf(ev.getY(p));
            aGes[4] = String.valueOf(ev.getPressure());
            aGes[5] = String.valueOf(ev.getSize());
            aGes[6] = String.valueOf(this.mVelocityTracker.getXVelocity(p));
            aGes[7] = String.valueOf(this.mVelocityTracker.getYVelocity(p));
            aGes[8] = String.valueOf(ev.getOrientation());
            this.gesData.add(aGes);
        }
    }
    public boolean sendSamples(){
        Intent data=new Intent("MainActivity");
        data.putExtra("data",this.ObjectToJson(this.gesData));
        this.gesData.clear();
        this.lbm.sendBroadcastSync(data);
        try {
            Thread.sleep(30);
        }catch(InterruptedException e) {
            return true;
        }
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
