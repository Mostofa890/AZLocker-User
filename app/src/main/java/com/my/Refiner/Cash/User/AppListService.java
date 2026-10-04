package com.my.Refiner.Cash.User;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppListService extends Service {

    private static final String TAG = "AppListService";
    private static final long INTERVAL = 5 * 60 * 1000; // ৫ মিনিট

    private Handler handler;
    private DatabaseReference dbRef;
    private String deviceId;
    private boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        dbRef = FirebaseDatabase.getInstance().getReference();

        // ✅ সঠিক Device ID সেভ/পড়া
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
        if (!isRunning) {
            isRunning = true;
            sendAppList();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    sendAppList();
                    if (isRunning) handler.postDelayed(this, INTERVAL);
                }
            }, INTERVAL);
        }
        return START_STICKY;
    }

    private void sendAppList() {
        try {
            PackageManager pm = getPackageManager();
            List<ApplicationInfo> apps = pm.getInstalledApplications(0);
            Map<String, Object> appMap = new HashMap<>();

            for (ApplicationInfo app : apps) {
                if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    Map<String, Object> appInfo = new HashMap<>();
                    appInfo.put("name", pm.getApplicationLabel(app).toString());
                    try {
                        appInfo.put("version", pm.getPackageInfo(app.packageName, 0).versionName);
                    } catch (Exception e) {
                        appInfo.put("version", "unknown");
                    }
                    appMap.put(app.packageName, appInfo);
                }
            }

            dbRef.child("devices").child(deviceId).child("apps").setValue(appMap)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Apps sent: " + appMap.size()))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed: " + e.getMessage()));
        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
