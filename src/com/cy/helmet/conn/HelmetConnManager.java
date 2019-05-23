package com.cy.helmet.conn;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.cy.helmet.HelmetApplication;
import com.cy.helmet.config.HelmetConfig;
import com.cy.helmet.config.ServerConfig;
import com.cy.helmet.core.protocol.Common;
import com.cy.helmet.core.protocol.MessageId;
import com.cy.helmet.core.protocol.ServerHelmet;
import com.cy.helmet.factory.HeartbeatFactory;
import com.cy.helmet.factory.RegisterFactory;
import com.cy.helmet.observer.ConnStatusChange;
import com.cy.helmet.storage.FileUtil;
import com.cy.helmet.util.LogUtil;
import com.cy.helmet.util.NetworkUtil;
import com.cy.helmet.util.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Observable;
import java.util.Observer;

/**
 * Created by yaojiaqing on 2018/2/3.
 */

public class HelmetConnManager implements Observer {

    public static final int HELMET_CONN_STAGE_ACTIVE = 1;
    public static final int HELMET_CONN_STAGE_REGISTER = 2;
    public static final int HELMET_CONN_STAGE_HEARTBEAT = 3;
    public static final int HELMET_CONN_STAGE_CLOSED = 4;

    private HelmetConnectBase mConnection;
    private int mCurState = HELMET_CONN_STAGE_CLOSED;

    private int mNonResponseHB = 0;

    private HelmetConfig mConfig;

    private HelmetMessageDispatcher mDispatcher = new HelmetMessageDispatcher();

    private HandlerThread mHandlerThread;
    private ConnectHandler mHandler;

    private ConnStatusChange mConnStatusObserver;

    public static final int MSG_FORCE_RESTART = 1000;
    public static final int MSG_PREPARE_SLEEP = 1001;
    public static final int MSG_TRIGGER_CONNECT = 1002;
    public static final int MSG_RESET_CONNECT = 1003;
    public static final int MSG_SEND_REQUEST_MESSAGE = 1004;
    public static final int MSG_REGISTER_SUCCESS = 1005;
    public static final int MSG_RECEIVE_HEARTBEAT_RESP = 1006;
    public static final int MSG_FORCE_REGISTER = 1007;
    public static final int MSG_NETWORK_DISCONNECT = 1008;


    private static HelmetConnManager mInstance;

    public synchronized static HelmetConnManager getInstance() {
        if (mInstance == null) {
            mInstance = new HelmetConnManager();
        }
        return mInstance;
    }

    public HelmetConnManager() {
        mConnStatusObserver = ConnStatusChange.getInstance();
        mConfig = HelmetConfig.get();
        mConfig.addObserver(this);
        mCurState = HELMET_CONN_STAGE_CLOSED;

        mHandlerThread = new HandlerThread("HELMET_CONNECT_THREAD");
        mHandlerThread.start();
        mHandler = new ConnectHandler(mHandlerThread.getLooper());
    }

    private void resetHelmetConnect(ServerConfig config) {
        Message msg = mHandler.obtainMessage(MSG_RESET_CONNECT);
        msg.obj = config;
        mHandler.sendMessage(msg);
    }

    public void sendMessage(SendMessage sendMessage) {
        Message msg = mHandler.obtainMessage(MSG_SEND_REQUEST_MESSAGE);
        msg.obj = sendMessage;
        mHandler.sendMessage(msg);
    }

