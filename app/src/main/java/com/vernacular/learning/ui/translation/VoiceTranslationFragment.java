package com.vernacular.learning.ui.translation;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.vernacular.learning.R;
import com.vernacular.learning.ai.PipelineResult;
import com.vernacular.learning.ai.VoicePipelineManager;
import com.vernacular.learning.utils.AudioPlayer;
import com.vernacular.learning.utils.AudioRecorder;
import com.vernacular.learning.utils.ThemeHelper;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Voice Translation Fragment implementing the offline teaching workflow:
 *
 * Teacher / Hindi
 *        ↓
 * Hindi → Santali
 *        ↓
 * Student / Santali
 *
 * Runs 100% locally and offline on CPU using Whisper ASR and IndicTrans2 neural translation.
 * Employs local Noto Sans Ol Chiki font resource for authentic Santali Unicode display.
 */
public class VoiceTranslationFragment extends Fragment {
    private static final String TAG = "VoiceTranslationFrag";

    private boolean isHindiToSantali = true;
    private boolean isRecording = false;
    private File currentOutputAudioFile = null;

    // Translation Direction Banner Views
    private MaterialCardView btnSwapDirection;
    private TextView tvDirectionInputText;
    private TextView tvDirectionOutputText;
    private TextView tvTeacherPanelTitle;
    private TextView tvTeacherInputLabel;
    private TextView tvFlowDividerText;
    private TextView tvStudentPanelTitle;
    private TextView tvStudentOutputLabel;

    // Teacher Panel Views
    private MaterialCardView btnTeacherControl;
    private ImageView ivTeacherControlIcon;
    private View viewTeacherRipple;
    private TextView tvTeacherStatus;
    private TextView badgeTeacherState;
    private EditText tvTeacherRecognized;
    private MaterialButton btnTranslate;

    // Student Panel Views
    private TextView badgeStudentState;
    private TextView tvStudentTranslated;
    private TextView tvStudentStatus;
    private MaterialCardView btnPlaySantali;
    private ImageView ivPlaySantaliIcon;
    private TextView tvPlaySantaliLabel;

    // Engine Status
    private TextView tvEngineStatus;

    // Translation Performance Card Views
    private MaterialCardView cardPerformance;
    private TextView tvPerformanceTime;
    private TextView tvPerformanceDesc;
    private TextView tvPerformanceBreakdown;
    private TextView tvPerformanceHistory;

    // Monotonic performance timing tracking
    private long translationStartTime = 0;
    private final List<String> recentPerformanceHistory = new ArrayList<>();

