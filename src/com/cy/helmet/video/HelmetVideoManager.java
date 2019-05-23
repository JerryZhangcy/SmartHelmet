package com.cy.helmet.video;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.cy.helmet.HelmetApplication;
import com.cy.helmet.HelmetClient;
import com.cy.helmet.MainActivity;
import com.cy.helmet.R;
import com.cy.helmet.WorkThreadManager;
import com.cy.helmet.config.HelmetConfig;
import com.cy.helmet.config.VideoConfig;
import com.cy.helmet.conn.HelmetConnManager;
import com.cy.helmet.conn.HelmetMessageSender;
import com.cy.helmet.conn.SendMessage;
import com.cy.helmet.core.protocol.Common;
import com.cy.helmet.core.protocol.HelmetServer;
import com.cy.helmet.factory.PhotoMessageFactory;
import com.cy.helmet.factory.VideoMessageFactory;
import com.cy.helmet.util.LogUtil;
import com.cy.helmet.video.callback.VideoLiveCallback;
import com.cy.helmet.video.callback.VideoRecordCallback;
import com.cy.helmet.video.camera.CameraListener;
import com.cy.helmet.video.configuration.AudioConfiguration;
import com.cy.helmet.video.controller.PhotoCallback;
import com.cy.helmet.video.entity.Watermark;
import com.cy.helmet.video.entity.WatermarkPosition;
import com.cy.helmet.video.stream.packer.flv.FlvPacker;
import com.cy.helmet.video.stream.packer.rtmp.RtmpPacker;
import com.cy.helmet.video.stream.sender.local.LocalSender;
import com.cy.helmet.video.stream.sender.rtmp.RtmpSender;
import com.cy.helmet.video.ui.RenderSurfaceView;
import com.cy.helmet.video.utils.SopCastLog;
import com.cy.helmet.video.utils.WeakHandler;
import com.cy.helmet.voice.HelmetVoiceManager;

import java.io.File;
import java.util.Observable;
import java.util.Observer;

/**
 * Created by yaojiaqing on 2018/2/8.
 */

public class HelmetVideoManager implements Observer {

    private WeakHandler mHandler = new WeakHandler(Looper.getMainLooper());

    private boolean mIsRecording;
    private boolean mIsLiving;

    // float camera preview
    private WindowManager.LayoutParams params;
    private WindowManager mWindowManager;
    private View mFloatView;

    private CameraPreview mCameraPreview;
    private Context mContext;

    private Runnable mCurTask = null;

    private CameraListener mCameraListener = new CameraListener() {
        @Override
        public void onOpenSuccess() {
            Log.e("YJQ", "camera open success: " + System.currentTimeMillis());
            onCameraOpenSuccess();
        }

        @Override
        public void onOpenFail(int error) {
            onCameraOpenFailed();
        }
    };

    private static HelmetVideoManager mInstance;

    public static synchronized HelmetVideoManager getInstance() {
        if (mInstance == null) {
            mInstance = new HelmetVideoManager();
        }
        return mInstance;
    }