    public void startConnect(String serverAddress, int serverPort) {
        mConnection = new HelmetConnectBase(serverAddress, serverPort) {
            @Override
            public boolean hasNetworkConnection() {
                return NetworkUtil.hasNetwork();
            }

            @Override
            public void onReceiveMessage(RecvMessage message) {
                if (message.isValid()) {

                    if (mConnStatusObserver != null) {
                        mConnStatusObserver.onConnectStatus(true);
                    }

                    ServerHelmet.S2HMessage s2HMessage = message.mS2HMessage;
                    MessageId.MsgId msgId = s2HMessage.getMsgid();
                    LogUtil.e("recv message................" + msgId);
                    if (msgId == MessageId.MsgId.S2H_MessageId_Register_Resp) {
                        if (!s2HMessage.hasRegisterResp()) {
                            LogUtil.e("invalid register response...");
                            return;
                        }

                        ServerHelmet.S2HRegisterResp registerResp = s2HMessage.getRegisterResp();
                        if (registerResp.getStatus() != 1) {
                            LogUtil.e("invalid register response status: " + registerResp.getStatus());
                            return;
                        }

                        LogUtil.e("registerResult: " + registerResp.toString());

                        if (registerResp.hasDeviceCfg()) {
                            Common.DeviceCfg cfg = registerResp.getDeviceCfg();
                            Message msg = mHandler.obtainMessage(MSG_REGISTER_SUCCESS);
                            msg.obj = cfg;
                            mHandler.sendMessage(msg);
                        }
                    } else if (msgId == MessageId.MsgId.S2H_MessageId_Heart_Beat_Resp) {
                        ServerHelmet.S2HHeartBeatResp heartBeatResp = s2HMessage.getHeartBeatResp();
                        Message msg = mHandler.obtainMessage(MSG_RECEIVE_HEARTBEAT_RESP);
                        if (heartBeatResp.hasTime()) {
                            msg.obj = heartBeatResp.getTime();
                        }
                        mHandler.sendMessage(msg);
                        mDispatcher.dispatchMessage(heartBeatResp);//add by Jerry
                    } else if (msgId == MessageId.MsgId.S2H_MessageId_Unknow_Device) {
                        //force transfer register state
                        forceRegister();
                    } else {
                        mDispatcher.dispatchMessage(message.mS2HMessage);
                    }
                }
            }
        };
    }

    private boolean updateActiveResult(String activeResult) {
        JSONObject activeJSON = null;

        try {
            activeJSON = new JSONObject(activeResult);
        } catch (JSONException e) {
            return false;
        }

        String result = activeJSON.optString("result", "");
        if (!"0".equals(result)) {
            return false;
        }

        JSONObject cfgJSON = activeJSON.optJSONObject("cfg");
        boolean updateResult = HelmetConfig.get().updateActiveInfo(cfgJSON);
        if (!updateResult) {
            return false;
        }

        return true;
    }

    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof ServerConfig) {
            ServerConfig serverConfig = (ServerConfig) arg;
            resetHelmetConnect(serverConfig);
        }
    }

    class ConnectHandler extends Handler {

        public ConnectHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_FORCE_RESTART:
                    mHandler.removeCallbacksAndMessages(null);
                    if (mConnection != null) {
                        mConnection.stop();
                        mConnection = null;
                    }

                    mCurState = mConfig.isActivated() ? HELMET_CONN_STAGE_REGISTER : HELMET_CONN_STAGE_ACTIVE;
                    triggerHelmetConnect();
                    break;

                case MSG_PREPARE_SLEEP:
                    if (mCurState == HELMET_CONN_STAGE_HEARTBEAT) {
                        // send heartbeat immediately if in heartbeat state
                        SendMessage message = new SendMessage(HeartbeatFactory.newInstance());
                        LogUtil.d("send heartbeat: " + message.mMsg.toString());
                        mConnection.sendMessage(message);
                        mNonResponseHB++;
                    }

                    mCurState = HELMET_CONN_STAGE_CLOSED;
                    if (mConnection != null) {
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                //disconnect
                                if (mCurState == HELMET_CONN_STAGE_CLOSED) {
                                    if (mConnection != null) {
                                        mConnection.stop();
                                        mConnection = null;
//                                        mCurState = HELMET_CONN_STAGE_REGISTER;
                                    }
                                }
                            }
                        }, 1000);
                    }

                    break;

                case MSG_RESET_CONNECT:
                    ServerConfig config = (ServerConfig) msg.obj;
                    // ignore reset connect if in sleep state
                    if ((config != null) && (mCurState != HELMET_CONN_STAGE_CLOSED)) {
                        if (mConnection != null) {
                            String newHost = config.webServerHost;
                            int newPort = config.webServerPort;
                            if (!TextUtils.equals(newHost, mConnection.getServerHost()) ||
                                    newPort != mConnection.getServerPort()) {

                                LogUtil.e(">>>>>>>>>>>>reset helmet connection<<<<<<<<<<<");

                                removeCallbacksAndMessages(null);
                                mConnection.stop();
                                mConnection = null;
                                mCurState = HELMET_CONN_STAGE_REGISTER;
                                triggerHelmetConnect();
                            }
                        } else {
                            triggerHelmetConnect();
                        }
                    }

                    break;

                case MSG_SEND_REQUEST_MESSAGE:
                    SendMessage sendMsg = (SendMessage) msg.obj;
                    if (mConnection != null) {
                        mConnection.sendMessage(sendMsg);
                    }

                    break;

                case MSG_REGISTER_SUCCESS:

                    if (mCurState == HELMET_CONN_STAGE_CLOSED) {
                        return;
                    }

                    Common.DeviceCfg deviceCfg = (Common.DeviceCfg) msg.obj;
                    if (deviceCfg != null) {
                        HelmetConfig.get().updateAll(deviceCfg);
                    }
                    mNonResponseHB = 0;
                    mCurState = HELMET_CONN_STAGE_HEARTBEAT;
                    triggerHelmetConnect();
                    break;

                case MSG_RECEIVE_HEARTBEAT_RESP:
                    if (msg.obj != null) {
                        try {
                            long time = (long) msg.obj;
                            Util.setSystemTime(time);
                        } catch (Exception e) {
                        }
                    }
                    if (mNonResponseHB > 0) {
                        mNonResponseHB--;
                    }
                    break;

                case MSG_TRIGGER_CONNECT:

                    if (mCurState == HELMET_CONN_STAGE_CLOSED) {
                        return;
                    }

                    if (!mConfig.isActivated()) {// do active
                        String activeResult = NetworkUtil.activeDevice();
                        LogUtil.e("activeResult: " + activeResult);
                        boolean success = updateActiveResult(activeResult);
                        if (success) {
                            mCurState = HELMET_CONN_STAGE_REGISTER;
                            triggerHelmetConnect();
                        } else {
                            //update state
                            mCurState = HELMET_CONN_STAGE_ACTIVE;
                        }
                    } else {
                        if (mConnection == null) {
                            String serverHost = mConfig.getWebServerHost();
                            int serverPort = mConfig.getWebServerPort();
                            startConnect(serverHost, serverPort);
                        }

                        if (mCurState == HELMET_CONN_STAGE_REGISTER) {//do register
                            LogUtil.d("register helmet......");
                            SendMessage message = new SendMessage(RegisterFactory.newInstance());
                            mConnection.sendMessage(message);
                        } else if (mCurState == HELMET_CONN_STAGE_HEARTBEAT) {//do heartbeat
                            if (mNonResponseHB >= 3) {
                                if (mConnStatusObserver != null) {
                                    mConnStatusObserver.onConnectStatus(false);
                                }
                                mNonResponseHB = 0;
                                mCurState = HELMET_CONN_STAGE_REGISTER;
                                triggerHelmetConnect();
                                LogUtil.e("more than 3 heartbeat not reachable: do register");
                            } else {
                                SendMessage message = new SendMessage(HeartbeatFactory.newInstance());
                                LogUtil.d("send heartbeat: " + message.mMsg.toString());
                                mConnection.sendMessage(message);
                                mNonResponseHB++;
                            }
                        }
                    }
                    break;

                case MSG_FORCE_REGISTER:

                    if (mCurState == HELMET_CONN_STAGE_CLOSED) {
                        return;
                    }

                    LogUtil.d("force register helmet......");
                    mCurState = HELMET_CONN_STAGE_REGISTER;
                    // commit to reduce frequency register operation
