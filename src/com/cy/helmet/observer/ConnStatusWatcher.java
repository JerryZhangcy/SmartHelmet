package com.cy.helmet.observer;

import com.cy.helmet.util.LogUtil;

import java.util.Observable;
import java.util.Observer;

public class ConnStatusWatcher implements Observer {

    @Override
    public void update(Observable observer, Object arg) {
        if (observer instanceof ConnStatusChange) {
            if (arg instanceof Boolean) {
                LogUtil.e("helmet server connect status: " + arg);
            }
        }
    }
}
