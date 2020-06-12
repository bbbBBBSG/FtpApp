package com.dh.ftp.sdk;

import android.util.Log;

public class CmdTYPE extends FtpCmd {
    private static final String TAG = CmdTYPE.class.getSimpleName();
    private String input;

    public CmdTYPE(SessionThread sessionThread, String input) {
        super(sessionThread);
        this.input = input;
    }

    public void run() {
        String output;
        Log.d(TAG, "TYPE executing");
        String param = FtpCmd.getParameter(this.input);
        if ("I".equals(param) || "L 8".equals(param)) {
            output = "200 Binary type set\r\n";
            this.sessionThread.setBinaryMode(true);
        } else if ("A".equals(param) || "A N".equals(param)) {
            output = "200 ASCII type set\r\n";
            this.sessionThread.setBinaryMode(false);
        } else {
            output = "503 Malformed TYPE command\r\n";
        }
        this.sessionThread.writeString(output);
        Log.d(TAG, "TYPE complete");
    }
}
