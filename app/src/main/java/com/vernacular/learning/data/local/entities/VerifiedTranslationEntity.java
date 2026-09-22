package com.vernacular.learning.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entity representing verified educational translations.
 * Stores only translations explicitly supplied and verified by authorized content sources.
 */
@Entity(
        tableName = "verified_translations",
        indices = {
                @Index(value = {"sourceLanguageId", "targetLanguageId"}),
                @Index(value = {"contentId"})
        }
)
public class VerifiedTranslationEntity {
    @PrimaryKey
    @NonNull
    public String translationId;

    public String contentId; // Related lesson or content ID, where applicable
    public String sourceLanguageId;
    public String targetLanguageId;
    public String originalText;
    public String translatedText;
    public String verificationStatus; // e.g. "VERIFIED", "PENDING_REVIEW"
    public String verifiedBy; // Authorized source reference
    public int contentVersion;
    public long updatedAt;

    public VerifiedTranslationEntity(@NonNull String translationId, String contentId,
                                     String sourceLanguageId, String targetLanguageId,
                                     String originalText, String translatedText,
                                     String verificationStatus, String verifiedBy,
                                     int contentVersion, long updatedAt) {
        this.translationId = translationId;
        this.contentId = contentId;
        this.sourceLanguageId = sourceLanguageId;
        this.targetLanguageId = targetLanguageId;
        this.originalText = originalText;
        this.translatedText = translatedText;
        this.verificationStatus = verificationStatus;
        this.verifiedBy = verifiedBy;
        this.contentVersion = contentVersion;
        this.updatedAt = updatedAt;
    }
}
