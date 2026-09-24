package com.vernacular.learning.ai;

import android.content.Context;
import android.util.Log;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Text-to-Speech Manager for local offline Santali speech synthesis.
 * Uses native Santali Piper VITS ONNX model (sat_piper_model.onnx)
 * with native Ol Chiki phoneme symbol mapping (U+1C50-U+1C7F).
 * Operates completely offline on device CPU within the 2 GB RAM budget.
 */
public class TTSManager {
    private static final String TAG = "TTSManager";

    public static final String MODEL_ASSET = "models/tts/sat_piper_model.onnx";
    public static final String CONFIG_ASSET = "models/tts/sat_piper_model.onnx.json";

    private OrtSession ttsSession;
    private final Map<String, List<Long>> phonemeIdMap = new HashMap<>();
    private int sampleRate = 16000;
    private float noiseScale = 0.667f;
    private float lengthScale = 1.0f;
    private float noiseW = 0.8f;
    private long bosId = 1L;
    private long eosId = 2L;
    private long padId = 0L;

    private boolean isInitialized = false;

    public synchronized boolean initialize(Context context) {
        if (isInitialized && ttsSession != null) {
            return true;
        }

        try {
            Log.i(TAG, "Initializing Santali Piper VITS TTS model...");
            long t0 = System.currentTimeMillis();

            // 1. Load config and phoneme mapping
            loadConfig(context);

            // 2. Load ONNX model session via ModelManager
            ModelManager modelManager = ModelManager.getInstance();
            ttsSession = modelManager.createSession(context, MODEL_ASSET);

            isInitialized = (ttsSession != null && !phonemeIdMap.isEmpty());
            long initTime = System.currentTimeMillis() - t0;
            Log.i(TAG, "Santali TTS initialized in " + initTime + " ms. Ready: " + isInitialized +
                    " (phoneme symbols: " + phonemeIdMap.size() + ", sampleRate: " + sampleRate + " Hz)");
            return isInitialized;
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Santali TTSManager", e);
            isInitialized = false;
            return false;
        }
    }

    public void loadConfig(InputStream is) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int r;
        while ((r = is.read(buf)) != -1) {
            baos.write(buf, 0, r);
        }
        JSONObject config = new JSONObject(baos.toString(StandardCharsets.UTF_8.name()));

        if (config.has("audio")) {
            JSONObject audio = config.getJSONObject("audio");
            if (audio.has("sample_rate")) {
                this.sampleRate = audio.getInt("sample_rate");
            }
        }

        if (config.has("inference")) {
            JSONObject inf = config.getJSONObject("inference");
            if (inf.has("noise_scale")) noiseScale = (float) inf.getDouble("noise_scale");
            if (inf.has("length_scale")) lengthScale = (float) inf.getDouble("length_scale");
            if (inf.has("noise_w")) noiseW = (float) inf.getDouble("noise_w");
        }

        if (config.has("phoneme_id_map")) {
            JSONObject idMapJson = config.getJSONObject("phoneme_id_map");
            Iterator<String> keys = idMapJson.keys();
            phonemeIdMap.clear();
            while (keys.hasNext()) {
                String symbol = keys.next();
                JSONArray arr = idMapJson.getJSONArray(symbol);
                List<Long> ids = new ArrayList<>(arr.length());
                for (int i = 0; i < arr.length(); i++) {
                    ids.add(arr.getLong(i));
                }
                phonemeIdMap.put(symbol, ids);
            }
        }

