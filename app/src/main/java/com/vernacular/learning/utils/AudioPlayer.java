package com.vernacular.learning.utils;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.os.Build;
import android.util.Log;
import java.io.File;

/**
 * Centralized, thread-safe Singleton MediaPlayer manager with system audio focus handling.
 * Prevents multiple audio streams from playing simultaneously across Live Translation, Flashcards,
 * and Study Material activities.
 */
public class AudioPlayer {
    private static final String TAG = "AudioPlayer";

    private static volatile AudioPlayer sInstance;

    public static AudioPlayer getInstance() {
        if (sInstance == null) {
            synchronized (AudioPlayer.class) {
                if (sInstance == null) {
                    sInstance = new AudioPlayer();
                }
            }
        }
        return sInstance;
    }

    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;

    // Audio focus management
    private AudioManager audioManager;
    private AudioFocusRequest focusRequest;
    private final AudioManager.OnAudioFocusChangeListener focusChangeListener = focusChange -> {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
            focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            stop();
        }
    };

    public interface PlaybackCallback {
        void onPlaybackStarted();
        void onPlaybackCompleted();
        void onError(String message);
    }

    public synchronized boolean isPlaying() {
        return isPlaying && mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public synchronized void play(Context context, File audioFile, PlaybackCallback callback) {
        stop();

        if (audioFile == null || !audioFile.exists() || audioFile.length() <= 44) {
            if (callback != null) callback.onError("Audio file does not exist or is invalid.");
            return;
        }

        try {
            if (context != null) {
                audioManager = (AudioManager) context.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
                if (audioManager != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build();
                        focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                                .setAudioAttributes(playbackAttributes)
                                .setAcceptsDelayedFocusGain(false)
                                .setOnAudioFocusChangeListener(focusChangeListener)
                                .build();
                        audioManager.requestAudioFocus(focusRequest);
                    } else {
                        audioManager.requestAudioFocus(focusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
                    }
                }
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build());

            mediaPlayer.setDataSource(audioFile.getAbsolutePath());

            mediaPlayer.setOnPreparedListener(mp -> {
                synchronized (AudioPlayer.this) {
                    isPlaying = true;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            PlaybackParams params = mp.getPlaybackParams();
                            params.setSpeed(1.0f);
                            params.setPitch(1.0f);
                            mp.setPlaybackParams(params);
                        } catch (Exception ignored) {}
                    }
                    mp.start();
                }
                if (callback != null) callback.onPlaybackStarted();
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                synchronized (AudioPlayer.this) {
                    isPlaying = false;
                    release();
                }
                if (callback != null) callback.onPlaybackCompleted();
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                synchronized (AudioPlayer.this) {
                    isPlaying = false;
                    release();
                }
                Log.w(TAG, "MediaPlayer error: " + what + ", extra: " + extra);
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

    public synchronized void play(File audioFile, PlaybackCallback callback) {
        play(null, audioFile, callback);
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
        if (audioManager != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
                    audioManager.abandonAudioFocusRequest(focusRequest);
                } else if (focusChangeListener != null) {
                    audioManager.abandonAudioFocus(focusChangeListener);
                }
            } catch (Exception ignored) {}
        }
        isPlaying = false;
    }
}
