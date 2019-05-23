package com.cy.helmet.sensor;

import android.provider.Settings;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import com.cy.helmet.config.HelmetConfig;

import static com.cy.helmet.HelmetApplication.mAppContext;

public class GSensor {
    public static final String NODE_NAME = "/sys/bus/platform/drivers/gsensor/tap_value";
    public static final int DEFAULT_GSENSOR_VALUE = 31;
    public static final int GENSOR_MAX = 31;
    public static final int GENSOR_MIN = 0;


    public static void serviceWriteGsensor(String value) {
        Settings.Secure.putString(mAppContext.getContentResolver(), "helmet_gsensor", value);
        SensorUtil.e("<serviceWriteGsensor> value = " + value);
        writeFile(value);
    }

    public static void initGsensorValue() {
        HelmetConfig mConfig = HelmetConfig.get();
        String value = String.valueOf(mConfig.precision);
        Settings.Secure.putString(mAppContext.getContentResolver(), "helmet_gsensor", value);
        SensorUtil.e("<initGsensorValue> value = " + value);
        if (value == null || TextUtils.isEmpty(value)) {
            value = String.valueOf(DEFAULT_GSENSOR_VALUE);
        }
        writeFile(value);
    }

    private static void writeFile(String value) {
        File regFile = new File(NODE_NAME);
        if (!regFile.exists()) {
            SensorUtil.e("gsensor node not exist!!!");
            return;
        }
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
}
