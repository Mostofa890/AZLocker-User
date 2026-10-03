package com.my.Refiner.Cash.User;

import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CommandPoller extends Service {

    private static final String TAG = "CommandPoller";

    private DatabaseReference dbRef;
    private DevicePolicyManager dpm;
    private ComponentName adminComponent;
    private String deviceId;
    private Handler handler;
    private boolean isRunning = false;
    private ValueEventListener commandListener;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, MyAdminReceiver.class);
        deviceId = getSharedPreferences("mdm", MODE_PRIVATE)
                .getString("device_id", "unknown");
        dbRef = FirebaseDatabase.getInstance().getReference();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isRunning) {
            isRunning = true;
            registerDeviceOnline();
            listenForCommands();
        }
        return START_STICKY;
    }

    /** Firebase-এ ডিভাইস অনলাইন হিসেবে চিহ্নিত করুন */
    private void registerDeviceOnline() {
        dbRef.child("devices").child(deviceId).child("status").setValue("online");
        dbRef.child("devices").child(deviceId).child("lastSeen").setValue(System.currentTimeMillis());
    }

    /** Firebase Realtime Database-এ লক স্ট্যাটাস লিসেন করুন */
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

        // devices/{deviceId}/locked — এই পাথটি লিসেন করুন
        dbRef.child("devices").child(deviceId).child("locked")
                .addValueEventListener(commandListener);
    }

    /** 🔒 ডিভাইস লক করুন */
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
        isRunning = false;
        if (commandListener != null) {
            dbRef.child("devices").child(deviceId).child("locked")
                    .removeEventListener(commandListener);
        }
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