    private HelmetVideoManager() {
        HelmetConfig.get().addObserver(this);
        mContext = HelmetApplication.mAppContext;
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
    }

    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof HelmetConfig) {
            if (arg instanceof VideoConfig) {
                if (mCameraPreview != null) {
                    VideoConfig config = (VideoConfig) arg;
                    int recordBps = config.videoCfgDetail.codeRate;
                    mCameraPreview.setRecordBps(recordBps);
                    int liveBps = config.liveVideoCfgDetail.codeRate;
                    mCameraPreview.setLiveBps(liveBps);

                    Log.e("YJQ", "newRecordRate: " + recordBps);
                    Log.e("YJQ", "newLiveRate: " + liveBps);
                }
            }
        }
    }

    private void processAccumulatedTask() {
        if (mCurTask != null) {
            mHandler.post(mCurTask);
            mCurTask = null;
        }
    }

    public CameraPreview initCameraPreView() {

        if (mCameraPreview != null) {
            return mCameraPreview;
        }

        Log.e("YJQ", "start camera preview....................");

        try {
            params = new WindowManager.LayoutParams();
            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
            params.format = PixelFormat.RGBA_8888;
            params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;

            params.width = 1;
            params.height = 1;

            LayoutInflater inflater = LayoutInflater.from(mContext);
            mFloatView = inflater.inflate(R.layout.layout_camera_preview, null);
            mWindowManager.addView(mFloatView, params);

            Log.e("YJQ", "start camera: " + System.currentTimeMillis());

            RenderSurfaceView surfaceView = (RenderSurfaceView) mFloatView.findViewById(R.id.render_surface_view);
            mCameraPreview = new CameraPreview(surfaceView, mCameraListener);
            SopCastLog.isOpen(true);

            //set watermark
            Watermark watermark = new Watermark(50, 25, WatermarkPosition.WATERMARK_ORIENTATION_BOTTOM_RIGHT, 8, 8);
            mCameraPreview.setWatermark(watermark);
        } catch (Exception e) {
            mCameraPreview = null;
        }

        return mCameraPreview;
    }

    public void takePhoto(final boolean isManual, final boolean playVoice, final boolean isCompress) {
        LogUtil.e("takePhoto: isManual(" + isManual + "), playVoice(" + playVoice + "), isCompress(" + isCompress + ")");
        if (mCameraPreview == null) {
            LogUtil.e("mCameraPreview is null...");
            if (mCurTask == null) {
                LogUtil.e("put taking photo as task...");
                mCurTask = new Runnable() {
                    @Override
                    public void run() {
                        takePhotoImpl(isManual, playVoice, isCompress);
                    }
                };
            } else {
                LogUtil.e("has current task, ignore photo...");
                if (!isManual) {
                    //only server order need response
                    HelmetMessageSender.sendTakePhotoResp(null);
                }
            }

            initCameraPreView();
        } else {
            takePhotoImpl(isManual, playVoice, isCompress);
        }
    }

    private void takePhotoImpl(final boolean isManual, final boolean playVoice, boolean isCompress) {
        if (mCameraPreview != null) {
            LogUtil.e("takePhoto: \n manual: " + isManual + "\n playVoice: " + playVoice + "\n comPress: " + isCompress);
            Log.e("YJQ", "start take photo: " + System.currentTimeMillis());

            boolean requestResult = mCameraPreview.takePhoto(playVoice, isCompress, new PhotoCallback() {
                @Override
                public void updatePhotoStatus(final boolean status, final File file) {
                    onTakePhotoFinish(isManual, playVoice, status, file);
                }
            });

            if (!requestResult) {
                LogUtil.e("taking photo now, ignore request...");
                //is taking photo now, take photo failed
                //only server order need response
                if (!isManual) {
                    HelmetMessageSender.sendTakePhotoResp(null);
                }
            }
        } else {
            if (!isManual) {
                LogUtil.e("taking photo failed, preview not initialized");
                HelmetMessageSender.sendTakePhotoResp(null);
            }
        }
    }

    public void startRecording(final boolean isManual, final boolean playVoice) {
        if (mCameraPreview == null) {
            if (mCurTask == null) {
                mCurTask = new Runnable() {
                    @Override
                    public void run() {
                        startRecordingImpl(isManual, playVoice);
                    }
                };
            } else {
                HelmetMessageSender.sendStartRecordResp(isManual, false);
            }
            initCameraPreView();
        } else {
            startRecordingImpl(isManual, playVoice);
        }
    }

    public void startRecordingImpl(final boolean isManual, final boolean playVoice) {
        if (mIsRecording) {
            HelmetMessageSender.sendStartRecordResp(isManual, true);
            Log.e("YJQ", "is recording now....................");
            return;
        }

        if (mCameraPreview == null) {
            HelmetMessageSender.sendStartRecordResp(isManual, false);
            return;
        }

        //初始化flv打包器
        int[] resolution = HelmetConfig.get().getRecordResolution();
        FlvPacker flvPacker = new FlvPacker();
        flvPacker.initAudioParams(AudioConfiguration.DEFAULT_FREQUENCY, 16, false);
        flvPacker.initVideoParams(resolution[0], resolution[1], 25);
        LocalSender flvSender = new LocalSender();

        mCameraPreview.setFlvPackerSender(flvPacker, flvSender);
        mCameraPreview.startRecording(new VideoRecordCallback() {
            @Override
            public void onRecordingStart(final boolean success) {
                onRecordStart(isManual, playVoice, success);
            }

            @Override
            public void onRecordingFinish() {
                onRecordFinish(playVoice);
            }
        });
    }

    public void stopRecording(boolean isManual) {
        mIsRecording = false;
        if (mCameraPreview != null) {
            mCameraPreview.stopRecording();
        }
        HelmetMessageSender.sendStopRecordResp(isManual, true);
    }

    public void startLiving(final String livingUrl, final int frameSize) {
        if (mCameraPreview == null) {
            if (mCurTask == null) {
                mCurTask = new Runnable() {
                    @Override
                    public void run() {
                        startLivingImpl(livingUrl, frameSize);
                    }
                };
            } else {
                HelmetMessageSender.sendStartLiveResp(false);
            }
            initCameraPreView();
        } else {
            startLivingImpl(livingUrl, frameSize);
        }
    }

    public void startLivingImpl(String livingUrl, int frameSize) {

        if (livingUrl == null || TextUtils.isEmpty(livingUrl.trim())) {
            HelmetMessageSender.sendStartLiveResp(false);
            return;
        }

        if (mIsLiving) {
            Log.e("YJQ", "is living now....................");
            HelmetMessageSender.sendStartLiveResp(true);
            return;
        }

        if (mCameraPreview == null) {
            HelmetMessageSender.sendStartLiveResp(false);
            return;
        }

        //初始化rtmp打包器
        int[] resolution = HelmetConfig.get().getLiveResolution();
        RtmpPacker rtmpPacker = new RtmpPacker();
        rtmpPacker.initAudioParams(AudioConfiguration.DEFAULT_FREQUENCY, 16, false);
        RtmpSender rtmpSender = new RtmpSender();
        rtmpSender.setVideoParams(resolution[0], resolution[1]);
        rtmpSender.setAudioParams(AudioConfiguration.DEFAULT_FREQUENCY, 16, false);

        mCameraPreview.setRtmpPackerSender(rtmpPacker, rtmpSender);
        mIsLiving = true;
        mCameraPreview.startLiving(livingUrl, frameSize, new VideoLiveCallback() {
            @Override
            public void onLivingStart(final boolean success) {
                onLiveStart(success);
            }

            @Override
            public void onLivingFinish() {
                onLiveFinish();
            }
        });

        Toast.makeText(HelmetApplication.mAppContext, "开始视频直播...", Toast.LENGTH_SHORT).show();
    }

    public void stopLiving() {
        mIsLiving = false;
        if (mCameraPreview != null) {
            mCameraPreview.stopLiving();
        }
    }

    public void onDestroy() {
        if (mCameraPreview != null) {
            mCameraPreview.stopRecording();
            mCameraPreview.stopLiving();
            mCameraPreview.release();
            mCameraPreview = null;
            mWindowManager.removeViewImmediate(mFloatView);
            mIsLiving = false;
        }
    }

    private void setReleasePreviewTask() {
        mHandler.removeCallbacks(releasePreviewRunnable);
        mHandler.postDelayed(releasePreviewRunnable, 10000);
    }

    private Runnable releasePreviewRunnable = new Runnable() {
        @Override
        public void run() {
            boolean isTakingPhoto = (mCameraPreview != null) && mCameraPreview.isTakingPhoto();
            if (!isTakingPhoto && !mIsLiving && !mIsRecording) {
                LogUtil.e("release camera preview.................");
                onDestroy();
            }
        }
    };

    private void onTakePhotoFinish(final boolean isManual, final boolean playVoice, final boolean status, final File file) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                setReleasePreviewTask();
                if (status && file != null) {
                    LogUtil.e("take photo: statue(" + status + "), path(" + file.getAbsolutePath() + ")");
                }
                if (!isManual) {
                    final File finalPhotoFile = (status ? file : null);
                    HelmetMessageSender.sendTakePhotoResp(finalPhotoFile);
                }
            }
        });
    }

    private void onRecordFinish(final boolean playVoice) {
        Log.e("YJQ", "onRecordFinish.................");
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mIsRecording = false;
                setReleasePreviewTask();
                Intent intent = new Intent(MainActivity.BROADCAST_VIDEO);
                intent.putExtra("status", HelmetApplication.mAppContext.getResources().getString(R.string.video_stop));
                HelmetApplication.mAppContext.sendBroadcast(intent);
                if (playVoice) {
                    HelmetVoiceManager.getInstance().playSoundPool(HelmetVoiceManager.STOP_RECORD_VIDEO);
                }
            }
        });
    }

    private void onRecordStart(final boolean isManual, final boolean playVoice, final boolean success) {
        Log.e("YJQ", "onRecordStart................." + success);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (success) {
                    mIsRecording = true;
                    mHandler.removeCallbacks(releasePreviewRunnable);
                    Intent intent = new Intent(MainActivity.BROADCAST_VIDEO);
                    intent.putExtra("status", HelmetApplication.mAppContext.getResources().getString(R.string.video_connected));
                    HelmetApplication.mAppContext.sendBroadcast(intent);
                    if (playVoice) {
                        HelmetVoiceManager.getInstance().playSoundPool(HelmetVoiceManager.START_RECORD_VIDEO);
                    }
                } else {
                    mIsRecording = false;
                    setReleasePreviewTask();
                    Intent intent = new Intent(MainActivity.BROADCAST_VIDEO);
                    intent.putExtra("status", HelmetApplication.mAppContext.getResources().getString(R.string.video_fail));
                    HelmetApplication.mAppContext.sendBroadcast(intent);
                    if (playVoice) {
                        HelmetVoiceManager.getInstance().playSoundPool(HelmetVoiceManager.STOP_RECORD_VIDEO);
                    }
                }

                HelmetMessageSender.sendStartRecordResp(isManual, true);
            }
        });
    }

    private void onLiveFinish() {
        Log.e("YJQ", "onLiveFinish.................");
        Log.e("Jerry_zhangcy", ">>>>>>>>>>>>>>>onLiveFinish<<<<<<<<<<<<<<<");
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mIsLiving = false;
                setReleasePreviewTask();
                Intent intent = new Intent(MainActivity.BROADCAST_LIVE);
                intent.putExtra("status", HelmetApplication.mAppContext.getResources().getString(R.string.video_live_stop));
                HelmetApplication.mAppContext.sendBroadcast(intent);
                HelmetVoiceManager.getInstance().playLocalVoice(HelmetVoiceManager.STOP_LIVING);
                HelmetMessageSender.sendStopLiveResp(true);
            }
        });
    }

    private void onLiveStart(final boolean success) {
        Log.e("YJQ", "onLiveStart.................");
        Log.e("Jerry_zhangcy", ">>>>>>>>>>>>>>>onLiveStart<<<<<<<<<<<<<<< success = " + success);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (success) {
                    mIsLiving = true;
                    mHandler.removeCallbacks(releasePreviewRunnable);
                    Intent intent = new Intent(MainActivity.BROADCAST_LIVE);
                    intent.putExtra("status", HelmetApplication.mAppContext.getResources().getString(R.string.video_live));
                    HelmetApplication.mAppContext.sendBroadcast(intent);
                    HelmetVoiceManager.getInstance().playLocalVoice(HelmetVoiceManager.START_LIVING);
                } else {
                    mIsLiving = false;
                    setReleasePreviewTask();
                    Intent intent = new Intent(MainActivity.BROADCAST_LIVE);
                    intent.putExtra("status", HelmetApplication.mAppContext.getResources().getString(R.string.video_live_fail));
                    HelmetApplication.mAppContext.sendBroadcast(intent);
                    HelmetVoiceManager.getInstance().playLocalVoice(HelmetVoiceManager.STOP_LIVING);
                }

                HelmetMessageSender.sendStartLiveResp(success);
            }
        });
    }

    private void onCameraOpenSuccess() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                processAccumulatedTask();
                setReleasePreviewTask();
            }
        });
    }

    private void onCameraOpenFailed() {
        mHandler.post(releasePreviewRunnable);
    }

    public void prepareSleep() {
        onDestroy();
    }

    public boolean isRecording() {
        return mIsRecording;
    }

    public boolean isLiving() {
        return mIsLiving;
    }

    public String getRecordingFileName() {
        if (mCameraPreview != null) {
            return mCameraPreview.getRecordingFileName();
        }
        return "";
    }
}
