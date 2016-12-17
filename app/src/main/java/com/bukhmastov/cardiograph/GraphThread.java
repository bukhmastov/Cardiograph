package com.bukhmastov.cardiograph;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.SurfaceHolder;

import java.util.Arrays;

public class GraphThread extends Thread {
    private final static String TAG = "GraphThread";
    private boolean running = true;
    private boolean ready = false;
    private final SurfaceHolder surfaceHolder;
    private final Handler handler;
    private final int FRAME_RATE = 40;  // syns with FRAME_RATE at arduino software
    private final int PIXEL_PER_FRAME = 2; // from 1 to 10
    private final int POINT_EMPTY = -520;
    private int width = 0, height = 0;
    private int middle;
    private double middleDouble, middleDoubleRatio, graphWidthDouble;
    private boolean upToDate = false;
    private int pointsLost = 0;
    private Paint graphLine, gridLine, gridBoldLine;
    private int[] buffer;
    private int gridShift = 0;
    public static int GT_DATA_LOOSING = 0;
    public static int GT_DATA_NORMAL = 1;
    private boolean data_loosing = false;

    GraphThread(SurfaceHolder surfaceHolder, Handler handler) {
        this.surfaceHolder = surfaceHolder;
        this.handler = handler;
        graphLine = new Paint();
        graphLine.setColor(Color.RED);
        graphLine.setStrokeWidth(2);
        gridLine = new Paint();
        gridLine.setColor(Color.LTGRAY);
        gridLine.setStrokeWidth(1);
        gridBoldLine = new Paint();
        gridBoldLine.setColor(Color.DKGRAY);
        gridBoldLine.setStrokeWidth(1);
        pointsLost = 0;
    }
    public void run() {
        Canvas canvas;
        while(!Thread.currentThread().isInterrupted() && running) {
            if(!ready) continue;
            long timeStart = System.currentTimeMillis();
            canvas = surfaceHolder.lockCanvas(null);
            try {
                if(!upToDate){
                    incoming(POINT_EMPTY);
                    pointsLost++;
                    if(pointsLost > FRAME_RATE * PIXEL_PER_FRAME && !data_loosing){
                        Message m = new Message();
                        Bundle b = new Bundle();
                        b.putInt("what", GT_DATA_LOOSING);
                        m.setData(b);
                        handler.sendMessage(m);
                        data_loosing = true;
                    }
                } else if(data_loosing){
                    Message m = new Message();
                    Bundle b = new Bundle();
                    b.putInt("what", GT_DATA_NORMAL);
                    m.setData(b);
                    handler.sendMessage(m);
                    pointsLost = 0;
                    data_loosing = false;
                }
                upToDate = false;
                synchronized (surfaceHolder) {
                    if(canvas != null){
                        canvas.drawColor(Color.rgb(242, 245, 242));
                        // draw grid
                        //gridShift += PIXEL_PER_FRAME * pointsReceived;
                        //if(gridShift >= 100) gridShift = gridShift % 100;
                        for(int i = middle; i >= 0; i -= 10){
                            if(i % 100 == middle % 100){
                                if(i == middle){
                                    canvas.drawLine(0, middle+1, width, middle+1, gridLine);
                                    canvas.drawLine(0, middle, width, middle, gridBoldLine);
                                    canvas.drawLine(0, middle-1, width, middle-1, gridLine);
                                } else {
                                    canvas.drawLine(0, i, width, i, gridBoldLine);
                                    canvas.drawLine(0, height-i, width, height-i, gridBoldLine);
                                }
                            } else {
                                canvas.drawLine(0, i, width, i, gridLine);
                                canvas.drawLine(0, height-i, width, height-i, gridLine);
                            }
                        }
                        for(int i = width - gridShift % 10; i >= 0; i -= 10){
                            if(i % 100 == (width - gridShift) % 100){
                                canvas.drawLine(i, 0, i, height, gridBoldLine);
                            } else {
                                canvas.drawLine(i, 0, i, height, gridLine);
                            }
                        }
                        // draw graph
                        int counter;
                        for (int i = buffer.length - 1; i > 0; i--) {
                            if(buffer[i] == POINT_EMPTY) continue;
                            counter = 0;
                            for (int j = i - 1; j > 0; j--) {
                                if(buffer[j] == POINT_EMPTY){
                                    if(counter > FRAME_RATE/2){
                                        i = j - 1;
                                        break;
                                    }
                                    counter++;
                                } else {
                                    double xFROM = graphWidthDouble - PIXEL_PER_FRAME * (buffer.length + 1 - i);
                                    double xT0 =   graphWidthDouble - PIXEL_PER_FRAME * (buffer.length - 1 - j);
                                    double yFROM = middleDouble - buffer[j] * middleDoubleRatio;
                                    double yTO =   middleDouble - buffer[i] * middleDoubleRatio;
                                    canvas.drawLine((int)xT0, (int)yTO, (int)xFROM, (int)yFROM, graphLine);
                                    i = j + 1;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            finally {
                if (canvas != null) {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
            long delayTime = 1000/FRAME_RATE - (System.currentTimeMillis() - timeStart);
            delayTime = delayTime > 0 ? delayTime : 1;
            try {
                Thread.sleep(delayTime);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    public void cancel() {
        running = false;
        interrupt();
    }
    public void setUP(int w, int h){
        width = w;
        height = h;
        middle = height/2;
        middleDouble = (double)height / 2.0;
        middleDoubleRatio = middleDouble / 511.0;
        graphWidthDouble = (double) width;
        gridShift = 1;
        buffer = new int[(int)(graphWidthDouble / PIXEL_PER_FRAME)];
        Arrays.fill(buffer, POINT_EMPTY);
        ready = true;
    }
    public void incoming(int point){
        int[] tmp = new int[buffer.length];
        Arrays.fill(tmp, POINT_EMPTY);
        System.arraycopy(buffer, 1, tmp, 0, buffer.length - 1);
        System.arraycopy(buffer, 0, tmp, buffer.length - 1, 1);
        tmp[tmp.length-1] = point;
        buffer = tmp;
        if(point != POINT_EMPTY) upToDate = true;
        //Log.d(TAG, "incoming | point " + point + (point == POINT_EMPTY ? " ALARM" : ""));
    }
}
