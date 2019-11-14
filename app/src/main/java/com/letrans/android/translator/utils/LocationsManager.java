package com.letrans.android.translator.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.letrans.android.translator.storage.TStorageManager;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationsManager {

    private static final String TAG = "RTranslator/LocationsManager";
    private static LocationsManager mInstance;
    private final HandlerThread myHandlerThread;
    private final Handler handler;
    private LocationManager mLocationManager;
    String provider = null;
    Criteria criteria;
    private boolean isEnabled;

    private LocationsManager(Context context) {
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        provider = LocationManager.NETWORK_PROVIDER;
        criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        myHandlerThread = new HandlerThread( "handler-thread");
        myHandlerThread.start();

        handler = new Handler(myHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 1:
                        Location location = (Location) msg.obj;
                        getLocation(location.getLatitude(), location.getLongitude());
                        break;
                }
            }
        };
    }

    public static LocationsManager getInstance(Context context) {
        if (mInstance == null) {
            synchronized (LocationsManager.class) {
                if (mInstance == null) {
                    mInstance = new LocationsManager(context);
                }
            }
        }
        return mInstance;
    }


    @SuppressLint("MissingPermission")
    public void getLocation() {
        isEnabled = false;
        Looper mLooper = myHandlerThread.getLooper();
        if (mLooper == null) return;
        provider = mLocationManager.getBestProvider(criteria, true);
        if (provider == null) return;
        mLocationManager.removeUpdates(mLocationListener);
        mLocationManager.requestLocationUpdates(provider, 60 * 60 * 1000, 1, mLocationListener);
    }

    LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            //getLocation(location.getLatitude(), location.getLongitude());
            Logger.d(TAG, "onLocationChanged");
            handler.removeMessages(1);
            Message msg = new Message();
            msg.what = 1;
            msg.obj = location;
            handler.sendMessage(msg);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    @SuppressLint("MissingPermission")
    private void getLocation(double latitude, double longitude) {
        Geocoder gc = new Geocoder(AppUtils.getApp(), Locale.US);
        List<Address> locationList = null;
        try {
            locationList = gc.getFromLocation(latitude, longitude, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String countryName;
        String locality;
        String countryCode;
        Logger.d(TAG,"getLocation locationList : " + locationList);
        if (locationList != null && locationList.size() > 0) {
            Address address = locationList.get(0);
            countryName = address.getCountryName();
            locality = address.getLocality();
            countryCode = address.getCountryCode();
            Logger.d(TAG,"getLocation countryName : " + countryName
                    + " countryCode : " + countryCode + " locality : " + locality);
            if (!TextUtils.isEmpty(countryName)) {
                TStorageManager.getInstance().setLocation(countryName + "_" + countryCode);
                mLocationManager.removeUpdates(mLocationListener);
                Logger.d(TAG, "quitSafely");
                myHandlerThread.quitSafely();
            }
        } else {
            if (!TextUtils.isEmpty(provider)) {
                provider = mLocationManager.getBestProvider(criteria, true);
                if (provider == null) return;
                Location location = mLocationManager.getLastKnownLocation(provider);
                if (location == null) return;
                if (isEnabled) {
                    mLocationManager.removeUpdates(mLocationListener);
                    myHandlerThread.quitSafely();
                    return;
                }
                isEnabled = true;
                getLocation(location.getLatitude(), location.getLongitude());
            }
        }
    }
}
