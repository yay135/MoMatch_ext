package com.example.jessi.sensors;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.apache.commons.net.ntp.TimeInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class SensorService extends Service implements SensorEventListener {
    private PowerManager.WakeLock wakeLock;
    public static SensorManager mSensorManager = null;
    netService mService;
    ArrayList<String[]> mSensorData = new ArrayList<>();
    private long laccLastTimestamp = 0;
    private long gyroLastTimestamp = 0;
    private long RVLastTimestamp = 0;
    private boolean flag = false;
    private boolean collected = false;
    private Queue<String[]> tmpData = new LinkedList<>();
    private Long offSet = 0L;
    private List<float[]> recentPositions = new ArrayList<>();
    private Long[] times = new Long[2];
    //Binder Usage client
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            netService.LocalBinder binder = (netService.LocalBinder) service;
            mService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    private BroadcastReceiver mListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("sig");
            if(message.equals("start")){
                flag = true;
                Log.e("broadcastreceiver","received start "+String.valueOf(System.currentTimeMillis()));
            }
            else if(message.equals("stop")){
                Log.e("broadcastreceiver","received stop");
                flag = false;
                try {
                    Thread.sleep(50);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }else{
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        new Thread(new Runnable() {
            public void run() {
                Long offsetValue;
                try {
                    NTPUDPClient client = new NTPUDPClient();
                    client.open();
                    InetAddress hostAddr = InetAddress.getByName("time.google.com");
                    TimeInfo info = client.getTime(hostAddr);
                    info.computeDetails(); // compute offset/delay if not already done
                    offsetValue = info.getOffset();
                    Long delayValue = info.getDelay();
                    String delay = (delayValue == null) ? "N/A" : delayValue.toString();
                    String offset = (offsetValue == null) ? "N/A" : offsetValue.toString();

                    Log.e("TNPUDP"," Roundtrip delay(ms)=" + delay
                            + ", clock offset(ms)=" + offset); // offset in ms
                    client.close();
                    offSet = offsetValue;
                    Log.d("offSet","set to"+String.valueOf(offSet));
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private ExecutorService aThread = Executors.newSingleThreadExecutor();
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        PowerManager mgr = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
        wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
        wakeLock.acquire();
        Log.d("triggerservicetime0", Long.toString(System.currentTimeMillis()));
        Log.d("sensorService", "service Start");
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        registorChosenSensors(mSensorManager, this);
        // Binder Usage client
        Intent intent1 = new Intent(this, netService.class);
        // Binder Usage client
        bindService(intent1, mConnection, Context.BIND_AUTO_CREATE);
        //CreateNewThread(new sendProcess());
        //sendMessage to main activity
        Intent notice = new Intent("NoticeMainActivity");
        notice.putExtra("message","start0");
        LocalBroadcastManager.getInstance(this).sendBroadcast(notice);
        // recieved start or stop signal from the tcp
        LocalBroadcastManager.getInstance(this).registerReceiver(mListener,new IntentFilter("tcpc"));
        return START_STICKY;
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // do nothing
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        new SensorEventLoggerTask().execute(event);
    }

    public void registorChosenSensors(SensorManager sensorManager, SensorEventListener listener) {
        //Sensor mAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor mLAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        Sensor mGyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        Sensor mRotationV = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        //Sensor mMagnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        //Sensor mLight = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        //sensorManager.registerListener(listener, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(listener, mGyroSensor, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(listener, mLAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(listener, mRotationV, SensorManager.SENSOR_DELAY_FASTEST);
       // sensorManager.registerListener(listener, mMagnetic, SensorManager.SENSOR_DELAY_FASTEST);
        //sensorManager.registerListener(listener, mLight, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onDestroy() {
        wakeLock.release();
        mSensorManager.unregisterListener(this);
        unbindService(mConnection);
        //unregister listener;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mListener);
    }

    private class SensorEventLoggerTask extends AsyncTask<SensorEvent, Void, String[]> {
        @Override
        protected String[] doInBackground(SensorEvent... events) {
            String[] data = new String[6];
            final long currentTime = System.currentTimeMillis();
            SensorEvent event = events[0];
            Sensor sensor = event.sensor;

            //package a packet with a size of 5
            final long currentTimestamp = event.timestamp;
            long sampleInterval = 2000000;  //denotes 2ms
            switch (sensor.getType()) {
                case Sensor.TYPE_ROTATION_VECTOR:
                    if ((currentTimestamp - RVLastTimestamp) <= 2.2 * sampleInterval) {
                        break;
                    } else {
                        //if(recentPositions.size()==0&&flag) {recentPositions.add(event.values); times[0] = currentTime;}
                        RVLastTimestamp = currentTimestamp;
                        float[] tmp = new float[4];
                        SensorManager.getQuaternionFromVector(tmp,event.values);
                        data[1] = "7";  //1 denotes sensor type "LINEAR_ACCELERATION"
                        //TODO: get values
                        data[2] = String.format("%.3f", tmp[0]); //Float.toString(event.values[0]);
                        data[3] = String.format("%.3f", tmp[1]); //Float.toString(event.values[1]);
                        data[4] = String.format("%.3f", tmp[2]); //Float.toString(event.values[2]);
                        data[5] = String.format("%.3f", tmp[3]);
                        data[0] = (String.valueOf(currentTime+offSet));
                        data[0] = data[0].substring(data[0].length() - 6);
                        if (flag) {
                            collected = true;
                            mSensorData.add(data);
                        } else {
//                                tmpData.add(data);
//                                if (tmpData.size() > 20) tmpData.remove();
                            // Log.d("buffer size",String.valueOf(tmpData.size()));
                        }
                    }
                    break;
                case Sensor.TYPE_LINEAR_ACCELERATION:
                    if ((currentTimestamp - laccLastTimestamp) <= 2.2 * sampleInterval) {
                        break;
                    } else {
                        //if(recentPositions.size()==0&&flag) {recentPositions.add(event.values); times[0] = currentTime;}
                        laccLastTimestamp = currentTimestamp;
                        data[1] = "1";  //1 denotes sensor type "LINEAR_ACCELERATION"
                        //TODO: get values
                        data[2] = String.format("%.3f", event.values[0]); //Float.toString(event.values[0]);
                        data[3] = String.format("%.3f", event.values[1]); //Float.toString(event.values[1]);
                        data[4] = String.format("%.3f", event.values[2]); //Float.toString(event.values[2]);
                        data[0] = (String.valueOf(currentTime+offSet));
                        data[0] = data[0].substring(data[0].length() - 6);
                        if (flag) {
                            collected = true;
                            mSensorData.add(data);
                        } else {
//                                tmpData.add(data);
//                                if (tmpData.size() > 20) tmpData.remove();
                            // Log.d("buffer size",String.valueOf(tmpData.size()));
                        }
                    }
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    if ((currentTimestamp - gyroLastTimestamp) <= 2.2 * sampleInterval) {
                        break;
                    } else {
//                        if(recentPositions.size()==1&&flag) {
//                            times[1] = currentTime;
//                            recentPositions.add(event.values);
//                            final List<float[]> tmp = new ArrayList<>(recentPositions);
//                            recentPositions.clear();
//                            new Thread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    final float[] rotationMatrix = new float[9];
//                                    SensorManager.getRotationMatrix(rotationMatrix, null,
//                                            tmp.get(0), tmp.get(1));
//                                    final float[] orientationAngles = new float[3];
//                                    SensorManager.getOrientation(rotationMatrix, orientationAngles);
//                                    String time = String.valueOf((times[0]+times[1])/2 + offSet);
//                                    String[] data = new String[5];
//                                    data[0] = time.substring(time.length() - 6);
//                                    data[1] = "3";
//                                    for (int i = 0; i < orientationAngles.length; i++) {
//                                        data[i + 2] = String.valueOf(orientationAngles[i]);
//                                    }
//                                    mSensorData.add(data);
//                                }
//                            }).start();
//                        }
                        gyroLastTimestamp = currentTimestamp;
                        data[1] = "2";  //1 denotes sensor type "GYROSCOPE"
                        //TODO: get values
                        data[2] = String.format("%.3f", event.values[0]); //Float.toString(event.values[0]);
                        data[3] = String.format("%.3f", event.values[1]); //Float.toString(event.values[1]);
                        data[4] = String.format("%.3f", event.values[2]); //Float.toString(event.values[2]);
                        data[0] = String.valueOf(currentTime+offSet);
                        data[0] = data[0].substring(data[0].length() - 6);
                        if (flag) {
                            collected = true;
                            mSensorData.add(data);
                        } else {
//                                tmpData.add(data);
//                                if (tmpData.size() > 20) tmpData.remove();
                        }
                    }
            }
            if(!flag&&collected){
                try{
                    Thread.sleep(100);// wait for orientation calc thread to finish;
                }catch (InterruptedException e ){
                    e.printStackTrace();
                }
                for(int i=0;i<mSensorData.size();i+=100){
                    int j = i + 100;
                    mService.send(mSensorData.subList(i,Math.min(j,mSensorData.size())));
                    try{
                        Thread.sleep(10);
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
                try{
                    Thread.sleep(100);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
                collected = false;
                mSensorData.clear();
                mService.sendMSG("stop");
            }
//            if(flag&&!tmpData.isEmpty()){
//                final ArrayList<String[]> data1 = new ArrayList<>(tmpData);
//                tmpData.clear();
//                Runnable Sender = new Runnable() {
//                    @Override
//                    public void run() {
//                        mService.send(data1);
//                        Log.d("Buffer","data sent");
//                    }
//                };
//                aThread.execute(Sender);
//            }
            return null;
        }
    }
}