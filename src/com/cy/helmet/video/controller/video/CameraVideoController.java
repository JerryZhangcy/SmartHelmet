package com.cy.helmet.video.controller.video;

import android.os.Build;
import android.util.Log;

import com.cy.helmet.util.LogUtil;
import com.cy.helmet.video.callback.VideoLiveCallback;
import com.cy.helmet.video.callback.VideoRecordCallback;
import com.cy.helmet.video.constant.SopCastConstant;
import com.cy.helmet.video.utils.SopCastLog;
import com.cy.helmet.video.video.MyRecorder;
import com.cy.helmet.video.video.MyRenderer;
import com.cy.helmet.video.video.OnVideoEncodeListener;

public class CameraVideoController implements IVideoController {

    private MyRecorder mRecorder;
    private MyRenderer mRenderer;
    private OnVideoEncodeListener mListener;

    //state
    private boolean isRecording = false;
    private boolean isLiving = false;

    public CameraVideoController(MyRenderer renderer) {
        mRenderer = renderer;
    }

    @Override
    public synchronized void startRecord(OnVideoEncodeListener listener, VideoRecordCallback callback) {
        if (listener == null) {
            return;
        }

        mListener = listener;

        Log.e("YJQ", "Start video recording...");
        initRecorderIfNecessary();
        mRecorder.setRecordCallback(callback);
        mRecorder.setRecordState(true);
        isRecording = true;
    }

    @Override
    public synchronized void stopRecord() {
        isRecording = false;
        if (mRecorder != null) {
            mRecorder.setRecordState(false);
        }
        releaseRecorderIfNecessary();
    }

    @Override
    public synchronized void startLive(OnVideoEncodeListener listener, VideoLiveCallback callback) {
        if (listener == null) {
            return;
        }

        mListener = listener;

        Log.e("YJQ", "Start video living");
        initRecorderIfNecessary();
        mRecorder.setLiveCallback(callback);
        mRecorder.setLivingState(true);
        isLiving = true;
    }

    @Override
    public synchronized void stopLive() {
        isLiving = false;
        if (mRecorder != null) {
            mRecorder.setLivingState(false);
        }

        releaseRecorderIfNecessary();
    }

    @Override
    public boolean isLiving() {
        if (mRecorder != null) {
            return mRecorder.isLiving();
        }
        return false;
    }

    private void initRecorderIfNecessary() {
        if (!isRecording && !isLiving) {
            mRecorder = new MyRecorder();
            mRecorder.setVideoEncodeListener(mListener);
            mRecorder.prepareEncoder();
            mRenderer.setRecorder(mRecorder);
        }
    }

    private void releaseRecorderIfNecessary() {
        if (!isRecording && !isLiving) {
            mRenderer.setRecorder(null);
            if (mRecorder != null) {
                mRecorder.setVideoEncodeListener(null);
                mRecorder.stop();
                mRecorder = null;
            }
        }
    }

    public void setVideoEncoderListener(OnVideoEncodeListener listener) {
        mListener = listener;
    }

    @Override
    public boolean setVideoBps(int bps) {
        //重新设置硬编bps，在低于19的版本需要重启编码器
        boolean result = false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            //由于重启硬编编码器效果不好，此次不做处理
            SopCastLog.d(SopCastConstant.TAG, "Bps need change, but MediaCodec do not support.");
        } else {
            if (mRecorder != null) {
                LogUtil.e("record bps changed: " + bps);
                mRecorder.setRecorderBps(bps);
                result = true;
            }
        }
        return result;
    }

    @Override
    public boolean setLiveBps(int bps) {
        //重新设置硬编bps，在低于19的版本需要重启编码器
        boolean result = false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            //由于重启硬编编码器效果不好，此次不做处理
            SopCastLog.d(SopCastConstant.TAG, "Bps need change, but MediaCodec do not support.");
        } else {
            if (mRecorder != null) {
                LogUtil.e("live bps changed: " + bps);
                mRecorder.setLiveBps(bps);
                result = true;
            }
        }
        return result;
    }
}
