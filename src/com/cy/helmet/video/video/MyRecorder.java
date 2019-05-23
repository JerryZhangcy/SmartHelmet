package com.cy.helmet.video.video;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.cy.helmet.util.LogUtil;
import com.cy.helmet.video.callback.VideoLiveCallback;
import com.cy.helmet.video.callback.VideoRecordCallback;
import com.cy.helmet.video.configuration.VideoConfiguration;
import com.cy.helmet.video.constant.SopCastConstant;
import com.cy.helmet.video.mediacodec.VideoMediaCodec;
import com.cy.helmet.video.utils.SopCastLog;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;

@TargetApi(18)
public class MyRecorder {

    private MediaCodec mMediaCodec;
    private MediaCodec mLiveMediaCodec;

    private InputSurface mInputSurface;
    private InputSurface mLiveInputSurface;

    private OnVideoEncodeListener mListener;

    private MediaCodec.BufferInfo mBufferInfo;
    private MediaCodec.BufferInfo mLiveBufferInfo;

    private HandlerThread mHandlerThread;
    private HandlerThread mLiveHandlerThread;

    private Handler mEncoderHandler;
    private Handler mLiveEncoderHandler;
    private ReentrantLock encodeLock = new ReentrantLock();
    private ReentrantLock liveEncodeLock = new ReentrantLock();
    private volatile boolean isStarted;

    //locker to control recording and living thread
    private Object mLivingLock = new Object();
    private Object mLock = new Object();

    private VideoRecordCallback mRecordCallback;
    private VideoLiveCallback mLiveCallback;

    public void setVideoEncodeListener(OnVideoEncodeListener listener) {
        mListener = listener;
    }

    public void prepareEncoder() {
        if (mMediaCodec != null || mInputSurface != null) {
            throw new RuntimeException("prepareEncoder called twice?");
        }

        VideoConfiguration videoConfig = VideoConfiguration.getRecordingConfig();
        VideoConfiguration liveConfig = VideoConfiguration.getLivingConfig();
        Log.d("txhlog", "videoConfig=" + videoConfig.maxBps);
        Log.d("txhlog", "liveConfig=" + liveConfig.maxBps);
        mMediaCodec = VideoMediaCodec.getVideoMediaCodec(videoConfig);
        mLiveMediaCodec = VideoMediaCodec.getVideoMediaCodec(liveConfig);

        mHandlerThread = new HandlerThread("RECORD_ENCODER_TASK");
        mHandlerThread.start();
        mEncoderHandler = new Handler(mHandlerThread.getLooper());

        mLiveHandlerThread = new HandlerThread("LIVE_ENCODER_TASK");
        mLiveHandlerThread.start();
        mLiveEncoderHandler = new Handler(mLiveHandlerThread.getLooper());

        mBufferInfo = new MediaCodec.BufferInfo();
        mLiveBufferInfo = new MediaCodec.BufferInfo();
        isStarted = true;
    }

    public boolean firstTimeSetup() {
        if (mMediaCodec == null || mInputSurface != null) {
            return false;
        }

        try {
            mInputSurface = new InputSurface(mMediaCodec.createInputSurface());
            mMediaCodec.start();
        } catch (Exception e) {
            LogUtil.e(e);
            releaseEncoder();
            return false;
        }

        return true;
    }

    public boolean firstTimeSetupLive() {
        if (mLiveMediaCodec == null || mLiveInputSurface != null) {
            return false;
        }

        try {
            mLiveInputSurface = new InputSurface(mLiveMediaCodec.createInputSurface());
            mLiveMediaCodec.start();
        } catch (Exception e) {
            LogUtil.e(e);
            releaseLiveEncoder();
            return false;
        }

        return true;
    }


    public void startSwapData() {
        mEncoderHandler.post(swapDataRunnable);
    }

    public void startLiveSwapData() {
        mLiveEncoderHandler.post(swapLiveDataRunnable);
    }

    public void makeCurrent() {
        if (mInputSurface != null) {
            mInputSurface.makeCurrent();
        }
    }

    public void makeLiveCurrent() {
        if (mLiveInputSurface != null) {
            mLiveInputSurface.makeCurrent();
        }
    }

    public void swapBuffers() {
        if (mMediaCodec == null) {
            return;
        }

        mInputSurface.swapBuffers();
        mInputSurface.setPresentationTime(System.nanoTime());
    }

    public void swapLiveBuffers() {
        if (mLiveMediaCodec == null) {
            return;
        }
        mLiveInputSurface.swapBuffers();
        mLiveInputSurface.setPresentationTime(System.nanoTime());
    }

    private Runnable swapDataRunnable = new Runnable() {
        @Override
        public void run() {
            drainEncoder();
        }
    };

    private Runnable swapLiveDataRunnable = new Runnable() {
        @Override
        public void run() {
            drainLiveEncoder();
        }
    };

    public void stop() {
        if (!isStarted) {
            return;
        }
        isStarted = false;
        Log.e("YJQ", "stop my recorder..................");
        //mEncoderHandler.removeCallbacks(null);
        mHandlerThread.quit();
        mLiveHandlerThread.quit();
        //mLiveEncoderHandler.removeCallbacks(null);
        encodeLock.lock();
        releaseEncoder();
        releaseLiveEncoder();
        encodeLock.unlock();
    }

    private void releaseEncoder() {
        if (mMediaCodec != null) {
            try {
                mMediaCodec.signalEndOfInputStream();
                mMediaCodec.stop();
            } catch (Throwable t) {

            }

            mMediaCodec.release();
            mMediaCodec = null;
        }
        if (mInputSurface != null) {
            mInputSurface.release();
            mInputSurface = null;
        }
    }

