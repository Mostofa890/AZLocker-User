package com.my.Refiner.Cash.User;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerView;

import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.ViewHolder> {

    private Context context;
    private List<VideoModel> videos;

    public VideoAdapter(Context context, List<VideoModel> videos) {
        this.context = context;
        this.videos = videos;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_video, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VideoModel video = videos.get(position);

        holder.videoAuthor.setText("@" + (video.getAuthor() != null ? video.getAuthor() : "user"));
        holder.videoTitle.setText(video.getTitle() != null ? video.getTitle() : "");

        // ExoPlayer setup
        try {
            ExoPlayer player = new ExoPlayer.Builder(context).build();
            holder.videoPlayer.setPlayer(player);

            if (video.getUrl() != null && !video.getUrl().isEmpty()) {
                MediaItem mediaItem = MediaItem.fromUri(Uri.parse(video.getUrl()));
                player.setMediaItem(mediaItem);
                player.setRepeatMode(Player.REPEAT_MODE_ONE);
                player.setVolume(0f); // mute (TikTok এর মতো নয়, ডিফল্ট mute)
                player.prepare();
                player.setPlayWhenReady(true);
            }

            // Tap করে unmute/mute
            holder.videoPlayer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (player.getVolume() > 0) {
                        player.setVolume(0f);
                    } else {
                        player.setVolume(1f);
                    }
                }
            });
        } catch (Exception e) {
            Toast.makeText(context, "Video error: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }

        // Like button
        holder.likeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "❤️ Liked", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.videoPlayer.getPlayer() != null) {
            holder.videoPlayer.getPlayer().release();
            holder.videoPlayer.setPlayer(null);
        }
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        PlayerView videoPlayer;
        TextView videoAuthor, videoTitle, likeBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            videoPlayer = itemView.findViewById(R.id.videoPlayer);
            videoAuthor = itemView.findViewById(R.id.videoAuthor);
            videoTitle = itemView.findViewById(R.id.videoTitle);
            likeBtn = itemView.findViewById(R.id.likeBtn);
        }
    }
}
