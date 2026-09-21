package com.vernacular.learning.ui.translation;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.card.MaterialCardView;
import com.vernacular.learning.R;
import com.vernacular.learning.utils.AudioHelper;

public class VoiceTranslationFragment extends Fragment {
    private TextView tvSourceLang;
    private TextView tvTargetLang;
    private View viewMicRipple;
    private TextView tvMicStatusLabel;
    private TextView tvRecognizedText;
    private TextView tvTranslatedText;
    private TextView tvEngineStatus;

    private boolean isListening = false;
    private int currentTargetLangIndex = 0;
    private final String[] targetLanguages = {"Santhali (ᱥᱟᱱᱛᱟᱲᱤ)", "Mundari (ᱢᱩᱱᱰᱟᱨᱤ)", "Ho (ᱦᱳ)"};

    private ObjectAnimator pulseAnimator;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_voice_translation, container, false);

        ImageView btnBack = root.findViewById(R.id.btnVtBack);
        LinearLayout btnLanguageToggle = root.findViewById(R.id.btnLanguageToggle);
        tvSourceLang = root.findViewById(R.id.tvSourceLang);
        tvTargetLang = root.findViewById(R.id.tvTargetLang);
        viewMicRipple = root.findViewById(R.id.viewMicRipple);
        MaterialCardView btnMicAction = root.findViewById(R.id.btnMicAction);
        tvMicStatusLabel = root.findViewById(R.id.tvMicStatusLabel);
        tvRecognizedText = root.findViewById(R.id.tvRecognizedText);
        tvTranslatedText = root.findViewById(R.id.tvTranslatedText);
        ImageView btnPlayTranslation = root.findViewById(R.id.btnPlayTranslation);
        tvEngineStatus = root.findViewById(R.id.tvEngineStatus);

        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // Cycle through target vernacular languages on click
        btnLanguageToggle.setOnClickListener(v -> {
            currentTargetLangIndex = (currentTargetLangIndex + 1) % targetLanguages.length;
            tvTargetLang.setText(targetLanguages[currentTargetLangIndex]);
            updateSampleTranslation();
        });

        // Microphone tap action
        btnMicAction.setOnClickListener(v -> {
            if (!isListening) {
                startListening();
            } else {
                stopListening();
            }
        });

        // Audio speaker click
        btnPlayTranslation.setOnClickListener(v -> {
            String speech = tvTranslatedText.getText().toString();
            AudioHelper.playPronunciation(requireContext(), speech, null);
        });

        return root;
    }

    private void startListening() {
        isListening = true;
        tvMicStatusLabel.setText(R.string.listening_indicator);
        tvEngineStatus.setText("Listening for speech in Hindi...");

        // Pulse animation on ripple view
        viewMicRipple.setVisibility(View.VISIBLE);
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
                viewMicRipple,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.35f, 1.0f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.35f, 1.0f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 0.4f, 0.9f, 0.4f)
        );
        pulseAnimator.setDuration(900);
        pulseAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        pulseAnimator.start();

        // Simulate voice recognition returning speech after 2 seconds
        handler.postDelayed(() -> {
            if (isListening && isAdded()) {
                stopListening();
                tvRecognizedText.setText("एक, दो, तीन");
                updateSampleTranslation();
                tvEngineStatus.setText("Speech translated successfully.");
            }
        }, 2200);
    }

    private void stopListening() {
        isListening = false;
        tvMicStatusLabel.setText(R.string.tap_to_speak);
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }
        viewMicRipple.setScaleX(1.0f);
        viewMicRipple.setScaleY(1.0f);
        viewMicRipple.setAlpha(0.4f);
    }

    private void updateSampleTranslation() {
        String lang = targetLanguages[currentTargetLangIndex];
        if (lang.contains("Mundari")) {
            tvTranslatedText.setText("मियाद, बारिया, आपी");
        } else if (lang.contains("Ho")) {
            tvTranslatedText.setText("मियद, बारिया, आपे");
        } else {
            tvTranslatedText.setText("ᱢᱤᱫ, ᱵᱟᱨ, ᱯᱮ");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (pulseAnimator != null) pulseAnimator.cancel();
        handler.removeCallbacksAndMessages(null);
    }
}
