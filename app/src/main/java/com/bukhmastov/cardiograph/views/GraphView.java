package com.bukhmastov.cardiograph.views;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bukhmastov.cardiograph.R;
import com.bukhmastov.cardiograph.activities.ConnectionActivity;
import com.bukhmastov.cardiograph.activities.ReplayActivity;
import com.bukhmastov.cardiograph.threads.GraphThread;

public class GraphView extends SurfaceView implements SurfaceHolder.Callback {

    public GraphThread graphThread = null;

    public GraphView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        getHolder().addCallback(this);
        init();
    }
    public GraphView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getHolder().addCallback(this);
        init();
    }
    public GraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        init();
    }
    public GraphView(Context context) {
        super(context);
        getHolder().addCallback(this);
        init();
    }

    private void init(){
        switch (this.getId()){
            case R.id.graphView1:
                ConnectionActivity.graphView1 = this;
                ReplayActivity.graphView1 = this;
                break;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        ViewGroup.LayoutParams lp = this.getLayoutParams();
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        lp.height = (int) (Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(getContext()).getString("graph_height", "100")) * getContext().getResources().getDisplayMetrics().density);
        this.setLayoutParams(lp);
        graphThread = new GraphThread(getHolder(), getContext(), handler);
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
                // shit happens ¯\_(ツ)_/¯
            }
        }
        switch (this.getId()){
            case R.id.graphView1:
                ConnectionActivity.graphView1 = this;
                ReplayActivity.graphView1 = this;
                break;
        }
    }

    private Handler handler = new Handler() {

        private static final String TAG = "GraphView.Handler";

        @Override
        public void handleMessage(Message msg) {
            switch (msg.getData().getInt("what")) {
                case GraphThread.GT_DATA_LOOSING: {
                    View thisView = findViewById(getId());
                    if (thisView != null) {
                        ViewGroup parent = (ViewGroup) thisView.getParent();
                        if (parent != null) {
                            try {
                                parent.removeViewAt(1);
                            } catch (NullPointerException e) {
                                // it's normal
                            }
                            TextView label = new TextView(getContext());
                            label.setText(R.string.data_loosing);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                label.setTextColor(getResources().getColor(R.color.darkRed, null));
                            }
                            label.setAllCaps(true);
                            label.setTypeface(null, Typeface.BOLD);
                            parent.addView(label);
                        }
                    }
                    break;
                }
                case GraphThread.GT_DATA_NORMAL: {
                    View thisView = findViewById(getId());
                    if (thisView != null) {
                        ViewGroup parent = (ViewGroup) thisView.getParent();
                        if (parent != null) {
                            try {
                                parent.removeViewAt(1);
                            } catch (NullPointerException e) {
                                // it's normal
                            }
                        }
                    }
                    break;
                }
            }
        }
    };

}
