package com.cy.helmet.config;

import android.text.TextUtils;

import com.cy.helmet.Constant;
import com.cy.helmet.HelmetApplication;
import com.cy.helmet.WorkThreadManager;
import com.cy.helmet.bluetooth.ble.HelmetBluetoothDataProcess;
import com.cy.helmet.bluetooth.ble.HelmetBluetoothManager;
import com.cy.helmet.core.protocol.Common;
import com.cy.helmet.location.LocationDataUtil;
import com.cy.helmet.networkstatus.NetWorkStatusUtil;
import com.cy.helmet.power.Externalpower;
import com.cy.helmet.sensor.GSensor;
import com.cy.helmet.util.DeviceStatusUtil;
import com.cy.helmet.storage.FileUtil;
import com.cy.helmet.util.LogUtil;
import com.cy.helmet.util.NetworkUtil;
import com.cy.helmet.util.StringUtil;
import com.cy.helmet.util.Util;
import com.cy.helmet.wifi.HelmetWifiManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

/**
 * Created by ubuntu on 18-1-3.
 */

public class HelmetConfig extends Observable {

//    message DeviceCfg
//    {
//        optional VoiceCfg voiceCfg = 1;//头盔上音频信息的配置
//        optional VideoCfg videoCfg = 2;//头盔上视频信息的配置
//        //业务服务器和GPS服务器配置，设备激活也是使用该消息，激活后业务服务器可能会重配，
//        //重配后设备需要重新注册到新的业务服务器
//        optional ServerCfg serverCfg = 3;
//        repeated WifiCfg wifiCfg = 4;//wifi配置,可能有两个，同serverCfg一样的场景定义
//        optional int32 openGps = 5;//gps开关，1=打开，0=关闭
//        optional int32 locationCfg = 6; //定位上传配置(上传频率单位秒)
//        optional int32 gyroscopeSwitch = 7;//螺旋仪开关, 1=打开,0=关闭
//        optional GyroscopeCfg gyroscopeCfg = 8;//陀螺仪跌倒检测阀值加速配置
//        optional int32 firstTfCard = 9;//1=优先nand，2=优先TF卡。配置保存优先TF卡还是nand flash
//        optional int32 gpsBusId = 10;//用于GPS上传的时候使用
//        optional string fotaServer = 11;//用于配置fota服务器的地址
//    }

    private static HelmetConfig mInstance;

    public static synchronized HelmetConfig get() {
        if (mInstance == null) {
            mInstance = new HelmetConfig();
        }
        return mInstance;
    }

    //deviceid
    private String mDeviceId = "unknown";

    private VoiceConfig voiceCfg = null;
    private VideoConfig videoCfg = null;
    private ServerConfig serverCfg = null;
    private List<WifiConfig> wifiCfgs = new ArrayList<>();
    private GyroscopeConfig gyroscopeCfg = null;

    public int openGps;
    public int locationCfg = 30;
    public int gyroscopeSwitch;
    public int firstTfCard = 2;//1-internal storage, 2-external storage
    public int gpsBusId;
    public String fotaServer;
    public int gpsCacheTime = 60;//gps cache time, minutes
    public int openPower = 0;//outer power 1 - open, 0 - close
    public int precision = GSensor.DEFAULT_GSENSOR_VALUE;//for fall value;
    public int mIsSleep = 1;//gps sleep or not 1->open 0->close default 1;
    public int mWifiCollectRate = 0;//wifi sampling frequency
    public int mwifiUploadRate = 0;//wifi upload frequency

