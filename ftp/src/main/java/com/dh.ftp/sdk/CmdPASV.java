package com.dh.ftp.sdk;

import android.util.Log;

import java.net.InetAddress;

public class CmdPASV extends FtpCmd {
    private static final String TAG = CmdPASV.class.getSimpleName();

    public CmdPASV(SessionThread sessionThread) {
        super(sessionThread);
    }

    public void run() {
        String cantOpen = "502 Couldn't open a port\r\n";
        Log.d(TAG, "PASV running");
        int port = this.sessionThread.onPasv();
        if (port == 0) {
            Log.e(TAG, "Couldn't open a port for PASV");
            this.sessionThread.writeString(cantOpen);
            return;
        }
        InetAddress addr = this.sessionThread.getDataSocketPasvIp();
        if (addr == null) {
            Log.e(TAG, "PASV IP string invalid");
            this.sessionThread.writeString(cantOpen);
            return;
        }
        Log.d(TAG, "PASV sending IP: " + addr.getHostAddress());
        if (port < 1) {
            Log.e(TAG, "PASV port number invalid");
            this.sessionThread.writeString(cantOpen);
            return;
        }
        String responseString = "227 Entering Passive Mode (" + addr.getHostAddress().replace('.', ',') + "," + (port / 256) + "," + (port % 256) + ").\r\n";
        this.sessionThread.writeString(responseString);
        Log.d(TAG, "PASV completed, sent: " + responseString);
    }
}
