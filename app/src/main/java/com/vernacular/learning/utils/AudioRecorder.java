package com.vernacular.learning.utils;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Clean native Android AudioRecord implementation for capturing 16kHz mono 16-bit PCM audio
 * and saving it as a standard valid WAV file.
 */
public class AudioRecorder {
    private static final String TAG = "AudioRecorder";

    public static final int SAMPLE_RATE = 16000;
    public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord audioRecord;
    private Thread recordingThread;
    private volatile boolean isRecording = false;
    private File currentOutputFile;
    private File tempPcmFile;

    public interface RecordingCallback {
        void onRecordingStarted();
        void onRecordingStopped(File outputFile);
        void onError(String errorMessage);
    }

    public boolean isRecording() {
        return isRecording;
    }

    @SuppressLint("MissingPermission")
    public synchronized boolean startRecording(File outputFile, RecordingCallback callback) {
        if (isRecording) {
            Log.w(TAG, "Already recording.");
            return false;
        }

        int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            minBufferSize = SAMPLE_RATE * 2;
        }
        final int bufferSize = Math.max(minBufferSize, 4096);

        try {
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
            );

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                if (callback != null) callback.onError("Failed to initialize AudioRecord (state not initialized).");
                release();
                return false;
            }

            this.currentOutputFile = outputFile;
            this.tempPcmFile = new File(outputFile.getParentFile(), outputFile.getName() + ".pcm");

            audioRecord.startRecording();
            isRecording = true;

            recordingThread = new Thread(() -> {
                writePcmData(bufferSize);
                if (callback != null) {
                    callback.onRecordingStopped(currentOutputFile);
                }
            }, "AudioRecorderThread");
            recordingThread.start();

            if (callback != null) {
                callback.onRecordingStarted();
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Exception starting audio recording", e);
            if (callback != null) callback.onError(e.getMessage());
            release();
            return false;
        }
    }

    private void writePcmData(int bufferSize) {
        byte[] buffer = new byte[bufferSize];
        try (FileOutputStream fos = new FileOutputStream(tempPcmFile)) {
            while (isRecording && audioRecord != null) {
                int read = audioRecord.read(buffer, 0, buffer.length);
                if (read > 0) {
                    fos.write(buffer, 0, read);
                }
            }
            fos.flush();
        } catch (IOException e) {
            Log.e(TAG, "Error writing raw PCM file", e);
        }

        // Convert PCM to standard WAV with 44-byte RIFF header
        copyPcmToWav(tempPcmFile, currentOutputFile);
        if (tempPcmFile.exists()) {
            tempPcmFile.delete();
        }
    }

    public synchronized void stopRecording() {
        if (!isRecording) return;
        isRecording = false;

        if (audioRecord != null) {
            try {
                if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop();
                }
            } catch (Exception e) {
                Log.w(TAG, "Error stopping AudioRecord", e);
            }
        }

        if (recordingThread != null) {
            try {
                recordingThread.join(1000);
            } catch (InterruptedException ignored) {}
            recordingThread = null;
        }

        release();
    }

    public synchronized void release() {
        isRecording = false;
        if (audioRecord != null) {
            try {
                audioRecord.release();
            } catch (Exception ignored) {}
            audioRecord = null;
        }
    }

    private void copyPcmToWav(File pcmFile, File wavFile) {
        if (!pcmFile.exists()) return;

        long totalAudioLen = pcmFile.length();
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = SAMPLE_RATE;
        int channels = 1;
        long byteRate = 16 * SAMPLE_RATE * channels / 8; // 32000

        byte[] header = new byte[44];
        header[0] = 'R'; header[1] = 'I'; header[2] = 'F'; header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W'; header[9] = 'A'; header[10] = 'V'; header[11] = 'E';
        header[12] = 'f'; header[13] = 'm'; header[14] = 't'; header[15] = ' ';
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0; // Subchunk1Size
        header[20] = 1; header[21] = 0; // AudioFormat (1 = PCM)
        header[22] = (byte) channels; header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (channels * 2); header[33] = 0; // BlockAlign
        header[34] = 16; header[35] = 0; // BitsPerSample
        header[36] = 'd'; header[37] = 'a'; header[38] = 't'; header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        try (FileOutputStream fos = new FileOutputStream(wavFile);
             FileInputStream fis = new FileInputStream(pcmFile)) {
            fos.write(header, 0, 44);
            byte[] buf = new byte[4096];
            int r;
            while ((r = fis.read(buf)) != -1) {
                fos.write(buf, 0, r);
            }
            fos.flush();
        } catch (IOException e) {
            Log.e(TAG, "Error generating WAV file", e);
        }
    }
}