    private void releaseLiveEncoder() {
        if (mLiveMediaCodec != null) {
            try {
                mLiveMediaCodec.signalEndOfInputStream();
                mLiveMediaCodec.stop();
            } catch (Throwable t) {
            }
            mLiveMediaCodec.release();
            mLiveMediaCodec = null;
        }

        if (mLiveInputSurface != null) {
            mLiveInputSurface.release();
            mLiveInputSurface = null;
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public boolean setRecorderBps(int bps) {
        if (mMediaCodec == null || mInputSurface == null) {
            return false;
        }
        SopCastLog.d(SopCastConstant.TAG, "bps :" + bps * 1024);
        Bundle bitrate = new Bundle();
        bitrate.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, bps * 1024);
        mMediaCodec.setParameters(bitrate);
        return true;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public boolean setLiveBps(int bps) {
        if (mLiveMediaCodec == null || mLiveInputSurface == null) {
            return false;
        }
        SopCastLog.d(SopCastConstant.TAG, "bps :" + bps * 1024);
        Bundle bitrate = new Bundle();
        bitrate.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, bps * 1024);
        mLiveMediaCodec.setParameters(bitrate);
        return true;
    }

    private void drainEncoder() {

        Log.e("YJQ", "drainEncoder..........");

        try {
            ByteBuffer[] outBuffers = mMediaCodec.getOutputBuffers();
            while (isStarted) {
                long startDrain = System.currentTimeMillis();
                if (!enableRecording) {
                    synchronized (mLock) {
                        if (!enableRecording) {
                            try {
                                mLock.wait();
                            } catch (InterruptedException e) {
                                continue;
                            }
                        }
                    }
                }

                try {
                    encodeLock.lock();
                    if (mMediaCodec != null) {
                        int outBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 2000);

                        Log.e("YJQ", "encodeTime_0: " + (System.currentTimeMillis() - startDrain));

                        if (outBufferIndex >= 0) {
                            ByteBuffer bb = outBuffers[outBufferIndex];
                            if (mListener != null && enableRecording) {
                                mListener.onVideoEncode(bb, mBufferInfo);
                            }

                            Log.e("YJQ", "encodeTime_1: " + (System.currentTimeMillis() - startDrain));

                            mMediaCodec.releaseOutputBuffer(outBufferIndex, false);
                            long finishDrain = System.currentTimeMillis();
                            Log.e("YJQ", "encodeTime_2: " + (finishDrain - startDrain));
                            Log.e("YJQ", " ");
                        } else {
                            try {
                                // wait 10ms
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        break;
                    }
                    encodeLock.unlock();
                } catch (Throwable t) {
                    if (enableRecording) {
                        setRecordState(false);
                        if (mRecordCallback != null) {
                            mRecordCallback.onRecordingFinish();
                        }
                    }
                }
            }
        } catch (Throwable t) {
            LogUtil.e("record runnable exception: ");
            LogUtil.e(new Exception(t));
        }
    }

    private void drainLiveEncoder() {
        try {
            ByteBuffer[] liveOutBuffers = mLiveMediaCodec.getOutputBuffers();
            while (isStarted) {
                if (!enableLiving) {
                    synchronized (mLivingLock) {
                        if (!enableLiving) {
                            try {
                                mLivingLock.wait();
                            } catch (InterruptedException e) {
                                continue;
                            }
                        }
                    }
                }

                try {
                    liveEncodeLock.lock();
                    if (mLiveMediaCodec != null) {
                        int outBufferIndex = mLiveMediaCodec.dequeueOutputBuffer(mLiveBufferInfo, 2000);
                        if (outBufferIndex >= 0) {
                            ByteBuffer bb = liveOutBuffers[outBufferIndex];
                            if (mListener != null && enableLiving) {
                                mListener.onLiveVideoEncode(bb, mLiveBufferInfo);
                            }
                            mLiveMediaCodec.releaseOutputBuffer(outBufferIndex, false);
                        } else {
                            try {
                                // wait 10ms
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        liveEncodeLock.unlock();
                    } else {
                        liveEncodeLock.unlock();
                        break;
                    }
                } catch (Throwable t) {
                    if (enableLiving) {
                        setLivingState(false);
                        if (mLiveCallback != null) {
                            LogUtil.e("live failed: ");
                            LogUtil.e(new Exception(t));
                            mLiveCallback.onLivingFinish();
                        }
                    }
                }
            }
        } catch (Throwable t) {
            LogUtil.e("live runnable exception: ");
            LogUtil.e(new Exception(t));
        }
    }

    private boolean enableLiving = false;
    private boolean enableRecording = false;

    public void setLivingState(boolean enable) {
        synchronized (mLivingLock) {
            if (enableLiving != enable) {
                if (!enableLiving) {
                    mLivingLock.notify();
                } else {
                    if (mLiveCallback != null) {
                        LogUtil.e("live finished: closed successfully.");
                        mLiveCallback.onLivingFinish();
                    }
                }
                enableLiving = enable;
            }
        }
    }

    public void setLiveCallback(VideoLiveCallback callback) {
        mLiveCallback = callback;
    }

    public void setRecordState(boolean enable) {
        synchronized (mLock) {
            if (enableRecording != enable) {
                if (!enableRecording) {
                    mLock.notify();
                } else {
                    if (mRecordCallback != null) {
                        mRecordCallback.onRecordingFinish();
                    }
                }
                enableRecording = enable;
            }
        }
    }

    public void setRecordCallback(VideoRecordCallback callback) {
        mRecordCallback = callback;
    }

    public boolean isLiving() {
        return enableLiving;
    }

    public boolean isRecording() {
        return enableRecording;
    }
}
