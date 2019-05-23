package com.cy.helmet.led;

import android.util.Log;

/**
 * Created by zhangchongyang on 18-3-14.
 */

public class LedConfig {
    private static final String LOG_LED_TAG = "Helm_Led";
    private int mRow;
    private int mColumns;

    public LedConfig(int row, int columns) {
        mRow = row;
        mColumns = columns;
    }

    public int getmRow() {
        return mRow;
    }

    public void setmRow(int mRow) {
        this.mRow = mRow;
    }

    public int getmColumns() {
        return mColumns;
    }

    public void setmColumns(int mColumns) {
        this.mColumns = mColumns;
    }



    public static void d(String value) {
        Log.e(LOG_LED_TAG, value);
    }

    public static void e(String value) {
        Log.e(LOG_LED_TAG, value);
    }
}
