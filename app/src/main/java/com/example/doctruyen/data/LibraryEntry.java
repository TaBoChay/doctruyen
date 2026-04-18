package com.example.doctruyen.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "library")
public class LibraryEntry {

    @PrimaryKey
    @NonNull
    public String id;      // = storyId + "_" + userId (unique cho từng user + truyện)

    public String storyId;
    public String userId;
    public boolean inLibrary;
}
