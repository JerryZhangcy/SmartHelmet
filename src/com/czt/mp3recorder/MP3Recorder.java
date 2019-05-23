//package com.czt.mp3recorder;
//
//import android.os.Handler;
//import android.os.Looper;
//
//import com.cy.helmet.voice.AudioRecorder;
//import com.czt.mp3recorder.util.LameUtil;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.Observable;
//import java.util.Observer;
//
//public class MP3Recorder implements Observer {
//
////    //=======================AudioRecord Default Settings=======================
////    private static final int DEFAULT_SAMPLING_RATE = 8000;
////
////    //======================Lame Default Settings=====================
////    private static final int DEFAULT_LAME_MP3_QUALITY = 7;
////
////    /**
////     * 与DEFAULT_CHANNEL_CONFIG相关，因为是mono单声，所以是1
////     */
////    private static final int DEFAULT_LAME_IN_CHANNEL = 1;
////
////    /**
////     * Encoded bit rate. MP3 file will be encoded with bit rate 32kbps
////     */
////    private static final int DEFAULT_LAME_MP3_BIT_RATE = 32;
//
////    private DataEncodeThread mEncodeThread;
////    private boolean mIsRecording = false;
//
////    /**
////     * Start recording. Create an encoding thread. Start record from this
////     * thread.
////     *
////     * @throws IOException initAudioRecorder throws
////     */
////    public void start(File file, int second) throws IOException {
////        if (mIsRecording) {
////            return;
////        }
////        mIsRecording = true; // 提早，防止init或startRecording被多次调用
////        initAudioRecorder(file);
////        AudioRecorder.getInstance().startRecord(this, mEncodeThread);
////
////        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
////            @Override
////            public void run() {
////                android.util.Log.e("YXL", "stop recording......");
////                stop();
////            }
////        }, second * 1000);
////    }
////
////    public void stop() {
////        mIsRecording = false;
////        AudioRecorder.getInstance().stopRecord(this);
////    }
//
//    public boolean isRecording() {
//        return mIsRecording;
//    }
//
//    /**
//     * Initialize audio recorder
//     */
//    private void initAudioRecorder(File file) throws IOException {
//        LameUtil.init(DEFAULT_SAMPLING_RATE, DEFAULT_LAME_IN_CHANNEL, DEFAULT_SAMPLING_RATE, DEFAULT_LAME_MP3_BIT_RATE, DEFAULT_LAME_MP3_QUALITY);
//        // Create and run thread used to encode data
//        // The thread will
//        int bufferSize = AudioRecorder.getRecordBufferSize();
//        mEncodeThread = new DataEncodeThread(file, bufferSize, new OnEncodeListener() {
//            @Override
//            public void onEncodeFinish(File file) {
//                if (mListener != null) {
//                    mListener.onFinishRecord(file);
//                }
//            }
//        });
//    }
//
//    private OnRecordListener mListener;
//    private int mRecordDuration;
//    private long mStartRecordTime;
//    private long mFinishRecordTime;
//
//    public void setRecordDuration(int second) {
//        if (second > 0) {
//            mRecordDuration = second;
//        }
//    }
//
//    public void setRecordListener(OnRecordListener listener) {
//        mListener = listener;
//    }
//
//    private void checkTimer() {
//        if (mRecordDuration > 0) {
//            if (mStartRecordTime == 0) {
//                mStartRecordTime = System.currentTimeMillis();
//                mFinishRecordTime = mStartRecordTime + mRecordDuration * 1000;
//            }
//
//            long curTimeStamp = System.currentTimeMillis();
//            if (curTimeStamp >= mFinishRecordTime || curTimeStamp < mStartRecordTime) {
//                stop();
//                mStartRecordTime = 0;
//            }
//        }
//    }
//
//    @Override
//    public void update(Observable o, Object arg) {
//        short[] srcData = (short[]) arg;
//        if (srcData != null) {
//            int dataLen = srcData.length;
//            if (dataLen > 0) {
//                mEncodeThread.addTask(srcData, dataLen);
//            }
//        }
//    }
//}