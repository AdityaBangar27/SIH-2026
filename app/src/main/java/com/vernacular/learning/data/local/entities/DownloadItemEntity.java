package com.vernacular.learning.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "download_items")
public class DownloadItemEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String sizeDescription;
    public long sizeBytes;
    public String category; // LessonPackages, TranslationModels, AudioFiles, Images
    public String downloadStatus; // NOT_DOWNLOADED, DOWNLOADING, DOWNLOADED, ERROR
    public int progressPercent;
    public String localFilePath;

    public DownloadItemEntity(String title, String sizeDescription, long sizeBytes,
                              String category, String downloadStatus, int progressPercent) {
        this.title = title;
        this.sizeDescription = sizeDescription;
        this.sizeBytes = sizeBytes;
        this.category = category;
        this.downloadStatus = downloadStatus;
        this.progressPercent = progressPercent;
    }
}
