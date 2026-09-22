package com.vernacular.learning.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.vernacular.learning.data.local.entities.WorksheetEntity;
import java.util.List;

@Dao
public interface WorksheetDao {
    @Query("SELECT * FROM worksheets ORDER BY createdAt DESC")
    List<WorksheetEntity> getAllWorksheets();

    @Query("SELECT * FROM worksheets ORDER BY createdAt DESC")
    LiveData<List<WorksheetEntity>> getAllWorksheetsLiveData();

    @Query("SELECT * FROM worksheets WHERE className = :className AND subject = :subject ORDER BY createdAt DESC")
    List<WorksheetEntity> getWorksheetsByClassAndSubject(String className, String subject);

    @Query("SELECT * FROM worksheets WHERE className = :className AND subject = :subject ORDER BY createdAt DESC")
    LiveData<List<WorksheetEntity>> getWorksheetsByClassAndSubjectLiveData(String className, String subject);

    @Query("SELECT * FROM worksheets WHERE worksheetId = :worksheetId LIMIT 1")
    WorksheetEntity getWorksheetById(String worksheetId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(WorksheetEntity worksheet);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<WorksheetEntity> worksheets);

    @Update
    void update(WorksheetEntity worksheet);

    @Delete
    void delete(WorksheetEntity worksheet);

    @Query("DELETE FROM worksheets WHERE worksheetId = :worksheetId")
    void deleteById(String worksheetId);

    @Query("SELECT COUNT(*) FROM worksheets")
    int getCount();

    @Query("SELECT COUNT(*) FROM worksheets")
    LiveData<Integer> getCountLiveData();
}
