package com.my.Refiner.Cash.User;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashSet;
import java.util.Set;

public class AppBlockService extends AccessibilityService {

    private static final String TAG = "AppBlockService";
    private Set<String> blockedApps = new HashSet<>();
    private DatabaseReference blockRef;

    @Override
    public void onCreate() {
        super.onCreate();
        blockRef = FirebaseDatabase.getInstance().getReference()
                .child("blockedApps");
        blockRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                blockedApps.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Boolean blocked = child.getValue(Boolean.class);
                    if (blocked != null && blocked) {
                        blockedApps.add(child.getKey());
                    }
                }
                Log.d(TAG, "Blocked apps: " + blockedApps);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Firebase error: " + error.getMessage());
            }
        });
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (event.getPackageName() != null) {
                String pkg = event.getPackageName().toString();
                if (blockedApps.contains(pkg)) {
                    Log.d(TAG, "Blocking: " + pkg);
                    performGlobalAction(GLOBAL_ACTION_HOME);
                }
            }
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Accessibility interrupted");
    }
}
