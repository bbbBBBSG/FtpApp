package com.dh.ftp.sdk;

import android.support.annotation.Nullable;

import java.io.Closeable;

public class AutoClose {
    public static void closeQuietly(@Nullable Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
