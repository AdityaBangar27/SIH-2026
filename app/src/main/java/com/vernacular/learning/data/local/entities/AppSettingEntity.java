package com.vernacular.learning.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entity representing key-value local application settings and user preferences.
 * Persists selections such as active language, class, subject, and theme.
 */
@Entity(tableName = "app_settings")
public class AppSettingEntity {
    @PrimaryKey
    @NonNull
    public String settingKey;

    public String settingValue;
    public long updatedAt;

    public AppSettingEntity(@NonNull String settingKey, String settingValue, long updatedAt) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
        this.updatedAt = updatedAt;
    }
}
