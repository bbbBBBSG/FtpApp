package com.dahai.ftpapp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.net.InetAddress;

import com.dh.ftp.FTPServerService;
import com.dh.ftp.sdk.Globals;
import com.tbruyelle.rxpermissions2.RxPermissions;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public class FtpServerControlActivity extends AppCompatActivity implements OnClickListener {
    private static final String TAG = FtpServerControlActivity.class.getSimpleName();

    private TextView instructionText;
    private TextView instructionTextPre;
    private TextView ipText;
    private View startStopButton;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            mFtpService = ((FTPServerService.ServiceBinder) service).getService();
        }

        public void onServiceDisconnected(ComponentName name) {
            mFtpService.stopSelf();
            mFtpService = null;
        }
    };

    private FTPServerService mFtpService;

    private BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        public void onReceive(Context ctx, Intent intent) {
            Log.e(FtpServerControlActivity.TAG, "Wifi status broadcast received");
            updateUi();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.server_control_activity);
        ipText = findViewById(R.id.ip_address);
        ipText.setOnClickListener(this);
        instructionText = findViewById(R.id.instruction);
        instructionTextPre = findViewById(R.id.instruction_pre);
        instructionTextPre.setText(R.string.instruction_pre_phone);
        startStopButton = findViewById(R.id.start_stop_button);
        startStopButton.setOnClickListener(this);
        updateUi();
        findViewById(R.id.wifi_container).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent("android.settings.WIFI_SETTINGS"));
            }
        });
        findViewById(R.id.ftp_setting).setOnClickListener(this);
        Log.d(TAG, "Registered for wifi updates");
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(wifiReceiver, filter);

        RxPermissions rxPermissions = new RxPermissions(this);
        Disposable subscribe = rxPermissions.request(Manifest.permission.READ_EXTERNAL_STORAGE)
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {

                    }
                });
    }


    @Override
    public void onStart() {
        super.onStart();
        updateUi();

        RxPermissions rxPermissions = new RxPermissions(this);
        boolean granted = rxPermissions.isGranted(Manifest.permission.READ_EXTERNAL_STORAGE);
        Log.e(TAG, "onStart: " + granted );
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUi();
    }

    @Override
    public void onDestroy() {
        if (mFtpService != null && mFtpService.isRunning()) {
            unbindService(mConnection);
        }
        unregisterReceiver(wifiReceiver);
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    private void updateUi() {
        TextView textView;
        Log.d(TAG, "Updating UI");
        WifiInfo info = ((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE)).getConnectionInfo();
        String wifiId = info != null ? info.getSSID() : null;
        boolean isWifi = info != null && info.getIpAddress() != 0;
        if (!isWifi) {
            wifiId = getString(R.string.no_wifi_hint);
        }
        setText(R.id.wifi_state, wifiId);
        ((ImageView) findViewById(R.id.wifi_state_image)).setImageResource(isWifi ? R.drawable.wifi_state4 : R.drawable.wifi_state0);
        boolean running = mFtpService != null && mFtpService.isRunning();
        if (running) {
            Log.d(TAG, "updateUi: server is running");
            InetAddress address = mFtpService.getWifiIp();
            if (address != null) {
                String port = ":" + mFtpService.getPort();
                textView = ipText;
                StringBuilder append = new StringBuilder().append("ftp://").append(address.getHostAddress());
                if (mFtpService.getPort() == 21) {
                    port = "";
                }
                textView.setText(append.append(port).toString());
            } else {
                unbindService(mConnection);
                ipText.setText("");
            }
        }
        startStopButton.setEnabled(isWifi);
        TextView startStopButtonText = findViewById(R.id.start_stop_button_text);
        if (isWifi) {
            int i;
            startStopButtonText.setText(running ? R.string.stop_server : R.string.start_server);
            startStopButtonText.setTextColor(getResources().getColor(R.color.start_stop_server));
            startStopButtonText.setCompoundDrawablesWithIntrinsicBounds(running ? R.drawable.disconnect : R.drawable.connect, 0, 0, 0);
            findViewById(R.id.ftp_setting).setEnabled(!running);
            textView = findViewById(R.id.tv_setting);
            Resources resources = getResources();
            if (running) {
                i = R.color.gray;
            } else {
                i = R.color.text_color_yellow;
            }
            textView.setTextColor(resources.getColor(i));
        } else {
            running = mFtpService != null && mFtpService.isRunning();
            if (running) {
                unbindService(mConnection);
            }
            startStopButtonText.setText(R.string.no_wifi);
            startStopButtonText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            startStopButtonText.setTextColor(-7829368);
        }
        ipText.setVisibility(running ? View.VISIBLE : View.GONE);
        ipText.setSelected(running);
        instructionText.setVisibility(running ? View.VISIBLE : View.GONE);
        instructionTextPre.setVisibility(running ? View.GONE : View.VISIBLE);
        if (Globals.getLastError() != null) {
            Toast.makeText(this, Globals.getLastError(), Toast.LENGTH_SHORT).show();
            Globals.setLastError(null);
        }
    }

    private void setText(int id, String text) {
        ((TextView) findViewById(id)).setText(text);
    }

    /**
     * 启动服务
     */
    private void runService() {
        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        Globals.setChrootDir(directory);
        bindService(new Intent(this, FTPServerService.class), mConnection, Context.BIND_AUTO_CREATE);
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.start_stop_button:
                Globals.setLastError(null);
                if (mFtpService == null || !mFtpService.isRunning()) {
                    runService();
                    break;
                }
                unbindService(mConnection);
                updateUi();
//                runService();
                break;
            case R.id.wifi_container:
                startActivity(new Intent("android.settings.WIFI_SETTINGS"));
                break;
            case R.id.ip_address:
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboardManager!=null) {
                    clipboardManager.setPrimaryClip(ClipData.newPlainText("copy", ipText.getText().toString().trim()));
                    Toast.makeText(this, R.string.msg_copy_success, Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }
}
