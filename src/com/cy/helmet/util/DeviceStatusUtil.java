package com.cy.helmet.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.cy.helmet.Constant;
import com.cy.helmet.HelmetApplication;
import com.cy.helmet.HelmetClient;
import com.cy.helmet.core.protocol.Common;
import com.cy.helmet.location.LocationDataUtil;
import com.cy.helmet.networkstatus.NetWorkStatus;
import com.cy.helmet.networkstatus.NetWorkStatusUtil;
import com.cy.helmet.storage.HelmetStorage;
import com.cy.helmet.storage.StorageBean;
import com.cy.helmet.temperature.TemperatureDetect;
import com.cy.helmet.video.HelmetVideoManager;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import static android.content.Context.BATTERY_SERVICE;

/**
 * Created by ubuntu on 18-1-5.
 */

public class DeviceStatusUtil {

//    optional int32 power = 1;//电量信息，1-100，这里嵌入式软件定义几个级别分别对应电量多少上传即可
//    optional int32 netStatus = 2;//网络状态，1-3等级，1=网络较差，2=网络一般，3=网络良好
//    optional int32 netType = 3;//1=wifi，2=移动网络
//    optional int32 tfCardLeft = 4;//tf卡容量剩余，1-100
//    optional int32 gpsType = 5;//1=lbs,2=gps
//    optional int32 videoStatus = 6;//摄像头是否正在录制状态
//    optional int32 liveStatus = 13;//摄像头是否正在录制状态
//    optional int32 helmetType = 7; //安全帽配置级别 1=低配,2=中配,3=高配
//    optional int32 adornStatus = 8;//1=佩戴中，2=未佩戴
//    //头盔上固件版本信息
//    optional SoftwareVersion softwareVersion = 10;
//    optional int32 hasTfCard = 11;//是否包含TF卡
//    optional int32 tfCardCapacity = 12;//TF卡的总容量，单位G

    public static int getPowerPercent() {
        BatteryManager batteryManager = (BatteryManager) HelmetApplication.mAppContext.getSystemService(BATTERY_SERVICE);
        int powerPercent = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            powerPercent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        } else {
            powerPercent = HelmetApplication.getPowerPercent();
        }

