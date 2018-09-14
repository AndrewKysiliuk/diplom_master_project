package com.example.dipapprev2;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DataTransfer extends Thread {
    private static final int RECIEVE_MESSAGE = 1;
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private Handler handler;

    public DataTransfer(BluetoothSocket socket, Handler handler) {
        mmSocket = socket;
        this.handler = handler;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {}

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    public void run() {
        byte[] buffer = new byte[256];
        int bytes;

        while (true) {
            try {
                bytes = mmInStream.read(buffer);
                handler.obtainMessage(RECIEVE_MESSAGE, bytes, -1, buffer).sendToTarget();
            } catch (IOException e) {
                break;
            }
        }
    }

    public void sendMessage(String message) {
        byte[] msgBuffer = message.getBytes();
        try {
            mmOutStream.write(msgBuffer);
        } catch (IOException e) {
        }
    }

    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
        }
    }
}