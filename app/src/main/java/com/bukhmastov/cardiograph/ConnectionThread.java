package com.bukhmastov.cardiograph;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ConnectionThread extends Thread {
    private final static String TAG = "ConnectionThread";
    private BluetoothDevice device;
    private BluetoothSocket mmSocket;
    private InputStream mmInStream;
    private OutputStream mmOutStream;
    private Handler handler;
    private final int BYTES_PER_FRAME = 3;
    public static int MESSAGE_CONNECTION = 0;
    public static int MESSAGE_DISCONNECTION = 1;
    public static int MESSAGE = 2;

    ConnectionThread(BluetoothDevice device, Handler handler) {
        this.device = device;
        this.handler = handler;
    }
    public void run(){
        byte[] buffer = new byte[0];
        byte[] incoming;
        byte[] tmp;
        boolean isConnected = false;
        int resetBytesCount = 0;
        if (this.connect()) {
            state(MESSAGE_CONNECTION, "connected_handshaking");
            long handshakeTimeStart = System.currentTimeMillis();
            this.write((byte)0xff);
            this.flush();
            while(!Thread.currentThread().isInterrupted()) {
                try {
                    if(!isConnected) { // handshake in progress
                        if(mmInStream.available() > 0) {
                            if ((byte) mmInStream.read() == (byte) 0xee) {
                                resetBytesCount++;
                                if (resetBytesCount > BYTES_PER_FRAME) { // handshake done
                                    isConnected = true;
                                    resetBytesCount = 0;
                                    state(MESSAGE_CONNECTION, "connected_and_ready");
                                }
                            } else {
                                resetBytesCount = 0;
                            }
                        }
                        if(!isConnected && System.currentTimeMillis() - handshakeTimeStart > 2000) {
                            state(MESSAGE_CONNECTION, "connected_handshake_failed");
                            this.cancel();
                        }
                        continue;
                    }
                    if(mmInStream.available() > 0){
                        incoming = new byte[mmInStream.available()];
                        mmInStream.read(incoming);
                        tmp = new byte[buffer.length + incoming.length];
                        System.arraycopy(buffer, 0, tmp, 0, buffer.length);
                        System.arraycopy(incoming, 0, tmp, buffer.length, incoming.length);
                        buffer = tmp;
                        for(int i = 0; i < (buffer.length - buffer.length % BYTES_PER_FRAME); i += BYTES_PER_FRAME) {
                            //Log.d(TAG, "mmInStream | new bunch | " + buffer[i] + " " + buffer[i + 1] + " " + buffer[i + 2]);
                            byte[] pair = {buffer[i + 1], buffer[i + 2]};
                            int message = (int) ByteBuffer.wrap(pair).order(ByteOrder.LITTLE_ENDIAN).getShort();
                            //Log.d(TAG, "mmInStream | type " + ((int) buffer[i]) + " | message " + message);
                            Message m = new Message();
                            Bundle b = new Bundle();
                            b.putInt("what", MESSAGE);
                            b.putByte("type", buffer[i]);
                            b.putInt("value", message);
                            m.setData(b);
                            handler.sendMessage(m);
                        }
                        tmp = new byte[buffer.length % BYTES_PER_FRAME];
                        System.arraycopy(buffer, (buffer.length - buffer.length % BYTES_PER_FRAME), tmp, 0, buffer.length % BYTES_PER_FRAME);
                        buffer = tmp;
                    } else {
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    break;
                }
            }
            if (!Thread.currentThread().isInterrupted()) this.cancel();
        }
    }
    public void cancel() {
        this.disconnect();
        interrupt();
    }
    private boolean connect(){
        state(MESSAGE_CONNECTION, "starting");
        try {
            try {
                mmSocket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(device, 1); // mmSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (Exception e) {
                throw new Exception("failed");
            }
            try {
                mmSocket.connect();
            } catch (IOException connectException) {
                throw new Exception("failed");
            }
            try {
                mmInStream = mmSocket.getInputStream();
                mmOutStream = mmSocket.getOutputStream();
            } catch (IOException e) {
                throw new Exception("failed");
            }
        } catch (Exception e) {
            state(MESSAGE_CONNECTION, e.getMessage());
            return false;
        }
        state(MESSAGE_CONNECTION, "connected");
        return true;
    }
    private boolean disconnect(){
        boolean showState = mmSocket.isConnected();
        try {
            mmSocket.close();
        } catch (Exception e) {
            if(showState) state(MESSAGE_DISCONNECTION, "failed");
            return false;
        }
        if(showState) state(MESSAGE_DISCONNECTION, "disconnected");
        return true;
    }
    private void write(byte data) {
        if(mmSocket.isConnected()) {
            try {
                mmOutStream.write(data);
            } catch (IOException e) {
                Log.d(TAG, "EXCEPTION write IOException | " + e.getMessage());
            }
        }
    }
    private void flush() {
        if(mmSocket.isConnected()) {
            try {
                mmOutStream.flush();
            } catch (IOException e) {
                Log.d(TAG, "EXCEPTION flush IOException | " + e.getMessage());
            }
        }
    }
    private void state(int what, String state){
        Message m = new Message();
        Bundle b = new Bundle();
        b.putInt("what", what);
        b.putString("state", state);
        m.setData(b);
        handler.sendMessage(m);
    }
}
