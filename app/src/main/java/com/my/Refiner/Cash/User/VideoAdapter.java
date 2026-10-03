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

        // WebView সেটআপ
        WebSettings settings = holder.videoWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        holder.videoWebView.setWebViewClient(new WebViewClient());
        holder.videoWebView.setWebChromeClient(new WebChromeClient());

        String url = video.getUrl();
        if (url != null && !url.isEmpty()) {
            String embedUrl = getEmbedUrl(url);
            holder.videoWebView.loadUrl(embedUrl);
        }
    }

    /** যেকোনো লিংককে embed URL-এ রূপান্তর */
    private String getEmbedUrl(String url) {
        try {
            // YouTube Shorts → YouTube Embed
            if (url.contains("youtube.com/shorts/")) {
                String id = url.substring(url.indexOf("shorts/") + 7);
                if (id.contains("?")) id = id.substring(0, id.indexOf("?"));
                return "https://www.youtube.com/embed/" + id + "?autoplay=1&mute=1&loop=1&playlist=" + id;
            }

            // YouTube watch → YouTube Embed
            if (url.contains("youtube.com/watch")) {
                String id = "";
                if (url.contains("v=")) {
                    id = url.substring(url.indexOf("v=") + 2);
                    if (id.contains("&")) id = id.substring(0, id.indexOf("&"));
                }
                return "https://www.youtube.com/embed/" + id + "?autoplay=1&mute=1&loop=1&playlist=" + id;
            }

            // youtu.be → YouTube Embed
            if (url.contains("youtu.be/")) {
                String id = url.substring(url.indexOf("youtu.be/") + 9);
                if (id.contains("?")) id = id.substring(0, id.indexOf("?"));
                return "https://www.youtube.com/embed/" + id + "?autoplay=1&mute=1&loop=1&playlist=" + id;
            }

            // Facebook → Facebook Embed (উইথ plugin)
            if (url.contains("facebook.com")) {
                return "https://www.facebook.com/plugins/video.php?href="
                        + java.net.URLEncoder.encode(url, "UTF-8")
                        + "&show_text=false&autoplay=true&mute=1";
            }

            // Instagram → Instagram Embed
            if (url.contains("instagram.com")) {
                String cleanUrl = url;
                if (!cleanUrl.endsWith("/")) cleanUrl += "/";
                return cleanUrl + "embed/";
            }

            // TikTok → TikTok Embed
            if (url.contains("tiktok.com")) {
                // TikTok embed সরাসরি লিংক support করে
                return url;
            }

            // MP4 বা অন্য যেকোনো লিংক — সরাসরি লোড
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
