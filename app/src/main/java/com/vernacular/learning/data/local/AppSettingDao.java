package com.vernacular.learning.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.vernacular.learning.data.local.entities.AppSettingEntity;
import java.util.List;

@Dao
public interface AppSettingDao {
    @Query("SELECT settingValue FROM app_settings WHERE settingKey = :key LIMIT 1")
    String getSetting(String key);

    @Query("SELECT settingValue FROM app_settings WHERE settingKey = :key LIMIT 1")
    LiveData<String> getSettingLiveData(String key);

    @Query("SELECT * FROM app_settings")
    List<AppSettingEntity> getAllSettings();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void setSetting(AppSettingEntity setting);

    @Query("DELETE FROM app_settings WHERE settingKey = :key")
    void deleteSetting(String key);
}
