package com.cy.helmet.voice;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.SparseArray;
import android.widget.Toast;

import com.cy.helmet.Constant;
import com.cy.helmet.HelmetApplication;
import com.cy.helmet.HelmetClient;
import com.cy.helmet.R;
import com.cy.helmet.callback.OnPlayCompleteListener;
import com.cy.helmet.conn.HelmetMessageSender;
import com.cy.helmet.timer.OnTimeoutListener;
import com.cy.helmet.core.protocol.Common;
import com.cy.helmet.core.protocol.MessageId;
import com.cy.helmet.core.protocol.ServerHelmet;
import com.cy.helmet.factory.AudioMessageFactory;
import com.cy.helmet.timer.TimeoutManager;
import com.cy.helmet.storage.FileUtil;
import com.cy.helmet.util.CommonUtil;
import com.cy.helmet.util.LogUtil;
import com.cy.helmet.util.NetworkUtil;
import com.google.protobuf.ByteString;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by jiaqing on 2018/1/3.
 */

public class HelmetVoiceManager {

    private static final int MESSAGE_RECEIVE_NEW_VOICE = 0;
    private static final int MESSAGE_RECORD_NEW_VOICE = 1;
    private static final int MESSAGE_SEND_NEW_VOICE = 2;
    private static final int MESSAGE_REPLAY_RECEIVED_VOICE = 3;

    //server voice code
    public static final int ENTER_FORBIDDEN_AREA = 100;
    public static final int ENTER_PERMISSIBLE_AREA = 101;
    public static final int LEAVE_WORK_AREA = 102;
    public static final int ENTER_WORK_AREA = 103;
    public static final int SERVER_CLOSE_GPS = 104;
    public static final int SERVER_OPEN_GPS = 105;
    public static final int EMERGENCY_EVACUATION = 200;

    //local voice id
    public static final int GPS_RETRY_REGISTER = 47000;
    public static final int GPS_CONNECTION_BREAK = 47001;
    public static final int GPS_ROLLBACK_CONFIG = 47002;
    public static final int GPS_TRANSFER_BASE_STATION = 47003;
    public static final int GPS_UPDATE_CONFIG = 47004;
    public static final int SOS_SEND_SUCCESS = 47005;
    public static final int START_RECORD_VIDEO = 47006;
    public static final int STOP_RECORD_VIDEO = 47007;
    public static final int SEND_VOICE_FAILED = 47009;
    public static final int START_LIVING = 47011;
    public static final int STOP_LIVING = 47012;
    public static final int SEND_SOS = 47013;
    public static final int REPLAY_LAST_VOICE = 47014;
    public static final int STARTING_UP_SUCCESS = 47015;
    public static final int HELMET_PUT_ON = 47016;
    public static final int HELMET_TAKE_OFF = 47017;
    public static final int LOW_POWER_ALERT = 47018;
    public static final int FUNCTION_NOT_SUPPORT = 47019;
    public static final int TALK_FINISH = 47020;
    public static final int TALK_FAILED = 47021;
    public static final int BLUETOOTH_PAIR_SUCCESS = 47022;
    public static final int POOR_NETWORK = 47023;
    public static final int NETWORK_RECOVERY = 47024;
    public static final int NETWORK_DISCONNECT = 47025;
    public static final int SOS_WARNING = 47026;
    public static final int BLUETOOTH_TURN_ON = 47027;
    public static final int BLUETOOTH_TURN_OFF = 47028;

    //start talk
    public static final int START_TALK_5S = 47029;
    public static final int START_TALK_10S = 47030;
    public static final int START_TALK_15S = 47031;
    public static final int START_TALK_20S = 47032;
    public static final int START_TALK_25S = 47033;
    public static final int START_TALK_30S = 47034;
    public static final int START_TALK_35S = 47035;
    public static final int START_TALK_40S = 47036;
    public static final int START_TALK_45S = 47037;
    public static final int START_TALK_50S = 47038;

    //camera hint
    public static final int CAMERA_CLICK = 47039;

    public static final int DEVICE_BOOT_COMPLETE = 47040;
    public static final int DEVICE_FOTA_SUCCESS = 47041;
    public static final int DEVICE_FOTA_DOWNLOAD_COMPLETE = 47042;
	
