package com.cy.helmet.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.cy.helmet.Constant;
import com.cy.helmet.HelmetApplication;
import com.cy.helmet.config.WifiConfig;
import com.cy.helmet.core.protocol.HelmetServer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by yaojiaqing on 2017/12/28.
 */

public class Util {

    public static byte[] wrapSendMessage(HelmetServer.H2SMessage message) {
        if (message != null) {
            byte[] protoByteArray = message.toByteArray();
            int protoLen = protoByteArray.length;
            byte[] buffer = new byte[protoLen + 5];
            byte[] lengthByteArray = intToByteArray(protoByteArray.length);
            ByteBuffer.wrap(buffer).put((byte) 0x50).put(lengthByteArray).put(protoByteArray);

            return buffer;
        }
        return null;
    }

    public static byte[] intToByteArray(int intData) {
        return new byte[]{
                (byte) (intData & 0xFF),
                (byte) ((intData >> 8) & 0xFF),
                (byte) ((intData >> 16) & 0xFF),
                (byte) ((intData >> 24) & 0xFF),
        };
    }

    public static int byteArrayToInt(byte[] bytes) {
        return (bytes[0] & 0xFF) |
                (bytes[1] & 0xFF) << 8 |
                (bytes[2] & 0xFF) << 16 |
                (bytes[3] & 0xFF) << 24;
    }

