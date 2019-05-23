package com.cy.helmet.video.configuration;

import android.util.Log;

import com.cy.helmet.config.HelmetConfig;

public final class VideoConfiguration {

    public static final int DEFAULT_HEIGHT = 1280;
    public static final int DEFAULT_WIDTH = 720;
    public static final int DEFAULT_FPS = 15;
    public static final int DEFAULT_MAX_BPS = 1300;
    public static final int DEFAULT_IFI = 1;//2;
    public static final String DEFAULT_MIME = "video/avc";

    public int height;
    public int width;
    public int maxBps;
    public int fps;
    public int ifi;
    public String mime; // must be default

    private static VideoConfiguration mRecordConfig;
    private static VideoConfiguration mLiveConfig;

    private VideoConfiguration() {
        height = DEFAULT_HEIGHT;
        width = DEFAULT_WIDTH;
        maxBps = DEFAULT_MAX_BPS;
        fps = DEFAULT_FPS;
        ifi = DEFAULT_IFI;
        mime = DEFAULT_MIME;
    }

    public static synchronized VideoConfiguration getRecordingConfig() {
        int[] resolution = HelmetConfig.get().getRecordResolution();
        int codeRate = HelmetConfig.get().getRecordCodeRate();
        if (mRecordConfig == null) {
            mRecordConfig = new VideoConfiguration()
                    .setSize(resolution[0], resolution[1])
                    .setBps(codeRate)
                    .setFps(20)
                    .setIfi(2);
        } else {
            mRecordConfig.setSize(resolution[0], resolution[1]);
            mRecordConfig.setBps(codeRate);
        }
        return mRecordConfig;
    }

    public static synchronized VideoConfiguration getLivingConfig() {
        int[] resolution = HelmetConfig.get().getLiveResolution();
        int codeRate = HelmetConfig.get().getLiveCodeRate();
        Log.d("txhlog", "liveResolution=" + resolution[0] + "  " + resolution[1] + " codeRate=" + codeRate);
        if (mLiveConfig == null) {
            mLiveConfig = new VideoConfiguration()
                    .setSize(resolution[0], resolution[1])
                    .setBps(codeRate)
                    .setFps(15)
                    .setIfi(2);
        } else {
            mLiveConfig.setSize(resolution[0], resolution[1]);
            mLiveConfig.setBps(codeRate);
        }
        return mLiveConfig;
    }

    public VideoConfiguration setSize(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public VideoConfiguration setBps(int maxBps) {
        this.maxBps = maxBps;
        return this;
    }

    public VideoConfiguration setFps(int fps) {
        this.fps = fps;
        return this;
    }

    public VideoConfiguration setIfi(int ifi) {
        this.ifi = ifi;
        return this;
    }
}