	public static final int DEVICE_LOW_BATTERY = 57002;//请注意，电量低
    public static final int DEVICE_POOR_NETWORK = 57003;//网络差
    public static final int DEVICE_NETWORK_REGAIN = 57004;//网络恢复
    public static final int DEVICE_WEAR_SUCCESS = 57005;//佩戴检测成功
    public static final int DEVICE_NETWORK_DISCONNECT = 57006;//网络已经断开
    public static final int DEVICE_WEAR_FAILED = 57007;//佩戴失敗
    public static final int DEVICE_TEMPERATURE_TOO_HIGH = 57008;//温度过高
    public static final int DEVICE_BLUETOOTH_DISCONNECT = 57009;//蓝牙断开连接

    private static HelmetVoiceManager mInstance;
    private HandlerThread mThread;
    private Handler mAudioHandler;

    private VoicePlayer mVoicePlayer;//voice player
    private VoiceRecorder mVoiceRecorder;//voice recorder

    private AtomicBoolean mIsRequestTalking = new AtomicBoolean(false);

    private SparseArray<Integer> mLocalVoiceIdResMap = new SparseArray<Integer>();

    //sound pool
    private SoundPool mSoundPool;
    private SparseArray<Integer> mSoundIdMap = new SparseArray<Integer>();

    public synchronized static HelmetVoiceManager getInstance() {
        if (mInstance == null) {
            mInstance = new HelmetVoiceManager();
        }
        return mInstance;
    }

    private HelmetVoiceManager() {
        if (mThread == null) {
            mThread = new HandlerThread("HELMET_AUDIO");
            mThread.start();
        }
        mAudioHandler = new AudioHandler(mThread.getLooper());

        initVoiceIdResMap();
        mVoiceRecorder = new VoiceRecorder();
        mVoicePlayer = new VoicePlayer();
        initSoundPool();
    }

    public void playLocalVoice(int voiceId) {
        if(CommonUtil.mIsShutDowning)//add by Jerry
            return;

        if (voiceId == HelmetVoiceManager.SOS_SEND_SUCCESS) {
            Voice.mTriggerSOS = true;
        }

        Integer resId = mLocalVoiceIdResMap.get(voiceId);
        if (resId != null) {
            Voice voice = new Voice(voiceId, resId);
            mVoicePlayer.playVoice(voice);
        }
    }

    public void playLocalVoice(int voiceId, OnPlayCompleteListener listener) {
        if(CommonUtil.mIsShutDowning)//add by Jerry
            return;
        Integer resId = mLocalVoiceIdResMap.get(voiceId);
        if (resId != null) {
            Voice voice = new Voice(voiceId, resId);
            voice.setOnPlayFinishListener(listener);
            mVoicePlayer.playVoice(voice);
        } else {
            if (listener != null) {
                listener.onPlayComplete(false);
            }
        }
    }

    public void playSoundPool(int voiceId) {
        if(CommonUtil.mIsShutDowning)//add by Jerry
            return;
        Integer soundId = mSoundIdMap.get(voiceId);
        if (soundId != null) {
            mSoundPool.play(soundId, 1, 1, 0, 0, 1);
        }
    }

    public void playStartTalk(int second, OnPlayCompleteListener listener) {
        int voiceId = -1;
        switch (second) {
            case 5:
                voiceId = START_TALK_5S;
                break;
            case 10:
                voiceId = START_TALK_10S;
                break;
            case 15:
                voiceId = START_TALK_15S;
                break;
            case 20:
                voiceId = START_TALK_20S;
                break;
            case 25:
                voiceId = START_TALK_25S;
                break;
            case 30:
                voiceId = START_TALK_30S;
                break;
            case 35:
                voiceId = START_TALK_35S;
                break;
            case 40:
                voiceId = START_TALK_40S;
                break;
            case 45:
                voiceId = START_TALK_45S;
                break;
            case 50:
                voiceId = START_TALK_50S;
                break;
            default:
                voiceId = -1;
                break;
        }

        if (voiceId > 0) {
            playLocalVoice(voiceId, listener);
        } else {
            if (listener != null) {
                listener.onPlayComplete(false);
            }
        }
    }

