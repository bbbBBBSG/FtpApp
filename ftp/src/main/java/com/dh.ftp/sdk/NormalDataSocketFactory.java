package com.dh.ftp.sdk;

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class NormalDataSocketFactory extends DataSocketFactory {
    private static final String TAG = NormalDataSocketFactory.class.getSimpleName();
    private InetAddress remoteAddr;
    private int remotePort;
    private ServerSocket server;

    public NormalDataSocketFactory() {
        clearState();
    }

    private void clearState() {
        AutoClose.closeQuietly(this.server);
        this.server = null;
        this.remoteAddr = null;
        this.remotePort = 0;
        Log.d(TAG, "NormalDataSocketFactory state cleared");
    }

    public int onPasv() {
        int i = 0;
        clearState();
        try {
            this.server = new ServerSocket(0, 5);
            Log.d(TAG, "Data socket pasv() listen successful");
            return this.server.getLocalPort();
        } catch (IOException e) {
            Log.e(TAG, "Data socket creation error");
            clearState();
            return i;
        }
    }

    public boolean onPort(InetAddress remoteAddr, int remotePort) {
        clearState();
        this.remoteAddr = remoteAddr;
        this.remotePort = remotePort;
        return true;
    }

    public Socket onTransfer() {
        Socket socket;
        if (this.server != null) {
            try {
                socket = this.server.accept();
                Log.d(TAG, "onTransfer pasv accept successful");
            } catch (Exception e) {
                Log.i(TAG, "Exception accepting PASV socket");
                socket = null;
            }
            clearState();
            return socket;
        } else if (this.remoteAddr == null || this.remotePort == 0) {
            Log.i(TAG, "PORT mode but not initialized correctly");
            clearState();
            return null;
        } else {
            try {
                socket = new Socket(this.remoteAddr, this.remotePort);
                try {
                    socket.setSoTimeout(30000);
                    return socket;
                } catch (Exception e2) {
                    Log.e(TAG, "Couldn't set SO_TIMEOUT");
                    clearState();
                    return null;
                }
            } catch (IOException e3) {
                Log.i(TAG, "Couldn't open PORT data socket to: " + this.remoteAddr + ":" + this.remotePort);
                clearState();
                return null;
            }
        }
    }

    public void reportTraffic(long bytes) {
    }
}
