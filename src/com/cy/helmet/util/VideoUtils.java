package com.cy.helmet.util;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.cy.helmet.HelmetApplication;
import com.cy.helmet.HelmetMainService;

/**
 * Created by tangxiaohui on 2018/1/22.
 */

public class VideoUtils {

    public static void takePhoto(boolean playVoice, boolean isCompress) {
        Context context = HelmetApplication.mAppContext;
        Intent intent = new Intent(context, HelmetMainService.class);
        Bundle bundle = new Bundle();
        bundle.putString("ACTION", "TAKE_PHOTO");
        bundle.putBoolean("NEED_COMPRESS", isCompress);
        bundle.putBoolean("PLAY_VOICE", playVoice);
        intent.putExtras(bundle);
        context.startService(intent);
    }

    public static void startRecordVideo(boolean isManual, boolean playVoice) {
        Context context = HelmetApplication.mAppContext;
        Intent intent = new Intent(context, HelmetMainService.class);
        Bundle bundle = new Bundle();
        bundle.putString("ACTION", "START_RECORD_VIDEO");
        bundle.putBoolean("IS_MANUAL", isManual);
        bundle.putBoolean("PLAY_VOICE", playVoice);
        intent.putExtras(bundle);
        context.startService(intent);
    }

    public static void stopRecordVideo(boolean isManual) {
        Context context = HelmetApplication.mAppContext;
        Intent intent = new Intent(context, HelmetMainService.class);
        Bundle bundle = new Bundle();
        bundle.putString("ACTION", "STOP_RECORD_VIDEO");
        bundle.putBoolean("IS_MANUAL", isManual);
        intent.putExtras(bundle);
        context.startService(intent);
    }

    public static void startLiveStream(Context context, String url,int frameSize) {
        Intent intent = new Intent(context, HelmetMainService.class);
        Bundle bundle = new Bundle();
        bundle.putString("ACTION", "START_LIVE_STREAM");
        bundle.putString("URL", url);
        bundle.putInt("FRAME_SIZE",frameSize);
        intent.putExtras(bundle);
        context.startService(intent);
    }

    public static void stopLiveStream(Context context) {
        Intent intent = new Intent(context, HelmetMainService.class);
        Bundle bundle = new Bundle();
        bundle.putString("ACTION", "STOP_LIVE_STREAM");
        intent.putExtras(bundle);
        context.startService(intent);
    }
}
