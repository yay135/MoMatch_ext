import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.knowm.xchart.QuickChart;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYSeries;

import javax.swing.*;
import java.io.*;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.Timer;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Monitor {
    private static TCPc mTCP;
    public static ConcurrentLinkedQueue<String> data0 = new ConcurrentLinkedQueue<>();
    public static ConcurrentLinkedQueue<String> data1 = new ConcurrentLinkedQueue<>();
    public static ConcurrentLinkedQueue<String> data2 = new ConcurrentLinkedQueue<>();
    public static ConcurrentLinkedQueue<String> data3 = new ConcurrentLinkedQueue<>();


    public static void main(String[] args){
       mTCP = new TCPc(new TCPc.OnMessageReceived() {
            @Override
            public void messageReceived(String message) {
                //System.out.println("TCP:"+message);
                if (message.equals("s")) {
                }
                if (message.equals("e")) {
                }
                if (message.equals("TYPE")) {
                    String t = "OBJ_"+"Monitor";
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
                if (message.startsWith("OBJbuffer")||message.startsWith("SWTbuffer")){
                    if(message.startsWith("OBJbuffer1")){
                        String data = message.substring(10);
                        data0.offer(data);
                    }else if(message.startsWith("SWTbuffer1")){
                        String data = message.substring(10);
                        data1.offer(data);
                    }else if(message.startsWith("SWTbuffer2")){
                        String data = message.substring(10);
                        data2.offer(data);
                    }else if(message.startsWith("SWTbuffer3")){
                        String data = message.substring(10);
                        data3.offer(data);
                    }
                }
            }
        });

       CreateNewThread(mTCP);
       SwingWorkerRealTime swrt = new SwingWorkerRealTime();
       Runnable plot = new Runnable() {
           @Override
           public void run() {
              swrt.go();
           }
       };
       CreateNewThread(plot);
    }
    public static void sendMSG(String msg) {
        if(msg.equals("stop")){
            System.out.println("bindee,"+String.valueOf(System.currentTimeMillis()));
        }
        mTCP.sendMessage(msg);
    }
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

}

/*TCPclient from the Internet https://stackoverflow.com/questions/38162775/really-simple-tcp-client*/
class TCPc implements Runnable{
    public static final String SERVER_IP = "192.168.0.126";//"10.34.25.234";//"172.20.10.2"; 192.168.43.182//server IP address//"129.252.131.137"
    public static final int SERVER_PORT = 8888;
    public static final String TAG = "TCPClient";
    // message to send to the server
    private String mServerMessage;
    // sends message received notifications
    private OnMessageReceived mMessageListener = null;
    // while this is true, the server will continue running
    private boolean mRun = false;
    // used to send messages
    private PrintWriter mBufferOut;
    // used to read messages from the server
    private BufferedReader mBufferIn;
    // store message recived.
    public Object message;
    /**
     * Constructor of the class. OnMessagedReceived listens for the messages received from server
     */
    public TCPc(OnMessageReceived listener) {
        mMessageListener = listener;
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
            System.out.println("sent:"+message);
        }
    }

    /**
     * Close the connection and release the members
     */
    public void stopClient() {

        mRun = false;

        if (mBufferOut != null) {
            mBufferOut.flush();
            mBufferOut.close();
        }

        mMessageListener = null;
        mBufferIn = null;
        mBufferOut = null;
        mServerMessage = null;
    }

    public void run() {

        mRun = true;

        try {
            //here you must put your computer's IP address.
            InetAddress serverAddr = InetAddress.getByName(SERVER_IP);

            System.out.println("TCP Client:" +"C: Connecting...");

            //create a socket to make the connection with the server
            Socket socket = new Socket(serverAddr, SERVER_PORT);

            try {

                //sends the message to the server
                mBufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                //receives the message which the server sends back
                mBufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));


                //in this while the client listens for the messages sent by the server
                while (mRun) {


                    mServerMessage = mBufferIn.readLine();

                    if (mServerMessage != null && mMessageListener != null) {
                        //call the method messageReceived from MyActivity class
                        mMessageListener.messageReceived(mServerMessage);
                    }

                }

                System.out.println("RESPONSE FROM SERVER,"+"S: Received Message: '" + mServerMessage + "'");

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("TCP,"+"S: Error"+ e);

            } finally {
                //the socket must be closed. It is not possible to reconnect to this socket
                // after it is closed, which means a new socket instance has to be created.
                socket.close();
            }

        } catch (Exception e) {

            System.out.println("TCP"+"C: Error"+e);

        }

    }

    //Declare the interface. The method messageReceived(String message) will must be implemented in the MyActivity
    //class at on asynckTask doInBackground
    public interface OnMessageReceived {
        public void messageReceived(String message);
    }

}

