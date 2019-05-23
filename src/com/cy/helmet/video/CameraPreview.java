package com.cy.helmet.video;

import android.content.Context;
import android.media.AudioManager;
import android.os.PowerManager;
import android.widget.Toast;

import com.cy.helmet.HelmetApplication;
import com.cy.helmet.R;
import com.cy.helmet.util.LogUtil;
import com.cy.helmet.video.callback.VideoLiveCallback;
import com.cy.helmet.video.callback.VideoRecordCallback;
import com.cy.helmet.video.camera.CameraData;
import com.cy.helmet.video.camera.CameraHolder;
import com.cy.helmet.video.camera.CameraListener;
import com.cy.helmet.video.camera.focus.FocusManager;
import com.cy.helmet.video.configuration.AudioConfiguration;
import com.cy.helmet.video.configuration.VideoConfiguration;
import com.cy.helmet.video.constant.SopCastConstant;
import com.cy.helmet.video.controller.PhotoCallback;
import com.cy.helmet.video.controller.StreamController;
import com.cy.helmet.video.entity.Watermark;
import com.cy.helmet.video.stream.packer.flv.FlvPacker;
import com.cy.helmet.video.stream.packer.rtmp.RtmpPacker;
import com.cy.helmet.video.stream.sender.local.LocalSender;
import com.cy.helmet.video.stream.sender.rtmp.RtmpSender;
import com.cy.helmet.video.ui.RenderSurfaceView;
import com.cy.helmet.video.utils.SopCastUtils;
import com.cy.helmet.video.utils.WeakHandler;
import com.cy.helmet.video.video.MyRenderer;

/**
 * Created by jiaqing on 2018/3/6.
 */

public class CameraPreview {

    private boolean mRecordingStatus = false;
    private boolean mLivingStatus = false;

    private static final String TAG = SopCastConstant.TAG;

    private Context mContext;

    protected MyRenderer mRenderer;
    protected RenderSurfaceView mRenderSurfaceView;
    private StreamController mStreamController;

    private PowerManager.WakeLock mWakeLock;
    private VideoConfiguration mVideoConfiguration = VideoConfiguration.getRecordingConfig();
    private AudioConfiguration mAudioConfiguration = AudioConfiguration.createDefault();
    private CameraListener mOutCameraOpenListener;
    private WeakHandler mHandler = new WeakHandler();

    private FocusManager mFocusManager;

    //callback
    private VideoRecordCallback mRecordCallback;
    private VideoLiveCallback mLiveCallback;


    public CameraPreview(RenderSurfaceView surfaceView, CameraListener listener) {
        mContext = HelmetApplication.mAppContext;
        mRenderSurfaceView = surfaceView;
        mOutCameraOpenListener = listener;
        initView();
    }

