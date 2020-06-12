package com.dh.ftp.sdk;


import android.util.Log;

public class CmdOPTS extends FtpCmd {
    private static final String TAG = CmdOPTS.class.getSimpleName();
    public static final String message = "TEMPLATE!!";
    private String input;

    public CmdOPTS(SessionThread sessionThread, String input) {
        super(sessionThread);
        this.input = input;
    }

    public void run() {
        String param = FtpCmd.getParameter(this.input);
        String errString = null;
        if (param == null) {
            errString = "550 Need argument to OPTS\r\n";
            Log.w(TAG, "Couldn't understand empty OPTS command");
        } else {
            String[] splits = param.split(" ");
            if (splits.length != 2) {
                errString = "550 Malformed OPTS command\r\n";
                Log.w(TAG, "Couldn't parse OPTS command");
            } else {
                String optName = splits[0].toUpperCase();
                String optVal = splits[1].toUpperCase();
                if (!optName.equals(this.sessionThread.encoding)) {
                    Log.d(TAG, "Unrecognized OPTS option: " + optName);
                    errString = "502 Unrecognized option\r\n";
                } else if ("ON".equals(optVal)) {
                    Log.d(TAG, "Got OPTS " + optName + " ON");
                } else {
                    Log.i(TAG, "Ignoring OPTS " + optName + " for something besides ON");
                }
            }
        }
        if (errString != null) {
            this.sessionThread.writeString(errString);
            Log.i(TAG, "Template log message");
            return;
        }
        this.sessionThread.writeString("200 OPTS accepted\r\n");
        Log.d(TAG, "Handled OPTS ok");
    }
}
