package com.cy.helmet.observer;

import java.util.Observable;

public class HelmetPSensorChange extends Observable {
    private static HelmetPSensorChange mInstance;
    private boolean mCover = false;

    public static synchronized HelmetPSensorChange getInstance() {
        if (mInstance == null) {
            synchronized (HelmetPSensorChange.class) {
                if (mInstance == null) {
                    mInstance = new HelmetPSensorChange();
                }
            }
        }
        return mInstance;
    }

    public synchronized void onPsensorStatus(boolean cover) {
        mCover = cover;
        setChanged();
        notifyObservers(new Boolean(mCover));
    }
}
