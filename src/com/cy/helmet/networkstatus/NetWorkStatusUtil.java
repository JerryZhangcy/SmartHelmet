package com.cy.helmet.networkstatus;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;

import com.cy.helmet.HelmetApplication;

/**
 * Created by zhangchongyang on 18-3-16.
 */

public class NetWorkStatusUtil {
    public static final int NETWORK_UNKNOW = -1;
    public static final int NETWORK_GOOD = 3;
    public static final int NETWORK_NORMAL = 2;
    public static final int NETWORK_POOR = 1;


    public static int mState = NETWORK_UNKNOW;

    public static int mCurrentNetType = ConnectivityManager.TYPE_MOBILE;

    public static void enableWifi(boolean value) {
        WifiManager mWifiManager = (WifiManager) HelmetApplication.mAppContext.getSystemService(Context.WIFI_SERVICE);
        if (mWifiManager != null) {
            mWifiManager.setWifiEnabled(value);
        }
    }

    public static void enableMobileData(boolean value) {
        TelephonyManager mTelephonyManager = TelephonyManager.from(HelmetApplication.mAppContext);
        ConnectivityManager mConnectivityManager = ConnectivityManager.from(HelmetApplication.mAppContext);
        if (mConnectivityManager.isNetworkSupported(android.net.ConnectivityManager.TYPE_MOBILE)
                && mTelephonyManager.getSimState() == android.telephony.TelephonyManager.SIM_STATE_READY) {
            mTelephonyManager.setDataEnabled(value);
        }
    }
}
