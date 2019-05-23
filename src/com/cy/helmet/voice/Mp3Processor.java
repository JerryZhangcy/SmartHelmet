package com.cy.helmet.voice;

import android.media.AudioRecord;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.cy.helmet.util.LogUtil;
import com.czt.mp3recorder.OnRecordListener;
import com.czt.mp3recorder.util.LameUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;

/**
 * Created by jiaqing on 2018/3/26.
 */

public class Mp3Processor extends AudioEncodeBase implements AudioRecord.OnRecordPositionUpdateListener {

    private static final int DEFAULT_SAMPLING_RATE = 8000;
    private static final int DEFAULT_LAME_MP3_QUALITY = 7;
    private static final int DEFAULT_LAME_IN_CHANNEL = 1;
    private static final int DEFAULT_LAME_MP3_BIT_RATE = 32;

    private boolean mIsRecording = false;

    private RecordHandler mHandler;
    private static final int PROCESS_STOP = 1;
    private static final int PROCESS_START = 2;

    private byte[] mMp3Buffer;
    private FileOutputStream mFileOutputStream;

    class RecordRequest {
        File file;
        int second;
        OnRecordListener listener;

        public RecordRequest(File file, int second, OnRecordListener listener) {
            this.file = file;
            this.second = second;
            this.listener = listener;
        }
    }

    /**
     * Start recording. Create an encoding thread. Start record from this
     * thread.
     *
     * @throws IOException initAudioRecorder throws
     */
    public boolean startRecord(File file, int second, OnRecordListener listener) {

        if (file == null || second <= 0 || listener == null) {
            LogUtil.e("illegal record parameter.");
            return false;
        }

        RecordRequest request = new RecordRequest(file, second, listener);
        Message msg = new Message();
        msg.what = PROCESS_START;
        msg.obj = request;
        mHandler.sendMessage(msg);

        return true;
    }

    public boolean isRecording() {
        return mIsRecording;
    }

    public boolean breakRecordingIfNecessary() {
        return mHandler.breakRecordingIfNecessary();
    }

    @Override
    public void update(Observable o, Object arg) {
        short[] srcData = (short[]) arg;
        if (srcData != null) {
            int dataLen = srcData.length;
            LogUtil.e("receive data mp3 : " + dataLen);
            if (dataLen > 0) {
                addTask(srcData, dataLen);
            }
        }
    }

    private class RecordHandler extends Handler {

        private Mp3Processor encodeThread;
        private RecordRequest request;

        public RecordHandler(final Looper looper, final Mp3Processor encodeThread) {
            super(looper);
            this.encodeThread = encodeThread;
        }

        public boolean breakRecordingIfNecessary() {
            if (mIsRecording) {
                removeMessages(PROCESS_STOP);
                sendEmptyMessage(PROCESS_STOP);
                return true;
            }

            return false;
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == PROCESS_START) {

                if (mIsRecording) {
                    return;
                }

                request = (RecordRequest) msg.obj;
                if (request == null) {
                    return;
                }

                mIsRecording = true;
                try {
                    mFileOutputStream = new FileOutputStream(request.file);
                    LameUtil.init(DEFAULT_SAMPLING_RATE,
                            DEFAULT_LAME_IN_CHANNEL,
                            DEFAULT_SAMPLING_RATE,
                            DEFAULT_LAME_MP3_BIT_RATE,
                            DEFAULT_LAME_MP3_QUALITY);

                    AudioRecorder.getInstance().startRecord(encodeThread);

                    // set record timer
                    sendEmptyMessageDelayed(PROCESS_STOP, request.second * 1000);

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if (request.listener != null) {
                                request.listener.onStartRecord();
                            }
                        }
                    });

                } catch (Exception e) {
                    LogUtil.e("record mp3 exception: ");
                    LogUtil.e(e);

                    mIsRecording = false;
                    request = null;
                    if (mFileOutputStream != null) {
                        try {
                            mFileOutputStream.close();
                        } catch (IOException e1) {
                        }
                    }
                }
            } else if (msg.what == PROCESS_STOP) {
                LogUtil.e("stop mp3 record......");
                try {
                    removeCallbacksAndMessages(null);
                    while (encodeThread.processData() > 0) ;
                    encodeThread.flushAndRelease();
                    AudioRecorder.getInstance().stopRecord(RecordHandler.this.encodeThread);
                } catch (Exception e) {

                } finally {
                    if (request != null && request.listener != null) {
                        final OnRecordListener recordListener = request.listener;
                        final File recordFile = request.file;
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                recordListener.onFinishRecord(recordFile);
                            }
                        });
                    }
                    mIsRecording = false;
                    request = null;
                }
            }
        }
    }

    /**
     * Constructor
     *
     * @throws FileNotFoundException file not found
     */
    public Mp3Processor() {
        super("Mp3Processor");
        start();

        int buffSize = AudioRecorder.getRecordBufferSize();
        mMp3Buffer = new byte[(int) (7200 + (buffSize * 2 * 1.25))];
        mHandler = new RecordHandler(getLooper(), this);
    }

    private void check() {
        if (mHandler == null) {
            throw new IllegalStateException();
        }
    }

    public Handler getHandler() {
        check();
        return mHandler;
    }

    @Override
    public void onMarkerReached(AudioRecord recorder) {
        // Do nothing
    }

    @Override
    public void onPeriodicNotification(AudioRecord recorder) {
        processData();
    }

    private int processData() {
        if (mTasks.size() > 0) {
            Task task = mTasks.remove(0);
            short[] buffer = task.getData();
            int readSize = task.getReadSize();
            int encodedSize = LameUtil.encode(buffer, buffer, readSize, mMp3Buffer);
            if (encodedSize > 0) {
                try {
                    mFileOutputStream.write(mMp3Buffer, 0, encodedSize);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return readSize;
        }
        return 0;
    }

    /**
     * Flush all data left in lame buffer to file
     */
    private void flushAndRelease() {
        //将MP3结尾信息写入buffer中
        final int flushResult = LameUtil.flush(mMp3Buffer);
        if (flushResult > 0) {
            try {
                mFileOutputStream.write(mMp3Buffer, 0, flushResult);
            } catch (IOException e) {
            } finally {
                if (mFileOutputStream != null) {
                    try {
                        mFileOutputStream.close();
                    } catch (IOException e) {
                    }
                    mFileOutputStream = null;
                }
                LameUtil.close();
            }
        }
    }

    private List<Task> mTasks = Collections.synchronizedList(new ArrayList<Task>());

    public void addTask(short[] rawData, int readSize) {
        mTasks.add(new Task(rawData, readSize));
    }

    private class Task {
        private short[] rawData;
        private int readSize;

        public Task(short[] rawData, int readSize) {
            this.rawData = rawData.clone();
            this.readSize = readSize;
        }

        public short[] getData() {
            return rawData;
        }

        public int getReadSize() {
            return readSize;
        }
    }
}
