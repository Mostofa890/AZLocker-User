package com.my.Refiner.Cash.User;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(this));

        // ✅ সরাসরি VideoFeedActivity-তে রিডাইরেক্ট
        // MainActivity কখনো UI দেখাবে না
        Intent intent = new Intent(MainActivity.this, VideoFeedActivity.class);
        startActivity(intent);
        finish();
    }
}
