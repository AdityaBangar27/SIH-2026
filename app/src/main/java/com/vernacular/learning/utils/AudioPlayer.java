package com.vernacular.learning.utils;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.util.Log;
import java.io.File;

/**
 * Clean native MediaPlayer wrapper for offline playback of local audio files.
 */
public class AudioPlayer {
    private static final String TAG = "AudioPlayer";

    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;

    public interface PlaybackCallback {
        void onPlaybackStarted();
        void onPlaybackCompleted();
        void onError(String message);
    }

    public synchronized boolean isPlaying() {
        return isPlaying && mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public synchronized void play(File audioFile, PlaybackCallback callback) {
        stop();

        if (audioFile == null || !audioFile.exists() || audioFile.length() == 0) {
            if (callback != null) callback.onError("Audio file does not exist or is empty.");
            return;
        }

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build());

            mediaPlayer.setDataSource(audioFile.getAbsolutePath());
            mediaPlayer.setOnPreparedListener(mp -> {
                isPlaying = true;
                mp.start();
                if (callback != null) callback.onPlaybackStarted();
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                release();
                if (callback != null) callback.onPlaybackCompleted();
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                isPlaying = false;
                release();
                if (callback != null) callback.onError("MediaPlayer error: " + what + ", extra: " + extra);
                return true;
            });

            mediaPlayer.prepareAsync();

        } catch (Exception e) {
            Log.e(TAG, "Failed to start local audio playback", e);
            release();
            if (callback != null) callback.onError(e.getMessage());
        }
    }

    public synchronized void stop() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (Exception ignored) {}
            release();
        }
        isPlaying = false;
    }

    public synchronized void release() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        isPlaying = false;
    }
}
