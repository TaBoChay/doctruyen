package com.example.doctruyen.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "reviews")
public class ReviewEntity {

    @PrimaryKey
    @NonNull
    public String id;

    public String storyId;
    public String userId;
    public String userName;
    public float rating;
    public String comment;
    public long timestamp;
}
