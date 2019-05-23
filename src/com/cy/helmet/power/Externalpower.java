package com.cy.helmet.power;

import android.provider.Settings;
import android.text.TextUtils;

import com.cy.helmet.sensor.SensorUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.cy.helmet.HelmetApplication.mAppContext;

public class Externalpower {
    public static final String NODE_NAME = "/sys/class/leds/aw9109_led/hct_5v_charge_gpio";
    public static final int OPEN = 1;
    public static final int CLOSE = 0;
    public static final int DEFAULT_VALUE = CLOSE;


    public static void serviceWriteExternalpower(int value) {
        Settings.Secure.putInt(mAppContext.getContentResolver(), "helmet_expower", value);
        writeFile(String.valueOf(value));
    }

    public static void initExternalpowerValue() {
        int value = Settings.Secure.getInt(mAppContext.getContentResolver(), "helmet_expower", DEFAULT_VALUE);
        writeFile(String.valueOf(value));
    }

    private static void writeFile(String value) {
        File regFile = new File(NODE_NAME);
        if (!regFile.exists()) {
            SensorUtil.e("Externalpower node not exist!!!");
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
