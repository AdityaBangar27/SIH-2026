package com.vernacular.learning.data.local;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.vernacular.learning.data.local.entities.DownloadItemEntity;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings({"unchecked", "deprecation"})
public final class DownloadDao_Impl implements DownloadDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<DownloadItemEntity> __insertionAdapterOfDownloadItemEntity;

  private final EntityDeletionOrUpdateAdapter<DownloadItemEntity> __updateAdapterOfDownloadItemEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateProgress;

  public DownloadDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfDownloadItemEntity = new EntityInsertionAdapter<DownloadItemEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `download_items` (`id`,`title`,`sizeDescription`,`sizeBytes`,`category`,`downloadStatus`,`progressPercent`,`localFilePath`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final DownloadItemEntity entity) {
        statement.bindLong(1, entity.id);
        if (entity.title == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.title);
        }
        if (entity.sizeDescription == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.sizeDescription);
        }
        statement.bindLong(4, entity.sizeBytes);
        if (entity.category == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.category);
        }
        if (entity.downloadStatus == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.downloadStatus);
        }
        statement.bindLong(7, entity.progressPercent);
        if (entity.localFilePath == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.localFilePath);
        }
      }
    };
    this.__updateAdapterOfDownloadItemEntity = new EntityDeletionOrUpdateAdapter<DownloadItemEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `download_items` SET `id` = ?,`title` = ?,`sizeDescription` = ?,`sizeBytes` = ?,`category` = ?,`downloadStatus` = ?,`progressPercent` = ?,`localFilePath` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final DownloadItemEntity entity) {
        statement.bindLong(1, entity.id);
        if (entity.title == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.title);
        }
        if (entity.sizeDescription == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.sizeDescription);
        }
        statement.bindLong(4, entity.sizeBytes);
        if (entity.category == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.category);
        }
        if (entity.downloadStatus == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.downloadStatus);
        }
        statement.bindLong(7, entity.progressPercent);
        if (entity.localFilePath == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.localFilePath);
        }
        statement.bindLong(9, entity.id);
      }
    };
    this.__preparedStmtOfUpdateProgress = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE download_items SET downloadStatus = ?, progressPercent = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public void insertAll(final List<DownloadItemEntity> items) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfDownloadItemEntity.insert(items);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void update(final DownloadItemEntity item) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfDownloadItemEntity.handle(item);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void updateProgress(final int id, final String status, final int progress) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateProgress.acquire();
    int _argIndex = 1;
    if (status == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, status);
    }
    _argIndex = 2;
    _stmt.bindLong(_argIndex, progress);
    _argIndex = 3;
    _stmt.bindLong(_argIndex, id);
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfUpdateProgress.release(_stmt);
    }
  }

  @Override
  public List<DownloadItemEntity> getAllDownloads() {
    final String _sql = "SELECT * FROM download_items ORDER BY id ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
      final int _cursorIndexOfSizeDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "sizeDescription");
      final int _cursorIndexOfSizeBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "sizeBytes");
      final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
      final int _cursorIndexOfDownloadStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "downloadStatus");
      final int _cursorIndexOfProgressPercent = CursorUtil.getColumnIndexOrThrow(_cursor, "progressPercent");
      final int _cursorIndexOfLocalFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "localFilePath");
      final List<DownloadItemEntity> _result = new ArrayList<DownloadItemEntity>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final DownloadItemEntity _item;
        final String _tmpTitle;
        if (_cursor.isNull(_cursorIndexOfTitle)) {
          _tmpTitle = null;
        } else {
          _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
        }
        final String _tmpSizeDescription;
        if (_cursor.isNull(_cursorIndexOfSizeDescription)) {
          _tmpSizeDescription = null;
        } else {
          _tmpSizeDescription = _cursor.getString(_cursorIndexOfSizeDescription);
        }
        final long _tmpSizeBytes;
        _tmpSizeBytes = _cursor.getLong(_cursorIndexOfSizeBytes);
        final String _tmpCategory;
        if (_cursor.isNull(_cursorIndexOfCategory)) {
          _tmpCategory = null;
        } else {
          _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
        }
        final String _tmpDownloadStatus;
        if (_cursor.isNull(_cursorIndexOfDownloadStatus)) {
          _tmpDownloadStatus = null;
        } else {
          _tmpDownloadStatus = _cursor.getString(_cursorIndexOfDownloadStatus);
        }
        final int _tmpProgressPercent;
        _tmpProgressPercent = _cursor.getInt(_cursorIndexOfProgressPercent);
        _item = new DownloadItemEntity(_tmpTitle,_tmpSizeDescription,_tmpSizeBytes,_tmpCategory,_tmpDownloadStatus,_tmpProgressPercent);
        _item.id = _cursor.getInt(_cursorIndexOfId);
        if (_cursor.isNull(_cursorIndexOfLocalFilePath)) {
          _item.localFilePath = null;
        } else {
          _item.localFilePath = _cursor.getString(_cursorIndexOfLocalFilePath);
        }
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public int getCount() {
    final String _sql = "SELECT COUNT(*) FROM download_items";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getInt(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
