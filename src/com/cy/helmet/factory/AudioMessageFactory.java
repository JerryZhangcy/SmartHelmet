package com.cy.helmet.factory;

import android.content.Context;

import com.cy.helmet.HelmetApplication;
import com.cy.helmet.config.HelmetConfig;
import com.cy.helmet.core.protocol.Common;
import com.cy.helmet.core.protocol.HelmetServer;
import com.cy.helmet.core.protocol.MessageId;
import com.cy.helmet.util.AppUtil;
import com.cy.helmet.util.LogUtil;
import com.cy.helmet.util.Util;
import com.google.protobuf.ByteString;

import java.io.File;
import java.util.UUID;

/**
 * Created by jiaqing on 2018/1/3.
 */

public class AudioMessageFactory {

    public static final int RECECEIVED_VOICE_MESSAGE = 1;
    public static final int START_PLAY_VOICE_MESSAGE = 2;
    public static final int FINISH_PLAY_VOICE_MESSAGE = 3;

    public static HelmetServer.H2SMessage newVoiceStatusMessage(String uuid, int status) {
        HelmetServer.H2SMessage.Builder h2Sbuilder = HelmetServer.H2SMessage.newBuilder();

        Context context = HelmetApplication.mAppContext;

        h2Sbuilder.setMsgid(MessageId.MsgId.H2S_MessageId_Play_Voice_Resp);

        h2Sbuilder.setVersion(AppUtil.getAppVersionName(context));
        h2Sbuilder.setDevid(HelmetConfig.get().getDeviceId());


        HelmetServer.H2SPlayVoiceStatus.Builder builder = HelmetServer.H2SPlayVoiceStatus.newBuilder();
        builder.setId(uuid);
        builder.setStatus(status);
        h2Sbuilder.setPlayVoiceStatus(builder);

        return h2Sbuilder.build();
    }

    public static HelmetServer.H2SMessage newRequestTalkMessage() {
        HelmetServer.H2SMessage.Builder h2Sbuilder = HelmetServer.H2SMessage.newBuilder();

        Context context = HelmetApplication.mAppContext;

        LogUtil.e("请求通话...");

        h2Sbuilder.setMsgid(MessageId.MsgId.H2S_MessageId_Req_Talk);
        h2Sbuilder.setVersion(AppUtil.getAppVersionName(context));
        h2Sbuilder.setDevid(HelmetConfig.get().getDeviceId());
        HelmetServer.H2SRequestTalk.Builder talkRequestBuilder = HelmetServer.H2SRequestTalk.newBuilder();
        h2Sbuilder.setRequestTalk(talkRequestBuilder.build());

        return h2Sbuilder.build();
    }

    public static HelmetServer.H2SMessage newSendTalkFailedMessage() {
        HelmetServer.H2SMessage.Builder h2Sbuilder = HelmetServer.H2SMessage.newBuilder();

        Context context = HelmetApplication.mAppContext;

        LogUtil.e("本地录音失败请求服务器重置");

        h2Sbuilder.setMsgid(MessageId.MsgId.H2S_MessageId_Refresh_Helmet_Voice);
        h2Sbuilder.setVersion(AppUtil.getAppVersionName(context));
        h2Sbuilder.setDevid(HelmetConfig.get().getDeviceId());
        HelmetServer.H2SRefreshHelmetVoice.Builder sendTalkFailed = HelmetServer.H2SRefreshHelmetVoice.newBuilder();
        h2Sbuilder.setRefreshHelmetVoice(sendTalkFailed.build());

        return h2Sbuilder.build();
    }

    public static HelmetServer.H2SMessage newUploadTalkMessage(File file) {
        if (file != null && file.exists()) {
            HelmetServer.H2SMessage.Builder h2Sbuilder = HelmetServer.H2SMessage.newBuilder();

            Context context = HelmetApplication.mAppContext;

            h2Sbuilder.setMsgid(MessageId.MsgId.H2S_MessageId_Send_Voice);
            h2Sbuilder.setVersion(AppUtil.getAppVersionName(context));
            h2Sbuilder.setDevid(HelmetConfig.get().getDeviceId());

            Common.VoiceData.Builder voiceBuilder = Common.VoiceData.newBuilder();
            String uuid = UUID.randomUUID().toString();
            voiceBuilder.setId(uuid);
            byte[] fileBytes = Util.readStreamFromFile(file);
            if (fileBytes == null) {
                return null;
            }

            voiceBuilder.setData(ByteString.copyFrom(fileBytes));
            h2Sbuilder.setVoiceData(voiceBuilder);

            return h2Sbuilder.build();
        }

        return null;
    }

    public static HelmetServer.H2SMessage newUploadTalkMessage(int voiceCode) {
        HelmetServer.H2SMessage.Builder h2Sbuilder = HelmetServer.H2SMessage.newBuilder();

        Context context = HelmetApplication.mAppContext;

        h2Sbuilder.setMsgid(MessageId.MsgId.H2S_MessageId_Heart_Beat);
        h2Sbuilder.setVersion(AppUtil.getAppVersionName(context));
        h2Sbuilder.setDevid(HelmetConfig.get().getDeviceId());

        Common.VoiceData.Builder voiceBuilder = Common.VoiceData.newBuilder();
        String uuid = UUID.randomUUID().toString();
        voiceBuilder.setId(uuid);
        voiceBuilder.setVoiceCode(voiceCode);

        h2Sbuilder.setVoiceData(voiceBuilder);

        return h2Sbuilder.build();
    }
}
