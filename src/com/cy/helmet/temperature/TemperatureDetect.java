package com.cy.helmet.temperature;

import android.text.TextUtils;
import android.util.Log;

import com.cy.helmet.video.HelmetVideoManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class TemperatureDetect {
    private static final String TAG = "Helm_temperature";
    private static TemperatureDetect mInstance;
    private DetectRunnable mDetectRunnable;
    private Thread mDetectedThread;
    private boolean mStart = false;
    private boolean mStop = false;
    private static final String AP_PATH = "/sys/class/thermal/thermal_zone15/temp";
    private static final String BATTERY_PATH = "/sys/class/thermal/thermal_zone0/temp";
    private static final int AP_TEMP = 80;
    private static final int AP_HIGH_TO_LOW_TEMP = 60;
    private static final int BATTERY_TEMP = 60;
    private static final int DETECTED_GAP = 30000;

    private int mAPTemp = 0;
    private int mBatteryTemp = 0;

    private boolean mCurrentTempWarning = false;

    private ITemperatureCallback mITemperatureCallback;

    public static synchronized TemperatureDetect getmInstance() {
        if (mInstance == null)
            mInstance = new TemperatureDetect();
        return mInstance;
    }

    public void setTemperatureCallback(ITemperatureCallback iTemperatureCallback) {
        mITemperatureCallback = iTemperatureCallback;
    }

    public int getAPTemp() {
        return mAPTemp;
    }

    public int getBatteryTemp() {
        return mBatteryTemp * 7 / 6; //modify for display
    }

    public boolean isTempWarning() {
        return mCurrentTempWarning;
    }

    public void start() {
        if (mStart)
            return;
        mStart = false;
        mStop = false;
        mDetectRunnable = new DetectRunnable();
        mDetectedThread = new Thread(mDetectRunnable, "TEMP_THREAD");
        synchronized (mDetectedThread) {
            mDetectedThread.start();
        }
    }

    public void stop() {
        mStart = false;
        mStop = true;
        if (mDetectedThread != null) {
            try {
                mDetectedThread.interrupt();
            } catch (Exception e) {
            }
        }
    }

    private int readTemperature(String path) {
        int temperature = 0;
        if (TextUtils.isEmpty(path))
            return temperature;
        File file = new File(path);
        if (!file.exists())
            return temperature;
        FileInputStream fis = null;
        BufferedReader reader = null;
        try {
            fis = new FileInputStream(path);
            reader = new BufferedReader(new InputStreamReader(fis));
            temperature = Integer.valueOf(reader.readLine());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null)
                    fis.close();
                if (reader != null)
                    reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        return temperature;
    }

    class DetectRunnable implements Runnable {

        @Override
        public void run() {
            while (!mStop) {
                mAPTemp = readTemperature(AP_PATH) / 1000;
                mBatteryTemp = readTemperature(BATTERY_PATH) / 1000;
                Log.e(TAG, "------>mAPTemp = " + mAPTemp + "  mBatteryTemp = " + mBatteryTemp);
                if (mAPTemp > AP_TEMP || mBatteryTemp > BATTERY_TEMP) {
                    mCurrentTempWarning = true;
                    if (mITemperatureCallback != null)
                        mITemperatureCallback.onTemperatureWaring();
                } else {
                    if (mCurrentTempWarning) {
                        if (mAPTemp <= AP_HIGH_TO_LOW_TEMP) {
                            mCurrentTempWarning = false;
                        }
                    } else {
                        mCurrentTempWarning = false;
                    }
                }

                try {
                    Thread.sleep(DETECTED_GAP);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
