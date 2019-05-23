package com.cy.helmet.location;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;

import com.cy.helmet.HelmetApplication;
import com.cy.helmet.config.HelmetConfig;
import com.cy.helmet.location.locationConnect.LocationHttpConnect;
import com.cy.helmet.location.locationConnect.LocationHttpSendMessage;
import com.cy.helmet.location.locationcaching.LocationCaching;
import com.cy.helmet.location.locationcaching.LocationCachingSend;
import com.cy.helmet.observer.GpsConnectChange;
import com.cy.helmet.sensor.SensorUtil;

import org.json.JSONObject;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.content.Context.ALARM_SERVICE;

public class LocationSendDataThread {
    private static LocationSendDataThread mInstance;
    private static final int TRY_MAX_NUM = 20;
    private static final int MSG_START_LOCATION_RECORD = 0x10000;
    private static final int MSG_STOP_LOCATION_RECORD = 0x10001;
    private Thread mSendThread;
    private DataRunnable mDataRunnable;
    private boolean mIsDataRunning = false;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START_LOCATION_RECORD:
                    LocationDataUtil.getInstance().recordLocation(true);
                    break;
                case MSG_STOP_LOCATION_RECORD:
                    LocationDataUtil.getInstance().recordLocation(false);
                    break;
            }
        }
    };

    /**
     * @return
     */
    public static synchronized LocationSendDataThread getInstance() {
        if (mInstance == null) {
            synchronized (LocationSendDataThread.class) {
                if (mInstance == null) {
                    mInstance = new LocationSendDataThread();
                }
            }
        }
        return mInstance;
    }


    /**
     * @throws Exception
     */
    public synchronized void start() throws Exception {
        if (!mIsDataRunning) {
            LocationUtil.d("LocationSendDataThread---oncreate +++");
            mIsDataRunning = true;
            mDataRunnable = new DataRunnable();
            mSendThread = new Thread(mDataRunnable, "Location_send");
            mSendThread.setDaemon(true);
            synchronized (mSendThread) {
                mSendThread.start();
            }
            LocationUtil.d("LocationSendDataThread---oncreate ---");
        }
    }

    public void notifyThread() {
        if (mSendThread != null) {
            synchronized (mSendThread) {
                mSendThread.notifyAll();
            }
        }
    }

    class DataRunnable implements Runnable {

        @Override
        public void run() {
            while (true) {

                LocationUtil.d("LocationSendDataThread---wait lock");
                synchronized (mSendThread) {
                    try {
                        mSendThread.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                LocationUtil.d("LocationSendDataThread---wait unlock");

                if (LocationDataUtil.mGpsDataState == LocationDataUtil.GPS_DATA_FIRST) {
                    LocationDataUtil.mGpsDataState = LocationDataUtil.GPS_DATA_SECOND;
                } else {
                    LocationDataUtil.mGpsDataState = LocationDataUtil.GPS_DATA_FIRST;
                }

                mHandler.obtainMessage(MSG_START_LOCATION_RECORD).sendToTarget();
                sendLocationData();

                try {
                    Thread.sleep(2000);//睡眠2S确保数据发送完
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                startNextAlarm();

                if (HelmetConfig.get().mIsSleep == 1)
                    mHandler.obtainMessage(MSG_STOP_LOCATION_RECORD).sendToTarget();
            }
        }
    }

    public void startNextAlarm() {
        AlarmManager mAlarmManager = (AlarmManager) HelmetApplication.mAppContext.getSystemService(ALARM_SERVICE);
        Intent sendDataIntent = new Intent();
        sendDataIntent.setClass(HelmetApplication.mAppContext,
                LocationSendDataService.class);
        PendingIntent sendData = PendingIntent.getService(HelmetApplication.mAppContext,
                0, sendDataIntent, FLAG_UPDATE_CURRENT);
        mAlarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + LocationDataUtil.getInstance().getGpsDataGap(),
                sendData);
    }

    private void sendLocationData() {
        JSONObject jsonObject = null;
        String longitude = "0";
        String latitude = "0";
        LocationDataUtil locationDataUtil = LocationDataUtil.getInstance();
        String IMEI = locationDataUtil.getIMEI();

        if (!LocationDataUtil.getInstance().getGpsDeviceEnabled()) {
            //lbs定位
            LocationSimInfo locationSimInfo = locationDataUtil.getCellInfo();
            if (locationSimInfo != null) {
                int mcc = locationSimInfo.getMCC();
                int mnc = locationSimInfo.getMNC();
                int lac = locationSimInfo.getLAC();
                int cid = locationSimInfo.getCID();
                jsonObject = LocationFactory.getInstance().getGpsLbs(LocationMsgDef.GPS_LBS,
                        IMEI, mcc, mnc, lac, cid, String.valueOf(SensorUtil.mGSensorX),
                        String.valueOf(SensorUtil.mGSensorY), String.valueOf(SensorUtil.mGSensorZ));
                LocationDataUtil.setLocationType(LocationManager.LBS_PROVIDER);
                LocationUtil.d("LocationSendDataThread---lbs------>jsonObject = " + jsonObject);
            }
        } else {
            Location location = null;
            int satellite = 0;
            int tryGetNum = 0;
            do {
                location = locationDataUtil.getCurrentLocation();
                satellite = locationDataUtil.getGpsSatelliteNum();
                tryGetNum++;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (location == null && tryGetNum < TRY_MAX_NUM);

            LocationUtil.d("LocationSendDataThread---------> satellite = "
                    + satellite + " tryGetNum = " + tryGetNum
                    + "  network = " + locationDataUtil.isNetworkAvailable()
                    + " location = " + (location == null));

            //GPS缓存逻辑+++
            if (!locationDataUtil.isNetworkAvailable() && location != null) {
                String firstLineTime = LocationCaching.getFirstLineInfo();
                long currentTime = System.currentTimeMillis();
                if (firstLineTime != null) {
                    long time = Long.valueOf(firstLineTime);
                    if (currentTime - time > locationDataUtil.getGpsCachingTime()) {
                        return;
                    }
                }

                if (HelmetConfig.get().mIsSleep == 0) {
                    LocationCachingSend.getInstance().sendNormalLocation();
                } else {
                    longitude = LocationUtil.roundByScale(location.getLongitude(), 6);
                    latitude = LocationUtil.roundByScale(location.getLatitude(), 6);
                    LocationUtil.d("LocationSendDataThread----cache----->longitude = "
                            + longitude + " latitude = " + latitude);
                    LocationCaching.writeCaching(latitude + "|" + longitude + "|"
                            + satellite + "|" + String.valueOf(SensorUtil.mGSensorX) + "|"
                            + String.valueOf(SensorUtil.mGSensorY) + "|"
                            + String.valueOf(SensorUtil.mGSensorZ) + "|"
                            + String.valueOf(currentTime), LocationUtil.GPS_CACHING_FILE_NAME);
                }
                GpsConnectChange.getInstance().onGpsConnectChange(false);
                locationDataUtil.resetCurrentLocation();
                return;
            }
            //GPS缓存逻辑---

            if (location != null) {
                longitude = LocationUtil.roundByScale(location.getLongitude(), 6);
                latitude = LocationUtil.roundByScale(location.getLatitude(), 6);
                jsonObject = LocationFactory.getInstance().getGpsData(LocationMsgDef.GPS_DATA, IMEI,
                        LocationManager.GPS_PROVIDER, latitude, longitude, satellite, System.currentTimeMillis(),
                        String.valueOf(SensorUtil.mGSensorX), String.valueOf(SensorUtil.mGSensorY),
                        String.valueOf(SensorUtil.mGSensorZ));
                LocationUtil.d("LocationSendDataThread----gps----->jsonObject = " + jsonObject);
                LocationDataUtil.setLocationType(LocationManager.GPS_PROVIDER);
            } else {
                //lbs定位
                LocationSimInfo locationSimInfo = locationDataUtil.getCellInfo();
                if (locationSimInfo != null) {
                    int mcc = locationSimInfo.getMCC();
                    int mnc = locationSimInfo.getMNC();
                    int lac = locationSimInfo.getLAC();
                    int cid = locationSimInfo.getCID();
                    jsonObject = LocationFactory.getInstance().getGpsLbs(LocationMsgDef.GPS_LBS,
                            IMEI, mcc, mnc, lac, cid, String.valueOf(SensorUtil.mGSensorX),
                            String.valueOf(SensorUtil.mGSensorY), String.valueOf(SensorUtil.mGSensorZ));
                    LocationDataUtil.setLocationType(LocationManager.LBS_PROVIDER);
                    LocationUtil.d("LocationSendDataThread---lbs------>jsonObject = " + jsonObject);
                }
            }
        }

        LocationUtil.d("LocationSendDataThread---final------>jsonObject = " + jsonObject);
        if (jsonObject != null) {
            if (HelmetConfig.get().mIsSleep == 1
                    || LocationDataUtil.getLocationType() == LocationManager.LBS_PROVIDER) {
                LocationHttpConnect.getInstance().sendMessage(new LocationHttpSendMessage(jsonObject,
                        LocationHttpSendMessage.TYPE_NORMAL));
            } else {
                LocationCachingSend.getInstance().sendNormalLocation();
            }
        } else {
            GpsConnectChange.getInstance().onGpsConnectChange(false);
            LocationDataUtil.setLocationType(LocationManager.GPS_UNKNOW);
        }
        locationDataUtil.resetCurrentLocation();

        //尝试发送缓存数据
        LocationCachingSend.getInstance().sendLocationCaching();
    }
}
