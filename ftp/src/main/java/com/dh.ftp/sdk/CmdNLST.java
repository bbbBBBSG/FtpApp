package com.dh.ftp.sdk;

import android.util.Log;

import java.io.File;

public class CmdNLST extends CmdAbstractListing {
    private static final String TAG = CmdLIST.class.getSimpleName();
    private String input;

    public CmdNLST(SessionThread sessionThread, String input) {
        super(sessionThread);
        this.input = input;
    }


    public void run() {
        String errString = null;

        mainblock: {
            String param = getParameter(input);
            if(param.startsWith("-")) {
                // Ignore options to list, which start with a dash
                param = "";
            }
            File fileToList = null;
            if(param.equals("")) {
                fileToList = sessionThread.getWorkingDir();
            } else {
                if(param.contains("*")) {
                    errString = "550 NLST does not support wildcards\r\n";
                    break mainblock;
                }
                fileToList = new File(sessionThread.getWorkingDir(), param);
                if(violatesChroot(fileToList)) {
                    errString = "450 Listing target violates chroot\r\n";
                    break mainblock;
                } else if(fileToList.isFile()) {
                    // Bernstein suggests that NLST should fail when a
                    // parameter is given and the parameter names a regular
                    // file (not a directory).
                    errString = "550 NLST for regular files is unsupported\r\n";
                    break mainblock;
                }
            }
            String listing;
            if(fileToList.isDirectory()) {
                StringBuilder response = new StringBuilder();
                errString = listDirectory(response, fileToList);
                if(errString != null) {
                    break mainblock;
                }
                listing = response.toString();
            } else {
                listing = makeLsString(fileToList);
                if(listing == null) {
                    errString = "450 Couldn't list that file\r\n";
                    break mainblock;
                }
            }
            errString = sendListing(listing);
            if(errString != null) {
                break mainblock;
            }
        }

        if(errString != null) {
            sessionThread.writeString(errString);
            Log.d(TAG, "NLST failed with: " + errString);
        } else {
            Log.d(TAG,  "NLST completed OK");
        }
        // The success or error response over the control connection will
        // have already been handled by sendListing, so we can just quit now.
    }

    String makeLsString(File file) {
        if (file.exists()) {
            String lastNamePart = file.getName();
            if (lastNamePart.contains("*") || lastNamePart.contains("/")) {
                Log.i(TAG, "Filename omitted due to disallowed character");
                return null;
            }
            Log.d(TAG, "Filename: " + lastNamePart);
            return lastNamePart + "\r\n";
        }
        Log.i(TAG, "makeLsString had nonexistent file");
        return null;
    }
}
