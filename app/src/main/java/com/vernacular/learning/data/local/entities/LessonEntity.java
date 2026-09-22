package com.vernacular.learning.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entity representing an educational lesson.
 * Stores lesson content, structure, language metadata, versioning, and download status.
 */
@Entity(
        tableName = "lessons",
        indices = {
                @Index(value = {"className", "subject", "languageId"}),
                @Index(value = {"sequenceOrder"})
        }
)
public class LessonEntity {
    @PrimaryKey
    @NonNull
    public String lessonId;

    public String className;
    public String subject;
    public String chapterNumber;
    public String title;
    public String description;
    public String contentReference;
    public String hindiContent;
    public String motherTongueContent;
    public String languageId;
    public String motherTongue;
    public String instruction;
    public String visualType;
    public int sequenceOrder;
    public int totalLessons;
    public int contentVersion;
    public long createdAt;
    public long updatedAt;
    public String downloadStatus; // e.g. "DOWNLOADED", "AVAILABLE", "NOT_DOWNLOADED"
    public boolean isCompleted;

    public LessonEntity(@NonNull String lessonId, String className, String subject,
                        String chapterNumber, String title, String description,
                        String contentReference, String hindiContent, String motherTongueContent,
                        String languageId, String motherTongue, String instruction,
                        String visualType, int sequenceOrder, int totalLessons,
                        int contentVersion, long createdAt, long updatedAt,
                        String downloadStatus, boolean isCompleted) {
        this.lessonId = lessonId;
        this.className = className;
        this.subject = subject;
        this.chapterNumber = chapterNumber;
        this.title = title;
        this.description = description;
        this.contentReference = contentReference;
        this.hindiContent = hindiContent;
        this.motherTongueContent = motherTongueContent;
        this.languageId = languageId;
        this.motherTongue = motherTongue;
        this.instruction = instruction;
        this.visualType = visualType;
        this.sequenceOrder = sequenceOrder;
        this.totalLessons = totalLessons;
        this.contentVersion = contentVersion;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.downloadStatus = downloadStatus;
        this.isCompleted = isCompleted;
    }
}
