# FtpApp
手机ftp
介绍：
1.该代码仅用于个人测试，在开发工作中找一点乐趣，将代码放到测试包中，查看测试妹妹手机中的照片，仅供娱乐
2.使用方式
2.1安装
```
	allprojects {
		repositories {
			maven { url 'https://jitpack.io' }
		}
	}
  
  dependencies {
	        implementation 'com.github.bbbBBBSG:FtpApp:1.0'
	}
```
在项目中的某个地方启动它
```
    private FTPServerService mFtpService;
    
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            mFtpService = ((FTPServerService.ServiceBinder) service).getService();
        }

        public void onServiceDisconnected(ComponentName name) {
            mFtpService.stopSelf();
            mFtpService = null;
        }
    };
    
    /**
     * 启动服务
     */
    private void runService() {
        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        Globals.setChrootDir(directory);
        bindService(new Intent(this, FTPServerService.class), mConnection, Context.BIND_AUTO_CREATE);
    }
    /**
     * 获取链接地址
     */
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
```
