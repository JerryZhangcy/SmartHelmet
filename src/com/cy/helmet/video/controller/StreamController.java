package com.cy.helmet.video.controller;

import android.media.MediaCodec;
import android.util.Log;

import com.cy.helmet.util.LogUtil;
import com.cy.helmet.video.audio.OnAudioEncodeListener;
import com.cy.helmet.video.callback.VideoLiveCallback;
import com.cy.helmet.video.callback.VideoRecordCallback;
import com.cy.helmet.video.configuration.AudioConfiguration;
import com.cy.helmet.video.controller.audio.IAudioController;
import com.cy.helmet.video.controller.audio.NormalAudioController;
import com.cy.helmet.video.controller.video.CameraVideoController;
import com.cy.helmet.video.controller.video.IVideoController;
import com.cy.helmet.video.stream.packer.Packer;
import com.cy.helmet.video.stream.packer.flv.FlvPacker;
import com.cy.helmet.video.stream.packer.rtmp.RtmpPacker;
import com.cy.helmet.video.stream.sender.local.LocalSender;
import com.cy.helmet.video.stream.sender.rtmp.RtmpSender;
import com.cy.helmet.video.utils.SopCastUtils;
import com.cy.helmet.video.video.MyRenderer;
import com.cy.helmet.video.video.OnVideoEncodeListener;

import java.nio.ByteBuffer;

