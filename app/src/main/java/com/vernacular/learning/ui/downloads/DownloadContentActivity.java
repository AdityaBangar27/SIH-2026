package com.vernacular.learning.ui.downloads;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.vernacular.learning.R;
import com.vernacular.learning.data.local.entities.DownloadItemEntity;
import com.vernacular.learning.data.repository.LearningRepository;
import com.vernacular.learning.utils.ThemeHelper;
import java.util.ArrayList;
import java.util.List;

public class DownloadContentActivity extends AppCompatActivity {
    private RecyclerView rvDownloads;
    private View bannerDownloaded;
    private LinearLayout layoutEmptyDownloads;
    private DownloadAdapter adapter;
    private final List<DownloadItemEntity> downloadList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_content);

        ImageView btnBack = findViewById(R.id.btnDownloadBack);
        rvDownloads = findViewById(R.id.rvDownloadPackages);
        bannerDownloaded = findViewById(R.id.bannerDownloaded);
        layoutEmptyDownloads = findViewById(R.id.layoutEmptyDownloads);

        btnBack.setOnClickListener(v -> finish());

        setupRecyclerView();
        observeDownloads();
    }

    private void setupRecyclerView() {
        rvDownloads.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DownloadAdapter(downloadList, this::checkCompletionStatus);
        rvDownloads.setAdapter(adapter);
    }

    private void observeDownloads() {
        LearningRepository.getInstance(this).getAllDownloadsLiveData().observe(this, items -> {
            downloadList.clear();
            if (items != null && !items.isEmpty()) {
                downloadList.addAll(items);
                adapter.notifyDataSetChanged();
                rvDownloads.setVisibility(View.VISIBLE);
                if (layoutEmptyDownloads != null) layoutEmptyDownloads.setVisibility(View.GONE);
                checkCompletionStatus(downloadList);
            } else {
                // Zero sample download items
                adapter.notifyDataSetChanged();
                rvDownloads.setVisibility(View.GONE);
                if (layoutEmptyDownloads != null) layoutEmptyDownloads.setVisibility(View.VISIBLE);
                bannerDownloaded.setVisibility(View.GONE);
            }
        });
    }

    private void checkCompletionStatus(List<DownloadItemEntity> items) {
        if (items == null || items.isEmpty()) {
            bannerDownloaded.setVisibility(View.GONE);
            return;
        }
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
