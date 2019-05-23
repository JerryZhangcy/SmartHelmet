package com.cy.helmet.video.stream.sender;

public interface Sender {
    void start();

    void onData(byte[] data, int type);

    void stop();

    boolean onTransferFile();
}