    private HelmetConfig() {

        String privateConfigStr = FileUtil.readPrivateConfig();
        String publicConfigStr = FileUtil.readPublicConfig();

        JSONObject publicConfigJSON = null;
        JSONObject privateConfigJSON = null;

        LogUtil.e("privateConfig: " + privateConfigStr);
        LogUtil.e("publicConfig: " + publicConfigStr);

        if (publicConfigStr != null && !TextUtils.isEmpty(publicConfigStr.trim())) {
            try {
                publicConfigJSON = new JSONObject(publicConfigStr);
            } catch (JSONException e) {
                LogUtil.e(e);
                FileUtil.deletePublicConfig();
            }
        }

        if (privateConfigStr != null && !TextUtils.isEmpty(privateConfigStr.trim())) {
            try {
                privateConfigJSON = new JSONObject(privateConfigStr);
                JSONObject cloudGpsServer = privateConfigJSON.optJSONObject("cloud_gps_server");
                if (cloudGpsServer != null) {
                    if (serverCfg == null) {
                        serverCfg = new ServerConfig(cloudGpsServer);
                    } else {
                        serverCfg.update(cloudGpsServer);
                    }
                }
            } catch (JSONException e) {
                LogUtil.e(e);
            }
        }

        if (publicConfigJSON != null) {
            //server
            JSONObject server = publicConfigJSON.optJSONObject("server");
            if (server != null) {
                if (serverCfg == null) {
                    serverCfg = new ServerConfig(server);
                } else {
                    serverCfg.update(server);
                }
            }

            //wifi
            JSONArray array = publicConfigJSON.optJSONArray("wifi");
            if (array != null) {
                for (int index = 0; index < array.length(); index++) {
                    JSONObject json = array.optJSONObject(index);
                    if (json != null) {
                        String ssid = json.optString("ssid", "");
                        String passwd = json.optString("passwd", "");
                        wifiCfgs.add(new WifiConfig(ssid, passwd));
                    }
                }
            }

            firstTfCard = publicConfigJSON.optInt("tf_first", 1);
        }

        if (privateConfigJSON != null) {

            //voice
            JSONObject voice = privateConfigJSON.optJSONObject("voice");
            if (voice != null) {
                voiceCfg = new VoiceConfig(voice);
            }

            //video
            JSONObject video = privateConfigJSON.optJSONObject("video");
            if (video != null) {
                videoCfg = new VideoConfig(video);
            }

            //gyroscope
            JSONObject gyroscope = privateConfigJSON.optJSONObject("gyroscope");
            if (gyroscope != null) {
                gyroscopeCfg = new GyroscopeConfig(gyroscope);
            }

            openGps = privateConfigJSON.optInt("openGps", 1);
            locationCfg = privateConfigJSON.optInt("location_interval", 30);
            gpsCacheTime = privateConfigJSON.optInt("gps_cache_time", 60);
            openPower = privateConfigJSON.optInt("ex_power_switch", 0);
            gyroscopeSwitch = privateConfigJSON.optInt("gyroscope_switch", 1);
            //add by jerry
            mIsSleep = privateConfigJSON.optInt("gps_open_sleep", 1);
            mWifiCollectRate = privateConfigJSON.optInt("wifiCollectRate",0);
            mwifiUploadRate = privateConfigJSON.optInt("wifiUploadRate",0);

            gpsBusId = privateConfigJSON.optInt("gps_bus_id", 1);
            fotaServer = privateConfigJSON.optString("fota_server", "");

            if (gyroscopeCfg != null) {
                precision = gyroscopeCfg.getPrecision();
            }
        }
    }

    public void updateWIFIInfo() {
        synchronized (HelmetConfig.this) {
            Util.checkWIFIState(wifiCfgs);
        }
    }

