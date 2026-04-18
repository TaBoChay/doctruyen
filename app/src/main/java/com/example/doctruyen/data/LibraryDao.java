package com.example.doctruyen.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LibraryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(LibraryEntry entry);

    @Query("SELECT EXISTS(SELECT 1 FROM library WHERE storyId = :storyId AND userId = :userId AND inLibrary = 1)")
    boolean isInLibrary(String storyId, String userId);

    @Query("DELETE FROM library WHERE storyId IN (:storyIds) AND userId = :userId")
    void deleteStories(List<String> storyIds, String userId);
}