        if (phonemeIdMap.containsKey("^") && !phonemeIdMap.get("^").isEmpty()) {
            bosId = phonemeIdMap.get("^").get(0);
        }
        if (phonemeIdMap.containsKey("$") && !phonemeIdMap.get("$").isEmpty()) {
            eosId = phonemeIdMap.get("$").get(0);
        }
        if (phonemeIdMap.containsKey("_") && !phonemeIdMap.get("_").isEmpty()) {
            padId = phonemeIdMap.get("_").get(0);
        }
    }

    private void loadConfig(Context context) throws Exception {
        InputStream is = null;
        try {
            try {
                is = context.getAssets().open(CONFIG_ASSET);
            } catch (Exception e) {
                File localAsset = new File("src/main/assets/" + CONFIG_ASSET);
                if (!localAsset.exists()) {
                    localAsset = new File("app/src/main/assets/" + CONFIG_ASSET);
                }
                if (localAsset.exists()) {
                    is = new java.io.FileInputStream(localAsset);
                } else {
                    throw e;
                }
            }
            loadConfig(is);
        } finally {
            if (is != null) {
                try { is.close(); } catch (Exception ignored) {}
            }
        }
    }

    public boolean isAvailable() {
        return isInitialized && ttsSession != null;
    }

    public String getStatusMessage() {
        if (isAvailable()) {
            return "TTS Ready (Santali Piper VITS)";
        } else {
            return "TTS Unavailable";
        }
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public Map<String, List<Long>> getPhonemeIdMap() {
        return phonemeIdMap;
    }

    /**
     * Converts Santali Ol Chiki text into phoneme token sequence for Piper VITS.
     * Intersperses pad tokens between phonemes as required by the model.
     */
    public List<Long> textToPhonemeIds(String text) {
        List<Long> ids = new ArrayList<>();
        ids.add(bosId);

        if (text != null) {
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                String s = String.valueOf(c);
                if (phonemeIdMap.containsKey(s)) {
                    ids.addAll(phonemeIdMap.get(s));
                    ids.add(padId);
                }
            }
        }

        ids.add(eosId);
        return ids;
    }

    /**
     * Synthesizes Santali Ol Chiki text into a local 16kHz mono WAV audio file.
     *
     * @param santaliText Santali Ol Chiki Unicode text.
     * @param outputFile Destination WAV file.
     * @return File referencing generated WAV, or null if synthesis failed.
     */
    public synchronized File synthesize(String santaliText, File outputFile) {
        if (!isAvailable()) {
            Log.w(TAG, "Cannot synthesize: TTS is not available.");
            return null;
        }

        if (santaliText == null || santaliText.trim().isEmpty()) {
            Log.w(TAG, "Empty input text for Santali TTS.");
            return null;
        }

        long t0 = System.currentTimeMillis();
        String cleanText = santaliText.trim();
        Log.i(TAG, "Synthesizing Santali text [" + cleanText.length() + " chars]: '" + cleanText + "'");

        try {
            List<Long> phonemeIds = textToPhonemeIds(cleanText);
            int seqLen = phonemeIds.size();
            if (seqLen <= 2) {
                Log.w(TAG, "No valid phonemes found in Santali text.");
                return null;
            }

            long[] inputIds = new long[seqLen];
            for (int i = 0; i < seqLen; i++) {
                inputIds[i] = phonemeIds.get(i);
            }

            OrtEnvironment env = ModelManager.getInstance().getEnvironment();

            OnnxTensor inputTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(inputIds), new long[]{1, seqLen});
            OnnxTensor lengthsTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(new long[]{seqLen}), new long[]{1});
            OnnxTensor scalesTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(new float[]{noiseScale, lengthScale, noiseW}), new long[]{3});

            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put("input", inputTensor);
            inputs.put("input_lengths", lengthsTensor);
            inputs.put("scales", scalesTensor);

            OrtSession.Result result = ttsSession.run(inputs);
            OnnxTensor outputTensor = (OnnxTensor) result.get(0);

            FloatBuffer fb = outputTensor.getFloatBuffer();
            int sampleCount = fb.remaining();
            float[] samples = new float[sampleCount];
            fb.get(samples);

            inputTensor.close();
            lengthsTensor.close();
            scalesTensor.close();
            outputTensor.close();
            result.close();

            if (sampleCount == 0) {
                Log.e(TAG, "TTS generated 0 audio samples.");
                return null;
            }

            // Peak amplitude normalization to 0.95
            float maxAmp = 0.0f;
            for (float s : samples) {
                float abs = Math.abs(s);
                if (abs > maxAmp) maxAmp = abs;
            }

            float normFactor = (maxAmp > 1e-4f) ? (0.95f / maxAmp) : 1.0f;
            short[] pcmData = new short[sampleCount];
            for (int i = 0; i < sampleCount; i++) {
                float val = samples[i] * normFactor;
                if (val > 1.0f) val = 1.0f;
                if (val < -1.0f) val = -1.0f;
                pcmData[i] = (short) (val * 32767.0f);
            }

            // Write 16kHz mono WAV file
            writeWavFile(pcmData, sampleRate, outputFile);

            long inferenceDuration = System.currentTimeMillis() - t0;
            float audioDurationSec = (float) sampleCount / sampleRate;

            Log.i(TAG, String.format("Santali TTS synthesis completed in %d ms | Audio: %.2fs (%d samples) | File: %d bytes",
                    inferenceDuration, audioDurationSec, sampleCount, outputFile.length()));

            if (outputFile.exists() && outputFile.length() > 44 && audioDurationSec > 0.1f) {
                return outputFile;
            } else {
                Log.e(TAG, "Generated audio file validation failed.");
                return null;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error during Santali TTS synthesis", e);
            return null;
        }
    }

    /**
     * Writes 16-bit PCM samples to a standard 44-byte RIFF WAV file.
     */
    private void writeWavFile(short[] pcm, int sRate, File wavFile) throws IOException {
        File parent = wavFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        int channels = 1;
        long totalAudioLen = pcm.length * 2L;
        long totalDataLen = totalAudioLen + 36;
        long byteRate = (long) sRate * channels * 2;

        byte[] header = new byte[44];
        header[0] = 'R'; header[1] = 'I'; header[2] = 'F'; header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W'; header[9] = 'A'; header[10] = 'V'; header[11] = 'E';
        header[12] = 'f'; header[13] = 'm'; header[14] = 't'; header[15] = ' ';
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0; // Subchunk1Size (16 for PCM)
        header[20] = 1; header[21] = 0; // AudioFormat (1 for PCM)
        header[22] = (byte) channels; header[23] = 0;
        header[24] = (byte) (sRate & 0xff);
        header[25] = (byte) ((sRate >> 8) & 0xff);
        header[26] = (byte) ((sRate >> 16) & 0xff);
        header[27] = (byte) ((sRate >> 24) & 0xff);
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

        try (FileOutputStream fos = new FileOutputStream(wavFile)) {
            fos.write(header, 0, 44);
            byte[] buffer = new byte[4096];
            int bufIdx = 0;
            for (short sample : pcm) {
                buffer[bufIdx++] = (byte) (sample & 0xff);
                buffer[bufIdx++] = (byte) ((sample >> 8) & 0xff);
                if (bufIdx >= buffer.length) {
                    fos.write(buffer, 0, bufIdx);
                    bufIdx = 0;
                }
            }
            if (bufIdx > 0) {
                fos.write(buffer, 0, bufIdx);
            }
            fos.flush();
        }
    }

    public synchronized void close() {
        ModelManager.getInstance().closeSession("models/tts/sat_piper_model.onnx");
        ttsSession = null;
        isInitialized = false;
    }
}
