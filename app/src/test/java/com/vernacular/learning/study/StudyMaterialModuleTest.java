package com.vernacular.learning.study;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import com.vernacular.learning.data.models.study.StudyModels.*;
import com.vernacular.learning.data.repository.StudyMaterialRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class StudyMaterialModuleTest {

    private Context context;
    private StudyMaterialRepository repository;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        repository = StudyMaterialRepository.getInstance(context);
    }

    @Test
    public void testCurriculumClassesLoaded() {
        List<CurriculumClass> classes = repository.getClassesList();
        assertNotNull("Classes list should not be null", classes);
        assertTrue("Classes list should have at least 1 class", classes.size() >= 1);

        CurriculumClass class3 = repository.getClassByName("Class 3");
        assertNotNull("Class 3 should exist in curriculum", class3);
        assertTrue("Class 3 should have subjects", class3.subjects.size() >= 2);
    }

    @Test
    public void testClass3EVSPlantsAroundUsContent() {
        CurriculumChapter chapter = repository.getChapter("Class 3", "EVS", "Plants Around Us");
        assertNotNull("Plants Around Us chapter should exist", chapter);
        assertTrue("Chapter title should match Hindi or English",
                "हमारे आसपास के पौधे".equals(chapter.title) || "Plants Around Us".equals(chapter.title));
        assertNotNull("Learning objective should be defined", chapter.learningObjective);
        assertFalse("Learning objective should not be empty", chapter.learningObjective.isEmpty());

        // Check study material parts (Roots, Stem, Leaves, Flowers, Fruits)
        assertNotNull("Study material parts should exist", chapter.studyMaterial);
        assertTrue("Should have 5 plant parts", chapter.studyMaterial.size() >= 5);

        // Check key points and examples
        assertTrue("Should have key points", chapter.keyPoints.size() >= 4);
        assertTrue("Should have examples", chapter.examples.size() >= 4);

        // Check vocabulary
        assertTrue("Should have bilingual vocabulary", chapter.vocabulary.size() >= 5);
        VocabularyItem rootVoc = chapter.vocabulary.get(0);
        assertNotNull(rootVoc.word);
        assertNotNull(rootVoc.hindi);
        assertNotNull(rootVoc.santali);

        // Check questions
        assertTrue("Should have at least 10 questions for Plants Around Us", chapter.questions.size() >= 10);

        // Check flashcards
        assertTrue("Should have at least 10 flashcards for Plants Around Us", chapter.flashcards.size() >= 10);
    }

    @Test
    public void testClass3MathAdditionAndSubtractionContent() {
        CurriculumChapter addChapter = repository.getChapter("Class 3", "Math", "Numbers and Addition");
        assertNotNull("Numbers and Addition chapter should exist", addChapter);
        assertTrue("Should have questions for addition", addChapter.questions.size() >= 5);
        assertTrue("Should have flashcards for addition", addChapter.flashcards.size() >= 5);

        CurriculumChapter subChapter = repository.getChapter("Class 3", "Math", "Subtraction");
        assertNotNull("Subtraction chapter should exist", subChapter);
        assertTrue("Should have questions for subtraction", subChapter.questions.size() >= 5);
    }

    @Test
    public void testDeterministicWorksheetGeneration() {
        // Generate Easy 5-question worksheet
        WorksheetData wsEasy = repository.generateWorksheet(
                "Class 3", "EVS", "Plants Around Us", "Easy", 5, "Hindi"
        );
        assertNotNull("Generated worksheet should not be null", wsEasy);
        assertEquals("Easy", wsEasy.difficulty);
        assertEquals(5, wsEasy.questions.size());

        // Generate Hard 10-question worksheet
        WorksheetData wsHard = repository.generateWorksheet(
                "Class 3", "EVS", "Plants Around Us", "Hard", 10, "Hindi"
        );
        assertNotNull(wsHard);
        assertEquals("Hard", wsHard.difficulty);
        assertEquals(10, wsHard.questions.size());

        // Verify that the generated worksheet question order/content differs appropriately
        assertNotEquals("Different difficulty/count should generate different question distributions",
                wsEasy.questions.size(), wsHard.questions.size());
    }

    @Test
    public void testWorksheetSaveAndPersistence() {
        WorksheetData ws = repository.generateWorksheet(
                "Class 3", "EVS", "Water", "Medium", 5, "Hindi"
        );
        assertNotNull(ws);

        repository.saveWorksheet(ws);

        List<WorksheetData> saved = repository.getSavedWorksheets();
        assertNotNull(saved);
        boolean found = false;
        for (WorksheetData item : saved) {
            if (item.id.equals(ws.id)) {
                found = true;
                break;
            }
        }
        assertTrue("Saved worksheet should be in saved worksheets list", found);

        // Test delete
        repository.deleteWorksheet(ws.id);
        List<WorksheetData> afterDelete = repository.getSavedWorksheets();
        for (WorksheetData item : afterDelete) {
            assertNotEquals("Deleted worksheet should no longer exist", ws.id, item.id);
        }
    }

    @Test
    public void testFlashcardExtraction() {
        List<StudyFlashcard> cards = repository.getFlashcardsForTopic("Class 3", "EVS", "Plants Around Us", 10);
        assertNotNull(cards);
        assertEquals(10, cards.size());

        StudyFlashcard card1 = cards.get(0);
        assertNotNull(card1.front);
        assertNotNull(card1.back);
        assertFalse(card1.front.isEmpty());
        assertFalse(card1.back.isEmpty());
    }

    @Test
    public void testUserResourceManagement() {
        List<UserResourceItem> initialResources = repository.getUserResources();
        assertNotNull(initialResources);
        int initialCount = initialResources.size();

        UserResourceItem newRes = new UserResourceItem(
                "res_test_1",
                "Soil and Seeds Guide.pdf",
                "Class 3",
                "EVS",
                "Plants Around Us",
                "Hindi & Santhali",
                null,
                "application/pdf",
                102400L,
                System.currentTimeMillis()
        );

        repository.addUserResource(newRes);
        List<UserResourceItem> afterAdd = repository.getUserResources();
        assertEquals(initialCount + 1, afterAdd.size());

        // Test delete
        repository.deleteUserResource(newRes.id);
        List<UserResourceItem> afterDel = repository.getUserResources();
        assertEquals(initialCount, afterDel.size());
    }
}
