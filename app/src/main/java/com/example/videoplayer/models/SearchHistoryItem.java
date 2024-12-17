package com.example.videoplayer.models;

public class SearchHistoryItem {
    private String title;
    private String thumbnailUrl;
    private long timestamp;
    private String videoId;

    public SearchHistoryItem(String title, String thumbnailUrl, long timestamp, String videoId) {
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.timestamp = timestamp;
        this.videoId = videoId;
    }

    public String getTitle() {
        return title;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getVideoId() {
        return videoId;
    }
}