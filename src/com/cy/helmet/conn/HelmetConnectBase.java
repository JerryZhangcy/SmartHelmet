package com.cy.helmet.conn;

import android.content.Context;
import android.util.Log;

import com.cy.helmet.Constant;
import com.cy.helmet.HelmetApplication;
import com.cy.helmet.observer.ConnStatusChange;
import com.cy.helmet.util.LogUtil;
import com.cy.helmet.util.NetworkUtil;
import com.cy.helmet.util.Util;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class HelmetConnectBase {

    protected static int connectTimeout = 10;

    protected Context mContext;
    protected SocketChannel mSocketChannel;

    protected int mServerPort = -1;
    protected String mServerHost = null;

    protected LinkedBlockingQueue<SendMessage> mSendMessageQueue = new LinkedBlockingQueue<SendMessage>();

    protected byte[] bufferArray;
    protected ByteBuffer buffer;
    protected boolean needReset = true;

    protected boolean started = false;
    protected boolean stoped = false;
    private boolean isFirstStart = true;

    protected boolean isConnected = false;

    protected SendRunnable mSendRunnable;
    protected ReceiveRunnable mReceiveRunnable;
    protected Thread mSendThread;
    protected Thread mReceiveThread;

    protected Object mNetworkStateLock = new Object();

    public HelmetConnectBase(String serverAddr, int serverPort) {
        mContext = HelmetApplication.mAppContext;
        this.mServerHost = serverAddr;
        this.mServerPort = serverPort;
        this.connectTimeout = connectTimeout;
        try {
            start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(SendMessage msg) {
        if (msg != null && msg.isValid()) {
            try {
                mSendMessageQueue.add(msg);
            } catch (Exception e) {
                LogUtil.e("send message failed: ");
                LogUtil.e(e);
            }
        }
    }

    protected SendMessage takeMessage() {
        SendMessage message = null;
        try {
            message = mSendMessageQueue.take();
        } catch (Exception e) {

        }
        return message;
    }

    private synchronized void init() {
        bufferArray = new byte[Constant.MAX_RECV_MSG_LEN];
        buffer = ByteBuffer.wrap(bufferArray);
        buffer.limit(Constant.MSG_PREFIX_LEN);
    }

    public void resetConnection(String serverAddr, int serverPort) {

    }

    protected synchronized void reset() throws Exception {
        if (needReset == false) {
            return;
        }

        if (mSocketChannel != null) {
            try {
                LogUtil.e("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx1");
                mSocketChannel.socket().close();
            } catch (Exception e) {
            }
            try {
                LogUtil.e("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx2");
                mSocketChannel.close();
            } catch (Exception e) {
            }
        }

        if (hasNetworkConnection()) {
            mSocketChannel = SocketChannel.open();
            mSocketChannel.configureBlocking(true);

            LogUtil.e("connect server>>>>" + mServerHost + ":" + mServerPort);

            mSocketChannel.socket().connect(new InetSocketAddress(mServerHost, mServerPort), 1000 * connectTimeout);
            mSocketChannel.socket().setSoTimeout(1000 * 5);
            needReset = false;
        } else {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
        }
    }

    public synchronized void start() throws Exception {

        if (this.started == true) {
            return;
        }

        this.init();

        mReceiveRunnable = new ReceiveRunnable();
        mReceiveThread = new Thread(mReceiveRunnable, "RECEIVE_THREAD");
        mReceiveThread.setDaemon(true);
        synchronized (mReceiveThread) {
            mReceiveThread.start();
            mReceiveThread.wait();
        }

        mSendRunnable = new SendRunnable();
        mSendThread = new Thread(mSendRunnable, "SEND_THREAD");
        mSendThread.setDaemon(true);
        synchronized (mSendThread) {
            mSendThread.start();
            mSendThread.wait();
        }

        this.started = true;
    }

    public synchronized void stop() {
        stoped = true;

        //notify connection closed
        ConnStatusChange.getInstance().onConnectStatus(false);

        synchronized (mNetworkStateLock) {
            mNetworkStateLock.notify();
        }

        if (mSocketChannel != null) {
            try {
                LogUtil.e("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx3");
                mSocketChannel.socket().close();
            } catch (Exception e) {
            }

            try {
                LogUtil.e("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx4");
                mSocketChannel.close();
            } catch (Exception e) {
            }
        }
        mSocketChannel = null;

        if (mReceiveThread != null) {
            try {
                mReceiveThread.interrupt();
            } catch (Exception e) {
            }
        }

        if (mSendThread != null) {
            try {
                mSendThread.interrupt();
            } catch (Exception e) {
            }
        }
    }

    private void receiveData() throws Exception {
        while (!stoped && hasRemainPacket()) {
            int read = mSocketChannel.read(buffer);
            if (read == 0) {
                continue;
            } else if (read < 0) {
                throw new Exception("end of stream");
            }

            if (buffer.limit() == Constant.MSG_PREFIX_LEN) {
                byte[] dateLenArray = new byte[4];
                System.arraycopy(bufferArray, 1, dateLenArray, 0, 4);
                int dataLen = Util.byteArrayToInt(dateLenArray);

                if (bufferArray[0] != 0x50) {
                    continue;
                }

                if (dataLen == 0) {
                    break;
                } else {
                    buffer.limit(Constant.MSG_PREFIX_LEN + dataLen);
                }
            }

            if (!hasRemainPacket()) {
                break;
            }

            if (!hasNetworkConnection()) {
                try {
//                    trySystemSleep();
                    Thread.sleep(1000);
                } catch (Exception e) {
                }
            }
        }

        byte[] data = new byte[buffer.position()];
        System.arraycopy(bufferArray, 0, data, 0, buffer.position());

        //reset buffer (must after arraycopy)
        buffer.clear();
        buffer.limit(Constant.MSG_PREFIX_LEN);

        RecvMessage msg = new RecvMessage(data);
        if (!msg.isValid()) {
            return;
        }

        onReceiveMessage(msg);

        mSendRunnable.wakeup();
    }

    private boolean hasRemainPacket() {
        return buffer.hasRemaining();
    }

    private void send(SendMessage msg) throws Exception {
        if (!NetworkUtil.hasNetwork()) {
            LogUtil.e("drop sending message, network is not available");
            return;
        }

        if (msg == null || !msg.isValid()) {
            LogUtil.e("drop sending message, invalid message");
            return;
        }

        if (mSocketChannel == null || mSocketChannel.isOpen() == false || mSocketChannel.isConnected() == false) {
            return;
        }

        LogUtil.e("send message................" + msg.mMsg.getMsgid());
        ByteBuffer bb = ByteBuffer.wrap(msg.toBytes());
        while (bb.hasRemaining()) {
            mSocketChannel.write(bb);
        }

        mSocketChannel.socket().getOutputStream().flush();
    }

    public abstract boolean hasNetworkConnection();

    public abstract void onReceiveMessage(RecvMessage message);

    class ReceiveRunnable implements Runnable {

        @Override
        public void run() {

            synchronized (mReceiveThread) {
                mReceiveThread.notifyAll();
            }

            while (!stoped) {
                try {
                    //block thread if network is not available
                    if (!hasNetworkConnection()) {
                        needReset = true;
                        synchronized (mNetworkStateLock) {
                            mNetworkStateLock.wait();
                        }
                    }
                    reset();
                    receiveData();
                } catch (java.net.SocketTimeoutException e) {
                    LogUtil.e(e);
                } catch (java.nio.channels.ClosedChannelException e) {
                    needReset = true;
                    LogUtil.e(e);
                } catch (Exception e) {
                    LogUtil.e(e);
                    needReset = true;
                } catch (Throwable t) {
                    LogUtil.e(new Exception(t));
                    needReset = true;
                } finally {
                    isConnected = !needReset;
                    if (needReset == true) {
                        try {
//                            trySystemSleep();
                            Thread.sleep(3000);
                        } catch (Exception e) {
                        }
                    }
                }
            }

            if (mSocketChannel != null) {
                try {
                    LogUtil.e("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx5");
                    mSocketChannel.socket().close();
                } catch (Exception e) {
                }

                try {
                    LogUtil.e("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx6");
                    mSocketChannel.close();
                } catch (Exception e) {
                }

                mSocketChannel = null;
            }
        }
    }

    class SendRunnable implements Runnable {
        public void run() {
            synchronized (mSendThread) {
                mSendThread.notifyAll();
            }
            while (stoped == false) {
                if (isFirstStart) {
                    isFirstStart = false;
                    waitMsg();
                }

                try {
                    SendMessage message = takeMessage();
                    if (message != null) {
                        send(message);
                    }
                } catch (Exception e) {
                    LogUtil.e(e);
                }
            }
        }

        private void waitMsg() {
            synchronized (this) {
                try {
                    this.wait(1000);
                } catch (InterruptedException e) {

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private void wakeup() {
            synchronized (this) {
                this.notifyAll();
            }
        }
    }

    public String getServerHost() {
        return mServerHost;
    }

    public int getServerPort() {
        return mServerPort;
    }

    public boolean isConnectKeepLive() {
        return !stoped && isConnected;
    }

    public void onNetworkStateChange(boolean available) {
        if (available) {
            synchronized (mNetworkStateLock) {
                mNetworkStateLock.notifyAll();
            }
        }
    }
}

	
	

