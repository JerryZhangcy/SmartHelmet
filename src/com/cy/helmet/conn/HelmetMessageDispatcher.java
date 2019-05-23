package com.cy.helmet.conn;

import android.content.Intent;
import android.util.Log;

import com.cy.helmet.Constant;
import com.cy.helmet.HelmetApplication;
import com.cy.helmet.HelmetClient;
import com.cy.helmet.R;
import com.cy.helmet.MainActivity;
import com.cy.helmet.WorkThreadManager;
import com.cy.helmet.config.HelmetConfig;
import com.cy.helmet.core.protocol.Common;
import com.cy.helmet.core.protocol.ServerHelmet;
import com.cy.helmet.timer.TimeoutManager;
import com.cy.helmet.storage.LocalFileManager;
import com.cy.helmet.util.LogUtil;
import com.cy.helmet.util.NetworkUtil;
import com.cy.helmet.util.VideoUtils;
import com.cy.helmet.video.stream.sender.sendqueue.NormalSendQueue;
import com.cy.helmet.voice.HelmetVoiceManager;

/**
 * Created by ubuntu on 18-1-3.
 */

public class HelmetMessageDispatcher {

    public void dispatchMessage(final ServerHelmet.S2HMessage s2HMessage) {
        if (s2HMessage == null) {
            return;
        }

        WorkThreadManager.executeOnDispatchThread(new Runnable() {
            @Override
            public void run() {
                handleMessage(s2HMessage);
            }
        });
    }

