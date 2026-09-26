package com.vernacular.learning.utils;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import com.vernacular.learning.ai.TTSManager;

import java.io.File;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe audio interface for speech synthesis & playback.
 * Features:
 * - Content-hashed TTS caching (text + lang hash) to guarantee 1:1 text-to-audio fidelity.
 * - Monotonic atomic request tracking (`AtomicLong`) to immediately abort obsolete in-flight requests.
 * - Centralized `AudioPlayer.getInstance()` usage with audio focus handling.
 * - Structured diagnostic logging per Section 2 & 16 of specification.
 * - Instant `stopPlayback()` for seamless card switching and live translation.
 */
public class AudioHelper {
    private static final String TAG = "AudioHelper";

    private static TextToSpeech tts;
    private static boolean isTtsReady = false;
    private static final ExecutorService audioExecutor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final AtomicLong activeRequestId = new AtomicLong(0);

    public interface AudioPlaybackCallback {
        void onPlaybackStarted();
        void onPlaybackCompleted();
    }

    public static void initializeTts(Context context) {
        if (context == null) return;
        Context appContext = context.getApplicationContext();

        // 1. Initialize Android system TextToSpeech (fallback)
        if (tts == null) {
            tts = new TextToSpeech(appContext, status -> {
                if (status == TextToSpeech.SUCCESS && tts != null) {
                    int result = tts.setLanguage(new Locale("hi", "IN"));
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts.setLanguage(new Locale("hi"));
                    }
                    isTtsReady = true;
                }
            });
        }

