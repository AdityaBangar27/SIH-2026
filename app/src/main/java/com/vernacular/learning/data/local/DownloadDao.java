package com.vernacular.learning.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.vernacular.learning.data.local.entities.DownloadItemEntity;
import java.util.List;

@Dao
public interface DownloadDao {
    @Query("SELECT * FROM download_items ORDER BY id ASC")
    List<DownloadItemEntity> getAllDownloads();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<DownloadItemEntity> items);

    @Update
    void update(DownloadItemEntity item);

    @Query("UPDATE download_items SET downloadStatus = :status, progressPercent = :progress WHERE id = :id")
    void updateProgress(int id, String status, int progress);

    @Query("SELECT COUNT(*) FROM download_items")
    int getCount();
}