    public void updateWifiConfig(//final BluetoothManager manager,
                                 final String ssid_24g,
                                 final String passwd_24g,
                                 final String ssid_5g,
                                 final String passwd_5g) {

        WorkThreadManager.executeOnConfigThread(new Runnable() {
            @Override
            public void run() {
                synchronized (HelmetConfig.this) {
                    List<WifiConfig> wifiList = new ArrayList<WifiConfig>();
                    if (!TextUtils.isEmpty(ssid_5g) && !TextUtils.isEmpty(passwd_5g)) {
                        wifiList.add(new WifiConfig(ssid_5g, passwd_5g));
                    } else {
                        wifiList.add(new WifiConfig("", ""));
                    }

                    if (!TextUtils.isEmpty(ssid_24g) && !TextUtils.isEmpty(passwd_24g)) {
                        wifiList.add(new WifiConfig(ssid_24g, passwd_24g));
                    } else {
                        wifiList.add(new WifiConfig("", ""));
                    }

                    int i = 0;
                    for (; i < wifiList.size() && i < wifiCfgs.size(); i++) {
                        WifiConfig newWifi = wifiList.get(i);
                        if (!newWifi.isEmpty()) {
                            wifiCfgs.set(i, newWifi);
                        }
                    }

                    for (; i < wifiList.size(); i++) {
                        WifiConfig newWifi = wifiList.get(i);
                        wifiCfgs.add(i, newWifi);
                    }

                    savePublicConfig();
                    Util.checkWIFIState(wifiCfgs);

                    //manager.sendMessage("{\"msg_id\":11,\"code\":0}");
                    String msg = "{\"msg_id\":11,\"code\":0}";
                    HelmetBluetoothDataProcess.sendMsgToRemote(msg);
                }
            }
        });
    }

    private void onWebConfigChanged(ServerConfig server) {
        if (server != null) {
            LogUtil.e("web server changed......");
            setChanged();
            notifyObservers(server.clone());
        }
    }

    private void onGyroscopeConfigChanged(GyroscopeConfig gyroscope) {
        if (gyroscope != null) {
            LogUtil.e("gyroscope precision changed: " + gyroscope.precision);
            GSensor.serviceWriteGsensor(String.valueOf(gyroscope.precision));
        }
    }

    private void onGPSConfigChanged(String gpsHost, int gpsPort, int interval) {

        if (TextUtils.isEmpty(gpsHost) || gpsPort <= 0 && interval <= 0) {
            LogUtil.e("illegal gps configuration..." + gpsHost + ", " + gpsPort + ", " + interval);
            return;
        }

        LogUtil.e("gps config changed......");

        LogUtil.e("gpsChanged_host: " + gpsHost);
        LogUtil.e("gpsChanged_port: " + gpsPort);
        LogUtil.e("gpsChanged_inter: " + interval);
    }

    public void updateServerConfig(//final BluetoothManager manager,
                                   final String serveHost,
                                   final int serverPort,
                                   final String gpsServerHost,
                                   final int gpsServerPort) {

        WorkThreadManager.executeOnConfigThread(new Runnable() {
            @Override
            public void run() {
                synchronized (HelmetConfig.this) {
                    boolean webServerChanged = false;
                    boolean gpsServerChanged = false;

                    if (serverCfg == null) {
                        webServerChanged = true;
                        gpsServerChanged = true;
                        serverCfg = new ServerConfig(serveHost, serverPort, gpsServerHost, gpsServerPort);
                    } else {
                        byte changeType = serverCfg.update(serveHost, serverPort, gpsServerHost, gpsServerPort);
                        webServerChanged = ServerConfig.isWebServerChanged(changeType);
                        gpsServerChanged = ServerConfig.isGPSServerChanged(changeType);
                    }

                    // notify web server changed
                    if (webServerChanged) {
                        onWebConfigChanged(serverCfg);
                    }

                    // notify web server changed
                    if (gpsServerChanged) {
                        onGPSConfigChanged(serverCfg.gpsServerHost, serverCfg.gpsServerPort, locationCfg);
                    }

                    //manager.sendMessage("{\"msg_id\":21,\"code\":0}");
                    String msg = "{\"msg_id\":21,\"code\":0}";
                    HelmetBluetoothDataProcess.sendMsgToRemote(msg);
                }
            }
        });
    }

    public void updateStorageConfig(//BluetoothManager manager,
                                    final boolean isTfFirst) {
        if (isTfFirst) {
            firstTfCard = 2;
        } else {
            firstTfCard = 1;
        }

        //if (manager != null) {
        //manager.sendMessage("{\"msg_id\":31,\"code\":0}");
        // }

        String msg = "{\"msg_id\":31,\"code\":0}";
        HelmetBluetoothDataProcess.sendMsgToRemote(msg);

    }

