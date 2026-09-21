package com.vernacular.learning.data.models;

public class MaterialItem {
    public int id;
    public String title;
    public String subtitle;
    public String category; // Mathematics, Languages, Science
    public int iconResId;
    public boolean isOfflineAvailable;

    public MaterialItem(int id, String title, String subtitle, String category, int iconResId, boolean isOfflineAvailable) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.category = category;
        this.iconResId = iconResId;
        this.isOfflineAvailable = isOfflineAvailable;
    }
}
