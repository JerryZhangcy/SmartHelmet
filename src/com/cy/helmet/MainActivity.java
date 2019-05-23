package com.cy.helmet;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.cy.helmet.conn.HelmetMessageSender;
import com.cy.helmet.factorytest.FactoryTest;
import com.cy.helmet.factorytest.FactoryUtil;
import com.cy.helmet.led.Led;
import com.cy.helmet.led.LedConfig;
import com.cy.helmet.led.SystemLedState;
import com.cy.helmet.location.LocationSendDataThread;
import com.cy.helmet.location.locationConnect.LocationHttpConnect;
import com.cy.helmet.networkstatus.NetWorkStatusUtil;
import com.cy.helmet.power.Externalpower;
import com.cy.helmet.sensor.GSensor;
import com.cy.helmet.sensor.PSensorCalibration;
import com.cy.helmet.temperature.TemperatureDetect;
import com.cy.helmet.video.HelmetVideoManager;
import com.cy.helmet.voice.HelmetVoiceManager;
import com.cy.helmet.util.LogUtil;
import com.cy.helmet.wifi.HelmetWifiManager;

import java.util.ArrayList;

public class MainActivity extends Activity implements View.OnClickListener {

    private Button mRequestSpeak, mFallBtn, mSendSos, mLed, mDialer;
    private Button mReplayVoice;
    private Button mManualTakePhoto;
    private Button mForceSleep;
    private Button mForceStart;
    private Button mBluetooth;

    private TextView mLiveStatus, mVideoStatus, mGpsdData;

    private AlertDialog mPermissionDialog = null;

    public static String BROADCAST_VIDEO = "android.intent.action.CHANGEVIDEOSTATUS";
    public static String BROADCAST_LIVE = "android.intent.action.CHANGELIVESTATUS";

    public static final int REQUEST_CODE_OPEN_SETTING_PAGE = 0;
    public static final int REQUEST_CODE_REQUEST_PERMISSION = 1;

    //add by Jerry
    private boolean mLedOpen = false;
    private static final int MSG_CLOSE_LED = 0x1002;
    private static final int MSG_START_MAIN_SERVICE = 0x1003;

    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.CAMERA,//照相权限
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        IntentFilter intent = new IntentFilter();
        intent.addAction(BROADCAST_VIDEO);
        intent.addAction(BROADCAST_LIVE);
        registerReceiver(zenmodeReceiver, intent);

        //add for factory test
        if (FactoryUtil.SUPPORT_FACTORY_TEST) {
            FactoryTest.getInstance().readFactoryConfig();
            GSensor.serviceWriteGsensor(String.valueOf(FactoryTest.mGsensorCalValue));
        } else {
            GSensor.initGsensorValue();
        }

        //add for expower
        Externalpower.initExternalpowerValue();

