package com.dh.ftp.sdk;

import android.util.Log;

import java.io.File;
import java.io.IOException;

public class CmdCDUP extends FtpCmd {
    private String TAG = CmdCDUP.class.getSimpleName();
    protected String input;

    public CmdCDUP(SessionThread sessionThread) {
        super(sessionThread);
    }

    public void run() {
        Log.d(this.TAG, "CDUP executing");
        String errString = null;
        File newDir = this.sessionThread.getWorkingDir().getParentFile();
        if (newDir == null) {
            errString = "550 Current dir cannot find parent\r\n";
        } else if (violatesChroot(newDir)) {
            errString = "550 Invalid name or chroot violation\r\n";
        } else {
            try {
                newDir = newDir.getCanonicalFile();
                if (!newDir.isDirectory()) {
                    errString = "550 Can't CWD to invalid directory\r\n";
                } else if (newDir.canRead()) {
                    this.sessionThread.setWorkingDir(newDir);
                } else {
                    errString = "550 That path is inaccessible\r\n";
                }
            } catch (IOException e) {
                errString = "550 Invalid path\r\n";
            }
        }
        if (errString != null) {
            this.sessionThread.writeString(errString);
            Log.i(this.TAG, "CDUP error: " + errString);
            return;
        }
        this.sessionThread.writeString("200 CDUP successful\r\n");
        Log.d(this.TAG, "CDUP success");
    }
}
