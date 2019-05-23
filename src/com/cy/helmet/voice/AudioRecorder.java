package com.cy.helmet.voice;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.cy.helmet.video.audio.AudioProcessor;
import com.czt.mp3recorder.PCMFormat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

/**
 * Created by jiaqing on 2018/3/25.
 */
public class AudioRecorder extends Observable {

    private static final int DEFAULT_AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private static final int DEFAULT_SAMPLING_RATE = 8000;
    private static final int DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final PCMFormat DEFAULT_AUDIO_FORMAT = PCMFormat.PCM_16BIT;
    private static final int FRAME_COUNT = 160;

    private AudioRecord mAudioRecord = null;
    private static int mBufferSize = -1;
    private short[] mRecordBuffer;

    private volatile boolean mIsRecording = false;

    private static AudioRecorder mInstance;

    private Set<Observer> mRecorderSet = new HashSet<Observer>();

    private Thread mRecordThread = null;

    public synchronized static AudioRecorder getInstance() {
        if (mInstance == null) {
            mInstance = new AudioRecorder();
        }
        return mInstance;
    }

    private AudioRecorder() {
        mBufferSize = getRecordBufferSize();
        mRecordBuffer = new short[mBufferSize];
    }

    public synchronized void startRecord(Mp3Processor recorder) {
        if (mAudioRecord == null) {
            mAudioRecord = new AudioRecord(DEFAULT_AUDIO_SOURCE,
                    DEFAULT_SAMPLING_RATE, DEFAULT_CHANNEL_CONFIG,
                    DEFAULT_AUDIO_FORMAT.getAudioFormat(),
                    mBufferSize);
        }
        addObserver(recorder);
        if (mRecorderSet.add(recorder)) {
            mAudioRecord.setRecordPositionUpdateListener(recorder, recorder.getHandler());
            mAudioRecord.setPositionNotificationPeriod(FRAME_COUNT);
        }

        if (!mIsRecording) {
            mIsRecording = true;
            startRecordingThread();
        }
    }

    public synchronized void stopRecord(Mp3Processor recorder) {
        deleteObserver(recorder);

        if (mRecorderSet.remove(recorder)) {
            mAudioRecord.setRecordPositionUpdateListener(null, null);
        }

        if (mRecorderSet.isEmpty()) {
            mIsRecording = false;
        }
    }

    public synchronized void startRecord(AudioProcessor processor) {
        if (mAudioRecord == null) {
            mAudioRecord = new AudioRecord(DEFAULT_AUDIO_SOURCE,
                    DEFAULT_SAMPLING_RATE, DEFAULT_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT.getAudioFormat(),
                    mBufferSize);
        }

        addObserver(processor);
        mRecorderSet.add(processor);
        if (!mIsRecording) {
            mIsRecording = true;
            startRecordingThread();
        }
    }

    public synchronized void stopRecord(AudioProcessor processor) {
        deleteObserver(processor);
        mRecorderSet.remove(processor);
        if (mRecorderSet.isEmpty()) {
            mIsRecording = false;
        }
    }

    public static int getRecordBufferSize() {
        if (mBufferSize == -1) {
            mBufferSize = AudioRecord.getMinBufferSize(DEFAULT_SAMPLING_RATE,
                    DEFAULT_CHANNEL_CONFIG,
                    DEFAULT_AUDIO_FORMAT.getAudioFormat());

            /* Get number of samples. Calculate the buffer size
             * (round up to the factor of given frame size)
		     * 使能被整除，方便下面的周期性通知
		     * */
            int bytesPerFrame = DEFAULT_AUDIO_FORMAT.getBytesPerFrame();
            int frameSize = mBufferSize / bytesPerFrame;
            if (frameSize % FRAME_COUNT != 0) {
                frameSize += (FRAME_COUNT - frameSize % FRAME_COUNT);
                mBufferSize = frameSize * bytesPerFrame;
            }
        }

        return mBufferSize;
    }

    private void dispatchRecordData(short[] srcBytes, int len) {
        short[] data = Arrays.copyOf(srcBytes, len);
        setChanged();
        notifyObservers(data);
    }

    public void startRecordingThread() {
        if (mRecordThread == null) {
            mRecordThread = new Thread() {
                @Override
                public void run() {
                    mAudioRecord.startRecording();
                    while (mIsRecording) {
                        int readSize = mAudioRecord.read(mRecordBuffer, 0, mBufferSize);
                        if (readSize > 0) {
                            dispatchRecordData(mRecordBuffer, readSize);
                        }
                    }
                    // release and finalize audioRecord
                    releaseAudioRecorder();
                }
            };
            mRecordThread.start();
        }
    }

    private synchronized void releaseAudioRecorder() {
        mRecordThread = null;
        if (mAudioRecord != null) {
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
        }
    }
}
