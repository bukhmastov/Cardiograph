package com.bukhmastov.cardiograph;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.TextView;

public class GraphView extends SurfaceView implements SurfaceHolder.Callback {
    private final static String TAG = "GraphView";
    private GraphView _this = null;
    public GraphThread graphThread = null;

    public GraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        _this = this;
        init();
    }
    public GraphView(Context context) {
        super(context);
        getHolder().addCallback(this);
        _this = this;
        init();
    }
    private void init(){
        switch (this.getId()){
            case R.id.graphView1: ConnectionActivity.graphView1 = this; break;
            case R.id.graphView2: ConnectionActivity.graphView2 = this; break;
            case R.id.graphView3: ConnectionActivity.graphView3 = this; break;
            case R.id.graphView4: ConnectionActivity.graphView4 = this; break;
            case R.id.graphView5: ConnectionActivity.graphView5 = this; break;
        }
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
        switch (this.getId()){
            case R.id.graphView1: ConnectionActivity.graphView1 = null; break;
            case R.id.graphView2: ConnectionActivity.graphView2 = null; break;
            case R.id.graphView3: ConnectionActivity.graphView3 = null; break;
            case R.id.graphView4: ConnectionActivity.graphView4 = null; break;
            case R.id.graphView5: ConnectionActivity.graphView5 = null; break;
        }
    }
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.getData().getInt("what") == GraphThread.GT_DATA_LOOSING) {
                TextView label = new TextView(getContext());
                label.setText(R.string.data_loosing);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) label.setTextColor(getResources().getColor(R.color.darkRed, null));
                label.setAllCaps(true);
                label.setTypeface(null, Typeface.BOLD);
                ((ViewGroup) findViewById(_this.getId()).getParent()).addView(label);
            }
            else if(msg.getData().getInt("what") == GraphThread.GT_DATA_NORMAL){
                try {
                    ((ViewGroup) findViewById(_this.getId()).getParent()).removeViewAt(1);
                } catch(Exception e){}
            }
        }
    };
}
