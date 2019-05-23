package com.cy.helmet.video.controller.video;

import com.cy.helmet.video.callback.VideoLiveCallback;
import com.cy.helmet.video.callback.VideoRecordCallback;
import com.cy.helmet.video.video.OnVideoEncodeListener;

public interface IVideoController {

    void startRecord(OnVideoEncodeListener listener, VideoRecordCallback callback);

    void stopRecord();

    void startLive(OnVideoEncodeListener listener, VideoLiveCallback callback);

    void stopLive();

    boolean isLiving();

    boolean setVideoBps(int bps);

    boolean setLiveBps(int bps);

    void setVideoEncoderListener(OnVideoEncodeListener listener);
}
