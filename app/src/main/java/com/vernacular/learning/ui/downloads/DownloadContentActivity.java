package com.vernacular.learning.ui.downloads;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.vernacular.learning.R;
import com.vernacular.learning.data.local.entities.DownloadItemEntity;
import java.util.ArrayList;
import java.util.List;

public class DownloadContentActivity extends AppCompatActivity {
    private RecyclerView rvDownloads;
    private View bannerDownloaded;
    private DownloadAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_content);

        ImageView btnBack = findViewById(R.id.btnDownloadBack);
        rvDownloads = findViewById(R.id.rvDownloadPackages);
        bannerDownloaded = findViewById(R.id.bannerDownloaded);

        btnBack.setOnClickListener(v -> finish());

        setupRecyclerView();
    }

    private void setupRecyclerView() {
        rvDownloads.setLayoutManager(new LinearLayoutManager(this));

        List<DownloadItemEntity> items = new ArrayList<>();
        items.add(new DownloadItemEntity("Lesson Packages", "120 MB", 120 * 1024 * 1024L, "LessonPackages", "NOT_DOWNLOADED", 0));
        items.add(new DownloadItemEntity("Translation Models", "250 MB", 250 * 1024 * 1024L, "TranslationModels", "NOT_DOWNLOADED", 0));
        items.add(new DownloadItemEntity("Audio Files", "180 MB", 180 * 1024 * 1024L, "AudioFiles", "DOWNLOADED", 100));
        items.add(new DownloadItemEntity("Images & Resources", "95 MB", 95 * 1024 * 1024L, "Images", "DOWNLOADED", 100));

        adapter = new DownloadAdapter(items, this::checkCompletionStatus);
        rvDownloads.setAdapter(adapter);

        checkCompletionStatus(items);
    }

    private void checkCompletionStatus(List<DownloadItemEntity> items) {
        boolean allDownloaded = true;
        for (DownloadItemEntity item : items) {
            if (!"DOWNLOADED".equals(item.downloadStatus)) {
                allDownloaded = false;
                break;
            }
        }
        bannerDownloaded.setVisibility(allDownloaded ? View.VISIBLE : View.GONE);
    }
}
