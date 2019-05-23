package com.cy.helmet.location;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.database.ContentObserver;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;

import com.cy.helmet.location.locationcaching.LocationCachingThread;
import com.cy.helmet.networkstatus.NetWorkStatus;
import com.cy.helmet.sensor.SensorUtil;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

/**
 * Created by zhangchongyang on 18-1-5.
 */

public class LocationRegisterService extends Service implements OnLocationChangeListener {

    private AlarmManager mAlarmManager;
    private boolean mIsDataAlarmRunning = false;
    private Handler mHandler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        getContentResolver()
                .registerContentObserver(
                        Settings.Secure
                                .getUriFor(Settings.System.LOCATION_PROVIDERS_ALLOWED),
                        false, mGpsMonitor);
        //GPS缓存alarm一直存在
        startSendDataAlarm();
        LocationDataUtil.getInstance().setLocationChangeListener(this);
        try {
            LocationCachingThread.getInstance().start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocationUtil.d("LocationRegisterService--------->onDestroy");
        getContentResolver().unregisterContentObserver(mGpsMonitor);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LocationUtil.d("LocationRegisterService--------->onStartCommand");
        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startSendDataAlarm() {
        mIsDataAlarmRunning = true;
        Intent sendDataIntent = new Intent();
        sendDataIntent.setClass(this, LocationSendDataService.class);
        PendingIntent sendData = PendingIntent.getService(this, 0, sendDataIntent, FLAG_UPDATE_CURRENT);
        mAlarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime(), sendData);
    }

    private void cancelSendDataAlarm() {
        mIsDataAlarmRunning = false;
        Intent sendDataIntent = new Intent();
        sendDataIntent.setClass(this, LocationSendDataService.class);
        PendingIntent sendData = PendingIntent.getService(this, 0, sendDataIntent, FLAG_UPDATE_CURRENT);
        mAlarmManager.cancel(sendData);
    }

    private final ContentObserver mGpsMonitor = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
            boolean isGpsEnabled = LocationDataUtil.getInstance().getGpsDeviceEnabled();
            boolean isMobileAvailable = NetWorkStatus.getInstance().isMobileSimAvailable();
            LocationUtil.d("LocationRegisterService-----mGpsMonitor----->isGpsEnabled = " + isGpsEnabled
                    + " isMobileAvailable = " + isMobileAvailable + " mIsDataAlarmRunning" + mIsDataAlarmRunning);
        }
    };

    @Override
    public void onLocationChange(Location newLocation) {
        long currentTime = System.currentTimeMillis();
        String longitude = "0";
        String latitude = "0";
        longitude = LocationUtil.roundByScale(newLocation.getLongitude(), 6);
        latitude = LocationUtil.roundByScale(newLocation.getLatitude(), 6);
        LocationCachingThread.getInstance().sendMessage(latitude + "|" + longitude + "|"
                + LocationDataUtil.getInstance().getGpsSatelliteNum() + "|"
                + String.valueOf(SensorUtil.mGSensorX) + "|"
                + String.valueOf(SensorUtil.mGSensorY) + "|"
                + String.valueOf(SensorUtil.mGSensorZ) + "|"
                + String.valueOf(currentTime));
        LocationUtil.d("LocationRegisterService-----onLocationChange----->longitude=" + longitude
                + " latitude = " + latitude + " currentTime = " + currentTime);
    }
}
