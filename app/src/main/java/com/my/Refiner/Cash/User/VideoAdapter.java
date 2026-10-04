package com.my.Refiner.Cash.User;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        WebSettings settings = holder.videoWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setUserAgentString("Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");

        holder.videoWebView.setWebViewClient(new WebViewClient());
        holder.videoWebView.setWebChromeClient(new WebChromeClient());

        String url = video.getUrl();
        if (url != null && !url.isEmpty()) {
            String embedUrl = getEmbedUrl(url);

            Map<String, String> headers = new HashMap<>();
            headers.put("Referer", "https://www.youtube.com/");
            headers.put("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");
            headers.put("Origin", "https://www.youtube.com");

            holder.videoWebView.loadUrl(embedUrl, headers);
        }
    }

    private String getEmbedUrl(String url) {
        try {
            // YouTube Shorts
            if (url.contains("youtube.com/shorts/")) {
                String id = url.substring(url.indexOf("shorts/") + 7);
                if (id.contains("?")) id = id.substring(0, id.indexOf("?"));
                if (id.contains("/")) id = id.substring(0, id.indexOf("/"));
                return "https://m.youtube.com/watch?v=" + id;
            }

            // YouTube watch
            if (url.contains("youtube.com/watch")) {
                return url.replace("www.youtube.com", "m.youtube.com");
            }

            // youtu.be
            if (url.contains("youtu.be/")) {
                String id = url.substring(url.indexOf("youtu.be/") + 9);
                if (id.contains("?")) id = id.substring(0, id.indexOf("?"));
                return "https://m.youtube.com/watch?v=" + id;
            }

            // Facebook
            if (url.contains("facebook.com")) {
                return "https://m.facebook.com" + url.substring(url.indexOf("facebook.com") + 12);
            }

            // Instagram
            if (url.contains("instagram.com")) {
                return url.replace("www.instagram.com", "www.instagram.com");
            }

            // TikTok
            if (url.contains("tiktok.com")) {
                return url;
            }

            // MP4 বা অন্য
            return url;

        } catch (Exception e) {
            return url;
        }
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.videoWebView != null) {
            holder.videoWebView.loadUrl("about:blank");
        }
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        WebView videoWebView;
        TextView videoAuthor, videoTitle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            videoWebView = itemView.findViewById(R.id.videoWebView);
            videoAuthor = itemView.findViewById(R.id.videoAuthor);
            videoTitle = itemView.findViewById(R.id.videoTitle);
        }
    }
}
