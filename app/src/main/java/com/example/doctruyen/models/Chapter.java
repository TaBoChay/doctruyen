package com.example.doctruyen.models;

import java.io.Serializable;

public class Chapter implements Serializable {
    private String id;
    private String storyId;
    private String title;
    private String content;
    private int chapterNumber;
    private String filePath;
    private boolean isRead;

    public Chapter(String id, String storyId, String title, int chapterNumber, String filePath) {
        this.id = id;
        this.storyId = storyId;
        this.title = title;
        this.chapterNumber = chapterNumber;
        this.filePath = filePath;
        this.isRead = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStoryId() { return storyId; }
    public void setStoryId(String storyId) { this.storyId = storyId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public int getChapterNumber() { return chapterNumber; }
    public void setChapterNumber(int chapterNumber) { this.chapterNumber = chapterNumber; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}