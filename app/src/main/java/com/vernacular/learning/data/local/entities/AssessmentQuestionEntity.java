package com.vernacular.learning.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entity representing assessment questions linked to lessons.
 * Does not contain hardcoded questions or answers.
 */
@Entity(
        tableName = "assessment_questions",
        indices = {
                @Index(value = {"lessonId"}),
                @Index(value = {"sequenceOrder"})
        }
)
public class AssessmentQuestionEntity {
    @PrimaryKey
    @NonNull
    public String questionId;

    public String lessonId;
    public String questionText;
    public String questionType; // e.g. "MULTIPLE_CHOICE", "TRUE_FALSE", "FILL_IN_BLANK"
    public String optionsData; // Structured JSON string or serialized options list
    public String correctAnswer; // Supplied strictly by authorized content sources
    public String explanation; // Optional explanation when supplied
    public int marks; // Score value
    public int sequenceOrder;
    public int contentVersion;

    public AssessmentQuestionEntity(@NonNull String questionId, String lessonId,
                                    String questionText, String questionType,
                                    String optionsData, String correctAnswer,
                                    String explanation, int marks,
                                    int sequenceOrder, int contentVersion) {
        this.questionId = questionId;
        this.lessonId = lessonId;
        this.questionText = questionText;
        this.questionType = questionType;
        this.optionsData = optionsData;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
        this.marks = marks;
        this.sequenceOrder = sequenceOrder;
        this.contentVersion = contentVersion;
    }
}
