package com.bukhmastov.cardiograph;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Objects;

public class DeviceListView extends ArrayAdapter<BtDevice> {
    private final Activity context;
    private final ArrayList<BtDevice> btDevices;
    public DeviceListView(Activity context, ArrayList<BtDevice> btDevices) {
        super(context, R.layout.listview_item_device_name_mac, btDevices);
        this.context = context;
        this.btDevices = btDevices;
    }
    @NonNull
    @Override
    public View getView(int position, View view, @NonNull ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        BtDevice btDevice = btDevices.get(position);
        View rowView;
        if(!Objects.equals(btDevice.name, "")){
            rowView = inflater.inflate(R.layout.listview_item_device_name_mac, null, true);
            ((TextView) rowView.findViewById(R.id.deviceName)).setText(btDevice.name);
            ((TextView) rowView.findViewById(R.id.deviceMacAddress)).setText(btDevice.mac);
        } else {
            rowView = inflater.inflate(R.layout.listview_item_device_mac, null, true);
            ((TextView) rowView.findViewById(R.id.deviceMacAddress)).setText(btDevice.mac);
        }
        return rowView;
    }
}
