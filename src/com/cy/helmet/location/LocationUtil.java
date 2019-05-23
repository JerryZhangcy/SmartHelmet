package com.cy.helmet.location;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.net.ContentHandler;
import java.text.DecimalFormat;

/**
 * Created by zhangchongyang on 18-1-3.
 */

public class LocationUtil {
    private static final String LOC_TAG = "Helm_Location";

    public static final String GPS_IP = null;//"101.132.124.203";
    public static final int GPS_PORT = -1;//10001;
    public static final String DEF_IMEI = "867793024101505";
    public static final String GPS_FIRMWARE = "MNL_VER_17030301ALPS05_5.00_99";

    public static final int GPS_DATA_UPLOAD_GAP = 20000;//工作状态定位数据数据上传时间间隔
    public static final int GPS_STATE_UPLOAD_GAP = 20000;//工作状态GPS状态上传时间间隔
    public static final int GPS_IDEL_UPLOAD_GAP = 60000; //非工作状态下定位数据上传时间间隔

    public static final int RECONNECT_DELAYED_TIME = 5000;//连接失败或者连接断开重连的时间间隔

    public static final int RECEIVE_TIME_OUT_TIME = 7000;//接收注册包的超时时间实际超时时间是7-2

    public static final boolean UPDATE_UI = false;//是否需要更新UI界面

    public static final String GPS_CACHING_FILE_PATH = "Helmet"+File.separator + "gpsCaching";
    public static final String GPS_CACHING_FILE_NAME = "GpsCaching.txt";
    public static final String GPS_CACHING_TMP_FILE_NAME = "GpsCachingTmp.txt";
    public static final String GPS_NORMAL_FILE_NAME = "GpsNormal.txt";
    public static final String GPS_URL_SUFFIX = "/gps_reciver/recive";
    public static final String WIFI_URL_SUFFIX = "/wifi_receiver/receive";

    /**
     * 将double格式化为指定小数位的String，不足小数位用0补全
     *
     * @param v     需要格式化的数字
     * @param scale 小数点后保留几位
     * @return
     */
    public static String roundByScale(double v, int scale) {
        if (scale < 0) {
            throw new IllegalArgumentException(
                    "The   scale   must   be   a   positive   integer   or   zero");
        }
        if (scale == 0) {
            return new DecimalFormat("0").format(v);
        }
        String formatStr = "0.";
        for (int i = 0; i < scale; i++) {
            formatStr = formatStr + "0";
        }
        return new DecimalFormat(formatStr).format(v);
    }

    /**
     * 主要将如下格式的子串加入反意符，例如："{"s":1,"y":"12.0000","x":"12.000"}"  转换为
     * "{\"s\":1,\"y\":\"12.0000\",\"x\":\"12.000\"}"
     *
     * @param origin
     * @return
     */
    public static String addStringTransfer(String origin) {
        if (null == origin)
            return origin;
        if (origin.startsWith("{") && origin.endsWith("}")) {
            String tag;
            tag = origin.replaceAll("\"", "\"");
            return tag;
        }
        return origin;
    }

    /**
     * 主要将如下格式的子串加入反意符，例如："{\"s\":1,\"y\":\"12.0000\",\"x\":\"12.000\"}" 转换为
     * "{"s":1,"y":"12.0000","x":"12.000"}"
     *
     * @param origin
     * @return
     */
    public static String removeStringTransfer(String origin) {
        if (null == origin)
            return origin;
        if (origin.startsWith("{") && origin.endsWith("}") && origin.contains("\\")) {
            String tag;
            tag = origin.replaceAll("\\\\", "");
            return tag;
        }
        return origin;
    }

    public static void d(String value) {
        Log.e(LOC_TAG, value);
    }

    public static void e(String value) {
        Log.e(LOC_TAG, value);
    }

}
