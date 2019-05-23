package com.cy.helmet;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.cy.helmet.conn.HelmetConnManager;
import com.cy.helmet.conn.HelmetMessageSender;
import com.cy.helmet.observer.ConnStatusChange;
import com.cy.helmet.util.LogUtil;
import com.cy.helmet.util.NetworkUtil;
import com.cy.helmet.voice.HelmetVoiceManager;

import org.json.JSONObject;

/**
 * Created by jiaqing on 2018/3/8.
 */

public class HelmetClient {

    private static final int MSG_SWITCH_DEVICE_STATE = 10001;
//    private static final int MSG_PLAY_HELMET_OFF = 10002;

    public static final int DEVICE_STATE_POWER_OFF = -1;
    public static final int DEVICE_STATE_PRE_SLEEP = 0;
    public static final int DEVICE_STATE_WAKE_UP = 1;

    private static int mCurState = DEVICE_STATE_POWER_OFF;
    private static int mNewState = DEVICE_STATE_POWER_OFF;

    private static Handler mHandler;
    private static int mPlayOffTimes = 0;

    /**
     * @param state dest state
     * @return wether switch to dest state
     */
    public static void switchHelmetState(int state) {

        if (state < DEVICE_STATE_PRE_SLEEP) {
            //illegal state
            return;
        }

        mNewState = state;
        if (mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    if (msg.what == MSG_SWITCH_DEVICE_STATE) {
                        if (mCurState != mNewState) {//state changed
                            mCurState = mNewState;
//                            startPlayTakeOffVoice();
                            forceResetDeviceState(mCurState);
                            mHandler.sendEmptyMessageDelayed(MSG_SWITCH_DEVICE_STATE, 2000);
                        }
                    }/* else if (msg.what == MSG_PLAY_HELMET_OFF) {
                        if (mCurState == DEVICE_STATE_PRE_SLEEP) {
                            if (mPlayOffTimes < 6) {
                                mPlayOffTimes++;
                                LogUtil.e("play helmet take off voice...");
                                HelmetVoiceManager.getInstance().playLocalVoice(HelmetVoiceManager.HELMET_TAKE_OFF);
                                mHandler.removeMessages(MSG_PLAY_HELMET_OFF);
                                mHandler.sendEmptyMessageDelayed(MSG_PLAY_HELMET_OFF, 10000);
                            } else {
                                removeMessages(MSG_PLAY_HELMET_OFF);
                            }
                        } else if (mCurState == DEVICE_STATE_WAKE_UP) {
                            removeMessages(MSG_PLAY_HELMET_OFF);
                        }
                    }*/
                }
            };
        }

        if (mCurState != mNewState) {
            if (!mHandler.hasMessages(MSG_SWITCH_DEVICE_STATE)) {
                mCurState = mNewState;
//                startPlayTakeOffVoice();
                forceResetDeviceState(mCurState);
                mHandler.sendEmptyMessageDelayed(MSG_SWITCH_DEVICE_STATE, 2000);
            }
        }
    }

    /*
    private static void startPlayTakeOffVoice() {
        if (mCurState == DEVICE_STATE_PRE_SLEEP) {
            mHandler.sendEmptyMessage(MSG_PLAY_HELMET_OFF);
        } else if (mCurState == DEVICE_STATE_WAKE_UP) {
            mHandler.removeMessages(MSG_PLAY_HELMET_OFF);
        }
    }*/

    private static void forceResetDeviceState(int state) {
        Intent intent = new Intent(HelmetApplication.mAppContext, HelmetMainService.class);
        Bundle bundle = new Bundle();
        bundle.putString("ACTION", "SWITCH_DEVICE_STATE");
        bundle.putInt("STATE", state);
        intent.putExtras(bundle);
        HelmetApplication.mAppContext.startService(intent);
    }

    public static boolean isSleepNow() {
        return mCurState == DEVICE_STATE_PRE_SLEEP;
    }

    public static void sendSOSMessage() {
        WorkThreadManager.executeOnSubThread(new Runnable() {
            @Override
            public void run() {
                if (HelmetConnManager.getInstance().isConnectionKeepLive()) {
                    LogUtil.e("send sos by long link...");
                    HelmetMessageSender.sendSOSMessage();
                } else {
                    String sosSendResult = NetworkUtil.sendSOSMessage();
                    LogUtil.e("send sos by http request: " + sosSendResult);
                    try {
                        JSONObject resultJSON = new JSONObject(sosSendResult);
                        int resultCode = resultJSON.getInt("code");
                        if (resultCode == 0) {
                            LogUtil.e("http send sos success...");
                            HelmetVoiceManager.getInstance().playLocalVoice(HelmetVoiceManager.SOS_SEND_SUCCESS);
                        }
                    } catch (Exception e) {
                        LogUtil.e("http send sos failed...");
                    }
                }
            }
        });
    }

    // test wear state
    public static boolean isWearHelmet() {
        return (mCurState == DEVICE_STATE_WAKE_UP);
    }
}
