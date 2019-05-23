package com.cy.helmet.sensor;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.provider.Settings;

import com.cy.helmet.factorytest.FactoryTest;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.SENSOR_SERVICE;
import static com.cy.helmet.HelmetApplication.mAppContext;

/**
 * Created by zhangchongyang on 18-3-6.
 */

public class PSensor implements SensorEventListener {
    private static PSensor mInstance;
    private List<PSensorChangeListener> mList = new ArrayList<PSensorChangeListener>();
    private SensorManager mSensorManager = null;
    private Sensor mSensor = null;
    public static final float COVER = 0.0f;//安全帽工作状态
    public static final float COVER2 = 2.0f;//安全帽工作状态
    public static final float UNCOVER = 1.0f;//安全帽非工作状态

    public interface PSensorChangeListener {
        public void onSensorChanged(boolean cover);
    }

    public static synchronized PSensor getInstance() {
        if (mInstance == null) {
            synchronized (PSensor.class) {
                if (mInstance == null) {
                    mInstance = new PSensor();
                }
            }
        }
        return mInstance;
    }

    public void setPSensorChangeListener(PSensorChangeListener pSensorChangeListener) {
        mList.add(pSensorChangeListener);
    }

    public void startMonitor() {
        mSensorManager = (SensorManager) mAppContext.getSystemService(SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mSensorManager.registerListener(this, mSensor
                , SensorManager.SENSOR_DELAY_GAME);
    }

    public void stopMonitor() {
        if (mSensorManager != null)
            mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            float distance = event.values[0];
            float lastdistance = Settings.Secure.getFloat(mAppContext.getContentResolver(), "helmet_idle",
                    PSensor.UNCOVER);
            SensorUtil.d("PSensor----onSensorChanged----->distance = "
                    + distance + " lastdistance = " + lastdistance);
            if (FactoryTest.mPsensorMode == FactoryTest.PSENSOR_MODE_OR) {
                //或的关系
                if ((distance == COVER2 && lastdistance == COVER)
                        || (distance == COVER && lastdistance == COVER2)) {
                    Settings.Secure.putFloat(mAppContext.getContentResolver(), "helmet_idle", distance);
                } else {
                    Settings.Secure.putFloat(mAppContext.getContentResolver(), "helmet_idle", distance);
                    if (!mList.isEmpty()) {
                        for (PSensorChangeListener pSensorChangeListener : mList) {
                            pSensorChangeListener.onSensorChanged(distance == COVER || distance == COVER2);
                        }
                    }
                }
            } else {
                //与的关系
                if ((distance == UNCOVER && lastdistance == COVER2)
                        || (distance == COVER2 && lastdistance == UNCOVER)) {
                    Settings.Secure.putFloat(mAppContext.getContentResolver(), "helmet_idle", distance);
                } else {
                    Settings.Secure.putFloat(mAppContext.getContentResolver(), "helmet_idle", distance);
                    if (!mList.isEmpty()) {
                        for (PSensorChangeListener pSensorChangeListener : mList) {
                            pSensorChangeListener.onSensorChanged(distance == COVER);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }
}
