package com.cy.helmet.networkstatus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

import com.cy.helmet.HelmetApplication;
import com.cy.helmet.HelmetClient;
import com.cy.helmet.conn.HelmetConnManager;
import com.cy.helmet.led.Led;
import com.cy.helmet.led.LedConfig;
import com.cy.helmet.location.LocationDataUtil;
import com.cy.helmet.location.LocationUtil;
import com.cy.helmet.voice.HelmetVoiceManager;

import java.util.ArrayList;
import java.util.List;

import static com.cy.helmet.HelmetApplication.mAppContext;

/**
 * Created by zhangchongyang on 18-3-16.
 */

public class NetWorkStatus {
    public static final int UNKONW_CONNECTED = -1;
    public static final int WIFI_CONNECTED = 0;
    public static final int MOBILE_CONNECTED = 1;
    private static NetWorkStatus mInstance;
    private netWorkAvailableBroadcastReceiver mNetWorkAvailableBroadcastReceiver;
    private static final int MIN_VOICE_GAP = 20000;
    private static final int MAX_VOICE_GAP = 300000;
    private int mCurrentGap = MIN_VOICE_GAP;
    private long mCurrentSystemTime = 0;

    private List<NetAvaileChangeListen> mList = new ArrayList<NetAvaileChangeListen>();


    private TelephonyManager mTelephonyManager;
    private PhoneSignal mPhoneSignal;
    private int mNetWorkLastState = NetWorkStatusUtil.NETWORK_UNKNOW;

    public interface NetAvaileChangeListen {
        public void netWorkDisconnect(boolean flag);
    }

    public static synchronized NetWorkStatus getInstance() {
        if (mInstance == null) {
            synchronized (NetWorkStatus.class) {
                if (mInstance == null) {
                    mInstance = new NetWorkStatus();
                }
            }
        }
        return mInstance;
    }

