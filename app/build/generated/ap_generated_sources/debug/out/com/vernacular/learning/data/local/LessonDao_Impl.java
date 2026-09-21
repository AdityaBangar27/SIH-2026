package com.vernacular.learning.data.local;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.vernacular.learning.data.local.entities.LessonEntity;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings({"unchecked", "deprecation"})
public final class LessonDao_Impl implements LessonDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<LessonEntity> __insertionAdapterOfLessonEntity;

  private final EntityDeletionOrUpdateAdapter<LessonEntity> __updateAdapterOfLessonEntity;

  public LessonDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfLessonEntity = new EntityInsertionAdapter<LessonEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `lessons` (`id`,`className`,`subject`,`chapterNumber`,`chapterTitle`,`hindiContent`,`motherTongueContent`,`motherTongue`,`instruction`,`visualType`,`lessonIndex`,`totalLessons`,`isCompleted`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final LessonEntity entity) {
        statement.bindLong(1, entity.id);
        if (entity.className == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.className);
        }
        if (entity.subject == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.subject);
        }
        if (entity.chapterNumber == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.chapterNumber);
        }
        if (entity.chapterTitle == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.chapterTitle);
        }
        if (entity.hindiContent == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.hindiContent);
        }
        if (entity.motherTongueContent == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.motherTongueContent);
        }
        if (entity.motherTongue == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.motherTongue);
        }
        if (entity.instruction == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.instruction);
        }
        if (entity.visualType == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.visualType);
        }
        statement.bindLong(11, entity.lessonIndex);
        statement.bindLong(12, entity.totalLessons);
        final int _tmp = entity.isCompleted ? 1 : 0;
        statement.bindLong(13, _tmp);
      }
    };
    this.__updateAdapterOfLessonEntity = new EntityDeletionOrUpdateAdapter<LessonEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `lessons` SET `id` = ?,`className` = ?,`subject` = ?,`chapterNumber` = ?,`chapterTitle` = ?,`hindiContent` = ?,`motherTongueContent` = ?,`motherTongue` = ?,`instruction` = ?,`visualType` = ?,`lessonIndex` = ?,`totalLessons` = ?,`isCompleted` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final LessonEntity entity) {
        statement.bindLong(1, entity.id);
        if (entity.className == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.className);
        }
        if (entity.subject == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.subject);
        }
        if (entity.chapterNumber == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.chapterNumber);
        }
        if (entity.chapterTitle == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.chapterTitle);
        }
        if (entity.hindiContent == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.hindiContent);
        }
        if (entity.motherTongueContent == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.motherTongueContent);
        }
        if (entity.motherTongue == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.motherTongue);
        }
        if (entity.instruction == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.instruction);
        }
        if (entity.visualType == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.visualType);
        }
        statement.bindLong(11, entity.lessonIndex);
        statement.bindLong(12, entity.totalLessons);
        final int _tmp = entity.isCompleted ? 1 : 0;
        statement.bindLong(13, _tmp);
        statement.bindLong(14, entity.id);
      }
    };
  }

  @Override
  public void insertAll(final List<LessonEntity> lessons) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfLessonEntity.insert(lessons);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void updateLesson(final LessonEntity lesson) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfLessonEntity.handle(lesson);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public List<LessonEntity> getLessons(final String className, final String subject,
      final String motherTongue) {
    final String _sql = "SELECT * FROM lessons WHERE className = ? AND subject = ? AND motherTongue = ? ORDER BY lessonIndex ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    if (className == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, className);
    }
    _argIndex = 2;
    if (subject == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, subject);
    }
    _argIndex = 3;
    if (motherTongue == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, motherTongue);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfClassName = CursorUtil.getColumnIndexOrThrow(_cursor, "className");
      final int _cursorIndexOfSubject = CursorUtil.getColumnIndexOrThrow(_cursor, "subject");
      final int _cursorIndexOfChapterNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "chapterNumber");
      final int _cursorIndexOfChapterTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "chapterTitle");
      final int _cursorIndexOfHindiContent = CursorUtil.getColumnIndexOrThrow(_cursor, "hindiContent");
      final int _cursorIndexOfMotherTongueContent = CursorUtil.getColumnIndexOrThrow(_cursor, "motherTongueContent");
      final int _cursorIndexOfMotherTongue = CursorUtil.getColumnIndexOrThrow(_cursor, "motherTongue");
      final int _cursorIndexOfInstruction = CursorUtil.getColumnIndexOrThrow(_cursor, "instruction");
      final int _cursorIndexOfVisualType = CursorUtil.getColumnIndexOrThrow(_cursor, "visualType");
      final int _cursorIndexOfLessonIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "lessonIndex");
      final int _cursorIndexOfTotalLessons = CursorUtil.getColumnIndexOrThrow(_cursor, "totalLessons");
      final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isCompleted");
      final List<LessonEntity> _result = new ArrayList<LessonEntity>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final LessonEntity _item;
        final String _tmpClassName;
        if (_cursor.isNull(_cursorIndexOfClassName)) {
          _tmpClassName = null;
        } else {
          _tmpClassName = _cursor.getString(_cursorIndexOfClassName);
        }
        final String _tmpSubject;
        if (_cursor.isNull(_cursorIndexOfSubject)) {
          _tmpSubject = null;
        } else {
          _tmpSubject = _cursor.getString(_cursorIndexOfSubject);
        }
        final String _tmpChapterNumber;
        if (_cursor.isNull(_cursorIndexOfChapterNumber)) {
          _tmpChapterNumber = null;
        } else {
          _tmpChapterNumber = _cursor.getString(_cursorIndexOfChapterNumber);
        }
        final String _tmpChapterTitle;
        if (_cursor.isNull(_cursorIndexOfChapterTitle)) {
          _tmpChapterTitle = null;
        } else {
          _tmpChapterTitle = _cursor.getString(_cursorIndexOfChapterTitle);
        }
        final String _tmpHindiContent;
        if (_cursor.isNull(_cursorIndexOfHindiContent)) {
          _tmpHindiContent = null;
        } else {
          _tmpHindiContent = _cursor.getString(_cursorIndexOfHindiContent);
        }
        final String _tmpMotherTongueContent;
        if (_cursor.isNull(_cursorIndexOfMotherTongueContent)) {
          _tmpMotherTongueContent = null;
        } else {
          _tmpMotherTongueContent = _cursor.getString(_cursorIndexOfMotherTongueContent);
        }
        final String _tmpMotherTongue;
        if (_cursor.isNull(_cursorIndexOfMotherTongue)) {
          _tmpMotherTongue = null;
        } else {
          _tmpMotherTongue = _cursor.getString(_cursorIndexOfMotherTongue);
        }
        final String _tmpInstruction;
        if (_cursor.isNull(_cursorIndexOfInstruction)) {
          _tmpInstruction = null;
        } else {
          _tmpInstruction = _cursor.getString(_cursorIndexOfInstruction);
        }
        final String _tmpVisualType;
        if (_cursor.isNull(_cursorIndexOfVisualType)) {
          _tmpVisualType = null;
        } else {
          _tmpVisualType = _cursor.getString(_cursorIndexOfVisualType);
        }
        final int _tmpLessonIndex;
        _tmpLessonIndex = _cursor.getInt(_cursorIndexOfLessonIndex);
        final int _tmpTotalLessons;
        _tmpTotalLessons = _cursor.getInt(_cursorIndexOfTotalLessons);
        _item = new LessonEntity(_tmpClassName,_tmpSubject,_tmpChapterNumber,_tmpChapterTitle,_tmpHindiContent,_tmpMotherTongueContent,_tmpMotherTongue,_tmpInstruction,_tmpVisualType,_tmpLessonIndex,_tmpTotalLessons);
        _item.id = _cursor.getInt(_cursorIndexOfId);
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfIsCompleted);
        _item.isCompleted = _tmp != 0;
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public LessonEntity getLessonById(final int id) {
    final String _sql = "SELECT * FROM lessons WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfClassName = CursorUtil.getColumnIndexOrThrow(_cursor, "className");
      final int _cursorIndexOfSubject = CursorUtil.getColumnIndexOrThrow(_cursor, "subject");
      final int _cursorIndexOfChapterNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "chapterNumber");
      final int _cursorIndexOfChapterTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "chapterTitle");
      final int _cursorIndexOfHindiContent = CursorUtil.getColumnIndexOrThrow(_cursor, "hindiContent");
      final int _cursorIndexOfMotherTongueContent = CursorUtil.getColumnIndexOrThrow(_cursor, "motherTongueContent");
      final int _cursorIndexOfMotherTongue = CursorUtil.getColumnIndexOrThrow(_cursor, "motherTongue");
      final int _cursorIndexOfInstruction = CursorUtil.getColumnIndexOrThrow(_cursor, "instruction");
      final int _cursorIndexOfVisualType = CursorUtil.getColumnIndexOrThrow(_cursor, "visualType");
      final int _cursorIndexOfLessonIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "lessonIndex");
      final int _cursorIndexOfTotalLessons = CursorUtil.getColumnIndexOrThrow(_cursor, "totalLessons");
      final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isCompleted");
      final LessonEntity _result;
      if (_cursor.moveToFirst()) {
        final String _tmpClassName;
        if (_cursor.isNull(_cursorIndexOfClassName)) {
          _tmpClassName = null;
        } else {
          _tmpClassName = _cursor.getString(_cursorIndexOfClassName);
        }
        final String _tmpSubject;
        if (_cursor.isNull(_cursorIndexOfSubject)) {
          _tmpSubject = null;
        } else {
          _tmpSubject = _cursor.getString(_cursorIndexOfSubject);
        }
        final String _tmpChapterNumber;
        if (_cursor.isNull(_cursorIndexOfChapterNumber)) {
          _tmpChapterNumber = null;
        } else {
          _tmpChapterNumber = _cursor.getString(_cursorIndexOfChapterNumber);
        }
        final String _tmpChapterTitle;
        if (_cursor.isNull(_cursorIndexOfChapterTitle)) {
          _tmpChapterTitle = null;
        } else {
          _tmpChapterTitle = _cursor.getString(_cursorIndexOfChapterTitle);
        }
        final String _tmpHindiContent;
        if (_cursor.isNull(_cursorIndexOfHindiContent)) {
          _tmpHindiContent = null;
        } else {
          _tmpHindiContent = _cursor.getString(_cursorIndexOfHindiContent);
        }
        final String _tmpMotherTongueContent;
        if (_cursor.isNull(_cursorIndexOfMotherTongueContent)) {
          _tmpMotherTongueContent = null;
        } else {
          _tmpMotherTongueContent = _cursor.getString(_cursorIndexOfMotherTongueContent);
        }
        final String _tmpMotherTongue;
        if (_cursor.isNull(_cursorIndexOfMotherTongue)) {
          _tmpMotherTongue = null;
        } else {
          _tmpMotherTongue = _cursor.getString(_cursorIndexOfMotherTongue);
        }
        final String _tmpInstruction;
        if (_cursor.isNull(_cursorIndexOfInstruction)) {
          _tmpInstruction = null;
        } else {
          _tmpInstruction = _cursor.getString(_cursorIndexOfInstruction);
        }
        final String _tmpVisualType;
        if (_cursor.isNull(_cursorIndexOfVisualType)) {
          _tmpVisualType = null;
        } else {
          _tmpVisualType = _cursor.getString(_cursorIndexOfVisualType);
        }
        final int _tmpLessonIndex;
        _tmpLessonIndex = _cursor.getInt(_cursorIndexOfLessonIndex);
        final int _tmpTotalLessons;
        _tmpTotalLessons = _cursor.getInt(_cursorIndexOfTotalLessons);
        _result = new LessonEntity(_tmpClassName,_tmpSubject,_tmpChapterNumber,_tmpChapterTitle,_tmpHindiContent,_tmpMotherTongueContent,_tmpMotherTongue,_tmpInstruction,_tmpVisualType,_tmpLessonIndex,_tmpTotalLessons);
        _result.id = _cursor.getInt(_cursorIndexOfId);
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfIsCompleted);
        _result.isCompleted = _tmp != 0;
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public int getCount() {
    final String _sql = "SELECT COUNT(*) FROM lessons";
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
