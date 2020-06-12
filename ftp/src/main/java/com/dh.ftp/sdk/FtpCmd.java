package com.dh.ftp.sdk;

import android.util.Log;

import java.io.File;

abstract class FtpCmd implements Runnable {
    private static final String TAG = FtpCmd.class.getSimpleName();
    SessionThread sessionThread;

    public abstract void run();

    private static FtpCmd getCmd(String cmd, SessionThread sessionThread, String input) {
        if ("SYST".equals(cmd)) {
            return new CmdSYST(sessionThread);
        }
        if ("USER".equals(cmd)) {
            return new CmdUSER(sessionThread, input);
        }
        if ("PASS".equals(cmd)) {
            return new CmdPASS(sessionThread, input);
        }
        if ("TYPE".equals(cmd)) {
            return new CmdTYPE(sessionThread, input);
        }
        if ("CWD".equals(cmd)) {
            return new CmdCWD(sessionThread, input);
        }
        if ("PWD".equals(cmd)) {
            return new CmdPWD(sessionThread);
        }
        if ("LIST".equals(cmd)) {
            return new CmdLIST(sessionThread, input);
        }
        if ("PASV".equals(cmd)) {
            return new CmdPASV(sessionThread);
        }
        if ("RETR".equals(cmd)) {
            return new CmdRETR(sessionThread, input);
        }
        if ("NLST".equals(cmd)) {
            return new CmdNLST(sessionThread, input);
        }
        if ("NOOP".equals(cmd)) {
            return new CmdNOOP(sessionThread);
        }
        if ("STOR".equals(cmd)) {
            return new CmdSTOR(sessionThread, input);
        }
        if ("DELE".equals(cmd)) {
            return new CmdDELE(sessionThread, input);
        }
        if ("RNFR".equals(cmd)) {
            return new CmdRNFR(sessionThread, input);
        }
        if ("RNTO".equals(cmd)) {
            return new CmdRNTO(sessionThread, input);
        }
        if ("RMD".equals(cmd)) {
            return new CmdRMD(sessionThread, input);
        }
        if ("MKD".equals(cmd)) {
            return new CmdMKD(sessionThread, input);
        }
        if ("OPTS".equals(cmd)) {
            return new CmdOPTS(sessionThread, input);
        }
        if ("PORT".equals(cmd)) {
            return new CmdPORT(sessionThread, input);
        }
        if ("QUIT".equals(cmd)) {
            return new CmdQUIT(sessionThread);
        }
        if ("FEAT".equals(cmd)) {
            return new CmdFEAT(sessionThread);
        }
        if ("SIZE".equals(cmd)) {
            return new CmdSIZE(sessionThread, input);
        }
        if ("CDUP".equals(cmd)) {
            return new CmdCDUP(sessionThread);
        }
        if ("APPE".equals(cmd)) {
            return new CmdAPPE(sessionThread, input);
        }
        if ("XCUP".equals(cmd)) {
            return new CmdCDUP(sessionThread);
        }
        if ("XPWD".equals(cmd)) {
            return new CmdPWD(sessionThread);
        }
        if ("XMKD".equals(cmd)) {
            return new CmdMKD(sessionThread, input);
        }
        if ("XRMD".equals(cmd)) {
            return new CmdRMD(sessionThread, input);
        }
        return null;
    }

    FtpCmd(SessionThread sessionThread) {
        this.sessionThread = sessionThread;
    }

    static void dispatchCommand(SessionThread session, String inputString) {
        String[] strings = inputString.split(" ");
        String unrecognizedCmdMsg = "502 Command not recognized\r\n";
        if (strings.length < 1) {
            Log.i(TAG, "No strings parsed");
            session.writeString(unrecognizedCmdMsg);
            return;
        }
        String verb = strings[0];
        if (verb.length() < 1) {
            Log.i(TAG, "Invalid command verb");
            session.writeString(unrecognizedCmdMsg);
            return;
        }
        verb = verb.trim().toUpperCase();
        FtpCmd cmdInstance = getCmd(verb, session, inputString);
        if (cmdInstance == null) {
            Log.d(TAG, "Ignoring unrecognized FTP verb: " + verb);
            session.writeString(unrecognizedCmdMsg);
        } else if (session.isAuthenticated() || cmdInstance.getClass().equals(CmdUSER.class) || cmdInstance.getClass().equals(CmdPASS.class) || cmdInstance.getClass().equals(CmdUSER.class)) {
            cmdInstance.run();
        } else {
            session.writeString("530 Login first with USER and PASS\r\n");
        }
    }

    static String getParameter(String input, boolean silent) {
        if (input == null) {
            return "";
        }
        int firstSpacePosition = input.indexOf(32);
        if (firstSpacePosition == -1) {
            return "";
        }
        String retString = input.substring(firstSpacePosition + 1).replaceAll("\\s+$", "");
        if (silent) {
            return retString;
        }
        Log.d(TAG, "Parsed argument: " + retString);
        return retString;
    }

    static String getParameter(String input) {
        return getParameter(input, false);
    }

    static File inputPathToChrootedFile(File existingPrefix, String param) {
        try {
            if (param.charAt(0) == '/') {
                return new File(Globals.getChrootDir(), param);
            }
        } catch (Exception e) {
        }
        return new File(existingPrefix, param);
    }

    boolean violatesChroot(File file) {
        File chroot = Globals.getChrootDir();
        try {
            String canonicalPath = file.getCanonicalPath();
            if (canonicalPath.startsWith(chroot.toString())) {
                return false;
            }
            Log.i(TAG, "Path violated folder restriction, denying");
            Log.d(TAG, "path: " + canonicalPath);
            Log.d(TAG, "chroot: " + chroot);
            return true;
        } catch (Exception e) {
            Log.i(TAG, "Path canonicalization problem: " + e);
            Log.i(TAG, "When checking file: " + file.getAbsolutePath());
            return true;
        }
    }
}
