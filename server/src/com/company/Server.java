package com.company;

import java.io.*;
import java.lang.reflect.Type;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.opencsv.CSVWriter;
import com.google.gson.Gson;


public class Server {
    private HashSet<ConcurrentLinkedQueue<String>> QueArr = new HashSet<>();
    private int SWcount = 0;
    private int OBJcount = 0;
    private static long currTimestamp;
    static Map<Socket, Long> timeDiffs = new ConcurrentHashMap<>();
    private static Map<Socket, PrintWriter> writers = new ConcurrentHashMap<>();
    private static Map<Socket, BufferedReader> readers = new ConcurrentHashMap<>();
    static Map<Socket, Boolean> status = new ConcurrentHashMap<>();
    static Map<Socket, Boolean> Accept = new ConcurrentHashMap<>();
    static Map<Socket, Boolean> isCali = new ConcurrentHashMap<>();
    private static Map<Socket, Long> lastTimeStamps = new ConcurrentHashMap<>();
    static Map<String, count> allCounts = new ConcurrentHashMap<>();
    static Map<String, Map<String, count>> allCountMaps = new ConcurrentHashMap<>();
    private static Map<String,Socket> AddressClient = new ConcurrentHashMap<>();
    private  static Map<Socket,String> TypeMap = new ConcurrentHashMap<>();
    private static ConcurrentLinkedQueue<String> gdata = new ConcurrentLinkedQueue<>();
    private ServerSocket server;

    private Server(InetAddress myNetAddress) throws Exception {
        if (myNetAddress != null)
            this.server = new ServerSocket(8888, 10, myNetAddress);
        else
            this.server = new ServerSocket(8888, 10, InetAddress.getLocalHost());
    }

    private void create(String deviceId,Socket client, String clientAddress, ConcurrentLinkedQueue<String> sq, int num, ExecutorService pool, ConcurrentLinkedQueue<wrapper> que) {
        Runnable sw = new sw_task(deviceId, client, clientAddress, sq, num, que);
        pool.execute(sw);
    }

    private void create(String deviceId, Socket client, String clientAddress, HashSet<ConcurrentLinkedQueue<String>> Arr, ExecutorService pool, int idx, ConcurrentLinkedQueue<wrapper> que, String Type) {
        count cc;
        if (allCounts.containsKey(deviceId)) {
            cc = allCounts.get(deviceId);
        } else {
            cc = new count(0);
            allCounts.put(deviceId, cc);
        }
        if(Type.equals("OBJ")) {
            Runnable node = new node_task(deviceId, client, clientAddress, cc, Arr, idx, que);
            pool.execute(node);
        }else if(Type.equals("SP")){
            Runnable sp = new sp_task(deviceId,client,clientAddress,cc,Arr,idx,que);
            pool.execute(sp);
        }
    }

