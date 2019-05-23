package com.cy.helmet.video.camera;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.provider.Settings;
import android.util.Log;

import com.cy.helmet.HelmetApplication;
import com.cy.helmet.WorkThreadManager;
import com.cy.helmet.storage.FileUtil;
import com.cy.helmet.util.LogUtil;
import com.cy.helmet.video.camera.exception.CameraHardwareException;
import com.cy.helmet.video.camera.exception.CameraNotSupportException;
import com.cy.helmet.video.configuration.CameraConfiguration;
import com.cy.helmet.video.controller.PhotoCallback;
import com.cy.helmet.video.utils.SopCastLog;
import com.cy.helmet.voice.HelmetVoiceManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @Title: CameraHolder
 * @Package com.youku.crazytogether.app.modules.sopCastV2
 * @Description:
 * @Author Jim
 * @Date 16/3/23
 * @Time 上午11:57
 * @Version
 */
@TargetApi(14)
public class CameraHolder {
    private static final String TAG = "CameraHolder";
    private final static int FOCUS_WIDTH = 80;
    private final static int FOCUS_HEIGHT = 80;

    private List<CameraData> mCameraDatas;
    private Camera mCameraDevice;
    private CameraData mCameraData;
    private State mState;
    private SurfaceTexture mTexture;
    private boolean isTouchMode = false;
    private boolean isOpenBackFirst = false;

    private int mIgnoreFrameCount = 0;
    private static final int MIN_IGNORE = 15;//add by Jerry

    private CameraConfiguration mConfiguration = CameraConfiguration.createDefault();

    private PhotoCallback photoCallback;

    public enum State {
        INIT,
        OPENED,
        PREVIEW
    }

    private static CameraHolder sHolder;

    public static synchronized CameraHolder instance() {
        if (sHolder == null) {
            sHolder = new CameraHolder();
        }
        return sHolder;
    }

    private CameraHolder() {
        mState = State.INIT;
    }

    public int getNumberOfCameras() {
        return Camera.getNumberOfCameras();
    }

    public CameraData getCameraData() {
        return mCameraData;
    }

    public boolean isLandscape() {
        return (mConfiguration.orientation != CameraConfiguration.Orientation.PORTRAIT);
    }

    public synchronized Camera openCamera()
            throws CameraHardwareException, CameraNotSupportException {
        if (mCameraDatas == null || mCameraDatas.size() == 0) {
            mCameraDatas = CameraUtils.getAllCamerasData(isOpenBackFirst);
        }
        CameraData cameraData = mCameraDatas.get(0);
        if (mCameraDevice != null && mCameraData == cameraData) {
            return mCameraDevice;
        }
        if (mCameraDevice != null) {
            releaseCamera();
        }
        try {
            SopCastLog.d(TAG, "open camera id:" + cameraData.cameraID + ", isFront: " + (cameraData.cameraFacing == 1));
            mCameraDevice = Camera.open(cameraData.cameraID);
        } catch (RuntimeException e) {
            SopCastLog.e(TAG, "fail to connect Camera");
            throw new CameraHardwareException(e);
        }
        if (mCameraDevice == null) {
            throw new CameraNotSupportException();
        }
        try {
            CameraUtils.initCameraParams(mCameraDevice, cameraData, isTouchMode, mConfiguration);
        } catch (Exception e) {
            e.printStackTrace();
            mCameraDevice.release();
            mCameraDevice = null;
            throw new CameraNotSupportException();
        }
        mCameraData = cameraData;
        mState = State.OPENED;
        return mCameraDevice;
    }

    public void setSurfaceTexture(SurfaceTexture texture) {
        mTexture = texture;
        if (mState == State.PREVIEW && mCameraDevice != null && mTexture != null) {
            try {
                mCameraDevice.setPreviewTexture(mTexture);
            } catch (IOException e) {
                releaseCamera();
            }
        }
    }

    public State getState() {
        return mState;
    }

    public void setConfiguration(CameraConfiguration configuration) {
        isTouchMode = (configuration.focusMode != CameraConfiguration.FocusMode.AUTO);
        isOpenBackFirst = (configuration.facing != CameraConfiguration.Facing.FRONT);
        mConfiguration = configuration;
    }

