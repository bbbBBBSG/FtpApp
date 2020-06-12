package com.dh.ftp.sdk;

import android.util.Log;

import java.io.File;

public class CmdRNTO extends FtpCmd {
    private static final String TAG = CmdRNTO.class.getSimpleName();
    private String input;

    public CmdRNTO(SessionThread sessionThread, String input) {
        super(sessionThread);
        this.input = input;
    }

    public void run() {
        String param = FtpCmd.getParameter(this.input);
        String errString = null;
        Log.d(TAG, "RNTO executing\r\n");
        Log.i(TAG, "param: " + param);
        File toFile = FtpCmd.inputPathToChrootedFile(this.sessionThread.getWorkingDir(), param);
        Log.i(TAG, "RNTO parsed: " + toFile.getPath());
        if (violatesChroot(toFile)) {
            errString = "550 Invalid name or chroot violation\r\n";
        } else {
            File fromFile = this.sessionThread.getRenameFrom();
            if (fromFile == null) {
                errString = "550 Rename error, maybe RNFR not sent\r\n";
            } else if (!fromFile.renameTo(toFile)) {
                errString = "550 Error during rename operation\r\n";
            }
        }
        if (errString != null) {
            this.sessionThread.writeString(errString);
            Log.i(TAG, "RNFR failed: " + errString.trim());
        } else {
            this.sessionThread.writeString("250 rename successful\r\n");
        }
        this.sessionThread.setRenameFrom(null);
        Log.d(TAG, "RNTO finished");
    }
}
