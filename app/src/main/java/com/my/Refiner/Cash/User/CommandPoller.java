package com.my.Refiner.Cash.User;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
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

    @Override
    public void onCreate() {
        super.onCreate();
        dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, MyAdminReceiver.class);
        deviceId = getSharedPreferences("mdm", MODE_PRIVATE)
                .getString("device_id", "unknown");
        dbRef = FirebaseDatabase.getInstance().getReference();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // ✅ Foreground Service চালু
        startForegroundService();

        // ✅ ডিভাইস অনলাইন চিহ্নিত
        registerDeviceOnline();

        // ✅ কমান্ড লিসেন
        if (commandListener == null) {
            listenForCommands();
        }

        return START_STICKY;
    }

    private void startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "AZ Locker Service",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Remote device management");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("AZ Locker")
                    .setContentText("Active")
                    .setSmallIcon(android.R.drawable.ic_lock_lock)
                    .build();
        } else {
            notification = new Notification.Builder(this)
                    .setContentTitle("AZ Locker")
                    .setContentText("Active")
                    .setSmallIcon(android.R.drawable.ic_lock_lock)
                    .build();
        }

        startForeground(NOTIFICATION_ID, notification);
    }

    private void registerDeviceOnline() {
        dbRef.child("devices").child(deviceId).child("status").setValue("online");
        dbRef.child("devices").child(deviceId).child("lastSeen").setValue(System.currentTimeMillis());
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
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (commandListener != null) {
            dbRef.child("devices").child(deviceId).child("locked")
                    .removeEventListener(commandListener);
        }
        // সার্ভিস আবার চালু করুন
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
