package com.vernacular.learning.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.vernacular.learning.data.local.entities.LessonEntity;
import java.util.List;

@Dao
public interface LessonDao {
    @Query("SELECT * FROM lessons WHERE className = :className AND subject = :subject AND motherTongue = :motherTongue ORDER BY lessonIndex ASC")
    List<LessonEntity> getLessons(String className, String subject, String motherTongue);

    @Query("SELECT * FROM lessons WHERE id = :id LIMIT 1")
    LessonEntity getLessonById(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<LessonEntity> lessons);

    @Update
    void updateLesson(LessonEntity lesson);

    @Query("SELECT COUNT(*) FROM lessons")
    int getCount();
}
