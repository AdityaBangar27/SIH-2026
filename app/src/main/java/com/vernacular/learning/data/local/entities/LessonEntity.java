package com.vernacular.learning.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "lessons")
public class LessonEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String className;
    public String subject;
    public String chapterNumber;
    public String chapterTitle;
    public String hindiContent;
    public String motherTongueContent;
    public String motherTongue;
    public String instruction;
    public String visualType;
    public int lessonIndex;
    public int totalLessons;
    public boolean isCompleted;

    public LessonEntity(String className, String subject, String chapterNumber, String chapterTitle,
                        String hindiContent, String motherTongueContent, String motherTongue,
                        String instruction, String visualType, int lessonIndex, int totalLessons) {
        this.className = className;
        this.subject = subject;
        this.chapterNumber = chapterNumber;
        this.chapterTitle = chapterTitle;
        this.hindiContent = hindiContent;
        this.motherTongueContent = motherTongueContent;
        this.motherTongue = motherTongue;
        this.instruction = instruction;
        this.visualType = visualType;
        this.lessonIndex = lessonIndex;
        this.totalLessons = totalLessons;
        this.isCompleted = false;
    }
}