class SwingWorkerRealTime {

    MySwingWorker mySwingWorker;
    SwingWrapper<XYChart> sw;
    XYChart chart;

    public void go() {
        String[] names = {"randomWalk0","randomWalk1","randomWalk2","randomWalk3"};
        // Create Chart
        chart =
                QuickChart.getChart(
                        "LACC Real-time Curve",
                        "Time",
                        "LACC Value",
                        names,
                        new double[] {0},
                        new double[][] {{0},{0},{0},{0}});

        chart.getStyler().setLegendVisible(false);
        chart.getStyler().setXAxisTicksVisible(false);
        chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
        chart.getStyler().setYAxisMax(5.0);
        chart.getStyler().setYAxisMin(0.0);
        // Show it
        sw = new SwingWrapper<XYChart>(chart);
        sw.displayChart();

        mySwingWorker = new MySwingWorker();
        mySwingWorker.execute();
    }

    private class MySwingWorker extends SwingWorker<Boolean, double[]> {
        LinkedList<Double> fifo1 = new LinkedList();
        LinkedList<Double> fifo3 = new LinkedList();
        LinkedList<Double> fifo2 = new LinkedList<>();
        LinkedList<Double> fifo4 = new LinkedList<>();
        LinkedList<String[]> buffer1 = new LinkedList<>();
        LinkedList<String[]> buffer3 = new LinkedList<>();
        LinkedList<String[]> buffer2 = new LinkedList<>();
        LinkedList<String[]> buffer4 = new LinkedList<>();
        double[] ydata0 = new double[1000];
        double[] ydata1 = new double[1000];
        double[] ydata2 = new double[1000];
        double[] ydata3 = new double[1000];
        ArrayList<String[]> ss;
        ArrayList<String[]> st;
        ArrayList<String[]> sg;
        ArrayList<String[]> sp;
        Gson gson = new Gson();
        Type colloectionType = new TypeToken<ArrayList<String[]>>() {}.getType();
        @Override
        protected Boolean doInBackground(){
            while(!isCancelled()) {
                String obj = null;
                String swt = null;
                String ngh = null;
                String tdp = null;
                if (Monitor.data0 != null&&Monitor.data0.peek()!=null) {
                    obj = Monitor.data0.poll();
                    //System.out.println(obj);
                }
                if (Monitor.data1 != null&&Monitor.data1.peek()!=null) {
                    swt = Monitor.data1.poll();
                }
                if (Monitor.data2 != null&&Monitor.data2.peek()!=null) {
                    ngh = Monitor.data2.poll();
                    //System.out.println(ngh);
                }
                if (Monitor.data3 !=null&&Monitor.data3.peek()!=null) {
                    tdp = Monitor.data3.poll();
                }
                if (obj != null) {
                    try {
                        ss = gson.fromJson(obj, colloectionType);
                        buffer1.addAll(ss);
                    } catch (JsonSyntaxException e) {
                        e.printStackTrace();
                        System.out.println("json err -> " + obj);
                    }
                }
                if (swt != null) {
                    try {
                        System.out.println(swt);
                        st = gson.fromJson(swt, colloectionType);
                        buffer3.addAll(st);
                    } catch (JsonSyntaxException e) {
                        e.printStackTrace();
                        System.out.println("json err -> " + swt);
                    }
                }
                if (ngh != null) {
                    try {
                        sg = gson.fromJson(ngh, colloectionType);
                        buffer2.addAll(sg);
                    } catch (JsonSyntaxException e) {
                        e.printStackTrace();
                        System.out.println("json err -> " + obj);
                    }
                }
                if (tdp != null) {
                    try {
                        sp = gson.fromJson(tdp, colloectionType);
                        buffer4.addAll(sp);
                    } catch (JsonSyntaxException e) {
                        e.printStackTrace();
                        System.out.println("json err -> " + obj);
                    }
                }
                String[] t = null;
                if(!buffer1.isEmpty()){
                    t = buffer1.removeFirst();
                }
                if(t!=null&&t[1].equals("1")) {
                    double x = Double.valueOf(t[2]);
                    double y = Double.valueOf(t[3]);
                    double z = Double.valueOf(t[4]);
                    double value = Math.pow(x * x + y * y + z * z, 0.5);
                    fifo1.add(value);
                    if (fifo1.size() > 1000) {
                        fifo1.removeFirst();
                    }
                }
                String[] s = null;
                if(!buffer3.isEmpty()){
                    s = buffer3.removeFirst();
                }
                if(s!=null&&s[1].equals("1")) {
                    double x = Double.valueOf(s[2]);
                    double y = Double.valueOf(s[3]);
                    double z = Double.valueOf(s[4]);
                    double value = Math.pow(x * x + y * y + z * z, 0.5);
                    fifo3.add(value);
                    if (fifo3.size() > 1000) {
                        fifo3.removeFirst();
                    }
                }
                String[] r = null;
                if(!buffer2.isEmpty()){
                    r = buffer2.removeFirst();
                }
                if(r!=null&&r[1].equals("1")) {
                    double x = Double.valueOf(r[2]);
                    double y = Double.valueOf(r[3]);
                    double z = Double.valueOf(r[4]);
                    double value = Math.pow(x * x + y * y + z * z, 0.5);
                    fifo2.add(value);
                    if (fifo2.size() > 1000) {
                        fifo2.removeFirst();
                    }
                }
                String[] g = null;
                if(!buffer4.isEmpty()){
                    g = buffer4.removeFirst();
                }
                if(g!=null&&g[1].equals("1")) {
                    double x = Double.valueOf(g[2]);
                    double y = Double.valueOf(g[3]);
                    double z = Double.valueOf(g[4]);
                    double value = Math.pow(x * x + y * y + z * z, 0.5);
                    fifo4.add(value);
                    if (fifo4.size() > 1000) {
                        fifo4.removeFirst();
                    }
                }
                for (int i = 0; i < fifo1.size(); i++) {
                    ydata0[i]=fifo1.get(i);
                }
                for (int i = 0; i < fifo3.size(); i++) {
                    ydata1[i]=fifo3.get(i);
                }
                for(int i = 0;i<fifo2.size();i++){
                    ydata2[i]=fifo2.get(i);
                }
                for(int i = 0;i<fifo4.size();i++){
                    ydata3[i]=fifo4.get(i);
                }
                publish(ydata0);
                publish(ydata1);
                publish(ydata2);
                publish(ydata3);
            }
            return true;
        }

        @Override
        protected void process(List<double[]> chunks) {
            double[] ydata0 = chunks.get(chunks.size() - 1);
            double[] ydata1 = chunks.get(chunks.size() - 2);

            double[] ydata2 = chunks.get(chunks.size() - 3);
            double[] ydata3 = chunks.get(chunks.size() - 4);
            chart.updateXYSeries("randomWalk0",null,ydata0, null);
            chart.updateXYSeries("randomWalk1",null,ydata1,null);
            chart.updateXYSeries("randomWalk2",null,ydata2,null);
            chart.updateXYSeries("randomWalk3",null,ydata3,null);

            sw.repaintChart();

            long start = System.currentTimeMillis();
            long duration = System.currentTimeMillis() - start;
            try {
                Thread.sleep(10 - duration); // 40 ms ==> 25fps
            } catch (InterruptedException e) {
                System.out.println("InterruptedException occurred.");
            }
        }
    }
}