    public void handleMessage(ServerHelmet.S2HMessage s2HMessage) {

        LogUtil.d("S2H= " + s2HMessage.toString());
        switch (s2HMessage.getMsgid()) {
            case S2H_MessageId_Sos_Resp:
                if (s2HMessage.hasSosResp()) {
                    ServerHelmet.S2HSosResp sosResp = s2HMessage.getSosResp();
                    int sosResult = sosResp.getResult();
                    if (sosResult == 1) {
                        HelmetVoiceManager.getInstance().playLocalVoice(HelmetVoiceManager.SOS_SEND_SUCCESS);
                    }
                }

                break;

            // push config from server at anytime
            case S2H_MessageId_Device_Cfg:
                LogUtil.d(s2HMessage);
                if (s2HMessage.hasDeviceCfg()) {
                    Common.DeviceCfg cfg = s2HMessage.getDeviceCfg();
                    HelmetConfig.get().updateAll(cfg);
                }

                HelmetMessageSender.sendHelmetConfigResp();
                break;

            // the message from server to respond talking request
            case S2H_MessageId_Req_Talk_Resp:
                // ignore talk if in pre-sleep state
                if (HelmetClient.isSleepNow()) {
                    LogUtil.e("Ignore talk response during pre-sleep state.");
                    HelmetMessageSender.sendTalkingVoice(null);
                    break;
                }

                boolean withinTimeout = TimeoutManager.getInstance().removeTimeoutTask(TimeoutManager.TASK_ID_REQUEST_TALK);
                if (withinTimeout) {
                    HelmetVoiceManager.getInstance().onReceiveAudioMessage(s2HMessage);
                } else {
                    LogUtil.w("request talk resp is without timeout");
                    HelmetMessageSender.sendTalkingVoice(null);
                }

                break;

            // voice data message send by server
            case S2H_MessageId_Send_Voice:
                HelmetVoiceManager.getInstance().onReceiveAudioMessage(s2HMessage);
                break;

            // response message from server to indicated
            // the server has received voice data send by helmet
            case S2H_MessageId_Send_Voice_Resp://
                ServerHelmet.S2HSendVoiceResp resp = s2HMessage.getSendVoiceResp();
                String uuid = resp.getId();
                int result = resp.getResult();
                if (result == Constant.RESULT_OK) {
                    TimeoutManager.getInstance().removeTimeoutTask(uuid);
                }
                break;

            // the order to upload video file list
            case S2H_MessageId_List_VedioFile:
                if (s2HMessage.hasListVideoFile()) {
                    ServerHelmet.S2HListFile listFile = s2HMessage.getListVideoFile();
                    final long startVideoTime = listFile.getStartTime();
                    final long endVideoTime = listFile.getEndTime();
                    uploadFileList(Constant.MEDIA_VIDEO, startVideoTime, endVideoTime);
                }
                break;

            // the order to upload video file list
            case S2H_MessageId_List_PhotoFile:
                if (s2HMessage.hasListPhotoFile()) {
                    ServerHelmet.S2HListFile listPhotoFile = s2HMessage.getListPhotoFile();
                    final long startPhotoTime = listPhotoFile.getStartTime();
                    final long endPhotoTime = listPhotoFile.getEndTime();
                    uploadFileList(Constant.MEDIA_PHOTO, startPhotoTime, endPhotoTime);
                }
                break;

            // the order to upload appointed file
            case S2H_MessageId_Get_File:
                if (s2HMessage.hasGetFile()) {
                    final ServerHelmet.S2HGetFile file = s2HMessage.getGetFile();
                    final String fileName = file.getFileName();

                    // ignore file upload request if in pre-sleep state
                    if (HelmetClient.isSleepNow()) {
                        LogUtil.e("Ignore file uploading during pre-sleep state.");
                        HelmetMessageSender.sendUploadFileResp(fileName, Constant.FILE_UPLOAD_FAILED);
                        break;
                    }

                    final int isSync = file.getIsSync();
                    final int fileType = file.getType();

                    //use dispatch url to upload file
                    final String url = file.getUrlAddr();

                    //get file upload speed limit
                    int speedLimit = file.getSpeedLimit();
                    speedLimit = (speedLimit <= 0 ? 0 : speedLimit);

                    LocalFileManager.getInstance().uploadFile(url, fileName, fileType, speedLimit, isSync == Constant.UPLOAD_IS_SYNC);
                }

                break;

            // the order to delete appointed file
            case S2H_MessageId_Del_File:
                if (s2HMessage.hasDelFile()) {
                    ServerHelmet.S2HDelFile delFile = s2HMessage.getDelFile();
                    String fileName = delFile.getFileName();
                    int fileType = delFile.getType();
                    deleteFile(fileName);
                }

                break;

            case S2H_MessageId_Take_Photo:
                // ignore take photo request if in pre-sleep state
                if (HelmetClient.isSleepNow()) {
                    LogUtil.e("Ignore taking photo during pre-sleep state.");
                    HelmetMessageSender.sendTakePhotoResp(null);
                    break;
                }

                ServerHelmet.S2HTakePhoto takePhotoMsg = s2HMessage.getTakePhoto();
                boolean playVoice = (Constant.PLAY_HINT_WHEN_TAKE_PHOTO == takePhotoMsg.getPlayVoice());
                boolean isCompress = false;
                LogUtil.e("takePhoto>>>>>" + playVoice + ":" + isCompress);

                if (takePhotoMsg.hasDocompress()) {
                    isCompress = (takePhotoMsg.getDocompress() == 1);
                }

                VideoUtils.takePhoto(playVoice, isCompress);

                break;

            case S2H_MessageId_Start_Video:
                // ignore record request if in pre-sleep state
                if (HelmetClient.isSleepNow()) {
                    LogUtil.e("Ignore recording pre-sleep state.");
                    HelmetMessageSender.sendStartRecordResp(false, false);
                    break;
                }

                ServerHelmet.S2HStartVideo startVideo = s2HMessage.getStartVideo();
                boolean playRecordVoice = (Constant.PLAY_HINT_WHEN_RECORD == startVideo.getPlayVoice());
                VideoUtils.startRecordVideo(false, playRecordVoice);
                break;

            case S2H_MessageId_Stop_Video:
                VideoUtils.stopRecordVideo(false);
                ServerHelmet.S2HStopVideo stopVideoMsg = s2HMessage.getStopVideo();
                boolean stopVideoHint = (Constant.PLAY_HINT_WHEN_RECORD == stopVideoMsg.getPlayVoice());
                LogUtil.e("stop take video.........." + stopVideoHint);
                HelmetMessageSender.sendStopRecordResp(false, true);
                break;

            case S2H_MessageId_Start_Video_Live:
                // ignore live request if in pre-sleep state
                if (HelmetClient.isSleepNow()) {
                    LogUtil.e("Ignore living pre-sleep state.");
                    HelmetMessageSender.sendStartLiveResp(false);
                    break;
                }
                Log.e("Jerry_zhangcy", ">>>>>>>>>>>>>>>接收到后台开启直播指令<<<<<<<<<<<<<<<");
                ServerHelmet.S2HStartVideoLive startVideoLive = s2HMessage.getStartVideoLive();
                boolean useDefAddress = (startVideoLive.getType() == 1);
                String liveAddress = null;
                int frameBufferSize = NormalSendQueue.NORMAL_FRAME_BUFFER_SIZE;
                if (!useDefAddress) {
                    liveAddress = startVideoLive.getUrl();
                }
                //add by jerry for living video frame buffer size
                if(startVideoLive.hasFrameBufferSize()) {
                    frameBufferSize = startVideoLive.getFrameBufferSize();
                    if(frameBufferSize < NormalSendQueue.MIN_FRAME_BUFFER_SIZE) {
                        frameBufferSize = NormalSendQueue.NORMAL_FRAME_BUFFER_SIZE;
                    }
                }
                VideoUtils.startLiveStream(HelmetApplication.mAppContext, liveAddress,frameBufferSize);

                break;

            case S2H_MessageId_Stop_Video_Live:
                Log.e("Jerry_zhangcy", ">>>>>>>>>>>>>>>接收到后台停止直播指令<<<<<<<<<<<<<<<");
                Intent intent1 = new Intent(MainActivity.BROADCAST_LIVE);
                intent1.putExtra("status", HelmetApplication.mAppContext.getResources().getString(R.string.video_live_stop));
                HelmetApplication.mAppContext.sendBroadcast(intent1);
                VideoUtils.stopLiveStream(HelmetApplication.mAppContext);
                break;

            case S2H_MessageId_NetTypeChoose:
                if (s2HMessage.hasNetTypeChoose()) {
                    ServerHelmet.S2HNetTypeChoose choose = s2HMessage.getNetTypeChoose();
                    int useType = choose.getUseType();
                    NetworkUtil.setNetwork(useType);
                }
                break;

            default:
                LogUtil.e("Unknown msgId: " + s2HMessage.getMsgid());
                break;
        }
    }

