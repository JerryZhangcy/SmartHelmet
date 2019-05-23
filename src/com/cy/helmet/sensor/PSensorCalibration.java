package com.cy.helmet.sensor;

import android.provider.Settings;

import com.cy.helmet.HelmetApplication;
import com.cy.helmet.util.NvramFileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class PSensorCalibration {
    private static final String LEFT_HIGH = "sys/bus/platform/drivers/als_ps/high";
    private static final String LEFT_LOW = "sys/bus/platform/drivers/als_ps/low";
    private static final String RIGHT_HIGH = "sys/bus/platform/drivers/als_ps/high2";
    private static final String RIGHT_LOW = "sys/bus/platform/drivers/als_ps/low2";

    private static final String LEFT_CAL = "sys/bus/platform/drivers/als_ps/cali";
    private static final String RIGHT_CAL = "sys/bus/platform/drivers/als_ps/cali2";

    private static PSensorCalibration mInstance;
    protected Thread mCalibrationThread;
    private calibrationRunnable mCalibrationRunnable;
    private boolean mStop = false;
    private boolean mStart = false;

    public static synchronized PSensorCalibration getInstance() {
        if (mInstance == null) {
            synchronized (PSensorCalibration.class) {
                if (mInstance == null) {
                    mInstance = new PSensorCalibration();
                }
            }
        }
        return mInstance;
    }

    public void start() throws Exception {
        if (mStart)
            return;
        mStart = true;
        mStop = false;
        mCalibrationRunnable = new calibrationRunnable();
        mCalibrationThread = new Thread(mCalibrationRunnable, "PSENSOR_CALIBRATION_THREAD");
        synchronized (mCalibrationThread) {
            mCalibrationThread.start();
        }
    }

    public void stop() {
        mStop = true;
        mStart = false;
        if (mCalibrationThread != null) {
            try {
                mCalibrationThread.interrupt();
            } catch (Exception e) {
            }
        }
    }

    private String readFile(String path) {
        File regFile = new File(path);
        String str = null;
        if (!regFile.exists()) {
            return null;
        }
        try {
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(regFile), 256);
            str = reader.readLine();
        } catch (Exception e) {

        }
        return str;
    }

    private void writeFile(String path, String value) {
        File regFile = new File(path);
        FileOutputStream regfout = null;
        try {
            regfout = new FileOutputStream(regFile);
            byte[] regbytes = value.getBytes();
            regfout.write(regbytes);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (regfout != null) {
                try {
                    regfout.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean validValue(int a, int b, int c, int d) {
        if (a > 0 && a < 10000
                && b > 0 && b < 10000
                && c > 0 && c < 10000
                && d > 0 && d < 10000) {
            return true;
        }
        return false;
    }


    private void doCalibration() {
        String calLeftHigh = readFile(LEFT_HIGH);
        String calLeftLow = readFile(LEFT_LOW);

        String calRightHigh = readFile(RIGHT_HIGH);
        String calRightLow = readFile(RIGHT_LOW);

        if (calLeftHigh != null && calLeftLow != null
                && calRightHigh != null && calRightLow != null) {
            String finalCalibrationValue = calLeftHigh + "|" + calLeftLow + "|" + calRightHigh + "|" + calRightLow + "|";
            NvramFileUtil.writePhoneInfoData(NvramFileUtil.stringParseAscii(finalCalibrationValue));
        }
    }


    /**
     * 存的数据是按照这个样式存储的2450|2450|2145|2045|xxxxxxxxxxxxxxxxxxxxx
     * 所以我们只需要读取前4位即可
     */
    public String[] readCalibration() {
        byte[] value = NvramFileUtil.readData();
        String str = NvramFileUtil.asciiParseString(value);
        if (str != null && str.contains("|")) {
            String[] result = str.split("\\|");
            return result;
        }
        return null;
    }


    class calibrationRunnable implements Runnable {

        @Override
        public void run() {

            String[] value = readCalibration();

            if (value != null) {
                int leftHigh = Integer.valueOf(value[0]);
                int leftLow = Integer.valueOf(value[1]);
                int rightHigh = Integer.valueOf(value[2]);
                int rightlow = Integer.valueOf(value[3]);
                SensorUtil.d("----->leftHigh = " + leftHigh + " leftLow = " + leftLow
                        + "  rightHigh = " + rightHigh + "  rightlow = " + rightlow);
                if (validValue(leftHigh, leftLow, rightHigh, rightlow)) {
                    writeFile(LEFT_HIGH, String.valueOf(leftHigh));
                    writeFile(LEFT_LOW, String.valueOf(leftLow));
                    writeFile(RIGHT_HIGH, String.valueOf(rightHigh));
                    writeFile(RIGHT_LOW, String.valueOf(rightlow));
                } else {
                    writeFile(LEFT_CAL, "1");
                    writeFile(RIGHT_CAL, "1");

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    doCalibration();
                }
            } else {
                writeFile(LEFT_CAL, "1");
                writeFile(RIGHT_CAL, "1");

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                doCalibration();
            }
            mStart = false;
            mStop = false;
        }
    }
}
