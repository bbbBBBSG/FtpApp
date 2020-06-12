package com.dh.ftp.sdk;

import android.util.Log;

public class CmdQUIT extends FtpCmd {
    public static final String message = "TEMPLATE!!";

    public CmdQUIT(SessionThread sessionThread) {
        super(sessionThread);
    }

    public void run() {
        Log.d(getClass().getSimpleName(), "QUITting");
        this.sessionThread.writeString("221 Goodbye\r\n");
        this.sessionThread.closeSocket();
    }
}
