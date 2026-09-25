package com.vernacular.learning.data.models.study;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class StudyModels {

    public static class CurriculumClass implements Serializable {
        public String id;
        public String name;
        public List<CurriculumSubject> subjects = new ArrayList<>();

        public CurriculumClass(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public static class CurriculumSubject implements Serializable {
        public String id;
        public String name;
        public String shortName;
        public String icon;
        public List<CurriculumChapter> chapters = new ArrayList<>();

        public CurriculumSubject(String id, String name, String shortName, String icon) {
            this.id = id;
            this.name = name;
            this.shortName = shortName;
            this.icon = icon;
        }
    }

    public static class StudyPart implements Serializable {
        public String part;
        public String desc;

        public StudyPart(String part, String desc) {
            this.part = part;
            this.desc = desc;
        }
    }

    public static class VocabularyItem implements Serializable {
        public String word;
        public String hindi;
        public String santali;
        public String meaning;

        public VocabularyItem(String word, String hindi, String santali, String meaning) {
            this.word = word;
            this.hindi = hindi;
            this.santali = santali;
            this.meaning = meaning;
        }
    }

    public static class StudyQuestion implements Serializable {
        public String id;
        public String type; // "MCQ", "FILL", "TRUE_FALSE"
        public String difficulty; // "Easy", "Medium", "Hard"
        public String text;
        public List<String> options = new ArrayList<>();
        public String correctAnswer;
        public String explanation;

        public StudyQuestion() {}

        public StudyQuestion(String id, String type, String difficulty, String text,
                             List<String> options, String correctAnswer, String explanation) {
            this.id = id;
            this.type = type;
            this.difficulty = difficulty;
            this.text = text;
            if (options != null) this.options = options;
            this.correctAnswer = correctAnswer;
            this.explanation = explanation;
        }
    }

    public static class StudyFlashcard implements Serializable {
        public String front;
        public String back;
        public String concept;
        public String visualHint;

        public StudyFlashcard(String front, String back, String concept, String visualHint) {
            this.front = front;
            this.back = back;
            this.concept = concept;
            this.visualHint = visualHint;
        }
    }

    public static class CurriculumChapter implements Serializable {
        public String id;
        public String title;
        public int chapterNumber;
        public String className;
        public String subjectName;
        public String learningObjective;
        public String explanation;
        public List<StudyPart> studyMaterial = new ArrayList<>();
        public List<String> keyPoints = new ArrayList<>();
        public List<String> examples = new ArrayList<>();
        public List<VocabularyItem> vocabulary = new ArrayList<>();
        public List<StudyQuestion> questions = new ArrayList<>();
        public List<StudyFlashcard> flashcards = new ArrayList<>();

        public CurriculumChapter() {}
    }

    public static class WorksheetData implements Serializable {
        public String id;
        public String title;
        public String className;
        public String subject;
        public String topic;
        public String difficulty;
        public int totalQuestions;
        public long createdAt;
        public boolean isCustomGenerated;
        public List<StudyQuestion> questions = new ArrayList<>();

        public WorksheetData() {}

        public WorksheetData(String id, String title, String className, String subject,
                             String topic, String difficulty, int totalQuestions,
                             long createdAt, boolean isCustomGenerated, List<StudyQuestion> questions) {
            this.id = id;
            this.title = title;
            this.className = className;
            this.subject = subject;
            this.topic = topic;
            this.difficulty = difficulty;
            this.totalQuestions = totalQuestions;
            this.createdAt = createdAt;
            this.isCustomGenerated = isCustomGenerated;
            if (questions != null) this.questions = questions;
        }
    }

    public static class UserResourceItem implements Serializable {
        public String id;
        public String name;
        public String className;
        public String subject;
        public String topic;
        public String language;
        public String localFilePath;
        public String mimeType;
        public long fileSizeBytes;
        public long dateAdded;

        public UserResourceItem() {}

        public UserResourceItem(String id, String name, String className, String subject,
                                String topic, String language, String localFilePath,
                                String mimeType, long fileSizeBytes, long dateAdded) {
            this.id = id;
            this.name = name;
            this.className = className;
            this.subject = subject;
            this.topic = topic;
            this.language = language;
            this.localFilePath = localFilePath;
            this.mimeType = mimeType;
            this.fileSizeBytes = fileSizeBytes;
            this.dateAdded = dateAdded;
        }
    }
}
