package com.cy.helmet.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.util.SimpleArrayMap;
import android.util.Log;
import android.util.SparseArray;

import static android.support.v4.content.PermissionChecker.checkSelfPermission;

public final class PermissionUtils {
    public static final String TAG = "PermissionUtils";
    // public static final int PERMISSION_GROUP_CAMERA = 0;
    public static final int PERMISSION_GROUP_CONTACTS = 0;
    public static final int PERMISSION_GROUP_LOCATION = 1;
    public static final int PERMISSION_GROUP_PHONE = 2;
    // public static final int PERMISSION_GROUP_SMSMMS = 4;
    public static final int PERMISSION_GROUP_STORAGE = 3;

    // Map of dangerous permissions introduced in later framework versions.
    // Used to conditionally bypass permission-hold checks on older devices.
    private static final SimpleArrayMap<String, Integer> MIN_SDK_PERMISSIONS;

    static {
        MIN_SDK_PERMISSIONS = new SimpleArrayMap<String, Integer>(6);
        MIN_SDK_PERMISSIONS.put("com.android.voicemail.permission.ADD_VOICEMAIL", 14);
        MIN_SDK_PERMISSIONS.put("android.permission.BODY_SENSORS", 20);
        MIN_SDK_PERMISSIONS.put("android.permission.READ_CALL_LOG", 16);
        MIN_SDK_PERMISSIONS.put("android.permission.READ_EXTERNAL_STORAGE", 16);
        MIN_SDK_PERMISSIONS.put("android.permission.USE_SIP", 9);
        MIN_SDK_PERMISSIONS.put("android.permission.WRITE_CALL_LOG", 16);
    }

    private static volatile int targetSdkVersion = -1;

    private static final SparseArray<PermissionsRequestCallback> sCallbacks = new SparseArray<PermissionsRequestCallback>();

    private PermissionUtils() {
    }

    /**
     * Checks all given permissions have been granted.
     *
     * @param grantResults
     *            results
     * @return returns true if all permissions have been granted.
     */
    public static boolean verifyPermissions(int... grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if the permission exists in this SDK version
     *
     * @param permission
     *            permission
     * @return returns true if the permission exists in this SDK version
     */
    private static boolean permissionExists(String permission) {
        // Check if the permission could potentially be missing on this device
        Integer minVersion = MIN_SDK_PERMISSIONS.get(permission);
        // If null was returned from the above call, there is no need for a
        // device API level check for the permission;
        // otherwise, we check if its minimum API level requirement is met
        return minVersion == null || Build.VERSION.SDK_INT >= minVersion;
    }

    /**
     * Returns true if the Activity or Fragment has access to all given
     * permissions.
     *
     * @param context
     *            context
     * @param permissions
     *            permission list
     * @return returns true if the Activity or Fragment has access to all given
     *         permissions.
     */
    public static boolean hasSelfPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (permissionExists(permission)
                    && checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks given permissions are needed to show rationale.
     *
     * @param activity
     *            activity
     * @param permissions
     *            permission list
     * @return returns true if one of the permission is needed to show
     *         rationale.
     */
    public static boolean shouldShowRequestPermissionRationale(Activity activity, String... permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get target sdk version.
     *
     * @param context
     *            context
     * @return target sdk version
     */
    @TargetApi(Build.VERSION_CODES.DONUT)
    public static int getTargetSdkVersion(Context context) {
        if (targetSdkVersion != -1) {
            return targetSdkVersion;
        }
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            targetSdkVersion = packageInfo.applicationInfo.targetSdkVersion;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return targetSdkVersion;
    }

    /**
     * Interface for activity.
     *
     * @param groupId
     *            see {@link }
     * @return permission granted of not
     */
    public static void checkSelfPermissions(Activity activity, int requestCode, PermissionsRequestCallback callback,
            int groupId, String... permissions) {
        if (PermissionUtils.hasSelfPermissions(activity, permissions)) {
            if (callback != null) {
                callback.onPermissionAllowed();
            }
        } else {
            if (callback != null) {
                sCallbacks.put(requestCode, callback);
            }
            if (PermissionUtils.shouldShowRequestPermissionRationale(activity, permissions)) {
                callback.onRequestPermission(requestCode);
                showRationaleForPermissions(activity, requestCode, groupId, permissions);
            } else {
                //callback.onPermissionNeverAsk();
                callback.onRequestPermission(requestCode);
                ActivityCompat.requestPermissions(activity, permissions, requestCode);
            }
        }
    }

    /**
     * Show rationale dialog for dangerous permission.
     */
    public static void showRationaleForPermissions(final Activity activity, final int requestCode, int groupId,
            final String... permissions) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode);

    }

    /**
     * Process results of request permissions from Activity
     */
    public static void onRequestPermissionsResult(Activity activity, int requestCode, int[] grantResults) {
        PermissionsRequestCallback callback = sCallbacks.get(requestCode);
        Log.i(TAG, "onRequestPermissionsResult callback = " + callback + ",requestCode = " + requestCode);
        /*
         * if (reference != null) { callback = reference.get(); if (callback ==
         * null) { Log.e(TAG, "onRequestPermissionsResult callback is null.");
         * return; } sCallbacks.remove(requestCode); } else { Log.e(TAG,
         * "onRequestPermissionsResult reference is null."); return; }
         */
        if (callback == null) {
            Log.e(TAG, "onRequestPermissionsResult callback is null.");
            return;
        } else {
            sCallbacks.remove(requestCode);
        }

        if (PermissionUtils.getTargetSdkVersion(activity) < 23
                && !PermissionUtils.hasSelfPermissions(activity, callback.onGetPermissions())) {
            callback.onPermissionDenied();
            return;
        }
        if (PermissionUtils.verifyPermissions(grantResults)) {
            callback.onPermissionAllowed();
        } else {
            if (!PermissionUtils.shouldShowRequestPermissionRationale(activity, callback.onGetPermissions())) {
                callback.onPermissionNeverAsk();
            } else {
                callback.onPermissionDenied();
            }
        }
    }

    public interface PermissionsRequestCallback {
        public void onPermissionAllowed();

        public void onPermissionDenied();

        /**
         * Notify user permissions denied with never ask or do nothing
         */
        public void onPermissionNeverAsk();

        /**
         * Return permissions that you request {Manifest.permission.CALL_PHONE}
         */
        public String[] onGetPermissions();

        public void onRequestPermission(int requestCode);
    }
}
