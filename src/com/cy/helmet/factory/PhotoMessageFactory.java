package com.cy.helmet.factory;

import android.content.Context;

import com.cy.helmet.Constant;
import com.cy.helmet.HelmetApplication;
import com.cy.helmet.config.HelmetConfig;
import com.cy.helmet.core.protocol.Common;
import com.cy.helmet.core.protocol.HelmetServer;
import com.cy.helmet.core.protocol.MessageId;
import com.cy.helmet.util.AppUtil;
import com.cy.helmet.storage.FileUtil;
import com.cy.helmet.util.LogUtil;

import java.io.File;
import java.util.List;

/**
 * Created by jiaqing on 2018/1/4.
 */

public class PhotoMessageFactory {

    public static HelmetServer.H2SMessage getPhotoFileListMessage(long startTime, long endTime) {

        Common.FileList.Builder fileListBuilder = Common.FileList.newBuilder();
        fileListBuilder.setStartTime(startTime);
        fileListBuilder.setEndTime(endTime);

        List<File> photoFileList = FileUtil.getMediaFileList(Constant.MEDIA_PHOTO, startTime, endTime);
        for (File file : photoFileList) {
            Common.FileList.FileInfo.Builder fileInfoBuilder = Common.FileList.FileInfo.newBuilder();
            fileInfoBuilder.setFilename(file.getName());
            fileInfoBuilder.setSize((int) file.length());
            LogUtil.e("selectPhotoName>>>" + file.getName());
            LogUtil.e("selectPhotoSize>>>" + file.length());

            fileListBuilder.addFiles(fileInfoBuilder.build());
        }

        Context context = HelmetApplication.mAppContext;
        HelmetServer.H2SMessage.Builder h2SMessagebuilder = HelmetServer.H2SMessage.newBuilder();
        h2SMessagebuilder.setMsgid(MessageId.MsgId.H2S_MessageId_List_PhotoFile_Resp);
        h2SMessagebuilder.setVersion(AppUtil.getAppVersionName(context));
        h2SMessagebuilder.setDevid(HelmetConfig.get().getDeviceId());
        h2SMessagebuilder.setListPhotoFileResp(fileListBuilder);

        return h2SMessagebuilder.build();
    }

    public static HelmetServer.H2SMessage getTakePhotoRespMessage(File newPhotoFile) {

        String fileName = "";
        if (newPhotoFile != null && newPhotoFile.exists()) {
            fileName = newPhotoFile.getName();
        }

        LogUtil.e("response take photo>>>>>>>" + fileName);

        Context context = HelmetApplication.mAppContext;
        HelmetServer.H2SMessage.Builder h2SMessagebuilder = HelmetServer.H2SMessage.newBuilder();
        h2SMessagebuilder.setMsgid(MessageId.MsgId.H2S_MessageId_Take_Photo_Resp);
        h2SMessagebuilder.setVersion(AppUtil.getAppVersionName(context));
        h2SMessagebuilder.setDevid(HelmetConfig.get().getDeviceId());

        HelmetServer.H2STakePhotoFinished.Builder tpBuilder = HelmetServer.H2STakePhotoFinished.newBuilder();
        tpBuilder.setFileName(fileName);
        h2SMessagebuilder.setTakePhotoFinished(tpBuilder);

        return h2SMessagebuilder.build();
    }

    public static HelmetServer.H2SMessage getTakePhotoFinishMessage(File photoFile) {
        String fileName = "";
        if (photoFile != null && photoFile.exists()) {
            fileName = photoFile.getName();
        }

        LogUtil.e("finish take photo>>>>>>>" + fileName);

        Context context = HelmetApplication.mAppContext;
        HelmetServer.H2SMessage.Builder h2SMessagebuilder = HelmetServer.H2SMessage.newBuilder();
        h2SMessagebuilder.setMsgid(MessageId.MsgId.H2S_MessageId_Take_Photo);
        h2SMessagebuilder.setVersion(AppUtil.getAppVersionName(context));
        h2SMessagebuilder.setDevid(HelmetConfig.get().getDeviceId());

        HelmetServer.H2STakePhotoFinished.Builder tpBuilder = HelmetServer.H2STakePhotoFinished.newBuilder();
        tpBuilder.setFileName(fileName);
        h2SMessagebuilder.setTakePhotoFinished(tpBuilder);

        return h2SMessagebuilder.build();
    }
}
