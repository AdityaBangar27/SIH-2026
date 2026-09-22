package com.vernacular.learning.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.vernacular.learning.data.local.entities.SyncMetadataEntity;
import java.util.List;

@Dao
public interface SyncMetadataDao {
    @Query("SELECT * FROM sync_metadata WHERE contentType = :contentType AND contentId = :contentId LIMIT 1")
    SyncMetadataEntity getMetadata(String contentType, String contentId);

    @Query("SELECT * FROM sync_metadata WHERE contentType = :contentType AND contentId = :contentId LIMIT 1")
    LiveData<SyncMetadataEntity> getMetadataLiveData(String contentType, String contentId);

    @Query("SELECT * FROM sync_metadata WHERE syncStatus = :status")
    List<SyncMetadataEntity> getItemsByStatus(String status);

    @Query("SELECT * FROM sync_metadata WHERE syncStatus = :status")
    LiveData<List<SyncMetadataEntity>> getItemsByStatusLiveData(String status);

    @Query("SELECT * FROM sync_metadata")
    List<SyncMetadataEntity> getAllMetadata();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(SyncMetadataEntity metadata);

    @Query("DELETE FROM sync_metadata WHERE contentType = :contentType AND contentId = :contentId")
    void deleteMetadata(String contentType, String contentId);
}
