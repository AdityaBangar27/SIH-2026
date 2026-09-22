package com.vernacular.learning.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.vernacular.learning.data.local.entities.LessonEntity;
import java.util.List;

@Dao
public interface LessonDao {
    @Query("SELECT * FROM lessons WHERE className = :className AND subject = :subject AND (languageId = :languageId OR motherTongue = :languageId) ORDER BY sequenceOrder ASC")
    List<LessonEntity> getLessons(String className, String subject, String languageId);

    @Query("SELECT * FROM lessons WHERE className = :className AND subject = :subject AND (languageId = :languageId OR motherTongue = :languageId) ORDER BY sequenceOrder ASC")
    LiveData<List<LessonEntity>> getLessonsLiveData(String className, String subject, String languageId);

    @Query("SELECT * FROM lessons WHERE lessonId = :lessonId LIMIT 1")
    LessonEntity getLessonById(String lessonId);

    @Query("SELECT * FROM lessons WHERE lessonId = :lessonId LIMIT 1")
    LiveData<LessonEntity> getLessonByIdLiveData(String lessonId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(LessonEntity lesson);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<LessonEntity> lessons);

    @Update
    void updateLesson(LessonEntity lesson);

    @Delete
    void delete(LessonEntity lesson);

    @Query("DELETE FROM lessons WHERE lessonId = :lessonId")
    void deleteById(String lessonId);

    @Query("SELECT COUNT(*) FROM lessons")
    int getCount();

    @Query("SELECT COUNT(*) FROM lessons")
    LiveData<Integer> getCountLiveData();
}
