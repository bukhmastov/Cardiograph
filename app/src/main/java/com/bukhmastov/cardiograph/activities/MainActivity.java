package com.bukhmastov.cardiograph.activities;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.bukhmastov.cardiograph.objects.BtDevice;
import com.bukhmastov.cardiograph.adapters.DeviceListView;
import com.bukhmastov.cardiograph.R;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    public static int theme;
    private BluetoothAdapter btAdapter;
    private ArrayList<BtDevice> btDevices = new ArrayList<>();
    private DeviceListView foundedDevicesAdapter;
    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.lobby_toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setLogo(R.mipmap.ic_launcher_simple);
        }

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            Toast.makeText(this, R.string.bt_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (btAdapter.isDiscovering()) btAdapter.cancelDiscovery();

        foundedDevicesAdapter = new DeviceListView(this, btDevices);
        ListView devices_view = (ListView) findViewById(R.id.devices_view);
        if (devices_view != null) {
            devices_view.setAdapter(foundedDevicesAdapter);
            devices_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    connect(btDevices.get(position).mac);
                }
            });
        }

        Button bt_enable_btn = (Button) findViewById(R.id.bt_enable_btn);
        if (bt_enable_btn != null) {
            bt_enable_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 0);
                }
            });
        }

        Button quick_connect_btn = (Button) findViewById(R.id.quick_connect_btn);
        if (quick_connect_btn != null) {
            quick_connect_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        connect(((EditText) findViewById(R.id.quick_connect_input)).getText().toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("storage_disclaimer", true)) {
            startActivity(new Intent(this, DisclaimerActivity.class));
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(bluetoothState, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        updateUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (btAdapter.isDiscovering()) {
            btAdapter.cancelDiscovery();
        }
        try {
            unregisterReceiver(bluetoothState);
        } catch (Exception e){
            Log.w(TAG, "Failed to unregisterReceiver bluetoothState");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.lobby_toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_toggle_search:
                if (btAdapter.isEnabled()) {
                    if (btAdapter.isDiscovering()) {
                        btAdapter.cancelDiscovery();
                    } else {
                        findDevices();
                    }
                } else {
                    startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 0);
                }
                return true;
            case R.id.action_archive:
                startActivity(new Intent(this, ArchiveActivity.class));
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    private void updateUI(){
        try {
            if (btAdapter.isEnabled()) {
                findViewById(R.id.btOffLayout).setVisibility(View.GONE);
                findViewById(R.id.btSelectConnectionLayout).setVisibility(View.VISIBLE);
                resetDeviceList();
                displayPairedDevices();
            } else {
                findViewById(R.id.btOffLayout).setVisibility(View.VISIBLE);
                findViewById(R.id.btSelectConnectionLayout).setVisibility(View.GONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void findDevices(){
        if (btAdapter == null) return;
        if (!btAdapter.isEnabled()) return;
        if (btAdapter.isDiscovering()) return;
        resetDeviceList();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION") + this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
        }
        try {
            unregisterReceiver(mReceiver);
        } catch (Exception e) {
            Log.w(TAG, "Failed to unregisterReceiver mReceiver");
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        btAdapter.startDiscovery();
    }
    private void resetDeviceList(){
        btDevices.clear();
        foundedDevicesAdapter.notifyDataSetChanged();
    }
    private void displayPairedDevices(){
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice pairedDevice : pairedDevices) addToDeviceList(pairedDevice.getName(), pairedDevice.getAddress(), true);
        }
    }
    private void addToDeviceList(String name, String address, Boolean isPaired){
        addToDeviceList(new BtDevice(name, address, isPaired));
    }
    private void addToDeviceList(BtDevice btDevice){
        btDevices.add(btDevice);
        foundedDevicesAdapter.notifyDataSetChanged();
    }
    private void connect(String mac){
        if (mac.isEmpty()) return;
        if (btAdapter.isDiscovering()) btAdapter.cancelDiscovery();
        Intent intent = new Intent(this, ConnectionActivity.class);
        intent.putExtra("mac", mac);
        startActivity(intent);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    if (menu != null) {
                        MenuItem action_toggle_search = menu.findItem(R.id.action_toggle_search);
                        if (action_toggle_search != null) {
                            action_toggle_search.setIcon(R.drawable.ic_bluetooth_connected);
                        }
                    }
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    if (menu != null) {
                        MenuItem action_toggle_search = menu.findItem(R.id.action_toggle_search);
                        if (action_toggle_search != null) {
                            action_toggle_search.setIcon(R.drawable.ic_bluetooth_search);
                        }
                    }
                    try {
                        unregisterReceiver(mReceiver);
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to unregisterReceiver mReceiver");
                    }
                    break;
                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device != null) {
                        Boolean ok = true;
                        for (BtDevice btDevice : btDevices) {
                            if (Objects.equals(btDevice.mac, device.getAddress())) {
                                ok = false;
                                break;
                            }
                        }
                        if (ok) {
                            addToDeviceList(new BtDevice(device.getName() == null ? "" : device.getName(), device.getAddress(), false));
                        }
                    }
                    break;
            }
        }
    };
    private BroadcastReceiver bluetoothState = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)) {
                case (BluetoothAdapter.STATE_ON) :
                case (BluetoothAdapter.STATE_OFF) : {
                    updateUI();
                    break;
                }
            }
        }
    };

}
