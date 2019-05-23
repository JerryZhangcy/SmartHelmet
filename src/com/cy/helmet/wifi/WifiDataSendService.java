package com.cy.helmet.wifi;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.cy.helmet.config.HelmetConfig;
import com.cy.helmet.util.LogUtil;
import com.cy.helmet.util.NetworkUtil;

public class WifiDataSendService extends Service {
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtil.e("--------->WifiDataSendService mwifiUploadRate = " +
                HelmetConfig.get().mwifiUploadRate +
                " mobileNet = " + NetworkUtil.hasNetwork());
        HelmetWifiManager.mSendAlarm = false;
        if (HelmetConfig.get().mwifiUploadRate > 0) {
            if (NetworkUtil.hasNetwork()) {
                HelmetWifiManager.getInstance().notifyDataSendThread();
            } else {
                WifiManagerUtil.deleteCachingFile();
                HelmetWifiManager.getInstance().startDataSendNextAlarm(false);
            }
        }
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
