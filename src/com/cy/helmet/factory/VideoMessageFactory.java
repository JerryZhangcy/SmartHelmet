package com.cy.helmet.factory;

import android.content.Context;
import android.text.TextUtils;

import com.cy.helmet.Constant;
import com.cy.helmet.HelmetApplication;
import com.cy.helmet.config.HelmetConfig;
import com.cy.helmet.core.protocol.Common;
import com.cy.helmet.core.protocol.HelmetServer;
import com.cy.helmet.core.protocol.MessageId;
import com.cy.helmet.util.AppUtil;
import com.cy.helmet.storage.FileUtil;
import com.cy.helmet.util.LogUtil;
import com.cy.helmet.video.HelmetVideoManager;

import java.io.File;
import java.util.List;

/**
 * Created by jiaqing on 2018/1/4.
 */

public class VideoMessageFactory {

    public static HelmetServer.H2SMessage getVideoFileListMessage(long startTime, long endTime) {

        Common.FileList.Builder fileListBuilder = Common.FileList.newBuilder();
        fileListBuilder.setStartTime(startTime);
        fileListBuilder.setEndTime(endTime);

        List<File> videoFileList = FileUtil.getMediaFileList(Constant.MEDIA_VIDEO, startTime, endTime);
        String recordingFileName = HelmetVideoManager.getInstance().getRecordingFileName();
        boolean needFilter = !TextUtils.isEmpty(recordingFileName);
        for (File file : videoFileList) {
            if (needFilter && file.getName().equals(recordingFileName)) {
                continue;
            }
            Common.FileList.FileInfo.Builder fileInfoBuilder = Common.FileList.FileInfo.newBuilder();
            fileInfoBuilder.setFilename(file.getName());
            fileInfoBuilder.setSize((int) file.length());

            LogUtil.e("selectFileName>>>" + file.getName());
            LogUtil.e("selectFileSize>>>" + file.length());

            fileListBuilder.addFiles(fileInfoBuilder.build());
        }

        Context context = HelmetApplication.mAppContext;
        HelmetServer.H2SMessage.Builder h2SMessagebuilder = HelmetServer.H2SMessage.newBuilder();
        h2SMessagebuilder.setMsgid(MessageId.MsgId.H2S_MessageId_List_VedioFile_Resp);
        h2SMessagebuilder.setVersion(AppUtil.getAppVersionName(context));
        h2SMessagebuilder.setDevid(HelmetConfig.get().getDeviceId());
        h2SMessagebuilder.setListVideoFileResp(fileListBuilder);

        return h2SMessagebuilder.build();
    }

    /**
     * get start recording resp message
     *
     * @param isManual true if the operation is executed by helmet, other by server
     * @param success  if the operation is success or not
     */
    public static HelmetServer.H2SMessage getStartRecordingRespMessage(boolean isManual, boolean success) {
        Context context = HelmetApplication.mAppContext;
        HelmetServer.H2SMessage.Builder h2SMessagebuilder = HelmetServer.H2SMessage.newBuilder();
        h2SMessagebuilder.setVersion(AppUtil.getAppVersionName(context));
        h2SMessagebuilder.setDevid(HelmetConfig.get().getDeviceId());

        if (isManual) {
            h2SMessagebuilder.setMsgid(MessageId.MsgId.H2S_MessageId_Start_Video);
            HelmetServer.H2SStartVideo.Builder builder = HelmetServer.H2SStartVideo.newBuilder();
            h2SMessagebuilder.setStartVideo(builder);
        } else {
            h2SMessagebuilder.setMsgid(MessageId.MsgId.H2S_MessageId_Start_Video_Resp);
            HelmetServer.H2SStartVideoResp.Builder tvBuilder = HelmetServer.H2SStartVideoResp.newBuilder();
            tvBuilder.setResult(success ? 1 : 0);
            h2SMessagebuilder.setStartVideoResp(tvBuilder);
        }

        return h2SMessagebuilder.build();
    }

