package com.example.doctruyen.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ReviewDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertReview(ReviewEntity review);

    @Query("SELECT * FROM reviews WHERE storyId = :storyId ORDER BY timestamp DESC")
    List<ReviewEntity> getReviewsForStory(String storyId);
}
