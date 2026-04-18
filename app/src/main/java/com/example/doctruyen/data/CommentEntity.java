package com.example.doctruyen.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "comments")
public class CommentEntity {

    @PrimaryKey
    @NonNull
    public String id;

    public String storyId;
    public String userId;
    public String userName;
    public String content;
    public long timestamp;
}
