package com.cy.helmet.voice;

import android.text.TextUtils;
import android.util.SparseArray;

import com.cy.helmet.callback.OnPlayCompleteListener;
import com.cy.helmet.util.LogUtil;

import java.io.File;

/**
 * Created by yaojiaqing on 2018/1/29.
 */

public class Voice {

    public static final int VOICE_STYLE_NONE = -1;
    public static final int VOICE_STYLE_RES = 0;
    public static final int VOICE_STYLE_FILE = 1;

    //1 - highest priority, 6 - lowest priority
    public static final int VOICE_PRIORITY_1 = 1;
    public static final int VOICE_PRIORITY_2 = 2;
    public static final int VOICE_PRIORITY_3 = 3;
    public static final int VOICE_PRIORITY_4 = 4;
    public static final int VOICE_PRIORITY_5 = 5;
    public static final int VOICE_PRIORITY_6 = 6;

    //default priority - lowest priority
    private int mPriority = VOICE_PRIORITY_6;

    //local voice id
    private int mVoiceResId;
    private int mVoiceId;

    //remote voice file and uuid
    private String mVoiceUUID;
    private File mVoiceFile;

    private OnPlayCompleteListener mListener;

    private int mVoiceStyle = VOICE_STYLE_NONE;

    //add by Jerry
    public static boolean mTriggerSOS = false;

    public Voice(int voiceId, int voiceResId) {
        mVoiceId = voiceId;
        mVoiceResId = voiceResId;
        mVoiceStyle = VOICE_STYLE_RES;
        Integer priority = mVoicePriorityMap.get(voiceId);
        if (priority != null) {
            mPriority = priority;
        }
    }

    public void setOnPlayFinishListener(OnPlayCompleteListener listener) {
        mListener = listener;
    }

    public void callBackIfNecessary(boolean success) {
        if (mListener != null && needCallback()) {
            mListener.onPlayComplete(success);
        }
    }

    public boolean needCallback() {
        return isRecordVoice();
    }

    public Voice(File voiceFile, String uuid) {
        if (voiceFile != null && voiceFile.exists() || voiceFile.isFile()) {
            mVoiceFile = voiceFile;
            mVoiceUUID = uuid;
            mVoiceStyle = VOICE_STYLE_FILE;
            if (!TextUtils.isEmpty(uuid)) {
                // latest downloaded voice
                mPriority = VOICE_PRIORITY_2;
            } else {
                //last downloaded voice
                mPriority = VOICE_PRIORITY_6;
            }

        }
    }

    public boolean isValid() {
        return mVoiceStyle != VOICE_STYLE_NONE;
    }

    public boolean isResID() {
        return mVoiceStyle == VOICE_STYLE_RES;
    }

    public boolean isFile() {
        return mVoiceStyle == VOICE_STYLE_FILE;
    }

    public int getVoiceID() {
        return mVoiceId;
    }

    public String getUUID() {
        return mVoiceUUID;
    }

    public File getFile() {
        return mVoiceFile;
    }

    public int getResId() {
        return mVoiceResId;
    }

    public int getPriority() {
        return mPriority;
    }

    public boolean isRecordVoice() {
        return (mVoiceId >= HelmetVoiceManager.START_TALK_5S) && (mVoiceId <= HelmetVoiceManager.START_TALK_50S);
    }

    public int getType() {
        return mVoiceStyle;
    }


    private static SparseArray<Integer> mVoicePriorityMap = new SparseArray<Integer>();

