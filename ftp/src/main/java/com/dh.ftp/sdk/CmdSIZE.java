package com.dh.ftp.sdk;

import android.util.Log;

import java.io.File;
import java.io.IOException;

public class CmdSIZE extends FtpCmd {
    private static final String TAG = CmdSIZE.class.getSimpleName();
    private String input;

    public CmdSIZE(SessionThread sessionThread, String input) {
        super(sessionThread);
        this.input = input;
    }

    public void run() {
        Log.d(TAG, "SIZE executing");
        String errString = null;
        String param = FtpCmd.getParameter(this.input);
        long size = 0;
        File currentDir = this.sessionThread.getWorkingDir();
        if (param.contains(File.separator)) {
            errString = "550 No directory traversal allowed in SIZE param\r\n";
        } else {
            File target = new File(currentDir, param);
            if (violatesChroot(target)) {
                errString = "550 SIZE target violates chroot\r\n";
            } else if (!target.exists()) {
                errString = "550 Cannot get the SIZE of nonexistent object\r\n";
                try {
                    Log.i(TAG, "Failed getting size of: " + target.getCanonicalPath());
                } catch (IOException e) {
                }
            } else if (target.isFile()) {
                size = target.length();
            } else {
                errString = "550 Cannot get the size of a non-file\r\n";
            }
        }
        if (errString != null) {
            this.sessionThread.writeString(errString);
        } else {
            this.sessionThread.writeString("213 " + size + "\r\n");
        }
        Log.d(TAG, "SIZE complete");
    }
}
