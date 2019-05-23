package com.cy.helmet.video.controller.audio;

import com.cy.helmet.video.audio.OnAudioEncodeListener;
import com.cy.helmet.video.configuration.AudioConfiguration;

public interface IAudioController {
    void start();

    void stop();

    void setAudioConfiguration(AudioConfiguration audioConfiguration);

    void setAudioEncodeListener(OnAudioEncodeListener listener);
}
