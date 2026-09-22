package com.vernacular.learning.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entity representing a language supported by the platform (e.g., Hindi, Santhali, Mundari, Ho).
 * Note: Table remains empty on installation until populated by an authorized source.
 */
@Entity(
        tableName = "languages",
        indices = {
                @Index(value = {"languageCode"}, unique = true)
        }
)
public class LanguageEntity {
    @PrimaryKey
    @NonNull
    public String languageId;

    public String languageName;
    public String languageCode;
    public String scriptInfo;
    public String availabilityStatus; // e.g. "AVAILABLE", "DOWNLOAD_REQUIRED"

    public LanguageEntity(@NonNull String languageId, String languageName, String languageCode,
                          String scriptInfo, String availabilityStatus) {
        this.languageId = languageId;
        this.languageName = languageName;
        this.languageCode = languageCode;
        this.scriptInfo = scriptInfo;
        this.availabilityStatus = availabilityStatus;
    }
}
