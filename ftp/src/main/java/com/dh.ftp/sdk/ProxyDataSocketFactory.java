package com.dh.ftp.sdk;

import android.util.Log;

import java.net.InetAddress;
import java.net.Socket;

class ProxyDataSocketFactory extends DataSocketFactory {
    private static final String TAG = ProxyDataSocketFactory.class.getSimpleName();
    private InetAddress clientAddress;
    private int clientPort;
    private ProxyConnector proxyConnector;
    private int proxyListenPort;
    private Socket socket;

    ProxyDataSocketFactory() {
        clearState();
    }

    private void clearState() {
        AutoClose.closeQuietly(this.socket);
        this.socket = null;
        this.proxyConnector = null;
        this.clientAddress = null;
        this.proxyListenPort = 0;
        this.clientPort = 0;
    }

    public int onPasv() {
        clearState();
        this.proxyConnector = Globals.getProxyConnector();
        if (this.proxyConnector == null) {
            Log.i(TAG, "Unexpected null proxyConnector in onPasv");
            clearState();
            return 0;
        }
        ProxyDataSocketInfo info = this.proxyConnector.pasvListen();
        if (info == null) {
            Log.i(TAG, "Null ProxyDataSocketInfo");
            clearState();
            return 0;
        }
        this.socket = info.getSocket();
        this.proxyListenPort = info.getRemotePublicPort();
        AutoClose.closeQuietly(info);
        return this.proxyListenPort;
    }

    public boolean onPort(InetAddress dest, int port) {
        clearState();
        this.proxyConnector = Globals.getProxyConnector();
        this.clientAddress = dest;
        this.clientPort = port;
        Log.d(TAG, "ProxyDataSocketFactory client port settings stored");
        return true;
    }

    public Socket onTransfer() {
        if (this.proxyConnector == null) {
            Log.w(TAG, "Unexpected null proxyConnector in onTransfer");
            return null;
        } else if (this.socket == null) {
            this.socket = this.proxyConnector.dataPortConnect(this.clientAddress, this.clientPort);
            return this.socket;
        } else if (this.proxyConnector.pasvAccept(this.socket)) {
            return this.socket;
        } else {
            Log.w(TAG, "proxyConnector pasvAccept failed");
            return null;
        }
    }

    public void reportTraffic(long bytes) {
        ProxyConnector pc = Globals.getProxyConnector();
        if (pc == null) {
            Log.d(TAG, "Can't report traffic, null ProxyConnector");
        } else {
            pc.incrementProxyUsage(bytes);
        }
    }
}
