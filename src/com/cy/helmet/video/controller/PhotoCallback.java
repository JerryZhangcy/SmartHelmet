package com.cy.helmet.video.controller;

import java.io.File;

/**
 * Created by tangxiaohui on 2018/1/23.
 */

public interface PhotoCallback {
    void updatePhotoStatus(boolean status, File file);
}