    public void updateNetworkConfig(//BluetoothManager manager,
                                    final int type) {
        switch (type) {
            case HelmetBluetoothManager.MOBILE_ONLY:
                NetworkUtil.mCurrentNetType = Constant.NET_CHOOSE_FORCE_MOBILE;
                NetWorkStatusUtil.enableWifi(false);
                NetWorkStatusUtil.enableMobileData(true);
                break;
            case HelmetBluetoothManager.WIFI_ONLY:
                NetworkUtil.mCurrentNetType = Constant.NET_CHOOSE_FORCE_WIFI;
                NetWorkStatusUtil.enableWifi(true);
                NetWorkStatusUtil.enableMobileData(false);
                break;
            case HelmetBluetoothManager.MOBILE_WIFI:
                NetworkUtil.mCurrentNetType = Constant.NET_CHOOSE_WIFI_FIRST;
                NetWorkStatusUtil.enableWifi(true);
                NetWorkStatusUtil.enableMobileData(true);
                break;
        }
        //if (manager != null) {
        //   manager.sendMessage("{\"msg_id\":61,\"code\":0}");
        //}

        String msg = "{\"msg_id\":61,\"code\":0}";
        HelmetBluetoothDataProcess.sendMsgToRemote(msg);

    }

    public synchronized boolean updateActiveInfo(JSONObject json) {
        LogUtil.e("txhlog updateActiveInfo=" + json.toString());
        if (json == null) {
            return false;
        }

        String webHost = json.optString("webServerHost", "");
        int webPort = json.optInt("webServerPort", -1);
        if (TextUtils.isEmpty(webHost) || webPort < 0) {
            return false;
        }

        String gpsHost = json.optString("gpsServerHost", "");
        int gpsPort = json.optInt("gpsServerPort", -1);
        if (TextUtils.isEmpty(gpsHost) || gpsPort < 0) {
            return false;
        }

        serverCfg = new ServerConfig(webHost, webPort, gpsHost, gpsPort);

        // notify gps server changed
        onGPSConfigChanged(gpsHost, gpsPort, locationCfg);

        String wifiSsid = json.optString("wifiSsid", "");
        String wifiPasswd = json.optString("wifiPassword", "");
        if (!TextUtils.isEmpty(wifiSsid) && !TextUtils.isEmpty(wifiPasswd)) {
            wifiCfgs.clear();
            wifiCfgs.add(new WifiConfig(wifiSsid, wifiPasswd));
        }

        String wifiSsid_5G = json.optString("wifiSsid5G", "");
        String wifiPasswd_5G = json.optString("wifiPassword5G", "");
        if (!TextUtils.isEmpty(wifiSsid_5G) && !TextUtils.isEmpty(wifiPasswd_5G)) {
            wifiCfgs.add(new WifiConfig(wifiSsid_5G, wifiPasswd_5G));
        }

        LogUtil.e("txhlog 11111111wifiCfgs=" + wifiCfgs.toString());
        Util.checkWIFIState(wifiCfgs);
        savePublicConfig();
        return true;
    }

