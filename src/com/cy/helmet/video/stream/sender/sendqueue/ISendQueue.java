package com.cy.helmet.video.stream.sender.sendqueue;

import com.cy.helmet.video.entity.Frame;

public interface ISendQueue {
    void start();

    void stop();

    void setBufferSize(int size);

    void putFrame(Frame frame);

    Frame takeFrame();

    void setSendQueueListener(SendQueueListener listener);

    void init(int frameSize);
}
