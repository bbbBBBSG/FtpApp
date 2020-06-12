package com.dh.ftp.sdk;


import android.util.Log;

public class CmdSYST extends FtpCmd {
    private static final String TAG = CmdSYST.class.getSimpleName();
    private static final String response = "215 UNIX Type: L8\r\n";

    public CmdSYST(SessionThread sessionThread) {
        super(sessionThread);
    }

    public void run() {
        Log.i(TAG, "SYST executing");
        this.sessionThread.writeString(response);
        Log.i(TAG, "SYST finished");
    }
}
