package com.dh.ftp.sdk;

import android.util.Log;

public class CmdUSER extends FtpCmd {
    private String input;

    public CmdUSER(SessionThread sessionThread, String input) {
        super(sessionThread);
        this.input = input;
    }

    public void run() {
        Log.d(getClass().getSimpleName(), "USER executing");
        String username = FtpCmd.getParameter(this.input);
        if (username.matches("[A-Za-z0-9]+")) {
            this.sessionThread.writeString("331 Send password\r\n");
            this.sessionThread.account.setUsername(username);
            return;
        }
        this.sessionThread.writeString("530 Invalid username\r\n");
    }
}
