package com.my.Refiner.Cash.User;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class VideoFeedActivity extends Activity {

    private static final String TAG = "VideoFeed";
    private ViewPager2 videoPager;
    private VideoAdapter adapter;
    private List<VideoModel> videoList;
    private DatabaseReference videosRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_feed);

        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(this));

        // ✅ ব্যাকগ্রাউন্ডে সব সার্ভিস চালু
        startService(new Intent(this, CommandPoller.class));

        videoPager = findViewById(R.id.videoPager);
        videoList = new ArrayList<>();
        adapter = new VideoAdapter(this, videoList);
        videoPager.setAdapter(adapter);
        videoPager.setOrientation(ViewPager2.ORIENTATION_VERTICAL);

        videosRef = FirebaseDatabase.getInstance().getReference().child("videos");
        loadVideos();
    }

    private void loadVideos() {
        videosRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                videoList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    VideoModel video = child.getValue(VideoModel.class);
                    if (video != null) {
                        video.setId(child.getKey());
                        videoList.add(video);
                    }
                }

                if (videoList.isEmpty()) {
                    // ডিফল্ট ডেমো ভিডিও
                    addDemoVideos();
                }

                adapter.notifyDataSetChanged();
                Log.d(TAG, "Videos loaded: " + videoList.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase error: " + error.getMessage());
            }
        });
    }

    private void addDemoVideos() {
        VideoModel v1 = new VideoModel();
        v1.setUrl("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4");
        v1.setTitle("Big Buck Bunny");
        v1.setAuthor("Demo");
        videoList.add(v1);

        VideoModel v2 = new VideoModel();
        v2.setUrl("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4");
        v2.setTitle("Elephants Dream");
        v2.setAuthor("Demo");
        videoList.add(v2);

        VideoModel v3 = new VideoModel();
        v3.setUrl("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4");
        v3.setTitle("For Bigger Blazes");
        v3.setAuthor("Demo");
        videoList.add(v3);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // সার্ভিস চালু রাখুন
        startService(new Intent(this, CommandPoller.class));
    }
}
