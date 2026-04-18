package com.example.doctruyen.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface ReadChapterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ReadChapterEntity entity);

    @Query("SELECT EXISTS(SELECT 1 FROM read_chapters WHERE chapterId = :chapterId AND isRead = 1)")
    boolean isChapterRead(String chapterId);
}
