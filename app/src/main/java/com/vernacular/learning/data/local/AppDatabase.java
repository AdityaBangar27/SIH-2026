package com.vernacular.learning.data.local;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.vernacular.learning.data.local.entities.ActivityEntity;
import com.vernacular.learning.data.local.entities.AppSettingEntity;
import com.vernacular.learning.data.local.entities.AssessmentQuestionEntity;
import com.vernacular.learning.data.local.entities.DownloadItemEntity;
import com.vernacular.learning.data.local.entities.LanguageEntity;
import com.vernacular.learning.data.local.entities.LessonEntity;
import com.vernacular.learning.data.local.entities.MediaReferenceEntity;
import com.vernacular.learning.data.local.entities.SyncMetadataEntity;
import com.vernacular.learning.data.local.entities.VerifiedTranslationEntity;
import com.vernacular.learning.data.local.entities.VocabularyEntity;
import com.vernacular.learning.data.local.entities.WorksheetEntity;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
        entities = {
                LessonEntity.class,
                LanguageEntity.class,
                VerifiedTranslationEntity.class,
                VocabularyEntity.class,
                ActivityEntity.class,
                AssessmentQuestionEntity.class,
                MediaReferenceEntity.class,
                WorksheetEntity.class,
                AppSettingEntity.class,
                SyncMetadataEntity.class,
                DownloadItemEntity.class
        },
        version = 2,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "vernacular_learning_db";
    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public abstract LessonDao lessonDao();
    public abstract LanguageDao languageDao();
    public abstract VerifiedTranslationDao verifiedTranslationDao();
    public abstract VocabularyDao vocabularyDao();
    public abstract ActivityDao activityDao();
    public abstract AssessmentQuestionDao assessmentQuestionDao();
    public abstract MediaReferenceDao mediaReferenceDao();
    public abstract WorksheetDao worksheetDao();
    public abstract AppSettingDao appSettingDao();
    public abstract SyncMetadataDao syncMetadataDao();
    public abstract DownloadDao downloadDao();

    /**
     * Non-destructive migration from version 1 to 2.
     * Preserves existing downloads and upgrades lessons to full metadata format without data loss.
     */
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 1. Upgrade lessons table to new schema
            database.execSQL("CREATE TABLE IF NOT EXISTS `lessons_new` (" +
                    "`lessonId` TEXT NOT NULL PRIMARY KEY, " +
                    "`className` TEXT, " +
                    "`subject` TEXT, " +
                    "`chapterNumber` TEXT, " +
                    "`title` TEXT, " +
                    "`description` TEXT, " +
                    "`contentReference` TEXT, " +
                    "`hindiContent` TEXT, " +
                    "`motherTongueContent` TEXT, " +
                    "`languageId` TEXT, " +
                    "`motherTongue` TEXT, " +
                    "`instruction` TEXT, " +
                    "`visualType` TEXT, " +
                    "`sequenceOrder` INTEGER NOT NULL, " +
                    "`totalLessons` INTEGER NOT NULL, " +
                    "`contentVersion` INTEGER NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL, " +
                    "`updatedAt` INTEGER NOT NULL, " +
                    "`downloadStatus` TEXT, " +
                    "`isCompleted` INTEGER NOT NULL)");

            // Migrate any existing lessons from v1
            database.execSQL("INSERT OR IGNORE INTO `lessons_new` (" +
                    "`lessonId`, `className`, `subject`, `chapterNumber`, `title`, " +
                    "`description`, `contentReference`, `hindiContent`, `motherTongueContent`, " +
                    "`languageId`, `motherTongue`, `instruction`, `visualType`, `sequenceOrder`, " +
                    "`totalLessons`, `contentVersion`, `createdAt`, `updatedAt`, `downloadStatus`, `isCompleted`) " +
                    "SELECT CAST(id AS TEXT), className, subject, chapterNumber, chapterTitle, " +
                    "'' as description, '' as contentReference, hindiContent, motherTongueContent, " +
                    "motherTongue as languageId, motherTongue, instruction, visualType, lessonIndex as sequenceOrder, " +
                    "totalLessons, 1 as contentVersion, 0 as createdAt, 0 as updatedAt, 'DOWNLOADED' as downloadStatus, isCompleted FROM `lessons`");

            database.execSQL("DROP TABLE IF EXISTS `lessons`");
            database.execSQL("ALTER TABLE `lessons_new` RENAME TO `lessons`");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_lessons_className_subject_languageId` ON `lessons` (`className`, `subject`, `languageId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_lessons_sequenceOrder` ON `lessons` (`sequenceOrder`)");

            // 2. Create languages table
            database.execSQL("CREATE TABLE IF NOT EXISTS `languages` (" +
                    "`languageId` TEXT NOT NULL PRIMARY KEY, " +
                    "`languageName` TEXT, " +
                    "`languageCode` TEXT, " +
                    "`scriptInfo` TEXT, " +
                    "`availabilityStatus` TEXT)");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_languages_languageCode` ON `languages` (`languageCode`)");

            // 3. Create verified_translations table
            database.execSQL("CREATE TABLE IF NOT EXISTS `verified_translations` (" +
                    "`translationId` TEXT NOT NULL PRIMARY KEY, " +
                    "`contentId` TEXT, " +
                    "`sourceLanguageId` TEXT, " +
                    "`targetLanguageId` TEXT, " +
                    "`originalText` TEXT, " +
                    "`translatedText` TEXT, " +
                    "`verificationStatus` TEXT, " +
                    "`verifiedBy` TEXT, " +
                    "`contentVersion` INTEGER NOT NULL, " +
                    "`updatedAt` INTEGER NOT NULL)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_verified_translations_sourceLanguageId_targetLanguageId` ON `verified_translations` (`sourceLanguageId`, `targetLanguageId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_verified_translations_contentId` ON `verified_translations` (`contentId`)");

            // 4. Create vocabulary table
            database.execSQL("CREATE TABLE IF NOT EXISTS `vocabulary` (" +
                    "`vocabularyId` TEXT NOT NULL PRIMARY KEY, " +
                    "`languageId` TEXT, " +
                    "`word` TEXT, " +
                    "`meaning` TEXT, " +
                    "`lessonId` TEXT, " +
                    "`transliteration` TEXT, " +
                    "`imageReference` TEXT, " +
                    "`audioReference` TEXT, " +
                    "`contentVersion` INTEGER NOT NULL)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_vocabulary_languageId` ON `vocabulary` (`languageId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_vocabulary_lessonId` ON `vocabulary` (`lessonId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_vocabulary_word` ON `vocabulary` (`word`)");

            // 5. Create activities table
            database.execSQL("CREATE TABLE IF NOT EXISTS `activities` (" +
                    "`activityId` TEXT NOT NULL PRIMARY KEY, " +
                    "`lessonId` TEXT, " +
                    "`title` TEXT, " +
                    "`instructions` TEXT, " +
                    "`activityType` TEXT, " +
                    "`contentReference` TEXT, " +
                    "`sequenceOrder` INTEGER NOT NULL, " +
                    "`contentVersion` INTEGER NOT NULL)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_activities_lessonId` ON `activities` (`lessonId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_activities_sequenceOrder` ON `activities` (`sequenceOrder`)");

            // 6. Create assessment_questions table
            database.execSQL("CREATE TABLE IF NOT EXISTS `assessment_questions` (" +
                    "`questionId` TEXT NOT NULL PRIMARY KEY, " +
                    "`lessonId` TEXT, " +
                    "`questionText` TEXT, " +
                    "`questionType` TEXT, " +
                    "`optionsData` TEXT, " +
                    "`correctAnswer` TEXT, " +
                    "`explanation` TEXT, " +
                    "`marks` INTEGER NOT NULL, " +
                    "`sequenceOrder` INTEGER NOT NULL, " +
                    "`contentVersion` INTEGER NOT NULL)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_assessment_questions_lessonId` ON `assessment_questions` (`lessonId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_assessment_questions_sequenceOrder` ON `assessment_questions` (`sequenceOrder`)");

            // 7. Create media_references table
            database.execSQL("CREATE TABLE IF NOT EXISTS `media_references` (" +
                    "`mediaId` TEXT NOT NULL PRIMARY KEY, " +
                    "`mediaType` TEXT, " +
                    "`relatedEntityId` TEXT, " +
                    "`localFilePath` TEXT, " +
                    "`mimeType` TEXT, " +
                    "`fileSizeBytes` INTEGER NOT NULL, " +
                    "`checksum` TEXT, " +
                    "`contentVersion` INTEGER NOT NULL, " +
                    "`downloadStatus` TEXT)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_media_references_relatedEntityId` ON `media_references` (`relatedEntityId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_media_references_mediaType` ON `media_references` (`mediaType`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_media_references_downloadStatus` ON `media_references` (`downloadStatus`)");

            // 8. Create worksheets table
            database.execSQL("CREATE TABLE IF NOT EXISTS `worksheets` (" +
                    "`worksheetId` TEXT NOT NULL PRIMARY KEY, " +
                    "`title` TEXT, " +
                    "`lessonId` TEXT, " +
                    "`className` TEXT, " +
                    "`subject` TEXT, " +
                    "`languageId` TEXT, " +
                    "`pdfFilePath` TEXT, " +
                    "`createdAt` INTEGER NOT NULL, " +
                    "`contentVersion` INTEGER NOT NULL, " +
                    "`downloadStatus` TEXT)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_worksheets_className_subject` ON `worksheets` (`className`, `subject`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_worksheets_languageId` ON `worksheets` (`languageId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_worksheets_lessonId` ON `worksheets` (`lessonId`)");

            // 9. Create app_settings table
            database.execSQL("CREATE TABLE IF NOT EXISTS `app_settings` (" +
                    "`settingKey` TEXT NOT NULL PRIMARY KEY, " +
                    "`settingValue` TEXT, " +
                    "`updatedAt` INTEGER NOT NULL)");

            // 10. Create sync_metadata table
            database.execSQL("CREATE TABLE IF NOT EXISTS `sync_metadata` (" +
                    "`contentType` TEXT NOT NULL, " +
                    "`contentId` TEXT NOT NULL, " +
                    "`contentVersion` INTEGER NOT NULL, " +
                    "`lastSyncTimestamp` INTEGER NOT NULL, " +
                    "`syncStatus` TEXT, " +
                    "`syncErrorMessage` TEXT, " +
                    "PRIMARY KEY(`contentType`, `contentId`))");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_metadata_contentType` ON `sync_metadata` (`contentType`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_metadata_syncStatus` ON `sync_metadata` (`syncStatus`)");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    )
                    .addMigrations(MIGRATION_1_2)
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
