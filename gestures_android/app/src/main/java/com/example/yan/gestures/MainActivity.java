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
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;

import org.apache.commons.net.ntp.TimeInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.view.MotionEvent.ACTION_CANCEL;
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
    private Button tex;
    private Long offSet = 0L;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d("pressed",String.valueOf(keyCode));
        boolean valid = false;
        String[] keyData = new String[4];
        String ss = String.valueOf(System.currentTimeMillis()+offSet);
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
            String ss = String.valueOf(System.currentTimeMillis()+offSet);
            ss = ss.substring(ss.length() - 6);
            keyDatas.get(keyCode)[3] = String.valueOf(ss);
            Intent data = new Intent("logo");
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
    protected void onResume() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    NTPUDPClient client = new NTPUDPClient();
                    client.open();
                    InetAddress hostAddr = InetAddress.getByName("time.google.com");
                    TimeInfo info = client.getTime(hostAddr);
                    info.computeDetails(); // compute offset/delay if not already done
                    Long offsetValue = info.getOffset();
                    offSet = offsetValue;
                    Long delayValue = info.getDelay();
                    String delay = (delayValue == null) ? "N/A" : delayValue.toString();
                    String offset = (offsetValue == null) ? "N/A" : offsetValue.toString();

                    Log.e("TNPUDP", " Roundtrip delay(ms)=" + delay
                            + ", clock offset(ms)=" + offset); // offset in ms
                    client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
        this.tex = findViewById(R.id.logo);
        tex.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(start){
                    start = false;
                    Log.e("button","click1");
                    Intent cmd=new Intent("logo");
                    cmd.putExtra("data","stop");
                    lbm.sendBroadcast(cmd);
                    return;
                }
                start = true;
                Log.e("button","clicked0");
                Intent cmd=new Intent("logo");
                cmd.putExtra("data","start");
                lbm.sendBroadcast(cmd);
                return;
            }
        });
        startService(new Intent(getApplicationContext(), netService.class));
        super.onCreate(savedInstanceState);
//        GestureOverlayView geov = findViewById(R.id.gestures);
//        geov.setGestureVisible(true);
//        geov.addOnGestureListener(new GestureOverlayView.OnGestureListener() {
//            @Override
//            public void onGestureStarted(GestureOverlayView gestureOverlayView,
//                                         MotionEvent motionEvent) {
//                //Log.e("gesture","started");
//                Intent cmd=new Intent("MainActivity");
//                cmd.putExtra("data","start");
//                lbm.sendBroadcast(cmd);
//                if(mVelocityTracker==null){
//                    mVelocityTracker = VelocityTracker.obtain();
//                }else{
//                    mVelocityTracker.clear();
//                }
//                gatherSamples(motionEvent);
//            }
//            @Override
//            public void onGesture(GestureOverlayView gestureOverlayView,
//                                  MotionEvent motionEvent) {
//                //Log.e("gesture","ongoing");
//                gatherSamples(motionEvent);
//            }
//            @Override
//            public void onGestureEnded(GestureOverlayView gestureOverlayView,
//                                       MotionEvent motionEvent) {
//                //Log.e("gesture","ended");
//                gatherSamples(motionEvent);
//                if(sendSamples()) {
//                    Intent cmd = new Intent("MainActivity");
//                    cmd.putExtra("data", "stop");
//                    lbm.sendBroadcast(cmd);
//                }
//            }
//            @Override
//            public void onGestureCancelled(GestureOverlayView gestureOverlayView,
//                                           MotionEvent motionEvent) {
//                //Log.e("gesture","canceled");
//                if(sendSamples()) {
//                    Intent cmd = new Intent("MainActivity");
//                    cmd.putExtra("data", "stop");
//                    lbm.sendBroadcast(cmd);
//                }
//                mVelocityTracker.recycle();
//            }
//        });
        this.lbm = LocalBroadcastManager.getInstance(this);
    }
    @Override
    public boolean onTouchEvent(MotionEvent event){
        if(start) {
            Log.e("event type", String.valueOf(event.getAction()));
            int action = event.getAction();
            if (action == ACTION_DOWN) {
                Log.e("screen", "touched");
                Intent cmd = new Intent("MainActivity");
                cmd.putExtra("data", "start");
                lbm.sendBroadcast(cmd);
                if (mVelocityTracker == null) {
                    mVelocityTracker = VelocityTracker.obtain();
                } else {
                    mVelocityTracker.clear();
                }
                gatherSamples(event);
            }
            if (action == ACTION_MOVE) {
                Log.e("screen", "gathering");
                gatherSamples(event);
            }
            if (action == ACTION_UP) {
                Log.e("screen", "sending");
                gatherSamples(event);
                if (sendSamples()) {
                    Intent cmd = new Intent("MainActivity");
                    cmd.putExtra("data", "stop");
                    lbm.sendBroadcast(cmd);
                }
            }
            if (action == ACTION_CANCEL) {
                if (sendSamples()) {
                    Intent cmd = new Intent("MainActivity");
                    cmd.putExtra("data", "stop");
                    lbm.sendBroadcast(cmd);
                }
                mVelocityTracker.recycle();
            }
            return true;
        }
        return true;
    }
    List<Long> tmp = new ArrayList<>();
    public void gatherSamples(MotionEvent ev) {
        long diff = System.currentTimeMillis() - SystemClock.uptimeMillis()+offSet;
        final int historySize = ev.getHistorySize();
        final int pointerCount = ev.getPointerCount();
        this.mVelocityTracker.addMovement(ev);
        this.mVelocityTracker.computeCurrentVelocity(1000);
        for (int h = 0; h < historySize; h++) {
            for (int p = 0; p < pointerCount; p++) {
                Float x = ev.getHistoricalX(p,h);
                long xTrace = x.longValue();
                if(tmp.size()>2){
                    long last0 = tmp.get(tmp.size()-1);long last1 = tmp.get(tmp.size()-2);
                    if((xTrace- last0)*(last0-last1)<=0){
                        String[] aGes = new String[9];
                        String ss = String.valueOf(ev.getHistoricalEventTime(h)+diff);
                        aGes[0] = ss.substring(ss.length()-6);
                        aGes[1] = String.valueOf(ev.getPointerId(p))+String.valueOf(9999);
                        aGes[2] = String.valueOf(ev.getHistoricalX(p,h));
                        aGes[3] = String.valueOf(ev.getHistoricalY(p,h));
                        aGes[4] = String.valueOf(ev.getHistoricalPressure(p,h));
                        aGes[5] = String.valueOf(ev.getHistoricalSize(p,h));
                        aGes[6] = "0.0";
                        aGes[7] = "0.0";
                        aGes[8] = String.valueOf(ev.getHistoricalOrientation(p,h));
                        this.gesData.add(aGes);
                    }
                }
                tmp.add(xTrace);
            }
        }
        for (int p = 0; p < pointerCount; p++) {
            boolean flag = false;
            Float x = ev.getX(p);
            long xTrace = x.longValue();
            if(tmp.size()>2){
                long last0 = tmp.get(tmp.size()-1);long last1 = tmp.get(tmp.size()-2);
                if((xTrace- last0)*(last0-last1)<=0){
                    flag = true;
                }
            }
            tmp.add(xTrace);
            String[] aGes = new String[9];
            String ss = String.valueOf(ev.getEventTime()+diff);
            aGes[0] = ss.substring(ss.length()-6);
            if(flag) {
                aGes[1] = String.valueOf(ev.getPointerId(p))+String.valueOf(99999);
            }else{
                aGes[1] = String.valueOf(ev.getPointerId(p));
            }
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
        Intent data=new Intent("logo");
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
