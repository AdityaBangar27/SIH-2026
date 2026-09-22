package com.vernacular.learning.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.vernacular.learning.data.local.entities.ActivityEntity;
import java.util.List;

@Dao
public interface ActivityDao {
    @Query("SELECT * FROM activities WHERE lessonId = :lessonId ORDER BY sequenceOrder ASC")
    List<ActivityEntity> getActivitiesForLesson(String lessonId);

    @Query("SELECT * FROM activities WHERE lessonId = :lessonId ORDER BY sequenceOrder ASC")
    LiveData<List<ActivityEntity>> getActivitiesForLessonLiveData(String lessonId);

    @Query("SELECT * FROM activities WHERE activityId = :activityId LIMIT 1")
    ActivityEntity getActivityById(String activityId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ActivityEntity activity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ActivityEntity> activities);

    @Update
    void update(ActivityEntity activity);

    @Delete
    void delete(ActivityEntity activity);

    @Query("SELECT COUNT(*) FROM activities")
    int getCount();
}