public class StreamController implements OnAudioEncodeListener,
        OnVideoEncodeListener,
        Packer.OnPacketListener {

    //flv packer and sender
    private FlvPacker mFlvPacker;
    private LocalSender mFlvSender;

    //rtmp packer and sender
    private RtmpPacker mRtmpPacker;
    private RtmpSender mRtmpSender;

    private IVideoController mVideoController;
    private IAudioController mAudioController;

    //callback
    private VideoRecordCallback mRecordCallback;
    private VideoLiveCallback mLiveCallback;

    public StreamController(MyRenderer render) {

        if (render == null) {
            throw new RuntimeException("Camera preview not ready.");
        }

        mVideoController = new CameraVideoController(render);
        mAudioController = new NormalAudioController();
    }

    public void setAudioConfiguration(AudioConfiguration audioConfiguration) {
        mAudioController.setAudioConfiguration(audioConfiguration);
    }

    public boolean setVideoBps(int bps) {
        return mVideoController.setVideoBps(bps);
    }

    public boolean setLiveBps(int bps) {
        return mVideoController.setLiveBps(bps);
    }

    @Override
    public void onAudioEncode(ByteBuffer bb, MediaCodec.BufferInfo bi) {
        if (mFlvPacker != null) {
            mFlvPacker.onAudioData(bb, bi);
        }

        if (mRtmpPacker != null) {
            mRtmpPacker.onAudioData(bb, bi);
        }
    }

    @Override
    public void onVideoEncode(ByteBuffer bb, MediaCodec.BufferInfo bi) {
        if (mFlvPacker != null) {
            mFlvPacker.onVideoData(bb, bi);
        }
    }

    @Override
    public void onLiveVideoEncode(ByteBuffer bb, MediaCodec.BufferInfo bi) {
        if (mRtmpPacker != null) {
            mRtmpPacker.onVideoData(bb, bi);
        }
    }

    @Override
    public void onPacket(byte[] data, int packetType) {
        if (mFlvSender != null) {
            mFlvSender.onData(data, packetType);
        }
    }

    @Override
    public void onPacketLiveStream(byte[] data, int packetType) {
        if (mRtmpSender != null) {
            mRtmpSender.onData(data, packetType);
        }
    }

    @Override
    public boolean onTransferFile() {
        if (mFlvSender != null) {
            return mFlvSender.onTransferFile();
        }

        return false;
    }

    public void setFlvPackerSender(FlvPacker packer, LocalSender sender) {
        mFlvPacker = packer;
        mFlvSender = sender;
        mFlvPacker.setPacketListener(this);
    }

    public void setRtmpPackerSender(RtmpPacker packer, RtmpSender sender) {
        mRtmpPacker = packer;
        mRtmpSender = sender;
        mRtmpPacker.setPacketListener(this);
    }

    public synchronized void startRecording(VideoRecordCallback callback) {
        if (mFlvPacker == null) {
            return;
        }

        if (mFlvSender == null) {
            return;
        }

        mRecordCallback = callback;

        mFlvPacker.start();
        mFlvSender.start();
        mVideoController.startRecord(StreamController.this, callback);
        mAudioController.setAudioEncodeListener(StreamController.this);
        mAudioController.start();
    }

    public synchronized void stopRecording() {
        if (mVideoController != null) {
            mVideoController.stopRecord();
        }

        if (mFlvSender != null) {
            mFlvSender.stop();
        }

        if (mFlvPacker != null) {
            mFlvPacker.stop();
        }

        if (mRtmpPacker == null || !mRtmpPacker.isRunning()) {
            mAudioController.setAudioEncodeListener(null);
            mAudioController.stop();
        }
    }

    public synchronized void startLiving(final String url, final int frameSize, final VideoLiveCallback callback) {

        LogUtil.e("start living......");

        if (mRtmpPacker == null) {
            return;
        }

        if (mRtmpSender == null) {
            return;
        }

        if (mVideoController.isLiving()) {
            return;
        }

        mLiveCallback = callback;

        mRtmpSender.setSenderListener(mSenderListener);
        mRtmpSender.setVideoStatusCallback(mLiveCallback);
        LogUtil.e("LiveUrl=" + url);
        mRtmpSender.setAddress(url);
        mRtmpSender.setFrameBufferSize(frameSize);
        mRtmpSender.connect();
    }

    private void startLivingInternal() {
        SopCastUtils.getLivingHandler().post(new Runnable() {
            @Override
            public void run() {
                if (mRtmpPacker == null) {
                    return;
                }

                if (mRtmpSender == null) {
                    return;
                }

                mRtmpPacker.start();
                mRtmpSender.start();

                mVideoController.startLive(StreamController.this, mLiveCallback);
                mAudioController.setAudioEncodeListener(StreamController.this);
                mAudioController.start();
            }
        });
    }

    public synchronized void stopLiving() {
        SopCastUtils.getLivingHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.e("Jerry_zhangcy", "<<<<<<<<<<停止直播>>>>>>>>  mVideoController = " + (mVideoController == null)
                      + " mFlvPacker = " + (mFlvPacker == null));
                if (mVideoController != null) {
                    mVideoController.stopLive();
                }

                if (mRtmpSender != null) {
                    mRtmpSender.stop();
                }

                if (mRtmpPacker != null) {
                    mRtmpPacker.stop();
                }

                if (mFlvPacker == null || !mFlvPacker.isRunning()) {
                    mAudioController.setAudioEncodeListener(null);
                    mAudioController.stop();
                }
            }
        },1000);
    }

    private RtmpSender.OnSenderListener mSenderListener = new RtmpSender.OnSenderListener() {
        @Override
        public void onConnecting() {
            Log.e("YJQ", ">>>>>>>>>>>>>>>on connecting<<<<<<<<<<<<<<<");
        }

        @Override
        public void onConnected() {
            Log.e("YJQ", ">>>>>>>>>>>>>>>on connected<<<<<<<<<<<<<<<");
            Log.e("Jerry_zhangcy", ">>>>>>>>>>>>>>>Rtmp 已经连接<<<<<<<<<<<<<<<");
            startLivingInternal();
        }

        @Override
        public void onDisConnected() {
            Log.e("YJQ", ">>>>>>>>>>>>>>>on disConnected<<<<<<<<<<<<<<<");
            Log.e("Jerry_zhangcy", ">>>>>>>>>>>>>>>Rtmp 断开连接<<<<<<<<<<<<<<<");
            stopLiving();
        }

        @Override
        public void onPublishFail() {
            Log.e("YJQ", ">>>>>>>>>>>>>>>on publish failed<<<<<<<<<<<<<<<");
            stopLiving();
        }

        @Override
        public void onNetGood() {
        }

        @Override
        public void onNetBad() {
        }
    };

    public String getRecordingFileName() {
        if (mFlvSender != null) {
            return mFlvSender.getRecordingFileName();
        }
        return "";
    }
}
