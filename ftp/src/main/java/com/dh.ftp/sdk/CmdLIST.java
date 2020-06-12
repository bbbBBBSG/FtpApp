package com.dh.ftp.sdk;

import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CmdLIST extends CmdAbstractListing {
    private static final long MS_IN_SIX_MONTHS = -1627869184;
    private String TAG = CmdLIST.class.getSimpleName();
    private String input;

    public CmdLIST(SessionThread sessionThread, String input) {
        super(sessionThread);
        this.input = input;
    }

    public void run() {
        String errString = null;

        mainblock: {
            String param = getParameter(input);
            Log.d(TAG,"LIST parameter: " + param);
            while(param.startsWith("-")) {
                // Skip all dashed -args, if present
                Log.d(TAG,"LIST is skipping dashed arg " + param);
                param = getParameter(param);
            }
            File fileToList = null;
            if(param.equals("")) {
                fileToList = sessionThread.getWorkingDir();
            } else {
                if(param.contains("*")) {
                    errString = "550 LIST does not support wildcards\r\n";
                    break mainblock;
                }
                fileToList = new File(sessionThread.getWorkingDir(), param);
                if(violatesChroot(fileToList)) {
                    errString = "450 Listing target violates chroot\r\n";
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
            Log.i(TAG, "LIST failed with: " + errString);
        } else {
            Log.i(TAG, "LIST completed OK");
        }
    }

    String makeLsString(File file) {
        StringBuilder response = new StringBuilder();
        if (file.exists()) {
            String lastNamePart = file.getName();
            if (lastNamePart.contains("*") || lastNamePart.contains("/")) {
                Log.i(this.TAG, "Filename omitted due to disallowed character");
                return null;
            }
            if (file.isDirectory()) {
                response.append("drwxr-xr-x 1 owner group");
            } else {
                response.append("-rw-r--r-- 1 owner group");
            }
            String sizeString = Long.toString(file.length());
            int padSpaces = 13 - sizeString.length();
            while (true) {
                int padSpaces2 = padSpaces;
                padSpaces = padSpaces2 - 1;
                if (padSpaces2 <= 0) {
                    break;
                }
                response.append(' ');
            }
            response.append(sizeString);
            response.append((System.currentTimeMillis() - file.lastModified() > MS_IN_SIX_MONTHS ? new SimpleDateFormat(" MMM dd HH:mm ", Locale.US) : new SimpleDateFormat(" MMM dd  yyyy ", Locale.US)).format(new Date(file.lastModified())));
            response.append(lastNamePart);
            response.append("\r\n");
            Log.e(this.TAG, "makeLsString had nonexistent file" + response.toString());
            return response.toString();
        }
        return null;
    }
}
