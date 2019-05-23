package com.cy.helmet.config;

import android.text.TextUtils;

import com.cy.helmet.core.protocol.Common;
import com.cy.helmet.util.Util;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ubuntu on 18-1-3.
 */

public class ServerConfig {
//    required string webServerHost = 1;//web服务器地址
//    required int32 webServerPort = 2;//web服务器端口
//    required string gpsServerHost = 3;//gps服务器地址
//    required int32 gpsServerPort = 4;//gps服务器端口

    public static final byte NO_SERVER_ADDRESS_CHANGED = 0x00;
    public static final byte WEB_SERVER_ADDRESS_CHANGED = 0x01;
    public static final byte GPS_SERVER_ADDRESS_CHANGED = 0x02;

    public String webServerHost = null;
    public int webServerPort = -1;
    public String gpsServerHost = null;
    public int gpsServerPort = -1;
    public String cloudGpsServerHost = null;
    public int cloudGpsServerPort = -1;

    public ServerConfig(JSONObject json) {
        update(json);
    }

    public ServerConfig(String webHost,
                        int webPort,
                        String gpsHost,
                        int gpsPort) {
        webServerHost = webHost;
        webServerPort = webPort;
        gpsServerHost = gpsHost;
        gpsServerPort = gpsPort;
    }

    public ServerConfig(Common.ServerCfg serverCfg) {
        update(serverCfg);
    }

    public void update(JSONObject json) {
        if (json == null) {
            return;
        }

        if (json.has("webhost") && json.has("webport")) {
            webServerHost = json.optString("webhost", webServerHost);
            webServerPort = json.optInt("webport", webServerPort);
        }

        if (json.has("gpshost") && json.has("gpsport")) {
            gpsServerHost = json.optString("gpshost", gpsServerHost);
            gpsServerPort = json.optInt("gpsport", gpsServerPort);
        }

        if (json.has("cloud_gps_host") && json.has("cloud_gps_port")) {
            cloudGpsServerHost = json.optString("cloud_gps_host", cloudGpsServerHost);
            cloudGpsServerPort = json.optInt("cloud_gps_port", cloudGpsServerPort);
        }
    }

    public byte update(Common.ServerCfg serverCfg) {

        byte changType = NO_SERVER_ADDRESS_CHANGED;

        String newWebHost = serverCfg.getWebServerHost();
        int newWebPort = serverCfg.getWebServerPort();
        if (!TextUtils.isEmpty(newWebHost) && newWebPort > 0) {
            if (!newWebHost.equals(webServerHost) || webServerPort != newWebPort) {
                webServerHost = newWebHost;
                webServerPort = newWebPort;
                changType |= WEB_SERVER_ADDRESS_CHANGED;
            }
        }

        String newGpsHost = serverCfg.getGpsServerHost();
        int newGpsPort = serverCfg.getGpsServerPort();
        if (!TextUtils.isEmpty(newGpsHost) && newGpsPort > 0) {
            if (!newGpsHost.equals(gpsServerHost) || gpsServerPort != newGpsPort) {
                gpsServerHost = newGpsHost;
                gpsServerPort = newGpsPort;
                changType |= GPS_SERVER_ADDRESS_CHANGED;
            }
        }

        String newCloudGpsHost = serverCfg.getCloubGpsServerHost();
        int newCloudGpsPort = serverCfg.getCloubgpsServerPort();
        if (!TextUtils.isEmpty(newCloudGpsHost) && newCloudGpsPort > 0) {
            cloudGpsServerHost = newCloudGpsHost;
            cloudGpsServerPort = newCloudGpsPort;
        }

        return changType;
    }

    public byte update(String serverHost, int serverPort, String gpsHost, int gpsPort) {
        byte changeType = NO_SERVER_ADDRESS_CHANGED;

        if (!TextUtils.isEmpty(serverHost) && serverPort > 0) {
            if (!serverHost.equals(webServerHost) || serverPort != webServerPort) {
                webServerHost = serverHost;
                webServerPort = serverPort;
                changeType |= WEB_SERVER_ADDRESS_CHANGED;
            }
        }

        if (!TextUtils.isEmpty(gpsHost) && gpsPort > 0) {
            if (!gpsHost.equals(gpsServerHost) || (gpsPort != gpsServerPort)) {
                gpsServerHost = gpsHost;
                gpsServerPort = gpsPort;
                changeType |= GPS_SERVER_ADDRESS_CHANGED;
            }
        }

        return changeType;
    }

    public Common.ServerCfg toServerCfg() {

        Common.ServerCfg.Builder builder = Common.ServerCfg.newBuilder();
        if (webServerHost != null) {
            builder.setWebServerHost(webServerHost);
        }
        if (gpsServerHost != null) {
            builder.setGpsServerHost(gpsServerHost);
        }
        builder.setGpsServerPort(gpsServerPort);
        builder.setWebServerPort(webServerPort);

        return builder.build();
    }

    @Override
    public String toString() {
        return "ServerCfg{" +
                "webServerHost='" + webServerHost + '\'' +
                ", webServerPort=" + webServerPort +
                ", gpsServerHost='" + gpsServerHost + '\'' +
                ", gpsServerPort=" + gpsServerPort +
                '}';
    }

    public JSONObject toJSON() {

        JSONObject json = new JSONObject();

        if (!TextUtils.isEmpty(webServerHost) && webServerPort > 0) {
            try {
                json.put("webhost", webServerHost);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            try {
                json.put("webport", webServerPort);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (!TextUtils.isEmpty(gpsServerHost) && gpsServerPort > 0) {
            try {
                json.put("gpshost", gpsServerHost);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            try {
                json.put("gpsport", gpsServerPort);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return json;
    }

    public JSONObject toJSONCloudGpsServer() {
        JSONObject json = new JSONObject();
        if (!TextUtils.isEmpty(cloudGpsServerHost) && cloudGpsServerPort > 0) {
            try {
                json.put("cloud_gps_host", cloudGpsServerHost);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            try {
                json.put("cloud_gps_port", cloudGpsServerPort);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return json;
    }

    public boolean isActivated() {
        return !TextUtils.isEmpty(webServerHost) && (webServerPort > 0);
    }

    @Override
    protected ServerConfig clone() {
        return new ServerConfig(webServerHost, webServerPort, gpsServerHost, gpsServerPort);
    }

    public static boolean isWebServerChanged(byte changeType) {
        return (changeType & WEB_SERVER_ADDRESS_CHANGED) != 0;
    }

    public static boolean isGPSServerChanged(byte changeType) {
        return (changeType & GPS_SERVER_ADDRESS_CHANGED) != 0;
    }
}
