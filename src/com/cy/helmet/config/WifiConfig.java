package com.cy.helmet.config;

import android.text.TextUtils;

import com.cy.helmet.core.protocol.Common;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ubuntu on 18-1-3.
 */

public class WifiConfig {

//    //wifi信息，用于下发wifi配置
//    message WifiCfg
//    {
//        required string wifiSsid = 1;//wifi ssid
//        required string passwd = 2;//wifi密码
//    }

    public String mWifiSsid;
    public String mWifiPasswd;

    public WifiConfig(String ssid, String passwd) {
        mWifiSsid = ssid;
        mWifiPasswd = passwd;
    }

    public void update(Common.WifiCfg wifiCfg) {
        mWifiSsid = wifiCfg.getWifiSsid();
        mWifiPasswd = wifiCfg.getPasswd();
    }

    @Override
    public String toString() {
        return "WifiCfg{" +
                "wifiSsid='" + mWifiSsid + '\'' +
                ", passwd='" + mWifiPasswd + '\'' +
                '}';
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("ssid", mWifiSsid);
            json.put("passwd", mWifiPasswd);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json;
    }

    public Common.WifiCfg toWifiCfg() {

        Common.WifiCfg.Builder builder = Common.WifiCfg.newBuilder();
        if (mWifiSsid != null) {
            builder.setWifiSsid(mWifiSsid);
        }
        if (mWifiPasswd != null) {
            builder.setPasswd(mWifiPasswd);
        }

        return builder.build();
    }

    public boolean isEmpty() {
        return TextUtils.isEmpty(mWifiSsid) || TextUtils.isEmpty(mWifiPasswd);
    }
}
