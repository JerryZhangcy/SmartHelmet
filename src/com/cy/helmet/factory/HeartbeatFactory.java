package com.cy.helmet.factory;

import android.content.Context;

import com.cy.helmet.HelmetApplication;
import com.cy.helmet.config.HelmetConfig;
import com.cy.helmet.core.protocol.Common;
import com.cy.helmet.core.protocol.HelmetServer;
import com.cy.helmet.core.protocol.MessageId;
import com.cy.helmet.util.AppUtil;
import com.cy.helmet.util.DeviceStatusUtil;

/**
 * Created by yaojiaqing on 2017/12/30.
 */

public class HeartbeatFactory {

    public static HelmetServer.H2SMessage newInstance() {
        if (HelmetApplication.mAppContext == null) {
            return null;
        }

        Context context = HelmetApplication.mAppContext;

        HelmetServer.H2SMessage.Builder builder = HelmetServer.H2SMessage.newBuilder();
        builder.setMsgid(MessageId.MsgId.H2S_MessageId_Heart_Beat);

        builder.setVersion(AppUtil.getAppVersionName(context));
        builder.setDevid(HelmetConfig.get().getDeviceId());


        HelmetServer.H2SHeartBeat.Builder hbBuilder = HelmetServer.H2SHeartBeat.newBuilder();
        hbBuilder.setTime(System.currentTimeMillis());
        hbBuilder.setDeviceStatus(DeviceStatusUtil.getDeviceStatus());
//
//        //set device status
//        Common.DeviceStatus.Builder statusBuilder = Common.DeviceStatus.newBuilder();
//        statusBuilder.setPower(74);//power(1-100)
//        statusBuilder.setNetStatus(2);//network state(1-poor, 2-commom, 3-good)
//        statusBuilder.setNetType(2);//network type(1-wifi, 2-data)
//        statusBuilder.setTfCardLeft(50);//TF card usable capacity(1-100)
//        statusBuilder.setGpsType(2);//GPS type(1-lbs, 2-gps)
//        statusBuilder.setVideoStatus(2);//video status
//        statusBuilder.setHelmetType(2);//helmet type(1-low, 2-middle, 3-high)
//        statusBuilder.setAdornStatus(2);//wear status(1-wear, 2-unwear)
//
//        //set version
//        Common.SoftwareVersion.Builder versionBuilder = Common.SoftwareVersion.newBuilder();
//        versionBuilder.setName(AppUtil.getAppVersionName(context));
//        versionBuilder.setVersion(AppUtil.getAppVersionCode(context) + "");
//        statusBuilder.setSoftwareVersion(versionBuilder);
//
//        hbBuilder.setDeviceStatus(statusBuilder);
        builder.setHeartBeat(hbBuilder);

        return builder.build();
    }


}
