package com.cy.helmet.location;

import android.content.Context;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Bundle;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.Log;

import com.cy.helmet.config.HelmetConfig;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This is to get location info.
 */

public class LocationProvider {

    private static final String TAG = LocationProvider.class.getSimpleName();
    private static final int LOCATION_UPDATE_TIME = 1000;

    private Context mContext;
    private android.location.LocationManager mLocationManager;
    private boolean mRecordLocation;

    LocationListener[] mLocationListeners = new LocationListener[]{
            new LocationListener(android.location.LocationManager.GPS_PROVIDER),
            new LocationListener(
                    android.location.LocationManager.NETWORK_PROVIDER)};

    private List<GpsSatellite> numSatelliteList = new ArrayList<GpsSatellite>(); // 卫星信号
    private OnLocationChangeListener mOnLocationChangeListener;

    /**
     * init the location provider.
     *
     * @param context the activity context.
     */
    public LocationProvider(Context context) {
        mContext = context;
    }

    /**
     * get current location info.
     *
     * @return the location.
     */
    public Location getCurrentLocation() {
        if (!mRecordLocation) {
            return null;
        }
        // go in best to worst order
        for (int i = 0; i < mLocationListeners.length; i++) {
            Location l = mLocationListeners[i].current();
            if (l != null) {
                return l;
            }
        }
        Log.d(TAG, "No location received yet.");
        return null;
    }

    public void setLocationChangeListener(OnLocationChangeListener onLocationChangeListener) {
        mOnLocationChangeListener = onLocationChangeListener;
    }

    public void resetCurrentLocation() {
        Log.d(TAG, "resetCurrentLocation");
        for (int j = 0; j < mLocationListeners.length; j++) {
            mLocationListeners[j].resetValid();
        }
    }

    /**
     * To record location, the location info maybe updated.
     *
     * @param recordLocation true to start record location.
     *                       false to stop record location.
     */
    public void recordLocation(boolean recordLocation) {
        if (mRecordLocation != recordLocation) {
            mRecordLocation = recordLocation;
            if (recordLocation) {
                startReceivingLocationUpdates();
            } else {
                stopReceivingLocationUpdates();
            }
        }
    }

    private void startReceivingLocationUpdates() {
        Log.d(TAG, "startReceivingLocationUpdates ++++");

        if (mLocationManager == null) {
            mLocationManager = (android.location.LocationManager) mContext
                    .getSystemService(Context.LOCATION_SERVICE);
        }
        if (mLocationManager != null) {
            try {
                mLocationManager.requestLocationUpdates(
                        android.location.LocationManager.NETWORK_PROVIDER,
                        LOCATION_UPDATE_TIME, 0F, mLocationListeners[1]);
            } catch (SecurityException ex) {
                Log.e(TAG, "fail to request location update, ignore", ex);
            } catch (IllegalArgumentException ex) {
                Log.e(TAG, "provider does not exist " + ex.getMessage());
            }

            try {
                mLocationManager.requestLocationUpdates(
                        android.location.LocationManager.GPS_PROVIDER,
                        LOCATION_UPDATE_TIME, 0F, mLocationListeners[0]);
            } catch (SecurityException ex) {
                Log.e(TAG, "fail to request location update, ignore", ex);
            } catch (IllegalArgumentException ex) {
                Log.e(TAG, "provider does not exist " + ex.getMessage());
            }

            try {
                mLocationManager.addGpsStatusListener(statusListener);
            } catch (Exception e) {
                Log.e(TAG, "provider does not exist " + e.getMessage());
            }

            Log.d(TAG, "startReceivingLocationUpdates----");

        }
    }

