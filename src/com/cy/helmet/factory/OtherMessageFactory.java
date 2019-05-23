package com.cy.helmet.factory;

import android.content.Context;

import com.cy.helmet.HelmetApplication;
import com.cy.helmet.config.HelmetConfig;
import com.cy.helmet.core.protocol.HelmetServer;
import com.cy.helmet.core.protocol.MessageId;
import com.cy.helmet.util.AppUtil;

/**
 * Created by jiaqing on 2018/1/4.
 */

public class OtherMessageFactory {

    public static HelmetServer.H2SMessage getSOSRequestMessage() {

        Context context = HelmetApplication.mAppContext;
        HelmetServer.H2SMessage.Builder h2SMessagebuilder = HelmetServer.H2SMessage.newBuilder();
        h2SMessagebuilder.setMsgid(MessageId.MsgId.H2S_MessageId_Sos);
        h2SMessagebuilder.setVersion(AppUtil.getAppVersionName(context));
        h2SMessagebuilder.setDevid(HelmetConfig.get().getDeviceId());

        HelmetServer.H2SSos.Builder sosBuilder = HelmetServer.H2SSos.newBuilder();
        h2SMessagebuilder.setSos(sosBuilder);

        return h2SMessagebuilder.build();
    }

    public static HelmetServer.H2SMessage getNetChooseRespMessage(int netType) {
        Context context = HelmetApplication.mAppContext;

        HelmetServer.H2SMessage.Builder h2SMessagebuilder = HelmetServer.H2SMessage.newBuilder();
        h2SMessagebuilder.setMsgid(MessageId.MsgId.H2S_MessageId_NetTypeChoose_Resp);
        h2SMessagebuilder.setVersion(AppUtil.getAppVersionName(context));
        h2SMessagebuilder.setDevid(HelmetConfig.get().getDeviceId());

        HelmetServer.H2SNetTypeChooseResp.Builder builder = HelmetServer.H2SNetTypeChooseResp.newBuilder();
        builder.setResult(netType);

        h2SMessagebuilder.setNetTypeChooseResp(builder);

        return h2SMessagebuilder.build();
    }
}