    public synchronized void updateAll(Common.DeviceCfg cfg) {

        if (cfg == null) {
            return;
        }

        try {
            if (cfg.hasVoiceCfg()) {
                if (voiceCfg == null) {
                    voiceCfg = new VoiceConfig(cfg.getVoiceCfg());
                } else {
                    voiceCfg.update(cfg.getVoiceCfg());
                }
            }

            if (cfg.hasVideoCfg()) {
                if (videoCfg == null) {
                    videoCfg = new VideoConfig(cfg.getVideoCfg());
                } else {
                    boolean changed = videoCfg.update(cfg.getVideoCfg());
                    if (changed) {
                        LogUtil.e("video coderate changed>>>>>>>>>>>>>>>>>>>>>");
                        setChanged();
                        notifyObservers(videoCfg);
                    }
                }
            }

            boolean gpsConfigChange = false;
            if (cfg.hasLocationCfg()) {
                int newInterval = cfg.getLocationCfg();
                if ((newInterval > 0) && (newInterval != locationCfg)) {
                    locationCfg = newInterval;
                    gpsConfigChange = true;
                }
            }

            if (cfg.hasServerCfg()) {
                if (serverCfg == null) {
                    serverCfg = new ServerConfig(cfg.getServerCfg());
                } else {
                    byte changeType = serverCfg.update(cfg.getServerCfg());
                    boolean webServerChange = ServerConfig.isWebServerChanged(changeType);
                    gpsConfigChange = ServerConfig.isGPSServerChanged(changeType);
                    if (webServerChange) {
                        onWebConfigChanged(serverCfg);
                    }
                }
            }

            if (gpsConfigChange) {
                onGPSConfigChanged(serverCfg.gpsServerHost, serverCfg.gpsServerPort, locationCfg);
            }

            List<Common.WifiCfg> wifiCfgList = cfg.getWifiCfgList();
            if (wifiCfgList != null && !wifiCfgList.isEmpty()) {
                wifiCfgs.clear();
                for (Common.WifiCfg wifiCfg : cfg.getWifiCfgList()) {
                    if (wifiCfg != null) {
                        WifiConfig tmp = new WifiConfig(wifiCfg.getWifiSsid(), wifiCfg.getPasswd());
                        wifiCfgs.add(tmp);
                    }
                }
            }

            if (cfg.hasGyroscopeCfg()) {
                if (gyroscopeCfg == null) {
                    gyroscopeCfg = new GyroscopeConfig(cfg.getGyroscopeCfg());
                    onGyroscopeConfigChanged(gyroscopeCfg);
                } else {
                    if (gyroscopeCfg.update(cfg.getGyroscopeCfg())) {
                        onGyroscopeConfigChanged(gyroscopeCfg);
                    }
                }
            }

            if (cfg.hasOpenGps()) {
                openGps = cfg.getOpenGps();// 1 open 0 close
                //add by Jerry
                LocationDataUtil.getInstance().switchGPSState(openGps == 1);
            }


            if (cfg.hasGyroscopeSwitch()) {
                gyroscopeSwitch = cfg.getGyroscopeSwitch();
            }

            if (cfg.hasFirstTfCard()) {
                firstTfCard = cfg.getFirstTfCard();
            }

            if (cfg.hasGpsBusId()) {
                gpsBusId = cfg.getGpsBusId();
            }

            if (cfg.hasFotaServer()) {
                fotaServer = cfg.getFotaServer();
            }

            if (cfg.hasGpsCacheTime()) {
                gpsCacheTime = cfg.getGpsCacheTime();
            }

            if (cfg.hasOpenPower()) {
                int exPower = cfg.getOpenPower();
                if (exPower != openPower) {
                    //TODO notify openPower changed
                    Externalpower.serviceWriteExternalpower(exPower);//add by Jerry
                }
                openPower = exPower;
            }

            //add by Jerry
            if (cfg.hasOpenSleep()) {
                mIsSleep = cfg.getOpenSleep();
            }

            if(cfg.hasWifiCollectRate()) {
                mWifiCollectRate = cfg.getWifiCollectRate();
            }

            if(cfg.hasWifiUploadRate()) {
                mwifiUploadRate = cfg.getWifiUploadRate();
            }

            if(mWifiCollectRate != 0 && mwifiUploadRate != 0) {
                HelmetWifiManager.getInstance().startDataCollectionAlarm(true);
                HelmetWifiManager.getInstance().startDataSendNextAlarm(true);
            }

            if (cfg.hasName()) {
                String helmetName = cfg.getName();
                // TODO notify helmet name
                HelmetBluetoothManager.getInstance().setHelmetName(helmetName);
            }

            LogUtil.e("txhlog 22222wifiCfgs=" + wifiCfgs.toString());
            Util.checkWIFIState(wifiCfgs);
            savePublicConfig();
            savePrivateConfig();
        } catch (Exception e) {
            LogUtil.e(e);
        }
    }

