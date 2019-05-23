package com.cy.helmet.factory;

import android.content.Context;

import com.cy.helmet.HelmetApplication;
import com.cy.helmet.config.HelmetConfig;
import com.cy.helmet.core.protocol.Common;
import com.cy.helmet.core.protocol.HelmetServer;
import com.cy.helmet.core.protocol.MessageId;
import com.cy.helmet.util.AppUtil;
import com.cy.helmet.util.DeviceStatusUtil;
import com.cy.helmet.util.LogUtil;

/**
 * Created by jiaqing on 2018/1/2.
 */

public class RegisterFactory {

    public static HelmetServer.H2SMessage newInstance() {
        if (HelmetApplication.mAppContext == null) {
            return null;
        }

        Context context = HelmetApplication.mAppContext;

        HelmetServer.H2SMessage.Builder builder = HelmetServer.H2SMessage.newBuilder();
        builder.setMsgid(MessageId.MsgId.H2S_MessageId_Register);
        builder.setVersion(AppUtil.getAppVersionName(context));
        builder.setDevid(HelmetConfig.get().getDeviceId());

        Common.DeviceInfo.Builder devInfoBuilder = Common.DeviceInfo.newBuilder();
        devInfoBuilder.setDevid(HelmetConfig.get().getDeviceId());
        devInfoBuilder.setGpsid(DeviceStatusUtil.getGPSId());

        devInfoBuilder.setDeviceCfg(HelmetConfig.get().toDeviceCfg());
        devInfoBuilder.setDeviceStatus(DeviceStatusUtil.getDeviceStatus());

        //print device status
        LogUtil.e(DeviceStatusUtil.getDeviceStatus().toString());

        builder.setRegister(devInfoBuilder);

        return builder.build();
    }
}
