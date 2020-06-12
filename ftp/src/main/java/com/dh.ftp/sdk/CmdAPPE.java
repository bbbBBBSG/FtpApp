package com.dh.ftp.sdk;

public class CmdAPPE extends CmdAbstractStore {
    private String input;

    public CmdAPPE(SessionThread sessionThread, String input) {
        super(sessionThread);
        this.input = input;
    }

    public void run() {
        doStorOrAppe(FtpCmd.getParameter(this.input), true);
    }
}
