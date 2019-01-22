package com.company;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.opencsv.CSVWriter;
import org.apache.commons.net.ntp.TimeInfo;
import sun.rmi.runtime.Log;

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
    private static long OFFSET = 0;
    private static boolean exit =false;
    public static void main(String[] args) {
        try {
            SensorData spData = new SensorData();
            SensorData swData = new SensorData();
            ConcurrentLinkedQueue<wrapper> que = new ConcurrentLinkedQueue<>();
            InetAddress myNetAddress = null;
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    System.in));
            System.out.println("Specify ipAddress:");
            String address = reader.readLine();
            myNetAddress = InetAddress.getByName(address);
            ServerSocket server = new ServerSocket(8888, 10, myNetAddress);
            List<Socket> sockets = new ArrayList<>();
            Map<Socket, Map<String,Object>> communicators = new HashMap<>();
            Map<Socket,Long> timeDiff = new HashMap();
            ExecutorService aThread = Executors.newSingleThreadExecutor();
            while (sockets.size() < 1) {
                System.out.println("Listening for "+sockets.size()+1+"st device...");
                Socket client = server.accept();
                String clientAddress = client.getInetAddress().getHostAddress();
                System.out.println("Connected with" + clientAddress);
                BufferedReader Reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                PrintWriter Writer = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));
                sockets.add(client);
                Map<String,Object> communicator = new HashMap<>();
                communicator.put("reader",Reader);communicator.put("writer",Writer);
                communicators.put(client,communicator);
            }
            //System.out.println("Started calibration");
            for(Socket client: sockets) {
                Thread.sleep(10);
                PrintWriter Writer = (PrintWriter) communicators.get(client).get("writer");
                BufferedReader Reader = (BufferedReader) communicators.get(client).get("reader");
                Writer.println("TYPE");
                Writer.flush();
                //System.out.println("sent type to "+client.getInetAddress());
                Thread.sleep(10);
                Reader.readLine();
                //System.out.println("sent time");
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
            System.out.println("Successfully connected with watch.");
            BufferedReader reader0 = (BufferedReader) communicators.get(sockets.get(0)).get("reader");
           // BufferedReader reader1 = (BufferedReader) communicators.get(sockets.get(1)).get("reader");
           // PrintWriter writer1 = (PrintWriter) communicators.get(sockets.get(1)).get("writer");
            PrintWriter writer0 = (PrintWriter) communicators.get(sockets.get(0)).get("writer");
            BufferedReader r = new BufferedReader(new InputStreamReader(
                    System.in));
            System.out.println("Specify data output directory:");
            String s = r.readLine();
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
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    if(wrap.type.startsWith("sw")){
                                        writer0.println("canStart");
                                        writer0.flush();
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
            short vendorId = 9025;
            short productId = 18509;
            RotationDriver driver = new RotationDriver(vendorId,productId);
            Runnable keyBoardCmd = new Runnable() {
                @Override
                public void run() {
                    while (!exit) {
                        try {Thread.sleep(1);} catch (InterruptedException e) { }
                        Scanner scanner = new Scanner(System.in);
                        String input = scanner.nextLine();
                        if (input.equals("c")) {
                            OFFSET = NTPcal();
                            System.out.println("INTDF:"+OFFSET+"ms");
                            driver.updateOffSet(OFFSET);
                            writer0.println("cali");
                            writer0.flush();
                        }else if(input.equals("q")){
                            exit = true;
                        }
                    }
                    driver.stopRotation();
                    driver.releaseAndClosePipe();
                }
            };
            ExecutorService keyBoardThread = Executors.newSingleThreadExecutor();
            keyBoardThread.execute(keyBoardCmd);
            while(!exit){
                try {Thread.sleep(1);} catch (InterruptedException e) {}
                String data = reader0.readLine();
                if(data.equals("start")){
                    count += 1;
                    System.out.println("start rotation log...");
                    Runnable runnable = new Runnable(){
                        @Override
                        public void run() {
                            driver.startRotation();
                        }
                    };
                    ExecutorService rt = Executors.newSingleThreadExecutor();
                    rt.execute(runnable);
                    while(true) {
                        try {Thread.sleep(1);} catch (InterruptedException e) {}
                        String data0 = reader0.readLine();
                        if(data0.equals("stop")){
                            rt.shutdownNow();
                            writer0.println("e");
                            writer0.flush();
                            driver.stopRotation();
                            System.out.println("stop rotation log.");
                            SensorData sensordata = new SensorData();
                            sensordata.data = driver.getRotationData();
                            writerToDisk(count,sensordata,1,"0.0.0.0",0L,"sp","rotationro",que);
                        }
                        else if(data0.equals("end")){
                            SensorData tmp = new SensorData();
                            tmp.data = new ArrayList<>(swData.data);
                            swData.data.clear();
                            writerToDisk(count, tmp, 0, sockets.get(0).getInetAddress().toString(), timeDiff.get(sockets.get(0)), "sw", "watchro", que);
                            swData.data.clear();
                            break;
                        }else{
                            swData.write(data0);
                        }
                    }

                }
            }
            keyBoardThread.shutdownNow();
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
    private static long NTPcal(){
        Long offSet = 0L;
        try {
            NTPUDPClient client = new NTPUDPClient();
            client.open();
            InetAddress hostAddr = InetAddress.getByName("time.google.com");
            TimeInfo info = client.getTime(hostAddr);
            info.computeDetails(); // compute offset/delay if not already done
            Long offsetValue= info.getOffset();
            offSet = offsetValue;
            Long delayValue = info.getDelay();
            String delay = (delayValue == null) ? "N/A" : delayValue.toString();
            String offset = (offsetValue == null) ? "N/A" : offsetValue.toString();
            client.close();
        }catch(IOException e){
            e.printStackTrace();
        }
        return offSet;
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