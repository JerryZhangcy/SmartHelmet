package com.cy.helmet.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class NetworkPing {
    private static final String TAG = "Helmet_NetworkPing";
    private PingRunnable mPingRunnable;
    private Thread mPingThread;
    private static NetworkPing mInstance;
    private boolean mIsStart = false;

    public static synchronized NetworkPing getInstance() {
        if (mInstance == null) {
            synchronized (NetworkPing.class) {
                if (mInstance == null) {
                    mInstance = new NetworkPing();
                }
            }
        }
        return mInstance;
    }


    public synchronized void startPing() {
        android.util.Log.e(TAG, "startPing  mIsStart = " + mIsStart);
        if (mIsStart)
            return;
        mIsStart = true;
        mPingRunnable = new PingRunnable();
        mPingThread = new Thread(mPingRunnable, "Helmet_ping");
        synchronized (mPingThread) {
            mPingThread.start();
        }
    }

    class PingRunnable implements Runnable {

        @Override
        public void run() {
            Process process = null;
            BufferedReader successReader = null;
            BufferedReader errorReader = null;
            String ping = "ping -c 5 www.baidu.com";
            try {
                process = Runtime.getRuntime().exec(ping);
                InputStream in = process.getInputStream();
                OutputStream out = process.getOutputStream();
                successReader = new BufferedReader(
                        new InputStreamReader(in));
                errorReader = new BufferedReader(new InputStreamReader(
                        process.getErrorStream()));
                String lineStr;
                android.util.Log.e(TAG, "************helmet ping begin*************");
                while ((lineStr = successReader.readLine()) != null) {
                    android.util.Log.e(TAG, lineStr);
                }
                while ((lineStr = errorReader.readLine()) != null) {
                    android.util.Log.e(TAG, "ping error: " + lineStr);
                }
                android.util.Log.e(TAG, "************helmet ping end*************");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (successReader != null) {
                        successReader.close();
                    }
                    if (errorReader != null) {
                        errorReader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (process != null) {
                    process.destroy();
                }
                mIsStart = false;
            }
        }
    }
}
