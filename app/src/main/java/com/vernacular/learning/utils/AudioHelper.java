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
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Audio interface for mother-tongue speech playback and sound feedback.
 * Uses the local offline neural Piper VITS TTSManager for authentic Santali and Hindi pronunciation,
 * with graceful fallback to Android system TextToSpeech.
 */
public class AudioHelper {
    private static final String TAG = "AudioHelper";

    private static TextToSpeech tts;
    private static boolean isTtsReady = false;
    private static final AudioPlayer audioPlayer = new AudioPlayer();
    private static final ExecutorService audioExecutor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

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

    public static void playPronunciation(Context context, String text, AudioPlaybackCallback callback) {
        if (text == null || text.trim().isEmpty()) {
            if (callback != null) callback.onPlaybackCompleted();
            return;
        }

        final String cleanText = text.trim();
        final Context appContext = (context != null) ? context.getApplicationContext() : null;

        if (context != null) {
            Toast.makeText(context, "Pronouncing: " + cleanText, Toast.LENGTH_SHORT).show();
        }

        audioExecutor.execute(() -> {
            TTSManager ttsManager = TTSManager.getInstance();
            if (appContext != null && !ttsManager.isAvailable()) {
                ttsManager.initialize(appContext);
            }

            // 1. Try local offline neural Piper TTS
            if (ttsManager.isAvailable() && appContext != null) {
                try {
                    File cacheDir = appContext.getCacheDir();
                    File tempWav = new File(cacheDir, "pronounce_" + System.currentTimeMillis() + ".wav");
                    File synthFile = ttsManager.synthesize(cleanText, tempWav);

                    if (synthFile != null && synthFile.exists() && synthFile.length() > 44) {
                        mainHandler.post(() -> {
                            audioPlayer.play(synthFile, new AudioPlayer.PlaybackCallback() {
                                @Override
                                public void onPlaybackStarted() {
                                    if (callback != null) callback.onPlaybackStarted();
                                }

                                @Override
                                public void onPlaybackCompleted() {
                                    if (callback != null) callback.onPlaybackCompleted();
                                    tempWav.delete();
                                }

                                @Override
                                public void onError(String message) {
                                    Log.w(TAG, "AudioPlayer playback error: " + message);
                                    if (callback != null) callback.onPlaybackCompleted();
                                    tempWav.delete();
                                }
                            });
                        });
                        return;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Offline neural TTS synthesis failed, trying fallback: " + e.getMessage());
                }
            }

            // 2. Fallback to Android system TTS or tone
            mainHandler.post(() -> {
                if (callback != null) callback.onPlaybackStarted();
                try {
                    if (tts != null && isTtsReady && TTSManager.containsDevanagari(cleanText)) {
                        tts.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "VERNACULAR_AUDIO_ID");
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

    public static void shutdown() {
        audioPlayer.stop();
        audioPlayer.release();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        isTtsReady = false;
    }
}