    public synchronized Common.DeviceCfg toDeviceCfg() {

        Common.DeviceCfg.Builder builder = Common.DeviceCfg.newBuilder();

        if (voiceCfg != null) {
            builder.setVoiceCfg(voiceCfg.toVoiceCfg());
        }

        if (videoCfg != null) {
            builder.setVideoCfg(videoCfg.toVideoCfg());
        }

        if (wifiCfgs != null && !wifiCfgs.isEmpty()) {
            for (WifiConfig config : wifiCfgs) {
                builder.addWifiCfg(config.toWifiCfg());
            }
        }

        if (gyroscopeCfg != null) {
            builder.setGyroscopeCfg(gyroscopeCfg.toGyroscopeCfg());
        }

        builder.setOpenGps(openGps);
        builder.setLocationCfg(locationCfg);
        builder.setGpsCacheTime(gpsCacheTime);
        builder.setOpenPower(openPower);
        builder.setGyroscopeSwitch(gyroscopeSwitch);
        builder.setFirstTfCard(firstTfCard);
        builder.setGpsBusId(gpsBusId);
        //add by Jerry
        builder.setOpenSleep(mIsSleep);
        builder.setWifiCollectRate(mWifiCollectRate);
        builder.setWifiUploadRate(mwifiUploadRate);

        if (fotaServer != null) {
            builder.setFotaServer(fotaServer);
        }

        return builder.build();
    }

    private void savePublicConfig() {
        JSONObject json = new JSONObject();
        if (serverCfg != null) {
            try {
                JSONObject server = serverCfg.toJSON();
                json.put("server", server);
            } catch (Exception e) {
            }
        }

        if (wifiCfgs != null && !wifiCfgs.isEmpty()) {
            try {
                JSONArray array = new JSONArray();
                for (WifiConfig config : wifiCfgs) {
                    array.put(config.toJSON());
                }
                json.put("wifi", array);
            } catch (JSONException e) {
            }
        }

        try {
            json.put("tf_first", firstTfCard);
        } catch (JSONException e) {
        }

        FileUtil.savePublicConfig(json.toString());
    }

    private void savePrivateConfig() {
        JSONObject json = new JSONObject();

        if (serverCfg != null) {
            try {
                json.put("cloud_gps_server", serverCfg.toJSONCloudGpsServer());
            } catch (JSONException e) {
                LogUtil.e(e);
            }
        }

        if (voiceCfg != null) {
            try {
                json.put("voice", voiceCfg.toJSON());
            } catch (JSONException e) {
                LogUtil.e(e);
            }
        }

        if (videoCfg != null) {
            try {
                json.put("video", videoCfg.toJSON());
            } catch (JSONException e) {
                LogUtil.e(e);
            }
        }

        if (gyroscopeCfg != null) {
            try {
                json.put("gyroscope", gyroscopeCfg.toJSON());
            } catch (JSONException e) {
                LogUtil.e(e);
            }
        }

        try {
            json.put("open_gps", openGps);
        } catch (JSONException e) {
            LogUtil.e(e);
        }

        try {
            json.put("location_interval", locationCfg);
        } catch (JSONException e) {
            LogUtil.e(e);
        }

        try {
            json.put("gps_cache_time", gpsCacheTime);
        } catch (JSONException e) {
            LogUtil.e(e);
        }

        try {
            json.put("ex_power_switch", openPower);
        } catch (JSONException e) {
            LogUtil.e(e);
        }

        try {
            json.put("gyroscope_switch", gyroscopeSwitch);
        } catch (JSONException e) {

        }

        try {
            json.put("gpsbus_id", gpsBusId);
        } catch (JSONException e) {

        }

        try {
            json.put("fota_server", fotaServer);
        } catch (JSONException e) {

        }

        //add by Jerry
        try {
            json.put("gps_open_sleep", mIsSleep);
        } catch (JSONException e) {
            LogUtil.e(e);
        }

        try {
            json.put("wifiCollectRate", mWifiCollectRate);
        } catch (JSONException e) {
            LogUtil.e(e);
        }

        try {
            json.put("wifiUploadRate", mwifiUploadRate);
        } catch (JSONException e) {
            LogUtil.e(e);
        }

        FileUtil.savePrivateConfig(json.toString());
    }


