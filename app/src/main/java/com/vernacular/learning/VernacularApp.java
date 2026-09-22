package com.vernacular.learning;

import android.app.Application;
import com.vernacular.learning.data.repository.LearningRepository;
import com.vernacular.learning.utils.AudioHelper;
import com.vernacular.learning.utils.ThemeHelper;

public class VernacularApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // 1. Immediately apply saved theme (Light, Dark, or System)
        ThemeHelper.applyTheme(this);

        // 2. Initialize local Room database (starts empty on first install)
        LearningRepository.getInstance(this);

        // 3. Initialize audio pronunciation helper
        AudioHelper.initializeTts(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        AudioHelper.shutdown();
    }
}
