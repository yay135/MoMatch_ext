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
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class SensorService extends Service implements SensorEventListener {
    public static SensorManager mSensorManager = null;
    netService mService;
    ArrayList<String[]> mSensorData = new ArrayList<>();
    private long laccLastTimestamp = 0;
    private long gyroLastTimestamp = 0;
    private WifiManager fm;
    private boolean flag = false;
    private boolean one = false;
    private boolean scanned = false;
    private boolean rssied = false;
    private ArrayList<String[]> rssi = new ArrayList<>();

    BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            List<ScanResult> allWifis = new ArrayList<>();
            Log.e("wifiReceiver","received result");
            allWifis = fm.getScanResults();
            String[] scan = new String[2*allWifis.size() + 2];
            String ss = String.valueOf(System.currentTimeMillis());
            scan[0] = String.valueOf(ss.substring(ss.length() - 6));
            scan[1] = "7";
            Log.d("wifi", String.valueOf(allWifis.size()));
            boolean success = intent.getBooleanExtra(
                    WifiManager.EXTRA_RESULTS_UPDATED, false);
            Log.d("wifi scan success?",String.valueOf(success));
            for (int i = 0,j=2; i < allWifis.size()&& j<allWifis.size()*2; i++,j+=2) {
                if(!(allWifis.get(i).SSID.equals("uscfacstaff")||allWifis.get(i).SSID.equals("EntertaiNET")||allWifis.get(i).SSID.equals("uscguest")||allWifis.get(i).SSID.equals("uscstudent")||allWifis.get(i).SSID.equals("eduroam")||allWifis.get(i).SSID.equals("null")))
                {
                    Log.d("SSID",allWifis.get(i).SSID);
                    Log.d("rssi", String.valueOf(allWifis.get(i).level));
                    scan[j] = String.valueOf(allWifis.get(i).SSID);
                    scan[j+1] = String.valueOf(allWifis.get(i).level);
                }
                else
                {
                    j-=2;
                }
            }
            Log.d("scan",Arrays.toString(scan));
            rssi.add(scan);
            rssied = true;
        };
    };
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
            if(message.equals("stop")){
                Log.e("broadcastreceiver","received stop");
                flag = false;
                try {
                    Thread.sleep(50);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }

        }
    };
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        this.fm = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    private ExecutorService aThread = Executors.newSingleThreadExecutor();
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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
        //Sensor mMagnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        //Sensor mLight = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        //sensorManager.registerListener(listener, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(listener, mGyroSensor, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(listener, mLAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        //sensorManager.registerListener(listener, mMagnetic, SensorManager.SENSOR_DELAY_FASTEST);
        //sensorManager.registerListener(listener, mLight, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(this);
        Intent intent1 = new Intent(this, netService.class);
        unbindService(mConnection);
        //unregister listener;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mListener);
    }

    private class SensorEventLoggerTask extends AsyncTask<SensorEvent, Void, String[]> {
        @Override
        protected String[] doInBackground(SensorEvent... events) {
            if(flag) {
//                if(!scanned) {
//                    scanned = true;
//                    fm.startScan();
//                }
                one = true;
                String[] data = new String[5];
                String[] dataAux = new String[5];
                long currentTime = System.currentTimeMillis();
                SensorEvent event = events[0];
                Sensor sensor = event.sensor;

                //package a packet with a size of 5
                long currentTimestamp = event.timestamp;
                long sampleInterval = 2000000;  //denotes 2ms
                if (mSensorData.size() < 10){
                    switch (sensor.getType()) {
                        case Sensor.TYPE_LINEAR_ACCELERATION:
                            if ((currentTimestamp - laccLastTimestamp) <= 2.2 * sampleInterval) {
                                break;
                            } else {
                                /*scan wifi ever specified interval
                                WifiInfo wifiInfo = fm.getConnectionInfo();*/
                                laccLastTimestamp = currentTimestamp;
                                data[1] = "1";  //1 denotes sensor type "LINEAR_ACCELERATION"
                                //TODO: get values
                                data[2] = String.format("%.3f", event.values[0]); //Float.toString(event.values[0]);
                                data[3] = String.format("%.3f", event.values[1]); //Float.toString(event.values[1]);
                                data[4] = String.format("%.3f", event.values[2]); //Float.toString(event.values[2]);
                                data[0] = (String.valueOf(currentTime));
                                data[0] = data[0].substring(data[0].length() - 6);
                                mSensorData.add(data);
                                /* auxiliary rssi append behind sensor data
                                dataAux[1] = "3"; // denote type wifi rssi
                                dataAux[2] = String.valueOf(wifiInfo.getRssi());
                                dataAux[0] = data[0]; // use the same time stamp of accelerometer data.
                                //mSensorData.add(data);
                                mSensorData.add(dataAux);//add rssi to send buffer
                                */
                            }
                            break;
                        case Sensor.TYPE_GYROSCOPE:
                            if ((currentTimestamp - gyroLastTimestamp) <= 2.2 * sampleInterval) {
                                break;
                            } else {
                                gyroLastTimestamp = currentTimestamp;
                                data[1] = "2";  //1 denotes sensor type "GYROSCOPE"
                                //TODO: get values
                                data[2] = String.format("%.3f", event.values[0]); //Float.toString(event.values[0]);
                                data[3] = String.format("%.3f", event.values[1]); //Float.toString(event.values[1]);
                                data[4] = String.format("%.3f", event.values[2]); //Float.toString(event.values[2]);
                                data[0] = String.valueOf(currentTime);
                                data[0] = data[0].substring(data[0].length() - 6);
                                mSensorData.add(data);
                                //Log.e("sensor", Arrays.toString(data));
                            }
                            break;
                    }
                }else{
                    final ArrayList<String[]> data0 = new ArrayList<>(mSensorData);
                    mSensorData.clear();
                    Runnable Sender = new Runnable() {
                        @Override
                        public void run() {
                            mService.send(data0);
                        }
                    };
                    aThread.execute(Sender);
                }
            }else {
                if (one&&rssied) {
                    //final ArrayList<String[]> data0 = new ArrayList<>(mSensorData);
                    mSensorData.clear();
                    Runnable Sender = new Runnable() {
                        @Override
                        public void run() {
                            //mService.send(rssi);
                            try{
                                Thread.sleep(10);
                            }catch (InterruptedException e){}
                            //mService.send(data0);
                            mService.sendMSG("stop");
                            scanned = false;
                            rssied = false;
                        }
                    };
                    aThread.execute(Sender);
                    one = false;

                }
            }
            return null;
        }
    }
}
