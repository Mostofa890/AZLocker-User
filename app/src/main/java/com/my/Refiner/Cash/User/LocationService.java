package com.my.Refiner.Cash.User;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class LocationService extends Service implements LocationListener {

    private static final String TAG = "LocationService";
    private LocationManager locationManager;
    private DatabaseReference dbRef;
    private String deviceId;

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        dbRef = FirebaseDatabase.getInstance().getReference();

        SharedPreferences prefs = getSharedPreferences("mdm", MODE_PRIVATE);
        deviceId = prefs.getString("device_id", null);
        if (deviceId == null || deviceId.equals("unknown")) {
            deviceId = Settings.Secure.getString(
                    getContentResolver(), Settings.Secure.ANDROID_ID);
            prefs.edit().putString("device_id", deviceId).apply();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 60000, 10, this);
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER, 60000, 10, this);

                Location last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (last == null) {
                    last = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                if (last != null) onLocationChanged(last);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage());
        }
        return START_STICKY;
    }

    @Override
    public void onLocationChanged(Location location) {
        try {
            Map<String, Object> loc = new HashMap<>();
            loc.put("lat", location.getLatitude());
            loc.put("lng", location.getLongitude());
            loc.put("accuracy", location.getAccuracy());
            loc.put("time", System.currentTimeMillis());

            dbRef.child("devices").child(deviceId).child("location").setValue(loc)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Location sent"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed: " + e.getMessage()));
        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage());
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override
    public void onProviderEnabled(String provider) {}
    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
