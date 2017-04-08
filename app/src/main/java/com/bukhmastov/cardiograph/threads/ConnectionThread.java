package com.bukhmastov.cardiograph.threads;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.bukhmastov.cardiograph.utils.Storage;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ConnectionThread extends Thread {

    private final static String TAG = "ConnectionThread";
    private Context context;
    private BluetoothDevice device;
    private BluetoothSocket mmSocket;
    private InputStream mmInStream;
    private OutputStream mmOutStream;
    private Handler handler;
    private final int FRAME_RATE;
    private final int AVERAGE_TOLERANCE;
    private final int MAX_TOLERANCE;
    private final int BYTES_PER_FRAME = 3;
    public static final int MESSAGE_CONNECTION = 0;
    public static final int MESSAGE_DISCONNECTION = 1;
    public static final int MESSAGE = 2;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());
    private boolean writeToFile = false;
    private FileOutputStream fileOutputStream = null;

    public ConnectionThread(BluetoothDevice device, Context context, Handler handler) {
        this.context = context;
        this.device = device;
        this.handler = handler;
        this.writeToFile = Storage.pref.get(context, "pref_use_archiving", true);
        FRAME_RATE = Integer.parseInt(Storage.pref.get(context, "frame_rate", "40"));
        AVERAGE_TOLERANCE = Integer.parseInt(Storage.pref.get(context, "pref_arduino_average_tolerance", "20"));
        MAX_TOLERANCE = Integer.parseInt(Storage.pref.get(context, "pref_arduino_max_tolerance", "10"));
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
            this.write((byte)FRAME_RATE);
            this.write((byte)BYTES_PER_FRAME);
            this.write((byte)AVERAGE_TOLERANCE);
            this.write((byte)MAX_TOLERANCE);
            this.flush();
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (!isConnected) { // handshake in progress
                        if (mmInStream.available() > 0) {
                            if ((byte) mmInStream.read() == (byte) 0xee) {
                                resetBytesCount++;
                                if (resetBytesCount > BYTES_PER_FRAME) { // handshake done
                                    isConnected = true;
                                    resetBytesCount = 0;
                                    if (writeToFile) {
                                        fileOutputStream = Storage.file.openWriteStream(context, "archive#Record" + " " + dateFormat.format(new Date()));
                                        writeToFile = fileOutputStream != null;
                                    }
                                    state(MESSAGE_CONNECTION, "connected_and_ready");
                                }
                            } else {
                                resetBytesCount = 0;
                            }
                        }
                        if (!isConnected && System.currentTimeMillis() - handshakeTimeStart > 2000) {
                            state(MESSAGE_CONNECTION, "connected_handshake_failed");
                            this.cancel();
                        }
                        continue;
                    }
                    if (mmInStream.available() > 0) {
                        incoming = new byte[mmInStream.available()];
                        mmInStream.read(incoming);
                        tmp = new byte[buffer.length + incoming.length];
                        System.arraycopy(buffer, 0, tmp, 0, buffer.length);
                        System.arraycopy(incoming, 0, tmp, buffer.length, incoming.length);
                        buffer = tmp;
                        for (int i = 0; i < (buffer.length - buffer.length % BYTES_PER_FRAME); i += BYTES_PER_FRAME) {
                            for (int j = 0; j < BYTES_PER_FRAME; j++) {
                                if (writeToFile) writeToFile = Storage.file.writeToStream(fileOutputStream, buffer[i + j]);
                            }
                            byte[] pair = {buffer[i + 1], buffer[i + 2]};
                            int message = (int) ByteBuffer.wrap(pair).order(ByteOrder.LITTLE_ENDIAN).getShort();
                            //Log.d(TAG, "type " + ((int) buffer[i]) + " | " + message);
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
                        for (int j = 0; j < BYTES_PER_FRAME; j++) {
                            if (writeToFile) writeToFile = Storage.file.writeToStream(fileOutputStream, (byte) 0xef);
                        }
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
        if (writeToFile && fileOutputStream != null) {
            Storage.file.closeWriteStream(fileOutputStream);
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
            if (showState) state(MESSAGE_DISCONNECTION, "failed");
            return false;
        }
        if (showState) state(MESSAGE_DISCONNECTION, "disconnected");
        return true;
    }
    private void write(byte data) {
        if (mmSocket.isConnected()) {
            try {
                mmOutStream.write(data);
            } catch (IOException e) {
                Log.d(TAG, "EXCEPTION write IOException | " + e.getMessage());
            }
        }
    }
    private void flush() {
        if (mmSocket.isConnected()) {
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
    public void pulseCalibrate(){
        this.write((byte)0xee);
    }

}
