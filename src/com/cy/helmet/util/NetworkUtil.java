package com.cy.helmet.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.text.TextUtils;

import com.cy.helmet.Constant;
import com.cy.helmet.HelmetApplication;
import com.cy.helmet.config.HelmetConfig;
import com.cy.helmet.conn.HelmetMessageSender;
import com.cy.helmet.networkstatus.NetWorkStatusUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by jiaqing on 2018/1/4.
 */

public class NetworkUtil {

    //add by Jerry
    public static int mCurrentNetType = Constant.NET_CHOOSE_WIFI_FIRST;

    public static String activeDevice() {
        InputStream is = null;
        ByteArrayOutputStream baos = null;

        String devId = HelmetConfig.get().getDeviceId();
        Uri uri = Uri.parse(Constant.ACTIVE_SERVER_ADDRESS)
                .buildUpon()
                .appendQueryParameter("devId", devId).build();
        String actServerAddr = uri.toString();

        LogUtil.e("activeAddress>>" + actServerAddr);

        try {
            URL url = new URL(actServerAddr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("GET");
            conn.addRequestProperty("Connection", "Keep-Alive");

            // 开始连接
            conn.connect();
            int code = conn.getResponseCode();
            if (code == 200) {
                is = conn.getInputStream();
                baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len = -1;
                while ((len = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }

                byte[] byteArray = baos.toByteArray();
                return new String(byteArray);
            }
        } catch (IOException e) {
            LogUtil.e(e);
            return "";
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return "";
    }

    public static boolean hasNetwork() {
        Context context = HelmetApplication.mAppContext;
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (manager == null) {
            return false;
        }

        NetworkInfo networkinfo = manager.getActiveNetworkInfo();

        return (networkinfo != null && networkinfo.isAvailable());
    }

    public static boolean isWifiNetwork() {
        Context context = HelmetApplication.mAppContext;
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetInfo != null
                && activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            return true;
        }

        return false;
    }

    public static String sendSOSMessage() {
        InputStream is = null;
        ByteArrayOutputStream baos = null;

        String deviceId = HelmetConfig.get().getDeviceId();
        String sendSOSUrl = HelmetConfig.get().getSOSServerUrl(deviceId);
        LogUtil.e("sendSOSUrl>>" + sendSOSUrl);

        if (StringUtil.isNullEmptyOrSpace(sendSOSUrl)) {
            LogUtil.e("build sos url failed.");
            return "";
        }

        try {
            URL url = new URL(sendSOSUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("GET");
            conn.addRequestProperty("Connection", "Keep-Alive");

            // 开始连接
            conn.connect();
            int code = conn.getResponseCode();
            if (code == 200) {
                is = conn.getInputStream();
                baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len = -1;
                while ((len = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }

                byte[] byteArray = baos.toByteArray();
                return new String(byteArray);
            } else {
                LogUtil.e("http send sos failed, resp code:" + code);
            }
        } catch (IOException e) {
            LogUtil.e(e);
            return "";
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return "";
    }

    public static void setNetwork(int type) {
        mCurrentNetType = type;//add by Jerry
        switch (type) {
            case Constant.NET_CHOOSE_WIFI_FIRST:
                //TODO set wifi first network
                NetWorkStatusUtil.enableMobileData(true);
                NetWorkStatusUtil.enableWifi(true);
                HelmetMessageSender.sendNetworkChooseResp(Constant.NET_CHOOSE_WIFI_FIRST);
                break;

            case Constant.NET_CHOOSE_FORCE_WIFI:
                //TODO set wifi network
                NetWorkStatusUtil.enableMobileData(false);
                NetWorkStatusUtil.enableWifi(true);
                HelmetMessageSender.sendNetworkChooseResp(Constant.NET_CHOOSE_FORCE_WIFI);
                break;

            case Constant.NET_CHOOSE_FORCE_MOBILE:
                //TODO set mobile network
                NetWorkStatusUtil.enableMobileData(true);
                NetWorkStatusUtil.enableWifi(false);
                HelmetMessageSender.sendNetworkChooseResp(Constant.NET_CHOOSE_FORCE_MOBILE);
                break;

            default:
                break;
        }
    }
}
