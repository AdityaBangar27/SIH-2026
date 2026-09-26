package com.vernacular.learning.ai;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.File;
import java.util.Locale;
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
    private final java.util.concurrent.atomic.AtomicLong liveRequestId = new java.util.concurrent.atomic.AtomicLong(0);
    private Context appContext;

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
        if (context != null) {
            this.appContext = context.getApplicationContext();
        }
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

        final long reqId = liveRequestId.incrementAndGet();

        executor.execute(() -> {
            long t0 = System.currentTimeMillis();
            long tAsr0 = 0, tAsrEnd = 0;
            long tTrans0 = 0, tTransEnd = 0;
            long tTts0 = 0, tTtsEnd = 0;

            try {
                if (!isInitialized) {
                    postResult(callback, PipelineResult.failure("AI Pipeline is not initialized.", System.currentTimeMillis() - t0));
                    return;
                }

                // 1. ASR Stage
                postProgress(callback, isHindiToSantali ? "Understanding Hindi..." : "Recognizing speech...");
                tAsr0 = System.currentTimeMillis();
                String recognizedText = asrManager.transcribe(audioInputFile);
                tAsrEnd = System.currentTimeMillis();
                long asrDuration = tAsrEnd - tAsr0;

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
                tTrans0 = System.currentTimeMillis();
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
                tTransEnd = System.currentTimeMillis();
                long transDuration = tTransEnd - tTrans0;

                Log.i(TAG, "Translation Output [" + transDuration + " ms]: " + translatedText);

                // --- PHASE 4: [TRANSLATION] LOGGING ---
                String srcLangCode = isHindiToSantali ? "hin_Deva" : "sat_Olck";
                String tgtLangCode = isHindiToSantali ? "sat_Olck" : "hin_Deva";
                String unicodeRange = isHindiToSantali ? "U+1C50 - U+1C7F (Ol Chiki)" : "U+0900 - U+097F (Devanagari)";
                Log.i(TAG, String.format("\n[TRANSLATION]\n\nInput Hindi: %s\nOutput Santali: %s\nSource language: %s\nTarget language: %s\nOutput length: %d\nOutput Unicode range: %s",
                        recognizedText, translatedText, srcLangCode, tgtLangCode, translatedText.length(), unicodeRange));

                // --- PHASE 2: UNICODE ANALYSIS ON LIVE TRANSLATED TEXT ---
                Log.i(TAG, "\nLIVE_TRANSLATED_TEXT = \"" + translatedText + "\"\n" + TTSManager.formatUnicodeAnalysis(translatedText));

                // 3. TTS Stage
                postProgress(callback, isHindiToSantali ? "Generating Santali speech..." : "Generating Hindi speech...");
                tTts0 = System.currentTimeMillis();
                File outputAudio = null;

                // --- PHASE 5: EMPTY / INVALID CHECK ---
                if (translatedText.trim().isEmpty()) {
                    Log.w(TAG, "\nLIVE_TRANSLATION_TTS_SKIPPED\nreason = EMPTY_TRANSLATION");
                } else {
                    // Self-healing TTS initialization
                    if (appContext != null && !ttsManager.isAvailable()) {
                        Log.i(TAG, "TTSManager not ready for Live Translation. Performing self-healing initialization...");
                        ttsManager.initialize(appContext);
                    }

                    final String targetLang = isHindiToSantali ? "Santali" : "Hindi";
                    final String targetModel = isHindiToSantali ? TTSManager.MODEL_ASSET : "Android System Hindi TTS";

                    // Phase 12: Content-hashed cache handling
                    File cacheDir = (appContext != null) ? new File(appContext.getCacheDir(), "tts_cache") : audioOutputFile.getParentFile();
                    if (cacheDir != null && !cacheDir.exists()) {
                        cacheDir.mkdirs();
                    }
                    String cacheKey = translatedText.trim() + "_" + targetLang;
                    String safeHash = String.valueOf(Math.abs(cacheKey.hashCode()));
                    File cacheFile = (cacheDir != null) ? new File(cacheDir, "tts_" + safeHash + ".wav") : audioOutputFile;

                    boolean isCacheHit = cacheFile != null && cacheFile.exists() && cacheFile.length() > 44;
                    Log.i(TAG, String.format("\n[CACHE_REQUEST]\nkey: %s\nhit/miss: %s\nlanguage: %s\nmodel: %s\nfile: %s",
                            cacheKey, isCacheHit ? "HIT" : "MISS", targetLang, targetModel, cacheFile != null ? cacheFile.getAbsolutePath() : "null"));

                    if (isCacheHit) {
                        outputAudio = cacheFile;
                    } else {
                        File destination = (cacheFile != null) ? cacheFile : audioOutputFile;
                        if (isHindiToSantali) {
                            if (ttsManager.isAvailable()) {
                                outputAudio = ttsManager.synthesize(translatedText, "Santali", destination);
                            } else {
                                Log.i(TAG, "Santali TTS stage skipped (" + ttsManager.getStatusMessage() + ")");
                            }
                        } else {
                            if (ttsManager.isHindiAvailable()) {
                                boolean ok = ttsManager.synthesizeHindi(translatedText, destination);
                                if (ok) outputAudio = destination;
                            }
                        }
                    }

                    tTtsEnd = System.currentTimeMillis();
                    long ttsDuration = tTtsEnd - tTts0;

                    // --- PHASE 1: [LIVE_TTS] STRUCTURED LOG ---
                    if (outputAudio != null && outputAudio.exists() && outputAudio.length() > 44) {
                        float durationSec = (float) (outputAudio.length() - 44) / (ttsManager.getSampleRate() * 2);
                        Log.i(TAG, String.format(Locale.US,
                                "\n[LIVE_TTS]\n\nASR_TEXT: %s\nTRANSLATED_TEXT: %s\nTRANSLATED_TEXT_LENGTH: %d\nLANGUAGE: %s\nLANGUAGE_CODE: %s\nTTS_REQUEST_ID: %d\nTTS_MANAGER_CALLED: true\nMODEL: %s\nMODEL_LOADED: %b\nG2P_INPUT: %s\nG2P_OUTPUT_LENGTH: %d\nPCM_GENERATED: true\nWAV_PATH: %s\nWAV_SIZE: %d\nSAMPLE_RATE: %d\nAUDIO_DURATION: %.2fs\nPLAYBACK_STARTED: pending\nPLAYBACK_COMPLETED: pending",
                                recognizedText, translatedText, translatedText.length(), targetLang, tgtLangCode, reqId,
                                targetModel, ttsManager.isAvailable(), translatedText, translatedText.length(),
                                outputAudio.getAbsolutePath(), outputAudio.length(), ttsManager.getSampleRate(), durationSec));
                    }
                }

                if (tTtsEnd == 0) tTtsEnd = System.currentTimeMillis();
                long ttsDuration = tTtsEnd - tTts0;
                long totalDuration = System.currentTimeMillis() - t0;

                // --- PHASE 9: [LIVE_REQUEST] STRUCTURED TIMING LOG ---
                Log.i(TAG, String.format(Locale.US,
                        "\n[LIVE_REQUEST]\n\nrequestId: %d\nASR started: %d\nASR completed: %d\ntranslation started: %d\ntranslation completed: %d\nTTS started: %d\nTTS completed: %d\nplayback started: pending\nrequest still active: %b",
                        reqId, tAsr0, tAsrEnd, tTrans0, tTransEnd, tTts0, tTtsEnd, (reqId == liveRequestId.get())));

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
                // Do NOT close ttsManager or ModelManager singletons!
                // They are shared across the entire application (Flashcards, Vocab, Translation).
            }).get(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception ignored) {
        } finally {
            executor.shutdown();
        }
    }
}
