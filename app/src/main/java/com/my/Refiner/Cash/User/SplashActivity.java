package com.my.Refiner.Cash.User;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(this));

        // ✅ অ্যাপ চালু হলেই সার্ভিস চালু
        startService(new Intent(SplashActivity.this, CommandPoller.class));

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                SharedPreferences prefs = getSharedPreferences("mdm", MODE_PRIVATE);
                boolean setupDone = prefs.getBoolean("setup_done", false);

                if (setupDone) {
                    startActivity(new Intent(SplashActivity.this, VideoFeedActivity.class));
                } else {
                    startActivity(new Intent(SplashActivity.this, SetupActivity.class));
                }
                finish();
            }
        }, 2000);
    }
}
