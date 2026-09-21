package com.vernacular.learning.data.repository;

import android.content.Context;
import com.vernacular.learning.R;
import com.vernacular.learning.data.local.AppDatabase;
import com.vernacular.learning.data.local.entities.DownloadItemEntity;
import com.vernacular.learning.data.local.entities.LessonEntity;
import com.vernacular.learning.data.models.FlashcardItem;
import com.vernacular.learning.data.models.MaterialItem;
import com.vernacular.learning.data.models.WorksheetItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class LearningRepository {
    private static volatile LearningRepository INSTANCE;
    private final AppDatabase database;

    private LearningRepository(Context context) {
        database = AppDatabase.getInstance(context);
        seedInitialDataIfEmpty();
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

    private void seedInitialDataIfEmpty() {
        Executors.newSingleThreadExecutor().execute(() -> {
            if (database.downloadDao().getCount() == 0) {
                List<DownloadItemEntity> downloads = new ArrayList<>();
                downloads.add(new DownloadItemEntity("Lesson Packages", "120 MB", 120 * 1024 * 1024L, "LessonPackages", "NOT_DOWNLOADED", 0));
                downloads.add(new DownloadItemEntity("Translation Models", "250 MB", 250 * 1024 * 1024L, "TranslationModels", "NOT_DOWNLOADED", 0));
                downloads.add(new DownloadItemEntity("Audio Files", "180 MB", 180 * 1024 * 1024L, "AudioFiles", "NOT_DOWNLOADED", 0));
                downloads.add(new DownloadItemEntity("Images & Resources", "95 MB", 95 * 1024 * 1024L, "Images", "NOT_DOWNLOADED", 0));
                database.downloadDao().insertAll(downloads);
            }

            if (database.lessonDao().getCount() == 0) {
                List<LessonEntity> lessons = new ArrayList<>();
                lessons.add(new LessonEntity(
                        "Class 1", "Mathematics", "1", "Numbers and Counting",
                        "एक, दो, तीन", "ᱮᱠ, ᱫᱳ, ᱛᱤᱱ (ᱢᱤᱫ, ᱵᱟᱨ, ᱯᱮ)", "Santhali (ᱥᱟᱱᱛᱟᱲᱤ)",
                        "Let's count the objects. Count and say aloud.", "apples_3", 1, 5
                ));
                lessons.add(new LessonEntity(
                        "Class 1", "Mathematics", "1", "Numbers and Counting",
                        "चार, पाँच, छह", "ᱪᱟᱨ, ᱯᱟᱺᱪ, ᱪᱷᱚ (ᱯᱩᱱ, ᱢᱚᱬᱮ, ᱛᱩᱨᱩᱭ)", "Santhali (ᱥᱟᱱᱛᱟᱲᱤ)",
                        "Count the next set of objects together.", "apples_3", 2, 5
                ));
                database.lessonDao().insertAll(lessons);
            }
        });
    }

    public List<FlashcardItem> getSampleFlashcards() {
        List<FlashcardItem> cards = new ArrayList<>();
        cards.add(new FlashcardItem(R.drawable.ic_apple_single, "सेब", "(Seb)", "Apple", "ᱟᱯᱮᱞ (Apel)", "सेब (Seb)", "सेब (Seb)"));
        cards.add(new FlashcardItem(R.drawable.ic_leaf_logo, "पेड़ / पत्ता", "(Patta)", "Leaf / Tree", "ᱥᱟᱠᱟᱢ (Sakam)", "सकाम (Sakam)", "साकाम (Sakam)"));
        cards.add(new FlashcardItem(R.drawable.ic_apples_three, "गिनती (१, २, ३)", "(Ginti)", "Counting", "ᱢᱤᱫ, ᱵᱟᱨ, ᱯᱮ", "मियाद, बारिया, आपी", "मियद, बारिया, आपे"));
        cards.add(new FlashcardItem(R.drawable.ic_school_kids, "विद्यार्थी / स्कूल", "(Vidyarthi)", "Student / School", "ᱯᱟᱹᱴᱷᱩᱣᱟᱹ (Pathuwa)", "पठुआ (Pathua)", "इतुन (Itun)"));
        return cards;
    }

    public List<WorksheetItem> getSampleWorksheets() {
        List<WorksheetItem> list = new ArrayList<>();
        list.add(new WorksheetItem(1, "Numbers 1–10", "Class 1 • Mathematics", R.drawable.ic_apples_three, null, false));
        list.add(new WorksheetItem(2, "Addition Practice", "Class 1 • Mathematics", 0, "2 + 3 = ?", false));
        list.add(new WorksheetItem(3, "Shapes & Objects", "Class 1 • Mathematics", R.drawable.ic_leaf_logo, null, true));
        return list;
    }

    public List<MaterialItem> getSampleMaterials() {
        List<MaterialItem> list = new ArrayList<>();
        list.add(new MaterialItem(1, "Numbers and Counting Package", "Class 1 • Mathematics • Santhali", "Mathematics", R.drawable.ic_lessons, true));
        list.add(new MaterialItem(2, "Addition and Subtraction Worksheets", "Class 1 • Mathematics • Bilingual", "Mathematics", R.drawable.ic_worksheet_quick, false));
        list.add(new MaterialItem(3, "Vernacular Alphabet & Phonetics", "Class 1 • Languages • Mundari & Ho", "Languages", R.drawable.ic_language, true));
        list.add(new MaterialItem(4, "Our Environment & Nature Around Us", "Class 1 • EVS • Santhali & Hindi", "Science", R.drawable.ic_leaf_logo, false));
        list.add(new MaterialItem(5, "Animal & Bird Vocabulary Cards", "Class 2 • Languages • Ho", "Languages", R.drawable.ic_voice_quick, true));
        return list;
    }
}
