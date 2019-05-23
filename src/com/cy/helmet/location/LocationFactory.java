package com.cy.helmet.location;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by zhangchongyang on 18-1-3.
 */

public class LocationFactory {
    private static LocationFactory mInstance;

    public static synchronized LocationFactory getInstance() {
        if (mInstance == null) {
            synchronized (LocationFactory.class) {
                if (mInstance == null) {
                    mInstance = new LocationFactory();
                }
            }
        }
        return mInstance;
    }

    private LocationFactory() {

    }

    public JSONObject getGpsconfirm(byte t, String e, String n) {
        LocationMsgDef.Common common = new LocationMsgDef.Common();
        common.setT(t);
        common.setE(e);
        JSONObject commonWrite = new JSONObject();
        try {
            commonWrite.put(LocationMsgDef.Common.COMMON_T, common.getT());
            commonWrite.put(LocationMsgDef.Common.COMMON_E, common.getE());
            commonWrite.put(LocationMsgDef.Common.COMMON_N, n);
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        return commonWrite;
    }

    public JSONObject getGpsData(byte t, String e, int s, String y, String x, int g, long time,
                                 String xs, String ys, String zs) {
        LocationMsgDef.GpsData gpsData = new LocationMsgDef.GpsData();
        gpsData.setS(s);
        gpsData.setX(x);
        gpsData.setY(y);
        gpsData.setG(g);
        gpsData.setXs(xs);
        gpsData.setYs(ys);
        gpsData.setZs(zs);
        gpsData.setTime(time);

        JSONObject gpsDataWrite = new JSONObject();
        try {
            gpsDataWrite.put(LocationMsgDef.GpsData.GPSDATA_S, gpsData.getS());
            gpsDataWrite.put(LocationMsgDef.GpsData.GPSDATA_Y, gpsData.getY());
            gpsDataWrite.put(LocationMsgDef.GpsData.GPSDATA_X, gpsData.getX());
            gpsDataWrite.put(LocationMsgDef.GpsData.GPSDATA_G, gpsData.getG());
            gpsDataWrite.put(LocationMsgDef.GpsData.GPSDATA_XS, gpsData.getXs());
            gpsDataWrite.put(LocationMsgDef.GpsData.GPSDATA_YS, gpsData.getYs());
            gpsDataWrite.put(LocationMsgDef.GpsData.GPSDATA_ZS, gpsData.getZs());
            gpsDataWrite.put(LocationMsgDef.GpsData.GPSDATA_TIME, gpsData.getTime());
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        //String gpsTransfer = LocationUtil.addStringTransfer(gpsDataWrite.toString());

        LocationMsgDef.Common common = new LocationMsgDef.Common();
        common.setT(t);
        common.setE(e);
        JSONObject commonWrite = new JSONObject();
        try {
            commonWrite.put(LocationMsgDef.Common.COMMON_T, common.getT());
            commonWrite.put(LocationMsgDef.Common.COMMON_E, common.getE());
            commonWrite.put(LocationMsgDef.Common.COMMON_N, gpsDataWrite);
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        return commonWrite;
    }

    public JSONObject getGpsCachingDate(byte t, String e, ArrayList<JSONObject> list) {
        JSONArray array = new JSONArray();
        for (JSONObject object : list) {
            array.put(object);
        }

        LocationMsgDef.Common common = new LocationMsgDef.Common();
        common.setT(t);
        common.setE(e);
        JSONObject commonWrite = new JSONObject();
        try {
            commonWrite.put(LocationMsgDef.Common.COMMON_T, common.getT());
            commonWrite.put(LocationMsgDef.Common.COMMON_E, common.getE());
            commonWrite.put(LocationMsgDef.Common.COMMON_N, array);
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        return commonWrite;
    }

    public JSONObject getGpsCachingEle(int s, String y, String x, int g,
                                       String xs, String ys, String zs, long time) {
        LocationMsgDef.GpsData gpsData = new LocationMsgDef.GpsData();
        gpsData.setS(s);
        gpsData.setX(x);
        gpsData.setY(y);
        gpsData.setG(g);
        gpsData.setXs(xs);
        gpsData.setYs(ys);
        gpsData.setZs(zs);
        gpsData.setTime(time);

        JSONObject gpsDataWrite = new JSONObject();
        try {
            gpsDataWrite.put(LocationMsgDef.GpsData.GPSDATA_S, gpsData.getS());
            gpsDataWrite.put(LocationMsgDef.GpsData.GPSDATA_Y, gpsData.getY());
            gpsDataWrite.put(LocationMsgDef.GpsData.GPSDATA_X, gpsData.getX());
            gpsDataWrite.put(LocationMsgDef.GpsData.GPSDATA_G, gpsData.getG());
            gpsDataWrite.put(LocationMsgDef.GpsData.GPSDATA_XS, gpsData.getXs());
            gpsDataWrite.put(LocationMsgDef.GpsData.GPSDATA_YS, gpsData.getYs());
            gpsDataWrite.put(LocationMsgDef.GpsData.GPSDATA_ZS, gpsData.getZs());
            gpsDataWrite.put(LocationMsgDef.GpsData.GPSDATA_TIME, gpsData.getTime());
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        return gpsDataWrite;
    }

    public JSONObject getGpsParameter(byte t, String e, int a, String i, int p, int d, int f) {
        LocationMsgDef.GpsParameter gpsParameter = new LocationMsgDef.GpsParameter();
        gpsParameter.setA(a);
        gpsParameter.setI(i);
        gpsParameter.setP(p);
        gpsParameter.setD(d);
        gpsParameter.setF(f);
        //gpsParameter.setB(b);
        JSONObject gpsParameterWrite = new JSONObject();
        try {
            gpsParameterWrite.put(LocationMsgDef.GpsParameter.GPSPARAMETER_A, gpsParameter.getA());
            gpsParameterWrite.put(LocationMsgDef.GpsParameter.GPSPARAMETER_I, gpsParameter.getI());
            gpsParameterWrite.put(LocationMsgDef.GpsParameter.GPSPARAMETER_P, gpsParameter.getP());
            gpsParameterWrite.put(LocationMsgDef.GpsParameter.GPSPARAMETER_D, gpsParameter.getD());
            gpsParameterWrite.put(LocationMsgDef.GpsParameter.GPSPARAMETER_F, gpsParameter.getF());
            //gpsParameterWrite.put(LocationMsgDef.GpsParameter.GPSPARAMETER_B, gpsParameter.getB());
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        //String gpsTransfer = LocationUtil.addStringTransfer(gpsParameterWrite.toString());

        LocationMsgDef.Common common = new LocationMsgDef.Common();
        common.setT(t);
        common.setE(e);
        JSONObject commonWrite = new JSONObject();
        try {
            commonWrite.put(LocationMsgDef.Common.COMMON_T, common.getT());
            commonWrite.put(LocationMsgDef.Common.COMMON_E, common.getE());
            commonWrite.put(LocationMsgDef.Common.COMMON_N, gpsParameterWrite);
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        return commonWrite;
    }

    public JSONObject getGpsState(byte t, String e, int c, int g, int b) {
        LocationMsgDef.GpsState gpsState = new LocationMsgDef.GpsState();
        gpsState.setC(c);
        gpsState.setG(g);
        gpsState.setB(b);
        JSONObject gpsStateWrite = new JSONObject();
        try {
            gpsStateWrite.put(LocationMsgDef.GpsState.GPSSTATE_C, gpsState.getC());
            gpsStateWrite.put(LocationMsgDef.GpsState.GPSSTATE_G, gpsState.getG());
            gpsStateWrite.put(LocationMsgDef.GpsState.GPSSTATE_B, gpsState.getB());
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        //String gpsTransfer = LocationUtil.addStringTransfer(gpsStateWrite.toString());

        LocationMsgDef.Common common = new LocationMsgDef.Common();
        common.setT(t);
        common.setE(e);
        JSONObject commonWrite = new JSONObject();
        try {
            commonWrite.put(LocationMsgDef.Common.COMMON_T, common.getT());
            commonWrite.put(LocationMsgDef.Common.COMMON_E, common.getE());
            commonWrite.put(LocationMsgDef.Common.COMMON_N, gpsStateWrite);
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        return commonWrite;
    }

    public JSONObject getGpsSosSame(byte t, String e, String m) {
        LocationMsgDef.GpsSosSame gpsSosSame = new LocationMsgDef.GpsSosSame();
        gpsSosSame.setM(m);
        JSONObject gpsSosSameWrite = new JSONObject();
        try {
            gpsSosSameWrite.put(LocationMsgDef.GpsSosSame.GPSSOSSAME_m, gpsSosSame.getM());
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        String gpsTransfer = LocationUtil.addStringTransfer(gpsSosSameWrite.toString());

        LocationMsgDef.Common common = new LocationMsgDef.Common();
        common.setT(t);
        common.setE(e);
        JSONObject commonWrite = new JSONObject();
        try {
            commonWrite.put(LocationMsgDef.Common.COMMON_T, common.getT());
            commonWrite.put(LocationMsgDef.Common.COMMON_E, common.getE());
            commonWrite.put(LocationMsgDef.Common.COMMON_N, gpsTransfer);
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        return commonWrite;
    }

    public JSONObject getGpsInfo(byte t, String e, int a, String p, String q, String m) {
        LocationMsgDef.GpsInfo gpsInfo = new LocationMsgDef.GpsInfo();
        gpsInfo.setA(a);
        gpsInfo.setP(p);
        gpsInfo.setQ(q);
        gpsInfo.setM(m);
        JSONObject gpsInfoWrite = new JSONObject();
        try {
            gpsInfoWrite.put(LocationMsgDef.GpsInfo.GPSINFO_A, gpsInfo.getA());
            gpsInfoWrite.put(LocationMsgDef.GpsInfo.GPSINFO_P, gpsInfo.getP());
            gpsInfoWrite.put(LocationMsgDef.GpsInfo.GPSINFO_Q, gpsInfo.getQ());
            gpsInfoWrite.put(LocationMsgDef.GpsInfo.GPSINFO_M, gpsInfo.getM());
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        //String gpsTransfer = LocationUtil.addStringTransfer(gpsInfoWrite.toString());

        LocationMsgDef.Common common = new LocationMsgDef.Common();
        common.setT(t);
        common.setE(e);
        JSONObject commonWrite = new JSONObject();
        try {
            commonWrite.put(LocationMsgDef.Common.COMMON_T, common.getT());
            commonWrite.put(LocationMsgDef.Common.COMMON_E, common.getE());
            commonWrite.put(LocationMsgDef.Common.COMMON_N, gpsInfoWrite);
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        return commonWrite;
    }

    public JSONObject getGpsError(byte t, String e, int a, byte p) {
        LocationMsgDef.GpsError gpsError = new LocationMsgDef.GpsError();
        gpsError.setA(a);
        gpsError.setP(p);
        JSONObject gpsErrorWrite = new JSONObject();
        try {
            gpsErrorWrite.put(LocationMsgDef.GpsError.GPSERROR_A, gpsError.getA());
            gpsErrorWrite.put(LocationMsgDef.GpsError.GPSERROR_P, gpsError.getP());
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        //String gpsTransfer = LocationUtil.addStringTransfer(gpsErrorWrite.toString());

        LocationMsgDef.Common common = new LocationMsgDef.Common();
        common.setT(t);
        common.setE(e);
        JSONObject commonWrite = new JSONObject();
        try {
            commonWrite.put(LocationMsgDef.Common.COMMON_T, common.getT());
            commonWrite.put(LocationMsgDef.Common.COMMON_E, common.getE());
            commonWrite.put(LocationMsgDef.Common.COMMON_N, gpsErrorWrite);
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        return commonWrite;
    }

    public JSONObject getGpsLbs(byte t, String e, int m, int n, int l, int c,
                                String xs, String ys, String zs) {
        LocationMsgDef.GpsLbs gpsLbs = new LocationMsgDef.GpsLbs();
        gpsLbs.setM(m);
        gpsLbs.setN(n);
        gpsLbs.setL(l);
        gpsLbs.setC(c);
        gpsLbs.setXs(xs);
        gpsLbs.setYs(ys);
        gpsLbs.setZs(zs);
        JSONObject gpsLbsWrite = new JSONObject();
        try {
            gpsLbsWrite.put(LocationMsgDef.GpsLbs.GPSLBS_M, gpsLbs.getM());
            gpsLbsWrite.put(LocationMsgDef.GpsLbs.GPSLBS_N, gpsLbs.getN());
            gpsLbsWrite.put(LocationMsgDef.GpsLbs.GPSLBS_L, gpsLbs.getL());
            gpsLbsWrite.put(LocationMsgDef.GpsLbs.GPSLBS_C, gpsLbs.getC());
            gpsLbsWrite.put(LocationMsgDef.GpsLbs.GPSDATA_XS, gpsLbs.getXs());
            gpsLbsWrite.put(LocationMsgDef.GpsLbs.GPSDATA_YS, gpsLbs.getYs());
            gpsLbsWrite.put(LocationMsgDef.GpsLbs.GPSDATA_ZS, gpsLbs.getZs());
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        //String gpsTransfer = LocationUtil.addStringTransfer(gpsLbsWrite.toString());
        LocationMsgDef.Common common = new LocationMsgDef.Common();
        common.setT(t);
        common.setE(e);
        JSONObject commonWrite = new JSONObject();
        try {
            commonWrite.put(LocationMsgDef.Common.COMMON_T, common.getT());
            commonWrite.put(LocationMsgDef.Common.COMMON_E, common.getE());
            commonWrite.put(LocationMsgDef.Common.COMMON_N, gpsLbsWrite);
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        return commonWrite;
    }

}
