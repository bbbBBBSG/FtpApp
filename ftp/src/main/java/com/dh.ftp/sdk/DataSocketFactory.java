package com.dh.ftp.sdk;

import java.net.InetAddress;
import java.net.Socket;

abstract class DataSocketFactory {
    public abstract int onPasv();

    public abstract boolean onPort(InetAddress inetAddress, int i);

    public abstract Socket onTransfer();

    public abstract void reportTraffic(long j);

    DataSocketFactory() {
    }
}
