package com.vernacular.learning.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.vernacular.learning.data.local.entities.VocabularyEntity;
import java.util.List;

@Dao
public interface VocabularyDao {
    @Query("SELECT * FROM vocabulary ORDER BY word ASC")
    List<VocabularyEntity> getAllVocabulary();

    @Query("SELECT * FROM vocabulary ORDER BY word ASC")
    LiveData<List<VocabularyEntity>> getAllVocabularyLiveData();

    @Query("SELECT * FROM vocabulary WHERE languageId = :languageId ORDER BY word ASC")
    List<VocabularyEntity> getVocabularyByLanguage(String languageId);

    @Query("SELECT * FROM vocabulary WHERE languageId = :languageId ORDER BY word ASC")
    LiveData<List<VocabularyEntity>> getVocabularyByLanguageLiveData(String languageId);

    @Query("SELECT * FROM vocabulary WHERE lessonId = :lessonId ORDER BY word ASC")
    List<VocabularyEntity> getVocabularyForLesson(String lessonId);

    @Query("SELECT * FROM vocabulary WHERE vocabularyId = :vocabularyId LIMIT 1")
    VocabularyEntity getVocabularyById(String vocabularyId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(VocabularyEntity item);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<VocabularyEntity> items);

    @Update
    void update(VocabularyEntity item);

    @Delete
    void delete(VocabularyEntity item);

    @Query("SELECT COUNT(*) FROM vocabulary")
    int getCount();
}