        return powerPercent;
    }

    public static int getNetStatus() {
        /* modify by Jerry
        if (getNetType() == 1) {

            WifiManager wifiManager = (WifiManager) HelmetApplication.mAppContext.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null) {
                return 1;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (HelmetApplication.mAppContext.checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_DENIED) {
                    return 1;
                }
            }
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            return WifiManager.calculateSignalLevel(wifiInfo.getRssi(), 3);
        } else {
            return 3;
        }*/

        return NetWorkStatusUtil.mState;
    }

    public static int getNetType() {
        try {
            NetworkInfo networkInfo = ((ConnectivityManager) HelmetApplication.mAppContext.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    return Constant.NETWORK_TYPE_WIFI;
                } else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                    return Constant.NETWORK_TYPE_MOBILE;
                }
            }
        } catch (Exception e) {

        }
        return Constant.NETWORK_TYPE_MOBILE;
    }

    public static int getGpsType() {
        int type = LocationDataUtil.getLocationType();
        LogUtil.d("gps type*********=" + type);
        return type;
    }

    public static int getVideoStatus() {
        return HelmetVideoManager.getInstance().isRecording() ? 1 : 0;
    }

    public static int getLiveStatus() {
        return HelmetVideoManager.getInstance().isLiving() ? 1 : 0;
    }

    public static int getHelmetType() {
        //1低配 2中配 3高配
        int type = SystemProperties.getInt("ro.config.helmet_type", 3);
        return type;
    }

    public static int getAdornStatus() {
        return HelmetClient.isWearHelmet() ? 1 : 2;
    }

    public static Common.SoftwareVersion getSoftwareVersion() {
        return com.cy.helmet.core.protocol.Common.SoftwareVersion.getDefaultInstance();
    }

    /**
     * getimei or uid
     *
     * @return
     */
    @SuppressLint("MissingPermission")
    public static String getImei(Context sContext) {
        String imei = "unknow";
        if (sContext == null) {
            LogUtil.d("getImei() sContext is null !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            return imei;
        }

        TelephonyManager tm = (TelephonyManager) sContext.getSystemService(Context.TELEPHONY_SERVICE);
        if (Build.VERSION.SDK_INT >= 23) {
            if (tm != null) {
                try {
                    if (PermissionUtils.hasSelfPermissions(sContext, "android.permission.READ_PHONE_STATE")) {
                        imei = tm.getImei(); //modify byJerry
                        LogUtil.d(">>>115_getImei :" + imei);
                    }
                } catch (Exception var5) {
                    LogUtil.e(" No IMEI.");
                }
            }

            if (TextUtils.isEmpty(imei)) {
                imei = getWlanAddress();
                LogUtil.e(">>>124_getImei :" + imei);
                if (TextUtils.isEmpty(imei)) {
                    imei = Settings.Secure.getString(sContext.getContentResolver(), Settings.Secure.ANDROID_ID);
                    LogUtil.e(">>>130_getImei :" + imei);
                    if (TextUtils.isEmpty(imei)) {
                        if (Build.VERSION.SDK_INT >= 9) {
                            imei = Build.SERIAL;
                        }
                        LogUtil.e(">>>>135 getDeviceId, serial no: " + imei);
                    }
                }
            }
        } else {
            if (tm != null) {
                try {
                    if (PermissionUtils.hasSelfPermissions(sContext, "android.permission.READ_PHONE_STATE")) {
                        imei = tm.getImei();//modify by Jerry
                    }
                } catch (Exception var4) {
                    LogUtil.e("No IMEI.");
                }
            }

            if (TextUtils.isEmpty(imei)) {
                LogUtil.e("No IMEI.");
                imei = getWlanAddress();
                if (TextUtils.isEmpty(imei)) {
                    LogUtil.e("Failed to take mac as IMEI. Try to use Secure.ANDROID_ID instead.");
                    imei = Settings.Secure.getString(sContext.getContentResolver(), "android_id");
                    LogUtil.e("getDeviceId: Secure.ANDROID_ID: " + imei);
                }
            }
        }
        return imei;
    }

    private static String getWlanAddress() {
      /*  if (TAG.isEmpty()) {*/
        String[] var0 = new String[]{"/sys/class/net/wlan0/address", "/sys/class/net/eth0/address", "/sys/devices/virtual/net/wlan0/address"};

        for (int i = 0; i < var0.length; ++i) {
            try {
                String var1 = b(var0[i]);
                if (var1 != null) {
                    return var1;
                }
            } catch (Exception var4) {
                LogUtil.e("open file  Failed" + var4.getMessage());
            }
        }
       /* }*/

        return null;
    }

    private static String b(String var0) throws FileNotFoundException {
        String var1 = null;
        FileReader var2 = new FileReader(var0);
        BufferedReader var3 = null;
        if (var2 != null) {
            try {
                var3 = new BufferedReader(var2, 1024);
                var1 = var3.readLine();
            } catch (IOException e) {
                LogUtil.e("Could not read from file " + e.getMessage());
            } finally {
                if (var2 != null) {
                    try {
                        var2.close();
                    } catch (IOException var16) {
                        var16.printStackTrace();
                    }
                }

                if (var3 != null) {
                    try {
                        var3.close();
                    } catch (IOException var15) {
                        var15.printStackTrace();
                    }
                }

            }
        }
        return var1;
    }

    public static Common.DeviceStatus getDeviceStatus() {

        //set device status
        Common.DeviceStatus.Builder statusBuilder = Common.DeviceStatus.newBuilder();
        statusBuilder.setPower(DeviceStatusUtil.getPowerPercent());//power(1-100)
        statusBuilder.setNetStatus(DeviceStatusUtil.getNetStatus());//network state(1-poor, 2-commom, 3-good)
        statusBuilder.setNetType(DeviceStatusUtil.getNetType());//network type(1-wifi, 2-data)
        statusBuilder.setGpsType(DeviceStatusUtil.getGpsType());//GPS type(1-lbs, 2-gps)
        statusBuilder.setVideoStatus(DeviceStatusUtil.getVideoStatus());//video status
        statusBuilder.setLiveStatus(DeviceStatusUtil.getLiveStatus());//live status
        statusBuilder.setHelmetType(DeviceStatusUtil.getHelmetType());//helmet type(1-low, 2-middle, 3-high)
        statusBuilder.setAdornStatus(DeviceStatusUtil.getAdornStatus());//wear status(1-wear, 2-unwear)

        //set version
        Common.SoftwareVersion.Builder versionBuilder = Common.SoftwareVersion.newBuilder();
        versionBuilder.setName(AppUtil.getAppVersionName(HelmetApplication.mAppContext));
        versionBuilder.setVersion(AppUtil.getAppVersionCode(HelmetApplication.mAppContext) + "");
        statusBuilder.setSoftwareVersion(versionBuilder);

        //tf-card
        StorageBean tfCardBean = HelmetStorage.getInstance().getExternalStorage();
        if (tfCardBean != null) {
            LogUtil.e("tf-card: " + tfCardBean.toString());
            statusBuilder.setTfCardLeft(tfCardBean.getFreeSpacePercent());
            statusBuilder.setHasTfCard(1);
            statusBuilder.setTfCardCapacity(tfCardBean.getTotalSizeGB());
        } else {
            statusBuilder.setTfCardLeft(0);
            statusBuilder.setHasTfCard(0);
            statusBuilder.setTfCardCapacity(0);
        }

        //set temperature
        statusBuilder.setCellTemperature(getCellTemperature());
        statusBuilder.setMainboardTemperature(getMainboardTemperature());

        // set mobile network status if exist
        //TODO
        statusBuilder.setMobileNetStatus(getMobileNetworkStatus());

        return statusBuilder.build();
    }

    public static String getGPSId() {
        return "123456";
    }

    public static int getCellTemperature() {
        //TODO fetch cell temperature method here
        return TemperatureDetect.getmInstance().getBatteryTemp();
    }

    public static int getMainboardTemperature() {
        //TODO fetch mainboard temperature method here
        return TemperatureDetect.getmInstance().getAPTemp();
    }

    public static int getMobileNetworkStatus() {
        //TODO fetch mobile network status here
        return NetWorkStatus.getInstance().getMobileLevel();
    }
}
