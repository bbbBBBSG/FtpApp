package com.dh.ftp.sdk;

import android.util.Log;

import java.io.File;

public class CmdDELE extends FtpCmd {
    private static final String TAG = CmdDELE.class.getSimpleName();
    private String input;

    public CmdDELE(SessionThread sessionThread, String input) {
        super(sessionThread);
        this.input = input;
    }

    public void run() {
        Log.i(TAG, "DELE executing");
        File storeFile = FtpCmd.inputPathToChrootedFile(this.sessionThread.getWorkingDir(), FtpCmd.getParameter(this.input));
        String errString = null;
        if (violatesChroot(storeFile)) {
            errString = "550 Invalid name or chroot violation\r\n";
        } else if (storeFile.isDirectory()) {
            errString = "550 Can't DELE a directory\r\n";
        } else if (!storeFile.delete()) {
            errString = "450 Error deleting file\r\n";
        }
        if (errString != null) {
            this.sessionThread.writeString(errString);
            Log.i(TAG, "DELE failed: " + errString.trim());
        } else {
            this.sessionThread.writeString("250 File successfully deleted\r\n");
            Util.deletedFileNotify(storeFile.getPath());
        }
        Log.i(TAG, "DELE finished");
    }
}
