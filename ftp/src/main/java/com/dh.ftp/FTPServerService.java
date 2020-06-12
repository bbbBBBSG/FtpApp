package com.dh.ftp;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.dahai.ftp.R;
import com.dh.ftp.sdk.AutoClose;
import com.dh.ftp.sdk.Defaults;
import com.dh.ftp.sdk.Globals;
import com.dh.ftp.sdk.ProxyConnector;
import com.dh.ftp.sdk.SessionThread;
import com.dh.ftp.sdk.TcpListener;
import com.dh.ftp.sdk.Util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FTPServerService extends Service implements Runnable {

    private static final String TAG = FTPServerService.class.getSimpleName();

    private static final int WAKE_INTERVAL_MS = 1000;
    private static final String WAKE_LOCK_TAG = "SwiFTP";
    private static Thread serverThread;
    private static SharedPreferences settings;
    private static WifiLock wifiLock;
    private boolean acceptNet;
    private boolean acceptWifi;
    private boolean fullWake;
    private ServerSocket listenSocket;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action==null) return;
            if ((action.equals("android.intent.action.MEDIA_UNMOUNTED") || action.equals("android.intent.action.MEDIA_BAD_REMOVAL")) && isRunning()) {
                stopSelf();
            } else if (action.equals("android.intent.action.TIME_TICK") && !isRunning()) {
                Log.d(FTPServerService.TAG, "Server has been killed");
                stopSelf();
            }
        }
    };
    // 端口
    private int port;
    private ProxyConnector proxyConnector;
    private List<SessionThread> sessionThreads = new ArrayList<>();
    private boolean shouldExit;
    private WakeLock wakeLock;
    private TcpListener wifiListener;

    public class ServiceBinder extends Binder {
        public FTPServerService getService() {
            return FTPServerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        this.shouldExit = false;
        IBinder result = new ServiceBinder();
        int attempts = 10;
        while (serverThread != null) {
            Log.w(TAG, "Won't start, server thread exists");
            if (attempts <= 0) {
                Log.e(TAG, "Server thread already exists");
                break;
            }
            attempts--;
            Util.sleepIgnoreInterupt(1000);
        }
        Log.d(TAG, "Creating server thread");
        serverThread = new Thread(this, "FTP Service");
        serverThread.start();
        return result;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "SwiFTP server created");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.MEDIA_BAD_REMOVAL");
        intentFilter.addAction("android.intent.action.MEDIA_UNMOUNTED");
        intentFilter.addAction("android.intent.action.TIME_TICK");
        registerReceiver(this.mReceiver, intentFilter);
    }

    /**
     * 是否正在运行
     */
    public boolean isRunning() {
        if (serverThread == null) {
            Log.d(TAG, "Server is not running (null serverThread)");
            return false;
        }
        if (serverThread.isAlive()) {
            Log.d(TAG, "Server is alive");
        } else {
            Log.d(TAG, "serverThread non-null but !isAlive()");
        }
        return true;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy() Stopping server");
        this.shouldExit = true;
        if (serverThread == null) {
            Log.w(TAG, "Stopping with null serverThread");
            return;
        }
        serverThread.interrupt();
        try {
            serverThread.join(10000);
        } catch (InterruptedException e) {
        }
        if (serverThread.isAlive()) {
            Log.w(TAG, "Server thread failed to exit");
        } else {
            Log.d(TAG, "serverThread join()ed ok");
            serverThread = null;
        }
        AutoClose.closeQuietly(this.listenSocket);

        if (wifiLock != null) {
            wifiLock.release();
            wifiLock = null;
        }
        clearNotification();
        unregisterReceiver(this.mReceiver);
        Log.d(TAG, "FTPServerService.onDestroy() finished");
    }

    private boolean loadSettings() {
        Log.d(TAG, "Loading settings");
        settings = PreferenceManager.getDefaultSharedPreferences(Globals.getContext());
        port = settings.getInt("portNum", Defaults.portNumber);
        if (port == 0) {
            port = Defaults.portNumber;
        }
        Log.d(TAG, "Using port " + port);
        acceptNet = false;
        acceptWifi = true;
        fullWake = false;
        return true;
    }

    private void setupListener() throws IOException {
        listenSocket = new ServerSocket();
        listenSocket.setReuseAddress(true);
        listenSocket.bind(new InetSocketAddress(port));
    }

    /**
     * 显示通知
     */
    private void setupNotification() {
//        CharSequence tickerText = getString(R.string.notif_server_starting);
//        long when = System.currentTimeMillis();
//        CharSequence contentTitle = getString(R.string.notif_title);
//        CharSequence contentText = "";
//        InetAddress address = getWifiIp();
//        if (address != null) {
//            String port = ":" + getPort();
//            StringBuilder append = new StringBuilder().append("ftp://").append(address.getHostAddress());
//            if (getPort() == 21) {
//                port = "";
//            }
//            contentText = append.append(port).toString();
//        }
//        Intent notificationIntent = new Intent(this, FtpServerControlActivity.class);
//        NotificationCompat.Builder notificationCompatBuilder = new NotificationCompat
//                .Builder(getApplicationContext(),
//                NotificationUtil.createNotificationChannel(this));
//        notificationCompatBuilder
//                .setSmallIcon(R.drawable.notification)
//                .setColor(Color.parseColor("#f05000"))
//                .setTicker(tickerText)
//                .setWhen(when)
//                .setOngoing(true)
//                .setContentTitle(contentTitle)
//                .setContentText(contentText)
//                .setContentIntent(PendingIntent.getActivity(this, 0, notificationIntent, 0))
//                .build();
//        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
//                .notify(123453,notificationCompatBuilder.build());
//        Log.d(TAG, "Notication setup done");
    }

    private void clearNotification() {
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(123453);
        Log.d(TAG, "Cleared notification");
    }

    private boolean safeSetupListener() {
        try {
            setupListener();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void run() {
        int consecutiveProxyStartFailures = 0;
        long proxyStartMillis = 0;
        Log.d(TAG, "Server thread running");
        if (loadSettings()) {
            if (acceptWifi) {
                int atmp = 0;
                while (!safeSetupListener()) {
                    atmp++;
                    if (atmp >= 10) {
                        break;
                    }
                    this.port++;
                }
                if (atmp >= 10) {
                    cleanupAndStopService();
                    return;
                }
                takeWifiLock();
            }
            takeWakeLock();
            Log.i(TAG, "SwiFTP server ready");
            setupNotification();
            while (!shouldExit) {
                if (acceptWifi) {
                    if (!(wifiListener == null || wifiListener.isAlive())) {
                        Log.d(TAG, "Joining crashed wifiListener thread");
                        try {
                            wifiListener.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        wifiListener = null;
                    }
                    if (wifiListener == null) {
                        wifiListener = new TcpListener(listenSocket, this);
                        wifiListener.start();
                    }
                }
                if (acceptNet) {
                    if (!(this.proxyConnector == null || this.proxyConnector.isAlive())) {
                        Log.d(TAG, "Joining crashed proxy connector");
                        try {
                            this.proxyConnector.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        this.proxyConnector = null;
                        if (new Date().getTime() - proxyStartMillis < 3000) {
                            Log.d(TAG, "Incrementing proxy start failures");
                            consecutiveProxyStartFailures++;
                        } else {
                            Log.d(TAG, "Resetting proxy start failures");
                            consecutiveProxyStartFailures = 0;
                        }
                    }
                    if (this.proxyConnector == null) {
                        long nowMillis = new Date().getTime();
                        boolean shouldStartListener = false;
                        if (consecutiveProxyStartFailures < 3 && nowMillis - proxyStartMillis > 5000) {
                            shouldStartListener = true;
                        } else if (nowMillis - proxyStartMillis > 30000) {
                            shouldStartListener = true;
                        }
                        if (shouldStartListener) {
                            Log.d(TAG, "Spawning ProxyConnector");
                            this.proxyConnector = new ProxyConnector(this);
                            this.proxyConnector.start();
                            proxyStartMillis = nowMillis;
                        }
                    }
                }
                try {
                    Thread.sleep(WAKE_INTERVAL_MS);
                } catch (InterruptedException e3) {
                    Log.d(TAG, "Thread interrupted");
                }
            }
            terminateAllSessions();
            if (this.proxyConnector != null) {
                this.proxyConnector.quit();
                this.proxyConnector = null;
            }
            if (this.wifiListener != null) {
                this.wifiListener.quit();
                this.wifiListener = null;
            }
            this.shouldExit = false;
            Log.d(TAG, "Exiting cleanly, returning from run()");
            clearNotification();
            releaseWakeLock();
            releaseWifiLock();
            return;
        }
        cleanupAndStopService();
    }

    private void terminateAllSessions() {
        Log.d(TAG, "Terminating " + this.sessionThreads.size() + " session thread(s)");
        synchronized (this) {
            for (SessionThread sessionThread : this.sessionThreads) {
                if (sessionThread != null) {
                    sessionThread.closeDataSocket();
                    sessionThread.closeSocket();
                }
            }
        }
    }

    private void cleanupAndStopService() {
        Context context = getApplicationContext();
        context.stopService(new Intent(context, FTPServerService.class));
        releaseWifiLock();
        releaseWakeLock();
        clearNotification();
    }

    /**
     * 电源管理
     * FULL_WAKE_LOCK：屏幕常量，不关闭
     * PARTIAL_WAKE_LOCK: 保持CPU 运转，屏幕可能是关闭的。
     */
    private void takeWakeLock() {
        if (wakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = this.fullWake ? pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, WAKE_LOCK_TAG) : pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
            wakeLock.setReferenceCounted(false);
        }
        Log.d(TAG, "Acquiring wake lock");
        wakeLock.acquire();
    }

    private void releaseWakeLock() {
        Log.d(TAG, "Releasing wake lock");
        if (this.wakeLock != null) {
            this.wakeLock.release();
            this.wakeLock = null;
            Log.d(TAG, "Finished releasing wake lock");
            return;
        }
        Log.d(TAG, "Couldn't release null wake lock");
    }

    /**
     * 保证WiF不被休眠时断开
     */
    private void takeWifiLock() {
        Log.d(TAG, "Taking wifi lock");
        if (wifiLock == null) {
            wifiLock = ((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE)).createWifiLock(WAKE_LOCK_TAG);
            wifiLock.setReferenceCounted(false);
        }
        wifiLock.acquire();
    }

    private void releaseWifiLock() {
        Log.d(TAG, "Releasing wifi lock");
        if (wifiLock != null) {
            wifiLock.release();
            wifiLock = null;
        }
    }

    public InetAddress getWifiIp() {
        WifiInfo info = ((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE)).getConnectionInfo();
        if (info == null) {
            return null;
        }
        int ipAsInt = info.getIpAddress();
        if (ipAsInt != 0) {
            return Util.intToInet(ipAsInt);
        }
        return null;
    }

    public static void updateClients() {

    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void registerSessionThread(SessionThread newSession) {
        synchronized (this) {
            List<SessionThread> toBeRemoved = new ArrayList<>();
            for (SessionThread sessionThread : this.sessionThreads) {
                if (!sessionThread.isAlive()) {
                    Log.d(TAG, "Cleaning up finished session...");
                    try {
                        sessionThread.join();
                        Log.d(TAG, "Thread joined");
                        toBeRemoved.add(sessionThread);
                        sessionThread.closeSocket();
                    } catch (InterruptedException e) {
                        Log.d(TAG, "Interrupted while joining");
                    }
                }
            }
            for (SessionThread removeThread : toBeRemoved) {
                this.sessionThreads.remove(removeThread);
            }
            this.sessionThreads.add(newSession);
        }
        Log.d(TAG, "Registered session thread");
    }

    public static SharedPreferences getSettings() {
        return settings;
    }
}
