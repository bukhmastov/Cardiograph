package com.bukhmastov.cardiograph.adapters;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.bukhmastov.cardiograph.objects.BtDevice;
import com.bukhmastov.cardiograph.R;

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
        if (!Objects.equals(btDevice.name, "")) {
            view = inflater.inflate(R.layout.listview_item_device_name_mac, null, true);
            ((TextView) view.findViewById(R.id.deviceName)).setText(btDevice.name);
            ((TextView) view.findViewById(R.id.deviceMacAddress)).setText(btDevice.mac);
        } else {
            view = inflater.inflate(R.layout.listview_item_device_mac, null, true);
            ((TextView) view.findViewById(R.id.deviceMacAddress)).setText(btDevice.mac);
        }
        return view;
    }

}
