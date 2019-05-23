package com.cy.helmet.led;

import com.cy.helmet.location.LocationDataUtil;
import com.cy.helmet.observer.ConnStatusChange;
import com.cy.helmet.observer.GpsConnectChange;
import com.cy.helmet.observer.HelmetPSensorChange;

import java.util.Observable;
import java.util.Observer;

/**
 * Created by zhangchongyang on 18-3-14.
 */

public class SystemLedState {

    private int mLastLedState = 0;//0 红灯  1 绿灯  2 黄灯
    private Boolean mProfConnect = false;
    private Boolean mCover = false;
    private Boolean mGpsConnect = false;

    public void startObservableSystemLedState() {
        ConnStatusChange.getInstance().addObserver(new Observer() {
            @Override
            public void update(Observable observable, Object o) {
                mProfConnect = (Boolean) o;
                changeLedState();
                LedConfig.d("----->ConnStatusChange mProfConnect = " + mProfConnect);
            }
        });

        HelmetPSensorChange.getInstance().addObserver(new Observer() {
            @Override
            public void update(Observable observable, Object o) {
                mCover = (Boolean) o;
                changeLedState();
                LedConfig.d("----->HelmetPSensorChange mCovery = " + mCover);
            }
        });

        GpsConnectChange.getInstance().addObserver(new Observer() {
            @Override
            public void update(Observable observable, Object o) {
                mGpsConnect = (Boolean) o;
                changeLedState();
                LedConfig.d("----->GpsConnectChange mGpsConnect = " + mGpsConnect);
            }
        });
    }

    private void changeLedState() {

        LedConfig.d("----->idle = " + LocationDataUtil.getInstance().getHelmetIdle()
                + "  mLastLedState = " + mLastLedState + " mCover = " + mCover
                + " mGpsConnect = " + mGpsConnect + " mProfConnect = " + mProfConnect);

        if (!mCover) {
            if (mLastLedState != 2)
                Led.getInstance().sendMessage(new LedConfig(0, 2));
            mLastLedState = 2;
        } else {
            if (mProfConnect || mGpsConnect) {
                if (mLastLedState != 1)
                    Led.getInstance().sendMessage(new LedConfig(0, 1));
                mLastLedState = 1;
            } else {
                if (mLastLedState != 0) {
                    Led.getInstance().sendMessage(new LedConfig(0, 0));
                }
                mLastLedState = 0;
            }
        }
    }
}