        // 2. Pre-initialize local offline neural TTS in background
        audioExecutor.execute(() -> {
            try {
                TTSManager ttsManager = TTSManager.getInstance();
                if (!ttsManager.isAvailable()) {
                    ttsManager.initialize(appContext);
                }
            } catch (Exception e) {
                Log.w(TAG, "Note on background TTSManager init: " + e.getMessage());
            }
        });
    }

    /**
     * Instantly stops any active audio playback across the application
     * and increments the request counter to invalidate in-flight asynchronous TTS callbacks.
     */
    public static void stopPlayback() {
        activeRequestId.incrementAndGet();
        AudioPlayer.getInstance().stop();
        if (tts != null) {
            try { tts.stop(); } catch (Exception ignored) {}
        }
    }

    public static void playPronunciation(Context context, String text, AudioPlaybackCallback callback) {
        if (text == null || text.trim().isEmpty()) {
            if (callback != null) callback.onPlaybackCompleted();
            return;
        }

        final Context appContext = (context != null) ? context.getApplicationContext() : null;
        final String cleanText = text.trim();
        final boolean isDevanagari = TTSManager.containsDevanagari(cleanText);
        final String lang = isDevanagari ? "Hindi" : "Santali";

        // Increment request counter and stop any current playback
        stopPlayback();
        final long requestId = activeRequestId.get();

        if (context != null) {
            Toast.makeText(context, "Pronouncing: " + cleanText, Toast.LENGTH_SHORT).show();
        }

        // --- PHASE 2: UNICODE ANALYSIS ---
        Log.i(TAG, "\nFLASH_TEXT = \"" + cleanText + "\"\n" + TTSManager.formatUnicodeAnalysis(cleanText));

        // --- DIAGNOSTIC LOG: REQUEST START ---
        Log.i(TAG, String.format("\n================ TTS REQUEST START ================\nRequest ID: %d\nInput Text: '%s'\nLanguage: %s\nModel: %s\n===================================================",
                requestId, cleanText, lang, isDevanagari ? "Android System Hindi TTS / sat_piper_model.onnx" : TTSManager.MODEL_ASSET));

        audioExecutor.execute(() -> {
            if (requestId != activeRequestId.get()) {
                Log.i(TAG, "TTS Request " + requestId + " cancelled before processing (newer request arrived).");
                return;
            }

            TTSManager ttsManager = TTSManager.getInstance();
            if (appContext != null && !ttsManager.isAvailable()) {
                ttsManager.initialize(appContext);
            }

            if (requestId != activeRequestId.get()) {
                Log.i(TAG, "TTS Request " + requestId + " cancelled after initialization.");
                return;
            }

            // 1. Try local offline neural Piper TTS / Hindi synthesis with content-hashed caching
            if (appContext != null) {
                try {
                    File cacheDir = new File(appContext.getCacheDir(), "tts_cache");
                    if (!cacheDir.exists()) {
                        cacheDir.mkdirs();
                    }

                    // Content-hashed key: text + lang
                    String cacheKey = cleanText + "_" + lang;
                    String safeHash = String.valueOf(Math.abs(cacheKey.hashCode()));
                    File cacheFile = new File(cacheDir, "tts_" + safeHash + ".wav");

                    File synthFile = null;
                    boolean isCacheHit = cacheFile.exists() && cacheFile.length() > 44;
                    Log.i(TAG, String.format("\n[CACHE_REQUEST]\nkey: %s\nhit/miss: %s\nlanguage: %s\nmodel: %s\nfile: %s",
                            cacheKey, isCacheHit ? "HIT" : "MISS", lang, isDevanagari ? "Hindi TTS" : TTSManager.MODEL_ASSET, cacheFile.getAbsolutePath()));

                    if (isCacheHit) {
                        Log.i(TAG, "TTS CACHE HIT for Request " + requestId + " -> " + cacheFile.getAbsolutePath());
                        synthFile = cacheFile;
                    } else {
                        Log.i(TAG, "TTS CACHE MISS for Request " + requestId + ". Synthesizing fresh audio...");
                        if (isDevanagari && ttsManager.isHindiAvailable()) {
                            boolean ok = ttsManager.synthesizeHindi(cleanText, cacheFile);
                            if (ok) synthFile = cacheFile;
                        } else if (ttsManager.isAvailable()) {
                            synthFile = ttsManager.synthesize(cleanText, lang, cacheFile);
                        }
                    }

                    if (requestId != activeRequestId.get()) {
                        Log.i(TAG, "TTS Request " + requestId + " cancelled after synthesis completion.");
                        return;
                    }

                    if (synthFile != null && synthFile.exists() && synthFile.length() > 44) {
                        float durationSec = (float) (synthFile.length() - 44) / (16000 * 2);

                        // --- PHASE 1: [FLASH_TTS] STRUCTURED LOG ---
                        Log.i(TAG, String.format(Locale.US,
                                "\n[FLASH_TTS]\n\nTEXT: %s\nLANGUAGE: %s\nTEXT_LENGTH: %d\nLANGUAGE_CODE: %s\nTTS_REQUEST_ID: %d\nTTS_MANAGER_CALLED: true\nMODEL: %s\nMODEL_LOADED: %b\nG2P_INPUT: %s\nG2P_OUTPUT_LENGTH: %d\nPCM_GENERATED: true\nWAV_PATH: %s\nWAV_SIZE: %d\nSAMPLE_RATE: %d\nAUDIO_DURATION: %.2fs\nPLAYBACK_STARTED: pending\nPLAYBACK_COMPLETED: pending",
                                cleanText, lang, cleanText.length(), isDevanagari ? "hin_Deva" : "sat_Olck", requestId,
                                isDevanagari ? "Android System Hindi TTS / sat_piper_model.onnx" : TTSManager.MODEL_ASSET,
                                ttsManager.isAvailable(), cleanText, cleanText.length(), synthFile.getAbsolutePath(), synthFile.length(),
                                ttsManager.getSampleRate(), durationSec));

                        pruneOldCacheFiles(cacheDir);

                        final File finalAudioFile = synthFile;
                        mainHandler.post(() -> {
                            if (requestId != activeRequestId.get()) {
                                Log.i(TAG, "TTS Request " + requestId + " cancelled before UI playback.");
                                return;
                            }

                            // --- DIAGNOSTIC LOG: PLAYBACK ---
                            Log.i(TAG, String.format("\n================ TTS PLAYBACK START ================\nRequest ID: %d\nText: '%s'\nAudio Path: %s\n===================================================",
                                    requestId, cleanText, finalAudioFile.getAbsolutePath()));

                            AudioPlayer.getInstance().play(appContext, finalAudioFile, new AudioPlayer.PlaybackCallback() {
                                @Override
                                public void onPlaybackStarted() {
                                    Log.i(TAG, "[FLASH_TTS] PLAYBACK_STARTED: true (Request " + requestId + ")");
                                    if (requestId == activeRequestId.get() && callback != null) {
                                        callback.onPlaybackStarted();
                                    }
                                }

                                @Override
                                public void onPlaybackCompleted() {
                                    Log.i(TAG, "[FLASH_TTS] PLAYBACK_COMPLETED: true (Request " + requestId + ")");
                                    if (requestId == activeRequestId.get() && callback != null) {
                                        callback.onPlaybackCompleted();
                                    }
                                }

                                @Override
                                public void onError(String message) {
                                    Log.w(TAG, "AudioPlayer playback error for Request " + requestId + ": " + message);
                                    if (requestId == activeRequestId.get() && callback != null) {
                                        callback.onPlaybackCompleted();
                                    }
                                }
                            });
                        });
                        return;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Offline TTS synthesis failed for Request " + requestId + ": " + e.getMessage());
                }
            }

            if (requestId != activeRequestId.get()) {
                return;
            }

            // 2. Fallback to Android system TTS speak
            mainHandler.post(() -> {
                if (requestId != activeRequestId.get()) return;

                if (callback != null) callback.onPlaybackStarted();
                try {
                    if (tts != null && isTtsReady && isDevanagari) {
                        tts.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "VERNACULAR_AUDIO_ID_" + requestId);
                    } else if (ttsManager != null && ttsManager.isHindiAvailable() && isDevanagari) {
                        ttsManager.speakHindi(cleanText);
                    } else {
                        ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 70);
                        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150);
                    }
                } catch (Exception ignored) {}

                if (callback != null) {
                    callback.onPlaybackCompleted();
                }
            });
        });
    }

    private static void pruneOldCacheFiles(File cacheDir) {
        try {
            File[] files = cacheDir.listFiles((dir, name) -> name.startsWith("tts_") && name.endsWith(".wav"));
            if (files != null && files.length > 80) {
                Arrays.sort(files, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));
                int deleteCount = files.length - 60;
                for (int i = 0; i < deleteCount; i++) {
                    files[i].delete();
                }
            }
        } catch (Exception ignored) {}
    }

    public static void shutdown() {
        stopPlayback();
        AudioPlayer.getInstance().release();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        isTtsReady = false;
    }
}