    private void initSoundPool() {
        Context context = HelmetApplication.mAppContext;
        if (Build.VERSION.SDK_INT >= 21) {
            SoundPool.Builder builder = new SoundPool.Builder();
            //传入音频数量
            builder.setMaxStreams(1);
            //AudioAttributes是一个封装音频各种属性的方法
            AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
            //设置音频流的合适的属性
            attrBuilder.setLegacyStreamType(AudioManager.STREAM_MUSIC);
            //加载一个AudioAttributes
            builder.setAudioAttributes(attrBuilder.build());
            mSoundPool = builder.build();
        } else {
            //当系统的SDK版本小于21时
            //设置最多可容纳2个音频流，音频的品质为5
            mSoundPool = new SoundPool(1, AudioManager.STREAM_SYSTEM, 5);
        }

        mSoundIdMap.put(CAMERA_CLICK, mSoundPool.load(context, R.raw.camera_click, 1));
        mSoundIdMap.put(START_RECORD_VIDEO, mSoundPool.load(context, R.raw.camera_init, 1));
        mSoundIdMap.put(STOP_RECORD_VIDEO, mSoundPool.load(context, R.raw.camera_stop, 1));
    }

    public void onReceiveAudioMessage(ServerHelmet.S2HMessage message) {

        if (message == null) {
            return;
        }

        MessageId.MsgId msgId = message.getMsgid();
        if (msgId == MessageId.MsgId.S2H_MessageId_Send_Voice) {
            mAudioHandler.removeMessages(MESSAGE_RECEIVE_NEW_VOICE);
            Message msg = new Message();
            msg.what = MESSAGE_RECEIVE_NEW_VOICE;
            msg.obj = message;
            mAudioHandler.sendMessage(msg);
        } else if (msgId == MessageId.MsgId.S2H_MessageId_Req_Talk_Resp) {
            if (!HelmetClient.isSleepNow()) {
                mIsRequestTalking.getAndSet(false);
                ServerHelmet.S2HRequestTalkResp talkResp = message.getRequestTalkResp();
                if (talkResp != null) {
                    boolean allowtalk = (talkResp.getResult() == 1);
                    int seconds = talkResp.getTalkSec();
                    LogUtil.e("allowTalk>>>>" + allowtalk);
                    if (allowtalk && seconds > 0) {
                        LogUtil.e("allowTalkSecond>>>>" + seconds);
                        mAudioHandler.removeMessages(MESSAGE_RECORD_NEW_VOICE);
                        Message msg = new Message();
                        msg.what = MESSAGE_RECORD_NEW_VOICE;
                        msg.arg1 = seconds;
                        mAudioHandler.sendMessage(msg);
                    } else {
                        HelmetVoiceManager.getInstance().playLocalVoice(HelmetVoiceManager.TALK_FAILED);
                    }
                }
            }
        }
    }

    public void replayReceivedVoice() {
        mAudioHandler.removeMessages(MESSAGE_REPLAY_RECEIVED_VOICE);
        Message msg = new Message();
        msg.what = MESSAGE_REPLAY_RECEIVED_VOICE;
        mAudioHandler.sendMessage(msg);
    }

    private File saveRemoteVoice(String uuid, byte[] bytes) {
        LogUtil.e("SaveAudioMessage..........." + uuid);

        File file = FileUtil.saveAudioMessage(uuid, bytes);
        if (file != null) {
            //send receive audio message
            HelmetMessageSender.sendRemoteVoiceResp(uuid, AudioMessageFactory.RECECEIVED_VOICE_MESSAGE);
        }

        return file;
    }

    public void sendRecordTalkMessage(File file) {
        if (file != null && file.exists()) {
            Message msg = new Message();
            msg.what = MESSAGE_SEND_NEW_VOICE;
            msg.obj = file;
            mAudioHandler.sendMessage(msg);
        }
    }

