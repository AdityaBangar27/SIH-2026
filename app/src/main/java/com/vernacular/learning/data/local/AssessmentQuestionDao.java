package com.vernacular.learning.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.vernacular.learning.data.local.entities.AssessmentQuestionEntity;
import java.util.List;

@Dao
public interface AssessmentQuestionDao {
    @Query("SELECT * FROM assessment_questions WHERE lessonId = :lessonId ORDER BY sequenceOrder ASC")
    List<AssessmentQuestionEntity> getQuestionsForLesson(String lessonId);

    @Query("SELECT * FROM assessment_questions WHERE lessonId = :lessonId ORDER BY sequenceOrder ASC")
    LiveData<List<AssessmentQuestionEntity>> getQuestionsForLessonLiveData(String lessonId);

    @Query("SELECT * FROM assessment_questions WHERE questionId = :questionId LIMIT 1")
    AssessmentQuestionEntity getQuestionById(String questionId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AssessmentQuestionEntity question);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<AssessmentQuestionEntity> questions);

    @Update
    void update(AssessmentQuestionEntity question);

    @Delete
    void delete(AssessmentQuestionEntity question);

    @Query("SELECT COUNT(*) FROM assessment_questions")
    int getCount();
}
