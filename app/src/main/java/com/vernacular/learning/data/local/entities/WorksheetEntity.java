package com.vernacular.learning.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entity representing educational printable and digital worksheets.
 */
@Entity(
        tableName = "worksheets",
        indices = {
                @Index(value = {"className", "subject"}),
                @Index(value = {"languageId"}),
                @Index(value = {"lessonId"})
        }
)
public class WorksheetEntity {
    @PrimaryKey
    @NonNull
    public String worksheetId;

    public String title;
    public String lessonId; // Optional link to specific lesson
    public String className;
    public String subject;
    public String languageId;
    public String pdfFilePath; // Local file path or content URI
    public long createdAt;
    public int contentVersion;
    public String downloadStatus; // e.g. "DOWNLOADED", "NOT_DOWNLOADED"

    public WorksheetEntity(@NonNull String worksheetId, String title, String lessonId,
                           String className, String subject, String languageId,
                           String pdfFilePath, long createdAt, int contentVersion,
                           String downloadStatus) {
        this.worksheetId = worksheetId;
        this.title = title;
        this.lessonId = lessonId;
        this.className = className;
        this.subject = subject;
        this.languageId = languageId;
        this.pdfFilePath = pdfFilePath;
        this.createdAt = createdAt;
        this.contentVersion = contentVersion;
        this.downloadStatus = downloadStatus;
    }
}
