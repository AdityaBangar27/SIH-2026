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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.android.material.card.MaterialCardView;
import com.vernacular.learning.R;
import com.vernacular.learning.ai.PipelineResult;
import com.vernacular.learning.ai.VoicePipelineManager;
import com.vernacular.learning.utils.AudioPlayer;
import com.vernacular.learning.utils.AudioRecorder;
import com.vernacular.learning.utils.ThemeHelper;
import java.io.File;

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

    private boolean isRecording = false;
    private File currentOutputAudioFile = null;

    // Teacher Panel Views
    private MaterialCardView btnTeacherControl;
    private ImageView ivTeacherControlIcon;
    private View viewTeacherRipple;
    private TextView tvTeacherStatus;
    private TextView badgeTeacherState;
    private TextView tvTeacherRecognized;

    // Student Panel Views
    private TextView badgeStudentState;
    private TextView tvStudentTranslated;
    private TextView tvStudentStatus;
    private MaterialCardView btnPlaySantali;
    private ImageView ivPlaySantaliIcon;
    private TextView tvPlaySantaliLabel;

    // Engine Status
    private TextView tvEngineStatus;

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
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // Teacher Panel bindings
        btnTeacherControl = root.findViewById(R.id.btnTeacherControl);
        ivTeacherControlIcon = root.findViewById(R.id.ivTeacherControlIcon);
        viewTeacherRipple = root.findViewById(R.id.viewTeacherRipple);
        tvTeacherStatus = root.findViewById(R.id.tvTeacherStatus);
        badgeTeacherState = root.findViewById(R.id.badgeTeacherState);
        tvTeacherRecognized = root.findViewById(R.id.tvTeacherRecognized);

        // Student Panel bindings
        badgeStudentState = root.findViewById(R.id.badgeStudentState);
        tvStudentTranslated = root.findViewById(R.id.tvStudentTranslated);
        tvStudentStatus = root.findViewById(R.id.tvStudentStatus);
        btnPlaySantali = root.findViewById(R.id.btnPlaySantali);
        ivPlaySantaliIcon = root.findViewById(R.id.ivPlaySantaliIcon);
        tvPlaySantaliLabel = root.findViewById(R.id.tvPlaySantaliLabel);

        // Engine Status
        tvEngineStatus = root.findViewById(R.id.tvEngineStatus);

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
        resetToIdle();

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
        // Teacher Microphone Control
        btnTeacherControl.setOnClickListener(v -> {
            if (voicePipelineManager != null && voicePipelineManager.isBusy()) {
                Toast.makeText(requireContext(), "AI pipeline is busy processing...", Toast.LENGTH_SHORT).show();
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

        // Santali Audio Playback button
        btnPlaySantali.setOnClickListener(v -> {
            if (currentOutputAudioFile != null && currentOutputAudioFile.exists() && currentOutputAudioFile.length() > 44) {
                audioPlayer.play(currentOutputAudioFile, new AudioPlayer.PlaybackCallback() {
                    @Override
                    public void onPlaybackStarted() {
                        if (isAdded()) {
                            tvPlaySantaliLabel.setText("Playing Santali audio...");
                            tvStudentStatus.setText("Playing Santali audio...");
                            badgeStudentState.setText("Playing");
                        }
                    }

                    @Override
                    public void onPlaybackCompleted() {
                        if (isAdded()) {
                            tvPlaySantaliLabel.setText(R.string.speech_ready);
                            tvStudentStatus.setText(R.string.speech_ready);
                            badgeStudentState.setText("Ready");
                        }
                    }

                    @Override
                    public void onError(String message) {
                        if (isAdded()) {
                            tvPlaySantaliLabel.setText(R.string.speech_ready);
                            tvStudentStatus.setText("Playback error.");
                            badgeStudentState.setText("Ready");
                        }
                    }
                });
            } else {
                Toast.makeText(requireContext(), "Santali TTS audio unavailable.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Starts recording Hindi speech from the microphone.
     */
    private void startRecording() {
        if (!isAdded()) return;

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

        audioRecorder.stopRecording();

        Context context = getContext();
        if (context == null) {
            resetToIdle();
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

        tvTeacherStatus.setText(R.string.understanding_hindi);
        badgeTeacherState.setText("ASR");
        tvEngineStatus.setText(R.string.understanding_hindi);

        File outputTtsFile = new File(cacheDir, "tts_output_" + System.currentTimeMillis() + ".wav");

        if (latestWav != null && latestWav.exists() && latestWav.length() > 44) {
            // Process captured audio through local offline Whisper ASR and IndicTrans2
            voicePipelineManager.processAsync(latestWav, outputTtsFile, new VoicePipelineManager.PipelineCallback() {
                @Override
                public void onProgress(String stageMessage) {
                    if (!isAdded()) return;
                    tvEngineStatus.setText(stageMessage);
                    if (stageMessage.contains("Translating")) {
                        tvTeacherStatus.setText(R.string.translating_to_santhali);
                        badgeTeacherState.setText("Translating");
                        badgeStudentState.setText("Translating");
                    }
                }

                @Override
                public void onComplete(PipelineResult result) {
                    if (!isAdded()) return;

                    if (result.isSuccess && result.recognizedHindiText != null && !result.recognizedHindiText.trim().isEmpty()) {
                        // 1. Display recognized Hindi Unicode
                        tvTeacherRecognized.setText(result.recognizedHindiText);

                        // 2. Display translated Santali Ol Chiki Unicode
                        tvStudentTranslated.setText(result.translatedSantaliText);

                        tvTeacherStatus.setText(R.string.status_ready);
                        badgeTeacherState.setText("Done");

                        // 3. Audio synthesis status
                        if (result.outputAudioFile != null && result.outputAudioFile.exists() && result.outputAudioFile.length() > 44) {
                            currentOutputAudioFile = result.outputAudioFile;
                            tvStudentStatus.setText(R.string.speech_ready);
                            tvPlaySantaliLabel.setText(R.string.speech_ready);
                            badgeStudentState.setText("Ready");
                            setPlayButtonEnabled(true);
                            tvEngineStatus.setText("Speech ready. Tap Play to listen.");
                        } else {
                            currentOutputAudioFile = null;
                            tvStudentStatus.setText("Text translation ready (TTS unavailable)");
                            tvPlaySantaliLabel.setText("Play Santali Speech");
                            badgeStudentState.setText("Ready");
                            setPlayButtonEnabled(false);
                            tvEngineStatus.setText("Text translation ready (TTS unavailable)");
                        }
                    } else {
                        // Error handling: do not fabricate translations or return raw errors
                        tvTeacherStatus.setText(R.string.asr_failed);
                        badgeTeacherState.setText("Failed");
                        tvStudentStatus.setText(R.string.translation_failed);
                        badgeStudentState.setText("Failed");
                        tvEngineStatus.setText(result.errorMessage != null ? result.errorMessage : "Could not understand the recording.");
                        resetToIdleDelayed(3000);
                    }
                }
            });
        } else {
            tvTeacherStatus.setText(R.string.no_speech_detected);
            resetToIdleDelayed(2500);
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
        tvTeacherStatus.setText(R.string.tap_to_speak_hindi);
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
