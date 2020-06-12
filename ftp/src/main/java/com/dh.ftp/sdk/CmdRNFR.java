package com.dh.ftp.sdk;

import android.util.Log;

import java.io.File;

public class CmdRNFR extends FtpCmd {
    private String input;

    public CmdRNFR(SessionThread sessionThread, String input) {
        super(sessionThread);
        this.input = input;
    }

    public void run() {
        String errString = null;
        File file = FtpCmd.inputPathToChrootedFile(this.sessionThread.getWorkingDir(), FtpCmd.getParameter(this.input));
        if (violatesChroot(file)) {
            errString = "550 Invalid name or chroot violation\r\n";
        } else if (!file.exists()) {
            errString = "450 Cannot rename nonexistent file\r\n";
        }
        if (errString != null) {
            this.sessionThread.writeString(errString);
            Log.i(getClass().getSimpleName(), "RNFR failed: " + errString.trim());
            this.sessionThread.setRenameFrom(null);
            return;
        }
        this.sessionThread.writeString("350 Filename noted, now send RNTO\r\n");
        this.sessionThread.setRenameFrom(file);
    }
}
