package com.company;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.knowm.xchart.QuickChart;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYSeries;

import javax.swing.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SwingWorkerRealTime {
    public static ConcurrentLinkedQueue<String> data0 = new ConcurrentLinkedQueue<>();
    public static ConcurrentLinkedQueue<String> data1 = new ConcurrentLinkedQueue<>();
    public static ConcurrentLinkedQueue<String> data2 = new ConcurrentLinkedQueue<>();
    public static ConcurrentLinkedQueue<String> data3 = new ConcurrentLinkedQueue<>();

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
        chart.getStyler().setYAxisMax(3.0);
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
                try {
                    Thread.sleep(1);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
                String obj = null;
                String swt = null;
                String ngh = null;
                String tdp = null;
                if (data0 != null&&data0.peek()!=null) {
                    obj = data0.poll();
                    //System.out.println(obj);
                }
                if (data1 != null&&data1.peek()!=null) {
                    swt = data1.poll();
                }
                if (data2 != null&&data2.peek()!=null) {
                    ngh = data2.poll();
                    //System.out.println(ngh);
                }
                if (data3 !=null&&data3.peek()!=null) {
                    tdp = data3.poll();
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