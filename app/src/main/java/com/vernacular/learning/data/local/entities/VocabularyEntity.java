package com.vernacular.learning.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entity representing educational vocabulary terms, word mappings, and pronunciation references.
 */
@Entity(
        tableName = "vocabulary",
        indices = {
                @Index(value = {"languageId"}),
                @Index(value = {"lessonId"}),
                @Index(value = {"word"})
        }
)
public class VocabularyEntity {
    @PrimaryKey
    @NonNull
    public String vocabularyId;

    public String languageId;
    public String word;
    public String meaning;
    public String lessonId; // Nullable if general vocabulary
    public String transliteration; // Optional pronunciation phonetic guide
    public String imageReference; // App-managed media ID or path
    public String audioReference; // App-managed media ID or path
    public int contentVersion;

    public VocabularyEntity(@NonNull String vocabularyId, String languageId, String word,
                            String meaning, String lessonId, String transliteration,
                            String imageReference, String audioReference, int contentVersion) {
        this.vocabularyId = vocabularyId;
        this.languageId = languageId;
        this.word = word;
        this.meaning = meaning;
        this.lessonId = lessonId;
        this.transliteration = transliteration;
        this.imageReference = imageReference;
        this.audioReference = audioReference;
        this.contentVersion = contentVersion;
    }
}
