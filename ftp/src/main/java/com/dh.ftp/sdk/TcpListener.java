package com.dh.ftp.sdk;

import android.util.Log;

import com.dh.ftp.FTPServerService;

import java.net.ServerSocket;
import java.net.Socket;

public class TcpListener extends Thread {
    private static final String TAG = TcpListener.class.getSimpleName();
    private FTPServerService ftpServerService;
    private ServerSocket listenSocket;

    public TcpListener(ServerSocket listenSocket, FTPServerService ftpServerService) {
        this.listenSocket = listenSocket;
        this.ftpServerService = ftpServerService;
    }

    public void quit() {
        AutoClose.closeQuietly(this.listenSocket);
    }

    public void run() {
        while (true) {
            try {
                Socket clientSocket = this.listenSocket.accept();
                Log.i(TAG, "New connection, spawned thread");
                SessionThread newSession = new SessionThread(clientSocket, new NormalDataSocketFactory(), SessionThread.Source.LOCAL);
                newSession.start();
                this.ftpServerService.registerSessionThread(newSession);
            } catch (Exception e) {
                Log.d(TAG, "Exception in TcpListener");
                return;
            }
        }
    }
}
