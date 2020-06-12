package com.dh.ftp.sdk;

public class CmdSTOR extends CmdAbstractStore {
    private String input;

    public CmdSTOR(SessionThread sessionThread, String input) {
        super(sessionThread);
        this.input = input;
    }

    public void run() {
        doStorOrAppe(FtpCmd.getParameter(this.input), false);
    }
}
