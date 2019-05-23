package com.cy.helmet;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.widget.Toast;

import com.cy.helmet.bluetooth.ble.HelmetBluetoothManager;
import com.cy.helmet.bluetooth.ble.HelmetBluetoothService;
import com.cy.helmet.conn.HelmetConnManager;
import com.cy.helmet.conn.SendMessage;
import com.cy.helmet.factory.H2SFallDetectionFactory;
import com.cy.helmet.location.LocationDataUtil;
import com.cy.helmet.location.LocationRegisterService;
import com.cy.helmet.location.LocationUtil;
import com.cy.helmet.location.locationcaching.LocationCachingSend;
import com.cy.helmet.networkstatus.NetWorkStatus;
import com.cy.helmet.observer.HelmetPSensorChange;
import com.cy.helmet.sensor.PSensor;
import com.cy.helmet.sensor.SensorUtil;
import com.cy.helmet.sensor.WearFailVoicePlay;
import com.cy.helmet.temperature.ITemperatureCallback;
import com.cy.helmet.temperature.TemperatureDetect;
import com.cy.helmet.util.CommonUtil;
import com.cy.helmet.video.HelmetVideoManager;
import com.cy.helmet.voice.HelmetVoiceManager;
import com.cy.helmet.voice.Voice;
import com.cy.helmet.wifi.HelmetWifiManager;

import static com.cy.helmet.HelmetApplication.mAppContext;
import static com.cy.helmet.voice.HelmetVoiceManager.DEVICE_FOTA_DOWNLOAD_COMPLETE;
import static com.cy.helmet.voice.HelmetVoiceManager.DEVICE_FOTA_SUCCESS;


/**
 * Created by zhangchongyang on 18-3-19.
 */

