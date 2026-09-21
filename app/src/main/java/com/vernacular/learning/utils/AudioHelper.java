package com.vernacular.learning.utils;

import android.content.Context;
import android.media.ToneGenerator;
import android.media.AudioManager;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;
import java.util.Locale;

/**
 * Audio interface for mother-tongue speech playback and sound feedback.
 * Ready for integration with team's offline neural speech models.
 */
public class AudioHelper {
    private static TextToSpeech tts;
    private static boolean isTtsReady = false;

    public interface AudioPlaybackCallback {
        void onPlaybackStarted();
        void onPlaybackCompleted();
    }

    public static void initializeTts(Context context) {
        if (tts == null) {
            tts = new TextToSpeech(context.getApplicationContext(), status -> {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(new Locale("hi", "IN"));
                    isTtsReady = (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED);
                }
            });
        }
    }

    public static void playPronunciation(Context context, String text, AudioPlaybackCallback callback) {
        if (callback != null) callback.onPlaybackStarted();

        try {
            if (tts != null && isTtsReady) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "VERNACULAR_AUDIO_ID");
            } else {
                // Gentle audio tone feedback for native words pending team audio model loading
                ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 70);
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150);
            }
        } catch (Exception e) {
            // Safe fallback
        }

        if (context != null) {
            Toast.makeText(context, "Pronouncing: " + text, Toast.LENGTH_SHORT).show();
        }

        if (callback != null) {
            callback.onPlaybackCompleted();
        }
    }

    public static void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }
}
