package com.dh.ftp.sdk;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

public class SessionThread extends Thread {
    private static int MAX_AUTH_FAILS = 3;
    private static final String TAG = SessionThread.class.getSimpleName();
    Account account;
    private int authFails;
    private boolean authenticated;
    private boolean binaryMode;
    protected ByteBuffer buffer;
    private Socket cmdSocket;
    private OutputStream dataOutputStream;
    private Socket dataSocket;
    private DataSocketFactory dataSocketFactory;
    String encoding;
    private File renameFrom;
    private boolean sendWelcomeBanner;
    private Source source;
    private File workingDir;

    public enum Source {
        LOCAL,
        PROXY
    }

    boolean sendViaDataSocket(String string) {
        try {
            byte[] bytes = string.getBytes(this.encoding);
            Log.d(TAG, "Using data connection encoding: " + this.encoding);
            return sendViaDataSocket(bytes, bytes.length);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Unsupported encoding for data socket send");
            return false;
        }
    }

    boolean sendViaDataSocket(byte[] bytes, int len) {
        return sendViaDataSocket(bytes, 0, len);
    }

    boolean sendViaDataSocket(byte[] bytes, int start, int len) {
        if (this.dataOutputStream == null) {
            Log.i(TAG, "Can't send via null dataOutputStream");
            return false;
        } else if (len == 0) {
            return true;
        } else {
            try {
                this.dataOutputStream.write(bytes, start, len);
                this.dataSocketFactory.reportTraffic((long) len);
                return true;
            } catch (IOException e) {
                Log.i(TAG, "Couldn't write output stream for data socket");
                Log.i(TAG, e.toString());
                return false;
            }
        }
    }

    int receiveFromDataSocket(byte[] buf) {
        if (this.dataSocket == null) {
            Log.i(TAG, "Can't receive from null dataSocket");
            return -2;
        } else if (this.dataSocket.isConnected()) {
            try {
                int bytesRead;
                InputStream in = this.dataSocket.getInputStream();
                do {
                    bytesRead = in.read(buf, 0, buf.length);
                } while (bytesRead == 0);
                if (bytesRead == -1) {
                    return -1;
                }
                this.dataSocketFactory.reportTraffic((long) bytesRead);
                return bytesRead;
            } catch (IOException e) {
                Log.i(TAG, "Error reading data socket");
                return 0;
            }
        } else {
            Log.i(TAG, "Can't receive from unconnected socket");
            return -2;
        }
    }

    int onPasv() {
        return this.dataSocketFactory.onPasv();
    }

    boolean onPort(InetAddress dest, int port) {
        return this.dataSocketFactory.onPort(dest, port);
    }

    InetAddress getDataSocketPasvIp() {
        return this.cmdSocket.getLocalAddress();
    }

    boolean startUsingDataSocket() {
        try {
            this.dataSocket = this.dataSocketFactory.onTransfer();
            if (this.dataSocket == null) {
                Log.i(TAG, "dataSocketFactory.onTransfer() returned null");
                return false;
            }
            this.dataOutputStream = this.dataSocket.getOutputStream();
            return true;
        } catch (IOException e) {
            Log.i(TAG, "IOException getting OutputStream for data socket");
            this.dataSocket = null;
            return false;
        }
    }

    private void quit() {
        Log.d(TAG, "SessionThread told to quit");
        closeSocket();
    }

    public void closeDataSocket() {
        Log.i(TAG, "Closing data socket");
        AutoClose.closeQuietly(this.dataOutputStream);
        AutoClose.closeQuietly(this.dataSocket);
        this.dataSocket = null;
    }

    public void run() {
        Log.i(TAG, "SessionThread started");
        if (this.sendWelcomeBanner) {
            writeString("220 SwiFTP " + Util.getVersion() + " ready\r\n");
        }
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(this.cmdSocket.getInputStream(), this.encoding), 8192);
            while (true) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }
                Log.i(TAG, "Received line from client: " + line);
                FtpCmd.dispatchCommand(this, line);
            }
            Log.i(TAG, "readLine gave null, quitting");
        } catch (IOException e) {
            Log.i(TAG, "Connection was dropped");
        }
        closeSocket();
    }

    public void closeSocket() {
        AutoClose.closeQuietly(this.cmdSocket);
    }

    private void writeBytes(byte[] bytes) {
        try {
            BufferedOutputStream out = new BufferedOutputStream(this.cmdSocket.getOutputStream(), Defaults.dataChunkSize);
            out.write(bytes);
            out.flush();
            this.dataSocketFactory.reportTraffic((long) bytes.length);
        } catch (IOException e) {
            Log.i(TAG, "Exception writing socket");
            closeSocket();
        }
    }

    public void writeString(String str) {
        byte[] strBytes;
        try {
            strBytes = str.getBytes(this.encoding);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Unsupported encoding: " + this.encoding);
            strBytes = str.getBytes();
        }
        writeBytes(strBytes);
    }

    protected Socket getSocket() {
        return this.cmdSocket;
    }

    public Account getAccount() {
        return this.account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public SessionThread(Socket socket, DataSocketFactory dataSocketFactory, Source source) {
        this.buffer = ByteBuffer.allocate(Defaults.getInputBufferSize());
        this.account = new Account();
        this.workingDir = Globals.getChrootDir();
        this.encoding = Defaults.SESSION_ENCODING;
        this.cmdSocket = socket;
        this.source = source;
        this.dataSocketFactory = dataSocketFactory;
        this.sendWelcomeBanner = source == Source.LOCAL;
    }

    boolean isBinaryMode() {
        return this.binaryMode;
    }

    void setBinaryMode(boolean binaryMode) {
        this.binaryMode = binaryMode;
    }

    boolean isAuthenticated() {
        return this.authenticated;
    }

    void authAttempt(boolean authenticated) {
        if (authenticated) {
            Log.i(TAG, "Authentication complete");
            this.authenticated = true;
            return;
        }
        if (this.source == Source.PROXY) {
            quit();
        } else {
            this.authFails++;
            Log.i(TAG, "Auth failed: " + this.authFails + "/" + MAX_AUTH_FAILS);
        }
        if (this.authFails > MAX_AUTH_FAILS) {
            Log.i(TAG, "Too many auth fails, quitting session");
            quit();
        }
    }

    public File getWorkingDir() {
        return this.workingDir;
    }

    void setWorkingDir(File workingDir) {
        try {
            this.workingDir = workingDir.getCanonicalFile().getAbsoluteFile();
        } catch (IOException e) {
            Log.i(TAG, "SessionThread canonical error");
        }
    }

    File getRenameFrom() {
        return this.renameFrom;
    }

    void setRenameFrom(File renameFrom) {
        this.renameFrom = renameFrom;
    }

    public String getEncoding() {
        return this.encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
}
