package com.cy.helmet.config;

import com.cy.helmet.core.protocol.Common;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by jiaqing on 2018/1/13.
 */

public class VideoCfgDetail {

    public int resolution;
    public int codeRate;

    public VideoCfgDetail(JSONObject json) {
        if (json == null) {
            throw new RuntimeException("JSONObject in 'VideoCfgDetail' constructor is null!");
        }
        if (json != null) {
            resolution = json.optInt("resolution", 720);
            codeRate = json.optInt("codeRate", 1500);
        }
    }

    public VideoCfgDetail(Common.VideoCfg.VideoCfgDetail cfg) {
        update(cfg);
    }

    public boolean update(Common.VideoCfg.VideoCfgDetail cfg) {
        boolean changed = (codeRate != cfg.getCodeRate());
        resolution = cfg.getResolution();
        codeRate = cfg.getCodeRate();
        return changed;
    }

    @Override
    public String toString() {
        return "VideoCfgDetail{" +
                "resolution=" + resolution +
                ", codeRate=" + codeRate +
                '}';
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("resolution", resolution);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            json.put("codeRate", codeRate);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json;
    }

    public Common.VideoCfg.VideoCfgDetail toVideoCfgDetail() {
        Common.VideoCfg.VideoCfgDetail.Builder builder = Common.VideoCfg.VideoCfgDetail.newBuilder();
        builder.setResolution(resolution);
        builder.setCodeRate(codeRate);
        return builder.build();
    }
}
