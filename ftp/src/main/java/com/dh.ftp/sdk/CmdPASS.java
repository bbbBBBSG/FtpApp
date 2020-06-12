package com.dh.ftp.sdk;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class CmdPASS extends FtpCmd {
    private static final String TAG = CmdPASS.class.getSimpleName();
    private String input;

    public CmdPASS(SessionThread sessionThread, String input) {
        super(sessionThread);
        this.input = input;
    }

    public void run() {
        Log.d(TAG, "Executing PASS");
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(Globals.getContext());
        if (settings.getBoolean("anonymous_login", true)) {
            this.sessionThread.writeString("230 Access granted\r\n");
            this.sessionThread.authAttempt(true);
            return;
        }
        String attemptPassword = FtpCmd.getParameter(this.input, true);
        String attemptUsername = this.sessionThread.account.getUsername();
        if (attemptUsername == null) {
            this.sessionThread.writeString("503 Must send USER first\r\n");
            return;
        }
        if (Globals.getContext() == null) {
            Log.e(TAG, "No global context in PASS\r\n");
        }
        String username = settings.getString("username", null);
        String password = settings.getString("password", null);
        if (username == null || password == null) {
            Log.e(TAG, "Username or password misconfigured");
            this.sessionThread.writeString("500 Internal error during authentication");
        } else if (username.equals(attemptUsername) && password.equals(attemptPassword)) {
            this.sessionThread.writeString("230 Access granted\r\n");
            Log.i(TAG, "User " + username + " password verified");
            this.sessionThread.authAttempt(true);
        } else {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            Log.i(TAG, "Failed authentication");
            this.sessionThread.writeString("530 Login incorrect.\r\n");
            this.sessionThread.authAttempt(false);
        }
    }
}
