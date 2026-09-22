package com.vernacular.learning.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import com.vernacular.learning.data.local.AppDatabase;
import com.vernacular.learning.data.local.entities.ActivityEntity;
import com.vernacular.learning.data.local.entities.AppSettingEntity;
import com.vernacular.learning.data.local.entities.AssessmentQuestionEntity;
import com.vernacular.learning.data.local.entities.LanguageEntity;
import com.vernacular.learning.data.local.entities.LessonEntity;
import com.vernacular.learning.data.local.entities.MediaReferenceEntity;
import com.vernacular.learning.data.local.entities.SyncMetadataEntity;
import com.vernacular.learning.data.local.entities.VerifiedTranslationEntity;
import com.vernacular.learning.data.local.entities.VocabularyEntity;
import com.vernacular.learning.data.local.entities.WorksheetEntity;
import java.io.IOException;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class DatabaseTest {
    private AppDatabase db;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        // In-memory database to isolate testing from any persisted production data
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
    }

    @After
    public void closeDb() throws IOException {
        db.close();
    }

    @Test
    public void testEmptyDatabaseOnCreation() {
        // Requirement 5: The database must be empty on first installation
        assertEquals(0, db.lessonDao().getCount());
        assertEquals(0, db.languageDao().getCount());
        assertEquals(0, db.verifiedTranslationDao().getCount());
        assertEquals(0, db.vocabularyDao().getCount());
        assertEquals(0, db.activityDao().getCount());
        assertEquals(0, db.assessmentQuestionDao().getCount());
        assertEquals(0, db.mediaReferenceDao().getCount());
        assertEquals(0, db.worksheetDao().getCount());
        assertEquals(0, db.appSettingDao().getAllSettings().size());
        assertEquals(0, db.syncMetadataDao().getAllMetadata().size());
        assertEquals(0, db.downloadDao().getCount());
    }

    @Test
    public void testLanguageOperations() {
        // Requirement B: Store languages without auto-populating
        LanguageEntity santhali = new LanguageEntity(
                "lang_sat", "Santhali", "sat", "Ol Chiki", "AVAILABLE"
        );
        LanguageEntity mundari = new LanguageEntity(
                "lang_unr", "Mundari", "unr", "Mundari Bani", "AVAILABLE"
        );
        LanguageEntity ho = new LanguageEntity(
                "lang_hoc", "Ho", "hoc", "Varang Kshiti", "AVAILABLE"
        );

        db.languageDao().insert(santhali);
        db.languageDao().insert(mundari);
        db.languageDao().insert(ho);

        assertEquals(3, db.languageDao().getCount());

        LanguageEntity fetched = db.languageDao().getLanguageById("lang_sat");
        assertNotNull(fetched);
        assertEquals("Santhali", fetched.languageName);
        assertEquals("Ol Chiki", fetched.scriptInfo);

        db.languageDao().delete(ho);
        assertEquals(2, db.languageDao().getCount());
    }

    @Test
    public void testLessonCrudAndFiltering() {
        // Requirement A: Store lessons and retrieve by class, subject, and language
        LessonEntity lesson1 = new LessonEntity(
                "lesson_c1_math_01", "Class 1", "Mathematics", "1",
                "Numbers 1-10", "Counting in mother tongue", "ref_lesson_01",
                "एक, दो, तीन", "ᱢᱤᱫ, ᱵᱟᱨ, ᱯᱮ", "lang_sat", "Santhali (ᱥᱟᱱᱛᱟᱲᱤ)",
                "Count objects aloud", "apples_3", 1, 5, 1,
                System.currentTimeMillis(), System.currentTimeMillis(),
                "DOWNLOADED", false
        );
        LessonEntity lesson2 = new LessonEntity(
                "lesson_c1_math_02", "Class 1", "Mathematics", "1",
                "Numbers 4-6", "Counting next group", "ref_lesson_02",
                "चार, पाँच, छह", "ᱯᱩᱱ, ᱢᱚᱬᱮ, ᱛᱩᱨᱩᱭ", "lang_sat", "Santhali (ᱥᱟᱱᱛᱟᱲᱤ)",
                "Count four, five, six", "apples_3", 2, 5, 1,
                System.currentTimeMillis(), System.currentTimeMillis(),
                "DOWNLOADED", false
        );
        LessonEntity lessonOtherSubject = new LessonEntity(
                "lesson_c1_evs_01", "Class 1", "EVS", "1",
                "Our Nature", "Trees and Plants", "ref_lesson_evs_01",
                "पेड़", "ᱫᱟᱨᱮ", "lang_sat", "Santhali (ᱥᱟᱱᱛᱟᱲᱤ)",
                "Identify trees", "leaf", 1, 3, 1,
                System.currentTimeMillis(), System.currentTimeMillis(),
                "DOWNLOADED", false
        );

        db.lessonDao().insert(lesson1);
        db.lessonDao().insert(lesson2);
        db.lessonDao().insert(lessonOtherSubject);

        assertEquals(3, db.lessonDao().getCount());

        // Filter by Class 1, Mathematics, lang_sat
        List<LessonEntity> mathLessons = db.lessonDao().getLessons("Class 1", "Mathematics", "lang_sat");
        assertEquals(2, mathLessons.size());
        assertEquals("Numbers 1-10", mathLessons.get(0).title);
        assertEquals("Numbers 4-6", mathLessons.get(1).title);

        // Update
        lesson1.isCompleted = true;
        db.lessonDao().updateLesson(lesson1);
        LessonEntity updated = db.lessonDao().getLessonById("lesson_c1_math_01");
        assertNotNull(updated);
        assertTrue(updated.isCompleted);

        // Safe Delete
        db.lessonDao().deleteById("lesson_c1_evs_01");
        assertEquals(2, db.lessonDao().getCount());
    }

    @Test
    public void testVerifiedTranslationOperations() {
        // Requirement C: Store only verified translations
        VerifiedTranslationEntity trans = new VerifiedTranslationEntity(
                "trans_001", "lesson_c1_math_01", "hi", "sat",
                "एक", "ᱢᱤᱫ", "VERIFIED", "Dept of Tribal Languages",
                1, System.currentTimeMillis()
        );

        db.verifiedTranslationDao().insert(trans);
        assertEquals(1, db.verifiedTranslationDao().getCount());

        List<VerifiedTranslationEntity> list = db.verifiedTranslationDao().getTranslations("hi", "sat");
        assertEquals(1, list.size());
        assertEquals("ᱢᱤᱫ", list.get(0).translatedText);
        assertEquals("VERIFIED", list.get(0).verificationStatus);
        assertEquals("Dept of Tribal Languages", list.get(0).verifiedBy);
    }

    @Test
    public void testVocabularyOperations() {
        // Requirement D: Store vocabulary
        VocabularyEntity vocab = new VocabularyEntity(
                "vocab_001", "lang_sat", "सेब", "Apple", "lesson_c1_math_01",
                "(Seb)", "media_apple_img", "media_apple_audio", 1
        );

        db.vocabularyDao().insert(vocab);
        assertEquals(1, db.vocabularyDao().getCount());

        List<VocabularyEntity> byLang = db.vocabularyDao().getVocabularyByLanguage("lang_sat");
        assertEquals(1, byLang.size());
        assertEquals("सेब", byLang.get(0).word);
        assertEquals("Apple", byLang.get(0).meaning);
        assertEquals("media_apple_audio", byLang.get(0).audioReference);
    }

    @Test
    public void testActivitiesAndQuestions() {
        // Requirements E & F: Activities and assessment questions
        ActivityEntity act = new ActivityEntity(
                "act_001", "lesson_c1_math_01", "Count the Apples",
                "Tap each apple to count aloud", "COUNTING", "ref_apples_activity",
                1, 1
        );
        db.activityDao().insert(act);
        assertEquals(1, db.activityDao().getCount());

        AssessmentQuestionEntity q = new AssessmentQuestionEntity(
                "q_001", "lesson_c1_math_01", "How many apples are shown?",
                "MULTIPLE_CHOICE", "[\"1\",\"2\",\"3\"]", "3",
                "There are 3 green apples", 1, 1, 1
        );
        db.assessmentQuestionDao().insert(q);
        assertEquals(1, db.assessmentQuestionDao().getCount());

        List<AssessmentQuestionEntity> questions = db.assessmentQuestionDao().getQuestionsForLesson("lesson_c1_math_01");
        assertEquals(1, questions.size());
        assertEquals("3", questions.get(0).correctAnswer);
    }

    @Test
    public void testMediaReferences() {
        // Requirement G: Store media references
        MediaReferenceEntity media = new MediaReferenceEntity(
                "media_audio_001", "AUDIO", "vocab_001",
                "/app/storage/audio/apple_santhali.mp3", "audio/mpeg",
                45000L, "checksum_abc123", 1, "DOWNLOADED"
        );

        db.mediaReferenceDao().insert(media);
        assertEquals(1, db.mediaReferenceDao().getCount());

        MediaReferenceEntity fetched = db.mediaReferenceDao().getMediaById("media_audio_001");
        assertNotNull(fetched);
        assertEquals("DOWNLOADED", fetched.downloadStatus);
        assertEquals(45000L, fetched.fileSizeBytes);

        // Update download status
        db.mediaReferenceDao().updateDownloadStatus("media_audio_001", "NOT_DOWNLOADED", "");
        MediaReferenceEntity updated = db.mediaReferenceDao().getMediaById("media_audio_001");
        assertEquals("NOT_DOWNLOADED", updated.downloadStatus);
    }

    @Test
    public void testWorksheetOperations() {
        // Requirement H: Store worksheets
        WorksheetEntity ws = new WorksheetEntity(
                "ws_c1_math_01", "Numbers Counting Worksheet 1", "lesson_c1_math_01",
                "Class 1", "Mathematics", "lang_sat",
                "/app/storage/worksheets/c1_math_01.pdf",
                System.currentTimeMillis(), 1, "DOWNLOADED"
        );

        db.worksheetDao().insert(ws);
        assertEquals(1, db.worksheetDao().getCount());

        List<WorksheetEntity> list = db.worksheetDao().getWorksheetsByClassAndSubject("Class 1", "Mathematics");
        assertEquals(1, list.size());
        assertEquals("Numbers Counting Worksheet 1", list.get(0).title);

        db.worksheetDao().deleteById("ws_c1_math_01");
        assertEquals(0, db.worksheetDao().getCount());
    }

    @Test
    public void testAppSettingsOperations() {
        // Requirement I: Store application settings
        db.appSettingDao().setSetting(new AppSettingEntity("selected_language", "Santhali", System.currentTimeMillis()));
        db.appSettingDao().setSetting(new AppSettingEntity("app_theme_mode", "DARK", System.currentTimeMillis()));

        assertEquals("Santhali", db.appSettingDao().getSetting("selected_language"));
        assertEquals("DARK", db.appSettingDao().getSetting("app_theme_mode"));
        assertNull(db.appSettingDao().getSetting("unknown_key"));
    }

    @Test
    public void testSyncMetadataOperations() {
        // Requirement J: Synchronization metadata
        SyncMetadataEntity meta = new SyncMetadataEntity(
                "LESSON", "lesson_c1_math_01", 1,
                System.currentTimeMillis(), "SYNCED", null
        );

        db.syncMetadataDao().insertOrUpdate(meta);
        SyncMetadataEntity fetched = db.syncMetadataDao().getMetadata("LESSON", "lesson_c1_math_01");
        assertNotNull(fetched);
        assertEquals("SYNCED", fetched.syncStatus);
        assertEquals(1, fetched.contentVersion);

        // Update with error
        meta.syncStatus = "FAILED";
        meta.syncErrorMessage = "Network timeout";
        db.syncMetadataDao().insertOrUpdate(meta);

        SyncMetadataEntity failed = db.syncMetadataDao().getMetadata("LESSON", "lesson_c1_math_01");
        assertEquals("FAILED", failed.syncStatus);
        assertEquals("Network timeout", failed.syncErrorMessage);
    }

    @Test
    public void testDuplicateHandlingUsingReplaceStrategy() {
        // Conflict resolution with stable IDs
        LanguageEntity l1 = new LanguageEntity("hi", "Hindi", "hi", "Devanagari", "AVAILABLE");
        db.languageDao().insert(l1);
        assertEquals(1, db.languageDao().getCount());

        // Re-insert same ID with updated info
        LanguageEntity l2 = new LanguageEntity("hi", "Hindi Updated", "hi", "Devanagari", "AVAILABLE");
        db.languageDao().insert(l2);
        // Count remains 1, replaced safely
        assertEquals(1, db.languageDao().getCount());
        assertEquals("Hindi Updated", db.languageDao().getLanguageById("hi").languageName);
    }
}
