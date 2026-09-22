package com.vernacular.learning.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.vernacular.learning.data.local.entities.LanguageEntity;
import java.util.List;

@Dao
public interface LanguageDao {
    @Query("SELECT * FROM languages ORDER BY languageName ASC")
    List<LanguageEntity> getAllLanguages();

    @Query("SELECT * FROM languages ORDER BY languageName ASC")
    LiveData<List<LanguageEntity>> getAllLanguagesLiveData();

    @Query("SELECT * FROM languages WHERE languageId = :languageId LIMIT 1")
    LanguageEntity getLanguageById(String languageId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(LanguageEntity language);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<LanguageEntity> languages);

    @Update
    void update(LanguageEntity language);

    @Delete
    void delete(LanguageEntity language);

    @Query("SELECT COUNT(*) FROM languages")
    int getCount();
}