        //start Led Thread
        try {
            Led.getInstance().start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //start Temperature Thread
        TemperatureDetect.getmInstance().start();

        //start http Thread
        try {
            LocationHttpConnect.getInstance().start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (Settings.Secure.getInt(getContentResolver(), "Helmet_boot_complete", 0) == 0) {

            NetWorkStatusUtil.enableWifi(true);
            NetWorkStatusUtil.enableMobileData(true);

            try {
                PSensorCalibration.getInstance().start();
            } catch (Exception e) {
                e.printStackTrace();
            }

            //PhoneWindowManager.java中会将设置为0
            mHandler.sendEmptyMessage(MSG_CLOSE_LED);
            HelmetVoiceManager.getInstance().playLocalVoice(HelmetVoiceManager.DEVICE_BOOT_COMPLETE);
            Settings.Secure.putInt(getContentResolver(), "Helmet_boot_complete", 1);
        }

        //监听系统的状态
        SystemLedState systemLedState = new SystemLedState();
        systemLedState.startObservableSystemLedState();

        //启动GPS获取线程
        try {
            LocationSendDataThread.getInstance().start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        HelmetWifiManager.getInstance().initWifiManager(this);
        HelmetWifiManager.getInstance().start();

        String[] unGrantPermission = getUnGrantedPermission();
        if (unGrantPermission == null) {
            initView();
        } else {
            requestPermission(unGrantPermission);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void initView() {
        mRequestSpeak = (Button) findViewById(R.id.request_send_voice);
        mRequestSpeak.setOnClickListener(this);

        mReplayVoice = (Button) findViewById(R.id.replay_receive_voice);
        mReplayVoice.setOnClickListener(this);

        mManualTakePhoto = (Button) findViewById(R.id.manual_take_photo);
        mManualTakePhoto.setOnClickListener(this);

        mFallBtn = (Button) findViewById(R.id.fall_btn);
        mFallBtn.setOnClickListener(this);

        mSendSos = (Button) findViewById(R.id.send_sos_btn);
        mSendSos.setOnClickListener(this);

        mForceStart = (Button) findViewById(R.id.force_start);
        mForceStart.setOnClickListener(this);

        mForceSleep = (Button) findViewById(R.id.force_sleep);
        mForceSleep.setOnClickListener(this);

        mVideoStatus = (TextView) findViewById(R.id.video_status);
        mLiveStatus = (TextView) findViewById(R.id.live_status);

        mGpsdData = (TextView) findViewById(R.id.gps_data);

        mLed = (Button) findViewById(R.id.led_btn);
        mLed.setOnClickListener(this);
		
		mBluetooth = (Button) findViewById(R.id.bluetooth);
        mBluetooth.setOnClickListener(this);

        mDialer = (Button) findViewById(R.id.dialer_screen);
        mDialer.setOnClickListener(this);

        mHandler.sendEmptyMessageDelayed(MSG_START_MAIN_SERVICE, 5000);//严实5S后启动主服务
    }

    //add by Jerry for start Helmet Control main service
    private void startHelmetControlServer() {
        Intent i = new Intent();
        i.setClass(this, HelmetControlMainService.class);
        startService(i);
    }

    private String[] getUnGrantedPermission() {
        ArrayList<String> requestPermission = new ArrayList<String>();
        if (Build.VERSION.SDK_INT >= 23) {
            try {

                for (int i = 0; i < PERMISSIONS_STORAGE.length; i++) {
                    int state = ContextCompat.checkSelfPermission(this, PERMISSIONS_STORAGE[i]);
                    if (state != PackageManager.PERMISSION_GRANTED) {
                        requestPermission.add(PERMISSIONS_STORAGE[i]);
                    }
                }
            } catch (Exception e) {
            }

            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                startActivity(intent);
            }
        }

        if (!requestPermission.isEmpty()) {
            String[] permissionArray = new String[requestPermission.size()];
            return requestPermission.toArray(permissionArray);
        } else {
            return null;
        }
    }

    public void requestPermission(String[] permissions) {
        if (permissions != null && permissions.length != 0) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_REQUEST_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_REQUEST_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        // 判断用户是否点击了不再提醒。(检测该权限是否还可以申请)
                        boolean doNotShow = shouldShowRequestPermissionRationale(permissions[0]);
                        if (!doNotShow) {
                            // 用户还是想用我的 APP 的
                            // 提示用户去应用设置界面手动开启权限
                            showDialogTipUserGoToAppSettting();
                            return;
                        } else {
                            finish();
                            return;
                        }
                    }
                }
                initView();
                Toast.makeText(this, "权限获取成功", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showDialogTipUserGoToAppSettting() {
        mPermissionDialog = new AlertDialog.Builder(this)
                .setTitle("申请必备权限")
                .setMessage("请设置相机/录音权限，否则无法使用此功能")
                .setPositiveButton("立即开启", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 跳转到应用设置界面
                        goToAppSetting();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                }).setCancelable(false).show();
    }

    // 跳转到当前应用的设置界面
    private void goToAppSetting() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, REQUEST_CODE_OPEN_SETTING_PAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LogUtil.e("onActivityResult=");
        if (requestCode == REQUEST_CODE_OPEN_SETTING_PAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                String[] unGrantPermission = getUnGrantedPermission();
                if (unGrantPermission != null && unGrantPermission.length != 0) {
                    requestPermission(unGrantPermission);
                } else {
                    LogUtil.e("11111111111111111111");
                    Toast.makeText(this, "权限获取成功", Toast.LENGTH_SHORT).show();
                    if (mPermissionDialog != null && mPermissionDialog.isShowing()) {
                        mPermissionDialog.dismiss();
                    }
                    LogUtil.e("2222222222222222222");
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.request_send_voice:
                HelmetVoiceManager.getInstance().requestTalk();
                break;
            case R.id.replay_receive_voice:
                HelmetVoiceManager.getInstance().replayReceivedVoice();
                break;

            case R.id.manual_take_photo:
                HelmetVideoManager.getInstance().takePhoto(true, true, false);
                break;

            case R.id.fall_btn:
                HelmetMessageSender.sendFallDetectMessage();
                Toast.makeText(MainActivity.this, R.string.fall_test_msg, Toast.LENGTH_LONG).show();
                break;
            case R.id.send_sos_btn:
                HelmetClient.sendSOSMessage();
                Toast.makeText(MainActivity.this, R.string.send_sos_msg, Toast.LENGTH_LONG).show();
                break;
            case R.id.led_btn:
                if (mLedOpen) {
                    Led.getInstance().sendMessage(new LedConfig(-1, -1));
                    mLedOpen = false;
                    mLed.setText(R.string.open_led);
                } else {
                    Led.getInstance().sendMessage(new LedConfig(100, 100));
                    mLedOpen = true;
                    mLed.setText(R.string.close_led);
                }
                break;
            case R.id.dialer_screen:
                Intent i = new Intent();
                i.setClassName("com.android.dialer", "com.android.dialer.app.DialtactsActivity");
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                try {
                    startActivity(i);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.force_sleep:
                HelmetClient.switchHelmetState(HelmetClient.DEVICE_STATE_PRE_SLEEP);
                HelmetVoiceManager.getInstance().playLocalVoice(HelmetVoiceManager.HELMET_TAKE_OFF);
                break;

            case R.id.force_start:
                HelmetClient.switchHelmetState(HelmetClient.DEVICE_STATE_WAKE_UP);
                HelmetVoiceManager.getInstance().playLocalVoice(HelmetVoiceManager.HELMET_PUT_ON);
                break;
            case R.id.bluetooth:
//                startActivity(new Intent(MainActivity.this, BluetoothActivity.class));
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(zenmodeReceiver);
    }

    private BroadcastReceiver zenmodeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BROADCAST_LIVE)) {
                String con = intent.getStringExtra("status");
                mLiveStatus.setText(con);
            }
            if (action.equals(BROADCAST_VIDEO)) {
                String con = intent.getStringExtra("status");
                mVideoStatus.setText(con);
            }
        }
    };

    //add for gps
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CLOSE_LED:
                    Led.getInstance().sendMessage(new LedConfig(-1, -1));
                    Led.getInstance().sendMessage(new LedConfig(0, 0));
                    Led.getInstance().sendMessage(new LedConfig(1, 0));
                    Led.getInstance().sendMessage(new LedConfig(2, 0));
                    break;
                case MSG_START_MAIN_SERVICE:
                    startHelmetControlServer();
                    break;
            }
        }
    };
}
