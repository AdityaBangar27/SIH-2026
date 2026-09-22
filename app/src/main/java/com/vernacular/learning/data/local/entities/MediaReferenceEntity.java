package com.vernacular.learning.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entity representing local app-managed media references (audio pronunciations, illustrations, diagrams).
 */
@Entity(
        tableName = "media_references",
        indices = {
                @Index(value = {"relatedEntityId"}),
                @Index(value = {"mediaType"}),
                @Index(value = {"downloadStatus"})
        }
)
public class MediaReferenceEntity {
    @PrimaryKey
    @NonNull
    public String mediaId;

    public String mediaType; // e.g. "IMAGE", "AUDIO", "PDF"
    public String relatedEntityId; // Lesson ID, Vocabulary ID, or Activity ID
    public String localFilePath; // Local file path or app-managed URI
    public String mimeType;
    public long fileSizeBytes;
    public String checksum;
    public int contentVersion;
    public String downloadStatus; // e.g. "DOWNLOADED", "NOT_DOWNLOADED", "DOWNLOADING", "ERROR"

    public MediaReferenceEntity(@NonNull String mediaId, String mediaType,
                                String relatedEntityId, String localFilePath,
                                String mimeType, long fileSizeBytes,
                                String checksum, int contentVersion,
                                String downloadStatus) {
        this.mediaId = mediaId;
        this.mediaType = mediaType;
        this.relatedEntityId = relatedEntityId;
        this.localFilePath = localFilePath;
        this.mimeType = mimeType;
        this.fileSizeBytes = fileSizeBytes;
        this.checksum = checksum;
        this.contentVersion = contentVersion;
        this.downloadStatus = downloadStatus;
    }
}
