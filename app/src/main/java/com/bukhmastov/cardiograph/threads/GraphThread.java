package com.bukhmastov.cardiograph.threads;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.SurfaceHolder;

import com.bukhmastov.cardiograph.utils.Storage;

import java.util.Arrays;

public class GraphThread extends Thread {

    public static final int GT_DATA_LOOSING = 0;
    public static final int GT_DATA_NORMAL = 1;

    private boolean running = true;
    private boolean paused = false;
    private final Object pauseLock = new Object();
    private boolean ready = false;
    private final SurfaceHolder surfaceHolder;
    private final Handler handler;
    private final int FRAME_RATE;
    private final int PIXEL_PER_FRAME;
    private final int MAX_POINT_ABS;
    private final int POINT_EMPTY;
    private int width = 0, height = 0;
    private int middle;
    private double middleDouble, middleDoubleRatio, graphWidthDouble;
    private boolean upToDate = false;
    private int pointsLost = 0;
    private Paint graphLine, gridLine, gridBoldLine;
    private int[] buffer;
    private int gridShift = 0;
    private boolean data_loosing = false;

    public GraphThread(SurfaceHolder surfaceHolder, Context context, Handler handler) {
        this.surfaceHolder = surfaceHolder;
        this.handler = handler;
        FRAME_RATE = Integer.parseInt(Storage.pref.get(context, "frame_rate", "40"));
        PIXEL_PER_FRAME = Integer.parseInt(Storage.pref.get(context, "pixel_per_frame", "2"));
        MAX_POINT_ABS = Integer.parseInt(Storage.pref.get(context, "max_point_abs", "250"));
        POINT_EMPTY = -(MAX_POINT_ABS + 100);
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
        unpause();
        while (!Thread.currentThread().isInterrupted() && running) {
            synchronized (pauseLock) {
                if (!ready) continue;
                if (paused) {
                    try {
                        pauseLock.wait();
                    } catch (InterruptedException ex) {
                        break;
                    }
                }
                long timeStart = System.currentTimeMillis();
                canvas = surfaceHolder.lockCanvas(null);
                try {
                    if (!upToDate) {
                        incoming(POINT_EMPTY);
                        pointsLost++;
                        if (pointsLost > FRAME_RATE * PIXEL_PER_FRAME && !data_loosing) {
                            Message m = new Message();
                            Bundle b = new Bundle();
                            b.putInt("what", GT_DATA_LOOSING);
                            m.setData(b);
                            handler.sendMessage(m);
                            data_loosing = true;
                        }
                    } else if (data_loosing) {
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
                        if (canvas != null) {
                            canvas.drawColor(Color.rgb(242, 245, 242));
                            // draw grid
                            int pointsReceived = 0;
                            gridShift += PIXEL_PER_FRAME * pointsReceived;
                            if (gridShift >= 100) gridShift = gridShift % 100;
                            for (int i = middle; i >= 0; i -= 10) {
                                if (i % 100 == middle % 100) {
                                    if (i == middle) {
                                        canvas.drawLine(0, middle + 1, width, middle + 1, gridLine);
                                        canvas.drawLine(0, middle, width, middle, gridBoldLine);
                                        canvas.drawLine(0, middle - 1, width, middle - 1, gridLine);
                                    } else {
                                        canvas.drawLine(0, i, width, i, gridBoldLine);
                                        canvas.drawLine(0, height - i, width, height - i, gridBoldLine);
                                    }
                                } else {
                                    canvas.drawLine(0, i, width, i, gridLine);
                                    canvas.drawLine(0, height - i, width, height - i, gridLine);
                                }
                            }
                            for (int i = width - gridShift % 10; i >= 0; i -= 10) {
                                if (i % 100 == (width - gridShift) % 100) {
                                    canvas.drawLine(i, 0, i, height, gridBoldLine);
                                } else {
                                    canvas.drawLine(i, 0, i, height, gridLine);
                                }
                            }
                            // draw graph
                            int counter;
                            for (int i = buffer.length - 1; i > 0; i--) {
                                if (buffer[i] == POINT_EMPTY) continue;
                                counter = 0;
                                for (int j = i - 1; j > 0; j--) {
                                    if (buffer[j] == POINT_EMPTY) {
                                        if (counter > (FRAME_RATE * PIXEL_PER_FRAME) / 2) {
                                            i = j;
                                            break;
                                        }
                                        counter++;
                                    } else {
                                        double xFROM = graphWidthDouble - PIXEL_PER_FRAME * (buffer.length - j);
                                        double xT0 = graphWidthDouble - PIXEL_PER_FRAME * (buffer.length - i);
                                        double yFROM = middleDouble - buffer[j] * middleDoubleRatio;
                                        double yTO = middleDouble - buffer[i] * middleDoubleRatio;
                                        canvas.drawLine((int) xFROM, (int) yFROM, (int) xT0, (int) yTO, graphLine);
                                        i = j + 1;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } finally {
                    if (canvas != null) {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }
                long delayTime = 1000 / FRAME_RATE - (System.currentTimeMillis() - timeStart);
                delayTime = delayTime > 0 ? delayTime : 1;
                try {
                    Thread.sleep(delayTime);
                } catch (InterruptedException e) {
                    break;
                }
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
        middleDoubleRatio = middleDouble / (double)MAX_POINT_ABS;
        graphWidthDouble = (double)width;
        gridShift = 1;
        buffer = new int[(int)(graphWidthDouble / PIXEL_PER_FRAME)];
        Arrays.fill(buffer, POINT_EMPTY);
        ready = true;
    }
    public void incoming(int point){
        if (point >  MAX_POINT_ABS && point != POINT_EMPTY) point =  MAX_POINT_ABS;
        if (point < -MAX_POINT_ABS && point != POINT_EMPTY) point = -MAX_POINT_ABS;
        int[] tmp = new int[buffer.length];
        Arrays.fill(tmp, POINT_EMPTY);
        System.arraycopy(buffer, 1, tmp, 0, buffer.length - 1);
        System.arraycopy(buffer, 0, tmp, buffer.length - 1, 1);
        tmp[tmp.length-1] = point;
        buffer = tmp;
        if (point != POINT_EMPTY) upToDate = true;
    }
    public void pause(){
        paused = true;
    }
    public void unpause(){
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll();
        }
    }

}