    private ObjectAnimator pulseAnimator;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private VoicePipelineManager voicePipelineManager;
    private AudioRecorder audioRecorder;
    private AudioPlayer audioPlayer;
    private boolean permissionRequestedForRecording = false;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    if (permissionRequestedForRecording) {
                        permissionRequestedForRecording = false;
                        startRecording();
                    }
                } else {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Microphone permission required for offline speech recognition.", Toast.LENGTH_SHORT).show();
                    }
                    resetToIdle();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_voice_translation, container, false);

        ImageView btnBack = root.findViewById(R.id.btnVtBack);
        btnBack.setOnClickListener(v -> {
            if (isAdded()) {
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                } else {
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        // Direction Banner bindings
        btnSwapDirection = root.findViewById(R.id.btnSwapDirection);
        tvDirectionInputText = root.findViewById(R.id.tvDirectionInputText);
        tvDirectionOutputText = root.findViewById(R.id.tvDirectionOutputText);
        tvTeacherPanelTitle = root.findViewById(R.id.tvTeacherPanelTitle);
        tvTeacherInputLabel = root.findViewById(R.id.tvTeacherInputLabel);
        tvFlowDividerText = root.findViewById(R.id.tvFlowDividerText);
        tvStudentPanelTitle = root.findViewById(R.id.tvStudentPanelTitle);
        tvStudentOutputLabel = root.findViewById(R.id.tvStudentOutputLabel);

        // Teacher Panel bindings
        btnTeacherControl = root.findViewById(R.id.btnTeacherControl);
        ivTeacherControlIcon = root.findViewById(R.id.ivTeacherControlIcon);
        viewTeacherRipple = root.findViewById(R.id.viewTeacherRipple);
        tvTeacherStatus = root.findViewById(R.id.tvTeacherStatus);
        badgeTeacherState = root.findViewById(R.id.badgeTeacherState);
        tvTeacherRecognized = root.findViewById(R.id.tvTeacherRecognized);
        btnTranslate = root.findViewById(R.id.btnTranslate);

        // Student Panel bindings
        badgeStudentState = root.findViewById(R.id.badgeStudentState);
        tvStudentTranslated = root.findViewById(R.id.tvStudentTranslated);
        tvStudentStatus = root.findViewById(R.id.tvStudentStatus);
        btnPlaySantali = root.findViewById(R.id.btnPlaySantali);
        ivPlaySantaliIcon = root.findViewById(R.id.ivPlaySantaliIcon);
        tvPlaySantaliLabel = root.findViewById(R.id.tvPlaySantaliLabel);

        // Engine Status
        tvEngineStatus = root.findViewById(R.id.tvEngineStatus);

        // Translation Performance Card bindings
        cardPerformance = root.findViewById(R.id.cardPerformance);
        tvPerformanceTime = root.findViewById(R.id.tvPerformanceTime);
        tvPerformanceDesc = root.findViewById(R.id.tvPerformanceDesc);
        tvPerformanceBreakdown = root.findViewById(R.id.tvPerformanceBreakdown);
        tvPerformanceHistory = root.findViewById(R.id.tvPerformanceHistory);

        // Ensure Ol Chiki local font is applied for Santali rendering
        try {
            Typeface olChikiTypeface = ResourcesCompat.getFont(requireContext(), R.font.noto_sans_ol_chiki);
            if (olChikiTypeface != null) {
                tvStudentTranslated.setTypeface(olChikiTypeface);
                tvStudentStatus.setTypeface(olChikiTypeface);
            }
        } catch (Exception e) {
            Log.w(TAG, "Error loading local Ol Chiki font resource", e);
        }

        audioRecorder = new AudioRecorder();
        audioPlayer = new AudioPlayer();
        voicePipelineManager = new VoicePipelineManager();

        setupControls();
        updateDirectionUI();
        resetToIdle();
        resetPerformanceCard();

        tvEngineStatus.setText("Initializing local offline AI pipeline (Whisper & IndicTrans2)...");
        voicePipelineManager.initializeAsync(requireContext(), success -> {
            if (isAdded()) {
                if (success) {
                    if (voicePipelineManager.getTtsManager().isAvailable()) {
                        tvEngineStatus.setText("Offline AI Pipeline Ready (ASR, Translation & TTS)");
                    } else {
                        tvEngineStatus.setText("Offline AI Pipeline Ready (ASR & IndicTrans2 | TTS Unavailable)");
                    }
                } else {
                    tvEngineStatus.setText(R.string.status_ready);
                }
            }
        });

        return root;
    }

    private void setupControls() {
        // Swap Direction Button
        if (btnSwapDirection != null) {
            btnSwapDirection.setOnClickListener(v -> {
                if (voicePipelineManager != null && voicePipelineManager.isBusy()) {
                    Toast.makeText(requireContext(), "AI pipeline is busy processing...", Toast.LENGTH_SHORT).show();
                    return;
                }
                isHindiToSantali = !isHindiToSantali;
                updateDirectionUI();
                resetToIdle();
                resetPerformanceCard();
                currentOutputAudioFile = null;
                setPlayButtonEnabled(false);
                if (tvTeacherRecognized != null) tvTeacherRecognized.setText("");
                if (tvStudentTranslated != null) tvStudentTranslated.setText("");
            });
        }

        // Teacher / Input Microphone Control
        btnTeacherControl.setOnClickListener(v -> {
            if (voicePipelineManager != null && voicePipelineManager.isBusy()) {
                Toast.makeText(requireContext(), "AI pipeline is busy processing...", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isHindiToSantali) {
                // Santali ASR limitation feedback: Offline Whisper-tiny does not have Santali acoustic model
                Toast.makeText(requireContext(), "Offline Santali voice recognition is unavailable on this device. Please enter or paste Santali text to translate into Hindi.", Toast.LENGTH_LONG).show();
                if (tvTeacherRecognized != null) {
                    tvTeacherRecognized.requestFocus();
                }
                return;
            }

            if (isRecording) {
                // Tapping active microphone again stops recording and begins processing
                stopRecordingAndProcess();
            } else {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    permissionRequestedForRecording = true;
                    requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
                } else {
                    startRecording();
                }
            }
        });

        // Translate Button
        btnTranslate.setOnClickListener(v -> {
            if (voicePipelineManager != null && voicePipelineManager.isBusy()) {
                Toast.makeText(requireContext(), "AI pipeline is busy processing...", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isRecording) {
                // If currently recording speech, translate action finishes recording and processes
                stopRecordingAndProcess();
            } else {
                String inputText = tvTeacherRecognized != null && tvTeacherRecognized.getText() != null
                        ? tvTeacherRecognized.getText().toString().trim() : "";
                if (!inputText.isEmpty()) {
                    processTextTranslation(inputText);
                } else {
                    Toast.makeText(requireContext(), isHindiToSantali
                            ? "Please speak or enter Hindi text to translate"
                            : "Please enter Santali text to translate", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Audio Playback button (Santali or Hindi depending on direction)
        btnPlaySantali.setOnClickListener(v -> {
            if (currentOutputAudioFile != null && currentOutputAudioFile.exists() && currentOutputAudioFile.length() > 44) {
                final String targetLang = isHindiToSantali ? "Santali" : "Hindi";
                audioPlayer.play(currentOutputAudioFile, new AudioPlayer.PlaybackCallback() {
                    @Override
                    public void onPlaybackStarted() {
                        if (isAdded()) {
                            tvPlaySantaliLabel.setText("Playing " + targetLang + " audio...");
                            tvStudentStatus.setText("Playing " + targetLang + " audio...");
                            badgeStudentState.setText("Playing");
                        }
                    }

                    @Override
                    public void onPlaybackCompleted() {
                        if (isAdded()) {
                            tvPlaySantaliLabel.setText(isHindiToSantali ? "Play Santali" : "Play Hindi");
                            tvStudentStatus.setText(R.string.speech_ready);
                            badgeStudentState.setText("Ready");
                        }
                    }

                    @Override
                    public void onError(String message) {
                        if (isAdded()) {
                            tvPlaySantaliLabel.setText(isHindiToSantali ? "Play Santali" : "Play Hindi");
                            tvStudentStatus.setText("Playback error.");
                            badgeStudentState.setText("Ready");
                        }
                    }
                });
            } else {
                Toast.makeText(requireContext(), (isHindiToSantali ? "Santali" : "Hindi") + " TTS audio unavailable.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Starts recording speech from the microphone.
     */
    private void startRecording() {
        if (!isAdded()) return;

        if (!isHindiToSantali) {
            Toast.makeText(requireContext(), "Offline Santali voice recognition is unavailable on this device. Please enter or paste Santali text to translate into Hindi.", Toast.LENGTH_LONG).show();
            if (tvTeacherRecognized != null) {
                tvTeacherRecognized.requestFocus();
            }
            return;
        }

        cancelAnimations();
        isRecording = true;
        currentOutputAudioFile = null;

        int primaryColor = ThemeHelper.getThemeColor(requireContext(), R.attr.customPrimaryColor);
        int onPrimaryColor = ContextCompat.getColor(requireContext(), R.color.white);

        ivTeacherControlIcon.setImageResource(R.drawable.ic_mic);
        ivTeacherControlIcon.setImageTintList(ColorStateList.valueOf(onPrimaryColor));
        btnTeacherControl.setCardBackgroundColor(primaryColor);
        tvTeacherStatus.setText(R.string.recording_hindi);
        badgeTeacherState.setText("Listening");

        startPulseAnimation(viewTeacherRipple);

        // Reset student result view for new recording
        tvStudentStatus.setText(R.string.placeholder_awaiting_translation);
        badgeStudentState.setText("Waiting");
        setPlayButtonEnabled(false);

        tvEngineStatus.setText("Recording Hindi speech...");

        File cacheDir = requireContext().getCacheDir();
        File inputWav = new File(cacheDir, "recorded_speech_" + System.currentTimeMillis() + ".wav");

        audioRecorder.startRecording(inputWav, new AudioRecorder.RecordingCallback() {
            @Override public void onRecordingStarted() {
                Log.i(TAG, "Audio recording started: " + inputWav.getAbsolutePath());
            }
            @Override public void onRecordingStopped(File outputFile) {
                Log.i(TAG, "Audio recording stopped: " + outputFile.getAbsolutePath());
            }
            @Override public void onError(String errorMessage) {
                Log.e(TAG, "Audio recording error: " + errorMessage);
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Recording error: " + errorMessage, Toast.LENGTH_SHORT).show();
                    resetToIdle();
                }
            }
        });
    }

    /**
     * Stops recording and feeds the captured audio into the offline AI pipeline.
     */
    private void stopRecordingAndProcess() {
        if (!isRecording) return;
        isRecording = false;
        cancelAnimations();

        // Start monotonic timing measurement immediately when Translate is triggered
        onTranslationStarted();

        audioRecorder.stopRecording();

        Context context = getContext();
        if (context == null) {
            resetToIdle();
            onTranslationFailed("Context unavailable");
            return;
        }

        File cacheDir = context.getCacheDir();
        File[] wavFiles = cacheDir.listFiles((dir, name) -> name.startsWith("recorded_speech_") && name.endsWith(".wav"));
        File latestWav = null;
        if (wavFiles != null && wavFiles.length > 0) {
            for (File f : wavFiles) {
                if (latestWav == null || f.lastModified() > latestWav.lastModified()) {
                    latestWav = f;
                }
            }
        }

        String speechRecogMsg = isHindiToSantali ? getString(R.string.understanding_hindi) : "Recognizing speech...";
        tvTeacherStatus.setText(speechRecogMsg);
        badgeTeacherState.setText("ASR");
        tvEngineStatus.setText(speechRecogMsg);

        File outputTtsFile = new File(cacheDir, "tts_output_" + System.currentTimeMillis() + ".wav");

        if (latestWav != null && latestWav.exists() && latestWav.length() > 44) {
            // Process captured audio through local offline Whisper ASR and IndicTrans2
            voicePipelineManager.processAsync(latestWav, outputTtsFile, isHindiToSantali, new VoicePipelineManager.PipelineCallback() {
                @Override
                public void onProgress(String stageMessage) {
                    if (!isAdded()) return;
                    tvEngineStatus.setText(stageMessage);
                    if (stageMessage.contains("Translating")) {
                        String transMsg = isHindiToSantali ? getString(R.string.translating_to_santhali) : "Translating to Hindi...";
                        tvTeacherStatus.setText(transMsg);
                        badgeTeacherState.setText("Translating");
                        badgeStudentState.setText("Translating");
                    } else if (stageMessage.contains("Generating")) {
                        String msg = isHindiToSantali ? getString(R.string.generating_santali_speech) : "Generating Hindi speech...";
                        tvTeacherStatus.setText(msg);
                        tvStudentStatus.setText(msg);
                        badgeTeacherState.setText("Generating");
                        badgeStudentState.setText("Generating");
                    }
                }

                @Override
                public void onComplete(PipelineResult result) {
                    if (!isAdded()) return;

                    if (result.isSuccess && result.recognizedHindiText != null && !result.recognizedHindiText.trim().isEmpty()) {
                        // 1. Display recognized source text
                        tvTeacherRecognized.setText(result.recognizedHindiText);

                        // 2. Display translated target text
                        tvStudentTranslated.setText(result.translatedSantaliText);

                        tvTeacherStatus.setText(R.string.status_ready);
                        badgeTeacherState.setText("Done");

                        // 3. Audio synthesis status
                        if (result.outputAudioFile != null && result.outputAudioFile.exists() && result.outputAudioFile.length() > 44) {
                            currentOutputAudioFile = result.outputAudioFile;
                            tvStudentStatus.setText(R.string.speech_ready);
                            tvPlaySantaliLabel.setText(isHindiToSantali ? "Play Santali" : "Play Hindi");
                            badgeStudentState.setText("Ready");
                            setPlayButtonEnabled(true);
                            tvEngineStatus.setText(R.string.speech_ready);
                        } else {
                            currentOutputAudioFile = null;
                            tvStudentStatus.setText("Text translation ready (TTS unavailable)");
                            tvPlaySantaliLabel.setText(isHindiToSantali ? "Play Santali" : "Play Hindi");
                            badgeStudentState.setText("Ready");
                            setPlayButtonEnabled(false);
                            tvEngineStatus.setText("Text translation ready (TTS unavailable)");
                        }

                        // Complete performance measurement and update card
                        onTranslationCompleted(result);
                    } else {
                        // Error handling: do not fabricate translations or return raw errors
                        tvTeacherStatus.setText(R.string.asr_failed);
                        badgeTeacherState.setText("Failed");
                        tvStudentStatus.setText(R.string.translation_failed);
                        badgeStudentState.setText("Failed");
                        tvEngineStatus.setText(result.errorMessage != null ? result.errorMessage : "Could not understand the recording.");
                        onTranslationFailed(result.errorMessage);
                        resetToIdleDelayed(3000);
                    }
                }
            });
        } else {
            tvTeacherStatus.setText(R.string.no_speech_detected);
            onTranslationFailed("No speech detected");
            resetToIdleDelayed(2500);
        }
    }

    /**
     * Resets the Translation Performance Card to the standby state before translation.
     */
    private void resetPerformanceCard() {
        if (!isAdded() || tvPerformanceTime == null) return;
        tvPerformanceTime.setText("--.-- s");
        tvPerformanceDesc.setText("Time will appear after translation");
        if (tvPerformanceBreakdown != null) {
            tvPerformanceBreakdown.setVisibility(View.GONE);
        }
        updateRecentHistoryView();
    }

    /**
     * Triggered immediately when Translate is initiated (Translate button or voice stop).
     * Records the monotonic start timestamp using SystemClock.elapsedRealtime()
     * and shows the processing indicator.
     */
    private void onTranslationStarted() {
        if (!isAdded() || tvPerformanceTime == null) return;
        translationStartTime = SystemClock.elapsedRealtime();
        tvPerformanceTime.setText("Processing...");
        tvPerformanceDesc.setText("Measuring translation time");
        if (tvPerformanceBreakdown != null) {
            tvPerformanceBreakdown.setVisibility(View.GONE);
        }
    }

    /**
     * Triggered when translation finishes successfully.
     * Computes the actual elapsed duration using SystemClock.elapsedRealtime()
     * and displays the measured seconds formatted to 2 decimal places.
     */
    private void onTranslationCompleted(PipelineResult result) {
        if (!isAdded() || tvPerformanceTime == null) return;
        long elapsedRealtimeMs = SystemClock.elapsedRealtime() - translationStartTime;
        if (elapsedRealtimeMs < 0) elapsedRealtimeMs = 0;

        double seconds = elapsedRealtimeMs / 1000.0;
        String timeStr = String.format(Locale.US, "%.2f s", seconds);

        tvPerformanceTime.setText(timeStr);
        tvPerformanceDesc.setText("Time taken for this translation");

        // Display individual stage breakdown if provided by pipeline
        if (result != null && (result.asrLatencyMs > 0 || result.translationLatencyMs > 0)) {
            StringBuilder sb = new StringBuilder();
            if (result.asrLatencyMs > 0) {
                sb.append(String.format(Locale.US, "ASR: %.2f s", result.asrLatencyMs / 1000.0));
            }
            if (result.translationLatencyMs > 0) {
                if (sb.length() > 0) sb.append("  •  ");
                sb.append(String.format(Locale.US, "Translation: %.2f s", result.translationLatencyMs / 1000.0));
            }
            if (result.totalLatencyMs > 0) {
                if (sb.length() > 0) sb.append("  •  ");
                sb.append(String.format(Locale.US, "Pipeline: %.2f s", result.totalLatencyMs / 1000.0));
            }
            if (tvPerformanceBreakdown != null) {
                tvPerformanceBreakdown.setText(sb.toString());
                tvPerformanceBreakdown.setVisibility(View.VISIBLE);
            }
        } else if (tvPerformanceBreakdown != null) {
            tvPerformanceBreakdown.setVisibility(View.GONE);
        }

        // Store in recent performance history (last 3 entries)
        recentPerformanceHistory.add(0, timeStr);
        while (recentPerformanceHistory.size() > 3) {
            recentPerformanceHistory.remove(recentPerformanceHistory.size() - 1);
        }
        updateRecentHistoryView();
    }

    /**
     * Triggered when translation fails, showing clear failure feedback.
     */
    private void onTranslationFailed(String errorMessage) {
        if (!isAdded() || tvPerformanceTime == null) return;
        long elapsedRealtimeMs = translationStartTime > 0 ? (SystemClock.elapsedRealtime() - translationStartTime) : 0;
        tvPerformanceTime.setText("Translation failed");
        if (elapsedRealtimeMs > 0) {
            double seconds = elapsedRealtimeMs / 1000.0;
            tvPerformanceDesc.setText(String.format(Locale.US, "Failed after %.2f s", seconds));
        } else {
            tvPerformanceDesc.setText("No completed translation time available");
        }
        if (tvPerformanceBreakdown != null) {
            tvPerformanceBreakdown.setVisibility(View.GONE);
        }
    }

    /**
     * Updates the compact recent performance history section (last 3 translation times).
     */
    private void updateRecentHistoryView() {
        if (!isAdded() || tvPerformanceHistory == null) return;
        if (recentPerformanceHistory.isEmpty()) {
            tvPerformanceHistory.setVisibility(View.GONE);
        } else {
            StringBuilder sb = new StringBuilder("Recent Performance: ");
            for (int i = 0; i < recentPerformanceHistory.size(); i++) {
                if (i > 0) sb.append("  •  ");
                sb.append(recentPerformanceHistory.get(i));
            }
            tvPerformanceHistory.setText(sb.toString());
            tvPerformanceHistory.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Translates typed or recognized text directly on background thread,
     * maintaining the exact pipeline behavior and measured runtime timing.
     */
    /**
     * Translates typed or recognized text directly on background thread,
     * maintaining the exact pipeline behavior and measured runtime timing.
     * Supports both Hindi -> Santali and Santali -> Hindi directions.
     */
    private void processTextTranslation(String inputText) {
        if (voicePipelineManager == null || !voicePipelineManager.isInitialized()) {
            Toast.makeText(requireContext(), "AI Pipeline is initializing...", Toast.LENGTH_SHORT).show();
            return;
        }

        onTranslationStarted();

        tvTeacherStatus.setText(isHindiToSantali ? getString(R.string.translating_to_santhali) : "Translating to Hindi...");
        badgeTeacherState.setText("Translating");
        badgeStudentState.setText("Translating");
        tvStudentStatus.setText(R.string.placeholder_awaiting_translation);
        tvEngineStatus.setText(isHindiToSantali ? "Translating to Santali..." : "Translating to Hindi...");
        setPlayButtonEnabled(false);

        File cacheDir = requireContext().getCacheDir();
        File outputTtsFile = new File(cacheDir, "tts_output_" + System.currentTimeMillis() + ".wav");

        new Thread(() -> {
            long t0 = System.currentTimeMillis();
            try {
                // Translation Stage
                long tTrans0 = System.currentTimeMillis();
                String translatedText = null;
                String normalized = com.vernacular.learning.utils.TwoWayTranslationHelper.normalize(inputText);

                if (isHindiToSantali) {
                    if (com.vernacular.learning.utils.TwoWayTranslationHelper.hasVerifiedTranslation(normalized)) {
                        translatedText = com.vernacular.learning.utils.TwoWayTranslationHelper.getVerifiedTranslation(normalized);
                    }
                } else {
                    if (com.vernacular.learning.utils.TwoWayTranslationHelper.hasVerifiedSantaliTranslation(normalized)) {
                        translatedText = com.vernacular.learning.utils.TwoWayTranslationHelper.getVerifiedSantaliTranslation(normalized);
                    }
                }

                if (translatedText == null || translatedText.isEmpty()) {
                    translatedText = voicePipelineManager.getTranslationManager().translate(inputText, isHindiToSantali);
                }

                if (translatedText == null || translatedText.isEmpty()) {
                    if (isHindiToSantali) {
                        translatedText = com.vernacular.learning.utils.TwoWayTranslationHelper.getVerifiedTranslation(normalized);
                    } else {
                        translatedText = com.vernacular.learning.utils.TwoWayTranslationHelper.getVerifiedSantaliTranslation(normalized);
                    }
                }
                if (translatedText == null) {
                    translatedText = "";
                }
                long transDuration = System.currentTimeMillis() - tTrans0;

                // TTS Stage
                long tTts0 = System.currentTimeMillis();
                File outputAudio = null;
                if (isHindiToSantali) {
                    if (voicePipelineManager.getTtsManager().isAvailable()) {
                        outputAudio = voicePipelineManager.getTtsManager().synthesize(translatedText, outputTtsFile);
                    }
                } else {
                    if (voicePipelineManager.getTtsManager().isHindiAvailable()) {
                        boolean ok = voicePipelineManager.getTtsManager().synthesizeHindi(translatedText, outputTtsFile);
                        if (ok) outputAudio = outputTtsFile;
                    }
                }
                long ttsDuration = System.currentTimeMillis() - tTts0;
                long totalDuration = System.currentTimeMillis() - t0;

                PipelineResult result = PipelineResult.success(
                        inputText,
                        translatedText,
                        outputAudio,
                        0,
                        transDuration,
                        ttsDuration,
                        totalDuration
                );

                final String finalTranslated = translatedText;
                final File finalAudio = outputAudio;

                handler.post(() -> {
                    if (!isAdded()) return;

                    tvStudentTranslated.setText(finalTranslated);
                    tvTeacherStatus.setText(R.string.status_ready);
                    badgeTeacherState.setText("Done");

                    String playLabel = isHindiToSantali ? "Play Santali" : "Play Hindi";

                    if (finalAudio != null && finalAudio.exists() && finalAudio.length() > 44) {
                        currentOutputAudioFile = finalAudio;
                        tvStudentStatus.setText(R.string.speech_ready);
                        tvPlaySantaliLabel.setText(playLabel);
                        badgeStudentState.setText("Ready");
                        setPlayButtonEnabled(true);
                        tvEngineStatus.setText(R.string.speech_ready);
                    } else {
                        currentOutputAudioFile = null;
                        tvStudentStatus.setText("Text translation ready (TTS unavailable)");
                        tvPlaySantaliLabel.setText(playLabel);
                        badgeStudentState.setText("Ready");
                        setPlayButtonEnabled(false);
                        tvEngineStatus.setText("Text translation ready (TTS unavailable)");
                    }

                    onTranslationCompleted(result);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error during text translation", e);
                handler.post(() -> {
                    if (!isAdded()) return;
                    tvTeacherStatus.setText(R.string.status_ready);
                    badgeTeacherState.setText("Failed");
                    tvStudentStatus.setText(R.string.translation_failed);
                    badgeStudentState.setText("Failed");
                    tvEngineStatus.setText("Translation failed: " + e.getMessage());
                    onTranslationFailed(e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Updates all UI labels and placeholders according to the selected translation direction.
     */
    private void updateDirectionUI() {
        if (!isAdded()) return;

        if (isHindiToSantali) {
            if (tvDirectionInputText != null) tvDirectionInputText.setText("Teacher • Hindi (हिंदी)");
            if (tvDirectionOutputText != null) tvDirectionOutputText.setText("Student • Santali (संथाली)");
            if (tvTeacherPanelTitle != null) tvTeacherPanelTitle.setText("Teacher (शिक्षक) • Hindi");
            if (tvTeacherStatus != null) tvTeacherStatus.setText(R.string.tap_to_speak_hindi);
            if (tvTeacherInputLabel != null) tvTeacherInputLabel.setText("Recognized Hindi (पहचानी गई हिंदी)");
            if (tvTeacherRecognized != null) tvTeacherRecognized.setHint("Awaiting Hindi speech or text...");
            if (btnTranslate != null) btnTranslate.setText("अनुवाद करें (Translate to Santali)");
            if (tvFlowDividerText != null) tvFlowDividerText.setText("Hindi → Santali (हिंदी → संथाली)");
            if (tvStudentPanelTitle != null) tvStudentPanelTitle.setText("Student (छात्र) • Santali");
            if (tvStudentOutputLabel != null) tvStudentOutputLabel.setText("Santali Translation (संथाली अनुवाद)");
            if (tvStudentTranslated != null) tvStudentTranslated.setHint(R.string.placeholder_awaiting_translation);
            if (tvPlaySantaliLabel != null) tvPlaySantaliLabel.setText("Play Santali");
        } else {
            if (tvDirectionInputText != null) tvDirectionInputText.setText("Teacher • Santali (संथाली)");
            if (tvDirectionOutputText != null) tvDirectionOutputText.setText("Student • Hindi (हिंदी)");
            if (tvTeacherPanelTitle != null) tvTeacherPanelTitle.setText("Teacher (शिक्षक) • Santali");
            if (tvTeacherStatus != null) tvTeacherStatus.setText("Enter or select Santali text");
            if (tvTeacherInputLabel != null) tvTeacherInputLabel.setText("Santali Text / Input (संथाली इनपुट)");
            if (tvTeacherRecognized != null) tvTeacherRecognized.setHint("Awaiting Santali speech or text...");
            if (btnTranslate != null) btnTranslate.setText("अनुवाद करें (Translate to Hindi)");
            if (tvFlowDividerText != null) tvFlowDividerText.setText("Santali → Hindi (संथाली → हिंदी)");
            if (tvStudentPanelTitle != null) tvStudentPanelTitle.setText("Student (छात्र) • Hindi");
            if (tvStudentOutputLabel != null) tvStudentOutputLabel.setText("Hindi Translation (हिंदी अनुवाद)");
            if (tvStudentTranslated != null) tvStudentTranslated.setHint("Awaiting Hindi translation...");
            if (tvPlaySantaliLabel != null) tvPlaySantaliLabel.setText("Play Hindi");
        }
    }

    private void setPlayButtonEnabled(boolean enabled) {
        if (!isAdded() || btnPlaySantali == null) return;
        btnPlaySantali.setEnabled(enabled);
        if (enabled) {
            int primaryColor = ThemeHelper.getThemeColor(requireContext(), R.attr.customPrimaryColor);
            btnPlaySantali.setCardBackgroundColor(primaryColor);
            tvPlaySantaliLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            ivPlaySantaliIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white)));
        } else {
            int disabledBg = ThemeHelper.getThemeColor(requireContext(), R.attr.customBorderColor);
            int disabledText = ThemeHelper.getThemeColor(requireContext(), R.attr.customTextSecondaryColor);
            btnPlaySantali.setCardBackgroundColor(disabledBg);
            tvPlaySantaliLabel.setTextColor(disabledText);
            ivPlaySantaliIcon.setImageTintList(ColorStateList.valueOf(disabledText));
        }
    }

    private void resetToIdleDelayed(long delayMs) {
        handler.postDelayed(this::resetToIdle, delayMs);
    }

    /**
     * Resets the interface to the idle ready state.
     */
    private void resetToIdle() {
        if (!isAdded()) return;

        isRecording = false;
        cancelAnimations();

        if (audioRecorder != null) {
            audioRecorder.stopRecording();
        }

        int primaryColor = ThemeHelper.getThemeColor(requireContext(), R.attr.customPrimaryColor);
        int onPrimaryColor = ContextCompat.getColor(requireContext(), R.color.white);

        ivTeacherControlIcon.setImageResource(R.drawable.ic_mic);
        ivTeacherControlIcon.setImageTintList(ColorStateList.valueOf(onPrimaryColor));
        btnTeacherControl.setCardBackgroundColor(primaryColor);
        viewTeacherRipple.setVisibility(View.INVISIBLE);
        tvTeacherStatus.setText(isHindiToSantali ? getString(R.string.tap_to_speak_hindi) : "Enter or select Santali text");
        badgeTeacherState.setText("Ready");

        if (badgeStudentState.getText().toString().equals("Waiting")) {
            badgeStudentState.setText("Ready");
        }

        if (voicePipelineManager != null && voicePipelineManager.isInitialized()) {
            if (voicePipelineManager.getTtsManager().isAvailable()) {
                tvEngineStatus.setText("Offline AI Pipeline Ready (ASR, Translation & TTS)");
            } else {
                tvEngineStatus.setText("Offline AI Pipeline Ready (ASR & IndicTrans2 | TTS Unavailable)");
            }
        } else {
            tvEngineStatus.setText(R.string.status_ready);
        }
    }

    private void startPulseAnimation(View rippleView) {
        rippleView.setVisibility(View.VISIBLE);
        if (pulseAnimator != null && pulseAnimator.isRunning()) {
            pulseAnimator.cancel();
        }

        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.45f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.45f);
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.6f, 0.0f);

        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(rippleView, scaleX, scaleY, alpha);
        pulseAnimator.setDuration(1000);
        pulseAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ObjectAnimator.RESTART);
        pulseAnimator.start();
    }

    private void cancelAnimations() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
            pulseAnimator = null;
        }
        if (viewTeacherRipple != null) {
            viewTeacherRipple.setVisibility(View.INVISIBLE);
            viewTeacherRipple.setScaleX(1.0f);
            viewTeacherRipple.setScaleY(1.0f);
            viewTeacherRipple.setAlpha(1.0f);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (audioRecorder != null && audioRecorder.isRecording()) {
            audioRecorder.stopRecording();
        }
        if (audioPlayer != null && audioPlayer.isPlaying()) {
            audioPlayer.stop();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cancelAnimations();
        handler.removeCallbacksAndMessages(null);
        if (audioRecorder != null) {
            audioRecorder.release();
            audioRecorder = null;
        }
        if (audioPlayer != null) {
            audioPlayer.release();
            audioPlayer = null;
        }
        if (voicePipelineManager != null) {
            voicePipelineManager.close();
            voicePipelineManager = null;
        }
    }
}
