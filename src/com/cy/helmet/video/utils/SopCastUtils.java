package com.cy.helmet.video.utils;

import android.os.Handler;
import android.os.HandlerThread;

public class SopCastUtils {

    private static Handler mRecordingHandler;
    private static HandlerThread mRecordingHandlerThread;

    private static Handler mLivingHandler;
    private static HandlerThread mLivingHandlerThread;

    public static Handler getRecordingHandler() {
        if (mRecordingHandler == null) {
            synchronized (SopCastUtils.class) {
                mRecordingHandlerThread = new HandlerThread("VIDEO_WORK_HANDLER");
                mRecordingHandlerThread.start();
                mRecordingHandler = new Handler(mRecordingHandlerThread.getLooper());
            }
        }
        return mRecordingHandler;
    }

    public static Handler getLivingHandler() {
        if (mLivingHandler == null) {
            synchronized (SopCastUtils.class) {
                mLivingHandlerThread = new HandlerThread("VIDEO_WORK_HANDLER");
                mLivingHandlerThread.start();
                mLivingHandler = new Handler(mLivingHandlerThread.getLooper());
            }
        }
        return mLivingHandler;
    }
}
