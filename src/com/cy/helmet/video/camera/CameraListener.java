package com.cy.helmet.video.camera;

public interface CameraListener {
    int CAMERA_NOT_SUPPORT = 1;
    int NO_CAMERA = 2;
    int CAMERA_DISABLED = 3;
    int CAMERA_OPEN_FAILED = 4;

    void onOpenSuccess();

    void onOpenFail(int error);
}
