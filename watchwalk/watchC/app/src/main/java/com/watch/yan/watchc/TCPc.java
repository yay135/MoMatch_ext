package com.watch.yan.watchc;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.provider.Settings.Secure;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/*TCPclient from the Internet https://stackoverflow.com/questions/38162775/really-simple-tcp-client*/
public class TCPc implements Runnable{
    public static final String SERVER_IP = "192.168.0.171";
    public static final int SERVER_PORT = 8888;
    public static final String TAG = "TCPClient";
    // message to send to the server
    private String mServerMessage;
    // sends message received notifications
    private OnMessageReceived mMessageListener = null;
    // while this is true, the server will continue running
    private boolean mRun = false;
    private boolean listen = false;
    // used to send messages
    private PrintWriter mBufferOut;
    // used to read messages from the server
    private BufferedReader mBufferIn;
    //
    private ConnectivityManager cm;
    //
    private WifiManager fm;
    //
    private LocalBroadcastManager lbm;
   //
    public static int timeReceived;
    /**
     * Constructor of the class. OnMessagedReceived listens for the messages received from server
     */
    public TCPc(LocalBroadcastManager lbm, WifiManager fm, ConnectivityManager cm, OnMessageReceived listener) {
        mMessageListener = listener;
        this.cm = cm;
        this.fm = fm;
        this.lbm = lbm;
    }

    /**
     * Sends the message entered by client to the server
     *
     * @param message text entered by client
     */
    public void sendMessage(final String message) {
        if (mBufferOut != null && !mBufferOut.checkError()) {
            mBufferOut.println(message);
            mBufferOut.flush();
        }
    }

    /**
     * Close the connection and release the members
     */
    public void stopClient() {

        mRun = false;
        listen = false;

        if (mBufferOut != null) {
            mBufferOut.flush();
            mBufferOut.close();
        }

        mMessageListener = null;
        mBufferIn = null;
        mBufferOut = null;
        mServerMessage = null;
    }

    public void disconnect(){
        listen = false;

        if (mBufferOut != null) {
            mBufferOut.flush();
            mBufferOut.close();
        }
        mBufferIn = null;
        mBufferOut = null;
    }

    public void run() {
        Timer timer = new Timer();
        mRun = true;

        try {
            //here you must put your computer's IP address.
            InetAddress serverAddr = InetAddress.getByName(SERVER_IP);

            Log.e("TCP Client", "C: Connecting...");

            //create a socket to make the connection with the server
            Socket socket = null;
            InetSocketAddress endpoint = new InetSocketAddress(serverAddr, SERVER_PORT);
            final Intent connection =  new Intent("tcpc");
            connection.putExtra("sig","connected");
            final Intent disconnection = new Intent("tcpc");
            disconnection.putExtra("sig","disconnected");
            while (mRun) {
                Thread.sleep(200);
                try {
                    NetworkInfo activeNetwork = this.cm.getActiveNetworkInfo();
                    boolean isWiFi = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
                    if (isWiFi) {
                        WifiInfo wifi = this.fm.getConnectionInfo();
                        String ssid = wifi.getSSID();
                        int rssi = wifi.getRssi();
                        int level = fm.calculateSignalLevel(rssi,5);
                        Log.e("rssi","wifi signal level"+level);
                        Log.e("ssid:",ssid);
                        if (ssid.equals("\"TP-Link_1F0D\"")&&level>=1) {
                            if (socket == null || socket.isClosed()) {
                                Log.d("position", "connecting...");
                                socket = new Socket(serverAddr, SERVER_PORT);
                                listen = true;
                                //sends the message to the server
                                mBufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                                //receives the message which the server sends back
                                mBufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));


                                //in this while the client listens for the messages sent by the server
                                Log.e("listen",String.valueOf(listen));
                                while (listen) {
                                    Thread.sleep(1);
                                    mServerMessage = mBufferIn.readLine();
//                                    int rssi0 = wifi.getRssi();
//                                    int level0 = fm.calculateSignalLevel(rssi0,5);
//                                    if(level0<1){
//                                        Log.e("Socket","low rssi,disconnecting");
//                                        lbm.sendBroadcast(disconnection);
//                                        disconnect();
//                                    }

                                    if (mServerMessage != null && mMessageListener != null) {
//                                        if (mServerMessage.equals("time")) {
//                                            timeReceived++;
//                                            Log.e("Broadcast","sent connected");
//                                            lbm.sendBroadcast(connection);
//                                            timer.schedule(new TimerTask() {
//                                                private int timeCount = timeReceived;
//                                                @Override
//                                                public void run() {
//                                                   if(this.timeCount==TCPc.timeReceived){
//                                                       Log.e("Socket","timeout,disconnecting");
//                                                       lbm.sendBroadcast(disconnection);
//                                                       disconnect();
//                                                   }
//                                                }
//                                            },1210000);
//
//                                        }
                                        //call the method messageReceived from MyActivity class
                                        mMessageListener.messageReceived(mServerMessage);
                                    }
                                }
                                socket.close();
                                lbm.sendBroadcast(disconnection);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    //the socket must be closed. It is not possible to reconnect to this socket
                    // after it is closed, which means a new socket instance has to be created.
                    if(socket!=null) {
                        if(!socket.isClosed()) {
                            socket.close();
                            lbm.sendBroadcast(disconnection);
                        }
                    }
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

        }finally {
        }

    }

    //Declare the interface. The method messageReceived(String message) will must be implemented in the MyActivity
    //class at on asynckTask doInBackground
    public interface OnMessageReceived {
        public void messageReceived(String message);
    }

}
//1207 10.42.0.10 dell
//1207 129.252.131.137 destop xiaopeng

