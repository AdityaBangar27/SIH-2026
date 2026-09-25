package com.vernacular.learning.ui.resources;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.vernacular.learning.R;
import com.vernacular.learning.data.models.study.StudyModels.UserResourceItem;
import com.vernacular.learning.data.repository.StudyMaterialRepository;
import com.vernacular.learning.utils.PreferenceHelper;
import com.vernacular.learning.utils.ThemeHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MyResourcesActivity extends AppCompatActivity {

    private RecyclerView rvResources;
    private LinearLayout layoutEmptyResources;
    private ChipGroup cgResourceFilters;
    private MaterialButton btnAddResource;

    private final List<UserResourceItem> allResources = new ArrayList<>();
    private final List<UserResourceItem> displayedResources = new ArrayList<>();
    private ResourceAdapter adapter;
    private StudyMaterialRepository repository;

    private String currentFilter = "All";
    private Uri pendingPickedUri = null;

    private final ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    pendingPickedUri = uri;
                    showAddResourceDetailsDialog(getFileNameFromUri(uri));
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_resources);

        repository = StudyMaterialRepository.getInstance(this);

        initViews();
        loadResources();
    }

    private void initViews() {
        ImageView btnBack = findViewById(R.id.btnBack);
        rvResources = findViewById(R.id.rvResources);
        layoutEmptyResources = findViewById(R.id.layoutEmptyResources);
        cgResourceFilters = findViewById(R.id.cgResourceFilters);
        btnAddResource = findViewById(R.id.btnAddResource);

        btnBack.setOnClickListener(v -> finish());

        rvResources.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ResourceAdapter(displayedResources, new ResourceAdapter.OnResourceActionListener() {
            @Override
            public void onOpen(UserResourceItem item) {
                openResource(item);
            }

            @Override
            public void onDelete(UserResourceItem item) {
                confirmDeleteResource(item);
            }
        });
        rvResources.setAdapter(adapter);

        btnAddResource.setOnClickListener(v -> {
            // Open Android system file picker
            try {
                filePickerLauncher.launch("*/*");
            } catch (Exception e) {
                // Fallback to direct dialog if picker unavailable
                showAddResourceDetailsDialog("New Study Material.pdf");
            }
        });

        cgResourceFilters.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipFilterEVS) {
                currentFilter = "EVS";
            } else if (checkedId == R.id.chipFilterMath) {
                currentFilter = "Mathematics";
            } else {
                currentFilter = "All";
            }
            applyFilter();
        });
    }

    private void loadResources() {
        allResources.clear();
        allResources.addAll(repository.getUserResources());
        applyFilter();
    }

    private void applyFilter() {
        displayedResources.clear();
        for (UserResourceItem item : allResources) {
            if ("All".equalsIgnoreCase(currentFilter)) {
                displayedResources.add(item);
            } else if (item.subject != null && item.subject.toLowerCase().contains(currentFilter.toLowerCase())) {
                displayedResources.add(item);
            }
        }

        if (displayedResources.isEmpty()) {
            rvResources.setVisibility(View.GONE);
            layoutEmptyResources.setVisibility(View.VISIBLE);
        } else {
            rvResources.setVisibility(View.VISIBLE);
            layoutEmptyResources.setVisibility(View.GONE);
        }
        adapter.notifyDataSetChanged();
    }

    private void showAddResourceDetailsDialog(String defaultFileName) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_resource, null);
        EditText etName = dialogView.findViewById(R.id.etResourceName);
        Spinner spnClass = dialogView.findViewById(R.id.spnResourceClass);
        Spinner spnSubject = dialogView.findViewById(R.id.spnResourceSubject);
        EditText etTopic = dialogView.findViewById(R.id.etResourceTopic);
        Spinner spnLanguage = dialogView.findViewById(R.id.spnResourceLanguage);

        if (defaultFileName != null && !defaultFileName.isEmpty()) {
            etName.setText(defaultFileName);
        }

        ArrayAdapter<String> classAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Class 3", "Class 1", "Class 2"});
        spnClass.setAdapter(classAdapter);

        ArrayAdapter<String> subAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"EVS", "Mathematics", "Hindi"});
        spnSubject.setAdapter(subAdapter);

        String currentMotherTongue = PreferenceHelper.getSelectedMotherTongue(this);
        ArrayAdapter<String> langAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Hindi & Santhali", "Hindi", currentMotherTongue});
        spnLanguage.setAdapter(langAdapter);

        etTopic.setText("Plants Around Us");

        new AlertDialog.Builder(this)
                .setTitle("अध्ययन सामग्री सहेजें")
                .setView(dialogView)
                .setPositiveButton("स्थानीय रूप से सहेजें", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "कृपया सामग्री का नाम दर्ज करें।", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String selectedClass = (String) spnClass.getSelectedItem();
                    String selectedSub = (String) spnSubject.getSelectedItem();
                    String topic = etTopic.getText().toString().trim();
                    String language = (String) spnLanguage.getSelectedItem();

                    saveResourceLocally(name, selectedClass, selectedSub, topic, language);
                })
                .setNegativeButton("रद्द करें", null)
                .show();
    }

    private void saveResourceLocally(String name, String className, String subject, String topic, String language) {
        String localPath = null;
        long fileSize = 0;

        if (pendingPickedUri != null) {
            try {
                File dir = new File(getFilesDir(), "resources");
                if (!dir.exists()) dir.mkdirs();
                File destFile = new File(dir, System.currentTimeMillis() + "_" + name);

                InputStream is = getContentResolver().openInputStream(pendingPickedUri);
                if (is != null) {
                    FileOutputStream fos = new FileOutputStream(destFile);
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = is.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.flush();
                    fos.close();
                    is.close();
                    localPath = destFile.getAbsolutePath();
                    fileSize = destFile.length();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            pendingPickedUri = null;
        }

        UserResourceItem item = new UserResourceItem(
                "res_" + UUID.randomUUID().toString().substring(0, 8),
                name,
                className,
                subject,
                topic,
                language,
                localPath,
                name.endsWith(".pdf") ? "application/pdf" : "image/jpeg",
                fileSize > 0 ? fileSize : 256000L,
                System.currentTimeMillis()
        );

        repository.addUserResource(item);
        Toast.makeText(this, "सामग्री सफलतापूर्वक सहेजी गई!", Toast.LENGTH_SHORT).show();
        loadResources();
    }

    private void openResource(UserResourceItem item) {
        if (item.localFilePath != null && new File(item.localFilePath).exists()) {
            try {
                File file = new File(item.localFilePath);
                Uri fileUri = FileProvider.getUriForFile(
                        this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        file
                );

                Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                viewIntent.setDataAndType(fileUri, item.mimeType != null ? item.mimeType : "*/*");
                viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(viewIntent);
                return;
            } catch (Exception e) {
                Toast.makeText(this, "This file type cannot be previewed on this device.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        // Realistic Educational Demo Viewer for built-in sample resources
        new AlertDialog.Builder(this)
                .setTitle(item.name)
                .setMessage("सामग्री: " + item.name + "\n" +
                        "कक्षा: " + item.className + "\n" +
                        "विषय: " + item.subject + "\n" +
                        "पाठ: " + (item.topic != null ? item.topic : "सामान्य") + "\n\n" +
                        "विवरण:\n" +
                        "• शिक्षक नोट्स, आरेख व द्विभाषी संदर्भ बिंदु।\n" +
                        "• कक्षा में ऑफ़लाइन पठन व उपयोग हेतु उपलब्ध।")
                .setPositiveButton("बंद करें", null)
                .show();
    }

    private void confirmDeleteResource(UserResourceItem item) {
        new AlertDialog.Builder(this)
                .setTitle("सामग्री हटाएँ")
                .setMessage("क्या आप वाकई '" + item.name + "' को स्थानीय मेमोरी से हटाना चाहते हैं?")
                .setPositiveButton("हटाएँ", (dialog, which) -> {
                    repository.deleteUserResource(item.id);
                    Toast.makeText(this, "सामग्री हटा दी गई।", Toast.LENGTH_SHORT).show();
                    loadResources();
                })
                .setNegativeButton("रद्द करें", null)
                .show();
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int colIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (colIndex != -1) {
                        result = cursor.getString(colIndex);
                    }
                }
            } catch (Exception ignored) {}
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result != null ? result : "study_material.pdf";
    }
}