    // fetch parameter method
    public String getDeviceId() {
        if ("unknown".equals(mDeviceId)) {
            // re-get 'IMEI' if is initial value
            mDeviceId = DeviceStatusUtil.getImei(HelmetApplication.mAppContext);
        }
        LogUtil.e("txhlog deviceId=" + mDeviceId);

        return mDeviceId;
    }

    public String getSOSServerUrl(String deviceId) {
        if (StringUtil.isNullEmptyOrSpace(deviceId)) {
            return "";
        }

        String serverHost;
        if (NetworkUtil.isWifiNetwork()) {
            serverHost = getGpsServerHost();
        } else {
            serverHost = getCloudServerHost();
        }

        if (!StringUtil.isNullEmptyOrSpace(serverHost)) {
            StringBuilder builder = new StringBuilder();
            builder.append("http://");
            builder.append(serverHost);
            builder.append(Constant.SOS_SERVER_URL_PATH);
            builder.append(deviceId);
            return builder.toString();
        }

        return "";
    }

    public String getWebServerHost() {
        if (serverCfg != null) {
            return serverCfg.webServerHost;
        }

        return "";
    }

    public int getWebServerPort() {
        if (serverCfg != null) {
            return serverCfg.webServerPort;
        }
        return -1;
    }

    public String getGpsServerHost() {
        if (serverCfg != null) {
            return serverCfg.gpsServerHost;
        }
        return "";
    }

    public int getGpsServerPort() {
        if (serverCfg != null) {
            return serverCfg.gpsServerPort;
        }
        return -1;
    }

    public String getCloudServerHost() {
        if (serverCfg != null) {
            return serverCfg.cloudGpsServerHost;
        }
        return "";
    }

    public int getCloudServerPort() {
        if (serverCfg != null) {
            return serverCfg.cloudGpsServerPort;
        }
        return -1;
    }

    public int getMaxRecordFileLength() {
        if (videoCfg == null || videoCfg.fileLength == 0) {
            return 32;
        }

        return videoCfg.fileLength;
    }

    public int[] getRecordResolution() {
        if (videoCfg == null || videoCfg.videoCfgDetail == null) {
            return getResolution(480);
        }

        return getResolution(videoCfg.videoCfgDetail.resolution);
    }

    public int getRecordCodeRate() {
        if (videoCfg == null || videoCfg.videoCfgDetail == null) {
            return 1500;
        }

        return videoCfg.videoCfgDetail.codeRate;
    }

    public int[] getLiveResolution() {
        if (videoCfg == null || videoCfg.liveVideoCfgDetail == null) {
            return getResolution(480);
        }

        return getResolution(videoCfg.liveVideoCfgDetail.resolution);
    }

    public int getLiveCodeRate() {
        if (videoCfg == null || videoCfg.liveVideoCfgDetail == null) {
            return 1500;
        }

        return videoCfg.liveVideoCfgDetail.codeRate;
    }

    public int getGpsBusId() {
        return gpsBusId;
    }

    private int[] getResolution(int liveResolution) {
        int[] resolution = new int[2];

        switch (liveResolution) {
            case 320:
                resolution[0] = 480;
                resolution[1] = 320;
                break;
            case 480:
                resolution[0] = 640;
                resolution[1] = 480;
                break;
            case 720:
                resolution[0] = 1280;
                resolution[1] = 720;
                break;
            case 1080:
                resolution[0] = 1920;
                resolution[1] = 1080;
                break;

            default:
                resolution[0] = 640;
                resolution[1] = 480;
                break;
        }
        return resolution;
    }

    public boolean isActivated() {
        LogUtil.e("isActive>>>" + (serverCfg != null && serverCfg.isActivated()));
        return serverCfg != null && serverCfg.isActivated();
    }
}
