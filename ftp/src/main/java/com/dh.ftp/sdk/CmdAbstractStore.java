package com.dh.ftp.sdk;

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

abstract class CmdAbstractStore extends FtpCmd {
    private static final String TAG = CmdAbstractStore.class.getSimpleName();
    public static final String message = "TEMPLATE!!";

    CmdAbstractStore(SessionThread sessionThread) {
        super(sessionThread);
    }

    void doStorOrAppe(String param, boolean append) {
        Log.d(TAG, "STOR/APPE executing with append=" + append);
        File storeFile = FtpCmd.inputPathToChrootedFile(this.sessionThread.getWorkingDir(), param);
        String errString = null;
        FileOutputStream out = null;
        if (violatesChroot(storeFile)) {
            errString = "550 Invalid name or chroot violation\r\n";
        } else if (storeFile.isDirectory()) {
            errString = "451 Can't overwrite a directory\r\n";
        } else {
            try {
                if (storeFile.exists() && !append) {
                    if (storeFile.delete()) {
                        Util.deletedFileNotify(storeFile.getPath());
                    } else {
                        errString = "451 Couldn't truncate file\r\n";
                    }
                }
                FileOutputStream out2 = new FileOutputStream(storeFile, append);
                if (this.sessionThread.startUsingDataSocket()) {
                    Log.d(TAG, "Data socket ready");
                    this.sessionThread.writeString("150 Data socket ready\r\n");
                    byte[] buffer = new byte[Defaults.getDataChunkSize()];
                    if (this.sessionThread.isBinaryMode()) {
                        Log.d(TAG, "Mode is binary");
                    } else {
                        Log.d(TAG, "Mode is ascii");
                    }
                    while (true) {
                        int numRead = this.sessionThread.receiveFromDataSocket(buffer);
                        switch (numRead) {
                            case -2:
                                errString = "425 Could not connect data socket\r\n";
                                out = out2;
                                break;
                            case -1:
                                Log.d(TAG, "Returned from final read");
                                out = out2;
                                break;
                            case 0:
                                errString = "426 Couldn't receive data\r\n";
                                out = out2;
                                break;
                            default:
                                try {
                                    if (this.sessionThread.isBinaryMode()) {
                                        out2.write(buffer, 0, numRead);
                                    } else {
                                        int startPos = 0;
                                        int endPos = 0;
                                        while (endPos < numRead) {
                                            if (buffer[endPos] == (byte) 13) {
                                                out2.write(buffer, startPos, endPos - startPos);
                                                startPos = endPos + 1;
                                            }
                                            endPos++;
                                        }
                                        if (startPos < numRead) {
                                            out2.write(buffer, startPos, endPos - startPos);
                                        }
                                    }
                                    out2.flush();
                                } catch (IOException e) {
                                    errString = "451 File IO problem. Device might be full.\r\n";
                                    Log.d(TAG, "Exception while storing: " + e);
                                    Log.d(TAG, "Message: " + e.getMessage());
                                    Log.d(TAG, "Stack trace: ");
                                    for (StackTraceElement elem : e.getStackTrace()) {
                                        Log.d(TAG, elem.toString());
                                    }
                                    out = out2;
                                    break;
                                }
                        }
                    }
                }
                errString = "425 Couldn't open data socket\r\n";
                out = out2;
            } catch (FileNotFoundException e2) {
                try {
                    errString = "451 Couldn't open file \"" + param + "\" aka \"" + storeFile.getCanonicalPath() + "\" for writing\r\n";
                } catch (IOException e3) {
                    errString = "451 Couldn't open file, nested exception\r\n";
                }
            }
        }
        AutoClose.closeQuietly(out);
        if (errString != null) {
            Log.i(TAG, "STOR error: " + errString.trim());
            this.sessionThread.writeString(errString);
        } else {
            this.sessionThread.writeString("226 Transmission complete\r\n");
            Util.newFileNotify(storeFile.getPath());
        }
        this.sessionThread.closeDataSocket();
        Log.d(TAG, "STOR finished");
    }
}
