package com.cy.helmet.location;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Environment;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.cy.helmet.Constant;
import com.cy.helmet.HelmetApplication;
import com.cy.helmet.config.HelmetConfig;
import com.cy.helmet.led.Led;
import com.cy.helmet.led.LedConfig;
import com.cy.helmet.networkstatus.NetWorkStatus;
import com.cy.helmet.sensor.PSensor;
import com.cy.helmet.voice.HelmetVoiceManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;

/**
 * Created by zhangchongyang on 18-1-10.
 */

public class LocationDataUtil {
    private static LocationDataUtil mInstance;
    private LocationManager mLocationManager;
    private boolean mIsReg = false;
    private BatteryReceiver mBatteryReceiver;
    private int mBattery = 0;

    private TelephonyManager mTelephonyManager;
    private int mCurrentSignal;

    private Location mLastLocation = null;

    private static final int BATTERY_UNKNOW = -1;
    private static final int BATTERY_FULL = 0;
    private static final int BATTERY_NORMAL = 1;
    private static final int BATTERY_LOW = 2;
    private static final int BATTERY_CHARGING_FULL = 3;
    private static final int BATTERY_CHARGING_NORMAL = 4;
    private int mLastBatteryState = BATTERY_UNKNOW;

    private static int mCurrentLocationType = LocationManager.GPS_UNKNOW;

    public static final int GPS_DATA_FIRST = 1;
    public static final int GPS_DATA_SECOND = 2;
    public static int mGpsDataState = GPS_DATA_FIRST;

    public static ArrayList<String> mGpsFirstData = new ArrayList<>();
    public static ArrayList<String> mGpsSecondData = new ArrayList<>();

    public static synchronized LocationDataUtil getInstance() {
        if (mInstance == null) {
            synchronized (LocationDataUtil.class) {
                if (mInstance == null) {
                    mInstance = new LocationDataUtil();
                }
            }
        }
        return mInstance;
    }

    public void initData() {
        mLocationManager = new LocationManager(HelmetApplication.mAppContext);
        mLocationManager.recordLocation(true);

        mBatteryReceiver = new BatteryReceiver();
        regBatteryReceiver(HelmetApplication.mAppContext);

        mTelephonyManager = (TelephonyManager) HelmetApplication.mAppContext
                .getSystemService(Context.TELEPHONY_SERVICE);
    }


    public void deInitData() {
        unRegBatteryReceiver(HelmetApplication.mAppContext);
    }


    public void setLocationChangeListener(OnLocationChangeListener onLocationChangeListener) {
        if (mLocationManager != null) {
            mLocationManager.setLocationChangeListener(onLocationChangeListener);
        }
    }

