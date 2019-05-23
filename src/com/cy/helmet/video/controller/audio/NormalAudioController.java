package com.cy.helmet.video.controller.audio;

import android.annotation.TargetApi;

import com.cy.helmet.video.audio.AudioProcessor;
import com.cy.helmet.video.audio.OnAudioEncodeListener;
import com.cy.helmet.video.configuration.AudioConfiguration;
import com.cy.helmet.video.constant.SopCastConstant;
import com.cy.helmet.video.utils.SopCastLog;
import com.cy.helmet.voice.AudioRecorder;

public class NormalAudioController implements IAudioController {
    private OnAudioEncodeListener mListener;
    private AudioProcessor mAudioProcessor;
    private AudioConfiguration mAudioConfiguration;

    public NormalAudioController() {
        mAudioConfiguration = AudioConfiguration.createDefault();
    }

    public void setAudioConfiguration(AudioConfiguration audioConfiguration) {
        mAudioConfiguration = audioConfiguration;
    }

    public void setAudioEncodeListener(OnAudioEncodeListener listener) {
        mListener = listener;
    }

    public synchronized void start() {
        mAudioProcessor = new AudioProcessor(mAudioConfiguration);
        mAudioProcessor.setAudioHEncodeListener(mListener);
        mAudioProcessor.start();
        AudioRecorder.getInstance().startRecord(mAudioProcessor);
    }

    public synchronized void stop() {
        SopCastLog.d(SopCastConstant.TAG, "Audio Recording stop");
        if (mAudioProcessor != null) {
            mAudioProcessor.stopEncode();
        }

        AudioRecorder.getInstance().stopRecord(mAudioProcessor);
    }
}
