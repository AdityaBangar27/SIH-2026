package com.vernacular.learning.data.repository;

import android.content.Context;
import android.util.Log;
import com.vernacular.learning.data.local.AppDatabase;
import com.vernacular.learning.data.local.entities.WorksheetEntity;
import com.vernacular.learning.data.models.study.StudyModels.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StudyMaterialRepository {
    private static final String TAG = "StudyMaterialRepo";
    private static volatile StudyMaterialRepository INSTANCE;

    private final Context appContext;
    private final List<CurriculumClass> classesList = new ArrayList<>();
    private final Map<String, StudyQuestion> questionsById = new HashMap<>();
    private final List<WorksheetData> defaultWorksheets = new ArrayList<>();
    private final List<WorksheetData> savedWorksheets = new ArrayList<>();
    private final List<UserResourceItem> userResources = new ArrayList<>();
    private boolean isDataLoaded = false;

    private StudyMaterialRepository(Context context) {
        this.appContext = context.getApplicationContext();
        loadCurriculumFromAssets();
        loadSavedWorksheets();
        loadUserResources();
    }

    public static StudyMaterialRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (StudyMaterialRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new StudyMaterialRepository(context);
                }
            }
        }
        return INSTANCE;
    }

    private synchronized void loadCurriculumFromAssets() {
        if (isDataLoaded) return;
        try {
            InputStream is = null;
            try {
                is = appContext.getAssets().open("study_material/curriculum_data.json");
            } catch (Exception ignored) {}

            if (is == null) {
                File f1 = new File("src/main/assets/study_material/curriculum_data.json");
                if (f1.exists()) {
                    is = new FileInputStream(f1);
                } else {
                    File f2 = new File("app/src/main/assets/study_material/curriculum_data.json");
                    if (f2.exists()) {
                        is = new FileInputStream(f2);
                    }
                }
            }

            if (is == null) {
                Log.e(TAG, "Cannot find curriculum_data.json in assets or filesystem");
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            is.close();

            JSONObject root = new JSONObject(sb.toString());
            JSONArray classesArr = root.optJSONArray("classes");
            if (classesArr != null) {
                for (int i = 0; i < classesArr.length(); i++) {
                    JSONObject classObj = classesArr.getJSONObject(i);
                    CurriculumClass cClass = new CurriculumClass(
                            classObj.optString("id"),
                            classObj.optString("name")
                    );

                    JSONArray subjectsArr = classObj.optJSONArray("subjects");
                    if (subjectsArr != null) {
                        for (int j = 0; j < subjectsArr.length(); j++) {
                            JSONObject subObj = subjectsArr.getJSONObject(j);
                            CurriculumSubject subject = new CurriculumSubject(
                                    subObj.optString("id"),
                                    subObj.optString("name"),
                                    subObj.optString("shortName"),
                                    subObj.optString("icon")
                            );

                            JSONArray chaptersArr = subObj.optJSONArray("chapters");
                            if (chaptersArr != null) {
                                for (int k = 0; k < chaptersArr.length(); k++) {
                                    JSONObject chapObj = chaptersArr.getJSONObject(k);
                                    CurriculumChapter chapter = new CurriculumChapter();
                                    chapter.id = chapObj.optString("id");
                                    chapter.title = chapObj.optString("title");
                                    chapter.chapterNumber = chapObj.optInt("chapterNumber", k + 1);
                                    chapter.className = cClass.name;
                                    chapter.subjectName = subject.shortName;
                                    chapter.learningObjective = chapObj.optString("learningObjective");
                                    chapter.explanation = chapObj.optString("explanation");

                                    // Study parts
                                    JSONArray matArr = chapObj.optJSONArray("studyMaterial");
                                    if (matArr != null) {
                                        for (int m = 0; m < matArr.length(); m++) {
                                            JSONObject pObj = matArr.getJSONObject(m);
                                            chapter.studyMaterial.add(new StudyPart(
                                                    pObj.optString("part"),
                                                    pObj.optString("desc")
                                            ));
                                        }
                                    }

                                    // Key points
                                    JSONArray kpArr = chapObj.optJSONArray("keyPoints");
                                    if (kpArr != null) {
                                        for (int m = 0; m < kpArr.length(); m++) {
                                            chapter.keyPoints.add(kpArr.getString(m));
                                        }
                                    }

                                    // Examples
                                    JSONArray exArr = chapObj.optJSONArray("examples");
                                    if (exArr != null) {
                                        for (int m = 0; m < exArr.length(); m++) {
                                            chapter.examples.add(exArr.getString(m));
                                        }
                                    }

                                    // Vocabulary
                                    JSONArray vocArr = chapObj.optJSONArray("vocabulary");
                                    if (vocArr != null) {
                                        for (int m = 0; m < vocArr.length(); m++) {
                                            JSONObject vObj = vocArr.getJSONObject(m);
                                            chapter.vocabulary.add(new VocabularyItem(
                                                    vObj.optString("word"),
                                                    vObj.optString("hindi"),
                                                    vObj.optString("santali"),
                                                    vObj.optString("meaning")
                                            ));
                                        }
                                    }

                                    // Questions
                                    JSONArray qArr = chapObj.optJSONArray("questions");
                                    if (qArr != null) {
                                        for (int m = 0; m < qArr.length(); m++) {
                                            JSONObject qObj = qArr.getJSONObject(m);
                                            List<String> options = new ArrayList<>();
                                            JSONArray optArr = qObj.optJSONArray("options");
                                            if (optArr != null) {
                                                for (int o = 0; o < optArr.length(); o++) {
                                                    options.add(optArr.getString(o));
                                                }
                                            }
                                            StudyQuestion q = new StudyQuestion(
                                                    qObj.optString("id"),
                                                    qObj.optString("type"),
                                                    qObj.optString("difficulty"),
                                                    qObj.optString("text"),
                                                    options,
                                                    qObj.optString("correctAnswer"),
                                                    qObj.optString("explanation")
                                            );
                                            chapter.questions.add(q);
                                            questionsById.put(q.id, q);
                                        }
                                    }

                                    // Flashcards
                                    JSONArray fcArr = chapObj.optJSONArray("flashcards");
                                    if (fcArr != null) {
                                        for (int m = 0; m < fcArr.length(); m++) {
                                            JSONObject fcObj = fcArr.getJSONObject(m);
                                            chapter.flashcards.add(new StudyFlashcard(
                                                    fcObj.optString("front"),
                                                    fcObj.optString("back"),
                                                    fcObj.optString("concept"),
                                                    fcObj.optString("visualHint")
                                            ));
                                        }
                                    }

                                    subject.chapters.add(chapter);
                                }
                            }
                            cClass.subjects.add(subject);
                        }
                    }
                    classesList.add(cClass);
                }
            }

            // Default Worksheets
            JSONArray defaultWsArr = root.optJSONArray("defaultWorksheets");
            if (defaultWsArr != null) {
                for (int i = 0; i < defaultWsArr.length(); i++) {
                    JSONObject wsObj = defaultWsArr.getJSONObject(i);
                    WorksheetData ws = new WorksheetData();
                    ws.id = wsObj.optString("id");
                    ws.title = wsObj.optString("title");
                    ws.className = wsObj.optString("className");
                    ws.subject = wsObj.optString("subject");
                    ws.topic = wsObj.optString("topic");
                    ws.difficulty = wsObj.optString("difficulty");
                    ws.totalQuestions = wsObj.optInt("totalQuestions", 5);
                    ws.createdAt = System.currentTimeMillis() - (i * 3600000L);
                    ws.isCustomGenerated = false;

                    JSONArray qIds = wsObj.optJSONArray("questions");
                    if (qIds != null) {
                        for (int q = 0; q < qIds.length(); q++) {
                            String qId = qIds.getString(q);
                            StudyQuestion matchedQ = questionsById.get(qId);
                            if (matchedQ != null) {
                                ws.questions.add(matchedQ);
                            }
                        }
                    }
                    defaultWorksheets.add(ws);
                }
            }

            isDataLoaded = true;
            Log.d(TAG, "Successfully loaded curriculum. Classes: " + classesList.size() +
                    ", Default Worksheets: " + defaultWorksheets.size());
        } catch (Exception e) {
            Log.e(TAG, "Error parsing curriculum_data.json", e);
        }
    }

    public List<CurriculumClass> getClassesList() {
        return new ArrayList<>(classesList);
    }

    public CurriculumClass getClassByName(String name) {
        for (CurriculumClass c : classesList) {
            if (c.name.equalsIgnoreCase(name) || c.id.equalsIgnoreCase(name)) return c;
            if (name != null && (c.name.contains(name) || c.id.contains(name))) return c;
        }
        if (name != null && (name.contains("3") || name.contains("३"))) {
            for (CurriculumClass c : classesList) {
                if (c.id.contains("3") || c.name.contains("3") || c.name.contains("३")) return c;
            }
        }
        return classesList.isEmpty() ? null : classesList.get(0);
    }

    public CurriculumSubject getSubject(String className, String subjectName) {
        CurriculumClass c = getClassByName(className);
        if (c == null) return null;
        for (CurriculumSubject s : c.subjects) {
            if (s.shortName.equalsIgnoreCase(subjectName) || s.name.equalsIgnoreCase(subjectName) || s.id.equalsIgnoreCase(subjectName)) {
                return s;
            }
            if (subjectName != null && (s.name.contains(subjectName) || s.shortName.contains(subjectName))) {
                return s;
            }
        }
        if ("EVS".equalsIgnoreCase(subjectName) || "पर्यावरण अध्ययन".equalsIgnoreCase(subjectName) || "पर्यावरण".equalsIgnoreCase(subjectName)) {
            for (CurriculumSubject s : c.subjects) {
                if (s.id.equals("evs") || s.shortName.equalsIgnoreCase("EVS")) return s;
            }
        }
        if ("Math".equalsIgnoreCase(subjectName) || "Mathematics".equalsIgnoreCase(subjectName) || "गणित".equalsIgnoreCase(subjectName)) {
            for (CurriculumSubject s : c.subjects) {
                if (s.id.equals("math") || s.shortName.equalsIgnoreCase("Math")) return s;
            }
        }
        return c.subjects.isEmpty() ? null : c.subjects.get(0);
    }

    public CurriculumChapter getChapter(String className, String subjectName, String topicName) {
        CurriculumSubject s = getSubject(className, subjectName);
        if (s == null) return null;
        for (CurriculumChapter ch : s.chapters) {
            if (ch.title.equalsIgnoreCase(topicName) || ch.id.equalsIgnoreCase(topicName)) {
                return ch;
            }
            if (topicName != null && !topicName.isEmpty()) {
                if (ch.title.toLowerCase().contains(topicName.toLowerCase())) {
                    return ch;
                }
            }
        }
        // Aliases mapping
        if ("Plants Around Us".equalsIgnoreCase(topicName) || "plants_around_us".equalsIgnoreCase(topicName) || "पौधे".equalsIgnoreCase(topicName)) {
            for (CurriculumChapter ch : s.chapters) {
                if (ch.id.equals("plants_around_us") || ch.title.contains("पौधे")) return ch;
            }
        }
        if ("Animals Around Us".equalsIgnoreCase(topicName) || "animals_around_us".equalsIgnoreCase(topicName) || "जानवर".equalsIgnoreCase(topicName)) {
            for (CurriculumChapter ch : s.chapters) {
                if (ch.id.equals("animals_around_us") || ch.title.contains("जानवर")) return ch;
            }
        }
        if ("Water".equalsIgnoreCase(topicName) || "water".equalsIgnoreCase(topicName) || "जल".equalsIgnoreCase(topicName) || "पानी".equalsIgnoreCase(topicName)) {
            for (CurriculumChapter ch : s.chapters) {
                if (ch.id.equals("water") || ch.title.contains("जल")) return ch;
            }
        }
        if ("Numbers and Addition".equalsIgnoreCase(topicName) || "addition".equalsIgnoreCase(topicName) || "जोड़".equalsIgnoreCase(topicName)) {
            for (CurriculumChapter ch : s.chapters) {
                if (ch.id.equals("addition") || ch.title.contains("जोड़")) return ch;
            }
        }
        if ("Subtraction".equalsIgnoreCase(topicName) || "subtraction".equalsIgnoreCase(topicName) || "घटाव".equalsIgnoreCase(topicName)) {
            for (CurriculumChapter ch : s.chapters) {
                if (ch.id.equals("subtraction") || ch.title.contains("घटाव")) return ch;
            }
        }
        return s.chapters.isEmpty() ? null : s.chapters.get(0);
    }

    public List<CurriculumChapter> getChaptersForSubject(String className, String subjectName) {
        CurriculumSubject s = getSubject(className, subjectName);
        return s != null ? s.chapters : new ArrayList<>();
    }

    // ==========================================
    // DETERMINISTIC WORKSHEET ENGINE
    // ==========================================
    public WorksheetData generateWorksheet(String className, String subject, String topic,
                                          String difficulty, int count, String language) {
        CurriculumChapter chapter = getChapter(className, subject, topic);
        List<StudyQuestion> bank = new ArrayList<>();
        if (chapter != null && !chapter.questions.isEmpty()) {
            bank.addAll(chapter.questions);
        } else {
            // Find questions across all chapters if topic has few
            for (CurriculumClass c : classesList) {
                for (CurriculumSubject s : c.subjects) {
                    for (CurriculumChapter ch : s.chapters) {
                        bank.addAll(ch.questions);
                    }
                }
            }
        }

        // Deterministic filtering by difficulty
        List<StudyQuestion> matchingDifficulty = new ArrayList<>();
        List<StudyQuestion> otherDifficulty = new ArrayList<>();

        for (StudyQuestion q : bank) {
            if (difficulty != null && difficulty.equalsIgnoreCase(q.difficulty)) {
                matchingDifficulty.add(q);
            } else {
                otherDifficulty.add(q);
            }
        }

        List<StudyQuestion> selectedQuestions = new ArrayList<>(matchingDifficulty);
        if (selectedQuestions.size() < count) {
            for (StudyQuestion q : otherDifficulty) {
                if (!selectedQuestions.contains(q)) {
                    selectedQuestions.add(q);
                    if (selectedQuestions.size() >= count) break;
                }
            }
        }

        if (selectedQuestions.size() > count) {
            selectedQuestions = selectedQuestions.subList(0, count);
        }

        String worksheetId = "ws_" + UUID.randomUUID().toString().substring(0, 8);
        String title = (topic != null && !topic.isEmpty() ? topic : "Class " + className) + " - " + difficulty + " Practice";

        return new WorksheetData(
                worksheetId,
                title,
                className,
                subject,
                topic != null ? topic : "General",
                difficulty,
                selectedQuestions.size(),
                System.currentTimeMillis(),
                true,
                selectedQuestions
        );
    }

    // ==========================================
    // WORKSHEETS PERSISTENCE (ROOM + LOCAL STORAGE)
    // ==========================================
    public List<WorksheetData> getAllWorksheets() {
        List<WorksheetData> combined = new ArrayList<>();
        combined.addAll(savedWorksheets);
        combined.addAll(defaultWorksheets);
        return combined;
    }

    public List<WorksheetData> getSavedWorksheets() {
        return new ArrayList<>(savedWorksheets);
    }

    public WorksheetData getWorksheetById(String id) {
        for (WorksheetData ws : savedWorksheets) {
            if (ws.id.equals(id)) return ws;
        }
        for (WorksheetData ws : defaultWorksheets) {
            if (ws.id.equals(id)) return ws;
        }
        return null;
    }

    public synchronized void saveWorksheet(WorksheetData worksheet) {
        // Remove existing if matching id
        for (int i = 0; i < savedWorksheets.size(); i++) {
            if (savedWorksheets.get(i).id.equals(worksheet.id)) {
                savedWorksheets.remove(i);
                break;
            }
        }
        savedWorksheets.add(0, worksheet);
        persistSavedWorksheets();

        // Also sync to Room database WorksheetEntity so existing screens see it
        try {
            WorksheetEntity entity = new WorksheetEntity(
                    worksheet.id,
                    worksheet.title,
                    worksheet.topic,
                    worksheet.className,
                    worksheet.subject,
                    "Hindi",
                    "",
                    worksheet.createdAt,
                    1,
                    "DOWNLOADED"
            );
            AppDatabase.databaseWriteExecutor.execute(() ->
                    AppDatabase.getInstance(appContext).worksheetDao().insert(entity)
            );
        } catch (Exception e) {
            Log.e(TAG, "Error syncing worksheet to Room", e);
        }
    }

    public synchronized void deleteWorksheet(String worksheetId) {
        for (int i = 0; i < savedWorksheets.size(); i++) {
            if (savedWorksheets.get(i).id.equals(worksheetId)) {
                savedWorksheets.remove(i);
                break;
            }
        }
        persistSavedWorksheets();

        // Remove from Room
        try {
            AppDatabase.databaseWriteExecutor.execute(() ->
                    AppDatabase.getInstance(appContext).worksheetDao().deleteById(worksheetId)
            );
        } catch (Exception e) {
            Log.e(TAG, "Error removing worksheet from Room", e);
        }
    }

    private void persistSavedWorksheets() {
        try {
            File dir = new File(appContext.getFilesDir(), "worksheets");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, "my_worksheets.json");

            JSONArray arr = new JSONArray();
            for (WorksheetData ws : savedWorksheets) {
                JSONObject obj = new JSONObject();
                obj.put("id", ws.id);
                obj.put("title", ws.title);
                obj.put("className", ws.className);
                obj.put("subject", ws.subject);
                obj.put("topic", ws.topic);
                obj.put("difficulty", ws.difficulty);
                obj.put("totalQuestions", ws.totalQuestions);
                obj.put("createdAt", ws.createdAt);
                obj.put("isCustomGenerated", ws.isCustomGenerated);

                JSONArray qArr = new JSONArray();
                for (StudyQuestion q : ws.questions) {
                    JSONObject qObj = new JSONObject();
                    qObj.put("id", q.id);
                    qObj.put("type", q.type);
                    qObj.put("difficulty", q.difficulty);
                    qObj.put("text", q.text);
                    JSONArray optArr = new JSONArray(q.options);
                    qObj.put("options", optArr);
                    qObj.put("correctAnswer", q.correctAnswer);
                    qObj.put("explanation", q.explanation);
                    qArr.put(qObj);
                }
                obj.put("questions", qArr);
                arr.put(obj);
            }

            FileOutputStream fos = new FileOutputStream(file);
            fos.write(arr.toString().getBytes(StandardCharsets.UTF_8));
            fos.flush();
            fos.close();
        } catch (Exception e) {
            Log.e(TAG, "Error persisting saved worksheets", e);
        }
    }

    private void loadSavedWorksheets() {
        try {
            File file = new File(new File(appContext.getFilesDir(), "worksheets"), "my_worksheets.json");
            if (!file.exists()) return;

            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            fis.close();

            JSONArray arr = new JSONArray(sb.toString());
            savedWorksheets.clear();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                WorksheetData ws = new WorksheetData();
                ws.id = obj.optString("id");
                ws.title = obj.optString("title");
                ws.className = obj.optString("className");
                ws.subject = obj.optString("subject");
                ws.topic = obj.optString("topic");
                ws.difficulty = obj.optString("difficulty");
                ws.totalQuestions = obj.optInt("totalQuestions", 5);
                ws.createdAt = obj.optLong("createdAt", System.currentTimeMillis());
                ws.isCustomGenerated = obj.optBoolean("isCustomGenerated", true);

                JSONArray qArr = obj.optJSONArray("questions");
                if (qArr != null) {
                    for (int j = 0; j < qArr.length(); j++) {
                        JSONObject qObj = qArr.getJSONObject(j);
                        List<String> options = new ArrayList<>();
                        JSONArray optArr = qObj.optJSONArray("options");
                        if (optArr != null) {
                            for (int k = 0; k < optArr.length(); k++) {
                                options.add(optArr.getString(k));
                            }
                        }
                        ws.questions.add(new StudyQuestion(
                                qObj.optString("id"),
                                qObj.optString("type"),
                                qObj.optString("difficulty"),
                                qObj.optString("text"),
                                options,
                                qObj.optString("correctAnswer"),
                                qObj.optString("explanation")
                        ));
                    }
                }
                savedWorksheets.add(ws);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading saved worksheets", e);
        }
    }

    // ==========================================
    // FLASHCARD ENGINE
    // ==========================================
    public List<StudyFlashcard> getFlashcardsForTopic(String className, String subject, String topic, int count) {
        CurriculumChapter chapter = getChapter(className, subject, topic);
        List<StudyFlashcard> list = new ArrayList<>();
        if (chapter != null && !chapter.flashcards.isEmpty()) {
            list.addAll(chapter.flashcards);
        } else {
            // Collect flashcards from all chapters
            for (CurriculumClass c : classesList) {
                for (CurriculumSubject s : c.subjects) {
                    for (CurriculumChapter ch : s.chapters) {
                        list.addAll(ch.flashcards);
                    }
                }
            }
        }
        if (count > 0 && list.size() > count) {
            return list.subList(0, count);
        }
        return list;
    }

    // ==========================================
    // MY RESOURCES MANAGEMENT (OFFLINE LOCAL STORAGE)
    // ==========================================
    public List<UserResourceItem> getUserResources() {
        return new ArrayList<>(userResources);
    }

    public synchronized void addUserResource(UserResourceItem resource) {
        userResources.add(0, resource);
        persistUserResources();
    }

    public synchronized void deleteUserResource(String id) {
        for (int i = 0; i < userResources.size(); i++) {
            if (userResources.get(i).id.equals(id)) {
                UserResourceItem item = userResources.remove(i);
                if (item.localFilePath != null) {
                    try {
                        File file = new File(item.localFilePath);
                        if (file.exists()) file.delete();
                    } catch (Exception ignored) {}
                }
                break;
            }
        }
        persistUserResources();
    }

    private void persistUserResources() {
        try {
            File dir = new File(appContext.getFilesDir(), "resources");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, "user_resources.json");

            JSONArray arr = new JSONArray();
            for (UserResourceItem item : userResources) {
                JSONObject obj = new JSONObject();
                obj.put("id", item.id);
                obj.put("name", item.name);
                obj.put("className", item.className);
                obj.put("subject", item.subject);
                obj.put("topic", item.topic);
                obj.put("language", item.language);
                obj.put("localFilePath", item.localFilePath);
                obj.put("mimeType", item.mimeType);
                obj.put("fileSizeBytes", item.fileSizeBytes);
                obj.put("dateAdded", item.dateAdded);
                arr.put(obj);
            }

            FileOutputStream fos = new FileOutputStream(file);
            fos.write(arr.toString().getBytes(StandardCharsets.UTF_8));
            fos.flush();
            fos.close();
        } catch (Exception e) {
            Log.e(TAG, "Error persisting user resources", e);
        }
    }

    private void loadUserResources() {
        try {
            File file = new File(new File(appContext.getFilesDir(), "resources"), "user_resources.json");
            if (!file.exists()) {
                // Pre-populate with realistic demo resource notes if empty
                UserResourceItem demo1 = new UserResourceItem(
                        "res_demo_1",
                        "Plants Chapter Notes.pdf",
                        "Class 3",
                        "EVS",
                        "Plants Around Us",
                        "Hindi & Santhali",
                        null,
                        "application/pdf",
                        458200L,
                        System.currentTimeMillis() - 86400000L
                );
                UserResourceItem demo2 = new UserResourceItem(
                        "res_demo_2",
                        "Math Practice Sheet.pdf",
                        "Class 3",
                        "Mathematics",
                        "Numbers and Addition",
                        "Hindi",
                        null,
                        "application/pdf",
                        312000L,
                        System.currentTimeMillis() - 172800000L
                );
                UserResourceItem demo3 = new UserResourceItem(
                        "res_demo_3",
                        "Animal Pictures.jpg",
                        "Class 3",
                        "EVS",
                        "Animals Around Us",
                        "Bilingual",
                        null,
                        "image/jpeg",
                        1205000L,
                        System.currentTimeMillis() - 259200000L
                );
                userResources.add(demo1);
                userResources.add(demo2);
                userResources.add(demo3);
                persistUserResources();
                return;
            }

            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            fis.close();

            JSONArray arr = new JSONArray(sb.toString());
            userResources.clear();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                userResources.add(new UserResourceItem(
                        obj.optString("id"),
                        obj.optString("name"),
                        obj.optString("className"),
                        obj.optString("subject"),
                        obj.optString("topic"),
                        obj.optString("language"),
                        obj.optString("localFilePath"),
                        obj.optString("mimeType"),
                        obj.optLong("fileSizeBytes"),
                        obj.optLong("dateAdded")
                ));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading user resources", e);
        }
    }
}
