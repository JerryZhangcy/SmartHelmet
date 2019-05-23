package com.cy.helmet.sensor;

import com.cy.helmet.location.LocationUtil;
import com.cy.helmet.voice.HelmetVoiceManager;
import static com.cy.helmet.HelmetApplication.mAppContext;
import com.cy.helmet.R;
/**
 * Created by haocheng on 18-3-24.
 */

public class WearFailVoicePlay {
    private static WearFailVoicePlay mInstance;
    protected Thread mVoiceThread;
    protected VoicePlayRunnable mVoicePlayRunnable;
    private boolean mStop = false;
    private boolean mStart = false;
    private static final int DELAY_TIME = 10000;
    private static final int MAX_COUNT = 6;
    private int mCount = 0;

    public static synchronized WearFailVoicePlay getInstance() {
        if (mInstance == null) {
            synchronized (WearFailVoicePlay.class) {
                if (mInstance == null) {
                    mInstance = new WearFailVoicePlay();
                }
            }
        }
        return mInstance;
    }

    public synchronized void start() throws Exception {
        SensorUtil.d("WearFailVoicePlay----start----->mStart = " + mStart);
        if(mStart == true)
            return;
        mStart = true;
        mStop = false;
        mVoicePlayRunnable = new VoicePlayRunnable();
        mVoiceThread = new Thread(mVoicePlayRunnable, "WEAR_FAIL_THREAD");
        synchronized (mVoiceThread) {
            mVoiceThread.start();
        }
    }


    public synchronized void stop() {
        mStop = true;
        mStart = false;
        mCount = 0;
        if (mVoiceThread != null) {
            try {
                mVoiceThread.interrupt();
            } catch (Exception e) {
            }
        }
    }

    class VoicePlayRunnable implements Runnable {

        @Override
        public void run() {
            SensorUtil.d("WearFailVoicePlay----start----->mStop = " + mStop + " mCount = " + mCount);
            while(mStop == false && mCount < MAX_COUNT) {
                try {
                    HelmetVoiceManager.getInstance().playLocalVoice(HelmetVoiceManager.DEVICE_WEAR_FAILED);
                    mCount++;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    waitMsg();
                }

                if(!mAppContext.getResources().getBoolean(R.bool.config_wear_fail_play_times)) {
                   mStop = true;
                }
            }
            mCount = 0;
            mStart = false;
        }

        private void waitMsg() {
            synchronized (this) {
                try {
                    Thread.sleep(DELAY_TIME);
                } catch (InterruptedException e) {
                    mCount = 0;
                    mStop = true;
                    mStart = false;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
