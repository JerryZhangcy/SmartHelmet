package com.cy.helmet.factory;

import android.content.Context;

import com.cy.helmet.HelmetApplication;
import com.cy.helmet.config.HelmetConfig;
import com.cy.helmet.core.protocol.HelmetServer;
import com.cy.helmet.core.protocol.MessageId;
import com.cy.helmet.util.AppUtil;
import com.cy.helmet.util.DeviceStatusUtil;

/**
 * Created by ubuntu on 18-1-4.
 */

public class DeviceStatusFactory {

    public static HelmetServer.H2SMessage newInstance() {
        if (HelmetApplication.mAppContext == null) {
            return null;
        }

        Context context = HelmetApplication.mAppContext;

        HelmetServer.H2SMessage.Builder builder = HelmetServer.H2SMessage.newBuilder();
        builder.setMsgid(MessageId.MsgId.H2S_MessageId_update_status);
        builder.setVersion(AppUtil.getAppVersionName(context));
        builder.setDevid(HelmetConfig.get().getDeviceId());
        builder.setDeviceStatus(DeviceStatusUtil.getDeviceStatus());


        return builder.build();
    }


}