    /**
     * 判断是否有网络可用
     *
     * @return
     */
    public boolean isNetworkAvailable() {
        ConnectivityManager connectivity = (ConnectivityManager) HelmetApplication.mAppContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            return false;
        } else {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 获取当前手机的IMEI号，目前是单卡项目，获取的是SIM卡1的IMEI
     *
     * @return
     */
    @SuppressLint("MissingPermission")
    public String getIMEI() {
        if (mTelephonyManager != null) {
            return mTelephonyManager.getImei();
        }
        return LocationUtil.DEF_IMEI;
    }

    /**
     * 获取当前的定位信息
     *
     * @return
     */
    public Location getCurrentLocation() {
        if (mLocationManager != null) {
            return mLocationManager.getCurrentLocation();
        }
        return null;
    }

    /**
     * 复位位置信息
     */
    public void resetCurrentLocation() {
        if (mLocationManager != null) {
            mLocationManager.resetCurrentLocation();
        }
    }

    /**
     *
     *
     */
    public void recordLocation(boolean recordLocation) {
        if (mLocationManager != null) {
            mLocationManager.recordLocation(recordLocation);
        }
    }

    /**
     * 设置上一次的定位信息
     *
     * @param location
     */
    public void setLastLocation(Location location) {
        mLastLocation = location;
    }

    /**
     * 获取上一次的定位信息
     *
     * @return
     */
    public Location getLastLocation() {
        return mLastLocation;
    }

    /**
     * 获取卫星的个数，目前定义信噪比大于35认为是有效的卫星
     *
     * @return
     */

    public int getGpsSatelliteNum() {
        if (mLocationManager != null)
            return mLocationManager.getGpsSatelliteNum();
        return 0;
    }

    /**
     * 获取定位的类型 0表示基站定位 1表示GPS定位 -1表示定位失败
     *
     * @return
     */
    public static int getLocationType() {
        return mCurrentLocationType;
    }

    /**
     * 设置当前的定位类型
     * 0表示基站定位 1表示GPS定位 -1表示定位失败
     *
     * @param type
     */
    public static void setLocationType(int type) {
        mCurrentLocationType = type;
    }

    /**
     * 获取电池的电量
     *
     * @return
     */
    public int getBatteryLevel() {
        return mBattery;
    }

    /**
     * 获取当前SIM卡的CSQ（db+113）/2
     *
     * @return
     */
    public int getGsmCsq() {
        return mCurrentSignal;
    }


    /**
     * 设置当前SIM卡的CSQ
     *
     * @param signal
     */

    public void setGsmCsq(int signal) {
        mCurrentSignal = signal;
    }


    /**
     * 获取定位数据上传的时间间隔
     *
     * @return
     */
    public int getGpsDataGap() {
        return HelmetConfig.get().locationCfg * 1000;
    }


    /**
     * 获取非工作状态下定位数据上传的时间间隔
     *
     * @return
     */
    @Deprecated
    public int getGpsIdleGap() {
        return LocationUtil.GPS_IDEL_UPLOAD_GAP;
    }


    /**
     * 获取GPS是否打开
     *
     * @return
     */
    public boolean getGpsDeviceEnabled() {
        String value = Settings.Secure.getString(HelmetApplication.mAppContext
                .getContentResolver(), Settings.System.LOCATION_PROVIDERS_ALLOWED);
        if (TextUtils.isEmpty(value)) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * 获取当前已插入SIM卡的基站信息
     */
    public LocationSimInfo getCellInfo() {
        if (mLocationManager != null)
            return mLocationManager.getCellInfo();
        return null;
    }


    /**
     * 获取当前是否是非工作状态
     *
     * @return
     */
    public boolean getHelmetIdle() {
        float value = Settings.Secure.getFloat(HelmetApplication.mAppContext.getContentResolver(), "helmet_idle",
                PSensor.UNCOVER);
        return value == PSensor.UNCOVER;
    }

    /**
     * 获取GPS需要缓存的时间
     *
     * @return
     */
    public long getGpsCachingTime() {

        return HelmetConfig.get().gpsCacheTime * 60 * 1000;//默认一个小时
    }

    public String getGpsUrl() {
        String ip = null;
        int port = -1;

        if (NetWorkStatus.getInstance().getCurrentConnected() == NetWorkStatus.MOBILE_CONNECTED) {
            ip = HelmetConfig.get().getCloudServerHost();
            port = HelmetConfig.get().getCloudServerPort();
        } else {
            ip = HelmetConfig.get().getGpsServerHost();
            port = HelmetConfig.get().getGpsServerPort();
        }

        if (ip != null && port != -1) {
            StringBuilder builder = new StringBuilder();
            builder.append("http://");
            builder.append(ip);
            builder.append(":");
            builder.append(port);
            builder.append(LocationUtil.GPS_URL_SUFFIX);
            return builder.toString();
        }
        return null;
    }

    private void regBatteryReceiver(Context context) {
        if (!mIsReg) {
            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            context.registerReceiver(mBatteryReceiver, intentFilter);
            mIsReg = true;
        }
    }

    private void unRegBatteryReceiver(Context context) {
        if (mIsReg) {
            context.unregisterReceiver(mBatteryReceiver);
            mIsReg = false;
        }
    }

    class BatteryReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                mBattery = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                int chargState = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
                int currrentState = BATTERY_UNKNOW;
                LocationUtil.e("LocationDataUtil---BatteryReceiver---->mBattery = " + mBattery + "  chargState = " + chargState);

                if (chargState != 0) {
                    if (mBattery == 100) {
                        currrentState = BATTERY_CHARGING_FULL;
                    } else {
                        currrentState = BATTERY_CHARGING_NORMAL;
                    }

                    if (currrentState != mLastBatteryState) {
                        if (currrentState == BATTERY_CHARGING_FULL) {
                            //充电完成亮绿灯
                            Led.getInstance().sendMessage(new LedConfig(1, 1));
                        } else if (currrentState == BATTERY_CHARGING_NORMAL) {
                            //充电中亮蓝灯
                            Led.getInstance().sendMessage(new LedConfig(1, 2));
                        }
                    }
                } else {
                    if (mBattery > 20) {
                        currrentState = BATTERY_FULL;
                    } else if (mBattery <= 20) {
                        currrentState = BATTERY_LOW;
                    }
                    if (currrentState != mLastBatteryState) {
                        if (currrentState == BATTERY_FULL) {
                            //电量充足亮绿灯
                            Led.getInstance().sendMessage(new LedConfig(1, 1));
                        } else if (currrentState == BATTERY_LOW) {
                            HelmetVoiceManager.getInstance().playLocalVoice(HelmetVoiceManager.DEVICE_LOW_BATTERY);
                            //电量低红灯
                            Led.getInstance().sendMessage(new LedConfig(1, 0));
                        }
                    }
                }
                mLastBatteryState = currrentState;
            }
        }
    }

    //open or close gps
    public void switchGPSState(boolean open) {
        // TODO Auto-generated method stub
        int current = Settings.Secure.getInt(HelmetApplication.mAppContext.getContentResolver(), Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_OFF);
        LocationUtil.d("LocationDataUtil------switchGPSState - current = " + current + ", open = " + open);
        if (open && (current == Settings.Secure.LOCATION_MODE_OFF)
                || !open && (current == Settings.Secure.LOCATION_MODE_HIGH_ACCURACY)) {
            int location = Settings.Secure.LOCATION_MODE_OFF;
            if (open) {
                location = Settings.Secure.LOCATION_MODE_HIGH_ACCURACY;
            }
            Settings.Secure.putInt(HelmetApplication.mAppContext.getContentResolver(), Settings.Secure.LOCATION_MODE, location);
        }
    }
}
