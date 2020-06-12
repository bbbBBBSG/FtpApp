package com.dahai.ftpapp;

import android.app.Application;
import android.content.Context;

/**
 * 作者： 大海
 * 时间： 2018/11/16
 * 描述：
 */
public class MyApp extends Application {

    private static Context mContext;
    public static Context getInstance() {
        return mContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;

    }
}
