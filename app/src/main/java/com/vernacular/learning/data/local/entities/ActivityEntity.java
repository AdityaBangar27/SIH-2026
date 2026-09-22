package com.vernacular.learning.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entity representing interactive learning activities related to a lesson.
 */
@Entity(
        tableName = "activities",
        indices = {
                @Index(value = {"lessonId"}),
                @Index(value = {"sequenceOrder"})
        }
)
public class ActivityEntity {
    @PrimaryKey
    @NonNull
    public String activityId;

    public String lessonId;
    public String title;
    public String instructions;
    public String activityType; // e.g. "COUNTING", "MATCHING", "PRONUNCIATION"
    public String contentReference;
    public int sequenceOrder;
    public int contentVersion;

    public ActivityEntity(@NonNull String activityId, String lessonId, String title,
                          String instructions, String activityType, String contentReference,
                          int sequenceOrder, int contentVersion) {
        this.activityId = activityId;
        this.lessonId = lessonId;
        this.title = title;
        this.instructions = instructions;
        this.activityType = activityType;
        this.contentReference = contentReference;
        this.sequenceOrder = sequenceOrder;
        this.contentVersion = contentVersion;
    }
}