//                    if (mConnection != null) {
//                        this.removeCallbacksAndMessages(null);
//                        SendMessage message = new SendMessage(RegisterFactory.newInstance());
//                        mConnection.sendMessage(message);
//                    }
                    break;

                case MSG_NETWORK_DISCONNECT:
                    if (mCurState == HELMET_CONN_STAGE_CLOSED) {
                        return;
                    }

                    removeCallbacksAndMessages(null);
                    mCurState = HELMET_CONN_STAGE_REGISTER;
                    break;
            }
        }
    }

    public boolean isConnectionKeepLive() {
        return (mConnection != null && mConnection.isConnectKeepLive());
    }

    public void forceStart() {
        mHandler.sendEmptyMessage(MSG_FORCE_RESTART);
    }

    public void prepareSleep() {
        LogUtil.e("prepare to sleep.................");
        mHandler.sendEmptyMessage(MSG_PREPARE_SLEEP);
    }

    private void forceRegister() {
        mHandler.sendEmptyMessage(MSG_FORCE_REGISTER);
    }

    public void triggerHelmetConnect() {
        mHandler.sendEmptyMessage(MSG_TRIGGER_CONNECT);
    }

    /**
     * @param available
     */
    // TODO need synchronize
    public void onNetworkStateChanged(boolean available) {
        if (mConnection != null) {
            mConnection.onNetworkStateChange(available);
        }

        if (!available) {
            mHandler.sendEmptyMessage(MSG_NETWORK_DISCONNECT);
            ConnStatusChange.getInstance().onConnectStatus(false);
        } else {
            triggerHelmetConnect();
        }
    }
}
