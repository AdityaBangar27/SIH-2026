package com.vernacular.learning.ai;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Orchestrates the sequential AI pipeline (ASR -> Translation -> TTS)
 * on a dedicated background thread, providing latency metrics and progress callbacks.
 * Enforces single-run execution without concurrent pipeline overlap.
 */
public class VoicePipelineManager {
    private static final String TAG = "VoicePipelineManager";

    private final ASRManager asrManager;
    private final TranslationManager translationManager;
    private final TTSManager ttsManager;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isInitialized = false;
    private final AtomicBoolean isBusy = new AtomicBoolean(false);

    public interface InitCallback {
        void onInitialized(boolean success);
    }

    public interface PipelineCallback {
        void onProgress(String stageMessage);
        void onComplete(PipelineResult result);
    }

    public VoicePipelineManager() {
        this.asrManager = new ASRManager();
        this.translationManager = new TranslationManager();
        this.ttsManager = TTSManager.getInstance();
    }

    public void initializeAsync(Context context, InitCallback callback) {
        executor.execute(() -> {
            Log.i(TAG, "Initializing local offline AI pipeline...");
            long t0 = System.currentTimeMillis();

            boolean asrOk = asrManager.initialize(context);
            boolean transOk = translationManager.initialize(context);
            boolean ttsOk = ttsManager.initialize(context);

            long initDuration = System.currentTimeMillis() - t0;
            Log.i(TAG, "AI pipeline initialization finished in " + initDuration + " ms." +
                    " ASR: " + (asrOk ? "OK" : "FAIL") +
                    ", Translation: " + (transOk ? "OK" : "FAIL") +
                    ", TTS: " + (ttsOk ? "OK" : "FAIL (" + ttsManager.getStatusMessage() + ")"));

            // ASR and Translation are primary requirements for voice translation
            isInitialized = asrOk && transOk;

            if (callback != null) {
                mainHandler.post(() -> callback.onInitialized(isInitialized));
            }
        });
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public boolean isBusy() {
        return isBusy.get();
    }

    public ASRManager getAsrManager() {
        return asrManager;
    }

    public TranslationManager getTranslationManager() {
        return translationManager;
    }

    public TTSManager getTtsManager() {
        return ttsManager;
    }

    /**
     * Processes input audio through the complete offline AI pipeline.
     * Defaults to Hindi -> Santali.
     */
    public void processAsync(File audioInputFile, File audioOutputFile, PipelineCallback callback) {
        processAsync(audioInputFile, audioOutputFile, true, callback);
    }

    /**
     * Processes input audio with explicit direction support.
     * @param isHindiToSantali true for Hindi -> Santali, false for Santali -> Hindi.
     */
    public void processAsync(File audioInputFile, File audioOutputFile, boolean isHindiToSantali, PipelineCallback callback) {
        if (!isBusy.compareAndSet(false, true)) {
            Log.w(TAG, "Pipeline is already busy processing another request.");
            return;
        }

        executor.execute(() -> {
            long t0 = System.currentTimeMillis();

            try {
                if (!isInitialized) {
                    postResult(callback, PipelineResult.failure("AI Pipeline is not initialized.", System.currentTimeMillis() - t0));
                    return;
                }

                // 1. ASR Stage
                postProgress(callback, isHindiToSantali ? "Understanding Hindi..." : "Recognizing speech...");
                long tAsr0 = System.currentTimeMillis();
                String recognizedText = asrManager.transcribe(audioInputFile);
                long asrDuration = System.currentTimeMillis() - tAsr0;

                if (recognizedText == null || recognizedText.trim().isEmpty()) {
                    postResult(callback, new PipelineResult(
                            "", "", audioOutputFile, asrDuration, 0, 0,
                            System.currentTimeMillis() - t0, false, "ASR produced no transcription text."
                    ));
                    return;
                }

                Log.i(TAG, "ASR Output [" + asrDuration + " ms]: " + recognizedText);

                // 2. Translation Stage
                postProgress(callback, isHindiToSantali ? "Translating to Santali..." : "Translating to Hindi...");
                long tTrans0 = System.currentTimeMillis();
                String translatedText = null;
                String normalized = com.vernacular.learning.utils.TwoWayTranslationHelper.normalize(recognizedText);

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
                    translatedText = translationManager.translate(recognizedText, isHindiToSantali);
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

                Log.i(TAG, "Translation Output [" + transDuration + " ms]: " + translatedText);

                // 3. TTS Stage
                postProgress(callback, isHindiToSantali ? "Generating Santali speech..." : "Generating Hindi speech...");
                long tTts0 = System.currentTimeMillis();
                File outputAudio = null;
                if (isHindiToSantali) {
                    if (ttsManager.isAvailable()) {
                        outputAudio = ttsManager.synthesize(translatedText, audioOutputFile);
                    } else {
                        Log.i(TAG, "Santali TTS stage skipped (" + ttsManager.getStatusMessage() + ")");
                    }
                } else {
                    if (ttsManager.isHindiAvailable()) {
                        boolean ok = ttsManager.synthesizeHindi(translatedText, audioOutputFile);
                        if (ok) outputAudio = audioOutputFile;
                    }
                }
                long ttsDuration = System.currentTimeMillis() - tTts0;

                long totalDuration = System.currentTimeMillis() - t0;

                Log.i(TAG, String.format("Pipeline Benchmark -> Total: %d ms | ASR: %d ms | Translation: %d ms | TTS: %d ms",
                        totalDuration, asrDuration, transDuration, ttsDuration));

                PipelineResult result = PipelineResult.success(
                        recognizedText,
                        translatedText,
                        outputAudio,
                        asrDuration,
                        transDuration,
                        ttsDuration,
                        totalDuration
                );

                postResult(callback, result);
            } finally {
                isBusy.set(false);
            }
        });
    }

    private void postProgress(PipelineCallback callback, String message) {
        if (callback != null) {
            mainHandler.post(() -> callback.onProgress(message));
        }
    }

    private void postResult(PipelineCallback callback, PipelineResult result) {
        if (callback != null) {
            mainHandler.post(() -> callback.onComplete(result));
        }
    }

    public void close() {
        try {
            executor.submit(() -> {
                asrManager.close();
                translationManager.close();
                ttsManager.close();
                ModelManager.getInstance().close();
            }).get(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception ignored) {
        } finally {
            executor.shutdown();
        }
    }
}
