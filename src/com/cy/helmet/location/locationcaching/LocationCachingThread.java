package com.cy.helmet.location.locationcaching;

import com.cy.helmet.location.LocationDataUtil;
import com.cy.helmet.location.LocationUtil;

import java.util.concurrent.LinkedBlockingQueue;

public class LocationCachingThread {
    private static LocationCachingThread mInstance;
    protected Thread mLocationSaveThread;
    protected LocationSaveRunnable mLocationSaveRunnable;
    private boolean mStop = false;
    protected LinkedBlockingQueue<String> mSendMessageQueue = new LinkedBlockingQueue<String>();

    public static synchronized LocationCachingThread getInstance() {
        if (mInstance == null) {
            synchronized (LocationCachingThread.class) {
                if (mInstance == null) {
                    mInstance = new LocationCachingThread();
                }
            }
        }
        return mInstance;
    }

    public synchronized void start() throws Exception {
        mStop = false;
        mLocationSaveRunnable = new LocationSaveRunnable();
        mLocationSaveThread = new Thread(mLocationSaveRunnable, "LOCATION_CACHING");
        mLocationSaveThread.setDaemon(true);
        synchronized (mLocationSaveThread) {
            mLocationSaveThread.start();
        }
    }

    public synchronized void stop() {
        mStop = true;
        if (mLocationSaveThread != null) {
            try {
                mLocationSaveThread.interrupt();
            } catch (Exception e) {
            }
        }
    }

    /**
     * @param config
     */
    public void sendMessage(String config) {
        if (config != null) {
            mSendMessageQueue.add(config);
        }
    }

    /**
     * @return
     */

    protected String pollMessage() {
        try {
            return mSendMessageQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    class LocationSaveRunnable implements Runnable {

        @Override
        public void run() {
            while (!mStop) {
                try {
                    //LocationCaching.writeCaching(pollMessage(), LocationUtil.GPS_NORMAL_FILE_NAME);
                    if(LocationDataUtil.mGpsDataState == LocationDataUtil.GPS_DATA_FIRST) {
                        LocationDataUtil.mGpsFirstData.add(pollMessage());
                    } else {
                        LocationDataUtil.mGpsSecondData.add(pollMessage());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
