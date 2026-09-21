package com.vernacular.learning.data.models;

public class WorksheetItem {
    public int id;
    public String title;
    public String subtitle;
    public int imagePreviewResId;
    public String textPreview; // For math formulas e.g. "2 + 3 = ?"
    public boolean isDownloaded;
    public int downloadProgress; // 0 to 100
    public boolean isDownloading;

    public WorksheetItem(int id, String title, String subtitle, int imagePreviewResId, String textPreview, boolean isDownloaded) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.imagePreviewResId = imagePreviewResId;
        this.textPreview = textPreview;
        this.isDownloaded = isDownloaded;
        this.downloadProgress = isDownloaded ? 100 : 0;
        this.isDownloading = false;
    }
}
