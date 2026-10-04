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
        settings.setUserAgentString(
                "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");

        holder.videoWebView.setWebViewClient(new WebViewClient());
        holder.videoWebView.setWebChromeClient(new WebChromeClient());

        String url = video.getUrl();
        if (url != null && !url.isEmpty()) {
            String finalUrl = getVideoUrl(url);

            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent",
                    "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");

            holder.videoWebView.loadUrl(finalUrl, headers);
        }
    }

    /** URL রূপান্তর — Cloudinary, YouTube, MP4 সব handle করে */
    private String getVideoUrl(String url) {
        try {
            // Cloudinary URL — সরাসরি HTML5 video player
            if (url.contains("cloudinary.com") || url.contains("res.cloudinary.com")) {
                return wrapInHtmlPlayer(url);
            }

            // MP4 / WebM / MKV direct link — HTML5 player
            if (url.endsWith(".mp4") || url.endsWith(".webm") ||
                url.endsWith(".mkv") || url.endsWith(".mov") ||
                url.contains(".mp4?") || url.contains(".webm?") ||
                url.contains(".mkv?") || url.contains(".mov?")) {
                return wrapInHtmlPlayer(url);
            }

            // YouTube Shorts — mobile URL
            if (url.contains("youtube.com/shorts/")) {
                String id = url.substring(url.indexOf("shorts/") + 7);
                if (id.contains("?")) id = id.substring(0, id.indexOf("?"));
                if (id.contains("/")) id = id.substring(0, id.indexOf("/"));
                return "https://m.youtube.com/watch?v=" + id;
            }

            // YouTube watch / youtu.be
            if (url.contains("youtube.com/watch") || url.contains("youtu.be/")) {
                return url.replace("www.youtube.com", "m.youtube.com");
            }

            // Facebook
            if (url.contains("facebook.com")) {
                return url.replace("www.facebook.com", "m.facebook.com");
            }

            // TikTok
            if (url.contains("tiktok.com")) {
                return url;
            }

            // Instagram
            if (url.contains("instagram.com")) {
                return url;
            }

            // Fallback
            return url;

        } catch (Exception e) {
            return url;
        }
    }

    /** MP4 URL-কে HTML5 video player-এ wrap করুন */
    private String wrapInHtmlPlayer(String videoUrl) {
        String html = "<!DOCTYPE html><html><head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0, user-scalable=no'>" +
                "<style>" +
                "* { margin:0; padding:0; box-sizing:border-box; }" +
                "html, body { width:100%; height:100%; background:#000; overflow:hidden; }" +
                "video { width:100%; height:100%; object-fit:contain; background:#000; }" +
                "</style></head><body>" +
                "<video controls autoplay muted loop playsinline preload='auto'>" +
                "<source src='" + videoUrl + "' type='video/mp4'>" +
                "Your browser does not support the video tag." +
                "</video></body></html>";
        return "data:text/html;charset=utf-8," + android.net.Uri.encode(html);
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
