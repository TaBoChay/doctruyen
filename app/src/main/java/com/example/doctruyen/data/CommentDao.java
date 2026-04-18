package com.example.doctruyen.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CommentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertComment(CommentEntity comment);

    @Query("SELECT * FROM comments WHERE storyId = :storyId ORDER BY timestamp DESC")
    List<CommentEntity> getCommentsForStory(String storyId);
}
