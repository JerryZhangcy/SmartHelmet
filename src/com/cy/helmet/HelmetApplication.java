package com.cy.helmet;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.text.TextUtils;

import com.cy.helmet.config.HelmetConfig;
import com.cy.helmet.crashlog.CrashHandler;
import com.cy.helmet.storage.FileUtil;
import com.cy.helmet.util.LogUtil;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by yaojiaqing on 2018/1/1.
 */

public class HelmetApplication extends Application {
    public static Context mAppContext;
    private static Intent mBatteryIntent;
    private Handler mHandler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
        mAppContext = getApplicationContext();
        checkWIFI();
        CrashHandler.getInstance().init(mAppContext);
        //BluetoothManager.getInstance();
    }

    public static int getPowerPercent() {
        if (mBatteryIntent == null) {
            mBatteryIntent = mAppContext.registerReceiver(null,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        }

        int level = mBatteryIntent.getIntExtra("level", 0);
        int scale = mBatteryIntent.getIntExtra("scale", 1);
        return (level * 100 / scale);
    }

    private void checkWIFI() {
        HelmetConfig mConfig = HelmetConfig.get();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mConfig.updateWIFIInfo();
            }
        }, 10000);
    }
}
