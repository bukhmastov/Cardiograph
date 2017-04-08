package com.bukhmastov.cardiograph.activities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
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

import com.bukhmastov.cardiograph.threads.ConnectionThread;
import com.bukhmastov.cardiograph.views.GraphView;
import com.bukhmastov.cardiograph.R;

public class ConnectionActivity extends AppCompatActivity {

    private BluetoothAdapter btAdapter;
    private String remoteDeviceMacAddress;
    private String remoteDeviceName;
    public ConnectionThread connectionThread;

    public LinearLayout container;
    public static GraphView graphView1 = null;

    private boolean pulseIsShowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);
        setSupportActionBar((Toolbar) findViewById(R.id.connection_toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.app_name);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null || !btAdapter.isEnabled()) {
            finish();
            return;
        }
        remoteDeviceMacAddress = getIntent().getStringExtra("mac");
        if (remoteDeviceMacAddress == null || remoteDeviceMacAddress.isEmpty() || !BluetoothAdapter.checkBluetoothAddress(remoteDeviceMacAddress)) {
            finish();
            return;
        }
        container = (LinearLayout) findViewById(R.id.container);
    }

    @Override
    protected void onResume() {
        super.onResume();
        connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (connectionThread != null) {
            connectionThread.cancel();
            connectionThread = null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: finish(); return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    private void connect(){
        container.removeAllViews();
        ((ViewGroup) findViewById(R.id.container)).addView((((LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.connection_state, null)), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        BluetoothDevice btDevice = btAdapter.getRemoteDevice(remoteDeviceMacAddress);
        remoteDeviceName = btDevice.getName();
        connectionThread = new ConnectionThread(btDevice, getBaseContext(), handler);
        connectionThread.start();
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

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.getData().getInt("what")) {
                case ConnectionThread.MESSAGE_CONNECTION: {
                    String state = msg.getData().getString("state");
                    assert state != null;
                    switch (state) {
                        case "connected_and_ready": {
                            draw(R.layout.connected_interface);
                            LinearLayout pulse_layout = (LinearLayout) findViewById(R.id.pulse_layout);
                            if (pulse_layout != null) {
                                pulse_layout.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        if (connectionThread != null) {
                                            connectionThread.pulseCalibrate();
                                        }
                                    }
                                });
                            }
                            break;
                        }
                        case "connected_handshake_failed": {
                            Snackbar.make(findViewById(R.id.container), getString(R.string.sync_failed), Snackbar.LENGTH_INDEFINITE).setAction(R.string.repeat, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    connect();
                                }
                            }).show();
                            break;
                        }
                        default: {
                            draw(R.layout.connection_state);
                            switch (state) {
                                case "starting":
                                    state = "Подключение к устройству" + "\n" + remoteDeviceName + " (" + remoteDeviceMacAddress + ")";
                                    break;
                                case "connected":
                                    state = "Соединен с устройством" + "\n"  + remoteDeviceName + " (" + remoteDeviceMacAddress + ")";
                                    break;
                                case "connected_handshaking":
                                    state = "Синхронизация с устройством";
                                    break;
                                case "failed":
                                    LinearLayout connection_image = (LinearLayout) findViewById(R.id.connection_image);
                                    if (connection_image != null) {
                                        connection_image.removeAllViews();
                                        ImageView gg = new ImageView(getBaseContext());
                                        gg.setImageDrawable(getDrawable(R.drawable.ic_warning));
                                        connection_image.addView(gg);
                                    }
                                    state = "Не удалось подключиться к устройству" + "\n"  + remoteDeviceName + " (" + remoteDeviceMacAddress + ")";
                                    break;
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
                case ConnectionThread.MESSAGE_DISCONNECTION: {
                    String state = msg.getData().getString("state");
                    assert state != null;
                    draw(R.layout.connection_state);
                    switch(state){
                        case "disconnected":
                            state = "Соединение разорвано";
                            break;
                        case "failed":
                            state = "Произошла ошибка во время разрыва соединения";
                            break;
                    }
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
                case ConnectionThread.MESSAGE: {
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
                        case (byte) 0x01: // 1 graph
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
