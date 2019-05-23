package com.cy.helmet.location.locationcaching;

import com.cy.helmet.location.LocationDataUtil;
import com.cy.helmet.location.LocationFactory;
import com.cy.helmet.location.LocationManager;
import com.cy.helmet.location.LocationMsgDef;
import com.cy.helmet.location.LocationUtil;
import com.cy.helmet.location.locationConnect.LocationHttpConnect;
import com.cy.helmet.location.locationConnect.LocationHttpSendMessage;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by zhangchongyang on 18-3-21.
 */

public class LocationCachingSend {
    private static LocationCachingSend mInstance;

    public static synchronized LocationCachingSend getInstance() {
        if (mInstance == null) {
            synchronized (LocationCachingSend.class) {
                if (mInstance == null) {
                    mInstance = new LocationCachingSend();
                }
            }
        }
        return mInstance;
    }

    private JSONObject getLocationData() {
        JSONObject jsonObject = null;
        LocationDataUtil locationDataUtil = LocationDataUtil.getInstance();
        String IMEI = locationDataUtil.getIMEI();
        ArrayList<JSONObject> list = LocationCaching.readFullCaching(LocationUtil.GPS_CACHING_FILE_NAME);
        if (list != null && list.size() > 0) {
            jsonObject = LocationFactory.getInstance().getGpsCachingDate(LocationMsgDef.GPS_CACHING, IMEI, list);
        }
        return jsonObject;
    }

    private JSONObject getLocationNormalData() {
        JSONObject jsonObject = null;
        ArrayList<JSONObject> list = new ArrayList<JSONObject>();
        LocationDataUtil locationDataUtil = LocationDataUtil.getInstance();
        String IMEI = locationDataUtil.getIMEI();
        if (LocationDataUtil.mGpsDataState == LocationDataUtil.GPS_DATA_FIRST) {
            if (LocationDataUtil.mGpsSecondData.size() > 0) {
                for (String value : LocationDataUtil.mGpsSecondData) {
                    String[] split = value.split("\\|");
                    JSONObject object = LocationFactory.getInstance().
                            getGpsCachingEle(LocationManager.GPS_PROVIDER,
                                    split[0], split[1], Integer.valueOf(split[2]),
                                    split[3], split[4], split[5], Long.parseLong(split[split.length - 1]));
                    list.add(object);
                }
                LocationDataUtil.mGpsSecondData.clear();
            }
        } else {
            if (LocationDataUtil.mGpsFirstData.size() > 0) {
                for (String value : LocationDataUtil.mGpsFirstData) {
                    String[] split = value.split("\\|");
                    JSONObject object = LocationFactory.getInstance().
                            getGpsCachingEle(LocationManager.GPS_PROVIDER,
                                    split[0], split[1], Integer.valueOf(split[2]),
                                    split[3], split[4], split[5], Long.parseLong(split[split.length - 1]));
                    list.add(object);
                }
                LocationDataUtil.mGpsFirstData.clear();
            }
        }

        if (list != null && list.size() > 0) {
            jsonObject = LocationFactory.getInstance().getGpsCachingDate(LocationMsgDef.GPS_CACHING, IMEI, list);
        }
        return jsonObject;
    }


    public void sendLocationCaching() {
        if (LocationDataUtil.getInstance().isNetworkAvailable()) {
            JSONObject jsonObject = getLocationData();
            LocationUtil.d("<sendLocationCaching>--------->LocationCachingSend---->jsonObject = " + jsonObject);
            if (jsonObject != null) {
                LocationHttpConnect.getInstance().sendMessage(new LocationHttpSendMessage(jsonObject,
                        LocationHttpSendMessage.TYPE_CACHING));
            }
        }
    }

    public void sendNormalLocation() {
        JSONObject jsonObject = getLocationNormalData();
        if (jsonObject == null)
            return;
        LocationUtil.d("<sendNormalLocation>--------->LocationCachingSend---->jsonObject = " + jsonObject);
        if (LocationDataUtil.getInstance().isNetworkAvailable()) {
            LocationHttpConnect.getInstance().sendMessage(new LocationHttpSendMessage(jsonObject,
                    LocationHttpSendMessage.TYPE_MUL));

        } else {
            LocationCaching.saveGpsCachingBySendFailed(jsonObject, LocationHttpSendMessage.TYPE_MUL);
        }
    }
}
