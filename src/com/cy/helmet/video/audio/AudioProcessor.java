package com.cy.helmet.video.audio;

import com.cy.helmet.video.configuration.AudioConfiguration;

import java.util.Arrays;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class AudioProcessor extends Thread implements Observer {
    private volatile boolean mStopFlag;
    private AudioEncoder mAudioEncoder;

    class RecordData {
        short[] shortDataArray;

        RecordData(short[] data) {
            shortDataArray = data;
        }
    }

    private BlockingQueue<RecordData> mDataQueue = new LinkedBlockingDeque<>();

    public AudioProcessor(AudioConfiguration audioConfiguration) {
        mAudioEncoder = new AudioEncoder(audioConfiguration);
        mAudioEncoder.prepareEncoder();
    }

    public void setAudioHEncodeListener(OnAudioEncodeListener listener) {
        mAudioEncoder.setOnAudioEncodeListener(listener);
    }

    public void stopEncode() {
        mStopFlag = true;
        if (mAudioEncoder != null) {
            mAudioEncoder.stop();
            mAudioEncoder = null;
        }
    }

    public void run() {
        while (!mStopFlag) {
            try {
                RecordData data = mDataQueue.take();
                if (data != null) {
                    byte[] bytes = toByteArray(data.shortDataArray);
                    if (mAudioEncoder != null) {
                        mAudioEncoder.offerEncoder(bytes);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public byte[] toByteArray(short[] src) {
        int count = src.length;
        byte[] dest = new byte[count << 1];
        for (int i = 0; i < count; i++) {
            dest[i * 2] = (byte) (src[i]);
            dest[i * 2 + 1] = (byte) (src[i] >> 8);
        }

        return dest;
    }

    @Override
    public void update(Observable o, Object arg) {
        short[] srcData = (short[]) arg;
        int dataLen = srcData.length;
        if (dataLen > 0) {
            short[] copyData = Arrays.copyOf(srcData, dataLen);
            mDataQueue.add(new RecordData(copyData));
        }
    }
}
