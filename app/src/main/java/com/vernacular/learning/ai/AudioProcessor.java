package com.vernacular.learning.ai;

import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Handles audio recording conversion, WAV parsing, float normalization,
 * and Whisper 80-mel log-spectrogram feature extraction.
 */
public class AudioProcessor {
    private static final String TAG = "AudioProcessor";

    public static final int SAMPLE_RATE = 16000;
    public static final int N_FFT = 400;
    public static final int HOP_LENGTH = 160;
    public static final int N_MELS = 80;
    public static final int N_FRAMES = 3000; // 30.0s window
    public static final int FFT_BINS = N_FFT / 2 + 1; // 201

    private final float[][] melFilterbank;
    private final float[] hannWindow;
    private final float[][] cosTable;
    private final float[][] sinTable;

    public AudioProcessor() {
        this.hannWindow = createHannWindow(N_FFT);
        this.melFilterbank = createMelFilterbank(N_MELS, N_FFT, SAMPLE_RATE);

        // Precompute trigonometric tables for 400-point STFT
        this.cosTable = new float[FFT_BINS][N_FFT];
        this.sinTable = new float[FFT_BINS][N_FFT];
        for (int k = 0; k < FFT_BINS; k++) {
            for (int n = 0; n < N_FFT; n++) {
                double angle = 2.0 * Math.PI * k * n / N_FFT;
                this.cosTable[k][n] = (float) Math.cos(angle);
                this.sinTable[k][n] = (float) Math.sin(angle);
            }
        }

        // Try loading exact Whisper Slaney mel filterbank if accessible on filesystem
        File localFile = new File("src/main/assets/tokenizers/asr/mel_filters.bin");
        if (!localFile.exists()) {
            localFile = new File("app/src/main/assets/tokenizers/asr/mel_filters.bin");
        }
        if (localFile.exists()) {
            try (FileInputStream fis = new FileInputStream(localFile)) {
                loadMelFilters(fis);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Loads 80x201 Float32 Slaney Mel Filterbank directly from binary stream.
     */
    public void loadMelFilters(InputStream is) {
        if (is == null) return;
        try {
            byte[] bytes = new byte[N_MELS * FFT_BINS * 4];
            int totalRead = 0;
            while (totalRead < bytes.length) {
                int r = is.read(bytes, totalRead, bytes.length - totalRead);
                if (r == -1) break;
                totalRead += r;
            }
            if (totalRead == bytes.length) {
                FloatBuffer fb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
                for (int m = 0; m < N_MELS; m++) {
                    fb.get(melFilterbank[m]);
                }
                Log.i(TAG, "Successfully loaded exact Whisper Slaney mel filterbank (" + N_MELS + "x" + FFT_BINS + ")");
            } else {
                Log.w(TAG, "Incomplete mel filterbank read: " + totalRead + " bytes");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading mel filterbank", e);
        }
    }

    /**
     * Reads PCM audio samples from a 16-bit 16kHz mono WAV file and normalizes to [-1.0, 1.0].
     */
    public float[] readWavPcmSamples(File wavFile) {
        if (wavFile == null || !wavFile.exists()) {
            Log.e(TAG, "WAV file does not exist.");
            return new float[0];
        }

        try (FileInputStream fis = new FileInputStream(wavFile)) {
            byte[] header = new byte[44];
            int readHeader = fis.read(header);
            if (readHeader < 44) {
                Log.e(TAG, "Invalid WAV header length.");
                return new float[0];
            }

            int fileSize = (int) wavFile.length();
            int pcmDataSize = fileSize - 44;
            if (pcmDataSize <= 0) return new float[0];

            byte[] pcmBytes = new byte[pcmDataSize];
            int totalRead = 0;
            while (totalRead < pcmDataSize) {
                int r = fis.read(pcmBytes, totalRead, pcmDataSize - totalRead);
                if (r == -1) break;
                totalRead += r;
            }

            short[] shorts = new short[totalRead / 2];
            ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);

            float[] samples = new float[shorts.length];
            for (int i = 0; i < shorts.length; i++) {
                samples[i] = shorts[i] / 32768.0f;
            }
            return samples;
        } catch (Exception e) {
            Log.e(TAG, "Failed to read WAV file", e);
            return new float[0];
        }
    }

    /**
     * Normalizes 16-bit PCM short array to float [-1.0, 1.0].
     */
    public float[] normalizePcm(short[] pcm) {
        if (pcm == null) return new float[0];
        float[] samples = new float[pcm.length];
        for (int i = 0; i < pcm.length; i++) {
            samples[i] = pcm[i] / 32768.0f;
        }
        return samples;
    }

    private float getPaddedSample(float[] audio, int idx) {
        int len = audio.length;
        if (len == 0) return 0.0f;
        if (len == 1) return audio[0];
        if (idx < 0) {
            int ref = -idx;
            if (ref >= len) ref = len - 1;
            return audio[ref];
        } else if (idx >= len) {
            int ref = 2 * (len - 1) - idx;
            if (ref < 0) ref = 0;
            return audio[ref];
        }
        return audio[idx];
    }

    /**
     * Computes Whisper 80-mel log-spectrogram array of shape [1, 80, 3000].
     * Uses active frames calculation with 200-sample reflect padding and Slaney mel filterbank.
     */
    public float[] generateMelSpectrogram(float[] audioSamples) {
        float[] output = new float[1 * N_MELS * N_FRAMES];
        if (audioSamples == null || audioSamples.length == 0) {
            return output;
        }

        int activeFrames = Math.min(N_FRAMES, Math.max(1, (audioSamples.length + 200) / HOP_LENGTH + 1));
        float[][] stftFrames = new float[activeFrames][FFT_BINS];

        float[] windowed = new float[N_FFT];
        for (int frame = 0; frame < activeFrames; frame++) {
            int offset = frame * HOP_LENGTH - 200;
            for (int i = 0; i < N_FFT; i++) {
                int sampleIdx = offset + i;
                windowed[i] = getPaddedSample(audioSamples, sampleIdx) * hannWindow[i];
            }

            for (int k = 0; k < FFT_BINS; k++) {
                float re = 0.0f;
                float im = 0.0f;
                float[] cosK = cosTable[k];
                float[] sinK = sinTable[k];
                for (int i = 0; i < N_FFT; i++) {
                    re += windowed[i] * cosK[i];
                    im -= windowed[i] * sinK[i];
                }
                stftFrames[frame][k] = re * re + im * im;
            }
        }

        // Apply Mel Filterbank & Log scaling
        float[][] melSpectrogram = new float[N_MELS][N_FRAMES];
        for (int m = 0; m < N_MELS; m++) {
            float[] filter = melFilterbank[m];
            for (int frame = 0; frame < activeFrames; frame++) {
                float melVal = 0.0f;
                float[] power = stftFrames[frame];
                for (int k = 0; k < FFT_BINS; k++) {
                    melVal += filter[k] * power[k];
                }
                melSpectrogram[m][frame] = (float) Math.log10(Math.max(melVal, 1e-10));
            }
            for (int frame = activeFrames; frame < N_FRAMES; frame++) {
                melSpectrogram[m][frame] = -10.0f;
            }
        }

        // Global normalization across all mel bands
        float maxVal = -1e9f;
        for (int m = 0; m < N_MELS; m++) {
            for (int frame = 0; frame < N_FRAMES; frame++) {
                if (melSpectrogram[m][frame] > maxVal) {
                    maxVal = melSpectrogram[m][frame];
                }
            }
        }

        float clipFloor = maxVal - 8.0f;
        for (int m = 0; m < N_MELS; m++) {
            int rowOffset = m * N_FRAMES;
            for (int frame = 0; frame < N_FRAMES; frame++) {
                float val = Math.max(melSpectrogram[m][frame], clipFloor);
                output[rowOffset + frame] = (val + 4.0f) / 4.0f;
            }
        }

        return output;
    }

    private float[] createHannWindow(int length) {
        float[] window = new float[length];
        for (int i = 0; i < length; i++) {
            window[i] = (float) (0.5 * (1.0 - Math.cos(2.0 * Math.PI * i / length)));
        }
        return window;
    }

    private float[][] createMelFilterbank(int numMels, int fftSize, int sampleRate) {
        int numBins = fftSize / 2 + 1;
        float[][] filters = new float[numMels][numBins];

        float minMel = hzToMel(0);
        float maxMel = hzToMel(sampleRate / 2.0f);

        float[] melPoints = new float[numMels + 2];
        for (int i = 0; i < melPoints.length; i++) {
            melPoints[i] = minMel + i * (maxMel - minMel) / (numMels + 1);
        }

        float[] hzPoints = new float[melPoints.length];
        int[] binPoints = new int[melPoints.length];
        for (int i = 0; i < melPoints.length; i++) {
            hzPoints[i] = melToHz(melPoints[i]);
            binPoints[i] = Math.min(numBins - 1, (int) Math.floor((fftSize + 1) * hzPoints[i] / sampleRate));
        }

        for (int m = 0; m < numMels; m++) {
            int leftBin = binPoints[m];
            int centerBin = binPoints[m + 1];
            int rightBin = binPoints[m + 2];

            for (int k = leftBin; k < centerBin; k++) {
                if (centerBin != leftBin) {
                    filters[m][k] = (float) (k - leftBin) / (centerBin - leftBin);
                }
            }

            for (int k = centerBin; k < rightBin; k++) {
                if (rightBin != centerBin) {
                    filters[m][k] = (float) (rightBin - k) / (rightBin - centerBin);
                }
            }
        }

        return filters;
    }

    private float hzToMel(float hz) {
        return (float) (2595.0 * Math.log10(1.0 + hz / 700.0));
    }

    private float melToHz(float mel) {
        return (float) (700.0 * (Math.pow(10.0, mel / 2595.0) - 1.0));
    }
}