public class HelmetControlMainService extends Service implements PSensor.PSensorChangeListener,
        ITemperatureCallback, NetWorkStatus.NetAvaileChangeListen {

    private PSensor mPSensor;
    private static final int DEFAULT_SCREEN_OFF_TIMEOUT = 60 * 1000;
    private static final int SCREEN_NEVER_OFF_TIMEOUT = 0;
    private MainReceiver mMainReceiver;
    private static final String ADUPS_DOWNLOAD_COMPLETE = "com.adups.fota.OUT_DOWLOAD_COMPLETE";
    private static final String ADUPS_UPDATE_SUCCESS = "com.adups.fota.OUT_UPDATE_SUCCESS";
    private static final int MSG_STOP_LIVING = 0x01;
    private static final int MSG_PLAY_STOP_LIVING_VOICE = 0x02;

    private NetWorkStatus mNetWorkStatus;
    private LocationDataUtil mLocationDataUtil;
    private boolean mIsBluetoothEnabling = false;

    private SensorManager mSm = null;
    private Sensor mGravitySensor;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_STOP_LIVING:
                    HelmetVideoManager mVideoManager = HelmetVideoManager.getInstance();
                    if (mVideoManager.isLiving()) {
                        mVideoManager.onDestroy();
                    }
                    if (mVideoManager.isRecording()) {
                        mVideoManager.stopRecording(true);
                    }
                    break;
                case MSG_PLAY_STOP_LIVING_VOICE:
                    HelmetVoiceManager.getInstance().playLocalVoice(HelmetVoiceManager.DEVICE_TEMPERATURE_TOO_HIGH);
                    mHandler.sendEmptyMessageDelayed(MSG_STOP_LIVING, 5000);//延时3S+语音播报时间2S
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        //add for psensor
        mPSensor = PSensor.getInstance();
        mPSensor.startMonitor();
        mPSensor.setPSensorChangeListener(this);
        mLocationDataUtil = LocationDataUtil.getInstance();//this must set blew NetWorkStatus
        mLocationDataUtil.initData();
        mNetWorkStatus = NetWorkStatus.getInstance();
        mNetWorkStatus.init();
        mNetWorkStatus.setListener(this);
        TemperatureDetect.getmInstance().setTemperatureCallback(this);
        HelmetBluetoothManager.getInstance().init();

        IntentFilter intentFilter = new IntentFilter("com.cy.helmet.ACTION_KEY_EVENT");
        intentFilter.addAction(Intent.ACTION_SHUTDOWN);
        intentFilter.addAction(ADUPS_DOWNLOAD_COMPLETE);
        intentFilter.addAction(ADUPS_UPDATE_SUCCESS);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        //intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        mMainReceiver = new MainReceiver();
        mAppContext.registerReceiver(mMainReceiver, intentFilter);

        mSm = (SensorManager) getSystemService(SENSOR_SERVICE);
        mGravitySensor = mSm.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER);
        mSm.registerListener(lsn, mGravitySensor, SensorManager.SENSOR_DELAY_GAME);

        HelmetWifiManager.getInstance().startDataCollectionAlarm(true);
        HelmetWifiManager.getInstance().startDataSendNextAlarm(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //start Location service
        startLocationService();
        //start Helmet service
        startHelmetMainService();
        //start Blue tooth service
        startBluetoothService();
        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPSensor.stopMonitor();
        mNetWorkStatus.deInit();
        mLocationDataUtil.deInitData();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(boolean cover) {
        LocationUtil.d("HelmetControlMainService----onSensorChanged----->distance = " + cover);
        HelmetPSensorChange.getInstance().onPsensorStatus(cover);
        HelmetClient.switchHelmetState(cover ? HelmetClient.DEVICE_STATE_WAKE_UP : HelmetClient.DEVICE_STATE_PRE_SLEEP);
        if (cover) {
            PowerManager powerManager = (PowerManager) mAppContext.getSystemService(Context.POWER_SERVICE);
            powerManager.wakeUp(SystemClock.uptimeMillis(), "Helemet_tag");
            try {
                WearFailVoicePlay.getInstance().stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
            HelmetVoiceManager.getInstance().playLocalVoice(HelmetVoiceManager.DEVICE_WEAR_SUCCESS);
        } else {
            try {
                WearFailVoicePlay.getInstance().start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    //add by Jerry for location
    private void startLocationService() {
        Intent i = new Intent();
        i.setClass(this, LocationRegisterService.class);
        startService(i);
    }

    private void startHelmetMainService() {
        if (!LocationDataUtil.getInstance().getHelmetIdle()) {
            HelmetClient.switchHelmetState(LocationDataUtil.getInstance().getHelmetIdle() ?
                    HelmetClient.DEVICE_STATE_PRE_SLEEP : HelmetClient.DEVICE_STATE_WAKE_UP);
        }
    }

    @Override
    public void onTemperatureWaring() {
        mHandler.obtainMessage(MSG_PLAY_STOP_LIVING_VOICE).sendToTarget();
    }

    @Override
    public void netWorkDisconnect(boolean flag) {
        LocationCachingSend.getInstance().sendLocationCaching();
    }


    class MainReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("com.cy.helmet.ACTION_KEY_EVENT")) {
                String key = intent.getStringExtra("helmet_key");
                String keyAction = intent.getStringExtra("helmet_action");
                boolean isLongPress = intent.getBooleanExtra("helmet_longpress", false);

                LocationUtil.d("HelmetControlMainService----MainReceiver----->action = " + action
                        + " key = " + key + "  isLongPress = " + isLongPress + " keyAction = " + keyAction);

                if (CommonUtil.mIsShutDowning) {
                    LocationUtil.d("HelmetControlMainService----MainReceiver----->shutDowning so return");
                    return;
                }
                if (key.equals("sos") && "down".equals(keyAction)) {
                    if(mAppContext.getResources().getBoolean(R.bool.config_support_sos)) {
                       Toast.makeText(mAppContext, R.string.send_sos_msg, Toast.LENGTH_LONG).show();
                       HelmetClient.sendSOSMessage();
                    }
                } else if (key.equals("phone") && ("up".equals(keyAction) || "longpress".equals(keyAction))) {
                    if (Voice.mTriggerSOS) {
                        LocationUtil.d("HelmetControlMainService----MainReceiver----->now SOS so return");
                        return;
                    }
                    if (isLongPress) {
                        HelmetVoiceManager.getInstance().replayReceivedVoice();
                    } else {
                        HelmetVoiceManager.getInstance().requestTalk();
                    }
                } else if (key.equals("camera")) {
                    if (TemperatureDetect.getmInstance().isTempWarning()) {
                        LocationUtil.d("HelmetControlMainService--camera--temperature is warning");
                        return;
                    }
                    if (isLongPress) {
                        if (HelmetVideoManager.getInstance().isRecording()) {
                            HelmetVideoManager.getInstance().stopRecording(true);
                        } else {
                            HelmetVideoManager.getInstance().startRecording(true, true);
                        }
                    } else {
                        HelmetVideoManager.getInstance().takePhoto(true, true, false);
                    }
                } else if (key.equals("fall")) {
                    Toast.makeText(mAppContext, R.string.fall_test_msg, Toast.LENGTH_LONG).show();
                    SendMessage sendMessage = new SendMessage(H2SFallDetectionFactory.newInstance());
                    HelmetConnManager.getInstance().sendMessage(sendMessage);
                } else if (key.equals("bluetooth")) {
                    HelmetBluetoothManager.isSetByService = false;
                    if (!mIsBluetoothEnabling) {
                        if (HelmetBluetoothManager.getInstance().isBluetoothEnable()) {
                            HelmetBluetoothManager.getInstance().openBluetooth(false);
                        } else {
                            mIsBluetoothEnabling = true;
                            HelmetBluetoothManager.getInstance().openBluetooth(true);
                        }
                    }
                }
            } else if (action.equals(Intent.ACTION_SHUTDOWN)) {
                LocationUtil.d("HelmetControlMainService----MainReceiver----->now is shut down");
                CommonUtil.mIsShutDowning = true;
                HelmetClient.switchHelmetState(HelmetClient.DEVICE_STATE_PRE_SLEEP);
            } else if (action.equals(ADUPS_UPDATE_SUCCESS)) {
                HelmetVoiceManager.getInstance().playLocalVoice(DEVICE_FOTA_SUCCESS);
            } else if (action.equals(ADUPS_DOWNLOAD_COMPLETE)) {
                HelmetVoiceManager.getInstance().playLocalVoice(DEVICE_FOTA_DOWNLOAD_COMPLETE);
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                LocationUtil.d("HelmetControlMainService----MainReceiver----->isSetByService = " +
                        HelmetBluetoothManager.isSetByService + " blueState = " + blueState);
                switch (blueState) {
                    case BluetoothAdapter.STATE_ON:
                        mIsBluetoothEnabling = false;
                        if (HelmetBluetoothManager.isSetByService) {
                            HelmetBluetoothManager.getInstance().bluetoothRename();
                        } else {
                            HelmetVoiceManager.getInstance().playLocalVoice(HelmetVoiceManager.BLUETOOTH_TURN_ON);
                            mHandler.postDelayed(mRunnable, 2000);
                        }
                        break;

                    case BluetoothAdapter.STATE_OFF:
                        if (HelmetBluetoothManager.isSetByService) {
                            HelmetBluetoothManager.isSetByService = false;
                        } else {
                            mHandler.removeCallbacks(mRunnable);
                            HelmetVoiceManager.getInstance().playLocalVoice(HelmetVoiceManager.BLUETOOTH_TURN_OFF);
                            stopBlueToothService();
                        }
                        break;
                }
            } //else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
            //BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            //if(device != null && device.getBondState() == BluetoothDevice.BOND_BONDED) {
            //HelmetVoiceManager.getInstance().playLocalVoice(HelmetVoiceManager.BLUETOOTH_PAIR_SUCCESS);
            //}
            //}
        }
    }

    Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            startBluetoothService();
        }
    };

    private void startBluetoothService() {
        BluetoothManager mBluetoothManager = (BluetoothManager) mAppContext.getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager != null) {
            BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
            if (bluetoothAdapter != null) {
                if (bluetoothAdapter.isEnabled()
                        && !HelmetBluetoothService.mServiceRunning) {
                    Intent i = new Intent();
                    i.setClass(HelmetControlMainService.this, HelmetBluetoothService.class);
                    startService(i);
                }
            }
        }
    }

    private void stopBlueToothService() {
        Intent i = new Intent();
        i.setClass(HelmetControlMainService.this, HelmetBluetoothService.class);
        stopService(i);
    }

    SensorEventListener lsn = new SensorEventListener() {
        public void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy) {
        }

        public void onSensorChanged(SensorEvent e) {
            if (e.sensor == mGravitySensor) {
                SensorUtil.mGSensorX = e.values[SensorManager.DATA_X];
                SensorUtil.mGSensorY = e.values[SensorManager.DATA_Y];
                SensorUtil.mGSensorZ = e.values[SensorManager.DATA_Z];
            }
        }
    };
}
