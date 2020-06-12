package com.dh.ftp.sdk;

import android.os.Environment;

import java.lang.reflect.InvocationTargetException;

public class Defaults {
    public static final String BIG5_ENCODING = "big5";
    public static final int DEFAULT_PORT_NUMBER = 8888;
    public static final String GBK_ENCODING = "GBK";
    public static final int REMOTE_PROXY_PORT = 2222;
    public static final String SESSION_ENCODING = (isInternationalBuild() ? BIG5_ENCODING : GBK_ENCODING);
    public static final String UTF8_ENCODING = "UTF-8";
    public static final String chrootDir = Environment.getExternalStorageDirectory().getPath();
    static int dataChunkSize = 65536;
    private static int inputBufferSize = 256;
    public static int portNumber = DEFAULT_PORT_NUMBER;

    private static boolean isInternationalBuild() {
        String build = "";
        try {
            Class<?> systemPropertiesClass = Class.forName("android.os.SystemProperties");
            build = (String) systemPropertiesClass.getMethod("get", new Class[]{String.class, String.class}).invoke(systemPropertiesClass, new Object[]{"ro.product.mod_device", ""});
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return "aries_tw".equals(build);
    }

    public static int getInputBufferSize() {
        return inputBufferSize;
    }

    public static int getDataChunkSize() {
        return dataChunkSize;
    }
}
