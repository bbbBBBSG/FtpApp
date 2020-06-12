package com.dh.ftp.sdk;

import android.util.Log;

import java.io.File;
import java.io.IOException;

public class CmdCWD extends FtpCmd {
    private static final String TAG = CmdCWD.class.getSimpleName();
    private String input;

    public CmdCWD(SessionThread sessionThread, String input) {
        super(sessionThread);
        this.input = input;
    }

    public void run() {
        Log.d(TAG, "CWD executing");
        File newDir = FtpCmd.inputPathToChrootedFile(this.sessionThread.getWorkingDir(), FtpCmd.getParameter(this.input));
        if (violatesChroot(newDir)) {
            String errString = "550 Invalid name or chroot violation\r\n";
            this.sessionThread.writeString(errString);
            Log.i(TAG, errString);
        } else {
            try {
                newDir = newDir.getCanonicalFile();
                if (!newDir.isDirectory()) {
                    this.sessionThread.writeString("550 Can't CWD to invalid directory\r\n");
                } else if (newDir.canRead()) {
                    this.sessionThread.setWorkingDir(newDir);
                    this.sessionThread.writeString("250 CWD successful\r\n");
                } else {
                    this.sessionThread.writeString("550 That path is inaccessible\r\n");
                }
            } catch (IOException e) {
                this.sessionThread.writeString("550 Invalid path\r\n");
            }
        }
        Log.d(TAG, "CWD complete");
    }
}
