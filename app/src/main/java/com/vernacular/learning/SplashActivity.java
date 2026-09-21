package com.vernacular.learning;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable navigateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.vernacular.learning.utils.ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View splashRoot = findViewById(R.id.splashRoot);
        if (splashRoot != null) {
            splashRoot.setAlpha(0f);
            splashRoot.animate().alpha(1f).setDuration(600).start();
        }

        navigateRunnable = this::navigateToMain;
        handler.postDelayed(navigateRunnable, 1800);

        // Allow immediate click-through
        if (splashRoot != null) {
            splashRoot.setOnClickListener(v -> navigateToMain());
        }
    }

    private void navigateToMain() {
        handler.removeCallbacks(navigateRunnable);
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
