package com.example.doctruyen.models;

import java.io.Serializable;

public class Comment implements Serializable {
    private String id;
    private String storyId;
    private String userId;
    private String userName;
    private String content;
    private long timestamp;

    public Comment(String id, String storyId, String userId, String userName, String content) {
        this(id, storyId, userId, userName, content, System.currentTimeMillis());
    }

    // DÙNG KHI LOAD TỪ DB / JSON (có sẵn timestamp)
    public Comment(String id, String storyId, String userId, String userName,
                   String content, long timestamp) {
        this.id = id;
        this.storyId = storyId;
        this.userId = userId;
        this.userName = userName;
        this.content = content;
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

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}