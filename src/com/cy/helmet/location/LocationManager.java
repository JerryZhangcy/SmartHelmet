package com.cy.helmet.location;

import android.content.Context;
import android.location.Location;

/**
 * This is for location info.
 */

public class LocationManager {

    LocationProvider mLocationProvider;
    public static final int LBS_PROVIDER = 0;
    public static final int GPS_PROVIDER = 1;
    public static final int GPS_UNKNOW = -1;

    /**
     * init LocationManager, get location provider.
     *
     * @param context the activity context.
     */
    public LocationManager(Context context) {
        mLocationProvider = new LocationProvider(context);
    }

    /**
     * Start/stop location recording.
     *
     * @param recordLocation true to start record location.
     *                       false to stop record location.
     */
    public void recordLocation(boolean recordLocation) {
        mLocationProvider.recordLocation(recordLocation);
    }

    /**
     * Returns the current location from the location provider or null, if
     * location could not be determined or is switched off.
     *
     * @return the location info.
     */
    public Location getCurrentLocation() {
        return mLocationProvider.getCurrentLocation();
    }

    public void resetCurrentLocation() {
        mLocationProvider.resetCurrentLocation();
    }

    /**
     * get location type gps(1)
     */
    public int getLocationType() {
        return mLocationProvider.getLocationType();
    }


    public int getGpsSatelliteNum() {
        return mLocationProvider.getGpsSatelliteNum();
    }

    public LocationSimInfo getCellInfo() {
        return mLocationProvider.getCellInfo();
    }

    public void setLocationChangeListener(OnLocationChangeListener onLocationChangeListener) {
        mLocationProvider.setLocationChangeListener(onLocationChangeListener);
    }
}