    /**
     * get stop recording resp message
     *
     * @param isManual true if the operation is executed by helmet, other by server
     * @param success  if the operation is success or not
     */
    public static HelmetServer.H2SMessage getStopRecordingRespMessage(boolean isManual, boolean success) {
        Context context = HelmetApplication.mAppContext;
        HelmetServer.H2SMessage.Builder h2SMessagebuilder = HelmetServer.H2SMessage.newBuilder();
        h2SMessagebuilder.setVersion(AppUtil.getAppVersionName(context));
        h2SMessagebuilder.setDevid(HelmetConfig.get().getDeviceId());
        if (isManual) {//executed by helmet
            h2SMessagebuilder.setMsgid(MessageId.MsgId.H2S_MessageId_Stop_Video);
            HelmetServer.H2SStopVideo.Builder builder = HelmetServer.H2SStopVideo.newBuilder();
            builder.setResult(1);
            h2SMessagebuilder.setStopVideo(builder);
        } else {//executed by server
            h2SMessagebuilder.setMsgid(MessageId.MsgId.H2S_MessageId_Stop_Video_Resp);
            HelmetServer.S2HServerStopVideoResp.Builder tvBuilder = HelmetServer.S2HServerStopVideoResp.newBuilder();
            tvBuilder.setResult(success ? 1 : 0);
            h2SMessagebuilder.setServerStopVideoResp(tvBuilder);
        }

        return h2SMessagebuilder.build();
    }

    public static HelmetServer.H2SMessage getStartVideoLiveRespMessage(boolean success) {
        Context context = HelmetApplication.mAppContext;
        HelmetServer.H2SMessage.Builder h2SMessagebuilder = HelmetServer.H2SMessage.newBuilder();
        h2SMessagebuilder.setMsgid(MessageId.MsgId.H2S_MessageId_Start_Video_Live_Resp);
        h2SMessagebuilder.setVersion(AppUtil.getAppVersionName(context));
        h2SMessagebuilder.setDevid(HelmetConfig.get().getDeviceId());

        HelmetServer.H2SStartVideoLiveResp.Builder vlrBuilder = HelmetServer.H2SStartVideoLiveResp.newBuilder();
        vlrBuilder.setResult(success ? 1 : 0);

        h2SMessagebuilder.setStartVideoLiveResp(vlrBuilder);

        return h2SMessagebuilder.build();
    }

    public static HelmetServer.H2SMessage getStopVideoLiveRespMessage(boolean success) {
        Context context = HelmetApplication.mAppContext;
        HelmetServer.H2SMessage.Builder h2SMessagebuilder = HelmetServer.H2SMessage.newBuilder();
        h2SMessagebuilder.setMsgid(MessageId.MsgId.H2S_MessageId_Stop_Video_Live_Resp);
        h2SMessagebuilder.setVersion(AppUtil.getAppVersionName(context));
        h2SMessagebuilder.setDevid(HelmetConfig.get().getDeviceId());

        HelmetServer.H2SStopVideoLiveResp.Builder vlrBuilder = HelmetServer.H2SStopVideoLiveResp.newBuilder();
        vlrBuilder.setResult(success ? 1 : 0);

        h2SMessagebuilder.setStopVideoLiveResp(vlrBuilder);

        return h2SMessagebuilder.build();
    }

    public static HelmetServer.H2SMessage getStrtTakeVideoReqMessage() {
        Context context = HelmetApplication.mAppContext;
        HelmetServer.H2SMessage.Builder h2SMessagebuilder = HelmetServer.H2SMessage.newBuilder();
        h2SMessagebuilder.setMsgid(MessageId.MsgId.H2S_MessageId_Start_Video);
        h2SMessagebuilder.setVersion(AppUtil.getAppVersionName(context));
        h2SMessagebuilder.setDevid(HelmetConfig.get().getDeviceId());

        HelmetServer.H2SStartVideo.Builder svBuilder = HelmetServer.H2SStartVideo.newBuilder();
        h2SMessagebuilder.setStartVideo(svBuilder);

        return h2SMessagebuilder.build();
    }

    public static HelmetServer.H2SMessage getStopTakeVideoReqMessage(boolean success) {
        Context context = HelmetApplication.mAppContext;
        HelmetServer.H2SMessage.Builder h2SMessagebuilder = HelmetServer.H2SMessage.newBuilder();
        h2SMessagebuilder.setMsgid(MessageId.MsgId.H2S_MessageId_Stop_Video);
        h2SMessagebuilder.setVersion(AppUtil.getAppVersionName(context));
        h2SMessagebuilder.setDevid(HelmetConfig.get().getDeviceId());

        HelmetServer.H2SStopVideo.Builder svBuilder = HelmetServer.H2SStopVideo.newBuilder();
        svBuilder.setResult(success ? 1 : 0);
        h2SMessagebuilder.setStopVideo(svBuilder);

        return h2SMessagebuilder.build();
    }


}
