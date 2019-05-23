package com.cy.helmet.video;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.os.Build;

import com.cy.helmet.video.audio.AudioUtils;
import com.cy.helmet.video.configuration.AudioConfiguration;
import com.cy.helmet.video.configuration.VideoConfiguration;
import com.cy.helmet.video.constant.SopCastConstant;
import com.cy.helmet.video.mediacodec.AudioMediaCodec;
import com.cy.helmet.video.mediacodec.MediaCodecHelper;
import com.cy.helmet.video.mediacodec.VideoMediaCodec;
import com.cy.helmet.video.utils.SopCastLog;
import com.cy.helmet.video.video.MyRenderer;

/**
 * Created by jiaqing on 2018/3/6.
 */

public class CameraUtil {

    private static final String TAG = SopCastConstant.TAG;

    public static final int NO_ERROR = 0;
    public static final int VIDEO_TYPE_ERROR = 1;
    public static final int AUDIO_TYPE_ERROR = 2;
    public static final int VIDEO_CONFIGURATION_ERROR = 3;
    public static final int AUDIO_CONFIGURATION_ERROR = 4;
    public static final int CAMERA_ERROR = 5;
    public static final int AUDIO_ERROR = 6;
    public static final int AUDIO_AEC_ERROR = 7;
    public static final int SDK_VERSION_ERROR = 8;
    public static final int CAMERA_PREVIEW_NOT_READY_ERROR = 9;


    public static int check(MyRenderer render) {

        if (render == null) {
            SopCastLog.w(TAG, "Camera preview not ready error");
            return CAMERA_PREVIEW_NOT_READY_ERROR;
        }

        VideoConfiguration videoConfig = VideoConfiguration.getRecordingConfig();
        AudioConfiguration audioConfig = AudioConfiguration.createDefault();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            SopCastLog.w(TAG, "Android sdk version error");
            return SDK_VERSION_ERROR;
        }
        if (!checkAec()) {
            SopCastLog.w(TAG, "Doesn't support audio aec");
            return AUDIO_AEC_ERROR;
        }
        if (!render.isCameraOpen()) {
            SopCastLog.w(TAG, "The camera have not open");
            return CAMERA_ERROR;
        }

        MediaCodecInfo videoMediaCodecInfo = MediaCodecHelper.selectCodec(videoConfig.mime);
        if (videoMediaCodecInfo == null) {
            SopCastLog.w(TAG, "Video type error");
            return VIDEO_TYPE_ERROR;
        }

        MediaCodecInfo audioMediaCodecInfo = MediaCodecHelper.selectCodec(audioConfig.mime);
        if (audioMediaCodecInfo == null) {
            SopCastLog.w(TAG, "Audio type error");
            return AUDIO_TYPE_ERROR;
        }
        MediaCodec videoMediaCodec = VideoMediaCodec.getVideoMediaCodec(videoConfig);
        if (videoMediaCodec == null) {
            SopCastLog.w(TAG, "Video mediacodec configuration error");
            return VIDEO_CONFIGURATION_ERROR;
        }
        MediaCodec audioMediaCodec = AudioMediaCodec.getAudioMediaCodec(audioConfig);
        if (audioMediaCodec == null) {
            SopCastLog.w(TAG, "Audio mediacodec configuration error");
            return AUDIO_CONFIGURATION_ERROR;
        }

//delete to avoid restart AudioRecord
//        if (!AudioUtils.checkMicSupport(audioConfig)) {
//            SopCastLog.w(TAG, "Can not record the audio");
//            return AUDIO_ERROR;
//        }
        return NO_ERROR;
    }

    private static boolean checkAec() {
        AudioConfiguration audioConfig = AudioConfiguration.createDefault();
        if (audioConfig.aec) {
            if (audioConfig.frequency == 8000 ||
                    audioConfig.frequency == 16000) {
                if (audioConfig.channelCount == 1) {
                    return true;
                }
            }
            return false;
        } else {
            return true;
        }
    }
}
