package com.vernacular.learning.data.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.vernacular.learning.data.local.AppDatabase;
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
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Main application repository managing local offline educational data.
 * Adheres strictly to the zero-sample-data requirement: no records are seeded automatically.
 * Database operations are safely executed on background worker threads.
 */
public class LearningRepository {
    private static volatile LearningRepository INSTANCE;
    private final AppDatabase database;
    private final ExecutorService executor;

    public interface Callback<T> {
        void onComplete(T result);
    }

    private LearningRepository(Context context) {
        database = AppDatabase.getInstance(context);
        executor = AppDatabase.databaseWriteExecutor;
        // Zero sample data: database begins completely empty on first installation.
    }

    public static LearningRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (LearningRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new LearningRepository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    public AppDatabase getDatabase() {
        return database;
    }

    // ==========================================
    // A. LESSONS
    // ==========================================
    public void getLessons(String className, String subject, String languageId, Callback<List<LessonEntity>> callback) {
        executor.execute(() -> {
            List<LessonEntity> lessons = database.lessonDao().getLessons(className, subject, languageId);
            if (callback != null) callback.onComplete(lessons);
        });
    }

    public LiveData<List<LessonEntity>> getLessonsLiveData(String className, String subject, String languageId) {
        return database.lessonDao().getLessonsLiveData(className, subject, languageId);
    }

    public void insertLesson(LessonEntity lesson) {
        executor.execute(() -> database.lessonDao().insert(lesson));
    }

    public void insertLessons(List<LessonEntity> lessons) {
        executor.execute(() -> database.lessonDao().insertAll(lessons));
    }

    public void deleteLesson(LessonEntity lesson) {
        executor.execute(() -> database.lessonDao().delete(lesson));
    }

    public void getLessonCount(Callback<Integer> callback) {
        executor.execute(() -> {
            int count = database.lessonDao().getCount();
            if (callback != null) callback.onComplete(count);
        });
    }

    // ==========================================
    // B. LANGUAGES
    // ==========================================
    public void getAllLanguages(Callback<List<LanguageEntity>> callback) {
        executor.execute(() -> {
            List<LanguageEntity> languages = database.languageDao().getAllLanguages();
            if (callback != null) callback.onComplete(languages);
        });
    }

    public LiveData<List<LanguageEntity>> getAllLanguagesLiveData() {
        return database.languageDao().getAllLanguagesLiveData();
    }

    public void insertLanguage(LanguageEntity language) {
        executor.execute(() -> database.languageDao().insert(language));
    }

    public void insertLanguages(List<LanguageEntity> languages) {
        executor.execute(() -> database.languageDao().insertAll(languages));
    }

    // ==========================================
    // C. VERIFIED TRANSLATIONS
    // ==========================================
    public void getTranslations(String sourceLangId, String targetLangId, Callback<List<VerifiedTranslationEntity>> callback) {
        executor.execute(() -> {
            List<VerifiedTranslationEntity> list = database.verifiedTranslationDao().getTranslations(sourceLangId, targetLangId);
            if (callback != null) callback.onComplete(list);
        });
    }

    public LiveData<List<VerifiedTranslationEntity>> getTranslationsLiveData(String sourceLangId, String targetLangId) {
        return database.verifiedTranslationDao().getTranslationsLiveData(sourceLangId, targetLangId);
    }

    public void insertTranslation(VerifiedTranslationEntity translation) {
        executor.execute(() -> database.verifiedTranslationDao().insert(translation));
    }

    public void insertTranslations(List<VerifiedTranslationEntity> translations) {
        executor.execute(() -> database.verifiedTranslationDao().insertAll(translations));
    }

    // ==========================================
    // D. VOCABULARY
    // ==========================================
    public void getAllVocabulary(Callback<List<VocabularyEntity>> callback) {
        executor.execute(() -> {
            List<VocabularyEntity> list = database.vocabularyDao().getAllVocabulary();
            if (callback != null) callback.onComplete(list);
        });
    }

    public LiveData<List<VocabularyEntity>> getAllVocabularyLiveData() {
        return database.vocabularyDao().getAllVocabularyLiveData();
    }

    public void getVocabularyByLanguage(String languageId, Callback<List<VocabularyEntity>> callback) {
        executor.execute(() -> {
            List<VocabularyEntity> list = database.vocabularyDao().getVocabularyByLanguage(languageId);
            if (callback != null) callback.onComplete(list);
        });
    }

    public LiveData<List<VocabularyEntity>> getVocabularyByLanguageLiveData(String languageId) {
        return database.vocabularyDao().getVocabularyByLanguageLiveData(languageId);
    }

    public void getVocabularyForLesson(String lessonId, Callback<List<VocabularyEntity>> callback) {
        executor.execute(() -> {
            List<VocabularyEntity> list = database.vocabularyDao().getVocabularyForLesson(lessonId);
            if (callback != null) callback.onComplete(list);
        });
    }

    public void insertVocabulary(VocabularyEntity item) {
        executor.execute(() -> database.vocabularyDao().insert(item));
    }

    public void insertVocabularyList(List<VocabularyEntity> items) {
        executor.execute(() -> database.vocabularyDao().insertAll(items));
    }

    // ==========================================
    // E. ACTIVITIES
    // ==========================================
    public void getActivitiesForLesson(String lessonId, Callback<List<ActivityEntity>> callback) {
        executor.execute(() -> {
            List<ActivityEntity> list = database.activityDao().getActivitiesForLesson(lessonId);
            if (callback != null) callback.onComplete(list);
        });
    }

    public LiveData<List<ActivityEntity>> getActivitiesForLessonLiveData(String lessonId) {
        return database.activityDao().getActivitiesForLessonLiveData(lessonId);
    }

    public void insertActivity(ActivityEntity activity) {
        executor.execute(() -> database.activityDao().insert(activity));
    }

    // ==========================================
    // F. ASSESSMENT QUESTIONS
    // ==========================================
    public void getQuestionsForLesson(String lessonId, Callback<List<AssessmentQuestionEntity>> callback) {
        executor.execute(() -> {
            List<AssessmentQuestionEntity> list = database.assessmentQuestionDao().getQuestionsForLesson(lessonId);
            if (callback != null) callback.onComplete(list);
        });
    }

    public LiveData<List<AssessmentQuestionEntity>> getQuestionsForLessonLiveData(String lessonId) {
        return database.assessmentQuestionDao().getQuestionsForLessonLiveData(lessonId);
    }

    public void insertQuestion(AssessmentQuestionEntity question) {
        executor.execute(() -> database.assessmentQuestionDao().insert(question));
    }

    // ==========================================
    // G. MEDIA REFERENCES
    // ==========================================
    public void getMediaForEntity(String entityId, Callback<List<MediaReferenceEntity>> callback) {
        executor.execute(() -> {
            List<MediaReferenceEntity> list = database.mediaReferenceDao().getMediaForEntity(entityId);
            if (callback != null) callback.onComplete(list);
        });
    }

    public LiveData<List<MediaReferenceEntity>> getDownloadedMediaLiveData() {
        return database.mediaReferenceDao().getDownloadedMediaLiveData();
    }

    public void insertMedia(MediaReferenceEntity media) {
        executor.execute(() -> database.mediaReferenceDao().insert(media));
    }

    // ==========================================
    // H. WORKSHEETS
    // ==========================================
    public void getAllWorksheets(Callback<List<WorksheetEntity>> callback) {
        executor.execute(() -> {
            List<WorksheetEntity> list = database.worksheetDao().getAllWorksheets();
            if (callback != null) callback.onComplete(list);
        });
    }

    public LiveData<List<WorksheetEntity>> getAllWorksheetsLiveData() {
        return database.worksheetDao().getAllWorksheetsLiveData();
    }

    public void getWorksheetsByClassAndSubject(String className, String subject, Callback<List<WorksheetEntity>> callback) {
        executor.execute(() -> {
            List<WorksheetEntity> list = database.worksheetDao().getWorksheetsByClassAndSubject(className, subject);
            if (callback != null) callback.onComplete(list);
        });
    }

    public void insertWorksheet(WorksheetEntity worksheet) {
        executor.execute(() -> database.worksheetDao().insert(worksheet));
    }

    public void insertWorksheets(List<WorksheetEntity> worksheets) {
        executor.execute(() -> database.worksheetDao().insertAll(worksheets));
    }

    // ==========================================
    // I. APPLICATION SETTINGS
    // ==========================================
    public void getSetting(String key, Callback<String> callback) {
        executor.execute(() -> {
            String val = database.appSettingDao().getSetting(key);
            if (callback != null) callback.onComplete(val);
        });
    }

    public LiveData<String> getSettingLiveData(String key) {
        return database.appSettingDao().getSettingLiveData(key);
    }

    public void saveSetting(String key, String value) {
        executor.execute(() -> database.appSettingDao().setSetting(new AppSettingEntity(key, value, System.currentTimeMillis())));
    }

    // ==========================================
    // J. SYNCHRONIZATION METADATA
    // ==========================================
    public void getSyncMetadata(String contentType, String contentId, Callback<SyncMetadataEntity> callback) {
        executor.execute(() -> {
            SyncMetadataEntity meta = database.syncMetadataDao().getMetadata(contentType, contentId);
            if (callback != null) callback.onComplete(meta);
        });
    }

    public void saveSyncMetadata(SyncMetadataEntity metadata) {
        executor.execute(() -> database.syncMetadataDao().insertOrUpdate(metadata));
    }

    // ==========================================
    // DOWNLOAD PACKAGES (App Packages)
    // ==========================================
    public void getAllDownloads(Callback<List<DownloadItemEntity>> callback) {
        executor.execute(() -> {
            List<DownloadItemEntity> list = database.downloadDao().getAllDownloads();
            if (callback != null) callback.onComplete(list);
        });
    }

    public LiveData<List<DownloadItemEntity>> getAllDownloadsLiveData() {
        return database.downloadDao().getAllDownloadsLiveData();
    }

    public void insertDownloadItem(DownloadItemEntity item) {
        executor.execute(() -> database.downloadDao().insert(item));
    }

    public void updateDownloadProgress(int id, String status, int progress) {
        executor.execute(() -> database.downloadDao().updateProgress(id, status, progress));
    }
}
