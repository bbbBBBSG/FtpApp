package com.dh.ftp.sdk;

import android.content.Context;

import java.io.File;

public class Globals {
    private static File chrootDir = new File(Defaults.chrootDir);
    private static String lastError;
    private static ProxyConnector proxyConnector;

    static ProxyConnector getProxyConnector() {
        if (proxyConnector == null || proxyConnector.isAlive()) {
            return proxyConnector;
        }
        return null;
    }

    static void setProxyConnector(ProxyConnector proxyConnector) {
        Globals.proxyConnector = proxyConnector;
    }

    static File getChrootDir() {
        return chrootDir;
    }

    public static void setChrootDir(File chrootDir) {
        if (chrootDir.isDirectory()) {
            Globals.chrootDir = chrootDir;
        }
    }

    public static String getLastError() {
        return lastError;
    }

    public static void setLastError(String lastError) {
        Globals.lastError = lastError;
    }

    public static Context getContext() {
        return ContextUtil.getContext();
    }
}
