package com.cy.helmet.factory;

import android.content.Context;

import com.cy.helmet.HelmetApplication;
import com.cy.helmet.config.HelmetConfig;
import com.cy.helmet.core.protocol.Common;
import com.cy.helmet.core.protocol.HelmetServer;
import com.cy.helmet.core.protocol.MessageId;
import com.cy.helmet.util.AppUtil;

/**
 * Created by ubuntu on 18-1-4.
 */

public class H2SHelmetCfgRespFactory {
    public static HelmetServer.H2SMessage newInstance(int status, Common.DeviceCfg cfg) {
        if (HelmetApplication.mAppContext == null) {
            return null;
        }

        Context context = HelmetApplication.mAppContext;

        HelmetServer.H2SMessage.Builder builder = HelmetServer.H2SMessage.newBuilder();
        builder.setMsgid(MessageId.MsgId.H2S_MessageId_HelmetCfg_Resp);
        builder.setVersion(AppUtil.getAppVersionName(context));
        builder.setDevid(HelmetConfig.get().getDeviceId());
        HelmetServer.H2SHelmetCfgResp.Builder cfgBuilder = HelmetServer.H2SHelmetCfgResp.newBuilder();

        cfgBuilder.setDeviceCfg(cfg);
        cfgBuilder.setResult(status);


        builder.setHelmetCfgResp(cfgBuilder);


        return builder.build();
    }

}
