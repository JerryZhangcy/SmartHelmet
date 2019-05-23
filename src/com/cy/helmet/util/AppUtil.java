package com.cy.helmet.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

/**
 * Created by yaojiaqing on 2017/12/28.
 */

public class AppUtil {

    public static String getAppVersionName(Context context) {
        /*
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
            String appVersionName = pi.versionName;
            return appVersionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "UNKNOWN_VERSION_NAME";
        }*/

        return android.os.SystemProperties.get("ro.build.display.id");
    }

    public static String getAppVersionCode(Context context) {
        /*
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
            int appVersionCode = pi.versionCode;
            return appVersionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        }*/

        return android.os.SystemProperties.get("ro.build.display.id");
    }
}
