package com.vernacular.learning.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.vernacular.learning.data.local.entities.MediaReferenceEntity;
import java.util.List;

@Dao
public interface MediaReferenceDao {
    @Query("SELECT * FROM media_references WHERE relatedEntityId = :entityId")
    List<MediaReferenceEntity> getMediaForEntity(String entityId);

    @Query("SELECT * FROM media_references WHERE relatedEntityId = :entityId AND mediaType = :mediaType")
    List<MediaReferenceEntity> getMediaForEntityAndType(String entityId, String mediaType);

    @Query("SELECT * FROM media_references WHERE mediaId = :mediaId LIMIT 1")
    MediaReferenceEntity getMediaById(String mediaId);

    @Query("SELECT * FROM media_references WHERE downloadStatus = 'DOWNLOADED'")
    List<MediaReferenceEntity> getDownloadedMedia();

    @Query("SELECT * FROM media_references WHERE downloadStatus = 'DOWNLOADED'")
    LiveData<List<MediaReferenceEntity>> getDownloadedMediaLiveData();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(MediaReferenceEntity media);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<MediaReferenceEntity> mediaList);

    @Update
    void update(MediaReferenceEntity media);

    @Query("UPDATE media_references SET downloadStatus = :status, localFilePath = :filePath WHERE mediaId = :mediaId")
    void updateDownloadStatus(String mediaId, String status, String filePath);

    @Delete
    void delete(MediaReferenceEntity media);

    @Query("DELETE FROM media_references WHERE mediaId = :mediaId")
    void deleteById(String mediaId);

    @Query("SELECT COUNT(*) FROM media_references")
    int getCount();
}
