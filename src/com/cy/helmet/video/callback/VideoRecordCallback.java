package com.cy.helmet.video.callback;

/**
 * Created by jiaqing on 2018/3/7.
 */

public interface VideoRecordCallback {
    public void onRecordingStart(boolean success);

    public void onRecordingFinish();

}
