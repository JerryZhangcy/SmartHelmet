package com.cy.helmet.config;

import com.cy.helmet.core.protocol.Common;
import com.cy.helmet.sensor.GSensor;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ubuntu on 18-1-3.
 */

public class GyroscopeConfig {
//    message GyroscopeCfg
//    {
//        required int32 precision = 1;//精度,0-31
//    }

    public int precision = GSensor.DEFAULT_GSENSOR_VALUE; //modify by Jerry

    public GyroscopeConfig(JSONObject json) {
        if (json != null) {
            precision = json.optInt("precision", GSensor.DEFAULT_GSENSOR_VALUE);//modify by Jerry
        }
    }

    public GyroscopeConfig(Common.GyroscopeCfg cfg) {
        update(cfg);
    }

    public boolean update(Common.GyroscopeCfg cfg) {
        if (cfg.hasPrecision()) {
            int newPrecision = cfg.getPrecision();
            if ((precision != newPrecision) && 
                 (newPrecision >= GSensor.GENSOR_MIN) && 
                 (newPrecision <= GSensor.GENSOR_MAX)) {
                precision = newPrecision;
                return true;
            }
        }

        return false;
    }

    public int getPrecision() { //modify by Jerry
       return precision;
    }

    @Override
    public String toString() {
        return "GyroscopeCfg{" +
                "precision=" + precision +
                '}';
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("precision", precision);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json;
    }

    public Common.GyroscopeCfg toGyroscopeCfg() {
        Common.GyroscopeCfg.Builder builder = Common.GyroscopeCfg.newBuilder();
        builder.setX(0);
        builder.setY(0);
        builder.setZ(0);
        builder.setPrecision(precision);
        return builder.build();
    }
}
