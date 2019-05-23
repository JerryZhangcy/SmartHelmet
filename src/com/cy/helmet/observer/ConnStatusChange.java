package com.cy.helmet.observer;

import java.util.Observable;

public class ConnStatusChange extends Observable {

    private static ConnStatusChange mInstance;
    private boolean mConnected = false;

    public static synchronized ConnStatusChange getInstance() {
        if (mInstance == null) {
            synchronized (ConnStatusChange.class) {
                if (mInstance == null) {
                    mInstance = new ConnStatusChange();
                }
            }
        }
        return mInstance;
    }

    public synchronized void onConnectStatus(boolean status) {
        if (status == mConnected) {
            return;
        }
        mConnected = status;
        setChanged();
        notifyObservers(new Boolean(mConnected));
    }
}
