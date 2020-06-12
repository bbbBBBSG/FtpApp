package com.dh.ftp.sdk;

import android.util.Log;

import java.io.File;

abstract class CmdAbstractListing extends FtpCmd {
    private static final String TAG = CmdAbstractListing.class.getSimpleName();

    abstract String makeLsString(File file);

    CmdAbstractListing(SessionThread sessionThread) {
        super(sessionThread);
    }

    String listDirectory(StringBuilder response, File dir) {
        if (!dir.isDirectory()) {
            return "500 Internal error, listDirectory on non-directory\r\n";
        }
        Log.d(TAG, "Listing directory: " + dir);
        File[] entries = dir.listFiles();
        if (entries == null) {
            return "500 Couldn't list directory. Check config and mount status.\r\n";
        }
        Log.d(TAG, "Dir len " + entries.length);
        for (File entry : entries) {
            String curLine = makeLsString(entry);
            if (curLine != null) {
                response.append(curLine);
            }
        }
        return null;
    }

    String sendListing(String listing) {
        if (this.sessionThread.startUsingDataSocket()) {
            Log.d(TAG, "LIST/NLST done making socket");
            this.sessionThread.writeString("150 Opening " + (this.sessionThread.isBinaryMode() ? "BINARY" : "ASCII") + " mode data connection for file list\r\n");
            Log.d(TAG, "Sent code 150, sending listing string now");
            if (this.sessionThread.sendViaDataSocket(listing)) {
                this.sessionThread.closeDataSocket();
                Log.d(TAG, "Listing sendViaDataSocket success");
                this.sessionThread.writeString("226 Data transmission OK\r\n");
                return null;
            }
            Log.d(TAG, "sendViaDataSocket failure");
            this.sessionThread.closeDataSocket();
            return "426 Data socket or network error\r\n";
        }
        this.sessionThread.closeDataSocket();
        return "425 Error opening data socket\r\n";
    }
}
