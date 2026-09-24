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
        this.ttsManager = new TTSManager();
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
     */
    public void processAsync(File audioInputFile, File audioOutputFile, PipelineCallback callback) {
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
                postProgress(callback, "Understanding Hindi...");
                long tAsr0 = System.currentTimeMillis();
                String hindiText = asrManager.transcribe(audioInputFile);
                long asrDuration = System.currentTimeMillis() - tAsr0;

                if (hindiText == null || hindiText.trim().isEmpty()) {
                    postResult(callback, new PipelineResult(
                            "", "", audioOutputFile, asrDuration, 0, 0,
                            System.currentTimeMillis() - t0, false, "ASR produced no transcription text."
                    ));
                    return;
                }

                Log.i(TAG, "ASR Output [" + asrDuration + " ms]: " + hindiText);

                // 2. Translation Stage
                postProgress(callback, "Translating to Santali...");
                long tTrans0 = System.currentTimeMillis();
                String santaliText = null;
                String normalized = com.vernacular.learning.utils.TwoWayTranslationHelper.normalize(hindiText);
                if (com.vernacular.learning.utils.TwoWayTranslationHelper.hasVerifiedTranslation(normalized)) {
                    santaliText = com.vernacular.learning.utils.TwoWayTranslationHelper.getVerifiedTranslation(normalized);
                }
                if (santaliText == null || santaliText.isEmpty()) {
                    santaliText = translationManager.translate(hindiText);
                }
                if (santaliText == null || santaliText.isEmpty()) {
                    santaliText = com.vernacular.learning.utils.TwoWayTranslationHelper.getVerifiedTranslation(normalized);
                }
                if (santaliText == null) {
                    santaliText = "";
                }
                long transDuration = System.currentTimeMillis() - tTrans0;

                Log.i(TAG, "Translation Output [" + transDuration + " ms]: " + santaliText);

                // 3. TTS Stage
                postProgress(callback, "Generating Santali speech...");
                long tTts0 = System.currentTimeMillis();
                File outputAudio = null;
                if (ttsManager.isAvailable()) {
                    outputAudio = ttsManager.synthesize(santaliText, audioOutputFile);
                } else {
                    Log.i(TAG, "TTS stage skipped (" + ttsManager.getStatusMessage() + ")");
                }
                long ttsDuration = System.currentTimeMillis() - tTts0;

                long totalDuration = System.currentTimeMillis() - t0;

                Log.i(TAG, String.format("Pipeline Benchmark -> Total: %d ms | ASR: %d ms | Translation: %d ms | TTS: %d ms",
                        totalDuration, asrDuration, transDuration, ttsDuration));

                PipelineResult result = PipelineResult.success(
                        hindiText,
                        santaliText,
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
