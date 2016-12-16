package com.bukhmastov.cardiograph;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GraphView extends SurfaceView implements SurfaceHolder.Callback {
    private final static String TAG = "GraphView";
    public GraphThread graphThread = null;

    public GraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        switch (this.getId()){
            case R.id.graphView1: ConnectionActivity.graphView1 = this; break;
            case R.id.graphView2: ConnectionActivity.graphView2 = this; break;
            case R.id.graphView3: ConnectionActivity.graphView3 = this; break;
            case R.id.graphView4: ConnectionActivity.graphView4 = this; break;
            case R.id.graphView5: ConnectionActivity.graphView5 = this; break;
        }
    }
    public GraphView(Context context) {
        super(context);
        getHolder().addCallback(this);
    }
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        graphThread = new GraphThread(getHolder(), handler);
        graphThread.start();
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        graphThread.setUP(width, height);
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        graphThread.cancel();
        while (retry) {
            try {
                graphThread.join();
                retry = false;
            } catch (InterruptedException e) {
                // shit happens
            }
        }
    }
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // handler
            // here
        }
    };
}
