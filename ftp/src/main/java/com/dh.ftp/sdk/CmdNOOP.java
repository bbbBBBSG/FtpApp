package com.dh.ftp.sdk;

public class CmdNOOP extends FtpCmd {
    public static final String message = "TEMPLATE!!";

    public CmdNOOP(SessionThread sessionThread) {
        super(sessionThread);
    }

    public void run() {
        this.sessionThread.writeString("200 NOOP ok\r\n");
    }
}
