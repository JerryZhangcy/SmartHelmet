package com.cy.helmet.observer;

import java.util.Observable;

public class GpsConnectChange extends Observable {
    private static GpsConnectChange mInstance;
    private boolean mSuccess = false;

    public static synchronized GpsConnectChange getInstance() {
        if (mInstance == null) {
            synchronized (GpsConnectChange.class) {
                if (mInstance == null) {
                    mInstance = new GpsConnectChange();
                }
            }
        }
        return mInstance;
    }

    public synchronized void onGpsConnectChange(boolean cover) {
        mSuccess = cover;
        setChanged();
        notifyObservers(new Boolean(mSuccess));
    }
}
