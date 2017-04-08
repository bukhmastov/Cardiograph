package com.bukhmastov.cardiograph.objects;

public class BtDevice {

    public String name = "";
    public String mac = "";
    public boolean isPaired = false;

    public BtDevice(String name, String mac, boolean isPaired){
        this.name = name;
        this.mac = mac;
        this.isPaired = isPaired;
    }

}
