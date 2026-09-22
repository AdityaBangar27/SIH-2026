package com.vernacular.learning.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.vernacular.learning.data.local.entities.VerifiedTranslationEntity;
import java.util.List;

@Dao
public interface VerifiedTranslationDao {
    @Query("SELECT * FROM verified_translations WHERE contentId = :contentId")
    List<VerifiedTranslationEntity> getTranslationsForContent(String contentId);

    @Query("SELECT * FROM verified_translations WHERE sourceLanguageId = :sourceLangId AND targetLanguageId = :targetLangId")
    List<VerifiedTranslationEntity> getTranslations(String sourceLangId, String targetLangId);

    @Query("SELECT * FROM verified_translations WHERE sourceLanguageId = :sourceLangId AND targetLanguageId = :targetLangId")
    LiveData<List<VerifiedTranslationEntity>> getTranslationsLiveData(String sourceLangId, String targetLangId);

    @Query("SELECT * FROM verified_translations WHERE translationId = :translationId LIMIT 1")
    VerifiedTranslationEntity getTranslationById(String translationId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(VerifiedTranslationEntity translation);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<VerifiedTranslationEntity> translations);

    @Update
    void update(VerifiedTranslationEntity translation);

    @Delete
    void delete(VerifiedTranslationEntity translation);

    @Query("SELECT COUNT(*) FROM verified_translations")
    int getCount();
}