    //add by Jerry beg
    public void dispatchMessage(final ServerHelmet.S2HHeartBeatResp s2HMessage) {
        if (s2HMessage == null) {
            return;
        }

        WorkThreadManager.executeOnDispatchThread(new Runnable() {
            @Override
            public void run() {
                handleMessage(s2HMessage);
            }
        });
    }

    public void handleMessage(ServerHelmet.S2HHeartBeatResp s2HMessage) {
        LogUtil.d("S2H= " + s2HMessage.toString());
        if (s2HMessage.hasGetFile()) {
            final ServerHelmet.S2HGetFile file = s2HMessage.getGetFile();
            final String fileName = file.getFileName();

            // ignore file upload request if in pre-sleep state
            if (HelmetClient.isSleepNow()) {
                LogUtil.e("Ignore file uploading during pre-sleep state.");
                HelmetMessageSender.sendUploadFileResp(fileName, Constant.FILE_UPLOAD_FAILED);
            } else {

                final int isSync = file.getIsSync();
                final int fileType = file.getType();

                //use dispatch url to upload file
                final String url = file.getUrlAddr();
                //get file upload speed limit
                int speedLimit = file.getSpeedLimit();
                speedLimit = (speedLimit <= 0 ? 0 : speedLimit);

                LocalFileManager.getInstance().uploadFile(url, fileName, fileType, speedLimit, isSync == Constant.UPLOAD_IS_SYNC);
            }
        }

        if (s2HMessage.hasListVideoFile()) {
            ServerHelmet.S2HListFile listFile = s2HMessage.getListVideoFile();
            final long startVideoTime = listFile.getStartTime();
            final long endVideoTime = listFile.getEndTime();
            uploadFileList(Constant.MEDIA_VIDEO, startVideoTime, endVideoTime);
        }

        if (s2HMessage.hasListPhotoFile()) {
            ServerHelmet.S2HListFile listPhotoFile = s2HMessage.getListPhotoFile();
            final long startPhotoTime = listPhotoFile.getStartTime();
            final long endPhotoTime = listPhotoFile.getEndTime();
            uploadFileList(Constant.MEDIA_PHOTO, startPhotoTime, endPhotoTime);
        }
    }

    //add by Jerry end


    private void deleteFile(final String fileName) {
        LogUtil.e("delete file>>>" + fileName);
        LocalFileManager.getInstance().deleteFileByName(fileName);
    }

    private void uploadFileList(final int fileType, final long startTime, final long endTime) {

        if (startTime > endTime) {
            return;
        }

        if (fileType != Constant.MEDIA_VIDEO && fileType != Constant.MEDIA_PHOTO) {
            return;
        }

        HelmetMessageSender.sendFileListResp(fileType, startTime, endTime);
    }
}
