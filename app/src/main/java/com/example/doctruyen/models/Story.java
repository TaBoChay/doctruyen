package com.example.doctruyen.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Story implements Serializable {
    private String id;
    private String title;
    private String author;
    private String description;
    private String coverImagePath;
    private String bannerImagePath;
    private String category;
    private String folderName;
    private String translatorEmail; // Email người đăng truyện
    private float rating;
    private int totalReviews;
    private int views;
    private ArrayList<Chapter> chapters;
    private boolean isInLibrary;

    private List<Review> reviews;
    private List<Comment> comments;

    // Cập nhật Constructor để bao gồm folderName
    public Story(String id, String title, String author, String description,
                 String coverImagePath, String bannerImagePath, String category, String folderName) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.description = description;
        this.coverImagePath = coverImagePath;
        this.bannerImagePath = bannerImagePath;
        this.category = category;
        this.folderName = folderName; // GÁN TRƯỜNG MỚI
        this.rating = 0;
        this.totalReviews = 0;
        this.views = 0;
        this.chapters = new ArrayList<>();
        this.isInLibrary = false;
        this.reviews = new ArrayList<>();
        this.comments = new ArrayList<>();
    }

    // --- GETTER & SETTER CHO FOLDER NAME (Dùng cho logic tìm kiếm) ---
    public String getFolderName() {
        return folderName;
    }
    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }
    // ------------------------------------------------------------------

    public String getTranslatorEmail() { return translatorEmail; }
    public void setTranslatorEmail(String translatorEmail) { this.translatorEmail = translatorEmail; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCoverImagePath() { return coverImagePath; }
    public void setCoverImagePath(String coverImagePath) { this.coverImagePath = coverImagePath; }

    public String getBannerImagePath() { return bannerImagePath; }
    public void setBannerImagePath(String bannerImagePath) { this.bannerImagePath = bannerImagePath; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public int getTotalReviews() { return totalReviews; }
    public void setTotalReviews(int totalReviews) { this.totalReviews = totalReviews; }
    
    public int getViews() { return views; }
    public void setViews(int views) { this.views = views; }

    public int getTotalChapters() { return chapters.size(); }

    public ArrayList<Chapter> getChapters() { return chapters; }
    public void setChapters(ArrayList<Chapter> chapters) { this.chapters = chapters; }

    public boolean isInLibrary() { return isInLibrary; }
    public void setInLibrary(boolean inLibrary) { isInLibrary = inLibrary; }

    // Getter/Setter cho Reviews
    public List<Review> getReviews() { return reviews; }
    public void setReviews(List<Review> reviews) { this.reviews = reviews; }

    // Getter/Setter cho Comments
    public List<Comment> getComments() { return comments; }
    public void setComments(List<Comment> comments) { this.comments = comments; }
}