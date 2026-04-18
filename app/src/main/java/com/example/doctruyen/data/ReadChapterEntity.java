package com.example.doctruyen.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "read_chapters")
public class ReadChapterEntity {

    @PrimaryKey
    @NonNull
    public String chapterId;

    public String storyId;
    public boolean isRead;

}
