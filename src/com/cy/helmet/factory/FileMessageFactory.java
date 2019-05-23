package com.cy.helmet.factory;

import android.content.Context;

import com.cy.helmet.HelmetApplication;
import com.cy.helmet.config.HelmetConfig;
import com.cy.helmet.core.protocol.HelmetServer;
import com.cy.helmet.core.protocol.MessageId;
import com.cy.helmet.util.AppUtil;

import java.io.File;

/**
 * Created by jiaqing on 2018/1/4.
 */

public class FileMessageFactory {

    public static HelmetServer.H2SMessage getFileUploadRespMessage(String fileName, int result) {
        HelmetServer.H2SMessage.Builder h2Sbuilder = HelmetServer.H2SMessage.newBuilder();

        Context context = HelmetApplication.mAppContext;

        h2Sbuilder.setMsgid(MessageId.MsgId.H2S_MessageId_Get_File_Resp);
        h2Sbuilder.setVersion(AppUtil.getAppVersionName(context));
        h2Sbuilder.setDevid(HelmetConfig.get().getDeviceId());

        HelmetServer.H2SGetFileResp.Builder respBuilder = HelmetServer.H2SGetFileResp.newBuilder();
        respBuilder.setName(fileName);
        respBuilder.setResult(result);

        h2Sbuilder.setGetFileResp(respBuilder);
        return h2Sbuilder.build();
    }

    public static HelmetServer.H2SMessage getFileDelRespMessage(String fileName, boolean delSuccess) {
        HelmetServer.H2SMessage.Builder h2Sbuilder = HelmetServer.H2SMessage.newBuilder();

        Context context = HelmetApplication.mAppContext;

        h2Sbuilder.setMsgid(MessageId.MsgId.H2S_MessageId_Del_File_Resp);
        h2Sbuilder.setVersion(AppUtil.getAppVersionName(context));
        h2Sbuilder.setDevid(HelmetConfig.get().getDeviceId());

        HelmetServer.H2SDelFileResp.Builder delRespBuilder = HelmetServer.H2SDelFileResp.newBuilder();
        delRespBuilder.setFileName(fileName);
        if (delSuccess) {
            delRespBuilder.setResult(1);
        } else {
            delRespBuilder.setResult(0);
        }

        h2Sbuilder.setDelFileResp(delRespBuilder);
        return h2Sbuilder.build();
    }
}
