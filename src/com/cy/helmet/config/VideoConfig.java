package com.cy.helmet.config;

import com.cy.helmet.core.protocol.Common;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ubuntu on 18-1-3.
 */

public class VideoConfig {

    public int fileLength;
    public VideoCfgDetail videoCfgDetail;
    public VideoCfgDetail liveVideoCfgDetail;

    public VideoConfig(JSONObject json) {
        if (json != null) {
            try {
                fileLength = json.getInt("maxVideoSize");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            JSONObject recordDetail = null;
            JSONObject liveDetail = null;

            try {
                recordDetail = json.getJSONObject("recordDetail");
                liveDetail = json.getJSONObject("liveDetail");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            videoCfgDetail = new VideoCfgDetail(recordDetail);
            liveVideoCfgDetail = new VideoCfgDetail(liveDetail);
        }
    }

    public VideoConfig(Common.VideoCfg cfg) {
        update(cfg);
    }

    public boolean update(Common.VideoCfg cfg) {
        boolean codeRateChanged = false;
        fileLength = cfg.getFileLength();
        if (cfg.hasVideoCfgDetail()) {
            if (videoCfgDetail == null) {
                videoCfgDetail = new VideoCfgDetail(cfg.getVideoCfgDetail());
            } else {
                boolean videoCodeRateChanged = videoCfgDetail.update(cfg.getVideoCfgDetail());
                codeRateChanged = codeRateChanged || videoCodeRateChanged;
            }
        }

        if (cfg.hasLiveVideoCfgDetail()) {
            if (liveVideoCfgDetail == null) {
                liveVideoCfgDetail = new VideoCfgDetail(cfg.getLiveVideoCfgDetail());
            } else {
                boolean videoCodeRateChanged = liveVideoCfgDetail.update(cfg.getLiveVideoCfgDetail());
                codeRateChanged = codeRateChanged || videoCodeRateChanged;
            }
        }

        return codeRateChanged;
    }

    @Override
    public String toString() {
        return "VideoCfg{" +
                "fileLength=" + fileLength +
                ", videoCfgDetail=" + videoCfgDetail +
                ", liveVideoCfgDetail=" + liveVideoCfgDetail +
                '}';
    }

    public Common.VideoCfg toVideoCfg() {
        Common.VideoCfg.Builder builder = Common.VideoCfg.newBuilder();
        builder.setFileLength(fileLength);

        if (videoCfgDetail != null) {
            builder.setVideoCfgDetail(videoCfgDetail.toVideoCfgDetail());
        }

        if (videoCfgDetail != null) {
            builder.setLiveVideoCfgDetail(liveVideoCfgDetail.toVideoCfgDetail());
        }

        return builder.build();
    }

    public JSONObject toJSON() {

        JSONObject json = new JSONObject();

        try {
            json.put("maxVideoSize", fileLength);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (videoCfgDetail != null) {
            try {
                json.put("recordDetail", videoCfgDetail.toJSON());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (liveVideoCfgDetail != null) {
            try {
                json.put("liveDetail", liveVideoCfgDetail.toJSON());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return json;
    }
}
