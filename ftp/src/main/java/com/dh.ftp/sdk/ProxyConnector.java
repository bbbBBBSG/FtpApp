package com.dh.ftp.sdk;

import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.dh.ftp.FTPServerService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ProxyConnector extends Thread {
    private static final int CONNECT_TIMEOUT = 5000;
    private static final String ENCODING = "UTF-8";
    private static final int IN_BUF_SIZE = 2048;
    private static final String PREFERRED_SERVER = "preferred_server";
    private static final String TAG = ProxyConnector.class.getSimpleName();
    private static final long UPDATE_USAGE_BYTES = 5000000;
    private static final String USAGE_PREFS_NAME = "proxy_usage_data";
    private Socket commandSocket;
    private FTPServerService ftpServerService;
    private String hostname;
    private InputStream inputStream;
    private State proxyState = State.DISCONNECTED;
    private long proxyUsage;

    public enum State {
        CONNECTING,
        CONNECTED,
        FAILED,
        UNREACHABLE,
        DISCONNECTED
    }

    public ProxyConnector(FTPServerService ftpServerService) {
        this.ftpServerService = ftpServerService;
        this.proxyUsage = getPersistedProxyUsage();
        setProxyState(State.DISCONNECTED);
        Globals.setProxyConnector(this);
    }

    public void run() {
        Log.i(TAG, "In ProxyConnector.run()");
        setProxyState(State.CONNECTING);
        try {
            for (String candidateHostname : getProxyList()) {
                this.hostname = candidateHostname;
                this.commandSocket = newAuthedSocket(this.hostname, Defaults.REMOTE_PROXY_PORT);
                if (this.commandSocket != null) {
                    this.commandSocket.setSoTimeout(0);
                    JSONObject response = sendRequest(this.commandSocket, makeJsonRequest("start_command_session"));
                    if (response != null) {
                        if (response.has("prefix")) {
                            Log.i(TAG, "Got prefix of: " + response.getString("prefix"));
                            break;
                        }
                        Log.i(TAG, "start_command_session didn't receive a prefix in response");
                    } else {
                        Log.i(TAG, "Couldn't create proxy command session");
                    }
                }
            }
            if (this.commandSocket == null) {
                Log.i(TAG, "No proxies accepted connection, failing.");
                setProxyState(State.UNREACHABLE);
                return;
            }
            setProxyState(State.CONNECTED);
            preferServer(this.hostname);
            this.inputStream = this.commandSocket.getInputStream();
            byte[] bytes = new byte[2048];
            while (true) {
                Log.d(TAG, "to proxy read()");
                int numBytes = this.inputStream.read(bytes);
                incrementProxyUsage((long) numBytes);
                Log.d(TAG, "from proxy read()");
                if (numBytes > 0) {
                    JSONObject incomingJson = new JSONObject(new String(bytes, "UTF-8"));
                    if (incomingJson.has("action")) {
                        incomingCommand(incomingJson);
                    } else {
                        Log.i(TAG, "Response received but no responseWaiter");
                    }
                } else if (numBytes == 0) {
                    Log.d(TAG, "Command socket read 0 bytes, looping");
                } else {
                    Log.d(TAG, "Command socket end of stream, exiting");
                    if (this.proxyState != State.DISCONNECTED) {
                        setProxyState(State.FAILED);
                    }
                    Log.i(TAG, "ProxyConnector thread quitting cleanly");
                    Globals.setProxyConnector(null);
                    this.hostname = null;
                    Log.d(TAG, "ProxyConnector.run() returning");
                    persistProxyUsage();
                    return;
                }
            }
        } catch (IOException e) {
            Log.i(TAG, "IOException in command session: " + e);
            setProxyState(State.FAILED);
        } catch (JSONException e2) {
            Log.i(TAG, "Commmand socket JSONException: " + e2);
            setProxyState(State.FAILED);
        } catch (Exception e3) {
            Log.i(TAG, "Other exception in ProxyConnector: " + e3);
            setProxyState(State.FAILED);
        } finally {
            Globals.setProxyConnector(null);
            this.hostname = null;
            Log.d(TAG, "ProxyConnector.run() returning");
            persistProxyUsage();
        }
    }

    private void preferServer(String hostname) {
        Editor editor = Globals.getContext().getSharedPreferences(PREFERRED_SERVER, 0).edit();
        editor.putString(PREFERRED_SERVER, hostname);
        editor.apply();
    }

    private String[] getProxyList() {
        String preferred = Globals.getContext().getSharedPreferences(PREFERRED_SERVER, 0).getString(PREFERRED_SERVER, null);
        List<String> proxyList = Arrays.asList(new String[]{"c1.swiftp.org", "c2.swiftp.org", "c3.swiftp.org", "c4.swiftp.org", "c5.swiftp.org", "c6.swiftp.org", "c7.swiftp.org", "c8.swiftp.org", "c9.swiftp.org"});
        Collections.shuffle(proxyList);
        String[] allProxies = (String[]) proxyList.toArray(new String[proxyList.size()]);
        if (preferred == null) {
            return allProxies;
        }
        return Util.concatStrArrays(new String[]{preferred}, allProxies);
    }

    private boolean checkAndPrintJsonError(JSONObject json) throws JSONException {
        if (!json.has("error_code")) {
            return false;
        }
        StringBuilder s = new StringBuilder("Error in JSON response, code: ");
        s.append(json.getString("error_code"));
        if (json.has("error_string")) {
            s.append(", string: ");
            s.append(json.getString("error_string"));
        }
        Log.i(TAG, s.toString());
        return true;
    }

    private void incomingCommand(JSONObject json) {
        try {
            String action = json.getString("action");
            if ("control_connection_waiting".equals(action)) {
                startControlSession(json.getInt("port"));
            } else if ("prefer_server".equals(action)) {
                String host = json.getString("host");
                preferServer(host);
                Log.i(TAG, "New preferred server: " + host);
            } else if ("message".equals(action)) {
                Log.i(TAG, "Got news from proxy server: \"" + json.getString("text") + "\"");
                FTPServerService.updateClients();
            } else if ("noop".equals(action)) {
                Log.d(TAG, "Proxy noop");
            } else {
                Log.i(TAG, "Unsupported incoming action: " + action);
            }
        } catch (JSONException e) {
            Log.i(TAG, "JSONException in proxy incomingCommand");
        }
    }

    private void startControlSession(int port) {
        Log.d(TAG, "Starting new proxy FTP control session");
        Socket socket = newAuthedSocket(this.hostname, port);
        if (socket == null) {
            Log.i(TAG, "startControlSession got null authed socket");
            return;
        }
        SessionThread thread = new SessionThread(socket, new ProxyDataSocketFactory(), SessionThread.Source.PROXY);
        thread.start();
        this.ftpServerService.registerSessionThread(thread);
    }

    private Socket newAuthedSocket(String hostname, int port) {
        if(hostname == null) {
            Log.i(TAG,"newAuthedSocket can't connect to null host");
            return null;
        }
        JSONObject json = new JSONObject();
        //String secret = retrieveSecret();
        Socket socket;
        OutputStream out = null;
        InputStream in = null;

        try {
            Log.d(TAG,"Opening proxy connection to " + hostname + ":" + port);
            socket = new Socket();
            socket.connect(new InetSocketAddress(hostname, port), CONNECT_TIMEOUT);
            json.put("android_id", Util.getAndroidId());
            json.put("swiftp_version", Util.getVersion());
            json.put("action", "login");
            out = socket.getOutputStream();
            in = socket.getInputStream();
            int numBytes;

            out.write(json.toString().getBytes(ENCODING));
            Log.d(TAG, "Sent login request");
            // Read and parse the server's response
            byte[] bytes = new byte[IN_BUF_SIZE];
            // Here we assume that the server's response will all be contained in
            // a single read, which may be unsafe for large responses
            numBytes = in.read(bytes);
            if(numBytes == -1) {
                Log.i(TAG, "Proxy socket closed while waiting for auth response");
                return null;
            } else if (numBytes == 0) {
                Log.i(TAG, "Short network read waiting for auth, quitting");
                return null;
            }
            json = new JSONObject(new String(bytes, 0, numBytes, ENCODING));
            if(checkAndPrintJsonError(json)) {
                return null;
            }
            Log.d(TAG,"newAuthedSocket successful");
            return socket;
        } catch(Exception e) {
            Log.i(TAG,"Exception during proxy connection or authentication: " + e);
            return null;
        }
    }

    public void quit() {
        setProxyState(State.DISCONNECTED);
        try {
            sendRequest(this.commandSocket, makeJsonRequest("finished"));
            AutoClose.closeQuietly(this.inputStream);
            AutoClose.closeQuietly(this.commandSocket);
        } catch (JSONException e) {
        }
        persistProxyUsage();
        Globals.setProxyConnector(null);
    }

    private JSONObject sendRequest(InputStream in, OutputStream out, JSONObject request) throws JSONException {
        try {
            byte[] buffer = Util.jsonToByteArray(request);
            if (buffer == null) {
                return null;
            }
            out.write(buffer);
            byte[] bytes = new byte[2048];
            if (in.read(bytes) < 1) {
                Log.i(TAG, "Proxy sendRequest short read on response");
                return null;
            }
            JSONObject response = Util.byteArrayToJson(bytes);
            if (response == null) {
                Log.i(TAG, "Null response to sendRequest");
            }
            if (!checkAndPrintJsonError(response)) {
                return response;
            }
            Log.i(TAG, "Error response to sendRequest");
            return null;
        } catch (IOException e) {
            Log.i(TAG, "IOException in proxy sendRequest: " + e);
            return null;
        }
    }

    private JSONObject sendRequest(Socket socket, JSONObject request) throws JSONException {
        try {
            if(socket == null) {
                // The server is probably shutting down
                Log.i(TAG,"null socket in ProxyConnector.sendRequest()");
                return null;
            } else {
                return sendRequest(socket.getInputStream(),
                        socket.getOutputStream(),
                        request);
            }
        } catch (IOException e) {
            Log.i(TAG,"IOException in proxy sendRequest wrapper: " + e);
            return null;
        }
    }

    ProxyDataSocketInfo pasvListen() {
        try {
            // connect to proxy and authenticate
            Log.d(TAG,"Sending data_pasv_listen to proxy");
            Socket socket = newAuthedSocket(this.hostname, Defaults.REMOTE_PROXY_PORT);
            if(socket == null) {
                Log.i(TAG,"pasvListen got null socket");
                return null;
            }
            JSONObject request = makeJsonRequest("data_pasv_listen");

            JSONObject response = sendRequest(socket, request);
            if(response == null) {
                return null;
            }
            int port = response.getInt("port");
            return new ProxyDataSocketInfo(socket, port);
        } catch(JSONException e) {
            Log.i(TAG, "JSONException in pasvListen");
            return null;
        }
    }

    Socket dataPortConnect(java.net.InetAddress clientAddr, int clientPort) {
        /**
         * This function is called by a ProxyDataSocketFactory when it's time to
         * transfer some data in PORT mode (not PASV mode). We send a
         * data_port_connect request to the proxy, containing the IP and port
         * of the FTP client to which a connection should be made.
         */
        try {
            Log.d(TAG,"Sending data_port_connect to proxy");
            Socket socket = newAuthedSocket(this.hostname, Defaults.REMOTE_PROXY_PORT);
            if(socket == null) {
                Log.i(TAG,"dataPortConnect got null socket");
                return null;
            }
            JSONObject request =  makeJsonRequest("data_port_connect");
            request.put("address", clientAddr.getHostAddress());
            request.put("port", clientPort);
            JSONObject response = sendRequest(socket, request);
            if(response == null) {
                return null; // logged elsewhere
            }
            return socket;
        } catch (JSONException e) {
            Log.i(TAG,"JSONException in dataPortConnect");
            return null;
        }
    }

    /**
     * Given a socket returned from pasvListen(), send a data_pasv_accept request
     * over the socket to the proxy, which should result in a socket that is ready
     * for data transfer with the FTP client. Of course, this will only work if the
     * FTP client connects to the proxy like it's supposed to. The client will have
     * already been told to connect by the response to its PASV command.
     *
     * This should only be called from the onTransfer method of ProxyDataSocketFactory.
     *
     * @param socket A socket previously returned from ProxyConnector.pasvListen()
     * @return true if the accept operation completed OK, otherwise false
     */
    boolean pasvAccept(Socket socket) {
        try {
            JSONObject response = sendRequest(socket, makeJsonRequest("data_pasv_accept"));
            if (response == null) {
                return false;
            }
            if (checkAndPrintJsonError(response)) {
                Log.i(TAG, "Error response to data_pasv_accept");
                return false;
            }
            Log.d(TAG, "Proxy data_pasv_accept successful");
            return true;
        } catch (JSONException e) {
            Log.i(TAG, "JSONException in pasvAccept: " + e);
            return false;
        }
    }

    private JSONObject makeJsonRequest(String action) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("action", action);
        return json;
    }

    private void persistProxyUsage() {
        if (this.proxyUsage != 0) {
            Editor editor = Globals.getContext().getSharedPreferences(USAGE_PREFS_NAME, 0).edit();
            editor.putLong(USAGE_PREFS_NAME, this.proxyUsage);
            editor.apply();
            Log.d(TAG, "Persisted proxy usage to preferences");
        }
    }

    private long getPersistedProxyUsage() {
        return Globals.getContext().getSharedPreferences(USAGE_PREFS_NAME, 0).getLong(USAGE_PREFS_NAME, 0);
    }

    void incrementProxyUsage(long num) {
        long oldProxyUsage = this.proxyUsage;
        this.proxyUsage += num;
        if (this.proxyUsage % UPDATE_USAGE_BYTES < oldProxyUsage % UPDATE_USAGE_BYTES) {
            FTPServerService.updateClients();
            persistProxyUsage();
        }
    }

    private void setProxyState(State state) {
        this.proxyState = state;
        Log.d(TAG, "Proxy state changed to " + state);
        FTPServerService.updateClients();
    }
}
