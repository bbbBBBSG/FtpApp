package com.dh.ftp.sdk;

import android.util.Log;

import java.io.File;
import java.io.IOException;

public class CmdPWD extends FtpCmd {
    private static final String TAG = CmdPWD.class.getSimpleName();

    public CmdPWD(SessionThread sessionThread) {
        super(sessionThread);
    }

    public void run() {
        Log.d(TAG, "PWD executing");
        try {
            File workingDir = this.sessionThread.getWorkingDir();
            String currentDir = (workingDir != null ? workingDir.getCanonicalPath() : Globals.getChrootDir().getCanonicalPath()).substring(Globals.getChrootDir().getCanonicalPath().length());
            if (currentDir.isEmpty()) {
                currentDir = "/";
            }
            this.sessionThread.writeString("257 \"" + currentDir + "\"\r\n");
        } catch (IOException e) {
            Log.e(TAG, "PWD canonicalize");
            this.sessionThread.closeSocket();
        }
        Log.d(TAG, "PWD complete");
    }
}
