package com.cy.helmet.conn;

import com.cy.helmet.Constant;
import com.cy.helmet.WorkThreadManager;
import com.cy.helmet.config.HelmetConfig;
import com.cy.helmet.core.protocol.Common;
import com.cy.helmet.core.protocol.HelmetServer;
import com.cy.helmet.factory.AudioMessageFactory;
import com.cy.helmet.factory.FileMessageFactory;
import com.cy.helmet.factory.H2SFallDetectionFactory;
import com.cy.helmet.factory.H2SHelmetCfgRespFactory;
import com.cy.helmet.factory.OtherMessageFactory;
import com.cy.helmet.factory.PhotoMessageFactory;
import com.cy.helmet.factory.VideoMessageFactory;
import com.cy.helmet.util.LogUtil;
import com.cy.helmet.voice.HelmetVoiceManager;

import java.io.File;

/**
 * Created by jiaqing on 2018/3/16.
 */

public class HelmetMessageSender {

    public static void sendTakePhotoResp(File photoFile) {
        HelmetServer.H2SMessage msg = PhotoMessageFactory.getTakePhotoRespMessage(photoFile);
        SendMessage sendMessage = new SendMessage(msg);
        HelmetConnManager.getInstance().sendMessage(sendMessage);
    }

    public static void sendStartRecordResp(boolean isManual, final boolean success) {
        HelmetServer.H2SMessage msg = VideoMessageFactory.getStartRecordingRespMessage(isManual, success);
        SendMessage sendMessage = new SendMessage(msg);
        HelmetConnManager.getInstance().sendMessage(sendMessage);
    }

    public static void sendStopRecordResp(boolean isManual, final boolean success) {
        HelmetServer.H2SMessage msg = VideoMessageFactory.getStopRecordingRespMessage(isManual, success);
        SendMessage sendMessage = new SendMessage(msg);
        HelmetConnManager.getInstance().sendMessage(sendMessage);
    }

    public static void sendStartLiveResp(final boolean success) {
        HelmetServer.H2SMessage msg = VideoMessageFactory.getStartVideoLiveRespMessage(success);
        SendMessage sendMessage = new SendMessage(msg);
        HelmetConnManager.getInstance().sendMessage(sendMessage);
    }

    public static void sendStopLiveResp(final boolean success) {
        HelmetServer.H2SMessage msg = VideoMessageFactory.getStopVideoLiveRespMessage(success);
        SendMessage sendMessage = new SendMessage(msg);
        HelmetConnManager.getInstance().sendMessage(sendMessage);
    }

    // voice
    public static void sendTalkRequest() {
        HelmetServer.H2SMessage message = AudioMessageFactory.newRequestTalkMessage();
        SendMessage sendMessage = new SendMessage(message);
        HelmetConnManager.getInstance().sendMessage(sendMessage);
    }

    public static void sendTalkFailedRequest() {
        HelmetServer.H2SMessage message = AudioMessageFactory.newSendTalkFailedMessage();
        SendMessage sendMessage = new SendMessage(message);
        HelmetConnManager.getInstance().sendMessage(sendMessage);
    }


    public static void sendRemoteVoiceResp(String uuid, int action) {
        HelmetServer.H2SMessage message = AudioMessageFactory.newVoiceStatusMessage(uuid, action);
        SendMessage sendMessage = new SendMessage(message);
        HelmetConnManager.getInstance().sendMessage(sendMessage);
    }

    public static String sendTalkingVoice(File file) {
        HelmetServer.H2SMessage talkDataMsg = AudioMessageFactory.newUploadTalkMessage(file);
        SendMessage sendMessage = new SendMessage(talkDataMsg);
        if (talkDataMsg != null) {
            final String voiceId = talkDataMsg.getVoiceData().getId();
            LogUtil.e("start send voice: " + voiceId);
            HelmetConnManager.getInstance().sendMessage(sendMessage);
            return voiceId;
        }
        return null;
    }

    public static void sendHelmetConfigResp() {
        Common.DeviceCfg cfg = HelmetConfig.get().toDeviceCfg();
        LogUtil.e("" + cfg);
        SendMessage sendMessage = new SendMessage(H2SHelmetCfgRespFactory.newInstance(1, cfg));
        HelmetConnManager.getInstance().sendMessage(sendMessage);
    }

    // file operation
    public static void sendUploadFileResp(String fileName, int result) {
        HelmetServer.H2SMessage message = FileMessageFactory.getFileUploadRespMessage(fileName, result);
        SendMessage sendMessage = new SendMessage(message);
        HelmetConnManager.getInstance().sendMessage(sendMessage);
    }

    public static void sendFileListResp(final int fileType, final long startTime, final long endTime) {
        WorkThreadManager.executeOnSubThread(new Runnable() {
            @Override
            public void run() {
                HelmetServer.H2SMessage message = null;
                if (fileType == Constant.MEDIA_VIDEO) {
                    message = VideoMessageFactory.getVideoFileListMessage(startTime, endTime);
                } else if (fileType == Constant.MEDIA_PHOTO) {
                    message = PhotoMessageFactory.getPhotoFileListMessage(startTime, endTime);
                    LogUtil.e("photoList: " + message.toString());
                }

                if (message != null) {
                    SendMessage sendMsg = new SendMessage(message);
                    HelmetConnManager.getInstance().sendMessage(sendMsg);
                }
            }
        });
    }

    public static void sendDeleteFileResp(String wholeFileName, boolean delSuccess) {
        HelmetServer.H2SMessage msg = FileMessageFactory.getFileDelRespMessage(wholeFileName, delSuccess);
        SendMessage sendMessage = new SendMessage(msg);
        HelmetConnManager.getInstance().sendMessage(sendMessage);
    }

    public static void sendFallDetectMessage() {
        SendMessage sendMessage = new SendMessage(H2SFallDetectionFactory.newInstance());
        HelmetConnManager.getInstance().sendMessage(sendMessage);
    }

    public static void sendSOSMessage() {
        HelmetVoiceManager.getInstance().playLocalVoice(HelmetVoiceManager.SOS_SEND_SUCCESS);
        HelmetServer.H2SMessage msg = OtherMessageFactory.getSOSRequestMessage();
        SendMessage sendMessage2 = new SendMessage(msg);
        HelmetConnManager.getInstance().sendMessage(sendMessage2);
    }

    public static void sendNetworkChooseResp(int type) {
        HelmetServer.H2SMessage msg = OtherMessageFactory.getNetChooseRespMessage(type);
        SendMessage sendMessage2 = new SendMessage(msg);
        HelmetConnManager.getInstance().sendMessage(sendMessage2);
    }
}
