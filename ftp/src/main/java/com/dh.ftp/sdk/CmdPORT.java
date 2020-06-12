package com.dh.ftp.sdk;

import android.support.v4.view.InputDeviceCompat;
import android.util.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class CmdPORT extends FtpCmd {
    private static final String TAG = CmdPORT.class.getSimpleName();
    private String input;

    public CmdPORT(SessionThread sessionThread, String input) {
        super(sessionThread);
        this.input = input;
    }

    public void run() {
        Log.d(TAG, "PORT running");
        String errString = null;
        String param = FtpCmd.getParameter(this.input);
        if (param.contains("|") && param.contains("::")) {
            errString = "550 No IPv6 support, reconfigure your client\r\n";
        } else {
            String[] substrs = param.split(",");
            if (substrs.length != 6) {
                errString = "550 Malformed PORT argument\r\n";
            } else {
                for (String substr : substrs) {
                    if (!substr.matches("[0-9]+") || substr.length() > 3) {
                        errString = "550 Invalid PORT argument: " + substr + "\r\n";
                        break;
                    }
                }
                byte[] ipBytes = new byte[4];
                int i = 0;
                while (i < 4) {
                    try {
                        int ipByteAsInt = Integer.parseInt(substrs[i]);
                        if (ipByteAsInt >= 128) {
                            ipByteAsInt += InputDeviceCompat.SOURCE_ANY;
                        }
                        ipBytes[i] = (byte) ipByteAsInt;
                        i++;
                    } catch (Exception e) {
                        errString = "550 Invalid PORT format: " + substrs[i] + "\r\n";
                    }
                }
                try {
                    this.sessionThread.onPort(InetAddress.getByAddress(ipBytes), (Integer.parseInt(substrs[4]) * 256) + Integer.parseInt(substrs[5]));
                } catch (UnknownHostException e2) {
                    errString = "550 Unknown host\r\n";
                }
            }
        }
        if (errString == null) {
            this.sessionThread.writeString("200 PORT OK\r\n");
            Log.d(TAG, "PORT completed");
            return;
        }
        Log.i(TAG, "PORT error: " + errString);
        this.sessionThread.writeString(errString);
    }
}