    private void initView() {
        mRenderSurfaceView.setZOrderMediaOverlay(false);
        mRenderer = mRenderSurfaceView.getRenderer();
        mStreamController = new StreamController(mRenderer);
        mRenderer.setCameraOpenListener(mCameraOpenListener);

        //focus
        mFocusManager = new FocusManager();

        PowerManager mPowerManager = ((PowerManager) mContext.getSystemService(Context.POWER_SERVICE));
        mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG);
    }

    public boolean setRecordBps(int bps) {
        return mStreamController.setVideoBps(bps);
    }

    public boolean setLiveBps(int bps) {
        return mStreamController.setLiveBps(bps);
    }

    private CameraListener mCameraOpenListener = new CameraListener() {
        @Override
        public void onOpenSuccess() {
            changeFocusModeUI();
            if (mOutCameraOpenListener != null) {
                mOutCameraOpenListener.onOpenSuccess();
            }

            Toast.makeText(HelmetApplication.mAppContext, R.string.open_camera_success, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onOpenFail(int error) {
            if (mOutCameraOpenListener != null) {
                mOutCameraOpenListener.onOpenFail(error);
            }
            Toast.makeText(HelmetApplication.mAppContext, R.string.open_camera_fail, Toast.LENGTH_SHORT).show();
        }
    };

    private void setAudioNormal() {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setSpeakerphoneOn(false);
    }

    protected void changeFocusModeUI() {
        CameraData cameraData = CameraHolder.instance().getCameraData();
        if (cameraData != null && cameraData.supportTouchFocus && cameraData.touchFocusMode) {
            if (mFocusManager != null) {
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        mFocusManager.refocus();
                    }
                }, 1000);
            }
        }
    }

    private void screenOn() {
        try {
            if (mWakeLock != null) {
                if (!mWakeLock.isHeld()) {
                    mWakeLock.acquire();
                }
            }
        } catch (Throwable t) {
        }
    }

    private void screenOff() {
        try {
            if (mWakeLock != null) {
                if (mWakeLock.isHeld()) {
                    mWakeLock.release();
                }
                mWakeLock = null;
            }
        } catch (Throwable t) {
        }
    }

    public void setFlvPackerSender(FlvPacker packer, LocalSender sender) {
        mStreamController.setFlvPackerSender(packer, sender);
    }

    public void setRtmpPackerSender(RtmpPacker packer, RtmpSender sender) {
        mStreamController.setRtmpPackerSender(packer, sender);
    }

    public void setWatermark(Watermark watermark) {
        mRenderer.setWatermark(watermark);
    }

    public void release() {
        CameraHolder.instance().releaseCamera();
        CameraHolder.instance().release();
        setAudioNormal();
        screenOff();
    }

    public final boolean takePhoto(final boolean playVoice, boolean compress, PhotoCallback callback) {
        CameraHolder holder = CameraHolder.instance();
        return holder.takePhoto(playVoice, compress, callback);
    }

    public final boolean isTakingPhoto() {
        return CameraHolder.instance().isTakingPhoto();
    }

    public void startRecording(final VideoRecordCallback callback) {
        SopCastUtils.getRecordingHandler().post(new Runnable() {
            @Override
            public void run() {
                try {
                    final int result = CameraUtil.check(mRenderer);
                    if (result == CameraUtil.NO_ERROR) {
                        chooseVoiceMode();
                        screenOn();
                        mStreamController.startRecording(callback);
                        callback.onRecordingStart(true);
                    } else {
                        LogUtil.e("start record failed, check error: " + result);
                        callback.onRecordingStart(false);
                    }
                } catch (Throwable t) {
                    LogUtil.e("start record failed: ");
                    LogUtil.e(new Exception(t));
                    callback.onRecordingStart(false);
                }
            }
        });
    }

    public void stopRecording() {
        SopCastUtils.getRecordingHandler().post(new Runnable() {
            @Override
            public void run() {
                try {
                    mStreamController.stopRecording();
                    setAudioNormal();
                    screenOff();
                } catch (Throwable t) {
                }
            }
        });
    }

    public void startLiving(final String url, final int frameSize, final VideoLiveCallback callback) {
        SopCastUtils.getLivingHandler().post(new Runnable() {
            @Override
            public void run() {
                try {
                    final int result = CameraUtil.check(mRenderer);
                    if (result == CameraUtil.NO_ERROR) {
                        chooseVoiceMode();
                        screenOn();
                        mStreamController.startLiving(url, frameSize, callback);
                    } else {
                        LogUtil.e("live failed: Camera check error(" + result + ")");
                        callback.onLivingStart(false);
                    }
                } catch (Throwable t) {
                    LogUtil.e("live failed: ");
                    LogUtil.e(new Exception(t));
                    callback.onLivingStart(false);
                }
            }
        });
    }

    public void stopLiving() {
        SopCastUtils.getLivingHandler().post(new Runnable() {
            @Override
            public void run() {
                try {
                    mStreamController.stopLiving();
                    setAudioNormal();
                    screenOff();
                } catch (Throwable t) {
                }
            }
        });
    }

    private void chooseVoiceMode() {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if (mAudioConfiguration.aec) {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.setSpeakerphoneOn(true);
        } else {
            audioManager.setMode(AudioManager.MODE_NORMAL);
            audioManager.setSpeakerphoneOn(false);
        }
    }

    public String getRecordingFileName() {
        if (mStreamController != null) {
            return mStreamController.getRecordingFileName();
        }
        return "";
    }
}
