package com.cy.helmet.video.stream.sender.rtmp;

import com.cy.helmet.util.LogUtil;
import com.cy.helmet.video.callback.VideoLiveCallback;
import com.cy.helmet.video.stream.packer.rtmp.RtmpPacker;
import com.cy.helmet.video.stream.sender.Sender;
import com.cy.helmet.video.stream.sender.rtmp.io.RtmpConnectListener;
import com.cy.helmet.video.stream.sender.rtmp.io.RtmpConnection;
import com.cy.helmet.video.stream.sender.sendqueue.ISendQueue;
import com.cy.helmet.video.stream.sender.sendqueue.NormalSendQueue;
import com.cy.helmet.video.stream.sender.sendqueue.SendQueueListener;
import com.cy.helmet.video.utils.WeakHandler;

public class RtmpSender implements Sender, SendQueueListener {
    private RtmpConnection rtmpConnection;
    private String mRtmpUrl;
    private OnSenderListener mListener;
    private WeakHandler mHandler = new WeakHandler();
    private ISendQueue mSendQueue = new NormalSendQueue();

    //add by Jerry for frame buffer size
    private int mFrameSize = 0;

    private VideoLiveCallback videoStatusCallback;

    @Override
    public void good() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onNetGood();
                }
            }
        });
    }

    @Override
    public void bad() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onNetBad();
                }
            }
        });
    }

    public interface OnSenderListener {
        void onConnecting();

        void onConnected();

        void onDisConnected();

        void onPublishFail();

        void onNetGood();

        void onNetBad();
    }

    public RtmpSender() {
        rtmpConnection = new RtmpConnection();
    }

    public void setAddress(String url) {
        mRtmpUrl = url;
    }

    //add by for video living frame buffer size
    public void setFrameBufferSize(int size) {
        mFrameSize = size;
        mSendQueue.init(size);
    }

    public void setSendQueue(ISendQueue sendQueue) {
        mSendQueue = sendQueue;
    }

    public void setVideoParams(int width, int height) {
        rtmpConnection.setVideoParams(width, height);
    }

    public void setAudioParams(int sampleRate, int sampleSize, boolean isStereo) {
        rtmpConnection.setAudioParams(sampleRate, sampleSize, isStereo);
    }

    public void setSenderListener(OnSenderListener listener) {
        mListener = listener;
    }

    public void setVideoStatusCallback(VideoLiveCallback callback) {
        videoStatusCallback = callback;
    }

//    public void connect() {
//        rtmpConnection.setSendQueue(mSendQueue);
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                connectNotInUi();
//            }
//        }).start();
//        if (mListener != null) {
//            mListener.onConnecting();
//        }
//    }

    public void connect() {
        rtmpConnection.setSendQueue(mSendQueue);
        if (mListener != null) {
            mListener.onConnecting();
        }
        connectNotInUi();
    }

    private synchronized void connectNotInUi() {
        rtmpConnection.setConnectListener(listener);
        rtmpConnection.connect(mRtmpUrl);
    }

    @Override
    public synchronized void start() {
        mSendQueue.setSendQueueListener(this);
        mSendQueue.start();
    }

    @Override
    public void onData(byte[] data, int type) {
        if (type == RtmpPacker.FIRST_AUDIO || type == RtmpPacker.AUDIO) {
            rtmpConnection.publishAudioData(data, type);
        } else if (type == RtmpPacker.FIRST_VIDEO ||
                type == RtmpPacker.INTER_FRAME || type == RtmpPacker.KEY_FRAME) {
            rtmpConnection.publishVideoData(data, type);
        }
    }

    @Override
    public synchronized void stop() {
        rtmpConnection.stop();
        rtmpConnection.setConnectListener(null);
        mSendQueue.setSendQueueListener(null);
        mSendQueue.stop();
    }

    @Override
    public boolean onTransferFile() {
        return false;
    }

    private RtmpConnectListener listener = new RtmpConnectListener() {
        @Override
        public void onUrlInvalid() {
            sendPublishResult(false);
        }

        @Override
        public void onSocketConnectSuccess() {

        }

        @Override
        public void onSocketConnectFail() {
            sendPublishResult(false);
        }

        @Override
        public void onHandshakeSuccess() {

        }

        @Override
        public void onHandshakeFail() {
            sendPublishResult(false);
        }

        @Override
        public void onRtmpConnectSuccess() {

        }

        @Override
        public void onRtmpConnectFail() {
            sendPublishResult(false);
        }

        @Override
        public void onCreateStreamSuccess() {

        }

        @Override
        public void onCreateStreamFail() {
            sendPublishResult(false);
        }

        @Override
        public void onPublishSuccess() {
            sendPublishResult(true);
        }

        @Override
        public void onPublishFail() {
            sendPublishResult(false);
        }

        @Override
        public void onSocketDisconnect() {
            sendDisconnectMsg();
        }

        @Override
        public void onStreamEnd() {
            sendDisconnectMsg();
        }
    };

    public void sendDisconnectMsg() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onDisConnected();
                }
                if (videoStatusCallback != null) {
                    LogUtil.e("live failed: server disconnect.");
                    videoStatusCallback.onLivingFinish();
                }
            }
        });
    }

    public void sendPublishResult(final boolean isSuccess) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    if (isSuccess) {
                        mListener.onConnected();
                    } else {
                        mListener.onPublishFail();
                    }
                }

                if (videoStatusCallback != null) {
                    videoStatusCallback.onLivingStart(isSuccess);
                }
            }
        });
    }
}
