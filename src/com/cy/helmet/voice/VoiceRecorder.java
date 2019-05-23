package com.cy.helmet.voice;

import com.cy.helmet.callback.OnPlayCompleteListener;
import com.cy.helmet.conn.HelmetMessageSender;
import com.cy.helmet.storage.FileUtil;
import com.cy.helmet.util.LogUtil;
import com.czt.mp3recorder.OnRecordListener;

import java.io.File;

/**
 * Created by ubuntu on 18-1-2.
 */

class VoiceRecorder {

    private boolean isPreRecording = false;

    Mp3Processor mProcessor = new Mp3Processor();

    public VoiceRecorder() {
    }

    public boolean isRecording() {
        return mProcessor.isRecording();
    }

    public boolean breakRecording() {
        return mProcessor.breakRecordingIfNecessary();
    }

    public void start(final int durationSec) {
        if (mProcessor.isRecording() || isPreRecording) {
            return;
        }

        if (durationSec <= 0) {
            return;
        }

        isPreRecording = true;
        HelmetVoiceManager.getInstance().playStartTalk(durationSec, new OnPlayCompleteListener() {
            @Override
            public void onPlayComplete(boolean success) {
                LogUtil.e("play start talk result: " + success);
                if (success) {
                    mProcessor.startRecord(FileUtil.getAudioFile("send_voice_file.mp3"), durationSec, new OnRecordListener() {
                        @Override
                        public void onStartRecord() {
                            LogUtil.e("start recording......");
                        }

                        @Override
                        public void onFinishRecord(File file) {
                            LogUtil.e("stop recording......");
                            if (file.exists()) {
                                HelmetVoiceManager.getInstance().sendRecordTalkMessage(file);
                            }
                            HelmetVoiceManager.getInstance().playLocalVoice(HelmetVoiceManager.TALK_FINISH);
                        }
                    });
                } else {
                    HelmetMessageSender.sendTalkFailedRequest();
                }

                isPreRecording = false;
            }
        });
    }
}
