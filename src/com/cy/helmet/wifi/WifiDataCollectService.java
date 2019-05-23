package com.cy.helmet.wifi;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.cy.helmet.config.HelmetConfig;
import com.cy.helmet.util.LogUtil;
import com.cy.helmet.util.NetworkUtil;

public class WifiDataCollectService extends Service {
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtil.e("--------->WifiDataCollectService mWifiCollectRate = " +
                HelmetConfig.get().mWifiCollectRate +
                " mobileNet = " + NetworkUtil.hasNetwork());
        HelmetWifiManager.mDataAlarm = false;
        if (HelmetConfig.get().mWifiCollectRate > 0) {
            HelmetWifiManager.getInstance().notifyDataCollectThread();
        }

        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