    static {
        //sos resp voice
        mVoicePriorityMap.put(HelmetVoiceManager.SOS_SEND_SUCCESS, VOICE_PRIORITY_1);

        //server voice code
        mVoicePriorityMap.put(HelmetVoiceManager.ENTER_FORBIDDEN_AREA, VOICE_PRIORITY_2);
        mVoicePriorityMap.put(HelmetVoiceManager.ENTER_PERMISSIBLE_AREA, VOICE_PRIORITY_2);
        mVoicePriorityMap.put(HelmetVoiceManager.LEAVE_WORK_AREA, VOICE_PRIORITY_2);
        mVoicePriorityMap.put(HelmetVoiceManager.ENTER_WORK_AREA, VOICE_PRIORITY_2);
        mVoicePriorityMap.put(HelmetVoiceManager.SERVER_CLOSE_GPS, VOICE_PRIORITY_2);
        mVoicePriorityMap.put(HelmetVoiceManager.SERVER_OPEN_GPS, VOICE_PRIORITY_2);
        mVoicePriorityMap.put(HelmetVoiceManager.EMERGENCY_EVACUATION, VOICE_PRIORITY_2);
        mVoicePriorityMap.put(HelmetVoiceManager.SEND_VOICE_FAILED, VOICE_PRIORITY_2);
        mVoicePriorityMap.put(HelmetVoiceManager.START_TALK_5S, VOICE_PRIORITY_2);
        mVoicePriorityMap.put(HelmetVoiceManager.START_TALK_10S, VOICE_PRIORITY_2);
        mVoicePriorityMap.put(HelmetVoiceManager.START_TALK_15S, VOICE_PRIORITY_2);
        mVoicePriorityMap.put(HelmetVoiceManager.START_TALK_20S, VOICE_PRIORITY_2);
        mVoicePriorityMap.put(HelmetVoiceManager.START_TALK_25S, VOICE_PRIORITY_2);
        mVoicePriorityMap.put(HelmetVoiceManager.START_TALK_30S, VOICE_PRIORITY_2);
        mVoicePriorityMap.put(HelmetVoiceManager.START_TALK_35S, VOICE_PRIORITY_2);
        mVoicePriorityMap.put(HelmetVoiceManager.START_TALK_40S, VOICE_PRIORITY_2);
        mVoicePriorityMap.put(HelmetVoiceManager.START_TALK_45S, VOICE_PRIORITY_2);
        mVoicePriorityMap.put(HelmetVoiceManager.START_TALK_50S, VOICE_PRIORITY_2);
        mVoicePriorityMap.put(HelmetVoiceManager.TALK_FINISH, VOICE_PRIORITY_2);
        mVoicePriorityMap.put(HelmetVoiceManager.TALK_FAILED, VOICE_PRIORITY_2);

        mVoicePriorityMap.put(HelmetVoiceManager.DEVICE_WEAR_SUCCESS, VOICE_PRIORITY_3);//modify by Jerry
        mVoicePriorityMap.put(HelmetVoiceManager.BLUETOOTH_PAIR_SUCCESS, VOICE_PRIORITY_2);
        mVoicePriorityMap.put(HelmetVoiceManager.DEVICE_BLUETOOTH_DISCONNECT, VOICE_PRIORITY_2);//add by jerry
        mVoicePriorityMap.put(HelmetVoiceManager.BLUETOOTH_TURN_ON, VOICE_PRIORITY_2);
        mVoicePriorityMap.put(HelmetVoiceManager.BLUETOOTH_TURN_OFF, VOICE_PRIORITY_2);

        mVoicePriorityMap.put(HelmetVoiceManager.REPLAY_LAST_VOICE, VOICE_PRIORITY_4);
        mVoicePriorityMap.put(HelmetVoiceManager.DEVICE_LOW_BATTERY, VOICE_PRIORITY_4);//modify by Jerry

        mVoicePriorityMap.put(HelmetVoiceManager.START_LIVING, VOICE_PRIORITY_5);
        mVoicePriorityMap.put(HelmetVoiceManager.STOP_LIVING, VOICE_PRIORITY_5);
        mVoicePriorityMap.put(HelmetVoiceManager.DEVICE_BOOT_COMPLETE, VOICE_PRIORITY_5);
        mVoicePriorityMap.put(HelmetVoiceManager.DEVICE_POOR_NETWORK, VOICE_PRIORITY_5);//modify by Jerry
        mVoicePriorityMap.put(HelmetVoiceManager.DEVICE_NETWORK_REGAIN, VOICE_PRIORITY_5);//modify by Jerry
        mVoicePriorityMap.put(HelmetVoiceManager.DEVICE_NETWORK_DISCONNECT, VOICE_PRIORITY_5);//modify by Jerry

        mVoicePriorityMap.put(HelmetVoiceManager.FUNCTION_NOT_SUPPORT, VOICE_PRIORITY_6);
        mVoicePriorityMap.put(HelmetVoiceManager.DEVICE_WEAR_FAILED, VOICE_PRIORITY_6);//modify by Jerry
        mVoicePriorityMap.put(HelmetVoiceManager.DEVICE_FOTA_SUCCESS, VOICE_PRIORITY_6);
        mVoicePriorityMap.put(HelmetVoiceManager.DEVICE_FOTA_DOWNLOAD_COMPLETE, VOICE_PRIORITY_6);
        mVoicePriorityMap.put(HelmetVoiceManager.DEVICE_TEMPERATURE_TOO_HIGH,VOICE_PRIORITY_2);
    }
}
