package com.my.Refiner.Cash.User;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {

    private DevicePolicyManager dpm;
    private ComponentName adminComponent;
    private DatabaseReference dbRef;
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, MyAdminReceiver.class);

        TextView statusText = findViewById(R.id.statusText);
        Button adminBtn = findViewById(R.id.adminBtn);
        Button startBtn = findViewById(R.id.startBtn);

        // Device ID তৈরি/পড়া
        SharedPreferences prefs = getSharedPreferences("mdm", MODE_PRIVATE);
        deviceId = prefs.getString("device_id", null);
        if (deviceId == null) {
            deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            prefs.edit().putString("device_id", deviceId).apply();
        }

        statusText.setText("Device ID: " + deviceId);

        // Firebase Database reference
        dbRef = FirebaseDatabase.getInstance().getReference();

        // অ্যাডমিন পারমিশন বাটন
        adminBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!dpm.isAdminActive(adminComponent)) {
                    Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
                    intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            "Remote lock permission required.");
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Admin already active", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // সার্ভিস চালু বাটন
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startService(new Intent(MainActivity.this, CommandPoller.class));
                Toast.makeText(MainActivity.this, "Service started", Toast.LENGTH_SHORT).show();
            }
        });

        // Firebase-এ ডিভাইস রেজিস্টার করুন
        registerDeviceInFirebase();
    }

    private void registerDeviceInFirebase() {
        Map<String, Object> deviceData = new HashMap<>();
        deviceData.put("name", android.os.Build.MODEL);
        deviceData.put("user", "User");
        deviceData.put("status", "online");
        deviceData.put("locked", false);
        deviceData.put("lastSeen", System.currentTimeMillis());

        dbRef.child("devices").child(deviceId).setValue(deviceData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(MainActivity.this, "Registered in Firebase", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Firebase error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        TextView statusText = findViewById(R.id.statusText);
        String adminStatus = dpm.isAdminActive(adminComponent) ? "Admin: ACTIVE" : "Admin: INACTIVE";
        statusText.setText("Device ID: " + deviceId + "\n" + adminStatus);
    }
}
