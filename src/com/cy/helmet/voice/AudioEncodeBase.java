package com.cy.helmet.voice;

import android.os.HandlerThread;

import java.util.Observable;
import java.util.Observer;

/**
 * Created by jiaqing on 2018/3/26.
 */

public class AudioEncodeBase extends HandlerThread implements Observer {


    public AudioEncodeBase(String name) {
        super(name);
    }

    @Override
    public void update(Observable o, Object arg) {

    }
}
