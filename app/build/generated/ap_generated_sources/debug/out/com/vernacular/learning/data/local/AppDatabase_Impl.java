package com.vernacular.learning.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile LessonDao _lessonDao;

  private volatile DownloadDao _downloadDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `lessons` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `className` TEXT, `subject` TEXT, `chapterNumber` TEXT, `chapterTitle` TEXT, `hindiContent` TEXT, `motherTongueContent` TEXT, `motherTongue` TEXT, `instruction` TEXT, `visualType` TEXT, `lessonIndex` INTEGER NOT NULL, `totalLessons` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `download_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT, `sizeDescription` TEXT, `sizeBytes` INTEGER NOT NULL, `category` TEXT, `downloadStatus` TEXT, `progressPercent` INTEGER NOT NULL, `localFilePath` TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'be4b4887d35a60f5acc2d4b3050f46d6')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `lessons`");
        db.execSQL("DROP TABLE IF EXISTS `download_items`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsLessons = new HashMap<String, TableInfo.Column>(13);
        _columnsLessons.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLessons.put("className", new TableInfo.Column("className", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLessons.put("subject", new TableInfo.Column("subject", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLessons.put("chapterNumber", new TableInfo.Column("chapterNumber", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLessons.put("chapterTitle", new TableInfo.Column("chapterTitle", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLessons.put("hindiContent", new TableInfo.Column("hindiContent", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLessons.put("motherTongueContent", new TableInfo.Column("motherTongueContent", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLessons.put("motherTongue", new TableInfo.Column("motherTongue", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLessons.put("instruction", new TableInfo.Column("instruction", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLessons.put("visualType", new TableInfo.Column("visualType", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLessons.put("lessonIndex", new TableInfo.Column("lessonIndex", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLessons.put("totalLessons", new TableInfo.Column("totalLessons", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLessons.put("isCompleted", new TableInfo.Column("isCompleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysLessons = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesLessons = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoLessons = new TableInfo("lessons", _columnsLessons, _foreignKeysLessons, _indicesLessons);
        final TableInfo _existingLessons = TableInfo.read(db, "lessons");
        if (!_infoLessons.equals(_existingLessons)) {
          return new RoomOpenHelper.ValidationResult(false, "lessons(com.vernacular.learning.data.local.entities.LessonEntity).\n"
                  + " Expected:\n" + _infoLessons + "\n"
                  + " Found:\n" + _existingLessons);
        }
        final HashMap<String, TableInfo.Column> _columnsDownloadItems = new HashMap<String, TableInfo.Column>(8);
        _columnsDownloadItems.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloadItems.put("title", new TableInfo.Column("title", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloadItems.put("sizeDescription", new TableInfo.Column("sizeDescription", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloadItems.put("sizeBytes", new TableInfo.Column("sizeBytes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloadItems.put("category", new TableInfo.Column("category", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloadItems.put("downloadStatus", new TableInfo.Column("downloadStatus", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloadItems.put("progressPercent", new TableInfo.Column("progressPercent", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloadItems.put("localFilePath", new TableInfo.Column("localFilePath", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDownloadItems = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesDownloadItems = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoDownloadItems = new TableInfo("download_items", _columnsDownloadItems, _foreignKeysDownloadItems, _indicesDownloadItems);
        final TableInfo _existingDownloadItems = TableInfo.read(db, "download_items");
        if (!_infoDownloadItems.equals(_existingDownloadItems)) {
          return new RoomOpenHelper.ValidationResult(false, "download_items(com.vernacular.learning.data.local.entities.DownloadItemEntity).\n"
                  + " Expected:\n" + _infoDownloadItems + "\n"
                  + " Found:\n" + _existingDownloadItems);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "be4b4887d35a60f5acc2d4b3050f46d6", "0e55668f7591e7192a53b843bc6a16e2");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "lessons","download_items");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `lessons`");
      _db.execSQL("DELETE FROM `download_items`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(LessonDao.class, LessonDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(DownloadDao.class, DownloadDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public LessonDao lessonDao() {
    if (_lessonDao != null) {
      return _lessonDao;
    } else {
      synchronized(this) {
        if(_lessonDao == null) {
          _lessonDao = new LessonDao_Impl(this);
        }
        return _lessonDao;
      }
    }
  }

  @Override
  public DownloadDao downloadDao() {
    if (_downloadDao != null) {
      return _downloadDao;
    } else {
      synchronized(this) {
        if(_downloadDao == null) {
          _downloadDao = new DownloadDao_Impl(this);
        }
        return _downloadDao;
      }
    }
  }
}