    private void stopReceivingLocationUpdates() {
        if (mLocationManager != null) {
            Log.d(TAG, "stopReceivingLocationUpdates++++");
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.e(TAG, "fail to remove location listners, ignore", ex);
                }
            }
            Log.d(TAG, "stopReceivingLocationUpdates----");
        }
    }

    /**
     * the location listener to set to provider when request location.
     */
    private class LocationListener implements android.location.LocationListener {
        Location mLastLocation;
        boolean mValid = false;
        String mProvider;

        /**
         * init location listener.
         *
         * @param provider the location provider.
         */
        public LocationListener(String provider) {
            mProvider = provider;
            mLastLocation = new Location(mProvider);
        }

        /**
         * the callback for location changed.
         *
         * @param newLocation the location info.
         */
        @Override
        public void onLocationChanged(Location newLocation) {
            if (newLocation.getLatitude() == 0.0
                    && newLocation.getLongitude() == 0.0) {
                // Hack to filter out 0.0,0.0 locations
                return;
            }
            Log.d(TAG, "onLocationChanged  newLocation = " + newLocation.toString());
            mLastLocation.set(newLocation);
            mValid = true;
            if (mOnLocationChangeListener != null && (HelmetConfig.get().mIsSleep == 0)) {
                mOnLocationChangeListener.onLocationChange(newLocation);
            }
        }

        /**
         * to notify provider enabled.
         *
         * @param provider the location provider.
         */
        @Override
        public void onProviderEnabled(String provider) {
            // do -noting
        }

        /**
         * to notify provider disabled.
         *
         * @param provider the location provider.
         */
        @Override
        public void onProviderDisabled(String provider) {
            mValid = false;
        }

        /**
         * to notify location provider status changed.
         *
         * @param provider the location provider.
         * @param status   the location provider status.
         * @param extras   the msg Bundle.
         */
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
                case android.location.LocationProvider.OUT_OF_SERVICE:
                case android.location.LocationProvider.TEMPORARILY_UNAVAILABLE: {
                    mValid = false;
                    break;
                }
            }
        }

        /**
         * get the current location info.
         *
         * @return
         */
        public Location current() {
            Log.d(TAG, "[current],mValid = " + mValid);
            return mValid ? mLastLocation : null;
        }

        public void resetValid() {
            Log.d(TAG, "[resetValid]");
            mValid = false;
        }
    }

    /**
     * get location type gps(1)
     */
    public int getLocationType() {
        return LocationManager.GPS_PROVIDER;//mLocationType;
    }


    private final GpsStatus.Listener statusListener = new GpsStatus.Listener() {
        @Override
        public void onGpsStatusChanged(int event) {
            GpsStatus status = mLocationManager.getGpsStatus(null); // 取当前状态
            updateGpsStatus(event, status);
        }
    };

    private void updateGpsStatus(int event, GpsStatus status) {
        if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
            int maxSatellites = status.getMaxSatellites();
            Iterator<GpsSatellite> it = status.getSatellites().iterator();
            numSatelliteList.clear();
            int count = 0;
            while (it.hasNext() && count <= maxSatellites) {
                GpsSatellite s = it.next();
                count++;
                if (s.getSnr() > 15)
                    numSatelliteList.add(s);
            }
        }
    }

    public int getGpsSatelliteNum() {
        if (numSatelliteList != null)
            return numSatelliteList.size();
        return 0;
    }


    public LocationSimInfo getCellInfo() {
        String imsi = null;
        CellLocation cel = null;
        LocationSimInfo locationSimInfo = new LocationSimInfo();
        TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            imsi = telephonyManager.getSubscriberId();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "[getCellInfo],imsi = " + imsi + " net_type = " + telephonyManager.getNetworkType());
        if (imsi == null || TextUtils.isEmpty(imsi)) {
            return null;
        }
        try {
            cel = telephonyManager.getCellLocation();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "[getCellInfo],cel = " + (cel == null));
        if (cel == null) {
            return null;
        }
        String operator = telephonyManager.getNetworkOperator();
        Log.d(TAG, "[getCellInfo],operator = " + operator);
        if (operator == null || operator.length() < 5)
            return null;
        int mcc = Integer.parseInt(operator.substring(0, 3));
        int mnc = Integer.parseInt(operator.substring(3));
        int cid = 0;
        int lac = 0;
        if (cel instanceof CdmaCellLocation) {
            Log.d(TAG, "[getCellInfo],cel instanceof CdmaCellLocation ");
            CdmaCellLocation location = (CdmaCellLocation) cel;
            cid = location.getBaseStationId();
            lac = location.getNetworkId();
            mnc = location.getSystemId();
        } else if (cel instanceof GsmCellLocation) {
            Log.d(TAG, "[getCellInfo],cel instanceof GsmCellLocation ");
            GsmCellLocation location = (GsmCellLocation) cel;
            cid = location.getCid();
            lac = location.getLac();
        } else {
            Log.d(TAG, "[getCellInfo],cel instanceof none ");
            return null;
        }
        Log.d(TAG, "[getCellInfo],mcc = " + mcc + " mnc = " + mnc + " lac = " + lac + " cid = " + cid);
        locationSimInfo.setMCC(mcc);
        locationSimInfo.setMNC(mnc);
        locationSimInfo.setLAC(lac);
        locationSimInfo.setCID(cid);
        return locationSimInfo;
    }
}