    //is taking photo now
    private boolean mIsTakingPhoto = false;
    //refuse taking photo request
    private boolean mRefuseTakingPhoto = false;
    private boolean mNeedCompress = false;
    private boolean mPlayTakePhotoVoice = true;

    public boolean takePhoto(boolean playVoice, boolean compress, PhotoCallback callback) {
        if (mRefuseTakingPhoto) {
            return false;
        } else {
            mRefuseTakingPhoto = true;
            mIsTakingPhoto = true;
            mNeedCompress = compress;
            photoCallback = callback;
            mPlayTakePhotoVoice = playVoice;
            mIgnoreFrameCount = 0;
            return true;
        }
    }

    public boolean isTakingPhoto() {
        return mRefuseTakingPhoto;
    }

    public synchronized void startPreview() {
        if (mState != State.OPENED) {
            return;
        }
        if (mCameraDevice == null) {
            return;
        }
        if (mTexture == null) {
            return;
        }
        try {
            mCameraDevice.setPreviewTexture(mTexture);
            mCameraDevice.startPreview();
            mState = State.PREVIEW;
        } catch (Exception e) {
            releaseCamera();
            e.printStackTrace();
        }

        mCameraDevice.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(final byte[] data, Camera camera) {
                // At preview mode, the frame data will push to here.
                if ((mIgnoreFrameCount++ < MIN_IGNORE)
                        && (Settings.Secure.getInt(HelmetApplication.mAppContext.getContentResolver(),
                        "Helmet_first_capture", 0) == 0)) {//modify by Jerry
                    return;
                }

                if (mIsTakingPhoto) {
                    final byte[] photoBytes = Arrays.copyOf(data, data.length);
                    mIsTakingPhoto = false;
                    final Camera.Size previewSize = mCameraDevice.getParameters().getPreviewSize();
                    WorkThreadManager.executeOnSubThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                long start = System.currentTimeMillis();
                                YuvImage yunImage = new YuvImage(photoBytes, ImageFormat.NV21, previewSize.width, previewSize.height, null);
                                if (yunImage != null) {
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();

                                    long yumStart = System.currentTimeMillis();
                                    yunImage.compressToJpeg(new Rect(0, 0, previewSize.width, previewSize.height), 100, baos);
                                    Log.e("YJQ", "yun2JPEG:" + (System.currentTimeMillis() - yumStart));

                                    Bitmap bitmap = BitmapFactory.decodeByteArray(baos.toByteArray(), 0, baos.size());

                                    //处理前置摄像头镜像
                                    boolean isFront = false;//modify by Jerry
                                    CameraData cameraData = CameraHolder.instance().getCameraData();
                                    if (cameraData != null) {
                                        int facing = cameraData.cameraFacing;
                                        isFront = (facing == CameraData.FACING_FRONT);
                                    }

                                    if (mPlayTakePhotoVoice) {
                                        //play take photo voice
                                        HelmetVoiceManager.getInstance().playSoundPool(HelmetVoiceManager.CAMERA_CLICK);
                                    } else {
                                        mPlayTakePhotoVoice = true;
                                    }

                                    long saveBitmap = System.currentTimeMillis();
                                    FileUtil.saveBitmapFromYuvImage(bitmap, isFront, mNeedCompress, photoCallback);
                                    Log.e("YJQ", "saveBitmapFremYum:" + (System.currentTimeMillis() - saveBitmap));

                                    //add by Jerry
                                    Settings.Secure.putInt(HelmetApplication.mAppContext.getContentResolver(),
                                            "Helmet_first_capture", 1);

                                    try {
                                        baos.close();
                                    } catch (Exception e) {
                                    }
                                }

                                Log.e("YJQ", "saveBitmapTime:" + (System.currentTimeMillis() - start));
                            } catch (Exception e) {
                                LogUtil.e(e);
                                if (photoCallback != null) {
                                    photoCallback.updatePhotoStatus(false, null);
                                }
                            }

                            mRefuseTakingPhoto = false;
                        }
                    });
                }
            }
        });
    }

    public synchronized void stopPreview() {
        if (mState != State.PREVIEW) {
            return;
        }
        if (mCameraDevice == null) {
            return;
        }
        mCameraDevice.setPreviewCallback(null);
        Camera.Parameters cameraParameters = mCameraDevice.getParameters();
        if (cameraParameters != null && cameraParameters.getFlashMode() != null
                && !cameraParameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_OFF)) {
            cameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        }
        mCameraDevice.setParameters(cameraParameters);
        mCameraDevice.stopPreview();
        mState = State.OPENED;
    }

    public synchronized void releaseCamera() {
        if (mState == State.PREVIEW) {
            stopPreview();
        }
        if (mState != State.OPENED) {
            return;
        }
        if (mCameraDevice == null) {
            return;
        }
        mCameraDevice.release();
        mCameraDevice = null;
        mCameraData = null;
        mState = State.INIT;
    }

    public void release() {
        mCameraDatas = null;
        mTexture = null;
        isTouchMode = false;
        isOpenBackFirst = false;
        mConfiguration = CameraConfiguration.createDefault();
    }

    public void setFocusPoint(int x, int y) {
        if (mState != State.PREVIEW || mCameraDevice == null) {
            return;
        }
        if (x < -1000 || x > 1000 || y < -1000 || y > 1000) {
            SopCastLog.w(TAG, "setFocusPoint: values are not ideal " + "x= " + x + " y= " + y);
            return;
        }
        Camera.Parameters params = mCameraDevice.getParameters();

        if (params != null && params.getMaxNumFocusAreas() > 0) {
            List<Camera.Area> focusArea = new ArrayList<Camera.Area>();
            focusArea.add(new Camera.Area(new Rect(x, y, x + FOCUS_WIDTH, y + FOCUS_HEIGHT), 1000));

            params.setFocusAreas(focusArea);

            try {
                mCameraDevice.setParameters(params);
            } catch (Exception e) {
                // Ignore, we might be setting it too
                // fast since previous attempt
            }
        } else {
            SopCastLog.w(TAG, "Not support Touch focus mode");
        }
    }

    public boolean doAutofocus(Camera.AutoFocusCallback focusCallback) {
        if (mState != State.PREVIEW || mCameraDevice == null) {
            return false;
        }
        // Make sure our auto settings aren't locked
        Camera.Parameters params = mCameraDevice.getParameters();
        if (params.isAutoExposureLockSupported()) {
            params.setAutoExposureLock(false);
        }

        if (params.isAutoWhiteBalanceLockSupported()) {
            params.setAutoWhiteBalanceLock(false);
        }

        mCameraDevice.setParameters(params);
        mCameraDevice.cancelAutoFocus();
        mCameraDevice.autoFocus(focusCallback);
        return true;
    }

    public void changeFocusMode(boolean touchMode) {
        if (mState != State.PREVIEW || mCameraDevice == null || mCameraData == null) {
            return;
        }
        isTouchMode = touchMode;
        mCameraData.touchFocusMode = touchMode;
        if (touchMode) {
            CameraUtils.setTouchFocusMode(mCameraDevice);
        } else {
            CameraUtils.setAutoFocusMode(mCameraDevice);
        }
    }

    public void switchFocusMode() {
        changeFocusMode(!isTouchMode);
    }

    public float cameraZoom(boolean isBig) {
        if (mState != State.PREVIEW || mCameraDevice == null || mCameraData == null) {
            return -1;
        }
        Camera.Parameters params = mCameraDevice.getParameters();
        if (isBig) {
            params.setZoom(Math.min(params.getZoom() + 1, params.getMaxZoom()));
        } else {
            params.setZoom(Math.max(params.getZoom() - 1, 0));
        }
        mCameraDevice.setParameters(params);
        return (float) params.getZoom() / params.getMaxZoom();
    }

    public boolean switchCamera() {
        if (mState != State.PREVIEW) {
            return false;
        }
        try {
            CameraData camera = mCameraDatas.remove(1);
            mCameraDatas.add(0, camera);
            openCamera();
            startPreview();
            return true;
        } catch (Exception e) {
            CameraData camera = mCameraDatas.remove(1);
            mCameraDatas.add(0, camera);
            try {
                openCamera();
                startPreview();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
            return false;
        }
    }

    public boolean switchLight() {
        if (mState != State.PREVIEW || mCameraDevice == null || mCameraData == null) {
            return false;
        }
        if (!mCameraData.hasLight) {
            return false;
        }
        Camera.Parameters cameraParameters = mCameraDevice.getParameters();
        if (cameraParameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_OFF)) {
            cameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        } else {
            cameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        }
        try {
            mCameraDevice.setParameters(cameraParameters);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
