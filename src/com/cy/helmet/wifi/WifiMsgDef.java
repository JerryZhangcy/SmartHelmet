package com.cy.helmet.wifi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class WifiMsgDef {
    public static final String WIFI_E = "e";
    public static final String WIFI_D= "d";
    public static final String WIFI_M = "m";
    public static final String WIFI_P = "p";
    public static final String WIFI_T = "t";

    public static JSONObject getWifiInfo(String m,int p,long t) {

        JSONObject gpsDataWrite = new JSONObject();
        try {
            gpsDataWrite.put(WIFI_M, m);
            gpsDataWrite.put(WIFI_P, p);
            gpsDataWrite.put(WIFI_T, t);
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        return gpsDataWrite;
    }


    public static JSONObject getWifiSendData(String e, ArrayList<JSONArray> list) {
        JSONArray array = new JSONArray();
        for (JSONArray object : list) {
            array.put(object);
        }

        JSONObject commonWrite = new JSONObject();
        try {
            commonWrite.put(WIFI_E, e);
            commonWrite.put(WIFI_D, array);
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        return commonWrite;
    }
}
