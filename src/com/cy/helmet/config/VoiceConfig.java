package com.cy.helmet.config;

import com.cy.helmet.core.protocol.Common;
import com.cy.helmet.util.LogUtil;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ubuntu on 18-1-3.
 */

public class VoiceConfig {

//    语音配置数据，帽子向服务器上传或者服务器向帽子下发
//    message VoiceCfg
//    {
//        optional int32 talkTimeSec = 1;//头盔上用户对讲的持续时间,单位秒
//    }

    public int talkTimeSec = 15;

    public VoiceConfig(JSONObject json) {
        if (json != null) {
            talkTimeSec = json.optInt("talkTime", 15);
        }
    }

    public VoiceConfig(Common.VoiceCfg cfg) {
        update(cfg);
    }

    public void update(Common.VoiceCfg cfg) {
        LogUtil.e((Object) cfg.getTalkTimeSec() + "");
        if (cfg.hasTalkTimeSec()) {
            talkTimeSec = cfg.getTalkTimeSec();
        }
    }

    @Override
    public String toString() {
        return "VoiceCfg{" +
                "talkTimeSec=" + talkTimeSec +
                '}';
    }

    public Common.VoiceCfg toVoiceCfg() {
        Common.VoiceCfg.Builder builder = Common.VoiceCfg.newBuilder();
        builder.setTalkTimeSec(talkTimeSec);
        return builder.build();
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("talkTimeSec", talkTimeSec);
        } catch (JSONException e) {
        }

        return json;
    }
}
