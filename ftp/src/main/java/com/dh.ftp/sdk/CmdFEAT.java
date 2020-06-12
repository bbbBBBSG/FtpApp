package com.dh.ftp.sdk;


import android.util.Log;

public class CmdFEAT extends FtpCmd {
    public static final String message = "TEMPLATE!!";

    public CmdFEAT(SessionThread sessionThread) {
        super(sessionThread);
    }

    public void run() {
        this.sessionThread.writeString("211-Features supported\r\n");
        this.sessionThread.writeString(" ");
        this.sessionThread.writeString(this.sessionThread.encoding);
        this.sessionThread.writeString("\r\n");
        this.sessionThread.writeString("211 End\r\n");
        Log.d(getClass().getSimpleName(), "Gave FEAT response");
    }
}