    public void requestTalk() {

        if (mVoiceRecorder != null && mVoiceRecorder.breakRecording()) {
            //interrupt voice record
            return;
        }

        if (mVoicePlayer.isPlaying()) {
            LogUtil.e("Playing voice now, ignore talking request");
            return;
        }

        if (mVoiceRecorder != null && mVoiceRecorder.isRecording()) {
            LogUtil.e("Recording now, ignore talking request");
            return;
        }

        if (HelmetClient.isSleepNow()) {
            HelmetVoiceManager.getInstance().playLocalVoice(HelmetVoiceManager.TALK_FAILED);
            LogUtil.e("Prepare to sleep, ignore talking request.");
            return;
        }

        if (!NetworkUtil.hasNetwork()) {
            HelmetVoiceManager.getInstance().playLocalVoice(HelmetVoiceManager.TALK_FAILED);
            return;
        }

        if (mIsRequestTalking.get()) {
            // avoid frequently request
            return;
        } else {
            mIsRequestTalking.getAndSet(true);
        }

        HelmetMessageSender.sendTalkRequest();
        TimeoutManager.getInstance().startTimeoutTask(TimeoutManager.TASK_ID_REQUEST_TALK, Constant.REQUEST_TALK_TIMEOUT_SEC, new OnTimeoutListener() {
            @Override
            public void onTimeout() {
                mIsRequestTalking.getAndSet(false);
                HelmetVoiceManager.getInstance().playLocalVoice(HelmetVoiceManager.TALK_FAILED);
                Toast.makeText(HelmetApplication.mAppContext, "请求对讲超时...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initVoiceIdResMap() {

        //server voice code
        mLocalVoiceIdResMap.put(ENTER_FORBIDDEN_AREA, R.raw.enter_forbidden_area);
        mLocalVoiceIdResMap.put(ENTER_PERMISSIBLE_AREA, R.raw.enter_permissible_area);
        mLocalVoiceIdResMap.put(ENTER_WORK_AREA, R.raw.enter_work_area);
        mLocalVoiceIdResMap.put(LEAVE_WORK_AREA, R.raw.leave_work_area);
        mLocalVoiceIdResMap.put(SERVER_CLOSE_GPS, R.raw.server_close_gps);
        mLocalVoiceIdResMap.put(SERVER_OPEN_GPS, R.raw.server_open_gps);
        mLocalVoiceIdResMap.put(EMERGENCY_EVACUATION, R.raw.emergency_evacuation);

        //local voice code
        mLocalVoiceIdResMap.put(GPS_RETRY_REGISTER, R.raw.gps_retry_register);
        mLocalVoiceIdResMap.put(GPS_CONNECTION_BREAK, R.raw.gps_connection_break);
        mLocalVoiceIdResMap.put(GPS_ROLLBACK_CONFIG, R.raw.gps_rollback_config);
        mLocalVoiceIdResMap.put(GPS_TRANSFER_BASE_STATION, R.raw.gps_transfer_base_station);
        mLocalVoiceIdResMap.put(GPS_UPDATE_CONFIG, R.raw.gps_update_config);
        mLocalVoiceIdResMap.put(SOS_SEND_SUCCESS, R.raw.sos_warning);//modify byJerry
        mLocalVoiceIdResMap.put(SEND_VOICE_FAILED, R.raw.send_voice_failed);
        mLocalVoiceIdResMap.put(START_LIVING, R.raw.start_living);
        mLocalVoiceIdResMap.put(STOP_LIVING, R.raw.stop_living);
        mLocalVoiceIdResMap.put(SEND_SOS, R.raw.send_sos);
        mLocalVoiceIdResMap.put(REPLAY_LAST_VOICE, R.raw.replay_last_voice);
        mLocalVoiceIdResMap.put(STARTING_UP_SUCCESS, R.raw.starting_up_success);
        mLocalVoiceIdResMap.put(HELMET_PUT_ON, R.raw.helmet_put_on);
        mLocalVoiceIdResMap.put(HELMET_TAKE_OFF, R.raw.helmet_take_off);
        mLocalVoiceIdResMap.put(LOW_POWER_ALERT, R.raw.low_power_alert);
        mLocalVoiceIdResMap.put(FUNCTION_NOT_SUPPORT, R.raw.function_not_support);
        mLocalVoiceIdResMap.put(TALK_FINISH, R.raw.talk_finish);
        mLocalVoiceIdResMap.put(TALK_FAILED, R.raw.talk_failed);
        mLocalVoiceIdResMap.put(BLUETOOTH_PAIR_SUCCESS, R.raw.bluetooth_pair_success);
        mLocalVoiceIdResMap.put(BLUETOOTH_TURN_ON, R.raw.bluetooth_turn_on);
        mLocalVoiceIdResMap.put(BLUETOOTH_TURN_OFF, R.raw.bluetooth_turn_off);
        mLocalVoiceIdResMap.put(POOR_NETWORK, R.raw.poor_network);
        mLocalVoiceIdResMap.put(NETWORK_DISCONNECT, R.raw.network_disconnect);
        mLocalVoiceIdResMap.put(NETWORK_RECOVERY, R.raw.network_recovery);
        mLocalVoiceIdResMap.put(SOS_WARNING, R.raw.sos_warning);

        mLocalVoiceIdResMap.put(START_TALK_5S, R.raw.start_talk_5s);
        mLocalVoiceIdResMap.put(START_TALK_10S, R.raw.start_talk_10s);
        mLocalVoiceIdResMap.put(START_TALK_15S, R.raw.start_talk_15s);
        mLocalVoiceIdResMap.put(START_TALK_20S, R.raw.start_talk_20s);
        mLocalVoiceIdResMap.put(START_TALK_25S, R.raw.start_talk_25s);
        mLocalVoiceIdResMap.put(START_TALK_30S, R.raw.start_talk_30s);
        mLocalVoiceIdResMap.put(START_TALK_35S, R.raw.start_talk_35s);
        mLocalVoiceIdResMap.put(START_TALK_40S, R.raw.start_talk_40s);
        mLocalVoiceIdResMap.put(START_TALK_45S, R.raw.start_talk_45s);
        mLocalVoiceIdResMap.put(START_TALK_50S, R.raw.start_talk_50s);

        //add by Jerry
        mLocalVoiceIdResMap.put(DEVICE_BOOT_COMPLETE, R.raw.kaijichenggong);
        mLocalVoiceIdResMap.put(DEVICE_LOW_BATTERY, R.raw.dianliangdi);
        mLocalVoiceIdResMap.put(DEVICE_POOR_NETWORK, R.raw.wangluocha);
        mLocalVoiceIdResMap.put(DEVICE_NETWORK_REGAIN, R.raw.wangluohuifu);
        mLocalVoiceIdResMap.put(DEVICE_WEAR_SUCCESS, R.raw.peidaijianchechenggong);
        mLocalVoiceIdResMap.put(DEVICE_NETWORK_DISCONNECT, R.raw.wangluolianjieduandiao);
        mLocalVoiceIdResMap.put(DEVICE_WEAR_FAILED, R.raw.peidaijiancheshibai);
        mLocalVoiceIdResMap.put(DEVICE_FOTA_SUCCESS, R.raw.system_update_success);
        mLocalVoiceIdResMap.put(DEVICE_FOTA_DOWNLOAD_COMPLETE, R.raw.system_ready_update);
        mLocalVoiceIdResMap.put(DEVICE_TEMPERATURE_TOO_HIGH,R.raw.temperature_toohigh);
        mLocalVoiceIdResMap.put(DEVICE_BLUETOOTH_DISCONNECT,R.raw.bluetooth_disconnect);


    }

    class AudioHandler extends Handler {

        public AudioHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Voice voice = null;

            switch (msg.what) {
                case MESSAGE_RECEIVE_NEW_VOICE:
                    ServerHelmet.S2HMessage message = (ServerHelmet.S2HMessage) msg.obj;
                    Common.VoiceData voiceData = message.getVoiceData();
                    ByteString audioBytes = voiceData.getData();
                    int localVoiceId = voiceData.getVoiceCode();
                    if (audioBytes != null && !audioBytes.isEmpty()) {
                        File voiceFile = saveRemoteVoice(voiceData.getId(), audioBytes.toByteArray());
                        voice = new Voice(voiceFile, voiceData.getId());
                    } else if (localVoiceId > 0) {
                        Integer voiceResId = mLocalVoiceIdResMap.get(localVoiceId);
                        if (voiceResId != null) {
                            voice = new Voice(localVoiceId, voiceResId);
                        }
                    }

                    if (voice != null) {
                        mVoicePlayer.playVoice(voice);
                    }

                    break;

                case MESSAGE_RECORD_NEW_VOICE:
                    int seconds = msg.arg1;
                    LogUtil.e("start record voice: " + seconds);
                    mVoiceRecorder.start(seconds);
                    break;

                case MESSAGE_SEND_NEW_VOICE:
                    File file = (File) msg.obj;
                    if (file != null && file.exists() && file.isFile()) {
                        final String voiceId = HelmetMessageSender.sendTalkingVoice(file);
                        file.delete();

                        // start timeout task
                        TimeoutManager.getInstance().startTimeoutTask(voiceId, 10, new OnTimeoutListener() {
                            @Override
                            public void onTimeout() {
                                HelmetVoiceManager.getInstance().playLocalVoice(HelmetVoiceManager.SEND_VOICE_FAILED);
                                Toast.makeText(HelmetApplication.mAppContext, "发送语音失败: " + voiceId, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    break;

                case MESSAGE_REPLAY_RECEIVED_VOICE:
                    if (mVoiceRecorder.isRecording()) {
                        LogUtil.e("Ignore replay request: device is recording now.");
                        return;
                    }

                    File voiceFile = FileUtil.getReceivedVoiceFile();
                    if (voiceFile != null) {
                        voice = new Voice(voiceFile, "");
                        mVoicePlayer.playVoice(voice);
                    }

                    break;
            }
        }
    }
}
