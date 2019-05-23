package com.cy.helmet.util;

import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by yaojiaqing on 2017/12/30.
 */

public class LogUtil {
    public static final String TAG = "Helmet";

    public static void v(String text) {
        Log.v(TAG, text);
    }

    public static void d(String text) {
        Log.d(TAG, text);
    }

    public static void i(String text) {
        Log.i(TAG, text);
    }

    public static void w(String text) {
        Log.w(TAG, text);
    }

    public static void w(Exception e) {
        StringWriter errors = new StringWriter();
        e.printStackTrace(new PrintWriter(errors));
        Log.w(TAG, errors.toString());
    }

    public static void w(String prefix, Exception e) {
        StringWriter errors = new StringWriter();
        e.printStackTrace(new PrintWriter(errors));
        Log.w(TAG, prefix + ": " + errors.toString());
    }

    public static void e(String text) {
        Log.e(TAG, text);
    }

    public static void e(Exception e) {
        StringWriter errors = new StringWriter();
        e.printStackTrace(new PrintWriter(errors));
        Log.e(TAG, errors.toString());
    }


    public static void d(Object msg) {
        StackTraceElement element = Thread.currentThread().getStackTrace()[3];
        if (msg instanceof Exception) {
            android.util.Log.e(TAG, element.getFileName().replace(".java", "") + "->" + element.getMethodName() + ": [" + element.getLineNumber() + "] " + String.valueOf(msg));

        } else {
            android.util.Log.d(TAG, element.getFileName().replace(".java", "") + "->" + element.getMethodName() + ": [" + element.getLineNumber() + "] " + String.valueOf(msg));
        }
    }
}
