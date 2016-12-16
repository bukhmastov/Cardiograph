package com.bukhmastov.cardiograph;

import android.content.res.Resources;
import android.os.Handler;
import android.view.SurfaceHolder;

public class GraphThread extends Thread {
    private final static String TAG = "GraphThread";
    private boolean running = true;
    GraphThread(SurfaceHolder holder, Resources resourses, Handler handler) {

    }
    public void run(){

    }
    public void cancel() {
        running = false;
        interrupt();
    }
}
