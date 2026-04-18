package com.example.doctruyen.models;

import java.io.Serializable;

public class Review implements Serializable {
    private String id;
    private String storyId;
    private String userId;
    private String userName;
    private float rating;
    private String comment;
    private long timestamp;

    // DÙNG KHI TẠO REVIEW MỚI
    public Review(String id, String storyId, String userId, String userName,
                  float rating, String comment) {
        this(id, storyId, userId, userName, rating, comment, System.currentTimeMillis());
    }

    // DÙNG KHI LOAD TỪ DB/JSON
    public Review(String id, String storyId, String userId, String userName,
                  float rating, String comment, long timestamp) {
        this.id = id;
        this.storyId = storyId;
        this.userId = userId;
        this.userName = userName;
        this.rating = rating;
        this.comment = comment;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStoryId() { return storyId; }
    public void setStoryId(String storyId) { this.storyId = storyId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}