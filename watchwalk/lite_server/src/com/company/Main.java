package com.company;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.opencsv.CSVWriter;

import java.io.*;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Scanner;


public class Main {
    private static int count = 0;
    public static void main(String[] args) {
        try {
            SensorData spData = new SensorData();
            SensorData swData = new SensorData();
            ConcurrentLinkedQueue<wrapper> que = new ConcurrentLinkedQueue<>();
            InetAddress myNetAddress = null;
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    System.in));
            //System.out.println("net address not found, specify ipAddress manually:");

//            String address = "192.168.0.126";
            String address = "192.168.0.171";

            myNetAddress = InetAddress.getByName(address);
            ServerSocket server = new ServerSocket(8888, 10, myNetAddress);
            List<Socket> sockets = new ArrayList<>();
            Map<Socket, Map<String,Object>> communicators = new HashMap<>();
            Map<Socket,Long> timeDiff = new HashMap();
            ExecutorService aThread = Executors.newSingleThreadExecutor();
            while (sockets.size() < 1) {
                System.out.println("Listening for "+sockets.size()+1+"st device");
                Socket client = server.accept();
                String clientAddress = client.getInetAddress().getHostAddress();
                System.out.println("connected with" + clientAddress);
                BufferedReader Reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                PrintWriter Writer = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));
                sockets.add(client);
                Map<String,Object> communicator = new HashMap<>();
                communicator.put("reader",Reader);communicator.put("writer",Writer);
                communicators.put(client,communicator);
            }
            System.out.println("started calibration");
            for(Socket client: sockets) {
                Thread.sleep(10);
                PrintWriter Writer = (PrintWriter) communicators.get(client).get("writer");
                BufferedReader Reader = (BufferedReader) communicators.get(client).get("reader");
                Writer.println("TYPE");
                Writer.flush();
                System.out.println("sent type to "+client.getInetAddress());
                Thread.sleep(10);
                Reader.readLine();
                System.out.println("sent time");
                Writer.println("time");
                Writer.flush();
                ArrayList<Long> timediffs = new ArrayList<>();
                while (true) {
                    String timeStr = Reader.readLine();
                    if (timeStr.equals("q")) {
                        break;
                    }
                    String time = timeStr.substring(0, timeStr.length() - 1);
                    Long aDiff = System.currentTimeMillis() - Long.parseLong(time);
                    timediffs.add(aDiff);
                }
                Long max = Long.MIN_VALUE; Long min = Long.MAX_VALUE; int h=-1; int l=-1;
                for(int i=0;i<timediffs.size();i++){
                    if(timediffs.get(i)>max){
                        max = timediffs.get(i);h=i;
                    }
                }
                timediffs.remove(h);
                for(int i=0;i<timediffs.size();i++){
                    if(timediffs.get(i)<min){
                        min = timediffs.get(i);l=i;
                    }
                }
                timediffs.remove(l);
                long sum = 0;
                for(Long num:timediffs){
                    sum += num;
                }
                timeDiff.put(client,sum/8);
            }
            System.out.println("Successfully connected with 2 devices");
            BufferedReader reader0 = (BufferedReader) communicators.get(sockets.get(0)).get("reader");
            PrintWriter writer0 = (PrintWriter) communicators.get(sockets.get(0)).get("writer");
//            BufferedReader r = new BufferedReader(new InputStreamReader(
//                    System.in));
            //System.out.println("input directory:");

//            String s = "c:\\Users\\yay13\\Sensordata";
            String s = "/home/xiaopeng/sensor_data";
            
            final String pa = s;
            Runnable writer = new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(100);
                            if (que.peek() != null) {
                                wrapper wrap = que.poll();
                                String path;
                                if (wrap != null) {
                                    if (wrap.label != null) {
                                        path = pa + "/exp_" + wrap.label.substring(wrap.label.length() - 2) + "/";
                                    } else {
                                        path = pa + "/exp_" + wrap.ip.substring(wrap.ip.length() - 2) + "/";
                                    }
                                    String DirectoryName = path.concat(String.valueOf(wrap.cc));
                                    String fileName = wrap.type +"_"+wrap.timeDiff+".csv";
                                    File directory = new File(DirectoryName);
                                    if (!directory.exists()) {
                                        boolean success = directory.mkdirs();
                                        while (!success) {
                                            success = directory.mkdirs();
                                        }
                                    }
                                    String Path = DirectoryName + "/" + fileName;
                                    System.out.println(wrap.type + wrap.idx + ":" + wrap.da.data.size());
                                    try {
                                        synchronized (wrapper.class) {
                                            wrap.da.writeToCSV(Path);
                                        }
                                        // System.out.println("Thread_" + wrap.type + "_" + wrap.ip + "_" + "finished writing");
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            ExecutorService anThread = Executors.newSingleThreadExecutor();
            anThread.execute(writer);
            boolean nextCmd = false;
            while (true) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                }
                String cmd = reader0.readLine();
                if(cmd.equals("s")){
                    count++;
                    SensorData sd = new SensorData();
                    writer0.println("s");
                    writer0.flush();
                    boolean finish = false;
                    while(!finish){
                        String data = reader0.readLine();
                        if(data.equals("stop")){
                            finish = true;
                            writerToDisk(count, sd, 1, sockets.get(0).getInetAddress().toString(), timeDiff.get(sockets.get(0)), "sp", "owijeof45", que);
                        }else if(data.equals("e")){
                            writer0.println("e");
                            writer0.flush();
                        }
                        else{
                            sd.write(data);
                        }
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
    private static void writerToDisk(int c,SensorData sen,int idx,String ip, Long timediff, String type,
                              String lable, ConcurrentLinkedQueue<wrapper> que){
        wrapper toPut = new wrapper();
        toPut.da = sen;toPut.cc = c;toPut.idx = 1;toPut.ip = ip;toPut.timeDiff = timediff;toPut.type = type;toPut.label = lable;
        while (true) {
            if (que.offer(toPut)) {
                break;
            }
        }
    }
}
class SensorData {
    ArrayList<String[]> data = new ArrayList<>();
    private CSVWriter writer = null;

    void write(String s) {
        synchronized (this) {
            Gson gson = new Gson();
            Type collectionType = new TypeToken<ArrayList<String[]>>() {
            }.getType();
            try {
                ArrayList<String[]> ss = gson.fromJson(s, collectionType);
                this.data.addAll(ss);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
                System.out.println("json err -> " + s);
            }
        }
    }

    void writeToCSV(String path) {
        synchronized (this) {
            try {
                this.writer = new CSVWriter(new FileWriter(path, true));
            } catch (Exception e) {
                e.printStackTrace();
            }
            this.writer.writeAll(this.data);
            this.data.clear();
            try {
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
class wrapper {
    SensorData da;String type;int idx;long timeDiff;String ip;int cc;String label;
}