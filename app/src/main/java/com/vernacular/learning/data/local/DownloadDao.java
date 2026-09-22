package com.vernacular.learning.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
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

    @Query("SELECT * FROM download_items ORDER BY id ASC")
    LiveData<List<DownloadItemEntity>> getAllDownloadsLiveData();

    @Query("SELECT * FROM download_items WHERE id = :id LIMIT 1")
    DownloadItemEntity getDownloadById(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DownloadItemEntity item);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<DownloadItemEntity> items);

    @Update
    void update(DownloadItemEntity item);

    @Query("UPDATE download_items SET downloadStatus = :status, progressPercent = :progress WHERE id = :id")
    void updateProgress(int id, String status, int progress);

    @Delete
    void delete(DownloadItemEntity item);

    @Query("SELECT COUNT(*) FROM download_items")
    int getCount();

    @Query("SELECT COUNT(*) FROM download_items")
    LiveData<Integer> getCountLiveData();
}
