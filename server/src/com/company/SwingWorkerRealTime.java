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
    public boolean Terminate = false;
    public static ConcurrentLinkedQueue<String> data1 = new ConcurrentLinkedQueue<>();

    MySwingWorker mySwingWorker;
    SwingWrapper<XYChart> sw;
    XYChart chart;

    public void go() {
        String[] names = {"data0"};
        // Create Chart
        chart =
                QuickChart.getChart(
                        "LACC Real-time Curve",
                        "Time",
                        "LACC Value",
                        names,
                        new double[] {0},
                        new double[][] {{0}});

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
        LinkedList<Double> fifo3 = new LinkedList();
        LinkedList<String[]> buffer3 = new LinkedList<>();
        double[] ydata1 = new double[1000];
        ArrayList<String[]> st;
        Gson gson = new Gson();
        Type colloectionType = new TypeToken<ArrayList<String[]>>() {}.getType();
        @Override
        protected Boolean doInBackground(){
            while(!isCancelled() && !Terminate) {
                try {
                    Thread.sleep(1);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
                String swt = null;
                if (data1 != null&&data1.peek()!=null) {
                    swt = data1.poll();
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
                for (int i = 0; i < fifo3.size(); i++) {
                    ydata1[i]=fifo3.get(i);
                }
                publish(ydata1);
            }
            return true;
        }

        @Override
        protected void process(List<double[]> chunks) {
            double[] ydata0 = chunks.get(chunks.size() - 1);
            chart.updateXYSeries("data0",null,ydata0, null);
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