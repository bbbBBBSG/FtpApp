package com.dh.ftp.sdk;

import android.util.Log;

import java.io.File;

public class CmdMKD extends FtpCmd {
    private static final String TAG = CmdMKD.class.getSimpleName();
    private String input;

    public CmdMKD(SessionThread sessionThread, String input) {
        super(sessionThread);
        this.input = input;
    }

    public void run() {
        Log.d(TAG, "MKD executing");
        String param = FtpCmd.getParameter(this.input);
        String errString = null;
        if (param.length() < 1) {
            errString = "550 Invalid name\r\n";
        } else {
            File toCreate = FtpCmd.inputPathToChrootedFile(this.sessionThread.getWorkingDir(), param);
            if (violatesChroot(toCreate)) {
                errString = "550 Invalid name or chroot violation\r\n";
            } else if (toCreate.exists()) {
                errString = "550 Already exists\r\n";
            } else if (!toCreate.mkdir()) {
                errString = "550 Error making directory (permissions?)\r\n";
            }
        }
        if (errString != null) {
            this.sessionThread.writeString(errString);
            Log.i(TAG, "MKD error: " + errString.trim());
        } else {
            this.sessionThread.writeString("250 Directory created\r\n");
        }
        Log.i(TAG, "MKD complete");
    }
}