    private void listen(ExecutorService pool, ConcurrentLinkedQueue<wrapper> que) throws Exception {
        System.out.println("listening...");
        while (true) {
            Thread.sleep(1);
            Socket client = this.server.accept();
            String clientAddress = client.getInetAddress().getHostAddress();
            System.out.println("connected with" + clientAddress);
            BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));
            long timeDiff = 0;
            timeDiffs.put(client, timeDiff);
            writers.put(client, writer);
            readers.put(client, reader);
            status.put(client, false);
            isCali.put(client, false);
            lastTimeStamps.put(client, System.currentTimeMillis());
            Thread.sleep(10);
            writer.println("TYPE");
            writer.flush();
            while (true) {
                Thread.sleep(1);
                String data = reader.readLine();
                if (data.matches("OBJ.*")) {
                    TypeMap.put(client,"OBJ");
                    if(data.contains("Monitor")){
                        Runnable PlotOverWifi = new Runnable() {
                            PrintWriter writer = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));
                            @Override
                            public void run() {
                                System.out.println("Monitor connected...");
                                while(true){
                                    try {
                                        Thread.sleep(1);
                                        if (gdata != null && gdata.peek() != null) {
                                            String data = gdata.poll();
                                            if (data != null) {
                                                writer.println(data);
                                                writer.flush();
                                            }
                                        }
                                    }catch (InterruptedException e){
                                        e.printStackTrace();
                                    }
                                }
                            }
                        };
                        pool.execute(PlotOverWifi);
                        break;
                    }
                    String deviceId = data.substring(4);
                    if(AddressClient.get(deviceId)!=null){
                        AddressClient.get(deviceId).close();
                    }
                    AddressClient.put(deviceId,client);
                    this.OBJcount++;
                    Thread.sleep(2000);
                    create(deviceId, client, clientAddress, this.QueArr, pool, this.OBJcount, que, "OBJ");
                    break;
                } else if (data.matches("SWT.*")) {
                    TypeMap.put(client,"SWT");
                    String deviceId = data.substring(4);
                    if(AddressClient.get(deviceId)!=null){
                        AddressClient.get(deviceId).close();
                    }
                    AddressClient.put(deviceId,client);
                    this.SWcount++;
                    ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
                    this.QueArr.add(queue);
                    Thread.sleep(2000);
                    create(deviceId, client, clientAddress, queue, this.SWcount, pool, que);
                    break;
                } else if (data.matches("SP.*")){
                    TypeMap.put(client,"SP");
                    String deviceId = data.substring(3);
                    if(AddressClient.get(deviceId)!=null){
                        AddressClient.get(deviceId).close();
                    }
                    AddressClient.put(deviceId,client);
                    this.OBJcount++;
                    Thread.sleep(2000);
                    create(deviceId, client, clientAddress, this.QueArr, pool, this.OBJcount, que, "SP");
                    break;
                }

            }
        }

    }

    private InetAddress getSocketAddress() {
        return this.server.getInetAddress();
    }

    private int getPort() {
        return this.server.getLocalPort();
    }

    public static void main(String[] args) throws Exception {
        if(args.length<1){
            System.out.println("usage: java server /path/to/directory");
            return;
        }
        String path = args[0];
        File directory = new File(path);
        if(!directory.exists()){
            System.out.println("usage: java server /path/to/directory");
            return;
        }
        final String pa = path;
        InetAddress myNetAddress = null;
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface netint : Collections.list(nets)){
            String netintName = netint.getDisplayName();
            if(netintName.startsWith("wl")){
                Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
                for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                    if(inetAddress.toString().startsWith("/1")){
                          myNetAddress = inetAddress;
                    }
                }
            }
        }
        if(myNetAddress == null){
            Console c = System.console();
            if(c==null){
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        System.in));
                System.out.println("net address not found, specify ipAddress manually:");
                String address = reader.readLine();
                myNetAddress = InetAddress.getByName(address);
            }else {
                String addressStr = c.readLine("net address not found, specify ipAddress manually:");
                myNetAddress = InetAddress.getByName(addressStr);
            }
        }
        ConcurrentLinkedQueue<wrapper> que = new ConcurrentLinkedQueue<>();
        ExecutorService pool = Executors.newFixedThreadPool(20);
        TimeLimiter timeLimiter = SimpleTimeLimiter.create(pool);
        Server app = new Server(myNetAddress);
        System.out.println("\r\nRunning Server: " +
                "Host=" + app.getSocketAddress().getHostAddress() +
                " Port=" + app.getPort());
        Runnable timer = new Runnable() {
            private long time;
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(500);
                        if (!writers.isEmpty() && !writers.isEmpty() && !readers.isEmpty() && !Accept.isEmpty()) {
                            for (Map.Entry<Socket,PrintWriter> entry : writers.entrySet()) {
                                if (!(entry.getKey()).isClosed()) {
                                    PrintWriter writer = entry.getValue();
                                    BufferedReader reader = readers.get(entry.getKey());
                                    long timeDiff = timeDiffs.get(entry.getKey());
                                    currTimestamp = System.currentTimeMillis();
                                    long lastTimestamp = lastTimeStamps.get(entry.getKey());
                                    if (Accept.containsKey(entry.getKey()) && status.containsKey(entry.getKey()) && (timeDiff == 0 || currTimestamp - lastTimestamp >1200000) && !status.get(entry.getKey()) && Accept.get(entry.getKey())) {
                                        isCali.put(entry.getKey(), true);
                                        timeDiff = 0;
                                        lastTimeStamps.put(entry.getKey(), currTimestamp);
                                        writer.println("time");
                                        writer.flush();
                                        if(TypeMap.get(entry.getKey()).equals("SWT") || TypeMap.get(entry.getKey()).equals("SP")) {
                                            while (true) {
                                                try {
                                                    String timeMSG = timeLimiter.callWithTimeout(reader::readLine, 10, TimeUnit.SECONDS);
                                                   // String timeMSG = reader.readLine();
                                                    time = System.currentTimeMillis();
                                                    if (timeMSG == null || timeMSG.isEmpty()) {
                                                        (entry.getKey()).close();
                                                        System.out.println("socket closed");
                                                        break;
                                                    }
                                                    if (timeMSG.matches(".*t")) {
                                                        timeDiff += (time - Long.parseLong(timeMSG.substring(0, timeMSG.length() - 1))) % 1000000;
                                                    }
                                                    if (timeMSG.equals("q")) {
                                                        timeDiff /= 5;
                                                        timeDiffs.put(entry.getKey(), timeDiff);
                                                        isCali.put(entry.getKey(), false);
                                                        System.out.println("calibration finished with diff: " + timeDiff);
                                                        break;
                                                    }
                                                } catch (TimeoutException | UncheckedIOException e) {
                                                    if(TypeMap.get(entry.getKey()).equals("SWT")) {
                                                        System.out.println("lost connection, closing...");
                                                        (entry.getKey()).close();
                                                        System.out.println("socket closed");
                                                        break;
                                                    }else{
                                                        writer.println("time");
                                                        writer.flush();
                                                    }
                                                } catch (ExecutionException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }else{
                                            Thread.sleep(5000);
                                        }
                                    }
                                }
                            }
                        }

                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        Runnable writer = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1);
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
                                String fileName = wrap.type + "_" + wrap.idx + "_" + wrap.timeDiff + "_" + wrap.ip + ".csv";
                                File directory = new File(DirectoryName);
                                if (!directory.exists()) {
                                    boolean success = directory.mkdirs();
                                    while(!success){
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
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        };
        pool.execute(writer);
        pool.execute(timer);
        app.listen(pool, que);
    }
}

class count {
    private int count;

    count(int num){
        this.count = num;
    }

    synchronized void increment() {
        this.count++;
    }

    synchronized int get() {
        return this.count;
    }
}
class sp_task implements Runnable{
    private Socket client;
    private String ip;
    private count cc;
    private HashSet<ConcurrentLinkedQueue<String>> arr;
    private int idx;
    private ConcurrentLinkedQueue<wrapper> que;
    private String deviceId;
    sp_task(String deviceId,Socket client,String ip, count cc, HashSet<ConcurrentLinkedQueue<String>> arr, int idx, ConcurrentLinkedQueue<wrapper> que){
        this.client = client;
        this.ip = ip;
        this.cc = cc;
        this.arr = arr;
        this.idx = idx;
        this.que = que;
        this.deviceId = deviceId;
    }
    public void run(){
        SensorData data;
        try{
            Server.Accept.put(client,true);
            System.out.println("Smart phone thread start.");
            BufferedReader reader = new BufferedReader(new InputStreamReader(this.client.getInputStream()));
            while(true){
                Thread.sleep(1);
                if(client.isClosed()){
                    System.out.println("Smart phone socket closed, closing...");
                }
                String message = reader.readLine();
                if(message.equals("start")) {
                    data = new SensorData();
                    //System.out.println("S" + this.idx);
                    System.out.println("Smart Phone" + this.idx + " moving...");
                    Server.status.put(client, true);
                    this.cc.increment();
                    String sign_start = "start_" + this.deviceId;
                    for (ConcurrentLinkedQueue<String> queue : arr) {
                        while (true) {
                            if (queue.offer(sign_start)) {
                                break;
                            }
                        }
                    }
                    while (true) {
                        Thread.sleep(1);
                        if (!Server.isCali.get(client)) {
                            String ss = reader.readLine();
                            if (ss.equals("stop")) {
                                //System.out.println("E" + this.idx);
                                System.out.println("smart phone" + this.idx + " static");
                                String sign_stop = "stop_" + this.deviceId;
                                for (ConcurrentLinkedQueue<String> queue : arr) {
                                    while (true) {
                                        if (queue.offer(sign_stop)) {
                                            break;
                                        }
                                    }
                                }
                                int c = this.cc.get();
                                wrapper toPut = new wrapper();
                                toPut.da = data;
                                toPut.cc = c;
                                toPut.idx = this.idx;
                                toPut.ip = this.ip;
                                toPut.timeDiff = Server.timeDiffs.get(this.client);
                                toPut.type = "SP";
                                toPut.label = this.deviceId;
                                while (true) {
                                    if (this.que.offer(toPut)) {
                                        break;
                                    }
                                }
                                Server.status.put(client, false);
                                break;
                            }
                            data.write(ss);
                            System.out.println(ss);
                        }

                    }
                }
            }
        }catch(IOException | InterruptedException e){
            e.printStackTrace();
        }
    }
}
class node_task implements Runnable {
    private Socket client;
    private String ip;
    private count cc;
    private HashSet<ConcurrentLinkedQueue<String>> arr;
    private int idx;
    private ConcurrentLinkedQueue<wrapper> que;
    private String deviceId;
    private long timeDiff;
    private boolean terminate = false;
    node_task(String deviceId, Socket client, String ip, count cc, HashSet<ConcurrentLinkedQueue<String>> arr, int idx, ConcurrentLinkedQueue<wrapper> que) {
        this.client = client;
        this.ip = ip;
        this.cc = cc;
        this.arr = arr;
        this.idx = idx;
        this.que = que;
        this.deviceId =deviceId;
    }

    public void run() {
        SensorData sd;
        long time;
        try {
            Server.Accept.put(client, true);
            System.out.println("Object thread start");
            BufferedReader reader = new BufferedReader(new InputStreamReader(this.client.getInputStream()));
            while (true) {
                Thread.sleep(1);
                if (client.isClosed()|| this.terminate) {
                    System.out.println("Socket closed terminating...");
                    break;
                }
                String message = reader.readLine();
                time = System.currentTimeMillis();
               // System.out.println(this.time);
                //for calibration only read once;
                if(Server.isCali.get(client)){
                    timeDiff=0;
                    Server.isCali.put(client,false);
                }
                if (message.equals("S")) {
                            sd = new SensorData();
                            System.out.println("S"+this.idx);
                            System.out.println("OBJ"+this.idx+" moving...");
                            Server.status.put(client, true);
                            this.cc.increment();
                            String sign_start = "start_" + this.deviceId;
                            for (ConcurrentLinkedQueue<String> queue : arr) {
                                while (true) {
                                    if (queue.offer(sign_start)) {
                                        break;
                                    }
                                }
                            }
                            while (true) {
                                Thread.sleep(1);
                                if (!Server.isCali.get(client)) {
                                    String ss;
                                    try {
                                        ExecutorService aThread = Executors.newSingleThreadExecutor();
                                        TimeLimiter timeLimiter = SimpleTimeLimiter.create(aThread);
                                        ss = timeLimiter.callWithTimeout(reader::readLine, 3, TimeUnit.SECONDS);
                                    }catch ( TimeoutException e){
                                        System.out.println("OBJ"+this.idx+" lost connection.");
                                        this.terminate = true;
                                        break;
                                    }
                                    if (ss.equals("E")) {
                                        System.out.println("E" + this.idx);
                                        System.out.println("OBJ" + this.idx + " static");
                                        String sign_stop = "stop_" + this.deviceId;
                                        for (ConcurrentLinkedQueue<String> queue : arr) {
                                            while (true) {
                                                if (queue.offer(sign_stop)) {
                                                    break;
                                                }
                                            }
                                        }
                                        int c = this.cc.get();
                                        wrapper toPut = new wrapper();
                                        toPut.da = sd;
                                        toPut.cc = c;
                                        toPut.idx = this.idx;
                                        toPut.ip = this.ip;
                                        toPut.timeDiff = Server.timeDiffs.get(this.client);
                                        toPut.type = "OBJ";
                                        toPut.label = this.deviceId;
                                        while (true) {
                                            if (this.que.offer(toPut)) {
                                                break;
                                            }
                                        }
                                        Server.status.put(client, false);
                                        break;
                                    }
                                    sd.write(ss);
                                }
                            }
                        }
                    else if (message.matches(".*T")) {
                        timeDiff += (time - Long.parseLong(message.substring(0, message.length() - 1)))%1000000;
                    }
                    else if (message.equals("Q")) {
                        timeDiff /= 5;
                        Server.timeDiffs.put(this.client, timeDiff);
                        Server.isCali.put(this.client, false);
                        System.out.println("calibration finished with differnce:"+timeDiff);
                    }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class sw_task implements Runnable {
    private volatile int startCount = 0;
    private volatile boolean isStart = false;
    private Map<String, SensorData> SenMap = new ConcurrentHashMap<>();
    private Map<String, count> CountMap;
    private Socket client;
    private String ip;
    private ConcurrentLinkedQueue<String> sq;
    private int swc;
    private ExecutorService pool;
    private ConcurrentLinkedQueue<wrapper> que;


    sw_task(String deviceId, Socket client, String ip, ConcurrentLinkedQueue<String> sq, int num, ConcurrentLinkedQueue<wrapper> que) {
        this.client = client;
        this.ip = ip;
        this.sq = sq;
        this.swc = num;
        this.pool = Executors.newFixedThreadPool(10);
        this.que = que;
        if (Server.allCountMaps.containsKey(deviceId)) {
            this.CountMap = Server.allCountMaps.get(deviceId);
        } else {
            this.CountMap = new ConcurrentHashMap<>();
            Server.allCountMaps.put(deviceId, this.CountMap);
        }
    }

    public void run() {
        try {
            Server.Accept.put(client,true);
            System.out.println("watch thread start");
            BufferedReader reader = new BufferedReader(new InputStreamReader(this.client.getInputStream()));
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(this.client.getOutputStream()));
            while (true) {
                Thread.sleep(1);
                if (client.isClosed()) {
                    System.out.println("socket closed terminating...");
                    break;
                }
                try {
                    if (sq != null && sq.peek() != null && sq.peek().startsWith("start")) {
                        synchronized (this) {
                            startCount++;
                        }
                        String sig = this.sq.poll();
                        String sigh = sig.substring(6);
                        //System.out.println(sigh);
                        this.SenMap.put(sigh, new SensorData());
                        if (this.CountMap.get(sigh) == null) {
                            count cou = Server.allCounts.get(sigh);
                            count c = new count(cou.get());
                            this.CountMap.put(sigh, c);
                        } else {
                            this.CountMap.get(sigh).increment();
                        }
                        if (!this.isStart) {
                            this.isStart = true;
                            Server.status.put(client, true);
                            Runnable bvn = new Runnable() {
                                @Override
                                public void run() {
                                    System.out.println("watch"+swc+" wake");
                                    while (true) {
                                        if (client.isClosed()) {
                                            System.out.println("socket closed terminating...");
                                            break;
                                        }
                                        try {
                                            Thread.sleep(1);
                                            if (!Server.isCali.get(client)) {
                                                String data = reader.readLine();

                                                if (data.contains("stop")) {
                                                    Server.status.put(client, false);
                                                    System.out.println("watch" + swc + " sleep");
                                                    break;
                                                }
                                                for (Map.Entry entry : SenMap.entrySet()) {
                                                    ((SensorData) entry.getValue()).write(data);

                                                }
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            };
                            this.pool.execute(bvn);
                            writer.println("s");
                            writer.flush();
                        }
                        Runnable bvm = new Runnable() {
                            private String label = sigh;
                            private SensorData Sen = SenMap.get(sigh);

                            @Override
                            public void run() {
                                while (true) {
                                    if (client.isClosed()) {
                                        System.out.println("Socket closed terminating...");
                                        break;
                                    }
                                    try {
                                        Thread.sleep(1);
                                        if (sq != null && sq.peek() != null && (sq.peek().equals("stop_" + this.label))) {
                                            sq.poll();
                                            synchronized (sw_task.class) {
                                                startCount--;
                                            }
                                            if (startCount == 0) {
                                                isStart = false;
                                                writer.println("e");
                                                writer.flush();
                                            }
                                            int c;
                                            c = CountMap.get(this.label).get();
                                            wrapper toPut = new wrapper();
                                            toPut.da = this.Sen;
                                            toPut.cc = c;
                                            toPut.idx = swc;
                                            toPut.ip = ip;
                                            toPut.timeDiff = Server.timeDiffs.get(client);
                                            toPut.type = "SWT";
                                            toPut.label = this.label;
                                            while (true) {
                                                if (que.offer(toPut)) {
                                                    break;
                                                }
                                            }
                                            SenMap.remove(sigh);
                                            break;
                                        }
                                    } catch (NullPointerException e) {
                                        e.fillInStackTrace();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        };
                        this.pool.execute(bvm);
                    }
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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
    SensorData da;
    String type;
    int idx;
    long timeDiff;
    String ip;
    int cc;
    String label;
}