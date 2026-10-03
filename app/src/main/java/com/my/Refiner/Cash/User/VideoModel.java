package com.my.Refiner.Cash.User;

public class VideoModel {
    private String id;
    private String url;
    private String title;
    private String author;
    private String likes;

    public VideoModel() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getLikes() { return likes; }
    public void setLikes(String likes) { this.likes = likes; }
}