    public static byte[] readStreamFromFile(File file) {
        if (file != null && file.exists()) {
            byte[] buffer = null;
            FileInputStream fs = null;
            ByteArrayOutputStream bao = null;
            try {
                long fileLen = file.length();
                if (fileLen <= Constant.MAX_SEND_PROTO_SIZE) {
                    fs = new FileInputStream(file);
                    bao = new ByteArrayOutputStream();
                    buffer = new byte[1024];
                    int len = 0;
                    while (-1 != (len = fs.read(buffer))) {
                        bao.write(buffer, 0, len);
                    }

                    return bao.toByteArray();
                }
            } catch (Exception e) {
                LogUtil.e(e);
            } finally {
                if (fs != null) {
                    try {
                        fs.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (bao != null) {
                    try {
                        bao.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }

    public static final String getFormatTime(long timeStamp) {
        Date date = new Date(timeStamp);
        SimpleDateFormat formater = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        return formater.format(date);
    }

    public static final String getFormatDate(long timeStamp) {
        Date date = new Date(timeStamp);
        SimpleDateFormat formater = new SimpleDateFormat("yyyyMMdd");
        return formater.format(date);
    }

    public static final String byte2hex(byte b[]) {
        if (b == null) {
            throw new IllegalArgumentException(
                    "Argument b ( byte array ) is null! ");
        }
        String hs = "";
        String stmp = "";
        for (int n = 0; n < b.length; n++) {
            stmp = Integer.toHexString(b[n] & 0xff);
            if (stmp.length() == 1) {
                hs = hs + "0" + stmp;
            } else {
                hs = hs + stmp;
            }
        }
        return hs.toUpperCase();
    }

    public static boolean isSystemApp() {
        PackageManager packageManager = null;
        ApplicationInfo applicationInfo = null;

        try {
            packageManager = HelmetApplication.mAppContext.getPackageManager();
            applicationInfo = packageManager.getApplicationInfo(HelmetApplication.mAppContext.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException var4) {
            applicationInfo = null;
        }
        if (applicationInfo != null) {
            if ((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0) {
                return true;
            }
        }
        return false;
    }

    public static void setWIFIInfo(Context context, String info) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("SMARTHELMET", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("wifi_info", info);
        editor.commit();
    }

    public static String getWIFIInfo(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("SMARTHELMET", Context.MODE_PRIVATE);
        String str = sharedPreferences.getString("wifi_info", "");
        return str;
    }

    public static void checkWIFIState(List<WifiConfig> wifiCfgs) {

        //add by Jerry
        if (NetworkUtil.mCurrentNetType == Constant.NET_CHOOSE_FORCE_MOBILE) {
            LogUtil.e("current network type is mobile only");
            return;
        }

        WifiManager wifiManager = (WifiManager) HelmetApplication.mAppContext.getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            boolean bRet = wifiManager.setWifiEnabled(true);
            LogUtil.e("txhlog open WIFI bRet=" + bRet);
        }

        if (wifiCfgs == null || wifiCfgs.isEmpty()) {
            LogUtil.e("check wifi state failed: no wifi configuration.");
            TelephonyManager mTelephonyManager = TelephonyManager.from(HelmetApplication.mAppContext);
            ConnectivityManager mConnectivityManager = ConnectivityManager.from(HelmetApplication.mAppContext);
            NetworkInfo mobNetInfo = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            LogUtil.e("check sim state. state = " + mTelephonyManager.getSimState() +
                    " mobNetInfo = " + mobNetInfo);
            if ((mConnectivityManager.isNetworkSupported(android.net.ConnectivityManager.TYPE_MOBILE)
                    && mTelephonyManager.getSimState() != android.telephony.TelephonyManager.SIM_STATE_READY)
                    || mobNetInfo != null && !mobNetInfo.isConnected()
                    || mobNetInfo == null) {
                LogUtil.e("Take the default wifi activation process ");
                connectWIFI(wifiManager, Constant.ACTIVATION_WIFI_SSID, Constant.ACTIVATION_WIFI_PWD);
            }
            return;
        }

        WifiInfo info = wifiManager.getConnectionInfo();
        String currSsid = info.getSSID();

        boolean wifiState = false;
        LogUtil.e("txhlog checkWIFIState currSsid=" + currSsid);
        for (int j = 0; j < wifiCfgs.size(); j++) {
            WifiConfig wifiCfg = wifiCfgs.get(j);

            if (wifiCfg.isEmpty()) {
                continue;
            }


            LogUtil.e("txhlog wifiCfg=" + wifiCfg.toString());
            if (!wifiState) {
                String wifiInfo = Util.getWIFIInfo(HelmetApplication.mAppContext);
                LogUtil.e("txhlog wifiInfo=" + wifiInfo);
                String[] wifi = wifiInfo.split("/");
                if (!TextUtils.isEmpty(wifiCfg.mWifiSsid) && !TextUtils.isEmpty(wifiCfg.mWifiPasswd)) {
                    if (wifi.length == 1) {
                        wifiState = connectWIFI(wifiManager, wifiCfg.mWifiSsid, wifiCfg.mWifiPasswd);
                        if (wifiState) {
                            Util.setWIFIInfo(HelmetApplication.mAppContext, wifiCfg.mWifiSsid + "/" + wifiCfg.mWifiPasswd);
                            break;
                        }
                    } else if (wifiCfg.mWifiSsid.equals(wifi[0]) && (wifiCfg.mWifiPasswd.equals(wifi[1]))) {
                        String name = "";
                        if (currSsid.startsWith("\"") && currSsid.endsWith("\"")) {
                            name = currSsid.substring(1, currSsid.length() - 1);
                        }
                        LogUtil.e("txhlog name=" + name + "  status=" + name.equals(wifiCfg.mWifiSsid));
                        if (!name.equals(wifiCfg.mWifiSsid)) {
                            wifiState = connectWIFI(wifiManager, wifiCfg.mWifiSsid, wifiCfg.mWifiPasswd);
                            if (wifiState) {
                                Util.setWIFIInfo(HelmetApplication.mAppContext, wifiCfg.mWifiSsid + "/" + wifiCfg.mWifiPasswd);
                                break;
                            }
                        } else {
                            break;
                        }
                    } else {
                        wifiState = connectWIFI(wifiManager, wifiCfg.mWifiSsid, wifiCfg.mWifiPasswd);
                        if (wifiState) {
                            Util.setWIFIInfo(HelmetApplication.mAppContext, wifiCfg.mWifiSsid + "/" + wifiCfg.mWifiPasswd);
                            break;
                        }
                    }
                }
            }
        }
    }

    private static boolean connectWIFI(WifiManager wifiManager, String ssid, String key) {
        LogUtil.e("txhlog ssid=" + ssid + " key=" + key);
        removeWifiBySsid(wifiManager);

        //创建一个新的WifiConfiguration ，CreateWifiInfo()需要自己实现
        WifiConfiguration wifiInfo = createWifiInfo(ssid, key);
        int wcgID = wifiManager.addNetwork(wifiInfo);
        LogUtil.e("txhlog wcgID=" + wcgID);
        boolean b = wifiManager.enableNetwork(wcgID, true);

        LogUtil.e("txhlog wc.SSID= " + ssid + " wifi state=" + b);
        return b;
    }

    private static WifiConfiguration createWifiInfo(String ssid, String key) {
        WifiConfiguration wc = new WifiConfiguration();
        wc.SSID = "\"" + ssid + "\"";
        wc.preSharedKey = "\"" + key + "\"";
        wc.hiddenSSID = true;
        wc.status = WifiConfiguration.Status.ENABLED;
        wc.priority = 999999;
        wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        return wc;
    }

    private static void removeWifiBySsid(WifiManager wifiManager) {
        List<WifiConfiguration> wifiConfigs = wifiManager.getConfiguredNetworks();
        if (wifiConfigs != null) {
            for (WifiConfiguration wifiConfig : wifiConfigs) {
                boolean status = wifiManager.removeNetwork(wifiConfig.networkId);
                LogUtil.e("txhlog removeWifiBySsid, SSID = " + wifiConfig.SSID + " status = " + status);
                wifiManager.saveConfiguration();
            }
        }
    }

    public static boolean setSystemTime(long unixTime) {
        Context context = HelmetApplication.mAppContext;
        ContentResolver resolver = context.getContentResolver();
        if (Settings.Global.getInt(resolver, "Helmet_cal_time", 0) == 0) {
            long currentTime = System.currentTimeMillis();
            if (unixTime != currentTime) {
                try {
                    LogUtil.e("synchronize system time: " + unixTime);
                    SystemClock.setCurrentTimeMillis(unixTime);
                    Settings.Global.putInt(resolver, "Helmet_cal_time", 1);
                } catch (Exception e) {
                    LogUtil.e("sync system time failed.");
                    return false;
                }
            }
        }

        return true;
    }
}