    public void init() {
        mNetWorkAvailableBroadcastReceiver = new netWorkAvailableBroadcastReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        mAppContext.registerReceiver(mNetWorkAvailableBroadcastReceiver, filter);

        mTelephonyManager = (TelephonyManager) mAppContext
                .getSystemService(Context.TELEPHONY_SERVICE);
        mPhoneSignal = new PhoneSignal();
        mTelephonyManager.listen(mPhoneSignal, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
    }

    public void setListener(NetAvaileChangeListen netAvaileChangeListen) {
        mList.add(netAvaileChangeListen);
    }

    public void removeListener(NetAvaileChangeListen netAvaileChangeListen) {
        if (mList != null && !mList.isEmpty()) {
            mList.remove(netAvaileChangeListen);
        }
    }


    public void deInit() {
        mAppContext.unregisterReceiver(mNetWorkAvailableBroadcastReceiver);
    }


    class netWorkAvailableBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connectMgr = (ConnectivityManager) mAppContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mobNetInfo = connectMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            NetworkInfo wifiNetInfo = connectMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            WifiManager wifiManager = (WifiManager) mAppContext.getSystemService(Context.WIFI_SERVICE);
            LocationUtil.d("NetWorkStatus---------->mobNetInfo = " +
                    mobNetInfo.isConnected() + " wifiNetInfo = " +
                    wifiNetInfo.isConnected());
            String action = intent.getAction();
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                if (mList != null && !mList.isEmpty()) {
                    for (NetAvaileChangeListen netAvaileChangeListen : mList) {
                        netAvaileChangeListen.netWorkDisconnect(!mobNetInfo.isConnected() && !wifiNetInfo.isConnected());
                    }
                }
            }
            determineCurrentNetworkStatus(connectMgr, wifiManager);
        }
    }

    public boolean isMobileSimAvailable() {
        TelephonyManager mTelephonyManager = TelephonyManager.from(HelmetApplication.mAppContext);
        ConnectivityManager connectMgr = (ConnectivityManager) mAppContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mobNetInfo = connectMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        NetworkInfo wifiNetInfo = connectMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if ((mobNetInfo != null && mobNetInfo.isConnected())
                || (mTelephonyManager.getSimState() == android.telephony.TelephonyManager.SIM_STATE_READY
                && wifiNetInfo.isConnected())) {
            return true;
        }
        return false;
    }

    public int getCurrentConnected() {
        ConnectivityManager connectMgr = (ConnectivityManager) mAppContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mobNetInfo = connectMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        NetworkInfo wifiNetInfo = connectMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiNetInfo != null && wifiNetInfo.isConnected()) {
            return WIFI_CONNECTED;
        } else if (mobNetInfo != null && mobNetInfo.isConnected()) {
            return MOBILE_CONNECTED;
        } else {
            return UNKONW_CONNECTED;
        }
    }

    private void determineCurrentNetworkStatus(ConnectivityManager connectMgr, WifiManager wifiManager) {
        NetworkInfo mobileNetInfo = connectMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        NetworkInfo wifiNetInfo = connectMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        int currentState = NetWorkStatusUtil.NETWORK_UNKNOW;

        if (wifiNetInfo.isConnected()) {
            NetWorkStatusUtil.mCurrentNetType = ConnectivityManager.TYPE_WIFI;
            WifiInfo info = wifiManager.getConnectionInfo();
            int level = getLevel(info.getRssi());
            LocationUtil.d("NetWorkStatus----determineCurrentNetworkStatus------>level = " + level);
            if (level >= 4) {
                currentState = NetWorkStatusUtil.NETWORK_GOOD;
            } else if (level < 4 && level >= 2) {
                currentState = NetWorkStatusUtil.NETWORK_NORMAL;
            } else {
                currentState = NetWorkStatusUtil.NETWORK_POOR;
            }
        } else if (mobileNetInfo.isConnected()) {
            NetWorkStatusUtil.mCurrentNetType = ConnectivityManager.TYPE_MOBILE;
            int csq = LocationDataUtil.getInstance().getGsmCsq();
            if (csq == 3) {
                currentState = NetWorkStatusUtil.NETWORK_GOOD;
            } else if (csq == 2) {
                currentState = NetWorkStatusUtil.NETWORK_NORMAL;
            } else {
                currentState = NetWorkStatusUtil.NETWORK_POOR;
            }
        } else {
            currentState = NetWorkStatusUtil.NETWORK_UNKNOW;
        }

        NetWorkStatusUtil.mState = currentState;

        if (currentState == NetWorkStatusUtil.NETWORK_UNKNOW
                && currentState != mNetWorkLastState) {
            if (mCurrentSystemTime == 0) {
                mCurrentSystemTime = System.currentTimeMillis();
                HelmetVoiceManager.getInstance().playLocalVoice(HelmetVoiceManager.DEVICE_NETWORK_DISCONNECT);
            } else {
                if (System.currentTimeMillis() - mCurrentSystemTime <= mCurrentGap) {
                    mCurrentGap = MAX_VOICE_GAP;
                } else {
                    mCurrentSystemTime = System.currentTimeMillis();
                    mCurrentGap = MIN_VOICE_GAP;
                    HelmetVoiceManager.getInstance().playLocalVoice(HelmetVoiceManager.DEVICE_NETWORK_DISCONNECT);
                }
            }
            Led.getInstance().sendMessage(new LedConfig(2, 0));
        } else if (currentState != mNetWorkLastState) {
            switch (currentState) {
                case NetWorkStatusUtil.NETWORK_GOOD:
                    Led.getInstance().sendMessage(new LedConfig(2, 1));
                    break;
                case NetWorkStatusUtil.NETWORK_NORMAL:
                    Led.getInstance().sendMessage(new LedConfig(2, 2));
                    break;
                case NetWorkStatusUtil.NETWORK_POOR:
                    Led.getInstance().sendMessage(new LedConfig(2, 0));
                    break;
            }

            if (currentState == NetWorkStatusUtil.NETWORK_POOR) {
                if (mCurrentSystemTime == 0) {
                    mCurrentSystemTime = System.currentTimeMillis();
                    HelmetVoiceManager.getInstance().playLocalVoice(HelmetVoiceManager.DEVICE_POOR_NETWORK);
                } else {
                    if (System.currentTimeMillis() - mCurrentSystemTime <= mCurrentGap) {
                        mCurrentGap = MAX_VOICE_GAP;
                    } else {
                        mCurrentSystemTime = System.currentTimeMillis();
                        mCurrentGap = MIN_VOICE_GAP;
                        HelmetVoiceManager.getInstance().playLocalVoice(HelmetVoiceManager.DEVICE_POOR_NETWORK);
                    }
                }
            } else if (((currentState == NetWorkStatusUtil.NETWORK_GOOD
                    && (mNetWorkLastState == NetWorkStatusUtil.NETWORK_POOR || mNetWorkLastState == NetWorkStatusUtil.NETWORK_UNKNOW))
                    || (currentState == NetWorkStatusUtil.NETWORK_NORMAL
                    && (mNetWorkLastState == NetWorkStatusUtil.NETWORK_POOR || mNetWorkLastState == NetWorkStatusUtil.NETWORK_UNKNOW)))) {
                if (mCurrentSystemTime == 0) {
                    mCurrentSystemTime = System.currentTimeMillis();
                    HelmetVoiceManager.getInstance().playLocalVoice(HelmetVoiceManager.DEVICE_NETWORK_REGAIN);
                } else {
                    if (System.currentTimeMillis() - mCurrentSystemTime <= mCurrentGap) {
                        mCurrentGap = MAX_VOICE_GAP;
                    } else {
                        mCurrentSystemTime = System.currentTimeMillis();
                        mCurrentGap = MIN_VOICE_GAP;
                        HelmetVoiceManager.getInstance().playLocalVoice(HelmetVoiceManager.DEVICE_NETWORK_REGAIN);
                    }
                }
            }

            //network state change trigger Heartbeat
            if (!HelmetClient.isSleepNow()) {
                HelmetConnManager.getInstance().triggerHelmetConnect();
            }
        }

        mNetWorkLastState = currentState;
    }


    private int getLevel(int rssi) {
        return WifiManager.calculateSignalLevel(rssi, 5);
    }

    class PhoneSignal extends PhoneStateListener {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);
            ConnectivityManager connectMgr = (ConnectivityManager) mAppContext.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo wifiNetInfo = connectMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            WifiManager wifiManager = (WifiManager) mAppContext.getSystemService(Context.WIFI_SERVICE);

            int signal = 0;//(signalStrength.getDbm() + 113) / 2;
            int currentDb = signalStrength.getDbm();
            if (currentDb <= -117) { //poor
                signal = 1;
            } else if (currentDb > -117 && currentDb <= -105) { //normal
                signal = 2;
            } else {  //good
                signal = 3;
            }
            LocationDataUtil.getInstance().setGsmCsq(signal);

            if (!wifiNetInfo.isConnected()) {
                determineCurrentNetworkStatus(connectMgr, wifiManager);
            }

            LocationUtil.d("LocationNetWorkMonitorUtil---------->signal = " + signal
                    + " getDbm = " + signalStrength.getDbm() + " getAsuLevel = " + signalStrength.getAsuLevel());

        }
    }

    public int getMobileLevel() {
        int signal = LocationDataUtil.getInstance().getGsmCsq();
        if (signal >= 3) {
            return NetWorkStatusUtil.NETWORK_GOOD;
        } else if (signal >= 2) {
            return NetWorkStatusUtil.NETWORK_NORMAL;
        } else {
            return NetWorkStatusUtil.NETWORK_POOR;
        }
    }
}
