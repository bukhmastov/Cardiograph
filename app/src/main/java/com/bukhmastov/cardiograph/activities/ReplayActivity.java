package com.bukhmastov.cardiograph.activities;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bukhmastov.cardiograph.R;
import com.bukhmastov.cardiograph.threads.ReplayThread;
import com.bukhmastov.cardiograph.utils.Storage;
import com.bukhmastov.cardiograph.views.GraphView;

public class ReplayActivity extends AppCompatActivity {

    private byte[] archive = null;
    public ReplayThread replayThread = null;

    public LinearLayout container;
    public static GraphView graphView1 = null;

    private boolean pulseIsShowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_replay);
        setSupportActionBar((Toolbar) findViewById(R.id.replay_toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        try {
            String fileName = getIntent().getStringExtra("file");
            if (fileName == null || fileName.isEmpty()) throw new Exception("Extra value 'file' required");
            archive = Storage.file.get(this, "archive#" + fileName, false);
            if (archive == null) throw new Exception("Archived data is null");
            container = (LinearLayout) findViewById(R.id.container);
            ((TextView) findViewById(R.id.filename)).setText(fileName);
            findViewById(R.id.pause).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (replayThread != null) {
                        replayThread.pause();
                    }
                }
            });
            findViewById(R.id.resume).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (replayThread != null) {
                        replayThread.unpause();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (replayThread == null) {
            replayThread = new ReplayThread(this, archive, handler);
            replayThread.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (replayThread != null) {
            replayThread.cancel();
            replayThread = null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: finish(); return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    private View inflate(int layout) {
        return ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layout, null);
    }
    private void draw(int layoutId){
        try {
            ViewGroup vg = ((ViewGroup) findViewById(R.id.container));
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.getData().getInt("what")) {
                case ReplayThread.MESSAGE_CONTROL: {
                    String state = msg.getData().getString("state");
                    assert state != null;
                    switch (state) {
                        case "started": {
                            draw(R.layout.connected_interface);
                            break;
                        }
                        case "finished": {
                            findViewById(R.id.pause).setVisibility(View.GONE);
                            findViewById(R.id.resume).setVisibility(View.GONE);
                            draw(R.layout.connection_state);
                            state = "Сессия завершена";
                            LinearLayout connection_image = (LinearLayout) findViewById(R.id.connection_image);
                            if (connection_image != null) {
                                connection_image.removeAllViews();
                                ImageView gg = new ImageView(getBaseContext());
                                gg.setImageDrawable(getDrawable(R.drawable.ic_disconnected));
                                connection_image.addView(gg);
                            }
                            TextView textView = (TextView) findViewById(R.id.connection_state);
                            if (textView != null) {
                                textView.setText(state);
                            }
                            break;
                        }
                    }
                    break;
                }
                case ReplayThread.MESSAGE_STATE: {
                    String state = msg.getData().getString("state");
                    assert state != null;
                    switch (state) {
                        case "paused": {
                            findViewById(R.id.pause).setVisibility(View.GONE);
                            findViewById(R.id.resume).setVisibility(View.VISIBLE);
                            if (graphView1 != null && graphView1.graphThread != null) {
                                graphView1.graphThread.pause();
                            }
                            break;
                        }
                        case "resumed": {
                            findViewById(R.id.pause).setVisibility(View.VISIBLE);
                            findViewById(R.id.resume).setVisibility(View.GONE);
                            if (graphView1 != null && graphView1.graphThread != null) {
                                graphView1.graphThread.unpause();
                            }
                            break;
                        }
                    }
                    break;
                }
                case ReplayThread.MESSAGE: {
                    Byte type = msg.getData().getByte("type");
                    int value = msg.getData().getInt("value");
                    switch (type) {
                        case (byte) 0x00: // PULSE
                            if (value < 0) {
                                FrameLayout pulse_info_layout = (FrameLayout) findViewById(R.id.pulse_info_layout);
                                if (pulse_info_layout != null) {
                                    View pulse_state = inflate(R.layout.pulse_state);
                                    if (pulse_state != null) {
                                        pulse_info_layout.removeAllViews();
                                        pulse_info_layout.addView(pulse_state, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                                        TextView pulse_state_text = (TextView) findViewById(R.id.pulse_state_text);
                                        if (pulse_state_text != null) {
                                            switch (value) {
                                                case -2:
                                                    pulse_state_text.setText(R.string.calibration);
                                                    break;
                                                case -1:
                                                    pulse_state_text.setText(R.string.measuring);
                                                    break;
                                            }
                                        }
                                    }
                                }
                                pulseIsShowing = false;
                            } else {
                                if (!pulseIsShowing) {
                                    FrameLayout pulse_info_layout = (FrameLayout) findViewById(R.id.pulse_info_layout);
                                    if (pulse_info_layout != null) {
                                        View pulse_value = inflate(R.layout.pulse_value);
                                        if (pulse_value != null) {
                                            pulse_info_layout.removeAllViews();
                                            ((ViewGroup) findViewById(R.id.pulse_info_layout)).addView(pulse_value, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                                        }
                                    }
                                    pulseIsShowing = true;
                                }
                                TextView pulse_value_text = (TextView) findViewById(R.id.pulse_value);
                                if (pulse_value_text != null) {
                                    String text = value + getString(R.string.hits_per_min);
                                    pulse_value_text.setText(text);
                                }
                                TextView pulse_desc_text = (TextView) findViewById(R.id.pulse_desc);
                                if (pulse_desc_text != null) {
                                    if (value < 20) {
                                        pulse_desc_text.setText(R.string.pulse_so_low);
                                    } else if (value < 60) {
                                        pulse_desc_text.setText(R.string.rare);
                                    } else if (value > 90) {
                                        pulse_desc_text.setText(R.string.frequent);
                                    } else {
                                        pulse_desc_text.setText(R.string.moderate);
                                    }
                                }
                            }
                            break;
                        case (byte) 0x01:
                            if (graphView1 != null && graphView1.graphThread != null) {
                                graphView1.graphThread.incoming(value);
                            }
                            break;
                    }
                    break;
                }
            }
        }
    };

}
