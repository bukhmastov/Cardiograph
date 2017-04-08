package com.bukhmastov.cardiograph.threads;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.bukhmastov.cardiograph.utils.Storage;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ReplayThread extends Thread {

    private final static String TAG = "ReplayThread";
    private Context context;
    private Handler handler;

    private byte[] archive = null;
    private int index = 0;

    private boolean running = true;
    private boolean paused = false;
    private final Object pauseLock = new Object();

    private static final int BYTES_PER_FRAME = 3;

    public static final int MESSAGE_CONTROL = 0;
    public static final int MESSAGE_STATE = 1;
    public static final int MESSAGE = 2;

    private final int FRAME_RATE;

    public ReplayThread(Context context, byte[] archive, Handler handler) {
        this.context = context;
        this.archive = archive;
        this.handler = handler;
        FRAME_RATE = Integer.parseInt(Storage.pref.get(context, "frame_rate", "40"));
    }

    public void run(){
        state(MESSAGE_CONTROL, "started");
        unpause();
        while (!Thread.currentThread().isInterrupted() || running) {
            synchronized (pauseLock) {
                try {
                    if (paused) {
                        try {
                            pauseLock.wait();
                        } catch (InterruptedException ex) {
                            running = false;
                            break;
                        }
                    }
                    if (index > Integer.MAX_VALUE - 10) {
                        throw new Exception("archive index out of Integer.MAX_VALUE");
                    }
                    long timeStart = System.currentTimeMillis();
                    for (int i = index; i < archive.length; i += BYTES_PER_FRAME) {
                        index += BYTES_PER_FRAME;
                        if (archive[i] == (byte) 0xef) {
                            break;
                        } else {
                            byte[] pair = {archive[i + 1], archive[i + 2]};
                            int message = (int) ByteBuffer.wrap(pair).order(ByteOrder.LITTLE_ENDIAN).getShort();
                            Message m = new Message();
                            Bundle b = new Bundle();
                            b.putInt("what", MESSAGE);
                            b.putByte("type", archive[i]);
                            b.putInt("value", message);
                            m.setData(b);
                            handler.sendMessage(m);
                        }
                    }
                    if (index >= archive.length) {
                        running = false;
                        break;
                    }
                    long delayTime = 1000 / FRAME_RATE - (System.currentTimeMillis() - timeStart);
                    delayTime = delayTime > 0 ? delayTime : 1;
                    try {
                        Thread.sleep(delayTime);
                    } catch (InterruptedException e) {
                        running = false;
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    running = false;
                }
            }
        }
        state(MESSAGE_CONTROL, "finished");
    }

    public void cancel() {
        running = false;
        interrupt();
    }
    public void pause(){
        paused = true;
        state(MESSAGE_STATE, "paused");
    }
    public void unpause(){
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll();
        }
        state(MESSAGE_STATE, "resumed");
    }
    private void state(int what, String state){
        Message m = new Message();
        Bundle b = new Bundle();
        b.putInt("what", what);
        b.putString("state", state);
        m.setData(b);
        handler.sendMessage(m);
    }

}
