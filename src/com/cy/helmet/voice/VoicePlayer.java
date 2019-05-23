package com.cy.helmet.voice;

import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.cy.helmet.HelmetApplication;
import com.cy.helmet.conn.HelmetMessageSender;
import com.cy.helmet.factory.AudioMessageFactory;
import com.cy.helmet.util.LogUtil;

import java.io.IOException;

/**
 * Created by yaojiaqing on 2018/1/29.
 */

class VoicePlayer {

    MediaPlayer mAudioPlayer = new MediaPlayer();


    private Voice mCurPlayVoice = null;

    private Voice mBlockVoice = null;

    private HandlerThread mHandlerThread;
    private Handler mPlayHandler;

    public VoicePlayer() {
        mHandlerThread = new HandlerThread("VOICE_PLAYER_THREAD");
        mHandlerThread.start();
        mPlayHandler = new PlayHandler(mHandlerThread.getLooper());
    }

    public synchronized void playVoice(Voice voice) {
        if (voice != null && voice.isValid()) {
            Message msg = mPlayHandler.obtainMessage();
            msg.obj = voice;
            mPlayHandler.sendMessage(msg);
        }
    }

    public boolean isPlaying() {
        return mPlayHandler.hasMessages(Voice.VOICE_STYLE_FILE) || mPlayHandler.hasMessages(Voice.VOICE_STYLE_RES);
    }

    class PlayHandler extends Handler {

        public PlayHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void dispatchMessage(Message msg) {
            super.dispatchMessage(msg);
            Voice voice = (Voice) msg.obj;

            if (voice == null) {
                return;
            }

            if (!voice.isValid()) {
                voice.callBackIfNecessary(false);
                return;
            }

            int newVoicePriority = voice.getPriority();

            if (mBlockVoice != null) {
                int blockVoicePriority = mBlockVoice.getPriority();
                if (newVoicePriority > blockVoicePriority) {
                    //drop low priority voice
                    return;
                } else if (newVoicePriority == blockVoicePriority) {
                    //update latest block
                    mBlockVoice = voice;
                    return;
                } else {
                    //higher priority, interrupt block voice and check playing voice
                    if (mBlockVoice != null) {
                        mBlockVoice.callBackIfNecessary(false);
                    }
                    mBlockVoice = null;
                }
            }

            if (mCurPlayVoice != null) {
                boolean interrupt = false;
                int curVoicePriority = mCurPlayVoice.getPriority();
                if (newVoicePriority < curVoicePriority) {
                    interrupt = true;
                } else if (newVoicePriority == curVoicePriority) {
                    if (newVoicePriority == Voice.VOICE_PRIORITY_2 && mCurPlayVoice.isRecordVoice()) {
                        mBlockVoice = voice;
                    } else {
                        interrupt = true;
                    }
                } else {
                    voice.callBackIfNecessary(false);
                }

                if (interrupt) {
                    if (mAudioPlayer.isPlaying()) {
                        mAudioPlayer.stop();
                    }

                    if (mCurPlayVoice != null) {
                        mCurPlayVoice.callBackIfNecessary(false);
                    }

                    play(voice);
                }
            } else {
                play(voice);
            }
        }

        private void play(final Voice voice) {

            if (voice == null) {
                return;
            }

            try {
                mCurPlayVoice = voice;
                mAudioPlayer.reset();

                if (voice.isResID()) {
                    AssetFileDescriptor voiceFile = HelmetApplication.mAppContext.getResources().openRawResourceFd(voice.getResId());
                    mAudioPlayer.setDataSource(voiceFile.getFileDescriptor(), voiceFile.getStartOffset(), voiceFile.getLength());
                } else if (voice.isFile()) {
                    mAudioPlayer.setDataSource(voice.getFile().getAbsolutePath());
                    if (!TextUtils.isEmpty(voice.getUUID())) {
                        //start play resp
                        HelmetMessageSender.sendRemoteVoiceResp(voice.getUUID(), AudioMessageFactory.START_PLAY_VOICE_MESSAGE);
                    }
                }

                mAudioPlayer.prepare();
                mAudioPlayer.start();
                mAudioPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        mPlayHandler.postAtFrontOfQueue(new Runnable() {
                            @Override
                            public void run() {

                                if (mCurPlayVoice != null &&
                                        mCurPlayVoice.getVoiceID() == HelmetVoiceManager.SOS_SEND_SUCCESS) {
                                    Voice.mTriggerSOS = false;
                                }

                                if (mCurPlayVoice != null) {
                                    mCurPlayVoice.callBackIfNecessary(true);
                                    mCurPlayVoice = null;
                                }

                                if (mBlockVoice != null) {
                                    play(mBlockVoice);
                                    mBlockVoice = null;
                                }
                            }
                        });

                        if (voice.isFile() && !TextUtils.isEmpty(voice.getUUID())) {
                            HelmetMessageSender.sendRemoteVoiceResp(voice.getUUID(), AudioMessageFactory.FINISH_PLAY_VOICE_MESSAGE);
                        }
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                LogUtil.e(e);
                if (mCurPlayVoice != null) {
                    mCurPlayVoice.callBackIfNecessary(false);
                    mCurPlayVoice = null;
                }
            }
        }
    }
}
