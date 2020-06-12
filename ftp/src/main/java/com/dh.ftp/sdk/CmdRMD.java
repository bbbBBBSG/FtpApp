package com.dh.ftp.sdk;

import android.util.Log;

import java.io.File;

public class CmdRMD extends FtpCmd {
    private static final String TAG = CmdRMD.class.getSimpleName();
    public static final String message = "TEMPLATE!!";
    private String input;

    public CmdRMD(SessionThread sessionThread, String input) {
        super(sessionThread);
        this.input = input;
    }

    public void run() {
        Log.i(TAG, "RMD executing");
        String param = FtpCmd.getParameter(this.input);
        String errString = null;
        if (param.length() < 1) {
            errString = "550 Invalid argument\r\n";
        } else {
            File toRemove = FtpCmd.inputPathToChrootedFile(this.sessionThread.getWorkingDir(), param);
            if (violatesChroot(toRemove)) {
                errString = "550 Invalid name or chroot violation\r\n";
            } else if (!toRemove.isDirectory()) {
                errString = "550 Can't RMD a non-directory\r\n";
            } else if (toRemove.equals(new File("/"))) {
                errString = "550 Won't RMD the root directory\r\n";
            } else if (!recursiveDelete(toRemove)) {
                errString = "550 Deletion error, possibly incomplete\r\n";
            }
        }
        if (errString != null) {
            this.sessionThread.writeString(errString);
            Log.i(TAG, "RMD failed: " + errString.trim());
        } else {
            this.sessionThread.writeString("250 Removed directory\r\n");
        }
        Log.d(TAG, "RMD finished");
    }

    private boolean recursiveDelete(File toDelete) {
        if (!toDelete.exists()) {
            return false;
        }
        if (toDelete.isDirectory()) {
            boolean success = true;
            for (File entry : toDelete.listFiles()) {
                success &= recursiveDelete(entry);
            }
            Log.d(TAG, "Recursively deleted: " + toDelete);
            if (success && toDelete.delete()) {
                return true;
            }
            return false;
        }
        Log.d(TAG, "RMD deleting file: " + toDelete);
        return toDelete.delete();
    }
}
