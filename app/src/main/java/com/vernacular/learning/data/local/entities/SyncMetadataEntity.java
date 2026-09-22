package com.vernacular.learning.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;

/**
 * Entity tracking content synchronization metadata for future server / offline synchronization sync.
 */
@Entity(
        tableName = "sync_metadata",
        primaryKeys = {"contentType", "contentId"},
        indices = {
                @Index(value = {"contentType"}),
                @Index(value = {"syncStatus"})
        }
)
public class SyncMetadataEntity {
    @NonNull
    public String contentType; // e.g. "LESSON", "VOCABULARY", "WORKSHEET", "LANGUAGE", "MEDIA"

    @NonNull
    public String contentId;

    public int contentVersion;
    public long lastSyncTimestamp;
    public String syncStatus; // e.g. "SYNCED", "PENDING_DOWNLOAD", "PENDING_UPLOAD", "FAILED"
    public String syncErrorMessage;

    public SyncMetadataEntity(@NonNull String contentType, @NonNull String contentId,
                              int contentVersion, long lastSyncTimestamp,
                              String syncStatus, String syncErrorMessage) {
        this.contentType = contentType;
        this.contentId = contentId;
        this.contentVersion = contentVersion;
        this.lastSyncTimestamp = lastSyncTimestamp;
        this.syncStatus = syncStatus;
        this.syncErrorMessage = syncErrorMessage;
    }
}
