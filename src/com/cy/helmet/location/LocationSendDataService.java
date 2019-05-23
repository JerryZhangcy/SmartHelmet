package com.cy.helmet.location;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import com.cy.helmet.networkstatus.NetWorkStatus;
import com.cy.helmet.observer.GpsConnectChange;

/**
 * Created by zhangchongyang on 18-1-5.
 */

public class LocationSendDataService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LocationDataUtil locationDataUtil = LocationDataUtil.getInstance();
        LocationUtil.d("--------->LocationSendDataService dataGap = "
                + locationDataUtil.getGpsDataGap()
                + " gpsState = " + locationDataUtil.getGpsDeviceEnabled()
                + " mobileNet = " + NetWorkStatus.getInstance().isMobileSimAvailable());

        if (locationDataUtil.getGpsDeviceEnabled()
                || NetWorkStatus.getInstance().isMobileSimAvailable()) {
            LocationSendDataThread.getInstance().notifyThread();
        } else {
            LocationSendDataThread.getInstance().startNextAlarm();
            GpsConnectChange.getInstance().onGpsConnectChange(false);
        }
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
